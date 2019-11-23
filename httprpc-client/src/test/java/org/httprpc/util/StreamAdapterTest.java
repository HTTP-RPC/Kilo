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

package org.httprpc.util;

import org.httprpc.io.JSONEncoder;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StreamAdapterTest {
    @Test
    public void testStreamAdapter() throws IOException {
        String expected = "[2,4,6]";

        List<Integer> values = Arrays.asList(1, 2, 3);

        StringWriter writer = new StringWriter();

        JSONEncoder jsonEncoder = new JSONEncoder(true);

        jsonEncoder.write(new StreamAdapter<>(values.stream().map(element -> element * 2)), writer);

        assertEquals(expected, writer.toString());
    }
}
