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
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test cases for the Schema search API layer.
 *
 * @since 2017-02
 * @author Jonathan Halliday (jonathan.halliday@redhat.com)
 */
public class SchemaRecommendationResourceIT extends AbstractResourceIT {

    @Test
    public void testSearchForMatchingFieldname() throws Exception {

        int firstId = registerSchema("matchingSubject", getAvroSchema(new String[] { "fieldone", "fieldtwo"}));
        int secondId = registerSchema("matchingSubject", getAvroSchema(new String[] { "fieldthree", "fieldfour"}));

        String result = client.target(URL_BASE+"/schemas/matching/fieldthree").request(CONTENT_TYPE).get(String.class);
        List<Integer> ids = objectMapper.readValue(result, new TypeReference<List<Integer>>() {});

        assertEquals(1, ids.size());
        assertEquals((Integer)secondId, ids.get(0));
    }

    @Test
    public void testSearchForMatchingFieldnameUsingJsonSchema() throws Exception {

        int firstId = registerSchema("matchingSubject", getJsonSchemaSchema(new String[] { "fieldeleven", "fieldtwelve"}));
        int secondId = registerSchema("matchingSubject", getJsonSchemaSchema(new String[] { "fieldthirteen", "fieldfourteen"}));

        String result = client.target(URL_BASE+"/schemas/matching/fieldthirteen").request(CONTENT_TYPE).get(String.class);
        List<Integer> ids = objectMapper.readValue(result, new TypeReference<List<Integer>>() {});

        assertEquals(1, ids.size());
        assertEquals((Integer)secondId, ids.get(0));
    }


    @Test
    public void testSearchForSimilarSchemas() throws Exception {

        int firstId = registerSchema("similarSubject", getAvroSchema(new String[] { "fieldA", "fieldsimilarone", "fieldsimilartwo"}));
        int secondId = registerSchema("similarSubject", getAvroSchema(new String[] { "fieldB", "fieldsimilarone", "fieldsimilartwo"}));

        String result = client.target(URL_BASE+"/schemas/similar/"+firstId).request(CONTENT_TYPE).get(String.class);
        List<Integer> ids = objectMapper.readValue(result, new TypeReference<List<Integer>>() {});

        // should match at least self and similar, plus maybe others - depending on order the tests run the index may not be empty when we start
        assertTrue( ids.size() >= 2);

        assertEquals((Integer)firstId, ids.get(0));
        assertEquals((Integer)secondId, ids.get(1));
    }
}
