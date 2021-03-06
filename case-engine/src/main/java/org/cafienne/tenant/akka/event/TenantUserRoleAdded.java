package org.cafienne.tenant.akka.event;

import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.tenant.TenantActor;
import org.cafienne.tenant.User;

@Manifest
public class TenantUserRoleAdded extends TenantUserRoleEvent {
    public TenantUserRoleAdded(TenantActor tenant, String userId, String role) {
        super(tenant, userId, role);
    }

    public TenantUserRoleAdded(ValueMap json) {
        super(json);
    }

    @Override
    protected void updateUserState(User user) {
        user.updateState(this);
    }
}
