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

package org.httprpc.kilo.test;

import org.httprpc.kilo.Content;
import org.httprpc.kilo.Description;
import org.httprpc.kilo.RequestMethod;
import org.httprpc.kilo.ResourcePath;
import org.httprpc.kilo.WebService;

import jakarta.servlet.annotation.WebServlet;
import java.util.List;

@WebServlet(urlPatterns = {"/math/*"}, loadOnStartup = 1)
@Description("Math example service.")
public class MathService extends WebService {
    @RequestMethod("GET")
    @ResourcePath("sum")
    @Description("Calculates the sum of two numbers.")
    public double getSum(
        @Description("The first number.") double a,
        @Description("The second number.") double b
    ) {
        return a + b;
    }

    @RequestMethod("GET")
    @ResourcePath("sum")
    @Description("Calculates the sum of a list of numbers.")
    public double getSum(
        @Description("The numbers to add.") List<Double> values
    ) {
        double total = 0;

        for (double value : values) {
            total += value;
        }

        return total;
    }

    @RequestMethod("POST")
    @ResourcePath("sum")
    @Description("Calculates the sum of a list of numbers.")
    @Content(type = Double.class, multiple = true)
    @SuppressWarnings("unchecked")
    public double getSum() {
        return getSum((List<Double>)getBody());
    }
}
