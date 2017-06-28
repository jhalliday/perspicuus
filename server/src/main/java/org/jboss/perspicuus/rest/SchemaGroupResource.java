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
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.jboss.logging.Logger;
import org.jboss.perspicuus.storage.StorageManager;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.*;
import java.util.Set;

/**
 * REST API for managing groups of schemas.
 *
 * @since 2017-03
 * @author Jonathan Halliday (jonathan.halliday@redhat.com)
 */
@Api(value = "groups")
@Path("/")
@Produces({"application/vnd.schemaregistry.v1+json",
        "application/vnd.schemaregistry+json; qs=0.9",
        "application/json; qs=0.5"})
@Consumes({"application/vnd.schemaregistry.v1+json",
        "application/vnd.schemaregistry+json",
        "application/json", "application/octet-stream"})
public class SchemaGroupResource {

    private static final Logger logger = Logger.getLogger(SchemaGroupResource.class);

    @Inject
    StorageManager storageManager;

    @ApiOperation(value = "Retrieve group member schema Ids")
    @ApiResponses(
            @ApiResponse(code = 404, message = "Not Found")
    )
    @GET
    @Path("/groups/{groupId}")
    @RolesAllowed("catalog_user")
    public Set<Integer> getSchemaGroupMembers(@PathParam("groupId") int groupId) {
        logger.debugv("getSchemaGroupMembers {0}", groupId);

        Set<Integer> resultSet = storageManager.getSchemaGroupMembers(groupId);
        if(resultSet == null) {
            throw new NotFoundException();
        }

        return resultSet;
    }

    @ApiOperation(value = "Create a new group")
    @POST
    @Path("/groups")
    @RolesAllowed("catalog_user")
    public int registerGroup() {
        logger.debugv("registerGroup");

        return storageManager.registerGroup();
    }

    @ApiOperation(value = "Add a schema to a group")
    @ApiResponses(
            @ApiResponse(code = 404, message = "Not Found")
    )
    @PUT
    @Path("/groups/{groupId}/{schemaId}")
    @RolesAllowed("catalog_user")
    public void addSchemaToGroup(@PathParam("groupId") int groupId, @PathParam("schemaId") int schemaId) {
        logger.debugv("addSchemaToGroup {0} {1}", groupId, schemaId);

        boolean success = storageManager.addSchemaToGroup(groupId, schemaId);
        if(!success) {
            throw new NotFoundException();
        }
    }

    @ApiOperation(value = "Remove a schema from a group")
    @ApiResponses(
            @ApiResponse(code = 404, message = "Not Found")
    )
    @DELETE
    @Path("/groups/{groupId}/{schemaId}")
    @RolesAllowed("catalog_user")
    public void removeSchemaFromGroup(@PathParam("groupId") int groupId, @PathParam("schemaId") int schemaId) {
        logger.debugv("removeSchemaFromGroup {0} {1}", groupId, schemaId);

        boolean success = storageManager.removeSchemaFromGroup(groupId, schemaId);
        if(!success) {
            throw new NotFoundException();
        }
    }

}
