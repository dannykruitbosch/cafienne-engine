/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.akka.command.task;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.ModelActor;
import org.cafienne.akka.actor.command.exception.InvalidCommandException;
import org.cafienne.cmmn.akka.command.CaseCommand;
import org.cafienne.cmmn.akka.command.MakePlanItemTransition;
import org.cafienne.cmmn.akka.command.response.CaseResponse;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.PlanItem;
import org.cafienne.cmmn.instance.Task;
import org.cafienne.cmmn.instance.Transition;
import org.cafienne.cmmn.instance.casefile.ValueMap;

import java.io.IOException;

/**
 * This command can be used to complete a task with additional task output parameters. An alternative is to use the {@link MakePlanItemTransition}
 * with {@link Transition}.Complete. However, that does not allow for passing output parameters, as it is generic across Tasks, Stages, Milestones and
 * Events.
 */
@Manifest
public class CompleteTask extends CaseCommand {
    protected final String taskId;
    protected final ValueMap taskOutput;
    protected Task<?> task;

    private enum Fields {
        taskId, taskOutput
    }

    /**
     * Create a command to transition the plan item with the specified id or name to complete. Note, if only the name is specified, then the command
     * will work on the first plan item within the case having the specified name. If the plan item is not a task or if no plan item can be found, a
     * CommandFailure will be returned.
     *
     * @param child   The child actor that completes, e.g. a HumanTask or a SubCase
     * @param taskOutput       An optional map with named output parameters for the task. These will be set on the task before the task is reported as complete. This
     *                         means that the parameters will also be bound to the case file, which may cause sentries to activate before the task completes.
     */
    public CompleteTask(ModelActor child, ValueMap taskOutput) {
        super(child.getCurrentUser(), child.getParentActorId());
        this.taskId = child.getId();
        this.taskOutput = taskOutput;
    }

    public CompleteTask(ValueMap json) {
        super(json);
        this.taskId = readField(json, Fields.taskId);
        this.taskOutput = readMap(json, Fields.taskOutput);
    }

    @Override
    public void validate(Case caseInstance) {
        super.validate(caseInstance);

        if (taskId == null || taskId.trim().isEmpty()) {
            throw new InvalidCommandException("Invalid or missing task id");
        }
        PlanItem planItem = getPlanItem(caseInstance);
        if (planItem == null) {
            throw new InvalidCommandException("Invalid or missing task id");
        }

        if (!(planItem.getInstance() instanceof Task<?>)) {
            throw new InvalidCommandException("Invalid or missing task id");
        }

        // Set the task pointer.
        task = planItem.getInstance();

//        if (task instanceof HumanTask) {
//            HumanTask task = planItem.getInstance();
//            State currentState = task.getPlanItem().getState();
//            if (currentState != State.Active) {
//                throw new InvalidCommandException("CompleteTask: Action can not be completed as the task (" + taskId + ") is not in Active but in " + currentState + " state");
//            }
//
//            String currentTaskAssignee = task.getWorkflowInteraction().getTaskAssignee();
//            if (currentTaskAssignee == null || currentTaskAssignee.trim().isEmpty()) {
//                throw new InvalidCommandException("CompleteHumanTask: Only Assigned or Delegated task can be completed");
//            }
//
//            String userId = getUser().id();
//            if (!userId.equals(currentTaskAssignee)) {
//                throw new InvalidCommandException("CompleteTask: Only the current task assignee (" + currentTaskAssignee + ") can complete the task (" + task.getId() + ")");
//            }
//
//            TaskState currentTaskState = task.getWorkflowInteraction().getCurrentTaskState();
//            if (!(currentTaskState == TaskState.Assigned || currentTaskState == TaskState.Delegated)) {
//                throw new InvalidCommandException("CompleteHumanTask: Action can not be completed as the task (" + taskId + ") is in " + currentTaskState + " state, but should be in any of ["
//                        + TaskState.Assigned + ", " + TaskState.Delegated + "] state");
//            }
//        } else {
//            // TODO: Add validation for ProcessTask & CaseTask
//        }
    }

    @Override
    public CaseResponse process(Case caseInstance) {
        task.goComplete(taskOutput);
        return new CaseResponse(this);
    }

    private PlanItem getPlanItem(Case caseInstance) {
        return caseInstance.getPlanItemById(taskId);
    }

    @Override
    public String toString() {
        String taskName = task != null ? task.getPlanItem().getName() +" with id "+taskId : taskId +" (unknown name)";
        return "CompleteTask "+taskName + " with output\n" + taskOutput;
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.taskId, taskId);
        writeField(generator, Fields.taskOutput, taskOutput);
    }
}