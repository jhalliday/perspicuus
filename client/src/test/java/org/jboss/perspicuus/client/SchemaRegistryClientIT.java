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

import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Test cases for the Schema Registry client.
 * Similar to SchemaRegistryResourceIT, but executed via the client.
 *
 * @since 2017-02
 * @author Jonathan Halliday (jonathan.halliday@redhat.com)
 */
public class SchemaRegistryClientIT {

    private final SchemaRegistryClient schemaRegistryClient = new SchemaRegistryClient("http://localhost:8080");

    private Schema getTestSchema() throws Exception {
        Schema schema = getCustomTestSchema("recordname", new String[] {"fieldname"});
        return schema;
    }

    private Schema getCustomTestSchema(String subject, String[] fieldnames) throws Exception {
        SchemaBuilder.FieldAssembler<Schema> fieldAssembler = SchemaBuilder.record(subject).fields();
        for(String fieldname : fieldnames) {
            fieldAssembler.name(fieldname).type().stringType().noDefault();
        }
        Schema schema = fieldAssembler.endRecord();
        return schema;
    }

    @Test
    public void testRegisterAndReadback() throws Exception {

        assertNull( schemaRegistryClient.getSchema(10000) );

        String subject = "clienttestsubject";
        Schema localSchema = getTestSchema();

        long schemaId = schemaRegistryClient.registerSchema(subject, localSchema.toString());

        Schema remoteSchema = schemaRegistryClient.getSchema(schemaId);

        assertEquals(localSchema, remoteSchema);

        List<String> subjects = schemaRegistryClient.getSubjects();
        assertTrue(subjects.contains(subject));
    }

    @Test
    public void testSearch() throws Exception {

        String subject = "clientsearchsubject";
        Schema localSchema = getTestSchema();

        assertNull( schemaRegistryClient.findInSubject(subject, localSchema) );

        long schemaId = schemaRegistryClient.registerSchema(subject, localSchema.toString());

        Schema remoteSchema = schemaRegistryClient.findInSubject(subject, localSchema);

        assertEquals(localSchema, remoteSchema);
    }

    @Test
    public void testSubjectVersions() throws Exception {

        String subject = "clientversionsubject";
        Schema localSchema = getTestSchema();

        assertNull( schemaRegistryClient.listVersions(subject) );

        assertEquals(-1, schemaRegistryClient.getLatestVersion(subject) );

        assertEquals(-1, schemaRegistryClient.getVersion(subject, 1) );

        long schemaId = schemaRegistryClient.registerSchema(subject, localSchema.toString());

        List<Long> versions = schemaRegistryClient.listVersions(subject);
        assertEquals(1, versions.size());
        assertEquals(new Long(schemaId), versions.get(0));

        long id = schemaRegistryClient.getLatestVersion(subject);
        assertEquals(schemaId, id);

        id = schemaRegistryClient.getVersion(subject, 1);
        assertEquals(schemaId, id);
    }

    @Test
    public void testTagCreateAndReadback() throws Exception {

        long id = 4;
        String key = "clientTestKey";
        String value = "testValue";

        schemaRegistryClient.annotate(id, key, value);

        String result = schemaRegistryClient.getAnnotation(id, key);

        assertEquals(value, result);

        Map<String,String> resultMap = schemaRegistryClient.getAnnotations(id);

        assertEquals(1, resultMap.size());
        assertEquals(value, resultMap.get(key));
    }

    @Test
    public void testTagDelete() throws Exception {

        long id = 4;
        String key = "clientTestKey";
        String value = "testValue";

        schemaRegistryClient.annotate(id, key, value);

        String result = schemaRegistryClient.getAnnotation(id, key);

        assertEquals(value, result);

        schemaRegistryClient.deleteAnnotation(id, key);

        result = schemaRegistryClient.getAnnotation(id, key);

        assertNull(result);
    }

    @Test
    public void testSearchForMatchingFieldname() throws Exception {

        long firstId = schemaRegistryClient.registerSchema("clientMatchingSubject", getCustomTestSchema("clientMatchingSubject", new String[] { "fieldone", "fieldtwo"}).toString() );
        long secondId = schemaRegistryClient.registerSchema("clientMatchingSubject", getCustomTestSchema("clientMatchingSubject", new String[] { "fieldthree", "fieldfour"}).toString() );

        List<Long> ids = schemaRegistryClient.findSchemasMatching("fieldthree");

        assertEquals(1, ids.size());
        assertEquals((Long)secondId, ids.get(0));
    }

    @Test
    public void testSearchForSimilarSchemas() throws Exception {

        long firstId = schemaRegistryClient.registerSchema("clientSimilarSubject", getCustomTestSchema("clientSimilarSubject", new String[] { "fieldA", "fieldsimilarone", "fieldsimilartwo"}).toString() );
        long secondId = schemaRegistryClient.registerSchema("clientSimilarSubject", getCustomTestSchema("clientSimilarSubject", new String[] { "fieldB", "fieldsimilarone", "fieldsimilartwo"}).toString() );

        List<Long> ids = schemaRegistryClient.findSimilarSchemas(firstId);

        // should match at least self and similar, plus maybe others - depending on order the tests run the index may not be empty when we start
        assertTrue( ids.size() >= 2);

        assertEquals((Long)firstId, ids.get(0));
        assertEquals((Long)secondId, ids.get(1));
    }

}
