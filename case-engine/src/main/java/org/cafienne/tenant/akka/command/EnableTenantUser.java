package org.cafienne.tenant.akka.command;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.tenant.TenantActor;
import org.cafienne.tenant.akka.command.TenantCommand;
import org.cafienne.tenant.akka.command.response.TenantResponse;
import org.cafienne.tenant.akka.event.TenantUserEnabled;

import java.io.IOException;

@Manifest
public class EnableTenantUser extends TenantCommand {
    public final String userId;

    private enum Fields {
        userId
    }

    public EnableTenantUser(TenantUser tenantOwner, String tenantId, String userId) {
        super(tenantOwner, tenantId);
        this.userId = userId;
    }

    public EnableTenantUser(ValueMap json) {
        super(json);
        this.userId = readField(json, Fields.userId);
    }

    @Override
    public TenantResponse process(TenantActor tenant) {
        tenant.addEvent(new TenantUserEnabled(tenant, userId)).updateState(tenant);
        return new TenantResponse(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.userId, userId);
    }
}