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
package org.jboss.perspicuus.client.spark;

import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;

import org.apache.spark.sql.types.*;
import org.apache.spark.sql.types.StructType;
import org.jboss.perspicuus.client.SchemaRegistryClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Client for storage and retrieval of Spark Schema.
 *
 * @since 2017-02
 * @author Jonathan Halliday (jonathan.halliday@redhat.com)
 */
public class MetadataClient {

    private final SchemaRegistryClient schemaRegistryClient;

    public MetadataClient(String url, String username, String password) throws IOException {
        schemaRegistryClient = new SchemaRegistryClient(url, username, password);
    }

    public StructType resolve(String key) throws IOException {

        int id = schemaRegistryClient.getLatestVersion(key);
        Schema avroSchema = schemaRegistryClient.getSchema(id);

        List<StructField> definedFields = new ArrayList<>(avroSchema.getFields().size());
        for(Schema.Field field : avroSchema.getFields()) {
            String avroType = field.schema().getType().getName();
            definedFields.add(new StructField(field.name(), avroTypeToSparkType(avroType), false, Metadata.empty()));
        }
        StructType definedSchema = org.apache.spark.sql.types.DataTypes.createStructType(definedFields);

        return definedSchema;
    }

    public void associate(String key, StructType sparkStructType) throws IOException {
        StructField[] sparkStructFields = sparkStructType.fields();

        SchemaBuilder.FieldAssembler fieldAssembler = SchemaBuilder.record(key).fields();
        for(StructField structField : sparkStructFields) {
            fieldAssembler.name(structField.name()).type( sparkTypeToAvroType(structField.dataType()) ).noDefault();
        }
        Schema schema = (Schema)fieldAssembler.endRecord();

        schemaRegistryClient.registerSchema(key, schema.toString());
    }

    public String sparkTypeToAvroType(DataType dataType) {
        if(dataType.typeName().equals("integer")) {
            return "int";
        } else {
            return "string";
        }
    }

    public DataType avroTypeToSparkType(String dataType) {
        if("int".equals(dataType)) {
            return org.apache.spark.sql.types.DataTypes.IntegerType;
        } else {
            return org.apache.spark.sql.types.DataTypes.StringType;
        }
    }
}
