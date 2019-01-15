/*
 * Copyright 2017-2019 Red Hat, Inc. and/or its affiliates.
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
import org.junit.Test;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Entity;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Test cases for the Schema Registry REST API layer.
 *
 * @since 2017-02
 * @author Jonathan Halliday (jonathan.halliday@redhat.com)
 */
public class SchemaRegistryResourceIT extends AbstractResourceIT {

    @Test
    public void testRegisterAndReadback() throws Exception {

        String subject = "testsubject";
        try {
            client.target(URL_BASE+"/schemas/ids/10000").request(CONTENT_TYPE).get(String.class);
            fail("Should throw NotFound");
        } catch (NotFoundException e) {
            // expected
        }

        Map<String,Object> schema = getAvroSchema();

        int schemaId = registerSchema(subject, schema);

        String result = client.target(URL_BASE+"/schemas/ids/"+schemaId).request(CONTENT_TYPE).get(String.class);
        Map<String,Object> actualResultMap = objectMapper.readValue(result, new TypeReference<Map<String,Object>>() {});

        assertEquals(schema, actualResultMap);

        result = client.target(URL_BASE+"/subjects").request(CONTENT_TYPE).get(String.class);
        List<String> subjects = objectMapper.readValue(result, new TypeReference<List<String>>() {});
        assertTrue(subjects.contains(subject));
    }

    @Test
    public void testRegisterAndReadbackOfJsonSchema() throws Exception {

        String subject = "testjsonsubject";
        Map<String,Object> schema = getJsonSchemaSchema(new String[] {"fieldone"});

        int schemaId = registerSchema(subject, schema);

        String result = client.target(URL_BASE+"/schemas/ids/"+schemaId).request(CONTENT_TYPE).get(String.class);
        Map<String,Object> actualResultMap = objectMapper.readValue(result, new TypeReference<Map<String,Object>>() {});

        assertEquals(schema, actualResultMap);
    }

    @Test
    public void testRegisterAndReadbackOfProtobuf() throws Exception {

        String subject = "testprotobufsubject";
        Map<String,Object> schema = getProtobufSchema(new String[] {"fieldone"});

        int schemaId = registerSchema(subject, schema);

        String result = client.target(URL_BASE+"/schemas/ids/"+schemaId).request(CONTENT_TYPE).get(String.class);
        Map<String,Object> actualResultMap = objectMapper.readValue(result, new TypeReference<Map<String,Object>>() {});

        assertEquals(schema, actualResultMap);
    }

    @Test
    public void testSearch() throws Exception {

        String subject = "searchsubject";
        Map<String,Object> schema = getAvroSchema();
        String schemaString = objectMapper.writeValueAsString(schema);

        try {
            client.target(URL_BASE + "/subjects/"+subject).request(CONTENT_TYPE).post(Entity.json(schemaString), String.class);
            fail("Should throw NotFound");
        } catch (NotFoundException e) {
            // expected
        }

        int schemaId = registerSchema(subject, schema);

        String result = client.target(URL_BASE + "/subjects/"+subject).request(CONTENT_TYPE).post(Entity.json(schemaString), String.class);
        Map<String,Object> actualResultMap = objectMapper.readValue(result, new TypeReference<Map<String,Object>>() {});

        assertEquals(schema.get("schema"), actualResultMap.get("schema"));
    }

    @Test
    public void testSubjectVersions() throws Exception {

        String subject = "versionsubject";
        Map<String,Object> schema = getAvroSchema();
        String schemaString = objectMapper.writeValueAsString(schema);

        try {
            client.target(URL_BASE + "/subjects/"+subject+"/versions").request(CONTENT_TYPE).get(String.class);
            fail("Should throw NotFound");
        } catch (NotFoundException e) {
            // expected
        }

        try {
            client.target(URL_BASE + "/subjects/"+subject+"/versions/latest").request(CONTENT_TYPE).get(String.class);
            fail("Should throw NotFound");
        } catch (NotFoundException e) {
            // expected
        }

        try {
            client.target(URL_BASE + "/subjects/"+subject+"/versions/1").request(CONTENT_TYPE).get(String.class);
            fail("Should throw NotFound");
        } catch (NotFoundException e) {
            // expected
        }

        int schemaId = registerSchema(subject, schema);

        String result = client.target(URL_BASE + "/subjects/"+subject+"/versions").request(CONTENT_TYPE).get(String.class);
        List<Integer> versionList = objectMapper.readValue(result, new TypeReference<List<Integer>>() {});

        assertEquals(1, versionList.size());
        assertEquals(Integer.valueOf(1), versionList.get(0));

        result = client.target(URL_BASE + "/subjects/"+subject+"/versions/latest").request(CONTENT_TYPE).get(String.class);
        Map<String,Object> actualResultMap = objectMapper.readValue(result, new TypeReference<Map<String,Object>>() {});

        Map<String,Object> expectedResult = getAvroSchema();
        expectedResult.put("id", (int)schemaId);
        expectedResult.put("version", 1);
        expectedResult.put("subject", subject);

        assertEquals(expectedResult, actualResultMap);

        result = client.target(URL_BASE + "/subjects/"+subject+"/versions/"+1).request(CONTENT_TYPE).get(String.class);
        actualResultMap = objectMapper.readValue(result, new TypeReference<Map<String,Object>>() {});

        assertEquals(expectedResult, actualResultMap);

        try {
            client.target(URL_BASE + "/subjects/"+subject+"/versions/"+100).request(CONTENT_TYPE).get(String.class);
            fail("Should throw NotFound");
        } catch (NotFoundException e) {
            // expected
        }
    }

    @Test
    public void testDeletions() throws Exception {

        String subject = "deletionsubject";

        int firstId = registerSchema(subject, getAvroSchema(new String[] {"fieldA"}));
        int secondId = registerSchema(subject, getAvroSchema(new String[] {"fieldA", "fieldB"}));
        int thirdId = registerSchema(subject, getAvroSchema(new String[] {"fieldA", "fieldB", "fieldC"}));

        String result = client.target(URL_BASE + "/subjects/"+subject+"/versions").request(CONTENT_TYPE).get(String.class);
        List<Integer> versionList = objectMapper.readValue(result, new TypeReference<List<Integer>>() {});
        assertEquals(3, versionList.size());
        assertEquals(1, (int)versionList.get(0));
        assertEquals(2, (int)versionList.get(1));
        assertEquals(3, (int)versionList.get(2));

        result = client.target(URL_BASE+"/subjects/"+subject+"/versions/2").request(CONTENT_TYPE).delete(String.class);
        assertEquals("2", result);

        result = client.target(URL_BASE + "/subjects/"+subject+"/versions").request(CONTENT_TYPE).get(String.class);
        versionList = objectMapper.readValue(result, new TypeReference<List<Integer>>() {});
        assertEquals(2, versionList.size());
        assertEquals(1, (int)versionList.get(0));
        assertEquals(3, (int)versionList.get(1));

        result = client.target(URL_BASE+"/subjects/"+subject).request(CONTENT_TYPE).delete(String.class);
        versionList = objectMapper.readValue(result, new TypeReference<List<Integer>>() {});
        assertEquals(2, versionList.size());
        assertEquals(1, (int)versionList.get(0));
        assertEquals(3, (int)versionList.get(1));

        try {
            client.target(URL_BASE + "/subjects/"+subject+"/versions").request(CONTENT_TYPE).get(String.class);
            fail("Should throw NotFound");
        } catch (NotFoundException e) {
            // expected
        }
    }
}
