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

import jakarta.servlet.annotation.WebServlet;
import org.httprpc.kilo.PageServlet;
import org.httprpc.kilo.WebService;

import java.util.List;

import static org.httprpc.kilo.util.Collections.*;

@WebServlet("/math/example")
public class MathServlet extends PageServlet {
    private interface Parameters {
        double getA();
        double getB();

        List<Double> getValues();
    }

    private @WebService.Instance MathService mathService = null;

    @Override
    protected Object execute() {
        var parameters = getParameters(Parameters.class);

        var a = parameters.getA();
        var b = parameters.getB();

        var values = parameters.getValues();

        var sum = mathService.getSum(a, b) + mathService.getSum(values);

        return mapOf(
            entry("sum", sum)
        );
    }
}
