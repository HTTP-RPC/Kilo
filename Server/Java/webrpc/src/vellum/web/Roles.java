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

package vellum.web;

/**
 * Interface for determining user role membership.
 */
public interface Roles {
    /**
     * Verifies user membership in a given role.
     *
     * @param role
     * The name of the role.
     *
     * @return
     * <tt>true</tt> if the user is a member of the role; <tt>false</tt>,
     * otherwise.
     */
    public boolean isUserInRole(String role);
}
