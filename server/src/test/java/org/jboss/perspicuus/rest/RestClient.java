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

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;

/**
 * Client wrapper to decorate API requests with basic Authentication header.
 *
 * @since 2017-06
 * @author Jonathan Halliday (jonathan.halliday@redhat.com)
 */
public class RestClient {

    public static Client client;

    static {
        client = ClientBuilder.newClient();
        client.register(new BasicAuthFilter());
    }
}

class BasicAuthFilter implements ClientRequestFilter {

    @Override
    public void filter(ClientRequestContext clientRequestContext) throws IOException {
        String userpass = "testuser:testpass"; // per users.properties file
        String header = "Basic "+ DatatypeConverter.printBase64Binary(userpass.getBytes("UTF-8"));
        clientRequestContext.getHeaders().add("Authorization", header);
    }
}
