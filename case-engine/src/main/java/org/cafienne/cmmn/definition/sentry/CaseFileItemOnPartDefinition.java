/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.definition.sentry;

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.Definition;
import org.cafienne.cmmn.definition.casefile.CaseFileItemDefinition;
import org.cafienne.cmmn.instance.sentry.CaseFileItemOnPart;
import org.cafienne.cmmn.instance.CaseFileItemTransition;
import org.cafienne.cmmn.instance.sentry.Sentry;
import org.w3c.dom.Element;

public class CaseFileItemOnPartDefinition extends OnPartDefinition {
    private CaseFileItemTransition standardEvent;
    private final String sourceRef;
    private CaseFileItemDefinition source;

    public CaseFileItemOnPartDefinition(Element element, Definition definition, CMMNElementDefinition parentElement) {
        super(element, definition, parentElement);
        String standardEventName = parse("standardEvent", String.class, true);
        try {
            standardEvent = CaseFileItemTransition.getEnum(standardEventName);
        } catch (IllegalArgumentException | NullPointerException e) {
            getCaseDefinition().addDefinitionError("The standard event '" + standardEventName + "' in sentry " + getParentElement().getName() + " is not valid");
        }
        sourceRef = parseAttribute("sourceRef", true);
    }

    @Override
    protected void resolveReferences() {
        super.resolveReferences();
        // Add a check that the source only has to be filled if the standardEvent is specified
        source = getCaseDefinition().findCaseFileItem(sourceRef);
        if (source == null) {
            getDefinition()
                    .addReferenceError("A case file item with name '" + sourceRef + "' is referenced from sentry " + getParentElement().getName() + ", but it does not exist in the case file model");
        }
    }

    public CaseFileItemTransition getStandardEvent() {
        return standardEvent;
    }

    public CaseFileItemDefinition getSource() {
        return source;
    }

    @Override
    public CaseFileItemOnPart createInstance(Sentry sentry) {
        return new CaseFileItemOnPart(sentry, this);
    }
}