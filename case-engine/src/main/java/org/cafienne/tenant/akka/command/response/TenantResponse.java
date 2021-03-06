package org.cafienne.tenant.akka.command.response;

import org.cafienne.akka.actor.command.response.ModelResponse;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.tenant.akka.command.TenantCommand;

@Manifest
public class TenantResponse extends ModelResponse {
    public TenantResponse(TenantCommand command) {
        super(command);
    }

    public TenantResponse(ValueMap json) {
        super(json);
    }
}
