/*
 * Copyright 2017, 2019 Red Hat, Inc. and/or its affiliates.
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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.avro.Schema;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import java.io.IOException;
import java.util.*;

/**
 * Client for communicating with a remote Schema Registry server.
 *
 * @since 2017-02
 * @author Jonathan Halliday (jonathan.halliday@redhat.com)
 */
public class SchemaRegistryClient {

    private static final String CONTENT_TYPE = "application/vnd.schemaregistry.v1+json";

    private final Client client = ClientBuilder.newClient();
    private final String serverURL;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public SchemaRegistryClient(String serverURL, String username, String password) {
        this.serverURL = serverURL;
        client.register(new BasicAuthFilter(username, password));
    }

    /**
     * Return a Schema corresponding to the given id, or null if there isn't one.
     *
     * @param id
     * @return
     * @throws IOException
     */
    public Schema getSchema(int id) throws IOException {
        try {
            String resultString = client.target(serverURL + "/schemas/ids/"+id).request(CONTENT_TYPE).get(String.class);
            Map<String,Object> resultMap = objectMapper.readValue(resultString, new TypeReference<Map<String,Object>>() {});
            String schemaString = (String)resultMap.get("schema");
            Schema avroSchema = new Schema.Parser().parse(schemaString);
            return avroSchema;
        } catch(NotFoundException e) {
            return null;
        }
    }

    /**
     * List schema ids for the given topic, in version order.
     * Note that versions number from one, whist the array indexes from zero.
     *
     * @param subject
     * @return
     * @throws IOException
     */
    public List<Integer> listVersions(String subject) throws IOException {
        try {
            String resultString = client.target(serverURL + "/subjects/"+subject+"/versions").request(CONTENT_TYPE).get(String.class);
            List<Integer> versionList = objectMapper.readValue(resultString, new TypeReference<List<Integer>>() {});
            return versionList;
        } catch (NotFoundException e) {
            return null;
        }
    }

    /**
     * Return the schema id for latest version of given subject.
     *
     * @param subject
     * @return
     * @throws IOException
     */
    public int getLatestVersion(String subject) throws IOException {
        return getVersion(subject, "latest");
    }

    /**
     * Return  the schema id for the given version of the subject.
     *
     * @param subject
     * @param version
     * @return
     * @throws IOException
     */
    public int getVersion(String subject, int version) throws IOException {
        return getVersion(subject, ""+version);
    }

    protected int getVersion(String subject, String version) throws IOException {
        try {
            String resultString = client.target(serverURL + "/subjects/"+subject+"/versions/"+version).request(CONTENT_TYPE).get(String.class);
            Map<String,Object> resultMap = objectMapper.readValue(resultString, new TypeReference<Map<String,Object>>() {});
            int id = (Integer) resultMap.get("id");
            return id;
        } catch (NotFoundException e) {
            return -1;
        }
    }

    /**
     * Return a list of known subjects
     *
     * @return
     * @throws IOException
     */
    public List<String> getSubjects() throws IOException {
        String resultString = client.target(serverURL+"/subjects").request(CONTENT_TYPE).get(String.class);
        List<String> subjects = objectMapper.readValue(resultString, new TypeReference<List<String>>() {});
        return subjects;
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
        Map<String,Object> requestMap = new HashMap<>();
        requestMap.put("schema", schemaInputString);
        String requestString = objectMapper.writeValueAsString(requestMap);
        try {
            String resultString = client.target(serverURL + "/subjects/"+subject).request(CONTENT_TYPE).post(Entity.json(requestString), String.class);
            Map<String,Object> resultMap = objectMapper.readValue(resultString, new TypeReference<Map<String,Object>>() {});
            String schemaResultString = (String)resultMap.get("schema");
            Schema avroSchema = new Schema.Parser().parse(schemaResultString);
            return avroSchema;
        } catch (NotFoundException e) {
            return null;
        }
    }

