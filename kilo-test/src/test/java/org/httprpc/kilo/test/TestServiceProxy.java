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

import org.httprpc.kilo.Empty;
import org.httprpc.kilo.FormData;
import org.httprpc.kilo.RequestMethod;
import org.httprpc.kilo.Required;
import org.httprpc.kilo.ResourcePath;

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface TestServiceProxy {
    @RequestMethod("GET")
    Map<String, Object> testGet(@Required String string, List<String> strings, Integer number, Set<Integer> numbers, char character) throws IOException;

    @RequestMethod("GET")
    @ResourcePath("a/?/b/?/c/?/d/?")
    Map<String, Object> testKeys(int a, String b, int c, String d) throws IOException;

    @RequestMethod("POST")
    @ResourcePath("foo/?/bar/?")
    Map<String, Object> testParameters(int x, int y, int a, int b, List<Double> values) throws IOException;

    @RequestMethod("POST")
    @ResourcePath("varargs")
    Map<String, Object> testVarargs(int[] numbers, String... strings) throws IOException;

    @RequestMethod("POST")
    @Empty
    TestService.Response testPost(@Required String string, List<String> strings, Integer number, Set<Integer> numbers) throws IOException;

    @RequestMethod("POST")
    @Empty
    @FormData
    TestService.Response testURLEncodedPost(@Required String string, List<String> strings, Integer number, Set<Integer> numbers) throws IOException;

    @RequestMethod("POST")
    @Empty
    @FormData(multipart = true)
    TestService.Response testMultipartPost(@Required String string, List<String> strings, Integer number, Set<Integer> numbers, URL... attachments) throws IOException;

    @RequestMethod("PUT")
    @ResourcePath("?")
    @Empty
    int testEmptyPut(int value) throws IOException;

    @RequestMethod("GET")
    @ResourcePath("headers")
    Map<String, String> testHeaders() throws IOException;

    @RequestMethod("GET")
    Map<String, String> testMissingException();

    @RequestMethod("GET")
    Map<String, String> testInvalidException() throws ParseException;

    @RequestMethod("GET")
    @ResourcePath("fibonacci")
    List<Number> getFibonacciSequence(int count) throws IOException;

    default int getFibonacciSum(int count) throws IOException {
        return getFibonacciSequence(count).stream().mapToInt(Number::intValue).sum();
    }
}
