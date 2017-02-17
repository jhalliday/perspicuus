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

import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Test cases for the Tag Registry REST API layer.
 *
 * @since 2017-02
 * @author Jonathan Halliday (jonathan.halliday@redhat.com)
 */
public class TagRegistryResourceIT {

    private final String URL_BASE = "http://localhost:8080";
    private final String CONTENT_TYPE = "application/vnd.schemaregistry.v1+json";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Client client = ClientBuilder.newClient();

    @Test
    public void testCreateAndReadback() throws Exception {

        try {
            client.target(URL_BASE+"/tags/schemas/1").request(CONTENT_TYPE).get(String.class);
            fail("Should throw NotFound");
        } catch (NotFoundException e) {
            // expected
        }

        client.target(URL_BASE+"/tags/schemas/1/testkey").request(CONTENT_TYPE).post(Entity.json("testvalue"), String.class);

        String resultString = client.target(URL_BASE+"/tags/schemas/1").request(CONTENT_TYPE).get(String.class);
        Map<String,String> resultMap = objectMapper.readValue(resultString, new TypeReference<Map<String,String>>() {});

        assertEquals(1, resultMap.size());
        assertEquals("testvalue", resultMap.get("testkey"));
    }

    @Test
    public void testReadGroupAndIndividual() throws Exception {

        // create two key-value pairs
        client.target(URL_BASE+"/tags/schemas/2/testkey1").request(CONTENT_TYPE).post(Entity.json("testvalue1"), String.class);
        client.target(URL_BASE+"/tags/schemas/2/testkey2").request(CONTENT_TYPE).post(Entity.json("testvalue2"), String.class);

        //make sure we can read back separately

        String resultString = client.target(URL_BASE+"/tags/schemas/2/testkey1").request(CONTENT_TYPE).get(String.class);
        Map<String,String> resultMap = objectMapper.readValue(resultString, new TypeReference<Map<String,String>>() {});
        assertEquals(1, resultMap.size());
        assertEquals("testvalue1", resultMap.get("testkey1"));

        // and read back together

        resultString = client.target(URL_BASE+"/tags/schemas/2").request(CONTENT_TYPE).get(String.class);
        resultMap = objectMapper.readValue(resultString, new TypeReference<Map<String,String>>() {});
        assertEquals(2, resultMap.size());
        assertEquals("testvalue1", resultMap.get("testkey1"));
        assertEquals("testvalue2", resultMap.get("testkey2"));
    }

    @Test
    public void testDelete() throws Exception {

        client.target(URL_BASE+"/tags/schemas/3/testkey").request(CONTENT_TYPE).post(Entity.json("testvalue"), String.class);

        String resultString = client.target(URL_BASE+"/tags/schemas/3/testkey").request(CONTENT_TYPE).get(String.class);
        Map<String,String> resultMap = objectMapper.readValue(resultString, new TypeReference<Map<String,String>>() {});
        assertEquals(1, resultMap.size());

        client.target(URL_BASE+"/tags/schemas/3/testkey").request(CONTENT_TYPE).delete();

        try {
            client.target(URL_BASE+"/tags/schemas/3/testkey").request(CONTENT_TYPE).get(String.class);
            fail("Should throw NotFound");
        } catch(NotFoundException e) {
            // expected
        }
    }
}
