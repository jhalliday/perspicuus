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

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.*;
import java.util.*;

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
        query.setParameter("hash", schemaEntity.getHash());
        List<SchemaEntity> schemaEntities = query.getResultList();

        if (!schemaEntities.isEmpty()) {
            result = schemaEntities.get(0);
        }

        return result;
    }

    public List<SchemaEntity> getSchemas(String subject) {
        EntityManager entityManager = threadEntityManager.get();
        SubjectEntity subjectEntity = findSubject(subject);
        if(subjectEntity == null) {
            return Collections.emptyList();
        }
        Query query = entityManager.createQuery("SELECT s FROM SchemaEntity s WHERE s.id IN :idList");
        query.setParameter("idList", subjectEntity.getSchemaIds());

        Map<Integer,SchemaEntity> map = new HashMap<>();
        for(SchemaEntity e : (List<SchemaEntity>)query.getResultList()) {
            map.put(e.getId(), e);
        }
        List<SchemaEntity> results =  new ArrayList<>(map.size());
        for(Integer i : subjectEntity.getSchemaIds()) {
            SchemaEntity schemaEntity = map.get(i);
            if(schemaEntity != null) {
                results.add(map.get(i));
            }
        }

        return results;
    }


    public SchemaEntity findSchema(int id) {

        EntityManager entityManager = threadEntityManager.get();

        SchemaEntity schemaEntity = null;

        schemaEntity = entityManager.find(SchemaEntity.class, id);

        return schemaEntity;
    }

    public SubjectEntity findSubject(String name) {

        EntityManager entityManager = threadEntityManager.get();

        SubjectEntity subjectEntity;

        subjectEntity = entityManager.find(SubjectEntity.class, name);

        return subjectEntity;
    }

    public List<String> listSubjectNames() {

        EntityManager entityManager = threadEntityManager.get();

        List<String> results = null;

        TypedQuery<String> query = entityManager.createNamedQuery("SubjectEntity.allNames", String.class);
        results = query.getResultList();

        return results;
    }

    private SubjectEntity ensureSubject(String subject) {
        EntityManager entityManager = threadEntityManager.get();
        SubjectEntity subjectEntity = entityManager.find(SubjectEntity.class, subject);
        if (subjectEntity == null) {
            subjectEntity = new SubjectEntity();
            subjectEntity.setName(subject);
            subjectEntity.setSchemaIds(new ArrayList<>());
            entityManager.persist(subjectEntity);
        }
        return subjectEntity;
    }

    public void setCompatibility(String subject, String compatibility) {

        EntityManager entityManager = threadEntityManager.get();

        SubjectEntity subjectEntity = ensureSubject(subject);

        if(!compatibility.equals(subjectEntity.getCompatibility())) {
            subjectEntity.setCompatibility(compatibility);
            entityManager.getTransaction().commit();
            entityManager.getTransaction().begin();
        }
    }

    public int register(String subject, String schema) {

        EntityManager entityManager = threadEntityManager.get();

        int schemaId = -1;

        SchemaEntity schemaEntity = findByHash(schema);
        if (schemaEntity == null) {
            schemaEntity = new SchemaEntity(schema);
            entityManager.persist(schemaEntity);
        }
        schemaId = schemaEntity.getId();

        SubjectEntity subjectEntity = ensureSubject(subject);

        boolean found = false;
        for(int id : subjectEntity.getSchemaIds()) {
            if(id == schemaId) {
                found = true;
                break;
            }
        }

        if(!found) {
            subjectEntity.getSchemaIds().add(schemaId);
            entityManager.getTransaction().commit();
            entityManager.getTransaction().begin();
        }

        return schemaId;
    }

    public void deleteSchemaAtIndex(SubjectEntity subjectEntity, int index) {
        EntityManager entityManager = threadEntityManager.get();
        if(subjectEntity.getSchemaIds().get(index) != 0) {
            subjectEntity.getSchemaIds().set(index, 0);
            entityManager.getTransaction().commit();
            entityManager.getTransaction().begin();
        }
    }

    public List<Integer> deleteAllSchemasFromSubject(SubjectEntity subjectEntity) {
        EntityManager entityManager = threadEntityManager.get();

        List<Integer> schemaIds = subjectEntity.getSchemaIds();
        ArrayList<Integer> versions = new ArrayList<>(schemaIds.size());
        for(int i = 0; i < schemaIds.size(); i++) {
            if(schemaIds.get(i) != 0) {
                // versions number from one, arrays from 0, so remember to offset...
                versions.add(i+1);
                schemaIds.set(i, 0);
            }
        }

        entityManager.getTransaction().commit();
        entityManager.getTransaction().begin();

        return versions;
    }
}
