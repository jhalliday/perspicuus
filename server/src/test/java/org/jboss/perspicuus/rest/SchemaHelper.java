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
package org.jboss.perspicuus.rest;

import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;

import java.util.HashMap;
import java.util.Map;

public class SchemaHelper {

    public static Map<String,Object> getSchema() {
        return getSchema(new String[0]);
    }

    public static Map<String,Object> getSchema(String[] fieldnames) {
        Map<String,Object> schemaOuterMap = new HashMap<>();
        SchemaBuilder.FieldAssembler<Schema> fieldAssembler = SchemaBuilder.record("recordname").fields();
        for(String fieldname : fieldnames) {
            fieldAssembler.name(fieldname).type().stringType().noDefault();
        }
        Schema schema = fieldAssembler.endRecord();
        schemaOuterMap.put("schema", schema.toString());
        return schemaOuterMap;
    }
}
