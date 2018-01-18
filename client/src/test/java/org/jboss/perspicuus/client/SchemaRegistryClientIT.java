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
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Test cases for the Schema Registry client.
 * Similar to SchemaRegistryResourceIT, but executed via the client.
 *
 * @since 2017-02
 * @author Jonathan Halliday (jonathan.halliday@redhat.com)
 */
public class SchemaRegistryClientIT {

    private final SchemaRegistryClient schemaRegistryClient = new SchemaRegistryClient("http://localhost:8080", "testuser", "testpass");

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

        int schemaId = schemaRegistryClient.registerSchema(subject, localSchema.toString());

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

        int schemaId = schemaRegistryClient.registerSchema(subject, localSchema.toString());

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

        int schemaId = schemaRegistryClient.registerSchema(subject, localSchema.toString());

        List<Integer> versions = schemaRegistryClient.listVersions(subject);
        assertEquals(1, versions.size());
        assertEquals(new Integer(1), versions.get(0));

        int id = schemaRegistryClient.getLatestVersion(subject);
        assertEquals(schemaId, id);

        id = schemaRegistryClient.getVersion(subject, 1);
        assertEquals(schemaId, id);
    }

    @Test
    public void testDeletions() throws Exception {

        String subject = "clientdeletionsubject";

        schemaRegistryClient.registerSchema(subject, getTestSchema().toString());

        int result = schemaRegistryClient.deleteVersion(subject, "latest");

        assertEquals(1, result);

        result = schemaRegistryClient.deleteVersion(subject, "latest");

        assertEquals(-1, result);

        schemaRegistryClient.registerSchema(subject, getTestSchema().toString());

        List<Integer> resultList = schemaRegistryClient.deleteSubject(subject);

        assertEquals(1, resultList.size());
        assertEquals(2, (int)resultList.get(0));

        resultList = schemaRegistryClient.deleteSubject(subject);
        assertEquals(0, resultList.size());
    }

    @Test
    public void testTagCreateAndReadback() throws Exception {

        int id = 4;
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

        int id = 4;
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

        int firstId = schemaRegistryClient.registerSchema("clientMatchingSubject", getCustomTestSchema("clientMatchingSubject", new String[] { "fieldone", "fieldtwo"}).toString() );
        int secondId = schemaRegistryClient.registerSchema("clientMatchingSubject", getCustomTestSchema("clientMatchingSubject", new String[] { "fieldthree", "fieldfour"}).toString() );

        List<Integer> ids = schemaRegistryClient.findSchemasMatching("fieldthree");

        assertEquals(1, ids.size());
        assertEquals((Integer)secondId, ids.get(0));
    }

    @Test
    public void testSearchForSimilarSchemas() throws Exception {

        int firstId = schemaRegistryClient.registerSchema("clientSimilarSubject", getCustomTestSchema("clientSimilarSubject", new String[] { "fieldA", "fieldsimilarone", "fieldsimilartwo"}).toString() );
        int secondId = schemaRegistryClient.registerSchema("clientSimilarSubject", getCustomTestSchema("clientSimilarSubject", new String[] { "fieldB", "fieldsimilarone", "fieldsimilartwo"}).toString() );

        List<Integer> ids = schemaRegistryClient.findSimilarSchemas(firstId);

        // should match at least self and similar, plus maybe others - depending on order the tests run the index may not be empty when we start
        assertTrue( ids.size() >= 2);

        assertEquals((Integer)firstId, ids.get(0));
        assertEquals((Integer)secondId, ids.get(1));
    }

    @Test
    public void testGroupLifecycle() throws Exception {

        int groupId = schemaRegistryClient.createGroup();

        schemaRegistryClient.addSchemaToGroup(groupId, 100);

        Set<Integer> members = schemaRegistryClient.getSchemaGroupMembers(groupId);

        assertEquals(1, members.size());
        assertEquals(new Integer(100), members.iterator().next());

        schemaRegistryClient.removeSchemaFromGroup(groupId, 100);

        members = schemaRegistryClient.getSchemaGroupMembers(groupId);
        assertTrue(members.isEmpty());
    }

    @Test
    public void testCompatibility() throws Exception {

        String subject = "compatibilitysubject";

        schemaRegistryClient.registerSchema(subject, getTestSchema().toString());

        Schema compatibleSchema = getTestSchema();

        boolean isCompatible = schemaRegistryClient.determineCompatibility(subject, "latest", compatibleSchema.toString());

        assertTrue(isCompatible);

        SchemaBuilder.FieldAssembler<Schema> fieldAssembler = SchemaBuilder.record("recordname").fields();
        fieldAssembler.name("fieldname").type().intType().noDefault();
        Schema incompatibleSchema = fieldAssembler.endRecord();

        isCompatible = schemaRegistryClient.determineCompatibility(subject, "latest", incompatibleSchema.toString());

        assertTrue(isCompatible);
    }

    @Test
    public void testGlobalCompatibilityLevel() throws Exception {

        String defaultLevel = schemaRegistryClient.getGlobalDefaultCompatibilityLevel();
        assertEquals("NONE", defaultLevel);
        String modifiedLevel = schemaRegistryClient.setGlobalDefaultCompatibilityLevel("BACKWARD");
        assertEquals("BACKWARD", modifiedLevel);
        String checkLevel = schemaRegistryClient.getGlobalDefaultCompatibilityLevel();
        assertEquals("BACKWARD", checkLevel);
    }
}
