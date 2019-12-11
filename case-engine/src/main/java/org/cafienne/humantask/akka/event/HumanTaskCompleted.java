/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.humantask.akka.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.cmmn.instance.task.humantask.HumanTask;
import org.cafienne.humantask.instance.TaskAction;
import org.cafienne.humantask.instance.TaskState;
import org.cafienne.humantask.instance.WorkflowTask;

import java.io.IOException;

@Manifest
public class HumanTaskCompleted extends HumanTaskTransitioned {
    private final ValueMap taskOutput; // taskOutput - task saved output

    private enum Fields {
        taskOutput
    }

    public HumanTaskCompleted(HumanTask task, ValueMap output) {
        super(task, TaskState.Completed, TaskAction.Complete);
        this.taskOutput = output;
    }

    public HumanTaskCompleted(ValueMap json) {
        super(json);
        this.taskOutput = readMap(json, Fields.taskOutput);
    }

    public void updateState(WorkflowTask task) {
        task.setOutput(taskOutput);
        task.complete();
    }

    /**
     * Get assignee for the task
     * @return assignee for the task
     */
    public ValueMap getTaskOutput() {
        return this.taskOutput;
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeTransitionEvent(generator);
        writeField(generator, Fields.taskOutput, taskOutput);
    }
}