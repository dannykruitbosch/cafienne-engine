/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.instance;

import org.cafienne.cmmn.akka.event.file.CaseFileEvent;
import org.cafienne.cmmn.definition.casefile.CaseFileItemDefinition;
import org.cafienne.cmmn.instance.casefile.Value;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.cmmn.instance.sentry.CaseFileItemOnPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class CaseFileItem extends CaseFileItemCollection<CaseFileItemDefinition> {

    private final List<CaseFileItemOnPart> connectedEntryCriteria = new ArrayList<>();
    private final List<CaseFileItemOnPart> connectedExitCriteria = new ArrayList<>();

    /**
     * History of events on this item
     */
    private final TransitionPublisher transitionPublisher = new TransitionPublisher(this);
    private State state = State.Null; // Current state of the item
    private CaseFileItemTransition lastTransition; // Last transition

    private Value<?> value = Value.NULL;
    private Value<?> removedValue = Value.NULL;
    /**
     * The parent case file item that we are contained in, or null if we are contained in the top level case file.
     */
    private final CaseFileItem parent;
    /**
     * The array that this CaseFileItem belongs to (if we have multiplicity "...OrMore"). Otherwise <code>null</code>
     */
    private final CaseFileItemArray array;
    /**
     * Our location in the {@link CaseFileItem#array}, if we belong to one. Else -1.
     */
    private int indexInArray;

    protected CaseFileItem(Case caseInstance, CaseFileItemDefinition definition, CaseFileItemCollection<?> parent, CaseFileItemArray array, int indexInArray) {
        super(caseInstance, definition, definition.getName());
        this.parent = parent instanceof CaseFileItem ? (CaseFileItem) parent : null;
        this.array = array;
        this.indexInArray = indexInArray;
        getCaseInstance().getSentryNetwork().connect(this);
    }

    public CaseFileItem(Case caseInstance, CaseFileItemDefinition definition, CaseFileItemCollection<?> parent) {
        this(caseInstance, definition, parent, null, -1);
    }

    /**
     * Constructor for {@link CaseFileItemArray}.
     * @param caseInstance
     * @param definition
     * @param parent
     */
    protected CaseFileItem(CaseFileItemDefinition definition, Case caseInstance, CaseFileItemCollection<?> parent) {
        this(caseInstance, definition, parent, null, -1);
    }

    /**
     * Returns the parent of this case file item, or null if this is a top level item (i.e., it is a child of the casefile)
     * @return
     */
    public CaseFileItem getParent() {
        return parent;
    }

    /**
     * Returns the child items of this case file item
     * 
     * @return
     */
    public Map<CaseFileItemDefinition, CaseFileItem> getChildren() {
        return getItems();
    }

    /**
     * Links the on part to this case file item. Is used by the case file item to connect the connected criterion whenever a transition happens.
     * 
     * @param onPart
     */
    public void connectOnPart(CaseFileItemOnPart onPart) {
        transitionPublisher.connectOnPart(onPart);
    }

    /**
     * Framework method that allows parameters to be bound back to the case file
     * @param p
     * @param parameterValue
     */
    void bindParameter(Parameter<?> p, Value<?> parameterValue) {
        // Spec says (table 5.3.4, page 36): just trigger the proper transition, as that will be obvious. But is it?
        switch (state) {
        case Available:
            replaceContent(parameterValue);
            break;
        case Discarded:
            throw new TransitionDeniedException("Cannot bind parameter value, because case file item has been deleted");
        case Null:
            createContent(parameterValue);
        default:
            break;
        }
    }

    private void makeTransition(CaseFileItemTransition transition, State newState, Value value) {
        // Now inform the sentry network of our change
        getCaseInstance().addEvent(new CaseFileEvent(this.getCaseInstance(), this.getName(), newState, transition, value, getPath(), indexInArray));
    }

    public void updateState(CaseFileEvent event) {
        addDebugInfo(() -> "CaseFile["+getName()+"]: updating CaseFileItem state based on CaseFileEvent");
        this.transitionPublisher.addEvent(event);
        this.state = event.getState();
        this.indexInArray = event.getIndex();
        this.lastTransition = event.getTransition();
        this.setValue(event.getValue());
        if (this.array != null) { // Update the array we belong too as well
            this.array.childChanged(this);
        }

        // Now propagate our new value into our parent.
        Value<?> valueToPropagate = this.value;
        if (array != null) { // Update the array we belong too as well
            array.childChanged(this);
            valueToPropagate = array.getValue();
        }
        propagateValueChangeToParent(getName(), valueToPropagate);
    }

    public void informConnectedEntryCriteria(CaseFileEvent event) {
        // Then inform the activating sentries
        transitionPublisher.informEntryCriteria(event);
    }

    public void informConnectedExitCriteria(CaseFileEvent event) {
        // Finally iterate the terminating sentries and inform them
        transitionPublisher.informExitCriteria(event);
        addDebugInfo(() -> "CaseFile["+getName()+"]: Completed behavior for transition " + event.getTransition());
    }

    // TODO: The following 4 methods should generate specific events instead of generic CaseFileEvent event
    public void createContent(Value<?> newContent) {
        validateState(State.Null, "create");
        validateContents(newContent);
        makeTransition(CaseFileItemTransition.Create, State.Available, newContent);
    }

    public void replaceContent(Value<?> newContent) {
        validateState(State.Available, "replace");
        validateContents(newContent);
        makeTransition(CaseFileItemTransition.Replace, State.Available, newContent);
    }

    public void updateContent(Value<?> newContent) {
        validateState(State.Available, "update");
        validateContents(newContent);
        makeTransition(CaseFileItemTransition.Update, State.Available, value.merge(newContent));
    }

    public void deleteContent() {
        validateState(State.Available, "delete");
        removedValue = value;
        makeTransition(CaseFileItemTransition.Delete, State.Discarded, Value.NULL);
        // Now recursively also delete all of our children... Are you sure? Isn't this overinterpreting the spec?
        getChildren().forEach((definition, childItem) -> childItem.deleteContent());
    }

    /**
     * Check whether we are in the correct state to execute the operation
     * @param expectedState
     * @param operationName
     */
    private void validateState(State expectedState, String operationName) {
        if (state != expectedState) {
            throw new TransitionDeniedException("Cannot " + operationName + " case file item " + getName() + " because it is in state " + state + " and should be in state " + expectedState + ".");
        }
    }

    /**
     * Check if the new content matches our definition
     * @param newContent
     */
    private void validateContents(Value<?> newContent) {
        getDefinition().getCaseFileItemDefinition().getDefinitionType().validate(this, newContent);
    }

    /**
     * Sets the value of this CaseFileItem. Internal framework method not to be used from applications.
     * @param newValue
     */
    public void setValue(Value<?> newValue) {
        addDebugInfo(() -> "Setting case file item [" + getPath() + "] value", newValue);

        this.value = newValue;
        if (newValue != null) newValue.setOwner(this);
    }

    /**
     * This updates the json structure of the parent CaseFileItem, without triggering CaseFileItemTransitions
     * @param childName
     * @param childValue
     */
    private void propagateValueChangeToParent(String childName, Value<?> childValue) {
        if (parent != null) {
            if (parent.value==null || parent.value == Value.NULL) {
                addDebugInfo(() -> "Creating a location in parent "+parent.getPath()+" to store the newly changed child "+getName());
                parent.value = new ValueMap();
            }
            if (parent.value instanceof ValueMap) {
                ((ValueMap) parent.value).put(childName, childValue);
                // And ... recurse
                parent.propagateValueChangeToParent(parent.getName(), parent.value);
            } else {
                addDebugInfo(() -> "Cannot propagate change in " + getPath()+" into parent, because it's value is not a ValueMap but a " + parent.value.getClass().getName());
            }
        }
    }

    /**
     * Returns the content of the CaseFileItem
     * @return
     */
    public Value<?> getValue() {
        return value;
    }

    /**
     * Returns the last known transition on this case file item
     * 
     * @return
     */
    public CaseFileItemTransition getLastTransition() {
        return lastTransition;
    }

    /**
     * Returns a path to this case file item.
     * 
     * @return
     */
    public Path getPath() {
        return new Path(this);
    }

    /**
     * Returns the current state of the case file item.
     * @return
     */
    public State getState() {
        return state;
    }

    @Override
    public String toString() {
        return getName() + " : " + value;
    }

    /**
     * Dump the CaseFile item as XML.
     * 
     * @param parentElement
     */
    public void dumpMemoryStateToXML(Element parentElement) {
        Element caseFileItemXML = parentElement.getOwnerDocument().createElement("CaseFileItem");
        parentElement.appendChild(caseFileItemXML);
        caseFileItemXML.setAttribute("name", getDefinition().getName());
        caseFileItemXML.setAttribute("transition", "" + lastTransition);
        if (array != null) {
            caseFileItemXML.setAttribute("index", "" + indexInArray);
        }

        // First print our contents (includes probably also content of our children...)
        Element contentElement = parentElement.getOwnerDocument().createElement("Content");
        caseFileItemXML.appendChild(contentElement);
        value.dumpMemoryStateToXML(contentElement);

        // Next, print our children.
        Iterator<Entry<CaseFileItemDefinition, CaseFileItem>> c = getChildren().entrySet().iterator();
        while (c.hasNext()) {
            c.next().getValue().dumpMemoryStateToXML(caseFileItemXML);
        }
        // XMLHelper.printXMLNode(parentElement)
    }

    /**
     * Reference to most recently updated case file item. Returns <code>this</code> by default. CaseFileItemArray overwrites it
     * and returns the most recently changed / updated case file item in it's array
     * @return
     */
    public CaseFileItem getCurrent() {
        return this;
    }

    /**
     * Resolve the path on this case file item. Base implementation that is overridden in {@link CaseFileItemArray}, where the index
     * accessor of the path is used to navigate to the correct case file item.
     * @param currentPath
     * @return
     */
    CaseFileItem resolve(Path currentPath) {
        return this;
    }

    /**
     * Returns the location of this case file item within the array it belongs to. If the case file item is not "iterable", then -1 is returned.
     * @return
     */
    public int getIndex() {
        return indexInArray;
    }

    public List<CaseFileItemOnPart> getConnectedEntryCriteria() {
        return connectedEntryCriteria;
    }

    public List<CaseFileItemOnPart> getConnectedExitCriteria() {
        return connectedExitCriteria;
    }
}

