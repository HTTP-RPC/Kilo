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

package org.httprpc.example;

import java.util.List;
import java.util.Map;

import javax.servlet.annotation.WebServlet;

import org.httprpc.DispatcherServlet;
import org.httprpc.RequestMethod;
import org.httprpc.ResourcePath;

import static org.httprpc.WebServiceProxy.mapOf;
import static org.httprpc.WebServiceProxy.entry;

/**
 * Math example servlet.
 */
@WebServlet(urlPatterns={"/math/*"}, loadOnStartup=1)
public class MathServlet extends DispatcherServlet {
    private static final long serialVersionUID = 0;

    @RequestMethod("GET")
    @ResourcePath("/sum")
    public double getSum(double a, double b) {
        return a + b;
    }

    @RequestMethod("GET")
    @ResourcePath("/sum")
    public double getSum(List<Double> values) {
        double total = 0;

        for (double value : values) {
            total += value;
        }

        return total;
    }

    @RequestMethod("GET")
    @ResourcePath("/statistics")
    public Map<String, ?> getStatistics(List<Double> values) {
        int count = values.size();

        double sum = 0;

        for (int i = 0; i < count; i++) {
            sum += values.get(i);
        }

        double average = sum / count;

        return mapOf(
            entry("count", count),
            entry("sum", sum),
            entry("average", average)
        );
    }
}
