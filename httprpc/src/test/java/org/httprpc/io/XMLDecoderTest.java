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

package org.httprpc.io;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.httprpc.AbstractTest;
import org.junit.Assert;
import org.junit.Test;

public class XMLDecoderTest extends AbstractTest {
    @Test
    public void testRead() throws IOException {
        String text = ""; // TODO

        List<Map<String, ?>> expected = listOf(); // TODO

        StringReader reader = new StringReader(text);

        XMLDecoder xmlDecoder = new XMLDecoder();

        Iterable<Map<String, ?>> cursor = xmlDecoder.read(reader);
        List<Map<String, ?>> actual = StreamSupport.stream(cursor.spliterator(), false).collect(Collectors.toList());

        Assert.assertEquals(expected, actual);
    }
}
