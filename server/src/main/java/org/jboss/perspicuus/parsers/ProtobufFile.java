/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
 *
 * Derived in part from Protolock, Copyright (c) 2018 Steve Manuel <nilslice@gmail.com>, BSD 3-Clause License.
 */
package org.jboss.perspicuus.parsers;

import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Range;
import com.google.common.io.Files;
import com.squareup.wire.schema.Location;
import com.squareup.wire.schema.internal.parser.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Indexed representation of the data resulting from parsing a single .proto protobuf schema file,
 * used mainly for schema validation.
 *
 * @see <a href="https://github.com/nilslice/protolock">Protolock</a>
 * @see ProtobufCompatibilityChecker
 *
 * @since 2019-01
 * @author Jonathan Halliday (jonathan.halliday@redhat.com)
 */
public class ProtobufFile {

    private final ProtoFileElement element;

    private final Map<String, Set<Object>> reservedFields = new HashMap<>();

    private final Map<String, Map<String,FieldElement>> fieldMap = new HashMap<>();
    private final Map<String, Map<String,EnumConstantElement>> enumFieldMap = new HashMap<>();

    private final Map<String, Map<String,FieldElement>> mapMap = new HashMap<>();

    private final Map<String, Set<Object>> nonReservedFields = new HashMap<>();
    private final Map<String, Set<Object>> nonReservedEnumFields = new HashMap<>();

    private final Map<String, Map<Integer,String>> fieldsById = new HashMap<>();
    private final Map<String, Map<Integer,String>> enumFieldsById = new HashMap<>();

    private final Map<String, Set<String>> serviceRPCnames = new HashMap<>();
    private final Map<String, Map<String,String>> serviceRPCSignatures = new HashMap<>();

    public ProtobufFile(String data) {
        element = ProtoParser.parse(Location.get(""), data);
        buildIndexes();
    }

    public ProtobufFile(File file) throws IOException {
        Location location = Location.get(file.getAbsolutePath());
        List<String> data = Files.readLines(file, StandardCharsets.UTF_8);
        element = ProtoParser.parse(location, String.join("\n", data));
        buildIndexes();
    }

    /*
     * message name -> Set { Integer/tag || String/name }
     */
    public Map<String, Set<Object>> getReservedFields() {
        return reservedFields;
    }

    /*
     * message name -> Map { field name -> FieldElement }
     */
    public Map<String, Map<String, FieldElement>> getFieldMap() {
        return fieldMap;
    }

    /*
     * enum name -> Map { String/name -> EnumConstantElement }
     */
    public Map<String, Map<String, EnumConstantElement>> getEnumFieldMap() {
        return enumFieldMap;
    }

    /*
     * message name -> Map { field name -> FieldElement }
     */
    public Map<String, Map<String, FieldElement>> getMapMap() {
        return mapMap;
    }

    /*
     * message name -> Set { Integer/tag || String/name }
     */
    public Map<String, Set<Object>> getNonReservedFields() {
        return nonReservedFields;
    }

    /*
     * enum name -> Set { Integer/tag || String/name }
     */
    public Map<String, Set<Object>> getNonReservedEnumFields() {
        return nonReservedEnumFields;
    }

    /*
     * message name -> Map { field id -> field name }
     */
    public Map<String, Map<Integer, String>> getFieldsById() {
        return fieldsById;
    }

    /*
     * enum name -> Map { field id -> field name }
     */
    public Map<String, Map<Integer, String>> getEnumFieldsById() {
        return enumFieldsById;
    }

    /*
     * service name -> Set { rpc name }
     */
    public Map<String, Set<String>> getServiceRPCnames() {
        return serviceRPCnames;
    }

    /*
     * service name -> Map { rpc name -> method signature }
     */
    public Map<String, Map<String, String>> getServiceRPCSignatures() {
        return serviceRPCSignatures;
    }

    private void buildIndexes() {

        for(TypeElement typeElement : element.types()) {
            if(typeElement instanceof MessageElement) {

                MessageElement messageElement = (MessageElement)typeElement;
                processMessageElement("", messageElement);

            } else if(typeElement instanceof EnumElement) {

                EnumElement enumElement = (EnumElement)typeElement;
                processEnumElement("", enumElement);

            } else {
                throw new RuntimeException();
            }
        }

        for(ServiceElement serviceElement : element.services()) {
            Set<String> rpcNames = new HashSet<>();
            Map<String,String> rpcSignatures = new HashMap<>();
            for(RpcElement rpcElement : serviceElement.rpcs()) {
                rpcNames.add(rpcElement.name());

                String signature = rpcElement.requestType()+":"+rpcElement.requestStreaming()+"->"+rpcElement.responseType()+":"+rpcElement.responseStreaming();
                rpcSignatures.put(rpcElement.name(), signature);
            }
            if(!rpcNames.isEmpty()) {
                serviceRPCnames.put(serviceElement.name(), rpcNames);
                serviceRPCSignatures.put(serviceElement.name(), rpcSignatures);
            }

        }
    }