/**
 * Case file item that represents an empty item.
 * See CMMN 1.0 specification page 107 ("an empty case file item must be returned")
 *
 */
class EmptyCaseFileItem extends CaseFileItem {
    private final static Logger logger = LoggerFactory.getLogger(EmptyCaseFileItem.class);

    EmptyCaseFileItem(CaseFileItem parent, String creationReason) {
        super(parent.getCaseInstance(), parent.getDefinition(), parent);
        logger.warn(creationReason);
    }

    @Override
    public void createContent(Value<?> newContent) {
        logger.warn("Creating content in EmptyCaseFileItem");
    }

    @Override
    public void updateContent(Value<?> newContent) {
        logger.warn("Updating content in EmptyCaseFileItem");
    }

    @Override
    public void replaceContent(Value<?> newContent) {
        logger.warn("Replacing content in EmptyCaseFileItem");
    }

    @Override
    public void deleteContent() {
        logger.warn("Deleting content in EmptyCaseFileItem");
    }
    
    @Override
    public Value<?> getValue() {
        logger.warn("Returning value from EmptyCaseFileItem");
        return Value.NULL;
    }
    
    @Override
    public void setValue(Value<?> newValue) {
        logger.warn("Setting value in EmptyCaseFileItem");
    }

    @Override
    void bindParameter(Parameter<?> p, Value<?> parameterValue) {
        logger.warn("Binding parameter to EmptyCaseFileItem");
    }
}