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

package org.httprpc.kilo;

import org.httprpc.kilo.io.JSONEncoder;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class NestedDataStructuresTest3 {
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws IOException {
        var url = new URL("https://jsonplaceholder.typicode.com/users");

        var users = (List<Map<String, Map<String, Object>>>)WebServiceProxy.get(url).invoke();

        var catchPhrases = users.stream()
            .map(user -> (String)user.get("company").get("catchPhrase"))
            .toList();

        var jsonEncoder = new JSONEncoder();

        jsonEncoder.write(catchPhrases, System.out);
    }
}
