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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.jboss.logging.Logger;
import org.jboss.perspicuus.storage.StorageManager;

import javax.inject.Inject;
import javax.ws.rs.*;
import java.util.List;

/**
 * API for searching stored schemas to find those matching given criteria.
 *
 * @since 2017-02
 * @author Jonathan Halliday (jonathan.halliday@redhat.com)
 */
@Api(value = "recommendations")
@Path("/")
@Produces({"application/vnd.schemaregistry.v1+json",
        "application/vnd.schemaregistry+json; qs=0.9",
        "application/json; qs=0.5"})
@Consumes({"application/vnd.schemaregistry.v1+json",
        "application/vnd.schemaregistry+json",
        "application/json", "application/octet-stream"})
public class SchemaRecommendationResource {

    private static final Logger logger = Logger.getLogger(SchemaRecommendationResource.class);

    @Inject
    StorageManager storageManager;

    @ApiOperation(value = "Retrieve Ids of schemas matching the query term")
    @GET
    @Path("/schemas/matching/{searchTerm}")
    public List<Integer> findMatchingSchemaIds(@PathParam("searchTerm") String searchTerm) {
        logger.debugv("findMatchingSchemaIds {0}", searchTerm);

        List<Integer> ids = storageManager.findMatchingSchemaIds(searchTerm);
        return ids;
    }

    @ApiOperation(value = "Retrieve Ids of schemas having similarity to the given schema")
    @GET
    @Path("/schemas/similar/{searchTerm}")
    public List<Integer> findSimilarSchemaIds(@PathParam("searchTerm") Integer id) {
        logger.debugv("findSimilarSchemaIds {0}", id);

        List<Integer> ids = storageManager.findSimilarSchemaIds(id);
        return ids;
    }

}