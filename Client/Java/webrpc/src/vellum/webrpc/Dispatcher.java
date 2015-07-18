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

/**
 * Interface for dispatching results to result handlers.
 */
public interface Dispatcher {
    /**
     * Dispatches a result.
     *
     * @param result
     * The result to dispatch.
     *
     * @param resultHandler
     * The result handler to which the result will be dispatched.
     */
    public void dispatchResult(Object result, ResultHandler resultHandler);

    /**
     * Dispatches an exception.
     *
     * @param exception
     * The exception to dispatch.
     *
     * @param resultHandler
     * The result handler to which the exception will be dispatched.
     */
    public void dispatchException(Exception exception, ResultHandler resultHandler);
}
