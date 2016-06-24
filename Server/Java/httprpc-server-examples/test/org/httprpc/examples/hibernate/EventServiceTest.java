/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.httprpc.examples.hibernate;

import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class EventServiceTest {
    @BeforeClass
    public static void startup() {
        new HibernateSessionFactoryManager().contextInitialized(null);
    }

    @AfterClass
    public static void shutdown() {
        new HibernateSessionFactoryManager().contextDestroyed(null);
    }

    @Test
    public void eventServiceTest() {
        EventService service = new EventService();

        service.addEvent("A");
        service.addEvent("B");
        service.addEvent("C");

        List<Map<String, ?>> events = service.getEvents();

        Assert.assertEquals("A", events.get(0).get("title"));
        Assert.assertEquals("B", events.get(1).get("title"));
        Assert.assertEquals("C", events.get(2).get("title"));
    }
}
