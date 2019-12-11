/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.test.basic;

import org.cafienne.cmmn.akka.command.MakeCaseTransition;
import org.cafienne.cmmn.akka.command.MakePlanItemTransition;
import org.cafienne.cmmn.akka.command.StartCase;
import org.cafienne.cmmn.definition.CaseDefinition;
import org.cafienne.cmmn.instance.State;
import org.cafienne.cmmn.instance.Transition;
import org.cafienne.cmmn.test.TestScript;
import org.cafienne.cmmn.test.assertions.CaseAssertion;
import org.cafienne.cmmn.test.assertions.PlanItemAssertion;
import org.cafienne.cmmn.test.assertions.StageAssertion;
import org.cafienne.akka.actor.identity.TenantUser;
import org.junit.Test;

public class Simple {
    private final CaseDefinition definitions = TestScript.getCaseDefinition("testdefinition/simple.xml");
    private final TenantUser testUser = TestScript.getTestUser("Anonymous");

    @Test
    public void testSimple() {
        String caseInstanceId = "Simple";
        TestScript testCase = new TestScript(caseInstanceId);

        StartCase startCase = new StartCase(testUser, caseInstanceId, definitions, null, null);
        testCase.addTestStep(startCase, action -> {
            CaseAssertion casePlan = new CaseAssertion(action);
            casePlan.assertLastTransition(Transition.Create, State.Active, State.Null);
            StageAssertion stage1 = casePlan.assertStage("Stage1");
            stage1.assertLastTransition(Transition.Start, State.Active, State.Available);
            PlanItemAssertion item1dot1 = stage1.assertPlanItem("Item1.1");
            item1dot1.assertLastTransition(Transition.Start, State.Active, State.Available);
            PlanItemAssertion item1 = casePlan.assertPlanItem("Item1");
            item1.assertLastTransition(Transition.Start, State.Active, State.Available);
        });

        // Completing Item1 should make it go to state Completed, others remain in same state
        testCase.addTestStep(new MakePlanItemTransition(testUser, caseInstanceId, null, Transition.Complete, "Item1"), action -> {
            CaseAssertion casePlan = new CaseAssertion(action);
            casePlan.assertLastTransition(Transition.Create, State.Active, State.Null);
            StageAssertion stage1 = casePlan.assertStage("Stage1");
            stage1.assertLastTransition(Transition.Start, State.Active, State.Available);
            PlanItemAssertion item1dot1 = stage1.assertPlanItem("Item1.1");
            item1dot1.assertLastTransition(Transition.Start, State.Active, State.Available);
            PlanItemAssertion item1 = casePlan.assertPlanItem("Item1");
            item1.assertLastTransition(Transition.Complete, State.Completed, State.Active);
        });
        
        // Completing Item1 again should not change state.
        testCase.addTestStep(new MakePlanItemTransition(testUser, caseInstanceId, null, Transition.Complete, "Item1"), action -> {
            CaseAssertion casePlan = new CaseAssertion(action);
            casePlan.assertLastTransition(Transition.Create, State.Active, State.Null);
            StageAssertion stage1 = casePlan.assertStage("Stage1");
            stage1.assertLastTransition(Transition.Start, State.Active, State.Available);
            PlanItemAssertion item1dot1 = stage1.assertPlanItem("Item1.1");
            item1dot1.assertLastTransition(Transition.Start, State.Active, State.Available);
            PlanItemAssertion item1 = casePlan.assertPlanItem("Item1");
            item1.assertLastTransition(Transition.Complete, State.Completed, State.Active);
        });
        
        // Suspend the whole case.
        testCase.addTestStep(new MakeCaseTransition(testUser, caseInstanceId, Transition.Suspend), action -> {
            CaseAssertion casePlan = new CaseAssertion(action);
            casePlan.assertLastTransition(Transition.Suspend, State.Suspended, State.Active);
            StageAssertion stage1 = casePlan.assertStage("Stage1");
            stage1.assertLastTransition(Transition.ParentSuspend, State.Suspended, State.Active);
            PlanItemAssertion item1dot1 = stage1.assertPlanItem("Item1.1");
            item1dot1.assertLastTransition(Transition.ParentSuspend, State.Suspended, State.Active);
            PlanItemAssertion item1 = casePlan.assertPlanItem("Item1");
            item1.assertLastTransition(Transition.Complete, State.Completed, State.Active);
        });
        
        
        // And re-activate it again
        testCase.addTestStep(new MakeCaseTransition(testUser, caseInstanceId, Transition.Reactivate), action -> {
            CaseAssertion casePlan = new CaseAssertion(action);
            casePlan.assertLastTransition(Transition.Reactivate, State.Active, State.Suspended);
            StageAssertion stage1 = casePlan.assertStage("Stage1");
            stage1.assertLastTransition(Transition.ParentResume, State.Active, State.Suspended);
            PlanItemAssertion item1dot1 = stage1.assertPlanItem("Item1.1");
            item1dot1.assertLastTransition(Transition.ParentResume, State.Active, State.Suspended);
            PlanItemAssertion item1 = casePlan.assertPlanItem("Item1");
            item1.assertLastTransition(Transition.Complete, State.Completed, State.Active);
        });        

        // Completing Item1.1 should make it go to state Completed, others remain in same state, causing completion check of surrounding stage
        testCase.addTestStep(new MakePlanItemTransition(testUser, caseInstanceId, null, Transition.Complete, "Item1.1"), action -> {
            CaseAssertion casePlan = new CaseAssertion(action);
            casePlan.assertLastTransition(Transition.Complete, State.Completed, State.Active);
            StageAssertion stage1 = casePlan.assertStage("Stage1");
            stage1.assertLastTransition(Transition.Complete, State.Completed, State.Active);
            PlanItemAssertion item1dot1 = stage1.assertPlanItem("Item1.1");
            item1dot1.assertLastTransition(Transition.Complete, State.Completed, State.Active);
            PlanItemAssertion item1 = casePlan.assertPlanItem("Item1");
            item1.assertLastTransition(Transition.Complete, State.Completed, State.Active);
        });
        
        // Complete the whole case
        testCase.addTestStep(new MakeCaseTransition(testUser, caseInstanceId, Transition.Complete), action -> {
            CaseAssertion casePlan = new CaseAssertion(action);
            casePlan.assertLastTransition(Transition.Complete, State.Completed, State.Active);
            StageAssertion stage1 = casePlan.assertStage("Stage1");
            stage1.assertLastTransition(Transition.Complete, State.Completed, State.Active);
            PlanItemAssertion item1dot1 = stage1.assertPlanItem("Item1.1");
            item1dot1.assertLastTransition(Transition.Complete, State.Completed, State.Active);
            PlanItemAssertion item1 = casePlan.assertPlanItem("Item1");
            item1.assertLastTransition(Transition.Complete, State.Completed, State.Active);
        }); 
        
        testCase.runTest();
    }
    
}