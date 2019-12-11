/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.instance.task.process;

import org.cafienne.cmmn.definition.ProcessTaskDefinition;
import org.cafienne.cmmn.instance.PlanItem;
import org.cafienne.cmmn.instance.State;
import org.cafienne.cmmn.instance.Task;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.processtask.akka.command.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessTask extends Task<ProcessTaskDefinition> {
    private final static Logger logger = LoggerFactory.getLogger(ProcessTask.class);

    public ProcessTask(PlanItem planItem, ProcessTaskDefinition definition) {
        super(planItem, definition);
    }

    @Override
    protected void startImplementation(ValueMap inputParameters) {
        final String taskId = this.getId();
        final TenantUser user = getCaseInstance().getCurrentUser();
        final String taskName = this.getPlanItem().getName();
        final String rootActorId = this.getCaseInstance().getRootActorId();
        final String parentId = this.getCaseInstance().getId();
        final boolean debugMode = this.getCaseInstance().debugMode();

        getCaseInstance().askProcess(new StartProcess(user, taskId, taskName, getDefinition().getImplementationDefinition(), inputParameters, parentId, rootActorId, debugMode),
            left -> goFault(new ValueMap("exception", left.toJson())),
            right -> {
                if (!this.getDefinition().isBlocking()) {
                    goComplete(new ValueMap());
                }
            });
    }

    @Override
    protected void createInstance() {
    }

    @Override
    protected void suspendInstance() {
        tell(new SuspendProcess(getCaseInstance().getCurrentUser(), getId()));
    }

    @Override
    protected void resumeInstance() {
        tell(new ResumeProcess(getCaseInstance().getCurrentUser(), getId()));
    }

    @Override
    protected void terminateInstance() {
        if (getPlanItem().getHistoryState() == State.Available) {
            addDebugInfo(() -> "Terminating process task '"+getPlanItem().getName()+"' without it being started; no need to inform the task actor");
        } else {
            tell(new TerminateProcess(getCaseInstance().getCurrentUser(), getId()));
        }
    }

    @Override
    protected void reactivateImplementation(ValueMap inputParameters) {
        // Apparently process has failed and we're trying again?
        startImplementation(inputParameters);
    }

    private void tell(ProcessCommand command) {
        if (!this.getDefinition().isBlocking()) {
            return;
        }
        getCaseInstance().askProcess(command,
            left -> goFault(new ValueMap("exception", left.toJson())));
    }
}