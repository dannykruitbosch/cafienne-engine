/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.akka.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.cmmn.akka.event.task.TaskEvent;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.Task;
import org.cafienne.cmmn.instance.casefile.ValueMap;

import java.io.IOException;

@Manifest
public class TaskInputFilled extends TaskEvent {
    private final ValueMap taskParameters;
    private final ValueMap mappedInputParameters;

    private enum Fields {
        taskParameters, mappedInputParameters
    }

    public TaskInputFilled(Task<?> task, ValueMap inputParameters, ValueMap mappedInputParameters) {
        super(task);
        this.taskParameters = inputParameters;
        this.mappedInputParameters = mappedInputParameters;
    }

    public TaskInputFilled(ValueMap json) {
        super(json);
        this.taskParameters = readMap(json, Fields.taskParameters);
        this.mappedInputParameters = readMap(json, Fields.mappedInputParameters);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeTaskEvent(generator);
        writeField(generator, Fields.taskParameters, taskParameters);
        writeField(generator, Fields.mappedInputParameters, mappedInputParameters);
    }

    @Override
    public String toString() {
        return "Task["+ getTaskId() + "] has input:\n" + taskParameters;
    }

    public ValueMap getTaskInputParameters() {
        return taskParameters;
    }

    public ValueMap getMappedInputParameters() {
        return mappedInputParameters;
    }

    @Override
    protected void recoverTaskEvent(Task task) {
        task.recoverTaskEvent(this);
    }
}