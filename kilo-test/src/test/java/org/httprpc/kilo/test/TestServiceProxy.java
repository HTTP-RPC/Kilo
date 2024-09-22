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
import org.httprpc.kilo.ServicePath;
import org.httprpc.kilo.WebServiceProxy;
import org.httprpc.kilo.io.TextDecoder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ServicePath("test")
public interface TestServiceProxy {
    class CustomRequestHandler implements WebServiceProxy.RequestHandler {
        private static final int EOF = -1;

        @Override
        public String getContentType() {
            return null;
        }

        @Override
        public void encodeRequest(Object body, OutputStream outputStream) throws IOException {
            try (var inputStream = ((URL)body).openStream()) {
                int b;
                while ((b = inputStream.read()) != EOF) {
                    outputStream.write(b);
                }
            }
        }
    }

    class CustomResponseHandler implements WebServiceProxy.ResponseHandler {
        @Override
        public Object decodeResponse(InputStream inputStream, String contentType) throws IOException {
            return ImageIO.read(inputStream);
        }
    }

    class CustomErrorHandler implements WebServiceProxy.ErrorHandler {
        @Override
        public void handleResponse(InputStream errorStream, String contentType, int statusCode) throws IOException {
            var textDecoder = new TextDecoder();

            throw new WebServiceProxyTest.CustomException(textDecoder.read(errorStream));
        }
    }

    @RequestMethod("GET")
    TestService.Response testGet(@Required String string, List<String> strings, Integer number, Set<Integer> numbers, char character) throws IOException;

    @RequestMethod("GET")
    @ResourcePath("a#/?/b*/?/c@/?/d=/?")
    Map<String, Object> testKeys(int a, String b, int c, String d) throws IOException;

    @RequestMethod("POST")
    @ResourcePath("foo/?/bar/?")
    Map<String, Object> testParameters(int x, int y, int a, int b, List<Double> values) throws IOException;

    @RequestMethod("POST")
    @ResourcePath("varargs")
    Map<String, Object> testVarargs(int[] numbers, String... strings) throws IOException;

    @RequestMethod("POST")
    @ResourcePath("image")
    @WebServiceProxy.Configuration(requestHandler = CustomRequestHandler.class, responseHandler = CustomResponseHandler.class, chunkSize = 4096)
    BufferedImage testImagePost(URL body) throws IOException;

    @RequestMethod("PUT")
    @ResourcePath("?")
    int testEmptyPut(int id, String value, Void body) throws IOException;

    @RequestMethod("GET")
    @WebServiceProxy.Configuration(connectTimeout = 500, readTimeout = 4000)
    int testTimeout(int value, int delay) throws IOException;

    @RequestMethod("GET")
    @ResourcePath("error")
    @WebServiceProxy.Configuration(errorHandler = CustomErrorHandler.class)
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
