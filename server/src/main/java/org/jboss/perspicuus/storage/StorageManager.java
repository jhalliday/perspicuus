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

import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.dsl.QueryBuilder;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

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

    public void deleteSubject(SubjectEntity subjectEntity) {
        EntityManager entityManager = threadEntityManager.get();
        entityManager.remove(subjectEntity);
        entityManager.getTransaction().commit();
        entityManager.getTransaction().begin();
    }

    public TagCollectionEntity getTags(int id) {

        EntityManager entityManager = threadEntityManager.get();

        TagCollectionEntity tagCollectionEntity = null;

        tagCollectionEntity = entityManager.find(TagCollectionEntity.class, id);

        return tagCollectionEntity;
    }

    public Set<Integer> getSchemaGroupMembers(int groupId) {
        EntityManager entityManager = threadEntityManager.get();

        SchemaGroupEntity schemaGroupEntity = entityManager.find(SchemaGroupEntity.class, groupId);
        if(schemaGroupEntity == null) {
            return null;
        }

        return schemaGroupEntity.getSchemaIds();
    }

    public int registerGroup() {
        EntityManager entityManager = threadEntityManager.get();

        SchemaGroupEntity schemaGroupEntity = new SchemaGroupEntity();

        entityManager.persist(schemaGroupEntity);
        int id = schemaGroupEntity.getId();

        entityManager.getTransaction().commit();
        entityManager.getTransaction().begin();

        return id;
    }

    public boolean addSchemaToGroup(int groupId, int memberId) {
        return alterSchemaGroup(groupId, memberId, true);
    }

    public boolean removeSchemaFromGroup(int groupId, int memberId) {
        return alterSchemaGroup(groupId, memberId, false);
    }

    protected boolean alterSchemaGroup(int groupId, int memberId, boolean addition) {

        EntityManager entityManager = threadEntityManager.get();

        SchemaGroupEntity schemaGroupEntity = entityManager.find(SchemaGroupEntity.class, groupId);
        if(schemaGroupEntity == null) {
            return false;
        }

        if(addition) {
            schemaGroupEntity.getSchemaIds().add(memberId);
        } else {
            schemaGroupEntity.getSchemaIds().remove(memberId);
        }
        entityManager.persist(schemaGroupEntity);

        entityManager.getTransaction().commit();
        entityManager.getTransaction().begin();

        return true;
    }

    public void updateTag(int id, String key, String value) {

        EntityManager entityManager = threadEntityManager.get();

        TagCollectionEntity tagCollectionEntity = entityManager.find(TagCollectionEntity.class, id);
        if(tagCollectionEntity == null) {
            tagCollectionEntity = new TagCollectionEntity();
            tagCollectionEntity.setId(id);
            tagCollectionEntity.setTags(new HashMap<>());
        }
        if(value != null) {
            tagCollectionEntity.getTags().put(key, value);
        } else {
            tagCollectionEntity.getTags().remove(key);
        }
        entityManager.persist(tagCollectionEntity);

        entityManager.getTransaction().commit();
        entityManager.getTransaction().begin();
    }

    @SuppressWarnings("unchecked")
    public List<Integer> findMatchingSchemaIds(String searchTerm) {

        FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(threadEntityManager.get());

        QueryBuilder queryBuilder = fullTextEntityManager.getSearchFactory().buildQueryBuilder().forEntity(SchemaEntity.class).get();

        org.apache.lucene.search.Query luceneQuery = queryBuilder.keyword().onField("content").matching(searchTerm).createQuery();

        Query fullTextQuery = fullTextEntityManager.createFullTextQuery(luceneQuery);

        List<SchemaEntity> results = fullTextQuery.getResultList();

        List<Integer> ids = new ArrayList<>(results.size());

        for(SchemaEntity schemaEntity : results) {
            ids.add(schemaEntity.getId());
        }

        return ids;
    }

    @SuppressWarnings("unchecked")
    public List<Integer> findSimilarSchemaIds(int id) {

        FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(threadEntityManager.get());

        QueryBuilder queryBuilder = fullTextEntityManager.getSearchFactory().buildQueryBuilder().forEntity(SchemaEntity.class).get();

        org.apache.lucene.search.Query luceneQuery = queryBuilder.moreLikeThis().comparingField("content").toEntityWithId(id).createQuery();

        Query fullTextQuery = fullTextEntityManager.createFullTextQuery(luceneQuery);

        List<SchemaEntity> results = fullTextQuery.getResultList();

        List<Integer> ids = new ArrayList<>();

        for(SchemaEntity schemaEntity : results) {
            ids.add(schemaEntity.getId());
        }

        return ids;
    }
}
