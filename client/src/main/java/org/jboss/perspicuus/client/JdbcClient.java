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

import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;

import java.io.IOException;
import java.sql.*;

/**
 * Client for conversion and storage of relational db table schema via JDBC metadata.
 *
 * @since 2017-03
 * @author Jonathan Halliday (jonathan.halliday@redhat.com)
 */
public class JdbcClient {

    private final SchemaRegistryClient schemaRegistryClient;
    private final DatabaseMetaData databaseMetaData;

    public JdbcClient(SchemaRegistryClient schemaRegistryClient, DatabaseMetaData databaseMetaData) {
        this.schemaRegistryClient = schemaRegistryClient;
        this.databaseMetaData = databaseMetaData;
    }

    public void associate(String key, String catalog, String tablename) throws IOException, SQLException {

        Schema schema = schemaFromTable(catalog, tablename);

        schemaRegistryClient.registerSchema(key, schema.toString());
    }

    public Schema schemaFromTable(String catalog, String tablename) throws SQLException {

        SchemaBuilder.FieldAssembler fieldAssembler = SchemaBuilder.record(tablename).fields();

        ResultSet columnsResultSet = databaseMetaData.getColumns(catalog, null, tablename, "%");
        while(columnsResultSet.next()) {
            String columnName = columnsResultSet.getString("COLUMN_NAME");
            int jdbcType = columnsResultSet.getInt("DATA_TYPE");
            fieldAssembler.name(columnName).type( jdbcTypeToAvroType(jdbcType) ).noDefault();
        }
        columnsResultSet.close();

        Schema schema = (Schema)fieldAssembler.endRecord();
        return schema;

    }

    public String jdbcTypeToAvroType(int jdbcType) {
        switch (jdbcType) {
            case Types.INTEGER:
                return "int";
            case Types.BIGINT:
                return "long";
            case Types.FLOAT:
                return "float";
            case Types.DOUBLE:
                return "double";
            case Types.BOOLEAN:
                return "boolean";
            case Types.BINARY:
                return "bytes";
            default:
                return "string";
        }
    }
}
