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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.httprpc.kilo.PageServlet;
import org.httprpc.kilo.WebService;
import org.httprpc.kilo.io.TemplateEncoder;

import java.io.IOException;

import static org.httprpc.kilo.util.Collections.*;
import static org.httprpc.kilo.util.Optionals.*;

@WebServlet("/math/example")
public class MathServlet extends PageServlet {
    private @WebService.Instance MathService mathService = null;

    @Override
    protected void process(HttpServletRequest request, HttpServletResponse response) throws IOException {
        var a = coalesce(map(request.getParameter("a"), Double::valueOf), () -> 0.0);
        var b = coalesce(map(request.getParameter("b"), Double::valueOf), () -> 0.0);

        var sum = mathService.getSum(a, b);

        var type = getClass();

        var templateEncoder = new TemplateEncoder(type, String.format("%s.html", type.getSimpleName()));

        templateEncoder.write(mapOf(
            entry("a", a),
            entry("b", b),
            entry("sum", sum)
        ), response.getWriter());
    }
}
