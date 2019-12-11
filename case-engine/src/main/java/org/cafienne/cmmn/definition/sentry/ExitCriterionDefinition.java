/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.definition.sentry;

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.Definition;
import org.cafienne.cmmn.instance.sentry.ExitCriterion;
import org.cafienne.cmmn.instance.Stage;
import org.cafienne.cmmn.instance.Transition;
import org.w3c.dom.Element;

public class ExitCriterionDefinition extends CriterionDefinition {
    public ExitCriterionDefinition(Element element, Definition definition, CMMNElementDefinition parentElement) {
        super(element, definition, parentElement);
    }

    @Override
    public Transition getTransition() {
        return Transition.Exit;
    }

    public ExitCriterion createInstance(Stage<?> stage) {
        return new ExitCriterion(stage, this);
    }
}