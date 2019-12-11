package org.cafienne.humantask.akka.command.response;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.command.response.ModelResponse;
import org.cafienne.cmmn.akka.command.response.CaseLastModified;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.akka.command.response.CaseResponse;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.humantask.akka.command.HumanTaskCommand;

import java.io.IOException;

@Manifest
public class HumanTaskResponse extends CaseResponse {
    private final String taskId;

    private enum Fields {
        taskId
    }

    public HumanTaskResponse(HumanTaskCommand command) {
        super(command);
        this.taskId = command.actorId;
    }

    public HumanTaskResponse(ValueMap json) {
        super(json);
        this.taskId = readField(json, Fields.taskId);
    }

    public CaseLastModified caseLastModified() {
        return new CaseLastModified(taskId, getLastModified());
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.taskId, taskId);
    }
}