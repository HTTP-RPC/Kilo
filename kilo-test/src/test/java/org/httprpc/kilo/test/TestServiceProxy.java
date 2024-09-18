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

import org.httprpc.kilo.Configuration;
import org.httprpc.kilo.ErrorHandler;
import org.httprpc.kilo.FormData;
import org.httprpc.kilo.RequestMethod;
import org.httprpc.kilo.Required;
import org.httprpc.kilo.ResourcePath;
import org.httprpc.kilo.ServicePath;
import org.httprpc.kilo.io.TextDecoder;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ServicePath("test")
public interface TestServiceProxy {
    class CustomErrorHandler implements ErrorHandler {
        @Override
        public void handleResponse(InputStream errorStream, String contentType, int statusCode) throws IOException {
            var textDecoder = new TextDecoder();

            throw new WebServiceProxyTest.CustomException(textDecoder.read(errorStream));
        }
    }

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
    @FormData
    @SuppressWarnings("deprecation")
    TestService.Response testURLEncodedPost(@Required String string, List<String> strings, Integer number, Set<Integer> numbers) throws IOException;

    @RequestMethod("POST")
    @FormData(multipart = true)
    @SuppressWarnings("deprecation")
    TestService.Response testMultipartPost(@Required String string, List<String> strings, Integer number, Set<Integer> numbers, URL... attachments) throws IOException;

    @RequestMethod("PUT")
    @ResourcePath("?")
    int testEmptyPut(int id, String value, Void body) throws IOException;

    @RequestMethod("GET")
    @Configuration(connectTimeout = 500, readTimeout = 4000)
    int testTimeout(int value, int delay) throws IOException;

    @RequestMethod("GET")
    @ResourcePath("error")
    @Configuration(errorHandler = CustomErrorHandler.class)
    void testCustomErrorHandler() throws IOException;

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
