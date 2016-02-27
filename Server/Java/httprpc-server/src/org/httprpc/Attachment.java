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

import java.io.IOException;
import java.io.InputStream;

/**
 * Interface representing an attachement.
 */
public interface Attachment {
    /**
     *
     * Returns the name of the attachment.
     *
     * @return
     * The attachment's name.
     */
    public String getName();

    /**
     * Returns the content type of the attachment.
     *
     * @return
     * The attachment's content type.
     */
    public String getContentType();

    /**
     * Returns the content of the attachment as an input stream.
     *
     * @return
     * The attachment's content as an input stream.
     *
     * @throws IOException
     * If an exception occurs while retrieving the content.
     */
    public InputStream getInputStream() throws IOException;
}
