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
import javax.persistence.*;
import java.util.ArrayList;
import java.util.HashMap;
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

    ThreadLocal<EntityManager> threadEntityManager = new ThreadLocal<>();

    public void threadInit() {
        EntityManager entityManager = threadEntityManager.get();
        if(entityManager == null) {
            entityManager = entityManagerFactory.createEntityManager();
            threadEntityManager.set(entityManager);
        }
        entityManager.getTransaction().begin();
    }

    public void threadCleanup() {
        EntityManager entityManager = threadEntityManager.get();
        if(entityManager != null) {
            EntityTransaction transaction = entityManager.getTransaction();
            if(transaction.isActive()) {
                transaction.rollback();
            }
            entityManager.close();
            threadEntityManager.remove();
        }
    }

    public SchemaEntity findByHash(String schema) {

        SchemaEntity schemaEntity = new SchemaEntity(schema);

        EntityManager entityManager = threadEntityManager.get();

        SchemaEntity result = null;

        TypedQuery<SchemaEntity> query = entityManager.createNamedQuery("SchemaEntity.byHash", SchemaEntity.class);
        query.setParameter("hash", schemaEntity.hash);
        List<SchemaEntity> schemaEntities = query.getResultList();

        if (!schemaEntities.isEmpty()) {
            result = schemaEntities.get(0);
        }

        return result;
    }

    public SchemaEntity findSchema(long id) {

        EntityManager entityManager = threadEntityManager.get();

        SchemaEntity schemaEntity = null;

        schemaEntity = entityManager.find(SchemaEntity.class, id);

        return schemaEntity;
    }

    public SubjectEntity findSubject(String name) {

        EntityManager entityManager = threadEntityManager.get();

        SubjectEntity schemaEntity;

        schemaEntity = entityManager.find(SubjectEntity.class, name);

        return schemaEntity;
    }

    public List<String> listSubjectNames() {

        EntityManager entityManager = threadEntityManager.get();

        List<String> results = null;

        TypedQuery<String> query = entityManager.createNamedQuery("SubjectEntity.allNames", String.class);
        results = query.getResultList();

        return results;
    }

    public long register(String subject, String schema) {

        EntityManager entityManager = threadEntityManager.get();

        long schemaId = -1L;

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
        entityManager.getTransaction().begin();

        return schemaId;
    }

    public TagCollectionEntity getTags(long id) {

        EntityManager entityManager = threadEntityManager.get();

        TagCollectionEntity tagCollectionEntity = null;

        tagCollectionEntity = entityManager.find(TagCollectionEntity.class, id);

        return tagCollectionEntity;
    }

    public void updateTag(long id, String key, String value) {

        EntityManager entityManager = threadEntityManager.get();

        TagCollectionEntity tagCollectionEntity = null;

        tagCollectionEntity = entityManager.find(TagCollectionEntity.class, id);
        if(tagCollectionEntity == null) {
            tagCollectionEntity = new TagCollectionEntity();
            tagCollectionEntity.id = id;
            tagCollectionEntity.tags = new HashMap<>();
        }
        if(value != null) {
            tagCollectionEntity.tags.put(key, value);
        } else {
            tagCollectionEntity.tags.remove(key);
        }
        entityManager.persist(tagCollectionEntity);

        entityManager.getTransaction().commit();
        entityManager.getTransaction().begin();
    }
}
