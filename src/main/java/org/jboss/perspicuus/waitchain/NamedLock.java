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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * This implementation provides an exclusive Lock whose state is held in the given Store.
 * Multiple instances created with the same lockId represent different logical locks.
 *
 * @see http://media.fightmymonster.com/Shared/docs/Wait%20Chain%20Algorithm.pdf  v111130.2
 * @see http://www.mail-archive.com/user@cassandra.apache.org/msg19117.html (thread)
 *
 * @since 2012-12
 * @author Jonathan Halliday (jonathan.halliday@redhat.com)
 */
public class NamedLock implements Lock {

    private static final List<UUID> emptyUUIDList = Collections.emptyList();

    private static long RELOAD_WAIT_MILLIS = 500;
    private static long MAX_CLOCK_DRIFT_MILLIS = 1;
    private static long UNLOCK_SLEEP_MILLIS = (2*MAX_CLOCK_DRIFT_MILLIS)+1;

    private final Store store;
    private final String lockId; // 'LID' in spec doc
    private final UUID clientLockId; // 'CLID' in spec doc

    private final int timeoutSeconds;
    private final long refreshIntervalMillis;

    private volatile Thread owningThread;

    /**
     * Create a new Lock for a given logical lockId (e.g. row name), in a given Store.
     * Once acquired, the lock will remain valid for at least the given duration.
     *
     * @param store the backing Store to use for Lock state storage.
     * @param lockId the logical name of the Lock, typically an id for the item being guarded.
     * @param timeout the minimum Lock duration expected.
     *                Note this is NOT an acquisition attempt timeout.
     *                A value of 0 indicates no timeout. This is not recommended.
     * @param unit the time unit of the timeout argument.
     */
    public NamedLock(Store store, String lockId, long timeout, TimeUnit unit) {
        this.store = store;
        this.lockId = lockId;
        this.clientLockId = UUID.randomUUID();

        long t = unit.toSeconds(timeout);
        if(t > Integer.MAX_VALUE) {
            t = Integer.MAX_VALUE;
        }
        this.timeoutSeconds = (int)t;

        // 80% of timeout, but minimum 10ms. may require tuning.
        long r = (timeoutSeconds*1000)*800;
        if(r < 10) {
            r = 10;
        }
        this.refreshIntervalMillis = r;
    }

    /**
     * Acquires the lock. May block indefinitely.
     * When called from a Thread that already hold the lock, this is a null-op.
     *
     * Note that if the lock acquisition is contested, the remaining TTL may be significantly less than
     * originally requested by the time this method returns.
     */
    @Override
    public synchronized void lock() {

        if(owningThread == Thread.currentThread()) {
            return;
        }
        // else...
        // if some other thread owns the lock we go via the Store for release


        store.write(lockId, clientLockId, emptyUUIDList, timeoutSeconds);

        List<UUIDTuple> canBeEarlier = store.read(lockId);

        if(canBeEarlier.size() <= 1) {
            // uncontended acquisition
            owningThread = Thread.currentThread();
            return;
        }

        long lastWriteAcksMillis = 0;
        while(true) {
            boolean recvAllAcks = true;
            for(UUIDTuple uuidTuple :  canBeEarlier) {
                if( (uuidTuple.getClid() != clientLockId) && (!uuidTuple.getAckList().contains(clientLockId)) ) {
                    // unacknowledged requests exist, lock is contended,
                    recvAllAcks = false;
                    break; // out of for loop
                }
            }

            if(recvAllAcks) {
                // wait precedence for contended lock is based on id ordering
                UUID nextClientLockId = canBeEarlier.get(0).getClid();
                if(nextClientLockId.equals(clientLockId)) {
                    // we have precedence, lock acquired.
                    owningThread = Thread.currentThread();
                    break; // out of while loop
                }
            }

            long currentMillis = System.currentTimeMillis();
            long intervalSinceLastWriteMillis = currentMillis - lastWriteAcksMillis;
            if(intervalSinceLastWriteMillis > refreshIntervalMillis) {
                List<UUID> list = extractClientLockIdList(canBeEarlier);
                store.write(lockId, clientLockId, list, timeoutSeconds);
                lastWriteAcksMillis = currentMillis;
            }

            try {
                Thread.sleep(RELOAD_WAIT_MILLIS);
            } catch(InterruptedException e) {
                // TODO
            }
            canBeEarlier = reload(canBeEarlier, store.read(lockId));
        }
    }

    private List<UUIDTuple> reload(List<UUIDTuple> originalList, List<UUIDTuple> updatedList) {

        Iterator<UUIDTuple> iterator = originalList.iterator();
        while(iterator.hasNext()) {
            UUIDTuple item = iterator.next();
            int i = updatedList.indexOf(item);
            if(i >= 0) {
                item.reload(updatedList.get(i).getAckList());
            } else {
                iterator.remove();
            }
        }
        return originalList;
    }

    private List<UUID> extractClientLockIdList(List<UUIDTuple> inputList) {
        List<UUID> outputList = new ArrayList<>(inputList.size()-1);
        for(UUIDTuple item : inputList) {
            // don't include ourselves
            if(item.getClid() != clientLockId) {
                outputList.add(item.getClid());
            }
        }
        return outputList;
    }

    /**
     * Acquires the lock unless the current thread is interrupted.
     * @throws InterruptedException if the current thread is interrupted while acquiring the lock
     */
    @Override
    public void lockInterruptibly() throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        lock();
    }

    /**
     * Acquires the lock only if it is free at the time of invocation.
     * @return true if the lock was acquired and false otherwise
     */
    @Override
    public boolean tryLock() {
        return false;  // TODO
    }

    /**
     * Acquires the lock if it is free within the given waiting time and the current thread has not been interrupted.
     *
     * @param time the maximum time to wait for the lock
     * @param unit the time unit of the time argument
     * @return true if the lock was acquired and false if the waiting time elapsed before the lock was acquired
     * @throws InterruptedException if the current thread is interrupted while acquiring the lock
     */
    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return false;  // TODO
    }

    /**
     * Releases the lock. This may be called from any thread.
     * Calling this method when the lock is not held is a null-op.
     */
    @Override
    public synchronized void unlock() {
        if(owningThread != null) {
            try {
                Thread.sleep(UNLOCK_SLEEP_MILLIS);
            } catch(InterruptedException e) {

            }
            store.delete(lockId, clientLockId);
            owningThread = null;
        }
    }

    /**
     * Returns a new Condition instance that is bound to this Lock instance.
     *
     * @return a new Condition instance for this Lock instance
     * @throws UnsupportedOperationException - this Lock implementation does not support conditions
     */
    @Override
    public Condition newCondition() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }
}
