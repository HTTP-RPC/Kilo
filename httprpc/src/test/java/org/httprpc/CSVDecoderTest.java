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

package org.httprpc;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class CSVDecoderTest extends AbstractTest {
    @Test
    public void testReadValues() throws IOException {
        List<Map<String, ?>> expected = listOf(
            mapOf(
                entry("a", "A,B,\"C\" "),
                entry("b", 1),
                entry("c", 2.0),
                entry("d", mapOf(
                    entry("e", true)
                ))
            ),
            mapOf(
                entry("a", " D\r\nE\r\nF\r\n"),
                entry("b", 2),
                entry("c", 4.0),
                entry("d", mapOf(
                    entry("e", false)
                ))
            )
        );

        LinkedList<Map<String, Object>> actual = new LinkedList<>();

        StringReader reader = new StringReader("\"a\",\"b\",\"c\",\"d.e\"\r\n"
            + "\"A,B,\"\"C\"\" \",1,2.0,true\r\n"
            + "\" D\r\nE\r\nF\r\n\",2,4.0,false\r\n");

        CSVDecoder csvDecoder = new CSVDecoder();

        CSVDecoder.Cursor cursor = csvDecoder.readValues(reader);

        for (Map<String, Object> row : cursor) {
            HashMap<String, Object> map = new HashMap<>();

            map.putAll(row);

            actual.add(map);
        }

        Assert.assertEquals(expected, actual);
    }
}
