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

package org.httprpc.kilo.tools;

import org.httprpc.kilo.web.WebServiceProxy;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

public class MemberServiceTest {
    private static final URI baseURI = URI.create("http://localhost:8080/kilo-test/");

    @Test
    public void testMembers() throws IOException {
        var memberServiceProxy = WebServiceProxy.of(MemberServiceProxy.class, baseURI);

        var result = memberServiceProxy.getMembers("foo", "bar");

        assertFalse(result.isEmpty());

        var first = result.getFirst();

        assertEquals("foo", first.getFirstName());
        assertEquals("bar", first.getLastName());
    }
}
