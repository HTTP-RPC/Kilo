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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Simple note management service.
 */
@MultipartConfig
public class NoteServlet extends HttpServlet {
    private static final long serialVersionUID = 0;

    private static LinkedHashMap<Integer, Map<String, ?>> notes = new LinkedHashMap<>();

    private static int nextNoteID = 1;

    private static final String ID_KEY = "id";
    private static final String DATE_KEY = "date";
    private static final String MESSAGE_KEY = "message";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");

        synchronized (notes) {
            JSONEncoder jsonEncoder = new JSONEncoder();

            jsonEncoder.writeValue(new ArrayList<>(notes.values()), response.getOutputStream());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        synchronized (notes) {
            HashMap<String, Object> note = new HashMap<>();

            note.put(ID_KEY, nextNoteID);
            note.put(DATE_KEY, new Date().getTime());
            note.put(MESSAGE_KEY, request.getParameter("message"));

            notes.put(nextNoteID, note);

            nextNoteID++;
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        synchronized (notes) {
            notes.remove(Integer.parseInt(request.getParameter("id")));
        }
    }
}
