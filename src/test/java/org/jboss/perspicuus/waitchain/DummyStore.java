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

import java.util.*;

/**
 * A backing store for Lock state using simple in-memory storage for test harness purposes.
 *
 * Note that the sort ordering used is based on JDK type comparison rather than Cassandra type comparison,
 * so results will be valid per the Store interface spec, but not necessarily identical to Cassandra.
 *
 * @since 2012-12
 * @author Jonathan Halliday (jonathan.halliday@redhat.com)
 */
public class DummyStore implements Store {

    private final Map<String,List<UUIDTuple>> state = new HashMap<>();

    @Override
    public synchronized void write(String lockId, UUID clientLockId, List<UUID> ackList, int ttlSeconds) throws RuntimeException {

        List<UUIDTuple> list = state.get(lockId);
        if(list == null) {
            list = new LinkedList<>();
            state.put(lockId, list);
        }

        UUIDTuple item = new UUIDTuple(clientLockId, new LinkedList<>(ackList));

        list.remove(item);
        list.add(item);
    }

    @Override
    public synchronized void delete(String lockId, UUID clientLockId) throws RuntimeException {

        List<UUIDTuple> list = state.get(lockId);
        if(list != null) {
            UUIDTuple item = new UUIDTuple(clientLockId, null);
            list.remove(item);
        }
    }

    @Override
    public synchronized List<UUIDTuple> read(String lockId) throws RuntimeException {

        List<UUIDTuple> results = new LinkedList<>();
        List<UUIDTuple> list = state.get(lockId);

        if(list != null) {
            results.addAll(list);
            Collections.sort(results, UUIDTupleComparator);
            for(UUIDTuple item : results) {
                Collections.sort(item.getAckList());
            }
        }

        return results;
    }

    private static final Comparator<UUIDTuple> UUIDTupleComparator = new Comparator<UUIDTuple>() {
        @Override
        public int compare(UUIDTuple o1, UUIDTuple o2) {
            return o1.getClid().compareTo(o2.getClid());
        }
    };
}
