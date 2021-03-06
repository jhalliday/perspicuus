/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
 *
 * Derived in part from Protolock, Copyright (c) 2018 Steve Manuel <nilslice@gmail.com>, BSD 3-Clause License.
 */
package org.jboss.perspicuus.parsers;

import com.squareup.wire.schema.internal.parser.EnumConstantElement;
import com.squareup.wire.schema.internal.parser.FieldElement;

import java.util.*;

/**
 * Provides compatibility validation functions for changes between two versions of a Protobuf schema document.
 *
 * @see <a href="https://github.com/nilslice/protolock">Protolock</a>
 *
 * @since 2019-01
 * @author Jonathan Halliday (jonathan.halliday@redhat.com)
 */
public class ProtobufCompatibilityChecker {

    // TODO https://github.com/square/wire/issues/797 RFE: capture EnumElement reserved info

    private final ProtobufFile fileBefore;
    private final ProtobufFile fileAfter;

    public ProtobufCompatibilityChecker(ProtobufFile fileBefore, ProtobufFile fileAfter) {
        this.fileBefore = fileBefore;
        this.fileAfter = fileAfter;
    }

    public boolean validate() {
        int totalIssues = 0;
        totalIssues += checkNoUsingReservedFields();
        totalIssues += checkNoRemovingReservedFields();
        totalIssues += checkNoRemovingFieldsWithoutReserve();
        totalIssues += checkNoChangingFieldIDs();
        totalIssues += checkNoChangingFieldTypes();
        totalIssues += checkNoChangingFieldNames();
        totalIssues += checkNoRemovingServiceRPCs();
        totalIssues += checkNoChangingRPCSignature();
        return totalIssues == 0;
    }


    /**
     * Determine if any message's previously reserved fields or IDs are now being used as part of the same message.
     *
     * Note: TODO can't currently validate enum reserved fields, as the parser doesn't capture those.
     *
     * @return number of issues
     */
    public int checkNoUsingReservedFields() {

        int issues = 0;

        Map<String, Set<Object>> reservedFields = fileBefore.getReservedFields();
        Map<String, Set<Object>> nonReservedFields = fileAfter.getNonReservedFields();

        for(Map.Entry<String,Set<Object>> entry : nonReservedFields.entrySet()) {
            Set<Object> old = reservedFields.get(entry.getKey());
            if(old != null) {
                Set<Object> intersection = new HashSet<>(entry.getValue());
                intersection.retainAll(old);
                if(!intersection.isEmpty()) {
                    issues += intersection.size();
                }
            }
        }

        return issues;
    }

    /**
     * Determine if any reserved field has been removed.
     *
     * Note: TODO can't currently validate enum reserved fields, as the parser doesn't capture those.
     *
     * @return number of issues
     */
    public int checkNoRemovingReservedFields() {

        int issues = 0;

        Map<String, Set<Object>> before = fileBefore.getReservedFields();
        Map<String, Set<Object>> after = fileAfter.getReservedFields();

        for(Map.Entry<String, Set<Object>> entry : before.entrySet()) {
            Set<Object> afterKeys = after.get(entry.getKey());

            if(afterKeys != null) {
                Set<Object> intersection = new HashSet<>(entry.getValue());
                intersection.retainAll(afterKeys);

                int diff = entry.getValue().size() - intersection.size();
                if (diff != 0) {
                    issues += diff;
                }
            } else {
                issues += entry.getValue().size();
            }
        }

        return issues;
    }

    /**
     * Determine if any field has been removed without a corresponding reservation of that field name or ID.
     *
     * Note: TODO can't currently validate enum reserved fields, as the parser doesn't capture those.
     *
     * @return number of issues
     */
    public int checkNoRemovingFieldsWithoutReserve() {

        int issues = 0;

        Map<String, Map<String, FieldElement>> before = fileBefore.getFieldMap();
        Map<String, Map<String, FieldElement>> after = fileAfter.getFieldMap();

        Map<String, Set<Object>> afterReservedFields = fileAfter.getReservedFields();
        Map<String, Set<Object>> afterNonreservedFields = fileAfter.getNonReservedFields();

        for(Map.Entry<String, Map<String, FieldElement>> entry : before.entrySet()) {
            Set<String> removedFieldNames = new HashSet<>(entry.getValue().keySet());
            Map<String, FieldElement> updated = after.get(entry.getKey());
            if(updated != null) {
                removedFieldNames.removeAll(updated.keySet());
            }

            // count once for each non-reserved field name
            Set<Object> reserved = afterReservedFields.get(entry.getKey());
            Set<Object> nonreserved = afterNonreservedFields.get(entry.getKey());
            Set<String> nonReservedRemovedFieldNames = new HashSet<>(removedFieldNames);
            nonReservedRemovedFieldNames.removeAll(reserved);
            issues += nonReservedRemovedFieldNames.size();

            // count again for each non-reserved field id
            for(FieldElement fieldElement : entry.getValue().values()) {
                if(removedFieldNames.contains(fieldElement.name()) &&
                        !(reserved.contains(fieldElement.tag()) || nonreserved.contains(fieldElement.tag()))) {
                    issues++;
                }
            }
        }

        return issues;
    }