    private void processMessageElement(String scope, MessageElement messageElement) {

        // reservedFields
        Set<Object> reservedFieldSet = new HashSet<>();
        for(ReservedElement reservedElement : messageElement.reserveds()) {
            for(Object value : reservedElement.values()) {
                if(value instanceof Range) {
                    reservedFieldSet.addAll( ContiguousSet.create((Range)value, DiscreteDomain.integers()) );
                } else {
                    reservedFieldSet.add(value);
                }
            }
        }
        if(!reservedFieldSet.isEmpty()) {
            reservedFields.put(scope+messageElement.name(), reservedFieldSet);
        }

        // fieldMap, mapMap, FieldsIDName
        Map<String,FieldElement> fieldTypeMap = new HashMap<>();
        Map<String,FieldElement> mapMap = new HashMap<>();
        Map<Integer,String> idsToNames = new HashMap<>();
        for(FieldElement fieldElement : messageElement.fields()) {
            fieldTypeMap.put(fieldElement.name(), fieldElement);
            if(fieldElement.type().startsWith("map<")) {
                mapMap.put(fieldElement.name(), fieldElement);
            }
            idsToNames.put(fieldElement.tag(), fieldElement.name());
        }
        for(OneOfElement oneOfElement : messageElement.oneOfs()) {
            for(FieldElement fieldElement : oneOfElement.fields()) {
                fieldTypeMap.put(fieldElement.name(), fieldElement);
                if(fieldElement.type().startsWith("map<")) {
                    mapMap.put(fieldElement.name(), fieldElement);
                }
                idsToNames.put(fieldElement.tag(), fieldElement.name());
            }
        }

        if(!fieldTypeMap.isEmpty()) {
            fieldMap.put(scope+messageElement.name(), fieldTypeMap);
        }
        if(!mapMap.isEmpty()) {
            this.mapMap.put(scope+messageElement.name(), mapMap);
        }
        if(!idsToNames.isEmpty()) {
            fieldsById.put(scope+messageElement.name(), idsToNames);
        }

        // nonReservedFields
        Set<Object> fieldKeySet = new HashSet<>();
        for(FieldElement fieldElement : messageElement.fields()) {
            fieldKeySet.add(fieldElement.tag());
            fieldKeySet.add(fieldElement.name());
        }
        for(OneOfElement oneOfElement : messageElement.oneOfs()) {
            for(FieldElement fieldElement : oneOfElement.fields()) {
                fieldKeySet.add(fieldElement.tag());
                fieldKeySet.add(fieldElement.name());
            }
        }


        if(!fieldKeySet.isEmpty()) {
            nonReservedFields.put(scope+messageElement.name(), fieldKeySet);
        }

        for(TypeElement typeElement : messageElement.nestedTypes()) {
            if(typeElement instanceof MessageElement) {
                processMessageElement(messageElement.name()+".", (MessageElement)typeElement);
            } else if(typeElement instanceof EnumElement) {
                processEnumElement(messageElement.name()+".", (EnumElement)typeElement);
            }
        }
    }

    private void processEnumElement(String scope, EnumElement enumElement) {

        // TODO reservedEnumFields - wire doesn't preserve these
        // https://github.com/square/wire/issues/797 RFE: capture EnumElement reserved info

        // enumFieldMap, enumFieldsIDName, nonReservedEnumFields
        Map<String,EnumConstantElement> map = new HashMap<>();
        Map<Integer,String> idsToNames = new HashMap<>();
        Set<Object> fieldKeySet = new HashSet<>();
        for(EnumConstantElement enumConstantElement : enumElement.constants()) {
            map.put(enumConstantElement.name(), enumConstantElement);
            idsToNames.put(enumConstantElement.tag(), enumConstantElement.name());

            fieldKeySet.add(enumConstantElement.tag());
            fieldKeySet.add(enumConstantElement.name());
        }
        if(!map.isEmpty()) {
            enumFieldMap.put(scope+enumElement.name(), map);
        }
        if(!idsToNames.isEmpty()) {
            enumFieldsById.put(scope+enumElement.name(), idsToNames);
        }
        if(!fieldKeySet.isEmpty()) {
            nonReservedEnumFields.put(scope+enumElement.name(), fieldKeySet);
        }
    }
}
