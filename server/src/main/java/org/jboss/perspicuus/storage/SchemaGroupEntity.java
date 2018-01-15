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

import javax.persistence.*;
import java.util.Set;

/**
 * Representation of a collection of SchemaEntity objects.
 * Can by used flexibly for e.g. all schemas related to a given project or user.
 *
 * @since 2017-03
 * @author Jonathan Halliday (jonathan.halliday@redhat.com)
 */
@Entity
public class SchemaGroupEntity {

    private Integer id;

    private Set<Integer> schemaIds;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @ElementCollection(fetch = FetchType.EAGER)
    @OrderColumn(name = "position")
    public Set<Integer> getSchemaIds() {
        return schemaIds;
    }

    public void setSchemaIds(Set<Integer> schemaIds) {
        this.schemaIds = schemaIds;
    }
}