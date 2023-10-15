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

import org.httprpc.kilo.RequestMethod;
import org.httprpc.kilo.Required;
import org.httprpc.kilo.ResourcePath;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface TestServiceProxy {
    @RequestMethod("GET")
    Map<String, Object> testGet(@Required String string, List<String> strings, int number) throws IOException;

    @RequestMethod("GET")
    @ResourcePath("a/?/b/?/c/?/d/?")
    Map<String, Object> testKeys(int a, String b, int c, String d) throws IOException;

    @RequestMethod("POST")
    @ResourcePath("foo/?/bar/?")
    Map<String, Object> testParameters(int x, int y, int a, int b, List<Double> values) throws IOException;

    @RequestMethod("PUT")
    @ResourcePath("?")
    int testEmptyPut(int value) throws IOException;

    @RequestMethod("GET")
    @ResourcePath("headers")
    Map<String, String> testHeaders() throws IOException;
}
