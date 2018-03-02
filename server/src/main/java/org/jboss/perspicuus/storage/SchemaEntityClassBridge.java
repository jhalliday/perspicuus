/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
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

import org.apache.lucene.document.Document;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.TwoWayFieldBridge;

import java.util.List;

/**
 * Custom ClassBridge to help make Schema structure indexable by Hibernate Search i.e. lucene.
 *
 * @since 2018-02
 * @author Jonathan Halliday (jonathan.halliday@redhat.com)
 */
public class SchemaEntityClassBridge implements TwoWayFieldBridge {

    @Override
    public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {

        SchemaEntity schemaEntity = (SchemaEntity)value;
        SchemaParser schemaParser = schemaEntity.getSchemaType().getSchemaParser();

        List<String> tokens = schemaParser.tokenizeForSearch(schemaEntity.getContent());
        String indexedString = String.join(" ", tokens);

        luceneOptions.addFieldToDocument(name, indexedString, document);
    }

    @Override
    public Object get(String name, Document document) {
        return null;
    }

    @Override
    public String objectToString(Object object) {
        return (String)object;
    }
}
