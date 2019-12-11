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
import org.cafienne.humantask.instance.WorkflowTask;

import java.io.IOException;
import java.time.Instant;

@Manifest
public class HumanTaskDueDateFilled extends HumanTaskEvent {
    public final Instant dueDate;

    public enum Fields {
        dueDate
    }

    public HumanTaskDueDateFilled(HumanTask task, Instant dueDate) {
        super(task);
        this.dueDate = dueDate;
    }

    public HumanTaskDueDateFilled(ValueMap json) {
        super(json);
        this.dueDate = json.rawInstant(Fields.dueDate);
    }

    public void updateState(WorkflowTask task) {
        task.setDueDate(dueDate);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeHumanTaskEvent(generator);
        writeField(generator, Fields.dueDate, dueDate);
    }

    protected void recoverHumanTaskEvent(WorkflowTask task) {
        updateState(task);
    }
}