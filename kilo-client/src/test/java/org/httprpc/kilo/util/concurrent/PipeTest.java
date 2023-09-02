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

package org.httprpc.kilo.util.concurrent;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.httprpc.kilo.util.Collections.listOf;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class PipeTest {
    private static ExecutorService executorService;

    @BeforeAll
    public static void setUpClass() {
        executorService = Executors.newSingleThreadExecutor();
    }

    @AfterAll
    public static void tearDownClass() {
        executorService.shutdown();
    }

    @Test
    public void testBoundedPipe() {
        testPipe(new Pipe<>(1));
    }

    @Test
    public void testBoundedPipeWithTimeout() {
        testPipe(new Pipe<>(1, 5000));
    }

    @Test
    public void testUnboundedPipe() {
        testPipe(new Pipe<>());
    }

    @Test
    public void testUnboundedPipeWithTimeout() {
        testPipe(new Pipe<>(Integer.MAX_VALUE, 5000));
    }

    private void testPipe(Pipe<Integer> pipe) {
        var expectedValues = listOf(0, 1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89);

        executorService.submit(() -> pipe.accept(expectedValues.stream()));

        var actualValues = new ArrayList<>(expectedValues.size());

        for (var element : pipe) {
            actualValues.add(element);
        }

        assertEquals(expectedValues, actualValues);
    }
}
