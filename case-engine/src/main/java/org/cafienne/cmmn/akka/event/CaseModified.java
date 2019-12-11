package org.cafienne.cmmn.akka.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.cmmn.akka.command.CaseCommand;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.CaseInstanceEvent;
import org.cafienne.cmmn.instance.State;
import org.cafienne.cmmn.instance.casefile.LongValue;
import org.cafienne.cmmn.instance.casefile.ValueMap;

import java.io.IOException;
import java.time.Instant;

/**
 * Event that is published after an {@link CaseCommand} has been fully handled by a {@link Case} instance.
 * Contains information about the last modified moment.
 *
 */
@Manifest
public class CaseModified extends CaseInstanceEvent {
    private final Instant lastModified;
    private final int numFailures;
    private final State state;

    public enum Fields {
        lastModified, numFailures, state
    }

    public CaseModified(Case caseInstance, Instant lastModified, int numFailures) {
        super(caseInstance);
        this.numFailures = numFailures;
        this.lastModified = lastModified;
        this.state = caseInstance.getCasePlan().getState();
    }

    public CaseModified(ValueMap json) {
        super(json);
        this.lastModified = readInstant(json, Fields.lastModified);
        this.numFailures = json.rawInt(Fields.numFailures);
        this.state = readEnum(json, Fields.state, State.class);
    }

    /**
     * Returns the moment at which the case was last modified
     * @return
     */
    public Instant lastModified() {
        return lastModified;
    }

    /**
     * Returns the state that the case plan currently has
     * @return
     */
    public State getState() {
        return this.state;
    }

    /**
     * Returns the number of plan items within the case in state Failed.
     * @return
     */
    public int getNumFailures() {
        return numFailures;
    }

    @Override
    public String toString() {
        return "CaseModified[" + getCaseInstanceId() + "] at " + lastModified;
    }

    @Override
    public void recover(Case caseInstance) {
        caseInstance.recoverLastModified(lastModified);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeCaseInstanceEvent(generator);
        writeField(generator, Fields.numFailures, new LongValue(numFailures));
        writeField(generator, Fields.state, state);
        writeField(generator, Fields.lastModified, lastModified);
    }
}