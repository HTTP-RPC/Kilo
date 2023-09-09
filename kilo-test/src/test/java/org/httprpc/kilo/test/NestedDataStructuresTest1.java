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

import org.httprpc.kilo.WebServiceProxy;
import org.httprpc.kilo.beans.BeanAdapter;
import org.httprpc.kilo.io.JSONEncoder;

import java.io.IOException;
import java.net.URL;
import java.util.List;

public class NestedDataStructuresTest1 {
    public static class User {
        private Company company;

        public Company getCompany() {
            return company;
        }

        public void setCompany(Company company) {
            this.company = company;
        }
    }

    public static class Company {
        private String catchPhrase;

        public String getCatchPhrase() {
            return catchPhrase;
        }

        public void setCatchPhrase(String catchPhrase) {
            this.catchPhrase = catchPhrase;
        }
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws IOException {
        var url = new URL("https://jsonplaceholder.typicode.com/users");

        List<User> users = BeanAdapter.coerce(WebServiceProxy.get(url).invoke(), List.class, User.class);

        var catchPhrases = users.stream()
            .map(user -> user.getCompany().getCatchPhrase())
            .toList();

        var jsonEncoder = new JSONEncoder();

        jsonEncoder.write(catchPhrases, System.out);
    }
}
