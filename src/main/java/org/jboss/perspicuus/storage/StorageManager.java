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

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;

/**
 * Storage layer functions. Uses JDBC database via JPA.
 *
 * @since 2017-02
 * @author Jonathan Halliday (jonathan.halliday@redhat.com)
 */
@ApplicationScoped
public class StorageManager {

    @PersistenceUnit(unitName = "perspicuus")
    private EntityManagerFactory entityManagerFactory;

    public SchemaEntity findByHash(String schema) {

        SchemaEntity schemaEntity = new SchemaEntity(schema);

        EntityManager entityManager = entityManagerFactory.createEntityManager();

        SchemaEntity result = null;

        try {

            entityManager.getTransaction().begin();
            TypedQuery<SchemaEntity> query = entityManager.createNamedQuery("SchemaEntity.byHash", SchemaEntity.class);
            query.setParameter("hash", schemaEntity.hash);
            List<SchemaEntity> schemaEntities = query.getResultList();

            if (!schemaEntities.isEmpty()) {
                result = schemaEntities.get(0);
            }
            entityManager.getTransaction().commit();

        } finally {
            entityManager.close();
        }

        return result;
    }

    public SchemaEntity findSchema(long id) {

        EntityManager entityManager = entityManagerFactory.createEntityManager();

        SchemaEntity schemaEntity = null;

        try {

            entityManager.getTransaction().begin();
            schemaEntity = entityManager.find(SchemaEntity.class, id);
            entityManager.getTransaction().commit();

        } finally {
            entityManager.close();
        }

        return schemaEntity;
    }

    public SubjectEntity findSubject(String name) {

        EntityManager entityManager = entityManagerFactory.createEntityManager();

        SubjectEntity schemaEntity;

        try {

            entityManager.getTransaction().begin();
            schemaEntity = entityManager.find(SubjectEntity.class, name);
            entityManager.getTransaction().commit();

        } finally {
            entityManager.close();
        }

        return schemaEntity;
    }

    public List<String> listSubjectNames() {

        EntityManager entityManager = entityManagerFactory.createEntityManager();

        List<String> results = null;

        try {

            entityManager.getTransaction().begin();
            TypedQuery<String> query = entityManager.createNamedQuery("SubjectEntity.allNames", String.class);
            results = query.getResultList();
            entityManager.getTransaction().commit();

        } finally {
            entityManager.close();
        }

        return results;
    }

    public long register(String subject, String schema) {

        EntityManager entityManager = entityManagerFactory.createEntityManager();

        long schemaId = -1L;

        try {
            entityManager.getTransaction().begin();

            SchemaEntity schemaEntity = findByHash(schema);
            if (schemaEntity == null) {
                schemaEntity = new SchemaEntity(schema);
                entityManager.persist(schemaEntity);
            }
            schemaId = schemaEntity.id;

            SubjectEntity subjectEntity = entityManager.find(SubjectEntity.class, subject);
            if (subjectEntity == null) {
                subjectEntity = new SubjectEntity();
                subjectEntity.name = subject;
                subjectEntity.schemaIds = new ArrayList<>();
            }
            subjectEntity.schemaIds.add(schemaId);

            entityManager.persist(subjectEntity);

            entityManager.getTransaction().commit();

        } finally {
            entityManager.close();
        }

        return schemaId;
    }

}