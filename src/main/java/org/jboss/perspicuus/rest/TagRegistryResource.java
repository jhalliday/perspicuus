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
package org.jboss.perspicuus.rest;

import org.jboss.logging.Logger;
import org.jboss.perspicuus.storage.StorageManager;
import org.jboss.perspicuus.storage.TagCollectionEntity;

import javax.inject.Inject;
import javax.ws.rs.*;
import java.util.HashMap;
import java.util.Map;

/**
 * REST API for the tag registry.
 *
 * @since 2017-02
 * @author Jonathan Halliday (jonathan.halliday@redhat.com)
 */
@Path("/")
@Produces({"application/vnd.schemaregistry.v1+json",
        "application/vnd.schemaregistry+json; qs=0.9",
        "application/json; qs=0.5"})
@Consumes({"application/vnd.schemaregistry.v1+json",
        "application/vnd.schemaregistry+json",
        "application/json", "application/octet-stream"})
public class TagRegistryResource {

    private static final Logger logger = Logger.getLogger(SchemaRegistryResource.class);

    @Inject
    StorageManager storageManager;

    @GET
    @Path("/tags/schemas/{id}")
    public Map<String,String> getTagsForSchema(@PathParam("id") Long id) {
        logger.debugv("getTagsForSchema {0}", id);

        TagCollectionEntity tagCollectionEntity = storageManager.getTags(id);

        if(tagCollectionEntity == null) {
            throw new NotFoundException();
        }

        return tagCollectionEntity.tags;
    }

    @GET
    @Path("/tags/schemas/{id}/{key}")
    public Map<String,String> getTagForSchema(@PathParam("id") Long id, @PathParam("key") String key) {
        logger.debugv("getTagForSchema {0} {1}", id, key);

        Map<String,String> tags = getTagsForSchema(id);

        String value = tags.get(key);

        if(value == null) {
            throw new NotFoundException();
        }

        Map<String,String> result = new HashMap<>();
        result.put(key, value);

        return result;
    }

    @POST
    @Path("/tags/schemas/{id}/{key}")
    public void updateTagForSchema(@PathParam("id") Long id, @PathParam("key") String key, String request) {
        logger.debugv("updateTagForSchema {0} {1}", id, key);

        storageManager.updateTag(id, key, request);
    }

    @DELETE
    @Path("/tags/schemas/{id}/{key}")
    public void removeTagForSchema(@PathParam("id") Long id, @PathParam("key") String key) {
        logger.debugv("updateTagForSchema {0} {1}", id, key);

        storageManager.updateTag(id, key, null);
    }

}
