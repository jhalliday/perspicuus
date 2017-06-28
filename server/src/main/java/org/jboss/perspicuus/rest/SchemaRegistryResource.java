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
import org.jboss.perspicuus.storage.SchemaEntity;
import org.jboss.perspicuus.storage.SubjectEntity;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * REST API for a schema registry.
 * Approximately API compatible with Confluent's schemaregistry.
 *
 * @since 2017-02
 * @author Jonathan Halliday (jonathan.halliday@redhat.com)
 */
@Api(value = "registry")
@Path("/")
@Produces({"application/vnd.schemaregistry.v1+json",
        "application/vnd.schemaregistry+json; qs=0.9",
        "application/json; qs=0.5"})
@Consumes({"application/vnd.schemaregistry.v1+json",
        "application/vnd.schemaregistry+json",
        "application/json", "application/octet-stream"})
public class SchemaRegistryResource {

    private static final Logger logger = Logger.getLogger(SchemaRegistryResource.class);

    @Inject
    StorageManager storageManager;

    // some request/response cases use an unadorned schema representation
    public static class TerseSchema {
        public String schema;
    }

    // some request/response use a schema decorated with context information
    public static class VerboseSchema {
        public String schema;
        public int id;
        public String subject;
        public int version;
    }

    public static class CustomNotFoundException extends NotFoundException {

        public static class ErrorWrapper {
            public int error_code;
            public String message;

            public ErrorWrapper(int error_code, String message) {
                this.error_code = error_code;
                this.message = message;
            }
        }

        public CustomNotFoundException() {
            super("Not Found");
        }

        public CustomNotFoundException(String message) {
            super(message);
        }

        @Override
        public Response getResponse() {
            String text = getMessage() != null ? getMessage() : Response.Status.NOT_FOUND.getReasonPhrase();
            ErrorWrapper errorWrapper = new ErrorWrapper(Response.Status.NOT_FOUND.getStatusCode(), text);
            return Response.status(Response.Status.NOT_FOUND).entity(errorWrapper).build();
        }
    }

    @ApiOperation(value = "Retrieve a schema by id number")
    @ApiResponses(
            @ApiResponse(code = 404, message = "Not Found")
    )
    @GET
    @Path("/schemas/ids/{id}")
    @RolesAllowed("catalog_user")
    public TerseSchema getSchema(@PathParam("id") Integer id) {
        logger.debugv("getSchema {0}", id);

        SchemaEntity schemaEntity = storageManager.findSchema(id);

        if(schemaEntity == null) {
            throw new CustomNotFoundException();
        }

        TerseSchema terseSchema = new TerseSchema();
        terseSchema.schema = schemaEntity.content;
        return terseSchema;
    }

    @ApiOperation(value = "Locate a schema within the given subject scope")
    @ApiResponses(
            @ApiResponse(code = 404, message = "Not Found")
    )
    @POST
    @Path("/subjects/{subject}")
    @RolesAllowed("catalog_user")
    public TerseSchema scopedSearch(@PathParam("subject") String subject, TerseSchema request) {
        logger.debugv("scopedSearch {0} {1}", subject, request.schema);

        SchemaEntity schemaEntity = storageManager.findByHash(request.schema);
        SubjectEntity subjectEntity = storageManager.findSubject(subject);

        if(schemaEntity == null || subjectEntity == null) {
            throw new CustomNotFoundException();
        }

        TerseSchema terseSchema = new TerseSchema();
        if(subjectEntity.schemaIds.contains(schemaEntity.id)) {
            terseSchema.schema = schemaEntity.content;
        }

        return terseSchema;
    }

    @ApiOperation(value = "List all known subjects")
    @GET
    @Path("/subjects")
    @RolesAllowed("catalog_user")
    public Set<String> listSubjectNames() {
        logger.debugv("listSubjectNames");

        List<String> resultList = storageManager.listSubjectNames();
        return new HashSet<>(resultList);
    }

