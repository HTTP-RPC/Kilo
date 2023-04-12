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
import org.httprpc.kilo.Description;
import org.httprpc.kilo.RequestMethod;
import org.httprpc.kilo.WebService;

import static org.httprpc.kilo.util.Collections.listOf;

/**
 * Tree service.
 */
@WebServlet(urlPatterns = {"/tree/*"}, loadOnStartup = 1)
@Description("Tree example service.")
public class TreeService extends WebService {
    @RequestMethod("GET")
    @Description("Returns an example tree structure.")
    public TreeNode getTree() {
        return new TreeNode("Seasons", listOf(
            new TreeNode("Winter", listOf(
                new TreeNode("January", null),
                new TreeNode("February", null),
                new TreeNode("March", null)
            )),
            new TreeNode("Spring", listOf(
                new TreeNode("April", null),
                new TreeNode("May", null),
                new TreeNode("June", null)
            )),
            new TreeNode("Summer", listOf(
                new TreeNode("July", null),
                new TreeNode("August", null),
                new TreeNode("September", null)
            )),
            new TreeNode("Fall", listOf(
                new TreeNode("October", null),
                new TreeNode("November", null),
                new TreeNode("December", null)
            ))
        ));
    }
}