    /**
     * Determine if any field ID number has been changed.
     *
     * @return number of issues
     */
    public int checkNoChangingFieldIDs() {

        int issues = 0;

        Map<String, Map<String,FieldElement>> before = fileBefore.getFieldMap();
        Map<String, Map<String,FieldElement>> after = fileAfter.getFieldMap();

        for(Map.Entry<String, Map<String,FieldElement>> entry : before.entrySet()) {
            Map<String,FieldElement> afterMap = after.get(entry.getKey());

            if(afterMap != null) {
                for(Map.Entry<String,FieldElement> beforeKV : entry.getValue().entrySet()) {
                    FieldElement afterFE = afterMap.get(beforeKV.getKey());
                    if(beforeKV.getValue().tag() != afterFE.tag()) {
                        issues++;
                    }
                }
            }
        }

        Map<String, Map<String, EnumConstantElement>> beforeEnum = fileBefore.getEnumFieldMap();
        Map<String, Map<String, EnumConstantElement>> afterEnum = fileAfter.getEnumFieldMap();

        for(Map.Entry<String, Map<String,EnumConstantElement>> entry : beforeEnum.entrySet()) {
            Map<String,EnumConstantElement> afterMap = afterEnum.get(entry.getKey());

            if(afterMap != null) {
                for(Map.Entry<String,EnumConstantElement> beforeKV : entry.getValue().entrySet()) {
                    EnumConstantElement afterECE = afterMap.get(beforeKV.getKey());
                    if(beforeKV.getValue().tag() != afterECE.tag()) {
                        issues++;
                    }
                }
            }
        }

        return issues;
    }

    /**
     * Determine if any field type has been changed.
     *
     * @return number of issues
     */
    public int checkNoChangingFieldTypes() {

        int issues = 0;

        Map<String, Map<String,FieldElement>> before = fileBefore.getFieldMap();
        Map<String, Map<String,FieldElement>> after = fileAfter.getFieldMap();

        for(Map.Entry<String, Map<String,FieldElement>> entry : before.entrySet()) {
            Map<String,FieldElement> afterMap = after.get(entry.getKey());

            if(afterMap != null) {
                for(Map.Entry<String,FieldElement> beforeKV : entry.getValue().entrySet()) {
                    FieldElement afterFE = afterMap.get(beforeKV.getKey());
                    if(!beforeKV.getValue().type().equals(afterFE.type())) {
                        issues++;
                    }

                    if(!Objects.equals(beforeKV.getValue().label(), afterFE.label())) {
                        issues++;
                    }
                }
            }
        }

        return issues;
    }

    /**
     * Determine if any message's previous fields have been renamed.
     *
     * @return number of issues
     */
    public int checkNoChangingFieldNames() {

        int issues = 0;

        Map<String, Map<Integer, String>> before = new HashMap<>(fileBefore.getFieldsById());
        before.putAll(fileBefore.getEnumFieldsById());

        Map<String, Map<Integer, String>> after = new HashMap<>(fileAfter.getFieldsById());
        after.putAll(fileAfter.getEnumFieldsById());

        for(Map.Entry<String, Map<Integer, String>> entry : before.entrySet()) {
            Map<Integer, String> afterMap = after.get(entry.getKey());

            if(afterMap != null) {
                for(Map.Entry<Integer,String> beforeKV : entry.getValue().entrySet()) {
                    String nameAfter = afterMap.get(beforeKV.getKey());

                    if(!beforeKV.getValue().equals(nameAfter)) {
                        issues++;
                    }
                }
            }
        }

        return issues;
    }

    /**
     * Determine if any RPCs provided by a Service have been removed.
     *
     * @return number of issues
     */
    public int checkNoRemovingServiceRPCs() {

        int issues = 0;

        Map<String, Set<String>> before = fileBefore.getServiceRPCnames();
        Map<String, Set<String>> after = fileAfter.getServiceRPCnames();

        for(Map.Entry<String, Set<String>> entry : before.entrySet()) {
            Set<String> afterSet = after.get(entry.getKey());

            Set<String> diff = new HashSet<>(entry.getValue());
            if(afterSet != null) {
                diff.removeAll(afterSet);
            }

            issues += diff.size();
        }

        return issues;
    }

    /**
     * Determine if any RPC signature has been changed while using the same name.
     *
     * @return number of issues
     */
    public int checkNoChangingRPCSignature() {

        int issues = 0;

        Map<String, Map<String,String>> before = fileBefore.getServiceRPCSignatures();
        Map<String, Map<String,String>> after = fileAfter.getServiceRPCSignatures();

        for(Map.Entry<String, Map<String,String>> entry : before.entrySet()) {
            Map<String, String> afterMap = after.get(entry.getKey());

            if(afterMap != null) {
                for (Map.Entry<String, String> beforeKV : entry.getValue().entrySet()) {
                    String afterSig = afterMap.get(beforeKV.getKey());
                    if(!beforeKV.getValue().equals(afterSig)) {
                        issues++;
                    }
                }
            }
        }

        return issues;
    }

}
