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

package vellum.webrpc;

import java.security.Principal;
import java.util.Locale;

/**
 * Abstract base class for web RPC services.
 */
public abstract class WebRPCService {
    private Locale locale = null;
    private Principal userPrincipal = null;

    private Roles roles = null;

    /**
     * Initializes the service with information about the current request.
     *
     * @param locale
     * The request locale.
     *
     * @param userPrincipal
     * The request user principal, or <tt>null</tt> if the user has not been
     * authenticated.
     *
     * @param roles
     * The requst roles, or <tt>null</tt> if the user has not been
     * authenticated.
     */
    protected void initialize(Locale locale, Principal userPrincipal, Roles roles) {
        if (locale == null) {
            throw new IllegalArgumentException();
        }

        this.locale = locale;
        this.userPrincipal = userPrincipal;

        this.roles = roles;
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
     * Returns the user principal associated with the current request.
     *
     * @return
     * The user principal associated with the current request, or <tt>null</tt>
     * if the user has not been authenticated.
     */
    protected Principal getUserPrincipal() {
        return userPrincipal;
    }

    /**
     * Verifies that the user making the current request belongs to a given
     * logical role.
     *
     * @param role
     * The name of the role.
     *
     * @return
     * <tt>true</tt> if the user belongs to the given role; <tt>false</tt> if
     * the user does not belong to the role or has not been authenticated.
     */
    protected boolean isUserInRole(String role) {
        return (roles == null) ? false : roles.isUserInRole(role);
    }
}
