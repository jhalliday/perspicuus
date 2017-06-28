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

import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.Metadata;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Test cases for the Spark client API.
 *
 * @since 2017-02
 * @author Jonathan Halliday (jonathan.halliday@redhat.com)
 */
public class SparkClientIT {

    @Test
    public void clientTest() throws IOException {

        List<StructField> definedFields = new ArrayList<>();
        definedFields.add(new StructField("stringCol", DataTypes.StringType, false, Metadata.empty()));
        definedFields.add(new StructField("intCol", DataTypes.IntegerType, false, Metadata.empty()));
        StructType definedSchema = DataTypes.createStructType(definedFields);
        //definedSchema.printTreeString();

        MetadataClient metadataClient = new MetadataClient("http://localhost:8080", "testuser", "testpass");
        String key = "testKey";

        metadataClient.associate(key, definedSchema);

        StructType resolvedSchema = metadataClient.resolve(key);
        //resolvedSchema.printTreeString();

        assertEquals(definedSchema, resolvedSchema);
    }
}
