/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.humantask.akka.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.cmmn.akka.event.task.TaskEvent;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.cmmn.instance.task.humantask.HumanTask;
import org.cafienne.humantask.instance.WorkflowTask;

import java.io.IOException;

public abstract class HumanTaskEvent extends TaskEvent<HumanTask> {
    public static final String TAG = "cafienne:task";

    public final String taskId; // taskName is same as the planItem id
    private final String taskName; // taskName is same as the planItemName

    private enum Fields {
        taskName, taskId
    }

    /**
     * Constructor used by HumanTaskCreated event, since at that moment the task name is not yet known
     * inside the task actor.
     * @param task
     */
    protected HumanTaskEvent(HumanTask task) {
        super(task);
        this.taskName = task.getPlanItem().getName();
        this.taskId = task.getPlanItem().getId();
    }

    protected HumanTaskEvent(ValueMap json) {
        super(json);
        this.taskName = readField(json, Fields.taskName);
        this.taskId = readField(json, Fields.taskId);
    }

    protected void writeHumanTaskEvent(JsonGenerator generator) throws IOException {
        super.writeModelEvent(generator);
        writeField(generator, Fields.taskName, taskName);
        writeField(generator, Fields.taskId, taskId);
    }

    /**
     * Get the task id
     * @return id of the task
     */
    public String getTaskId() {
        return taskId;
    }

    /**
     * Get the name of the task
     * @return
     */
    public String getTaskName() {
        return taskName;
    }

    @Override
    final protected void recoverTaskEvent(HumanTask task) {
        recoverHumanTaskEvent(task.getImplementation());
    }

    abstract protected void recoverHumanTaskEvent(WorkflowTask task);
}