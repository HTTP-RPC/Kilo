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

package org.httprpc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Executors;

import org.junit.Assert;
import org.junit.Test;

public class TextDecoderTest {
    private WebServiceProxy serviceProxy;

    public TextDecoderTest() throws MalformedURLException {
        serviceProxy = new WebServiceProxy(new URL("http://localhost:8080"), Executors.newSingleThreadExecutor());
    }

    @Test
    public void testPlainText() throws IOException {
        Assert.assertTrue(decode("héllo\ngöodbye", "text/plain").equals("héllo\ngöodbye"));
    }

    private String decode(String text, String contentType) throws IOException {
        return (String)serviceProxy.decodeResponse(new ByteArrayInputStream(text.getBytes()), contentType);
    }
}
