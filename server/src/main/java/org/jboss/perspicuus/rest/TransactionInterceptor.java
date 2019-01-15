/*
 * Copyright 2017-2019 Red Hat, Inc. and/or its affiliates.
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

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * JAX-RS Filter for handling JPA session and transaction context in a one-per-request fashion.
 *
 * @since 2017-02
 * @author Jonathan Halliday (jonathan.halliday@redhat.com)
 */
@Provider
public class TransactionInterceptor implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger logger = Logger.getLogger(TransactionInterceptor.class);

    private static final ThreadLocal<Integer> reentrantCounter = new ThreadLocal<>();

    @Inject
    StorageManager storageManager;

    @Override
    public void filter(ContainerRequestContext containerRequestContext) throws IOException {
        logger.debugv("inbound");

        Integer count = reentrantCounter.get();
        if(count != null) {
            reentrantCounter.set(count+1);
            Thread.dumpStack();
            return;
        }
        reentrantCounter.set(1);

        logger.debugv("inbound - threadInit");
        storageManager.threadInit();
    }

    @Override
    public void filter(ContainerRequestContext containerRequestContext, ContainerResponseContext containerResponseContext) throws IOException {
        logger.debugv("outbound");

        Integer count = reentrantCounter.get();

        if(count == null) {
            // occurs where the inbound interceptor chain was not called, because the path didn't match any defined jax-rs url pattern.
            return;
        }

        if(count > 1) {
            reentrantCounter.set(count-1);
            return;
        }
        reentrantCounter.remove();

        logger.debugv("outbound - threadCleanup");
        storageManager.threadCleanup();
    }
}
