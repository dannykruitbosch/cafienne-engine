package org.cafienne.cmmn.test.expression;


import org.cafienne.cmmn.akka.command.StartCase;
import org.cafienne.cmmn.akka.event.PlanItemTransitioned;
import org.cafienne.cmmn.akka.event.RepetitionRuleEvaluated;
import org.cafienne.cmmn.definition.CaseDefinition;
import org.cafienne.cmmn.instance.PlanItemEvent;
import org.cafienne.cmmn.instance.State;
import org.cafienne.cmmn.instance.Transition;
import org.cafienne.cmmn.instance.casefile.Value;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.cmmn.test.TestScript;
import org.cafienne.cmmn.test.assertions.CaseAssertion;
import org.cafienne.cmmn.test.assertions.FailureAssertion;
import org.cafienne.cmmn.test.assertions.HumanTaskAssertion;
import org.cafienne.cmmn.test.assertions.event.TaskOutputAssertion;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.humantask.akka.command.CompleteHumanTask;
import org.junit.Test;

public class VariousSpelExpressions {
    private final CaseDefinition xml = TestScript.getCaseDefinition("testdefinition/expression/spelexpressions.xml");

    private final String caseInstanceId = "SpelExpressionsTest";
    private final String input = "basic";
    private final String other = "other input";
    private final String defaultOutput = "My Output";
    private final String stopNowOutput = "stop now";
    private final String taskName = "HumanTask";
    private final String inputParameterName = "InputContent";
    private final ValueMap basicInput = new ValueMap(inputParameterName, input);
    private final ValueMap otherInput = new ValueMap(inputParameterName, other);
    private final ValueMap defaultTaskOutput = new ValueMap("Output", defaultOutput);
    private final ValueMap stopNowTaskOutput = new ValueMap("Output", stopNowOutput);
    private final ValueMap specialTaskOutput = new ValueMap("SpecialOutput", new ValueMap("Multi", new int[]{1, 2, 3, 4}));
    private final TenantUser user = TestScript.getTestUser("user");

    @Test
    public void testHumanTaskExpressions() {
        TestScript testCase = new TestScript("expressions");
        testCase.addTestStep(new StartCase(user, caseInstanceId, xml, basicInput, null), caseStarted -> {
            String taskId = testCase.getEventListener().awaitPlanItemState("HumanTask", State.Active).getPlanItemId();

            CaseAssertion cp = new CaseAssertion(caseStarted);
            TestScript.debugMessage("Start of case: " + cp);

            // Now complete the HumanTask with the default output, and validate the task output filled event
            testCase.addTestStep(new CompleteHumanTask(user, caseInstanceId, taskId, defaultTaskOutput.cloneValueNode()), taskCompleted -> {
                HumanTaskAssertion casePlan = new HumanTaskAssertion(taskCompleted);
                TestScript.debugMessage("Current case: " + casePlan);

                testCase.getEventListener().awaitTaskOutputFilled(taskName, taskEvent -> {
                    TaskOutputAssertion toa = new TaskOutputAssertion(taskEvent);
                    toa.assertValue("CaseID", caseInstanceId);
                    toa.assertValue("TaskName", taskName);
                    toa.assertValue("TaskOutput", defaultOutput);
                    Value v = toa.getValue("MultiOutput");
//                    System.out.println("Value of special output: " + v);
                    return true;
                });

                testCase.getEventListener().awaitPlanItemEvent("HumanTask", PlanItemTransitioned.class, e -> e.getTransition().equals(Transition.Complete));
            });
        });

        testCase.runTest();
    }

    @Test
    public void testMilestoneTerminationOnDifferentInput() {
        TestScript testCase = new TestScript("expressions");
        // Using "other" input should immediately make the Milestone occur, and also have the HumanTask end up terminated, because the stage is terminated
        testCase.addTestStep(new StartCase(user, caseInstanceId, xml, otherInput, null), caseStarted -> {
            CaseAssertion cp = new CaseAssertion(caseStarted);
            TestScript.debugMessage("Start of case: " + cp);

            // Milestone must have occured, causing stage and task to be terminated
            testCase.getEventListener().awaitPlanItemState("HumanTask", State.Terminated);
            testCase.getEventListener().awaitPlanItemState("Milestone", State.Completed);
        });

        testCase.runTest();
    }

