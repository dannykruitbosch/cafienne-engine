/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.definition.parameter;

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.ModelDefinition;
import org.cafienne.cmmn.definition.casefile.CaseFileError;
import org.cafienne.cmmn.definition.casefile.CaseFileItemDefinition;
import org.cafienne.akka.actor.serialization.json.Value;
import org.w3c.dom.Element;

public class InputParameterDefinition extends ParameterDefinition {
    public InputParameterDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
    }

    public void validate(Value value) throws CaseFileError {
        CaseFileItemDefinition binding = getBinding();
        if (binding != null) {
            binding.validate(value);
        }
    }
}
