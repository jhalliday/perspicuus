/*
 * Copyright 2018, 2019 Red Hat, Inc. and/or its affiliates.
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

import io.swagger.annotations.*;
import org.jboss.logging.Logger;
import org.jboss.perspicuus.storage.SchemaEntity;
import org.jboss.perspicuus.parsers.SchemaParser;
import org.jboss.perspicuus.storage.StorageManager;
import org.jboss.perspicuus.storage.SubjectEntity;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST API for the schema compatibility functions.
 *
 * @since 2018-01
 * @author Jonathan Halliday (jonathan.halliday@redhat.com)
 */
@SwaggerDefinition(
        securityDefinition = @SecurityDefinition(basicAuthDefinitions = {@BasicAuthDefinition(key="basicAuth")})
)
@Api(value = "registry", authorizations = { @Authorization(value = "basicAuth") })
@Path("/")
@Produces({"application/vnd.schemaregistry.v1+json",
        "application/vnd.schemaregistry+json; qs=0.9",
        "application/json; qs=0.5"})
@Consumes({"application/vnd.schemaregistry.v1+json",
        "application/vnd.schemaregistry+json",
        "application/json", "application/octet-stream"})
public class SchemaCompatibilityResource {

    private static final Logger logger = Logger.getLogger(SchemaCompatibilityResource.class);

    @Inject
    StorageManager storageManager;

    @Inject
    SchemaRegistryResource schemaRegistryResource;

    private final String GLOBAL_SUBJECT_KEY = "_GLOBALCONFIG";
    private final String DEFAULT_COMPATIBILITY = "NONE";

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
    @Path("/compatibility/subjects/{subject}/versions/{version}")
    @RolesAllowed("catalog_user")
    public CompatibilityReport determineCompatibility(@PathParam("subject") String subject,
                                                      @PathParam("version") String version,
                                                      SchemaRegistryResource.TerseSchema request) {
        logger.debugv("determineCompatibility({0} {1} {2})", subject, version, request.schema);

        SchemaRegistryResource.VerboseSchema verboseSchema = schemaRegistryResource.getSchemaInScope(subject, version);
        String level = getInternalCompatibility(subject);

        boolean isCompatible = verboseSchema.schemaEntity.isCompatibleWith(level, request.schema);
        CompatibilityReport compatibilityReport = new CompatibilityReport(isCompatible);

        return compatibilityReport;
    }

    public boolean determineCompatibility(String subject, String proposedSchema) {
        List<SchemaEntity> existingSchemaEntities = storageManager.getSchemas(subject);
        if(existingSchemaEntities.isEmpty()) {
            return true;
        }
        List<String> existingSchemaStrings = existingSchemaEntities.stream().map(se -> se.getContent()).collect(Collectors.toList());
        SchemaParser schemaParser = existingSchemaEntities.get(existingSchemaEntities.size()-1).getSchemaType().getSchemaParser();
        String level = getInternalCompatibility(subject);
        boolean result = schemaParser.isCompatibleWith(level, existingSchemaStrings, proposedSchema);
        return result;
    }


    public static class Compatibility {
        public String compatibility;
    }

    public static class CompatibilityLevel {
        public final String compatibilityLevel;

        public CompatibilityLevel(String compatibilityLevel) {
            this.compatibilityLevel = compatibilityLevel;
        }
    }

    private boolean isValidCompatibilityLevel(String compatibility) {
        switch (compatibility) {
            case "NONE":
            case "BACKWARD":
            case "BACKWARD_TRANSITIVE":
            case "FORWARD":
            case "FULL":
                return true;
            default:
                return false;
        }
    }

    @ApiOperation(value = "Get the subject's schema compatibility setting")
    @GET
    @Path("/config/{subject}")
    @RolesAllowed("catalog_user")
    public CompatibilityLevel getCompatibility(@PathParam("subject") String subject) {
        logger.debugv("getCompatibility {0}", subject);

        SubjectEntity subjectEntity = storageManager.findSubject(subject);

        if(subjectEntity == null || subjectEntity.isDeleted()) {
            throw new NotFoundException();
        }

        CompatibilityLevel compatibilityLevel = getDefaultCompatibility();

        if(subjectEntity.getCompatibility() != null) {
            compatibilityLevel = new CompatibilityLevel(subjectEntity.getCompatibility());
        }

        return compatibilityLevel;
    }

    @ApiOperation(value = "Configure a subject's schema compatibility setting")
    @PUT
    @Path("/config/{subject}")
    @RolesAllowed("catalog_user")
    public Compatibility setCompatibility(@PathParam("subject") String subject, Compatibility requestedCompatibility) {
        logger.debugv("setCompatibility {0} {1}", subject, requestedCompatibility == null ? "null" : requestedCompatibility.compatibility);

        if(!isValidCompatibilityLevel(requestedCompatibility.compatibility)) {
            throw new ClientErrorException("Bad compatibility level", 422);
        }

        storageManager.setCompatibility(subject, requestedCompatibility.compatibility);

        return requestedCompatibility;
    }

    public String getInternalCompatibility(String subject) {

        SubjectEntity subjectEntity = storageManager.findSubject(subject);

        if(subjectEntity == null || subjectEntity.getCompatibility() == null) {
            return getDefaultCompatibility().compatibilityLevel;
        } else {
            return subjectEntity.getCompatibility();
        }
    }


    @ApiOperation(value = "Get the global default schema compatibility setting")
    @GET
    @Path("/config")
    @RolesAllowed("catalog_user")
    public CompatibilityLevel getDefaultCompatibility() {
        logger.debugv("getDefaultCompatibility");

        CompatibilityLevel compatibilityLevel = new CompatibilityLevel(DEFAULT_COMPATIBILITY);

        SubjectEntity subjectEntity = storageManager.findSubject(GLOBAL_SUBJECT_KEY);

        if(subjectEntity != null) {
            compatibilityLevel = new CompatibilityLevel(subjectEntity.getCompatibility());
        }

        return compatibilityLevel;
    }

    @ApiOperation(value = "Set the global default schema compatibility setting")
    @PUT
    @Path("/config")
    @RolesAllowed("catalog_user")
    public Compatibility setDefaultCompatibility(Compatibility requestedCompatibility) {
        return setCompatibility(GLOBAL_SUBJECT_KEY, requestedCompatibility);
    }
}
