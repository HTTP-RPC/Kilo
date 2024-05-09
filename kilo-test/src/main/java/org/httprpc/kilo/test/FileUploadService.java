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

package org.httprpc.kilo.test;

import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import org.httprpc.kilo.Description;
import org.httprpc.kilo.Empty;
import org.httprpc.kilo.RequestMethod;
import org.httprpc.kilo.Required;
import org.httprpc.kilo.WebService;

@WebServlet(urlPatterns = {"/file-upload/*"}, loadOnStartup = 1)
@MultipartConfig
@Description("File upload example service.")
public class FileUploadService extends WebService {
    @RequestMethod("POST")
    @Description("Uploads a single file.")
    @Empty
    public long uploadFile(
        @Description("The file to upload.") @Required URL file
    ) throws IOException {
        long bytes = 0;

        try (var inputStream = file.openStream()) {
            while (inputStream.read() != -1) {
                bytes++;
            }
        }

        return bytes;
    }

    @RequestMethod("POST")
    @Description("Uploads a list of files.")
    @Empty
    public long uploadFiles(
        @Description("The files to upload.") List<URL> files
    ) throws IOException {
        long bytes = 0;

        for (var file : files) {
            try (var inputStream = file.openStream()) {
                while (inputStream.read() != -1) {
                    bytes++;
                }
            }
        }

        return bytes;
    }
}
