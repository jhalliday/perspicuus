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

import com.datastax.driver.core.*;
import com.datastax.driver.core.exceptions.DriverException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * This implementation utilizes Cassandra 1.2 or later via. the CQL3 Java driver.
 *
 * @see http://cassandra.apache.org/
 * @see https://github.com/datastax/java-driver
 * @see http://www.datastax.com/docs/1.1/references/cql/index
 *
 * @since 2012-12
 * @author Jonathan Halliday (jonathan.halliday@redhat.com)
 */
public class CassandraStore implements Store {

    /*
     * CREATE KEYSPACE waitchain WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1 };
     * use waitchain;
     * CREATE TABLE locks (lid text, clid uuid, acklist list<uuid>, PRIMARY KEY(lid, clid));
     */

    private final Session session;
    private final PreparedStatement writeStatement;
    private final PreparedStatement deleteStatement;
    private final PreparedStatement scanStatement;

    public CassandraStore(Session session) {
        this.session = session;
        try {
            // TODO remove hardcoded TTL
            // https://issues.apache.org/jira/browse/CASSANDRA-4450 for TTL bind var in PreparedStatement
            writeStatement = session.prepare("INSERT INTO locks (lid, clid, ackList) VALUES (?, ?, ?)" +
                    " USING TTL 30");
            deleteStatement = session.prepare("DELETE FROM locks WHERE lid=? AND clid=?");
            scanStatement = session.prepare("SELECT clid, ackList FROM locks WHERE lid=?");
        } catch(DriverException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void write(String lockId, UUID clientLockId, List<UUID> ackList, int ttl) throws RuntimeException {
        BoundStatement boundStatement = new BoundStatement(writeStatement);
        boundStatement.setConsistencyLevel(ConsistencyLevel.QUORUM);
        boundStatement.setString(0, lockId);
        boundStatement.setUUID(1, clientLockId);
        boundStatement.setList(2, ackList);
        //boundStatement.setInt(3, ttl);
        executeOrThrow(boundStatement);
    }

    @Override
    public void delete(String lockId, UUID clientLockId) throws RuntimeException {
        BoundStatement boundStatement = new BoundStatement(deleteStatement);
        boundStatement.setConsistencyLevel(ConsistencyLevel.QUORUM);
        boundStatement.setString(0, lockId);
        boundStatement.setUUID(1, clientLockId);
        executeOrThrow(boundStatement);
    }

    @Override
    public List<UUIDTuple> read(String lockId) throws RuntimeException {
        BoundStatement boundStatement = new BoundStatement(scanStatement);
        boundStatement.setConsistencyLevel(ConsistencyLevel.QUORUM);
        boundStatement.setString(0, lockId);
        ResultSet resultSet = executeOrThrow(boundStatement);

        // we rely on the server/driver giving us the lists in sorted order, so we don't sort here.

        List<Row> rows = resultSet.all();
        List<UUIDTuple> results = new ArrayList<>(rows.size());
        for(Row row : rows) {
            UUIDTuple result = new UUIDTuple(row.getUUID(0), row.getList(1, UUID.class));
            results.add(result);
        }

        return results;
    }

    private ResultSet executeOrThrow(BoundStatement boundStatement) throws RuntimeException {
        try {
            return session.execute(boundStatement);
        } catch(DriverException e) {
            throw new RuntimeException(e);
        }
    }
}