    @ApiOperation(value = "List all schema ids for the given subject")
    @ApiResponses(
            @ApiResponse(code = 404, message = "Not Found")
    )
    @GET
    @Path("/subjects/{subject}/versions")
    @RolesAllowed("catalog_user")
    public List<Integer> listSubjectSchemaIds(@PathParam("subject") String subject) {
        logger.debugv("listSubjectSchemaIds {0}", subject);

        SubjectEntity subjectEntity = storageManager.findSubject(subject);

        if(subjectEntity == null) {
            throw new CustomNotFoundException();
        }

        return subjectEntity.schemaIds;
    }

    // 'latest' is a valid version, otherwise number 1-N
    @ApiOperation(value = "Retrieve a specific version of the schema for a given subject")
    @ApiResponses(
            @ApiResponse(code = 404, message = "Not Found")
    )
    @GET
    @Path("/subjects/{subject}/versions/{version}")
    @RolesAllowed("catalog_user")
    public VerboseSchema getSchemaInScope(@PathParam("subject") String subject,
                                          @PathParam("version") String version) {
        logger.debugv("getSchemaInScope {0} {1}", subject, version);

        SubjectEntity subjectEntity = storageManager.findSubject(subject);

        if(subjectEntity == null) {
            throw new CustomNotFoundException();
        }

        int schemaId = 0;
        int resolvedVersion = 0;
        if("latest".equalsIgnoreCase(version)) {
            resolvedVersion = subjectEntity.schemaIds.size();
            schemaId = subjectEntity.schemaIds.get(resolvedVersion-1);
        } else {
            resolvedVersion = Integer.parseInt(version);
            if(subjectEntity.schemaIds.size() >= resolvedVersion) {
                // versions number from one, arrays from 0, so remember to offset...
                schemaId = subjectEntity.schemaIds.get(resolvedVersion-1);
            }
        }

        SchemaEntity schemaEntity = storageManager.findSchema(schemaId);

        if(schemaEntity == null) {
            throw new CustomNotFoundException();
        }

        VerboseSchema verboseSchema = new VerboseSchema();
        verboseSchema.schema = schemaEntity.content;
        verboseSchema.id = schemaEntity.id;
        verboseSchema.subject = subject;
        verboseSchema.version = resolvedVersion;
        return verboseSchema;

    }

    public static class CompatibilityReport {
        public final boolean is_compatible;

        public CompatibilityReport(boolean isCompatible) {
            is_compatible = isCompatible;
        }
    }

    @ApiOperation(value = "Test compatibility of the provided schema against an existing one from the repository")
    @ApiResponses(
            @ApiResponse(code = 404, message = "Not Found")
    )
    @POST
    @Path("/subjects/{subject}/versions/{version}")
    @RolesAllowed("catalog_user")
    public CompatibilityReport determineCompatibility(@PathParam("subject") String subject,
                                                      @PathParam("version") String version,
                                                      TerseSchema request) {
        logger.debugv("determineCompatibility({0} {1} {2})", subject, version, request.schema);

        VerboseSchema verboseSchema = getSchemaInScope(subject, version);

        boolean isCompatible = SchemaEntity.isCompatibleWith(verboseSchema.schema, request.schema);

        CompatibilityReport compatibilityReport = new CompatibilityReport(isCompatible);

        return compatibilityReport;
    }

    public static class RegisterResponse {
        public final int id;

        public RegisterResponse(int id) {
            this.id = id;
        }
    }

    @ApiOperation(value = "Register a new schema, adding it to the given topic")
    @POST
    @Path("/subjects/{subject}/versions")
    @RolesAllowed("catalog_user")
    public RegisterResponse addSchema(@PathParam("subject") String subject, TerseSchema request) {
        logger.debugv("addSchema {0} {1}", subject, request);

        int id = storageManager.register(subject, request.schema);

        RegisterResponse registerResponse = new RegisterResponse(id);

        return registerResponse;
    }
}

