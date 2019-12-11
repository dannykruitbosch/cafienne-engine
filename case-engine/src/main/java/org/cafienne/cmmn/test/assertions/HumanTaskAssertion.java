package org.cafienne.cmmn.test.assertions;

import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.cmmn.test.CaseTestCommand;
import org.cafienne.cmmn.test.TestScript;
import org.cafienne.cmmn.test.filter.EventFilter;
import org.cafienne.cmmn.test.humantask.HumanTaskTestCommand;
import org.cafienne.humantask.akka.command.response.HumanTaskValidationResponse;
import org.cafienne.humantask.akka.event.*;
import org.cafienne.humantask.instance.TaskAction;
import org.cafienne.humantask.instance.TaskState;

import java.time.Instant;

public class HumanTaskAssertion extends ModelTestCommandAssertion<CaseTestCommand> {
//    private final String identifier;
//
//    public HumanTaskAssertion(CaseTestCommand testCommand, PlanItemCreated pic) {
//        super(testCommand);
//        this.identifier = pic.getPlanItemId();
//    }

//    HumanTaskAssertion assertType(Class c) {
//        filter("A HumanTask for identifier "+identifier+" has not given any events yet", e -> true);
//        return this;
//    }
////
//    void filter(String msg, EventFilter<HumanTaskTransitioned> filter) {
//        System.out.println("Searchign for HumanTaskTransitioned events");
//        testCommand.getEventListener().waitUntil(msg, HumanTaskTransitioned.class, e -> {
//            if (e.getTaskId().equals(testCommand.getActorId())) {
//                return filter.matches(e);
//            }
//            return false;
//        });
//    }

    public HumanTaskAssertion(CaseTestCommand testCommand) {
        super(testCommand);
    }

    public HumanTaskValidationResponse getValidationResponse() {
        return (HumanTaskValidationResponse) this.testCommand.getActualResponse();
    }

    /**
     * Wait for the task to emit a HumanTaskOutputSaved event with the expected output
     *
     * @return
     */
    public HumanTaskAssertion assertTaskOutput(ValueMap expectedOutput) {
        testCommand.getEvents().filter(HumanTaskOutputSaved.class).filter(e -> e.getTaskOutput().equals(expectedOutput));
        return this;
    }

    /**
     * Asserts task expectedState
     *
     * @param expectedState
     * @return
     */
    public HumanTaskAssertion assertTaskState(TaskState expectedState) {
        testCommand.getEvents().filter(HumanTaskTransitioned.class).filter(e -> e.getCurrentState().equals(expectedState));
        return this;
    }

    /**
     * Asserts task assignee
     *
     * @param expectedAssignee
     * @return
     */
    public HumanTaskAssertion assertAssignee(String expectedAssignee) {
        testCommand.getEvents().filter(HumanTaskAssigned.class).filter(e -> e.assignee.equals(expectedAssignee));
        return this;
    }

    /**
     * Asserts task owner
     *
     * @param expectedOwner
     * @return
     */
    public HumanTaskAssertion assertOwner(String expectedOwner) {
        testCommand.getEvents().filter(HumanTaskOwnerChanged.class).filter(e -> e.owner.equals(expectedOwner));
        return this;
    }

    /**
     * Asserts task due date
     *
     * @param expectedDueDate
     * @return
     */
    public HumanTaskAssertion assertDueDate(Instant expectedDueDate) {
        testCommand.getEvents().filter(HumanTaskDueDateFilled.class).filter(e -> e.dueDate.equals(expectedDueDate));
        return this;
    }

    public HumanTaskAssertion assertTaskCompleted() {
        testCommand.getEvents().filter(HumanTaskCompleted.class).assertNotEmpty();
        return this;
    }
}