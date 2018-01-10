/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
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
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test cases for the Schema search API layer.
 *
 * @since 2017-02
 * @author Jonathan Halliday (jonathan.halliday@redhat.com)
 */
public class SchemaRecommendationResourceIT {

    private final String URL_BASE = "http://localhost:8080";
    private final String CONTENT_TYPE = "application/vnd.schemaregistry.v1+json";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Client client = RestClient.client;

    private int registerSchema(String subject, Map<String,Object> request) throws Exception {
        String schemaString = objectMapper.writeValueAsString(request);

        String result = client.target(URL_BASE+"/subjects/"+subject+"/versions").request(CONTENT_TYPE).post(Entity.json(schemaString), String.class);
        Map<String,Object> actualResultMap = objectMapper.readValue(result, new TypeReference<Map<String,Object>>() {});
        assertEquals(1, actualResultMap.size());
        assertTrue(actualResultMap.containsKey("id"));
        int id = (Integer)actualResultMap.get("id");
        return id;
    }

    @Test
    public void testSearchForMatchingFieldname() throws Exception {

        int firstId = registerSchema("matchingSubject", SchemaHelper.getSchema(new String[] { "fieldone", "fieldtwo"}));
        int secondId = registerSchema("matchingSubject", SchemaHelper.getSchema(new String[] { "fieldthree", "fieldfour"}));

        String result = client.target(URL_BASE+"/schemas/matching/fieldthree").request(CONTENT_TYPE).get(String.class);
        List<Integer> ids = objectMapper.readValue(result, new TypeReference<List<Integer>>() {});

        assertEquals(1, ids.size());
        assertEquals((Integer)secondId, ids.get(0));
    }

    @Test
    public void testSearchForSimilarSchemas() throws Exception {

        int firstId = registerSchema("similarSubject", SchemaHelper.getSchema(new String[] { "fieldA", "fieldsimilarone", "fieldsimilartwo"}));
        int secondId = registerSchema("similarSubject", SchemaHelper.getSchema(new String[] { "fieldB", "fieldsimilarone", "fieldsimilartwo"}));

        String result = client.target(URL_BASE+"/schemas/similar/"+firstId).request(CONTENT_TYPE).get(String.class);
        List<Integer> ids = objectMapper.readValue(result, new TypeReference<List<Integer>>() {});

        // should match at least self and similar, plus maybe others - depending on order the tests run the index may not be empty when we start
        assertTrue( ids.size() >= 2);

        assertEquals((Integer)firstId, ids.get(0));
        assertEquals((Integer)secondId, ids.get(1));
    }
}
