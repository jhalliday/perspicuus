/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
 *
 * Derived in part from Protolock, Copyright (c) 2018 Steve Manuel <nilslice@gmail.com>, BSD 3-Clause License.
 */
package org.jboss.perspicuus.parsers;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for Protobuf schema compatibility validation functions.
 *
 * @see <a href="https://github.com/nilslice/protolock">Protolock</a>
 * @see ProtobufCompatibilityChecker
 *
 * @since 2019-01
 * @author Jonathan Halliday (jonathan.halliday@redhat.com)
 */
public class ProtobufCompatibilityTest {

    // https://github.com/square/wire/issues/797 RFE: capture EnumElement reserved info

    private ProtobufFile loadProtoFile(String filename) {
        InputStream inputStream = ProtobufCompatibilityTest.class.getResourceAsStream("/proto/"+filename);
        String data = new BufferedReader(new InputStreamReader(inputStream))
                .lines().collect(Collectors.joining("\n"));
        ProtobufFile protobufFile = new ProtobufFile(data);
        return protobufFile;
    }

    private ProtobufCompatibilityChecker getChecker(String currentFile, String updatedFile) {
        ProtobufFile current = loadProtoFile(currentFile);
        ProtobufFile updated = loadProtoFile(updatedFile);
        ProtobufCompatibilityChecker protobufCompatibilityChecker = new ProtobufCompatibilityChecker(current, updated);
        return protobufCompatibilityChecker;
    }

    @Test
    public void testParser() {
        ProtobufFile protobufFile = loadProtoFile("simple.proto");
        assertNotNull(protobufFile);
    }

    @Test
    public void testNoUsingReservedFields() {

        // TODO +6 for for enums
        assertEquals(9, getChecker("noUsingReservedFields.proto", "usingReservedFields.proto").checkNoUsingReservedFields());

        assertEquals(0, getChecker( "usingReservedFields.proto", "noUsingReservedFields.proto").checkNoUsingReservedFields());
    }

    @Test
    public void testNoRemovingReservedFields() {

        // TODO +4 for enums
        assertEquals(9, getChecker("noRemoveReservedFields.proto", "removeReservedFields.proto").checkNoRemovingReservedFields());

        assertEquals(0, getChecker( "removeReservedFields.proto", "noRemoveReservedFields.proto").checkNoRemovingReservedFields());
    }

    @Test
    public void testNoRemovingFieldsWithoutReserve() {

        // TODO +3 for enums
        assertEquals(6, getChecker("noRemovingFieldsWithoutReserve.proto", "removingFieldsWithoutReserve.proto").checkNoRemovingFieldsWithoutReserve());

        assertEquals(0, getChecker( "removingFieldsWithoutReserve.proto", "removingFieldsWithoutReserve.proto").checkNoRemovingFieldsWithoutReserve());
    }

    @Test
    public void testNoChangingFieldIDs() {

        assertEquals(7, getChecker("noChangeFieldIDs.proto", "changeFieldIDs.proto").checkNoChangingFieldIDs());

        assertEquals(0, getChecker( "changeFieldIDs.proto", "changeFieldIDs.proto").checkNoChangingFieldIDs());
    }

    @Test
    public void testNoChangingFieldTypes() {

        assertEquals(6, getChecker("noChangingFieldTypes.proto", "changingFieldTypes.proto").checkNoChangingFieldTypes());

        assertEquals(0, getChecker( "changingFieldTypes.proto", "changingFieldTypes.proto").checkNoChangingFieldTypes());
    }

    @Test
    public void testNoChangingFieldNames() {

        assertEquals(8, getChecker("noChangingFieldNames.proto", "changingFieldNames.proto").checkNoChangingFieldNames());

        assertEquals(0, getChecker( "changingFieldNames.proto", "changingFieldNames.proto").checkNoChangingFieldNames());
    }

    @Test
    public void testNoRemovingServiceRPCs() {

        assertEquals(2, getChecker("noRemovingServicesRPCs.proto", "removingServicesRPCs.proto").checkNoRemovingServiceRPCs());

        assertEquals(0, getChecker( "removingServicesRPCs.proto", "noRemovingServicesRPCs.proto").checkNoRemovingServiceRPCs());
    }

    @Test
    public void testNoChangingRPCSignature() {

        assertEquals(2, getChecker("noChangingRPCSignature.proto", "changingRPCSignature.proto").checkNoChangingRPCSignature());

        assertEquals(0, getChecker( "changingRPCSignature.proto", "changingRPCSignature.proto").checkNoChangingRPCSignature());
    }

    @Test
    public void testNoConflictSameNameNestedMessages() {

        ProtobufCompatibilityChecker checker = getChecker("noConflictSameNameNestedMessages.proto", "noConflictSameNameNestedMessages.proto");
        assertEquals(0, checker.checkNoUsingReservedFields());
    }

    @Test
    public void testShouldConflictReusingFieldsNestedMessages() {

        assertEquals(1, getChecker("noConflictSameNameNestedMessages.proto", "shouldConflictNestedMessage.proto").checkNoUsingReservedFields());
    }


}
