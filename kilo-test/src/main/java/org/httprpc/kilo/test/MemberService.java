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
import org.httprpc.kilo.Name;
import org.httprpc.kilo.RequestMethod;
import org.httprpc.kilo.WebService;

import java.util.List;

import static org.httprpc.kilo.util.Collections.*;

@WebServlet(urlPatterns = {"/members/*"}, loadOnStartup = 1)
public class MemberService extends WebService {
    @RequestMethod("GET")
    public List<Person> getMembers(
        @Name("first_name") String firstName,
        @Name("last_name") String lastName
    ) {
        var member = new Person();

        member.setFirstName(firstName);
        member.setLastName(lastName);

        return listOf(member);
    }
}
