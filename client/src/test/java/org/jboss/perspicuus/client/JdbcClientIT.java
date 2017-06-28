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
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.Assert.*;

/**
 * Test cases for the JDBC client functions.
 *
 * @since 2017-03
 * @author Jonathan Halliday (jonathan.halliday@redhat.com)
 */
public class JdbcClientIT {

    private final SchemaRegistryClient schemaRegistryClient = new SchemaRegistryClient("http://localhost:8080", "testuser", "testpass");

    private final static String catalogName = "CLIENT_TEST";
    private final static String tableName = "TEST_TABLE";

    private static Connection connection;

    @BeforeClass
    public static void setup() throws SQLException {
        connection = DriverManager.getConnection("jdbc:h2:mem:"+catalogName+";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE", "sa", "sa");

        Statement statement = connection.createStatement();
        statement.execute("CREATE TABLE "+tableName+" (col_a INTEGER, col_b DOUBLE, col_c VARCHAR)");
        statement.close();
    }

    @AfterClass
    public static void teardown() throws SQLException {
        connection.close();
    }

    @Test
    public void clientTest() throws IOException, SQLException {

        JdbcClient jdbcClient = new JdbcClient(schemaRegistryClient, connection.getMetaData());

        Schema actualSchema = jdbcClient.schemaFromTable(catalogName, tableName);

        // note that as the identifiers in the create table statement above are not quoted,
        // the db uppercases them automatically.
        Schema expectedSchema = SchemaBuilder.record(tableName).fields()
                .name("COL_A").type("int").noDefault()
                .name("COL_B").type("double").noDefault()
                .name("COL_C").type("string").noDefault()
                .endRecord();

        assertEquals(expectedSchema, actualSchema);


        String subject = "testjdbckey";
        jdbcClient.associate(subject, catalogName, tableName);

        int id = schemaRegistryClient.getLatestVersion(subject);
        Schema schemaFromServer = schemaRegistryClient.getSchema(id);

        assertEquals(expectedSchema, schemaFromServer);

    }
}
