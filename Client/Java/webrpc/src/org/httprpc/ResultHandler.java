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

/**
 * Interface representing a result handler.
 */
public interface ResultHandler<V> {
    /**
     * Executes the result handler.
     *
     * @param result
     * The result of executing a remote method, or <tt>null</tt> if an error
     * occurred.
     *
     * @param exception
     * An exception representing the error that occurred while invoking the
     * remote method, or <tt>null</tt> if an error did not occur.
     */
    public void execute(V result, Exception exception);
}
