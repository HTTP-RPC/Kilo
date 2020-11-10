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

package org.httprpc.test;

import org.httprpc.Description;
import org.httprpc.RequestMethod;
import org.httprpc.WebService;

import javax.servlet.annotation.WebServlet;

import static org.httprpc.util.Collections.listOf;

/**
 * Tree service.
 */
@WebServlet(urlPatterns={"/tree/*"}, loadOnStartup=1)
@Description("Tree service.")
public class TreeService extends WebService {
    @RequestMethod("GET")
    @Description("Returns an example tree structure.")
    public TreeNode getTree() {
        TreeNode root = new TreeNode("Seasons");

        TreeNode winter = new TreeNode("Winter");
        winter.setChildren(listOf(new TreeNode("January"), new TreeNode("February"), new TreeNode("March")));

        TreeNode spring = new TreeNode("Spring");
        spring.setChildren(listOf(new TreeNode("April"), new TreeNode("May"), new TreeNode("June")));

        TreeNode summer = new TreeNode("Summer");
        summer.setChildren(listOf(new TreeNode("July"), new TreeNode("August"), new TreeNode("September")));

        TreeNode fall = new TreeNode("Fall");
        fall.setChildren(listOf(new TreeNode("October"), new TreeNode("November"), new TreeNode("December")));

        root.setChildren(listOf(winter, spring, summer, fall));

        return root;
    }
}
