/*
 * Copyright 2018, 2019 Red Hat, Inc. and/or its affiliates.
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

import org.apache.avro.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Schema parsing functions for Avro schema.
 *
 * @since 2018-02
 * @author Jonathan Halliday (jonathan.halliday@redhat.com)
 */
public class AvroSchemaParser implements SchemaParser {

    @Override
    public Optional<String> parseToCanonicalForm(String rawSchema) {
        try {
            Schema avroSchema = new Schema.Parser().parse(rawSchema);
            String reconstitutedCanonicalForm = avroSchema.toString();
            return Optional.of(reconstitutedCanonicalForm);
        } catch(SchemaParseException e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean isCompatibleWith(String compatibilityLevel, List<String> existingSchemaStrings, String proposedSchemaString) {

        SchemaValidator schemaValidator = validatorFor(compatibilityLevel);

        if(schemaValidator == null) {
            return true;
        }

        List<Schema> existingSchemas = existingSchemaStrings.stream().map(s -> new Schema.Parser().parse(s)).collect(Collectors.toList());
        Collections.reverse(existingSchemas); // the most recent must come first, i.e. reverse-chronological.
        Schema toValidate = new Schema.Parser().parse(proposedSchemaString);

        try {
            schemaValidator.validate(toValidate, existingSchemas);
            return true;
        } catch (SchemaValidationException e) {
            return false;
        }
    }

    private SchemaValidator validatorFor(String compatibilityLevel) {
        switch (compatibilityLevel) {
            case "BACKWARD":
                return new SchemaValidatorBuilder().canReadStrategy().validateLatest();
            case "BACKWARD_TRANSITIVE":
                return new SchemaValidatorBuilder().canReadStrategy().validateAll();
            case "FORWARD":
                return new SchemaValidatorBuilder().canBeReadStrategy().validateLatest();
            case "FORWARD_TRANSITIVE":
                return new SchemaValidatorBuilder().canBeReadStrategy().validateAll();
            case "FULL":
                return new SchemaValidatorBuilder().mutualReadStrategy().validateLatest();
            case "FULL_TRANSITIVE":
                return new SchemaValidatorBuilder().mutualReadStrategy().validateAll();
            default:
                return null;
        }

    }
}
