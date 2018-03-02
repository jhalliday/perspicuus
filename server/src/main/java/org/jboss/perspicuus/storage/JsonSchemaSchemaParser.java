/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
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
package org.jboss.perspicuus.storage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * Schema parsing functions for JsonSchema.
 *
 * @since 2018-02
 * @author Jonathan Halliday (jonathan.halliday@redhat.com)
 */
public class JsonSchemaSchemaParser implements SchemaParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Optional<String> parseToCanonicalForm(String rawSchema) {
        try {

            JsonNode node = objectMapper.readTree(rawSchema);

            JsonSchemaFactory factory = JsonSchemaFactory.getInstance();

            // force a parse - we don't need the return value, but use the process
            // to implicitly validate the correctness of the schema structure.
            JsonSchema jsonSchema = factory.getSchema(node);

            String reconstitutedCanonicalForm = objectMapper.writeValueAsString(node);
            return Optional.of(reconstitutedCanonicalForm);

        } catch(IOException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<String> tokenizeForSearch(String schema) {

        List<String> results = new ArrayList<>();

        try {

            JsonNode node = objectMapper.readTree(schema);

            ObjectNode objectNode = (ObjectNode)node.get("properties");
            if(objectNode != null) {
                Iterator<String> iter = objectNode.fieldNames();
                while(iter.hasNext()) {
                    results.add(iter.next());
                }
            }

        } catch(IOException e) {
            // should be rare, since this code is only called after the same schema has already been parsed elsewhere.
            throw new RuntimeException("failed to parse json-schema: "+schema);
        }

        return results;
    }

    public boolean isCompatibleWith(String compatibilityLevel, String existingSchema, String proposedSchema) {
        return existingSchema.equals(proposedSchema);
    }
}
