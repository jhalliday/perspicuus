package org.jboss.perspicuus.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * TODO doc me
 *
 * @since 2017-02
 * @author Jonathan Halliday (jonathan.halliday@redhat.com)
 */
public class SchemaRegistryResourceIT {

    private final String URL_BASE = "http://localhost:8080";
    private final String CONTENT_TYPE = "application/vnd.schemaregistry.v1+json";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Client client = ClientBuilder.newClient();

    private long registerSchema(String subject, Map<String,Object> request) throws Exception {
        String schemaString = objectMapper.writeValueAsString(request);

        String result = client.target(URL_BASE+"/subjects/"+subject+"/versions").request(CONTENT_TYPE).post(Entity.json(schemaString), String.class);
        Map<String,Object> actualResultMap = objectMapper.readValue(result, new TypeReference<Map<String,Object>>() {});
        assertEquals(1, actualResultMap.size());
        assertTrue(actualResultMap.containsKey("id"));
        int id = (Integer)actualResultMap.get("id");
        return(long)id;
    }

    private Map<String,Object> getTestSchema() throws Exception {
        Map<String,Object> schemaOuterMap = new HashMap<>();
        Map<String,Object> schemaInnerMap = new HashMap<>();
        schemaInnerMap.put("type", "string");
        schemaOuterMap.put("schema", objectMapper.writeValueAsString(schemaInnerMap));
        return schemaOuterMap;
    }

    @Test
    public void testRegisterAndReadback() throws Exception {

        String subject = "testsubject";
        try {
            client.target(URL_BASE+"/schemas/ids/10000").request(CONTENT_TYPE).get(String.class);
            fail("Should throw NotFound");
        } catch (NotFoundException e) {
            // expected
        }

        Map<String,Object> schema = getTestSchema();

        long schemaId = registerSchema(subject, schema);

        String result = client.target(URL_BASE+"/schemas/ids/"+schemaId).request(CONTENT_TYPE).get(String.class);
        Map<String,Object> actualResultMap = objectMapper.readValue(result, new TypeReference<Map<String,Object>>() {});

        assertEquals(schema, actualResultMap);

        result = client.target(URL_BASE+"/subjects").request(CONTENT_TYPE).get(String.class);
        List<String> subjects = objectMapper.readValue(result, new TypeReference<List<String>>() {});
        assertTrue(subjects.contains(subject));
    }

    @Test
    public void testSearch() throws Exception {

        String subject = "searchsubject";
        Map<String,Object> schema = getTestSchema();
        String schemaString = objectMapper.writeValueAsString(schema);

        try {
            client.target(URL_BASE + "/subjects/"+subject).request(CONTENT_TYPE).post(Entity.json(schemaString), String.class);
            fail("Should throw NotFound");
        } catch (NotFoundException e) {
            // expected
        }

        long schemaId = registerSchema(subject, schema);

        String result = client.target(URL_BASE + "/subjects/"+subject).request(CONTENT_TYPE).post(Entity.json(schemaString), String.class);
        Map<String,Object> actualResultMap = objectMapper.readValue(result, new TypeReference<Map<String,Object>>() {});

        assertEquals(schema, actualResultMap);
    }

    @Test
    public void testSubjectVersions() throws Exception {

        String subject = "versionsubject";
        Map<String,Object> schema = getTestSchema();
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
            client.target(URL_BASE + "/subjects/"+subject+"/1").request(CONTENT_TYPE).get(String.class);
            fail("Should throw NotFound");
        } catch (NotFoundException e) {
            // expected
        }

        long schemaId = registerSchema(subject, schema);

        String result = client.target(URL_BASE + "/subjects/"+subject+"/versions").request(CONTENT_TYPE).get(String.class);
        List<Long> versionList = objectMapper.readValue(result, new TypeReference<List<Long>>() {});

        assertEquals(1, versionList.size());
        assertEquals((Long)schemaId, versionList.get(0));

        result = client.target(URL_BASE + "/subjects/"+subject+"/versions/latest").request(CONTENT_TYPE).get(String.class);
        Map<String,Object> actualResultMap = objectMapper.readValue(result, new TypeReference<Map<String,Object>>() {});

        Map<String,Object> expectedResult = getTestSchema();
        expectedResult.put("id", (int)schemaId);
        expectedResult.put("version", 1);
        expectedResult.put("subject", subject);

        assertEquals(expectedResult, actualResultMap);

        result = client.target(URL_BASE + "/subjects/"+subject+"/versions/latest").request(CONTENT_TYPE).get(String.class);
        actualResultMap = objectMapper.readValue(result, new TypeReference<Map<String,Object>>() {});

        assertEquals(expectedResult, actualResultMap);
    }
}
