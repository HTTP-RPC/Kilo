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

import java.io.IOException;

/**
 * Thrown to indicate that a service operation returned an error.
 */
public class WebServiceException extends IOException {
    private int statusCode;

    /**
     * Constructs a new web service exception.
     *
     * @param message
     * The error message returned by the service.
     *
     * @param statusCode
     * The HTTP status code returned by the service.
     */
    public WebServiceException(String message, int statusCode) {
        super(message);

        this.statusCode = statusCode;
    }

    /**
     * Returns the HTTP status code returned by the service.
     *
     * @return
     * The HTTP status code returned by the service.
     */
    public int getStatusCode() {
        return statusCode;
    }
}
