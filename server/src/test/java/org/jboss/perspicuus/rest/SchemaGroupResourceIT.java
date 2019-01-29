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

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test cases for the group management API.
 *
 * @since 2017-03
 * @author Jonathan Halliday (jonathan.halliday@redhat.com)
 */
public class SchemaGroupResourceIT extends AbstractResourceIT {

    private List<Integer> getMembers(int groupId) throws IOException {
        String result = client.target(URL_BASE+"/groups/"+groupId).request(CONTENT_TYPE).get(String.class);
        List<Integer> memberList = objectMapper.readValue(result, new TypeReference<List<Integer>>() {});
        return memberList;
    }

    @Test
    public void testGroupLifecycle() throws IOException {

        String groupId = client.target(URL_BASE+"/groups").request(CONTENT_TYPE).post(Entity.json("{}"), String.class);

        client.target(URL_BASE+"/groups/"+groupId+"/1").request(CONTENT_TYPE).put(Entity.json("{}"), String.class);

        List<Integer> membersList = getMembers(Integer.parseInt(groupId));

        assertEquals(1, membersList.size());
        assertEquals(Integer.valueOf(1), membersList.get(0));

        client.target(URL_BASE+"/groups/"+groupId+"/1").request(CONTENT_TYPE).delete();

        membersList = getMembers(Integer.parseInt(groupId));
        assertTrue(membersList.isEmpty());
    }
}
