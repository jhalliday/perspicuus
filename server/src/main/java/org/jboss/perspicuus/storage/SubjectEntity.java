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
import java.util.ArrayList;
import java.util.List;

/**
 * Storage layer representation of a Subject, which is an ordered collection of versions of a Schema.
 *
 * @since 2017-02
 * @author Jonathan Halliday (jonathan.halliday@redhat.com)
 */
@Entity
@NamedQueries({
        @NamedQuery(name = "SubjectEntity.allNames", query = "SELECT e.name FROM SubjectEntity e")
})
public class SubjectEntity {

    private String name;

    private List<Integer> schemaIds = new ArrayList<>();

    private String compatibility;

    @Id
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ElementCollection(fetch = FetchType.EAGER)
    @OrderColumn(name = "position")
    public List<Integer> getSchemaIds() {
        return schemaIds;
    }

    public void setSchemaIds(List<Integer> schemaIds) {
        this.schemaIds = schemaIds;
    }

    public String getCompatibility() {
        return compatibility;
    }

    public void setCompatibility(String compatibility) {
        this.compatibility = compatibility;
    }

    @Transient
    public boolean isDeleted() {
        if(getSchemaIds().isEmpty()) {
            return false;
        }

        for(Integer i : getSchemaIds()) {
            if(i != 0) {
                return false;
            }
        }

        return true;
    }
}
