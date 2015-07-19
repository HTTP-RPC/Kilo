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

package vellum.webrpc;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebRPCServiceTest {
    private static class TestDispatcher implements Dispatcher {
        @Override
        public synchronized void dispatchResult(Object result, ResultHandler resultHandler) {
            resultHandler.execute(result, null);
        }

        @Override
        public synchronized void dispatchException(Exception exception, ResultHandler resultHandler) {
            resultHandler.execute(null, exception);
        }
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        ExecutorService threadPool = Executors.newFixedThreadPool(10);

        WebRPCService service = new WebRPCService(new URL("http://localhost:8080/webrpc-test-1.0/test/"),
            threadPool, new TestDispatcher());

        // TODO Use HTTPS; authenticate user

        HashMap<String, Object> addArguments = new HashMap<>();
        addArguments.put("a", 2);
        addArguments.put("b", 4);
        addArguments.put("values", new Integer[] {1, 2, 3, 4});

        // TODO Pass an array of Argument instead of or in addition to a map?

        service.invoke("add", addArguments, new ResultHandler() {
            @Override
            public void execute(Object result, Exception exception) {
                validate(result, 6.0, exception);
            }
        });

        HashMap<String, Object> addArrayArguments = new HashMap<>();
        addArrayArguments.put("values", new Integer[] {1, 2, 3, 4});

        service.invoke("addArray", addArrayArguments, new ResultHandler() {
            @Override
            public void execute(Object result, Exception exception) {
                validate(result, 10.0, exception);
            }
        });

        HashMap<String, Object> addVarargsArguments = new HashMap<>();
        addVarargsArguments.put("values", new Integer[] {1, 2, 3, 4});

        service.invoke("addVarargs", addVarargsArguments, (result, exception) -> {
            validate(result, 10.0, exception);
        });

        // TODO More tests

        HashMap<String, Object> getStatisticsArguments = new HashMap<>();
        getStatisticsArguments.put("values", new Integer[] {1, 3, 5});

        service.invoke("getStatistics", getStatisticsArguments, (result, exception) -> {
            HashMap<String, Object> expected = new HashMap<>();
            expected.put("count", 3L);
            expected.put("average", 3.0);
            expected.put("sum", 9.0);

            validate(result, expected, exception);

            Statistics statistics = new Statistics((Map<String, Object>)result);

            System.out.printf("count = %d, sum = %f, average = %f\n",
                statistics.getCount(),
                statistics.getSum(),
                statistics.getAverage());
        });

        // TODO More tests

        threadPool.shutdown();
    }

    private static void validate(Object actual, Object expected, Exception exception) {
        String message;
        if ((actual == null) ? expected == null : actual.equals(expected)) {
            message = "OK";
        } else {
            message = "FAIL: " + exception.getMessage();
        }

        System.out.println(message);
    }
}
