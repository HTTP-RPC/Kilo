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

import java.util.HashMap;

import org.junit.Assert;
import org.junit.Test;

public class WebRPCServiceTest {
    @Test
    public void testStatistics() {
        double count = 3.0;
        int average = 3;
        int sum = 9;

        HashMap<String, Object> properties = new HashMap<>();
        properties.put("count", count);
        properties.put("average", average);
        properties.put("sum", sum);

        Statistics statistics = new Statistics(properties);

        Assert.assertEquals(statistics.getCount(), (int)count);
        Assert.assertEquals(statistics.getAverage(), average, 1e-9);
        Assert.assertEquals(statistics.getSum(), sum, 1e-9);
    }
}
