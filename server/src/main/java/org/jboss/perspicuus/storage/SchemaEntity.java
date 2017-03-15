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
import org.hibernate.search.annotations.*;

import javax.persistence.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

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
@Indexed
public class SchemaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public Long id;

    @Column(nullable = false, unique = true)
    public String hash;

    @Column(nullable = false)
    @Field(name="content", store = Store.YES, bridge = @FieldBridge(impl = AvroSchemaStringBridge.class))
    public String content;

    public SchemaEntity() {}

    public SchemaEntity(String schema) {
        Schema avroSchema = new Schema.Parser().parse(schema);
        this.content = avroSchema.toString();

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
}
