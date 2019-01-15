/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.perspicuus.parsers;

import com.squareup.wire.schema.Location;
import com.squareup.wire.schema.internal.parser.*;

import java.util.List;
import java.util.Optional;

/**
 * Schema parsing functions for Google's Protocol Buffers schema.
 *
 * @since 2019-01
 * @author Jonathan Halliday (jonathan.halliday@redhat.com)
 */
public class ProtobufSchemaParser implements SchemaParser {

    @Override
    public Optional<String> parseToCanonicalForm(String rawSchema) {

        ProtoFileElement element;
        try {
            element = ProtoParser.parse(Location.get(""), rawSchema);
        } catch (Exception e) {
            return Optional.empty();
        }
        String output = toSchema(element); // element.toSchema();

        return Optional.of(output);
    }

    // wire's ProtoFileElement.toSchema is broken in 2.2.0, fixed in 2.3.0
    // but that's not released yet, so for now we have our own version copied from it...
    private String toSchema(ProtoFileElement protoFileElement) {
        StringBuilder builder = new StringBuilder();
        builder.append("//").append(protoFileElement.location()).append('\n');
        if (protoFileElement.syntax() != null) {
            builder.append("syntax = \"").append(protoFileElement.syntax()).append("\";\n");
        }
        if (protoFileElement.packageName() != null) {
            builder.append("package ").append(protoFileElement.packageName()).append(";\n");
        }
        if (!protoFileElement.imports().isEmpty() || !protoFileElement.publicImports().isEmpty()) {
            builder.append('\n');
            for (String file : protoFileElement.imports()) {
                builder.append("import \"").append(file).append("\";\n");
            }
            for (String file : protoFileElement.publicImports()) {
                builder.append("import public \"").append(file).append("\";\n");
            }
        }
        if (!protoFileElement.options().isEmpty()) {
            builder.append('\n');
            for (OptionElement option : protoFileElement.options()) {
                builder.append(option.toSchemaDeclaration());
            }
        }
        if (!protoFileElement.types().isEmpty()) {
            builder.append('\n');
            for (TypeElement typeElement : protoFileElement.types()) {
                builder.append(typeElement.toSchema());
            }
        }
        if (!protoFileElement.extendDeclarations().isEmpty()) {
            builder.append('\n');
            for (ExtendElement extendDeclaration : protoFileElement.extendDeclarations()) {
                builder.append(extendDeclaration.toSchema());
            }
        }
        if (!protoFileElement.services().isEmpty()) {
            builder.append('\n');
            for (ServiceElement service : protoFileElement.services()) {
                builder.append(service.toSchema());
            }
        }
        return builder.toString();
    }

    @Override
    public boolean isCompatibleWith(String compatibilityLevel, List<String> existingSchemas, String proposedSchema) {
        return false;
    }
}
