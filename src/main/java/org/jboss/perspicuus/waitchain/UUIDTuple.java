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
 * A representation of a client's claim to a lock and its associated acknowledgement set.
 *
 * Instances are considered equal if they represent information for the same client, regardless
 * of the content of that information.
 *
 * @since 2012-12
 * @author Jonathan Halliday (jonathan.halliday@redhat.com)
 */
public class UUIDTuple {

    private final UUID clid;
    private volatile List<UUID> ackList;

    public UUIDTuple(UUID clid, List<UUID> ackList) {
        this.clid = clid;
        this.ackList = ackList;
    }

    public UUID getClid() {
        return clid;
    }

    public List<UUID> getAckList() {
        return ackList;
    }

    /**
     * Replaces the existing ac
     *
     * @param ackList
     */
    public void reload(List<UUID> ackList) {
        this.ackList = ackList;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UUIDTuple uuidTuple = (UUIDTuple) o;

        if (!clid.equals(uuidTuple.clid)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return clid.hashCode();
    }
}