    /**
     * Register the given schema under the provided subject.
     *
     * @param subject
     * @param schema
     * @return
     * @throws IOException
     */
    public int registerSchema(String subject, String schema) throws IOException {
        Map<String,Object> requestMap = new HashMap<>();
        requestMap.put("schema", schema);
        String requestString = objectMapper.writeValueAsString(requestMap);
        String result = client.target(serverURL+"/subjects/"+subject+"/versions")
                .request(CONTENT_TYPE).post(Entity.json(requestString), String.class);
        Map<String, Object> resultMap = objectMapper.readValue(result, new TypeReference<Map<String, Object>>() {});
        int id = (Integer) resultMap.get("id");
        return id;
    }

    /**
     * Delete a specific schema version from the subject.
     *
     * @param subject
     * @param version
     * @throws IOException
     */
    public int deleteVersion(String subject, String version) throws IOException {
        try {
            String result = client.target(serverURL + "/subjects/" + subject + "/versions/" + version)
                    .request(CONTENT_TYPE).delete(String.class);
            return Integer.parseInt(result);
        } catch (NotFoundException e) {
            return -1;
        }
    }

    /**
     * Delete a subject.
     *
     * @param subject
     * @throws IOException
     */
    public List<Integer> deleteSubject(String subject) throws IOException {
        try {
            String resultString = client.target(serverURL + "/subjects/" + subject).request(CONTENT_TYPE).delete(String.class);
            List<Integer> versionList = objectMapper.readValue(resultString, new TypeReference<List<Integer>>() {
            });
            return versionList;
        } catch (NotFoundException e) {
            return Collections.emptyList();
        }
    }

    /**
     * Get the global default schema compatibility setting.
     *
     * @return
     * @throws IOException
     */
    public String getGlobalDefaultCompatibilityLevel() throws IOException {
        String result = client.target(serverURL+"/config").request(CONTENT_TYPE).get(String.class);
        Map<String, Object> resultMap = objectMapper.readValue(result, new TypeReference<Map<String, Object>>() {});
        return (String)resultMap.get("compatibilityLevel");
    }

    /**
     * Set the global default schema compatibility setting.
     *
     * @param level
     * @return
     * @throws IOException
     */
    public String setGlobalDefaultCompatibilityLevel(String level) throws IOException {
        Map<String,Object> requestMap = new HashMap<>();
        requestMap.put("compatibility", level);
        String requestString = objectMapper.writeValueAsString(requestMap);
        String result = client.target(serverURL+"/config").request(CONTENT_TYPE).put(Entity.json(requestString), String.class);
        Map<String, Object> resultMap = objectMapper.readValue(result, new TypeReference<Map<String, Object>>() {});
        return (String)resultMap.get("compatibility");
    }

    /**
     * Configure a subject's schema compatibility setting.
     *
     * @param subject
     * @param level
     * @throws IOException
     */
    public void setSubjectCompatibilityLevel(String subject, String level) throws IOException {
        Map<String,Object> requestMap = new HashMap<>();
        requestMap.put("compatibility", level);
        String requestString = objectMapper.writeValueAsString(requestMap);
        try {
            client.target(serverURL + "/config/" + subject).request(CONTENT_TYPE).put(Entity.json(requestString), String.class);
        } catch(NotFoundException e) {
            return;
        }
    }

    public boolean determineCompatibility(String subject, String version, String schema) throws Exception {

        Map<String,Object> requestMap = new HashMap<>();
        requestMap.put("schema", schema);
        String requestString = objectMapper.writeValueAsString(requestMap);

        String result = client.target(serverURL+"/compatibility/subjects/"+subject+"/versions/"+version).request(CONTENT_TYPE).post(Entity.json(requestString), String.class);

        Map<String,Object> actualResultMap = objectMapper.readValue(result, new TypeReference<Map<String,Object>>() {});
        Boolean isCompatible = (Boolean)actualResultMap.get("is_compatible");

        return isCompatible;
    }
}
