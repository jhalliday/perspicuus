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
package org.jboss.perspicuus.client;

import org.apache.avro.Schema;

import java.io.IOException;

/**
 * Client for communicating with a remote Schema Registry server.
 * This API extends the base by adding Avro specific methods.
 * These retrieval methods may throw RuntimeExceptions related to type conversion if used with other schema types.
 *
 * @since 2019-03
 * @author Jonathan Halliday (jonathan.halliday@redhat.com)
 */
public class AvroSchemaRegistryClient extends SchemaRegistryClient {

    public AvroSchemaRegistryClient(String serverURL, String username, String password) {
        super(serverURL, username, password);
    }

    /**
     * Return a Schema corresponding to the given id, or null if there isn't one.
     *
     * @param id
     * @return
     * @throws IOException
     */
    public Schema getSchema(int id) throws IOException {
        String schemaString = getStringSchema(id);
        if(schemaString == null) {
            return null;
        }
        Schema avroSchema = new Schema.Parser().parse(schemaString);
        return avroSchema;
    }

    /**
     * Return the schema matching the given one, within the scope of the specified subject, or null if there is no match.
     *
     * @param subject
     * @param schema
     * @return
     * @throws IOException
     */
    public Schema findInSubject(String subject, Schema schema) throws IOException {
        String schemaInputString = schema.toString();
        String schemaResultString = findStringInSubject(subject, schemaInputString);
        if(schemaResultString == null) {
            return null;
        }
        Schema avroSchema = new Schema.Parser().parse(schemaResultString);
        return avroSchema;
    }
}
