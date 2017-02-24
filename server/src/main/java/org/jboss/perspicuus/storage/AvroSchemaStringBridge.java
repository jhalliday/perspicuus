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
package org.jboss.perspicuus.storage;

import org.apache.avro.Schema;
import org.hibernate.search.bridge.StringBridge;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom StringBridge to help make Avro's Schema structure indexable by Hibernate Search i.e. lucene.
 *
 * @since 2017-02
 * @author Jonathan Halliday (jonathan.halliday@redhat.com)
 */
public class AvroSchemaStringBridge implements StringBridge {

    // in its current form this is essentially a custom Analyzer,
    // stripping all the unwanted formatting and returning just the relevant strings for indexing.

    @Override
    public String objectToString(Object object) {

        String string = (String)object;

        if(string.startsWith("{")) {

            Schema avroSchema = new Schema.Parser().parse((String) object);
            List<String> tokens = new ArrayList<>();

            tokens.add(avroSchema.getName());

            for (Schema.Field field : avroSchema.getFields()) {
                tokens.add(field.name());
            }

            String result = String.join(" ", tokens);
            return result;

        } else {
            return string;
        }
    }
}
