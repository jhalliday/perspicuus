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
package org.jboss.perspicuus.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.avro.Schema;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public SchemaRegistryClient(String serverURL) {
        this.serverURL = serverURL;
    }

    /**
     * Return a Schema corresponding to the given id, or null if there isn't one.
     *
     * @param id
     * @return
     * @throws IOException
     */
    public Schema getSchema(long id) throws IOException {
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
    public List<Long> listVersions(String subject) throws IOException {
        try {
            String resultString = client.target(serverURL + "/subjects/"+subject+"/versions").request(CONTENT_TYPE).get(String.class);
            List<Long> versionList = objectMapper.readValue(resultString, new TypeReference<List<Long>>() {});
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
    public long getLatestVersion(String subject) throws IOException {
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
    public long getVersion(String subject, int version) throws IOException {
        return getVersion(subject, ""+version);
    }

    protected long getVersion(String subject, String version) throws IOException {
        try {
            String resultString = client.target(serverURL + "/subjects/"+subject+"/versions/"+version).request(CONTENT_TYPE).get(String.class);
            Map<String,Object> resultMap = objectMapper.readValue(resultString, new TypeReference<Map<String,Object>>() {});
            int id = (Integer) resultMap.get("id");
            return (long) id;
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
    public long registerSchema(String subject, String schema) throws IOException {
        Map<String,Object> requestMap = new HashMap<>();
        requestMap.put("schema", schema);
        String requestString = objectMapper.writeValueAsString(requestMap);
        String result = client.target(serverURL+"/subjects/"+subject+"/versions")
                .request(CONTENT_TYPE).post(Entity.json(requestString), String.class);
        Map<String, Object> resultMap = objectMapper.readValue(result, new TypeReference<Map<String, Object>>() {});
        int id = (Integer) resultMap.get("id");
        return (long) id;
    }


    public void annotate(long schemaId, String key, String value) throws IOException {
        client.target(serverURL+"/tags/schemas/"+schemaId+"/"+key).request(CONTENT_TYPE).post(Entity.json(value), String.class);
    }

    public void deleteAnnotation(long schemaId, String key) throws IOException {
        try {
            client.target(serverURL + "/tags/schemas/" + schemaId + "/" + key).request(CONTENT_TYPE).delete();
        } catch(NotFoundException e) {
            return;
        }
    }

    public Map<String,String> getAnnotations(long schemaId) throws IOException {
        try {
            String resultString = client.target(serverURL + "/tags/schemas/" + schemaId).request(CONTENT_TYPE).get(String.class);
            Map<String, String> resultMap = objectMapper.readValue(resultString, new TypeReference<Map<String, String>>() {
            });
            return resultMap;
        } catch(NotFoundException e) {
            return null;
        }
    }

    public String getAnnotation(long schemaId, String key) throws IOException {
        try {
            String resultString = client.target(serverURL + "/tags/schemas/" + schemaId + "/" + key).request(CONTENT_TYPE).get(String.class);
            Map<String, String> resultMap = objectMapper.readValue(resultString, new TypeReference<Map<String, String>>() {});
            return resultMap.get(key);
        } catch (NotFoundException e) {
            return null;
        }
    }
}
