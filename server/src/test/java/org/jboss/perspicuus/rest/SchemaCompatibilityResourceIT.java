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

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test cases for schema compatibility API.
 *
 * @since 2018-01
 * @author Jonathan Halliday (jonathan.halliday@redhat.com)
 */
public class SchemaCompatibilityResourceIT extends AbstractResourceIT {

    @Test
    public void testGlobalConfig() throws Exception {

        String result = client.target(URL_BASE+"/config").request(CONTENT_TYPE).get(String.class);
        Map<String,Object> resultMap = objectMapper.readValue(result, new TypeReference<Map<String,Object>>() {});
        assertEquals(1, resultMap.size());
        assertEquals("NONE", resultMap.get("compatibilityLevel"));

        Map<String,String> config = new HashMap<>();
        config.put("compatibility", "BACKWARD");
        result = client.target(URL_BASE+"/config").request(CONTENT_TYPE).put(Entity.json(config), String.class);
        resultMap = objectMapper.readValue(result, new TypeReference<Map<String,Object>>() {});
        assertEquals(1, resultMap.size());
        assertEquals("BACKWARD", resultMap.get("compatibility"));

        result = client.target(URL_BASE+"/config").request(CONTENT_TYPE).get(String.class);
        resultMap = objectMapper.readValue(result, new TypeReference<Map<String,Object>>() {});
        assertEquals(1, resultMap.size());
        assertEquals("BACKWARD", resultMap.get("compatibilityLevel"));
    }

    @Test
    public void testSubjectConfig() throws Exception {

        String subject = "compatibilityconfigsubject";

        Map<String,String> config = new HashMap<>();
        config.put("compatibility", "BACKWARD");
        String result = client.target(URL_BASE+"/config/"+subject).request(CONTENT_TYPE).put(Entity.json(config), String.class);
        Map<String,Object> resultMap = objectMapper.readValue(result, new TypeReference<Map<String,Object>>() {});
        assertEquals(1, resultMap.size());
        assertEquals("BACKWARD", resultMap.get("compatibility"));
    }

    @Test
    public void testDefaultCompatibility() throws Exception {

        String subject = "compatibilitysubject";

        Map<String,Object> schema = getAvroSchema();

        int schemaId = registerSchema(subject, schema);

        String schemaString = objectMapper.writeValueAsString(schema);

        String result = client.target(URL_BASE+"/compatibility/subjects/"+subject+"/versions/1").request(CONTENT_TYPE).post(Entity.json(schemaString), String.class);

        Map<String,Object> actualResultMap = objectMapper.readValue(result, new TypeReference<Map<String,Object>>() {});
        Boolean isCompatible = (Boolean)actualResultMap.get("is_compatible");

        assertTrue(isCompatible);

        schema.clear();
        Schema schemaObject = SchemaBuilder.record("recordname").fields().name("fieldname").type().intType().noDefault().endRecord();
        schema.put("schema", schemaObject.toString());
        schemaString = objectMapper.writeValueAsString(schema);

        result = client.target(URL_BASE+"/compatibility/subjects/"+subject+"/versions/1").request(CONTENT_TYPE).post(Entity.json(schemaString), String.class);

        actualResultMap = objectMapper.readValue(result, new TypeReference<Map<String,Object>>() {});
        isCompatible = (Boolean)actualResultMap.get("is_compatible");

        assertFalse(isCompatible);
    }

    @Test
    public void testBackwardCompatibility() throws Exception {

        String subject = "backwardcompatibilitysubject";

        Map<String,Object> schema = new HashMap<>();
        Schema schemaObject = SchemaBuilder.record("recordname").fields().name("fieldA").type().intType().noDefault().endRecord();
        schema.put("schema", schemaObject.toString());
        int schemaId = registerSchema(subject, schema);

        Map<String,String> config = new HashMap<>();
        config.put("compatibility", "BACKWARD");
        String result = client.target(URL_BASE+"/config/"+subject).request(CONTENT_TYPE).put(Entity.json(config), String.class);

        schema = new HashMap<>();
        schemaObject = SchemaBuilder.record("recordname").fields().name("fieldA").type().intType().noDefault()
                                                .name("fieldB").type().intType().intDefault(0).endRecord();
        schema.put("schema", schemaObject.toString());
        String schemaString = objectMapper.writeValueAsString(schema);
        result = client.target(URL_BASE+"/compatibility/subjects/"+subject+"/versions/1").request(CONTENT_TYPE).post(Entity.json(schemaString), String.class);

        Map<String,Object> actualResultMap = objectMapper.readValue(result, new TypeReference<Map<String,Object>>() {});
        Boolean isCompatible = (Boolean)actualResultMap.get("is_compatible");

        assertTrue(isCompatible);

        schema = new HashMap<>();
        schemaObject = SchemaBuilder.record("recordname").fields().name("fieldA").type().intType().noDefault()
                .name("fieldB").type().intType().noDefault().endRecord();
        schema.put("schema", schemaObject.toString());
        schemaString = objectMapper.writeValueAsString(schema);
        result = client.target(URL_BASE+"/compatibility/subjects/"+subject+"/versions/1").request(CONTENT_TYPE).post(Entity.json(schemaString), String.class);

        actualResultMap = objectMapper.readValue(result, new TypeReference<Map<String,Object>>() {});
        isCompatible = (Boolean)actualResultMap.get("is_compatible");

        assertFalse(isCompatible);
    }
}
