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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebRPCServiceTest {
    private static class TestDispatcher implements Dispatcher {
        @Override
        public synchronized <V> void dispatchResult(V result, ResultHandler<V> resultHandler) {
            resultHandler.execute(result, null);
        }

        @Override
        public synchronized void dispatchException(Exception exception, ResultHandler<?> resultHandler) {
            System.out.println(exception.getMessage());
        }
    }

    public static void main(String[] args) throws Exception {
        ExecutorService threadPool = Executors.newFixedThreadPool(10);

        WebRPCService service = new WebRPCService(new URL("http://localhost:8080/webrpc-test-1.0/test/"),
            threadPool, new TestDispatcher());

        // TODO Use HTTPS; authenticate user

        HashMap<String, Object> addArguments = new HashMap<>();
        addArguments.put("a", 2);
        addArguments.put("b", 4);

        service.invoke("add", addArguments, new ResultHandler<Number>() {
            @Override
            public void execute(Number result, Exception exception) {
                validate(result.doubleValue() == 6.0);
            }
        });

        HashMap<String, Object> addValuesArguments = new HashMap<>();
        addValuesArguments.put("values", Arrays.asList(1, 2, 3, 4));

        service.invoke("addValues", addValuesArguments, (Number result, Exception exception) -> {
            validate(result.doubleValue() == 10.0);
        });

        // TODO More tests

        HashMap<String, Object> getStatisticsArguments = new HashMap<>();
        getStatisticsArguments.put("values", Arrays.asList(1, 3, 5));

        service.invoke("getStatistics", getStatisticsArguments, (Map<String, Object> result, Exception exception) -> {
            Statistics statistics = new Statistics(result);

            validate(statistics.getCount() == 3
                && statistics.getAverage() == 3.0
                && statistics.getSum() == 9.0);
        });

        // TODO More tests

        threadPool.shutdown();
    }

    private static void validate(boolean condition) {
        System.out.println(condition ? "OK" : "FAIL");
    }
}
