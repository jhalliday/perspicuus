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

import org.junit.Test;
import org.mockito.ArgumentMatcher;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * Unit tests for NamedLock, run against in-memory DummyStore.
 *
 * @since 2012-12
 * @author Jonathan Halliday (jonathan.halliday@redhat.com)
 */
public class TestLock {

    @Test
    public void uncontendedAcquireAndRelease() {

        Store store = spy(new DummyStore());

        List<UUIDTuple> state = store.read("testKey");
        assertTrue(state.isEmpty());

        Lock lock = new NamedLock(store, "testKey", 30, TimeUnit.SECONDS);

        reset(store);
        lock.lock();
        verify(store).write(eq("testKey"), (UUID)any(), eq(Collections.<UUID>emptyList()), eq(30));
        verify(store).read("testKey");

        state = store.read("testKey");
        assertEquals(1, state.size());
        assertTrue(state.get(0).getAckList().isEmpty());

        reset(store);
        lock.unlock();
        verify(store).delete(eq("testKey"), (UUID)any());

        state = store.read("testKey");
        assertTrue(state.isEmpty());
    }

    @Test
    public void contendedAcquireAndRelease() {

        Store store = spy(new DummyStore());

        List<UUIDTuple> state = store.read("testKey");
        assertTrue(state.isEmpty());

        List<UUID> ackList = Collections.emptyList();
        UUID blockingUUID = new UUID(0, 0); // should sort first in jdk comparator, i.e. create a high precedence claim.

        store.write("testKey", blockingUUID, ackList, 30);
        state = store.read("testKey");
        assertEquals(1, state.size());

        Timer timer = new Timer(true);
        DeletionTimerTask myTimerTask = new DeletionTimerTask(store, "testKey", blockingUUID);

        // WARNING: somewhat non-determinstic test, depends on thread scheduling
        // test expects the timer to be accurate to +/-  200ms
        // more specifically, to run between RELOAD_WAIT_MILLIS and 2*RELOAD_WAIT_MILLIS i.e. 500 - 1000ms
        timer.schedule(myTimerTask, 800);

        Lock lockA = new NamedLock(store, "testKey", 30, TimeUnit.SECONDS);

        reset(store);
        lockA.lock();
        verify(store).write(eq("testKey"), (UUID) any(), eq(Collections.<UUID>emptyList()), eq(30));
        verify(store).write(eq("testKey"), (UUID)any(), argThat(new ListOfSize(1)), eq(30));
        // expect reads at t=0, t=500, t=1000 per RELOAD_WAIT_MILLIS = 500
        verify(store, times(3)).read("testKey");
        verify(store).delete("testKey", blockingUUID);

        state = store.read("testKey");
        assertEquals(1, state.size());
        UUIDTuple tuple = state.get(0);

        assertNotEquals(blockingUUID, tuple.getClid());
        assertEquals(1, tuple.getAckList().size());
        assertEquals(blockingUUID, tuple.getAckList().get(0));

        reset(store);
        lockA.unlock();
        verify(store).delete(eq("testKey"), (UUID)any());

        state = store.read("testKey");
        assertTrue(state.isEmpty());
    }

    public class ListOfSize extends ArgumentMatcher<List> {

        private final int size;

        public ListOfSize(int size) {
            this.size = size;
        }

        @Override
        public boolean matches(Object list) {
            return ((List)list).size() == size;
        }
    }

    public class DeletionTimerTask extends TimerTask {

        private final Store store;
        private final String lockId;
        private final UUID clientLockId;


        public DeletionTimerTask(Store store, String lockId, UUID clientLockId) {
            this.store = store;
            this.lockId = lockId;
            this.clientLockId = clientLockId;
        }

        @Override
        public void run() {
            store.delete(lockId, clientLockId);
        }
    }
}
