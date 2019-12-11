/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.util;

import java.util.UUID;


public class Guid {
    private final UUID uuid;

    public Guid() {
        uuid = UUID.randomUUID();
    }

    public String toString() {
        return uuid.toString().replace("-", "_");
    }

}