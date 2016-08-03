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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import org.httprpc.Encoder;

public class TestEncoder implements Encoder {
    private static final String OCTET_STREAM_MIME_TYPE = "application/octet-stream";

    @Override
    public String getContentType(Object value) {
        URL url = (URL)value;

        return (url == null) ? OCTET_STREAM_MIME_TYPE : URLConnection.guessContentTypeFromName(url.getFile());
    }

    @Override
    public void writeValue(Object value, OutputStream outputStream) throws IOException {
        URL url = (URL)value;

        if (url != null) {
            try (InputStream inputStream = url.openStream()) {
                int b;
                while ((b = inputStream.read()) != -1) {
                    outputStream.write(b);
                }
            }
        }
    }
}
