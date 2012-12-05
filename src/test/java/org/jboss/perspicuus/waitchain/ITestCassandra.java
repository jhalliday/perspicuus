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

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.KeyspaceMetadata;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.TableMetadata;
import com.datastax.driver.core.exceptions.DriverException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * Tests for NamedLock, run against CassandraStore.
 *
 * @since 2012-12
 * @author Jonathan Halliday (jonathan.halliday@redhat.com)
 */
public class ITestCassandra {

    private static Cluster cluster;
    private static Session session;

    @BeforeClass
    public static void setUpOnce() throws DriverException
    {
        cluster = new Cluster.Builder()
            .addContactPoints("localhost")
            .build();

        cluster.getMetadata().getKeyspaces();
        boolean keyspaceExists = false;
        boolean tableExists = false;
        // .getKeyspace will throw an exception if not exists. /sigh.
        for(KeyspaceMetadata keyspaceMetadata : cluster.getMetadata().getKeyspaces()) {
            if(keyspaceMetadata.getName().equals("waitchain")) {
                keyspaceExists = true;
                for(TableMetadata tableMetadata : keyspaceMetadata.getTables()) {
                    if(tableMetadata.getName().equals("locks")) {
                        tableExists = true;
                        break;
                    }
                }
                break;
            }
        }

        if(!keyspaceExists) {
            session = cluster.connect();
            session.execute("CREATE KEYSPACE waitchain WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1 }");
            session.shutdown();
            session = cluster.connect("waitchain");
        }

        if(!tableExists) {
            session.execute("CREATE TABLE locks (lid text, clid uuid, acklist list<uuid>, PRIMARY KEY(lid, clid))");
        }
    }

    @AfterClass
    public static void tearDownOnce() throws DriverException {
        session.execute("DROP TABLE locks");
        session.shutdown();
    }

    @Test
    public void testLock() throws Exception {

        // TODO asserts

        Store store = new CassandraStore(session);

        Lock lock = new NamedLock(store, "testKey", 30, TimeUnit.SECONDS);

        System.out.println(""+new Date()+" locking...");

        lock.lock();

        System.out.println(""+new Date()+" locked, sleeping");

        Thread.sleep(20*1000);

        System.out.println(""+new Date()+" awake, relocking");

        Lock rivalLock = new NamedLock(store, "testKey", 30, TimeUnit.SECONDS);
        rivalLock.lock();

        System.out.println(""+new Date()+" relocked");

        lock.unlock();
        rivalLock.unlock();

        System.out.println(""+new Date()+" unlocked");
    }
}
