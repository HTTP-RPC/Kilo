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
import java.util.AbstractList;
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
        response.setContentType("application/json;charset=UTF-8");

        String pathInfo = request.getPathInfo();

        Object result;
        if (pathInfo == null) {
            HashMap<String, Object> map = new HashMap<>();

            map.put("string", request.getParameter("string"));
            map.put("strings", Arrays.asList(request.getParameterValues("strings")));

            // TODO Additional arguments

            result = map;
        } else {
            switch (pathInfo.substring(1)) {
                case "longList": {
                    result = new AbstractList<Integer>() {
                        @Override
                        public Integer get(int index) {
                            return index;
                        }

                        @Override
                        public int size() {
                            return Integer.MAX_VALUE;
                        }
                    };

                    break;
                }

                case "delayedResult": {
                    result = request.getParameter("result");

                    int delay = Integer.parseInt(request.getParameter("delay"));

                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException exception) {
                        throw new RuntimeException(exception);
                    }

                    break;
                }

                case "sum": {
                    int a = Integer.parseInt(request.getParameter("a"));
                    int b = Integer.parseInt(request.getParameter("b"));

                    result = a + b;

                    break;
                }

                default: {
                    result = null;
                    break;
                }
            }
        }

        JSONEncoder jsonEncoder = new JSONEncoder();

        jsonEncoder.writeValue(result, response.getOutputStream());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // TODO
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");

        String result = request.getParameter("text").equals("héllo") ? "göodbye" : null;

        JSONEncoder jsonEncoder = new JSONEncoder();

        jsonEncoder.writeValue(result, response.getOutputStream());
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");

        boolean result = (Integer.parseInt(request.getParameter("id")) == 101);

        JSONEncoder jsonEncoder = new JSONEncoder();

        jsonEncoder.writeValue(result, response.getOutputStream());
    }
}
