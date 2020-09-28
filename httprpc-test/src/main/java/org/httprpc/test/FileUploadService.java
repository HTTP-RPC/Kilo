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

import org.httprpc.Description;
import org.httprpc.RequestMethod;
import org.httprpc.WebService;

import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

/**
 * File upload example service.
 */
@WebServlet(urlPatterns={"/upload/*"}, loadOnStartup=1)
@MultipartConfig
@Description("File upload example service.")
public class FileUploadService extends WebService {
    private static final long serialVersionUID = 0;

    @RequestMethod("POST")
    @Description("Uploads a single file.")
    public long upload(
        @Description("The file to upload.") URL file
    ) throws IOException {
        long bytes = 0;

        if (file != null) {
            try (InputStream inputStream = file.openStream()) {
                while (inputStream.read() != -1) {
                    bytes++;
                }
            }
        }

        return bytes;
    }

    @RequestMethod("POST")
    @Description("Uploads a list of files.")
    public long upload(
        @Description("The files to upload.") List<URL> files
    ) throws IOException {
        long bytes = 0;

        for (URL file : files) {
            try (InputStream inputStream = file.openStream()) {
                while (inputStream.read() != -1) {
                    bytes++;
                }
            }
        }

        return bytes;
    }
}
