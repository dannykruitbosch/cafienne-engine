/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.platform.akka.response;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.serialization.Fields;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.platform.akka.command.PlatformCommand;

import java.io.IOException;

/**
 */
@Manifest
public class PlatformUpdateStatus extends PlatformResponse {
    private final ValueMap map;

    public PlatformUpdateStatus(PlatformCommand command, ValueMap map) {
        super(command);
        this.map = map;
    }

    public PlatformUpdateStatus(ValueMap json) {
        super(json);
        map = json.with(Fields.update);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.update, map);
    }

    public ValueMap getValue() {
        return map;
    }

}
