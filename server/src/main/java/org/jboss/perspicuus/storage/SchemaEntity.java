/*
 * Copyright 2017-2019 Red Hat, Inc. and/or its affiliates.
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

import javax.persistence.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

/**
 * Storage layer representation of a Schema.
 *
 * @since 2017-02
 * @author Jonathan Halliday (jonathan.halliday@redhat.com)
 */
@Entity
@NamedQueries({
        @NamedQuery(name = "SchemaEntity.byHash", query = "SELECT e FROM SchemaEntity e WHERE e.hash=:hash")
})
public class SchemaEntity {

    /*
      A SchemaEntity may contain a schema in one of a number of known types (e.g. Avro, JsonSchema, ...)
      These are not polymorphic at the object model or relation storage levels, since there all schema are just Strings.
      However, they are polymorphic with regard to the some functions that require internal knowledge of the schema
      structure, requiring a type-appropriate Parser to be applied for e.g. compatibility testing.
     */

    private Integer id;

    private String hash;

    private String content;

    private SchemaType schemaType;

    public SchemaEntity() {}

    public SchemaEntity(String schema) {

        Optional<String> canonicalSchema = Optional.empty();

        // the only way to identify the type of schema we've been given is to
        // try each parser in turn until one accepts it as valid.

        for(SchemaType schemaType : SchemaType.values()) {
            SchemaParser schemaParser = schemaType.getSchemaParser();
            canonicalSchema = schemaParser.parseToCanonicalForm(schema);
            if(canonicalSchema.isPresent()) {
                this.content = canonicalSchema.get();
                this.schemaType = schemaType;
                break;
            }
        }

        if(!canonicalSchema.isPresent()) {
            throw new IllegalArgumentException("can't parse provided schema as any known type");
        }

        try {
            // https://avro.apache.org/docs/current/spec.html#Schema+Fingerprints
            // recommends MD5 or SHA-256, both of which are present as standard in java
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(content.getBytes());
            byte[] digestBytes = messageDigest.digest();
            this.hash = Arrays.toString(digestBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Column(nullable = false, unique = true)
    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    @Column(nullable = false)
    @Lob
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public SchemaType getSchemaType() {
        return schemaType;
    }

    public void setSchemaType(SchemaType schemaType) {
        this.schemaType = schemaType;
    }

    public boolean isCompatibleWith(String compatibilityLevel, String secondSchema) {
        return schemaType.getSchemaParser().isCompatibleWith(compatibilityLevel, Collections.singletonList(this.content), secondSchema);
    }
}
