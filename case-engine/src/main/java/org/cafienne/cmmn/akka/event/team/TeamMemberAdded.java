package org.cafienne.cmmn.akka.event.team;

import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.cmmn.instance.team.Member;

/**
 * Event caused when a userId is added to the case team.
 */
@Manifest
public class TeamMemberAdded extends DeprecatedCaseTeamEvent {
    public TeamMemberAdded(Case caseInstance, Member member) {
        super(caseInstance, member);
    }

    public TeamMemberAdded(ValueMap json) {
        super(json);
    }

    @Override
    public void updateState(Case actor) {
        actor.getCaseTeam().updateState(this);
    }
}
