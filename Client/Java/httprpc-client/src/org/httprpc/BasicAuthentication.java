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

import java.net.HttpURLConnection;

/**
 * HTTP <a href="http://tools.ietf.org/rfc/rfc2617.txt">basic authentication</a>
 * provider.
 */
public class BasicAuthentication implements Authentication {
    private String username;
    private String password;

    private static char[] lookup = new char[64];

    static {
        for (int i = 0; i < 26; i++) {
            lookup[i] = (char)('A' + i);
        }

        for (int i = 26, j = 0; i < 52; i++, j++) {
            lookup[i] = (char)('a' + j);
        }

        for (int i = 52, j = 0; i < 62; i++, j++) {
            lookup[i] = (char)('0' + j);
        }

        lookup[62] = '+';
        lookup[63] = '/';
    }

    private static final String AUTHORIZATION_KEY = "Authorization";

    /**
     * Constructs a new basic authentication provider.
     *
     * @param username
     * The username that will be used to authenticate requests.
     *
     * @param password
     * The password that will be used to authenticate requests.
     */
    public BasicAuthentication(String username, String password) {
        if (username == null) {
            throw new IllegalArgumentException();
        }

        if (password == null) {
            throw new IllegalArgumentException();
        }

        this.username = username;
        this.password = password;
    }

    @Override
    public void authenticateRequest(HttpURLConnection connection) {
        connection.setRequestProperty(AUTHORIZATION_KEY, "Basic " + encode(String.format("%s:%s", username, password)));
    }

    private static String encode(String value) {
        // TODO Use java.util.Base64 in Java 8
        byte[] bytes = value.getBytes();

        StringBuilder resultBuilder = new StringBuilder(4 * (bytes.length / 3 + 1));

        for (int i = 0, n = bytes.length; i < n; ) {
            byte byte0 = bytes[i++];
            byte byte1 = (i++ < n) ? bytes[i - 1] : 0;
            byte byte2 = (i++ < n) ? bytes[i - 1] : 0;

            resultBuilder.append(lookup[byte0 >> 2]);
            resultBuilder.append(lookup[((byte0 << 4) | byte1 >> 4) & 63]);
            resultBuilder.append(lookup[((byte1 << 2) | byte2 >> 6) & 63]);
            resultBuilder.append(lookup[byte2 & 63]);

            if (i > n) {
                for (int m = resultBuilder.length(), j = m - (i - n); j < m; j++) {
                    resultBuilder.setCharAt(j, '=');
                }
            }
        }

        return resultBuilder.toString();
    }
}
