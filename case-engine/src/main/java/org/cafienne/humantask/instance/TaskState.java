/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.humantask.instance;

public enum TaskState {
    Null, Unassigned, Assigned, Delegated, Completed, Suspended, Terminated;

    @Override
    public String toString() {
        return name();
    }

    boolean isSemiTerminal() {
        return this == Completed || this == Terminated || this == Suspended;
    }
}