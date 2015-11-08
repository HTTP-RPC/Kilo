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

import java.util.Locale;

/**
 * Abstract base class for HTTP-RPC web services.
 */
public abstract class WebService {
    private Locale locale = null;
    private String userName = null;

    /**
     * Initializes the service with information about the current request.
     *
     * @param locale
     * The locale associated with the current request.
     *
     * @param userName
     * The user name associated with the current request, or <tt>null</tt> if
     * the request has not been authenticated.
     */
    protected void initialize(Locale locale, String userName) {
        if (locale == null) {
            throw new IllegalArgumentException();
        }

        this.locale = locale;
        this.userName = userName;
    }

    /**
     * Returns the locale associated with the current request.
     *
     * @return
     * The locale associated with the current request.
     */
    protected Locale getLocale() {
        return locale;
    }

    /**
     * Returns the user name associated with the current request.
     *
     * @return
     * The user name associated with the current request, or <tt>null</tt> if
     * the user has not been authenticated.
     */
    protected String getUserName() {
        return userName;
    }
}
