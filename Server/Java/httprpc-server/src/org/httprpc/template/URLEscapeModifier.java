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

package org.httprpc.template;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;

/**
 * URL escape modifier.
 */
public class URLEscapeModifier implements Modifier {
    private static final String UTF_8_ENCODING = "UTF-8";

    @Override
    public Object apply(Object value, String argument, Locale locale) {
        String result;
        try {
            result = URLEncoder.encode(value.toString(), UTF_8_ENCODING);
        } catch (UnsupportedEncodingException exception) {
            throw new RuntimeException(exception);
        }

        return result;
    }
}
