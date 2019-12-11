/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.definition.casefile;

import java.util.LinkedHashMap;
import java.util.Map;

import org.cafienne.cmmn.definition.Definition;
import org.cafienne.cmmn.definition.DefinitionsDocument;
import org.w3c.dom.Element;

/**
 * Implementation of CMMN spec 5.1.4
 */
public class CaseFileItemDefinitionDefinition extends Definition {

    private final DefinitionType definitionType;
    private final String structureRef;
    private final String importRef;
    private ImportDefinition importDefinition;
    private final Map<String, PropertyDefinition> properties = new LinkedHashMap<String, PropertyDefinition>();

    public CaseFileItemDefinitionDefinition(Element definitionElement, DefinitionsDocument document) {
        super(definitionElement, document);
        this.definitionType = readDefinitionType();
        this.structureRef = parseAttribute("structureRef", false, "");
        this.importRef = parseAttribute("importRef", false, "");
        parse("property", PropertyDefinition.class, properties);
    }

    private DefinitionType readDefinitionType() {
        String typeName = parseAttribute("definitionType", false, "http://www.omg.org/spec/CMMN/DefinitionType/Unspecified");
        return DefinitionType.resolveDefinitionType(typeName);
    }

    @Override
    protected void resolveReferences() {
        super.resolveReferences();

        if (!importRef.isEmpty()) {
            importDefinition = getDefinition().getDefinitionsDocument().getImportDefinition(importRef);
            if (importDefinition == null) {
                super.addReferenceError("The case file item definition '" + this.getName() + "' refers to an import named " + importRef + ", but that definition is not found");
            }
        }
    }

    public DefinitionType getDefinitionType() {
        return definitionType;
    }

    public String getStructureRef() {
        return structureRef;
    }

    public ImportDefinition getImport() {
        return importDefinition;
    }

    public Map<String, PropertyDefinition> getProperties() {
        return properties;
    }
}