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

/**
 * The set of Schema dialects that the system knows how to handle.
 *
 * @since 2018-02
 * @author Jonathan Halliday (jonathan.halliday@redhat.com)
 */
public enum SchemaType {

    AVRO(new AvroSchemaParser()),
    JSON_SCHEMA(new JsonSchemaSchemaParser());

    private final SchemaParser schemaParser;

    SchemaType(SchemaParser schemaParser) {
        this.schemaParser = schemaParser;
    }

    public SchemaParser getSchemaParser() {
        return schemaParser;
    }
}