    @Test
    public void testMilestoneDrivenTerminationOnTaskInstanceLimit() {
        TestScript testCase = new TestScript("expressions");
        testCase.addTestStep(new StartCase(user, caseInstanceId, xml, basicInput, null), act -> {
            CaseAssertion cp = new CaseAssertion(act);
            TestScript.debugMessage("Current case: " + cp);

            // Now await the first HumanTask to become active, and then complete it, with the default task output;
            //  This ought to result in a new HumanTask, which we will also complete.
            String taskId = testCase.getEventListener().awaitPlanItemState("HumanTask", State.Active).getPlanItemId();

            testCase.addTestStep(new CompleteHumanTask(user, caseInstanceId, taskId, defaultTaskOutput.cloneValueNode()), action -> {
                HumanTaskAssertion assertion = new HumanTaskAssertion(action);

                // Validate output again. Does not add too much value, as this is also done in the test above
                testCase.getEventListener().awaitTaskOutputFilled(taskName, taskEvent -> {
                    TaskOutputAssertion toa = new TaskOutputAssertion(taskEvent);
                    toa.assertValue("CaseID", caseInstanceId);
                    toa.assertValue("TaskName", taskName);
                    toa.assertValue("TaskOutput", defaultOutput);
                    return true;
                });

                // Determine the id of the newly available HumanTask, so that we can complete that one too.
                String nextTaskId = testCase.getEventListener().awaitPlanItemTransitioned("HumanTask", e ->
                        !e.getPlanItemId().equals(taskId) && e.getCurrentState().equals(State.Active)).getPlanItemId();

                // Print the case...
//                TestScript.debugMessage("Current case: " + new CaseAssertion(action));

                testCase.addTestStep(new CompleteHumanTask(user, caseInstanceId, nextTaskId, defaultTaskOutput.cloneValueNode()), result -> {
//                    TestScript.debugMessage("Current case: " + new CaseAssertion(result));
                    // Await completion of 'nextTaskId'
                    testCase.getEventListener().awaitPlanItemState(nextTaskId, State.Completed);
                    // There must be yet another HumanTask, but it must be in state terminated, and also it may not repeat
                    String lastTaskId = testCase.getEventListener().awaitPlanItemTransitioned("HumanTask",
                        e -> (!(e.getPlanItemId().equals(taskId) || e.getPlanItemId().equals(nextTaskId)) && e.getCurrentState().equals(State.Terminated))).getPlanItemId();
                    // Last task must not repeat
                    testCase.getEventListener().awaitPlanItemEvent(lastTaskId, RepetitionRuleEvaluated.class, e -> !e.isRepeating());
                });
            });
        });

        testCase.runTest();
    }

    @Test
    public void testMilestoneDrivenTerminationOnTaskOutputContent() {
        TestScript testCase = new TestScript("expressions");
        testCase.addTestStep(new StartCase(user, caseInstanceId, xml, basicInput, null), act -> {
            // Get the id of the first "HumanTask" in the case. It must be in state Active
            String taskId = testCase.getEventListener().awaitPlanItemState("HumanTask", State.Active).getPlanItemId();
            TestScript.debugMessage("Current case: " + new CaseAssertion(act));
            // Now complete that task with the "stop now" output; this should make the milestone Occur, which must Terminate the Stage.
            testCase.addTestStep(new CompleteHumanTask(user, caseInstanceId, taskId, stopNowTaskOutput.cloneValueNode()), action -> {
                // Validate the output; it must be 'stop now'
                testCase.getEventListener().awaitTaskOutputFilled(taskName, taskEvent -> {
                    TaskOutputAssertion toa = new TaskOutputAssertion(taskEvent);
                    toa.assertValue("CaseID", caseInstanceId);
                    toa.assertValue("TaskName", taskName);
                    toa.assertValue("TaskOutput", stopNowOutput);
                    return true;
                });

                // Now fetch the next task id
                String nextTaskId = testCase.getEventListener().awaitPlanItemTransitioned("HumanTask", e -> !e.getPlanItemId().equals(taskId) && e.getCurrentState().equals(State.Active)).getPlanItemId();

//                CaseAssertion casePlan = new CaseAssertion(action);
//                TestScript.debugMessage("Current case: " + casePlan);

                testCase.addTestStep(new CompleteHumanTask(user, caseInstanceId, nextTaskId, defaultTaskOutput.cloneValueNode()), result -> {
                    TestScript.debugMessage("Current case: " + new FailureAssertion(result));
                    // Last HumanTask must be terminated
                    testCase.getEventListener().awaitPlanItemState(nextTaskId, State.Terminated);
                });
            });
        });

        testCase.runTest();
    }


    @Test
    public void testMilestoneDrivenTerminationOnTaskMultiOutputContent() {
        TestScript testCase = new TestScript("expressions");
        testCase.addTestStep(new StartCase(user, caseInstanceId, xml, basicInput, null), act -> {
            // Get the id of the first "HumanTask" in the case. It must be in state Active
            String taskId = testCase.getEventListener().awaitPlanItemState("HumanTask", State.Active).getPlanItemId();

            TestScript.debugMessage("Current case: " + new CaseAssertion(act));
            // Now complete that task with the "stop now" output; this should make the milestone Occur, which must Terminate the Stage.
            testCase.addTestStep(new CompleteHumanTask(user, caseInstanceId, taskId, specialTaskOutput.cloneValueNode()), action -> {
                // Validate the output; it must be 'stop now'
                testCase.getEventListener().awaitTaskOutputFilled(taskName, taskEvent -> {
                    TaskOutputAssertion toa = new TaskOutputAssertion(taskEvent);
                    toa.assertValue("CaseID", caseInstanceId);
                    toa.assertValue("TaskName", taskName);
//                    System.out.println("Value of special output: " + toa.getValue("MultiOutput"));
                    return true;
                });

                // Now fetch the next task id
                String nextTaskId = testCase.getEventListener().awaitPlanItemEvent("HumanTask", PlanItemEvent.class, e -> !e.getPlanItemId().equals(taskId)).getPlanItemId();

//                CaseAssertion casePlan = new CaseAssertion(action);
//                TestScript.debugMessage("Current case: " + casePlan);

                testCase.addTestStep(new CompleteHumanTask(user, caseInstanceId, nextTaskId, defaultTaskOutput.cloneValueNode()), result -> {
//                    TestScript.debugMessage("Current case: " + new CaseAssertion(result));
                    // Last HumanTask must be terminated
                    testCase.getEventListener().awaitPlanItemState(nextTaskId, State.Completed);
                });
            });
        });

        testCase.runTest();
    }
}