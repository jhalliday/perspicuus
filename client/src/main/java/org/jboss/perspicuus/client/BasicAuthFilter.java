/*
 * Copyright 2017, 2019 Red Hat, Inc. and/or its affiliates.
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
package org.jboss.perspicuus.client;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Request filter to add Basic Authentication header to outbound REST API calls from the client.
 *
 * @since 2017-06
 * @author Jonathan Halliday (jonathan.halliday@redhat.com)
 */
public class BasicAuthFilter implements ClientRequestFilter {

    private final String header;

    public BasicAuthFilter(String username, String password) {
        String userpass = username+":"+password;
        header = "Basic "+ DatatypeConverter.printBase64Binary(userpass.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void filter(ClientRequestContext clientRequestContext) throws IOException {
        clientRequestContext.getHeaders().add("Authorization", header);
    }
}