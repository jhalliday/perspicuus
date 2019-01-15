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
package org.jboss.perspicuus.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.jboss.perspicuus.parsers.AvroSchemaParser;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Base class with utility code for REST API test cases.
 *
 * @since 2018-01
 * @author Jonathan Halliday (jonathan.halliday@redhat.com)
 */
public abstract class AbstractResourceIT {

    protected final String URL_BASE = "http://localhost:8080";
    protected final String CONTENT_TYPE = "application/vnd.schemaregistry.v1+json";

    protected final ObjectMapper objectMapper = new ObjectMapper();

    protected final Client client = RestClient.client;

    protected int registerSchema(String subject, Map<String,Object> request) throws Exception {
        String schemaString = objectMapper.writeValueAsString(request);
        return registerSchema(subject, schemaString);
    }

    protected int registerSchema(String subject, String envelopeString) throws Exception {
        String result = client.target(URL_BASE+"/subjects/"+subject+"/versions").request(CONTENT_TYPE).post(Entity.json(envelopeString), String.class);
        Map<String,Object> actualResultMap = objectMapper.readValue(result, new TypeReference<Map<String,Object>>() {});
        assertEquals(1, actualResultMap.size());
        assertTrue(actualResultMap.containsKey("id"));
        int id = (Integer)actualResultMap.get("id");
        return id;

    }

    protected void setCompatibilityLevel(String subject, String level) throws Exception {
        Map<String,String> config = new HashMap<>();
        config.put("compatibility", level);
        String result = client.target(URL_BASE+"/config/"+subject).request(CONTENT_TYPE).put(Entity.json(config), String.class);
        Map<String,Object> resultMap = objectMapper.readValue(result, new TypeReference<Map<String,Object>>() {});
        assertEquals(1, resultMap.size());
        assertEquals(level, resultMap.get("compatibility"));
    }

    public String canonicalString(String avroSchema) {
        return new AvroSchemaParser().parseToCanonicalForm(avroSchema).get();
    }

    public Map<String,Object> getAvroSchema() {
        return getAvroSchema(new String[0]);
    }

    public Map<String,Object> getAvroSchema(String[] fieldnames) {
        SchemaBuilder.FieldAssembler<Schema> fieldAssembler = SchemaBuilder.record("recordname").fields();
        for(String fieldname : fieldnames) {
            fieldAssembler.name(fieldname).type().stringType().noDefault();
        }
        Schema schema = fieldAssembler.endRecord();

        Map<String,Object> schemaOuterMap = wrapInEnvelope(schema.toString());
        return schemaOuterMap;
    }

    public Map<String,Object> getJsonSchemaSchema() throws IOException {
        return getJsonSchemaSchema(new String[0]);
    }

    public Map<String,Object> getJsonSchemaSchema(String[] fieldnames) throws IOException {

        Map<String,Object> properties = new HashMap<>();
        for(String fieldname : fieldnames) {
            Map<String,Object> fieldtype = new HashMap<>();
            fieldtype.put("type", "string");
            properties.put(fieldname, fieldtype);
        }
        Map<String,Object> schema = new HashMap<>();
        schema.put("properties", properties);

        Map<String, Object> schemaOuterMap = wrapInEnvelope(objectMapper.writeValueAsString(schema));
        return schemaOuterMap;
    }

    public Map<String,Object> getProtobufSchema(String[] fieldnames) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("//\n");
        stringBuilder.append("syntax = \"proto3\";\n");
        stringBuilder.append("package test;\n\n");
        stringBuilder.append("message TestMessage {\n");
        for(int i = 0; i < fieldnames.length; i++) {
            stringBuilder.append("  string ");
            stringBuilder.append(fieldnames[i]);
            stringBuilder.append(" = ");
            stringBuilder.append(i);
            stringBuilder.append(";\n");
        }
        stringBuilder.append("}\n");

        String schema = stringBuilder.toString();
        Map<String, Object> schemaOuterMap = wrapInEnvelope(schema);
        return schemaOuterMap;
    }

    public Map<String,Object> wrapInEnvelope(String schema) {
        Map<String,Object> schemaOuterMap = new HashMap<>();
        schemaOuterMap.put("schema", schema);
        return schemaOuterMap;
    }
}
