/*
Copyright 2012 Red Hat, Inc. and/or its affiliates.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package org.jboss.perspicuus.waitchain;

import java.util.List;
import java.util.UUID;

/**
 * A backing store for Lock state.
 *
 * @since 2012-12
 * @author Jonathan Halliday (jonathan.halliday@redhat.com)
 */
public interface Store {

    /**
     * Write the given lock information to the store.
     *
     * @param lockId the logical name of the Lock, typically an id for the item being guarded
     * @param clientLockId the instance identifier for the client's lock claim
     * @param ackList the acknowledgement list for rival client lock ids
     * @param ttlSeconds the TTL for the lock data, in seconds.
     *                   A value of 0 indicates no timeout. This is not recommended.
     * @throws RuntimeException if the store is not certain to have been written
     */
    void write(String lockId, UUID clientLockId, List<UUID> ackList, int ttlSeconds) throws RuntimeException;
    // Note: this is functionally equivalent to: write(String, UUIDTuple, long), which would arguable be cleaner design.

    /**
     * Remove the specified lock information from the store.
     * Null-op if the information is not present.
     *
     * @param lockId the logical name of the Lock, typically an id for the item being guarded
     * @param clientLockId the instance identifier for the client's lock claim
     * @throws RuntimeException if the store is not certain to have been written
     */
    void delete(String lockId, UUID clientLockId) throws RuntimeException;

    /**
     * Returns information on existing claims for the specified lock.
     *
     * Items in the returned List will be in consistent order by clid, but that order is NOT guaranteed to
     * be consistent with UUID's natural ordering. Items in the individual ackLists will be likewise ordered.
     *
     * @param lockId the logical name of the Lock, typically an id for the item being guarded
     * @return a non-null List of items describing potentially competing claims for the lock
     * @throws RuntimeException if the store cannot be read successfully
     */
    List<UUIDTuple> read(String lockId) throws RuntimeException;
}
