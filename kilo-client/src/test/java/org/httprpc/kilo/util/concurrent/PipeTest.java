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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.httprpc.kilo.util.Collections.*;
import static org.junit.jupiter.api.Assertions.*;

public class PipeTest {
    private static ExecutorService executorService = null;

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
        testPipe(new Pipe<>(1, 60000));
    }

    @Test
    public void testUnboundedPipe() {
        testPipe(new Pipe<>());
    }

    @Test
    public void testUnboundedPipeWithTimeout() {
        testPipe(new Pipe<>(Integer.MAX_VALUE, 60000));
    }

    private void testPipe(Pipe<Integer> pipe) {
        var expectedValues = listOf(0, 1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89);

        executorService.submit(() -> pipe.submit(expectedValues));

        assertEquals(expectedValues, listOf(pipe));
    }

    @Test
    public void testPollTimeout() {
        var pipe = new Pipe<String>(1, 100);

        executorService.submit(() -> {
            // No-op
        });

        assertThrows(TimeoutException.class, () -> pipe.iterator().hasNext());
    }

    @Test
    public void testOfferTimeout() throws Exception {
        var pipe = new Pipe<String>(1, 100);

        var future = executorService.submit(() -> {
            try {
                pipe.submit(listOf("abc"));
            } catch (TimeoutException exception) {
                return true;
            }

            return false;
        });

        assertTrue(future.get());
    }
}
