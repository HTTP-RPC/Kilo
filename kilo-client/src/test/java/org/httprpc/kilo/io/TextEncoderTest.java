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

package org.httprpc.kilo.io;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class TextEncoderTest {
    @Test
    public void testString() throws IOException {
        assertEquals("héllo/r/nwørld", encode("héllo/r/nwørld"));
    }

    @Test
    public void testLocalDate() throws IOException {
        assertEquals("2024-03-31", encode(LocalDate.of(2024, 3, 31)));
    }

    private static String encode(Object value) throws IOException {
        var textEncoder = new TextEncoder();

        var writer = new StringWriter();

        textEncoder.write(value, writer);

        return writer.toString();
    }
}
