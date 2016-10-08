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

package org.httprpc.server;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Test servlet.
 */
@MultipartConfig
public class TestServlet extends HttpServlet {
    private static final long serialVersionUID = 0;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // TODO Check for path info

        response.setContentType("application/json;charset=UTF-8");

        HashMap<String, Object> result = new HashMap<>();

        result.put("string", request.getParameter("string"));
        result.put("strings", Arrays.asList(request.getParameterValues("strings")));

        // TODO Additional arguments

        JSONEncoder jsonEncoder = new JSONEncoder();

        jsonEncoder.writeValue(result, response.getOutputStream());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // TODO
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // TODO
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // TODO
    }
}
