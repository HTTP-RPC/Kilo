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

package org.httprpc.demo;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Note servlet.
 */
@WebServlet(urlPatterns={"/notes/*"}, loadOnStartup=1)
public class NoteServlet extends HttpServlet {
    private static final long serialVersionUID = 0;

    private static LinkedHashMap<Integer, Map<String, ?>> notes = new LinkedHashMap<>();

    private static int nextNoteID = 1;

    private static final String ID_KEY = "id";
    private static final String DATE_KEY = "date";
    private static final String MESSAGE_KEY = "message";

    @Override
    protected synchronized void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");

        PrintWriter writer = response.getWriter();

        writer.write("[");

        int i = 0;

        for (Map<String, ?> note : notes.values()) {
            if (i > 0) {
                writer.write(", ");
            }

            writer.write(String.format("{\"id\": \"%s\", \"date\": %d, \"message\": \"%s\"}",
                note.get(ID_KEY),
                note.get(DATE_KEY),
                note.get(MESSAGE_KEY)));

            i++;
        }

        writer.write("]");
    }

    @Override
    protected synchronized void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (request.getCharacterEncoding() == null) {
            request.setCharacterEncoding("UTF-8");
        }

        HashMap<String, Object> note = new HashMap<>();

        Date date = new Date();

        note.put(ID_KEY, nextNoteID);
        note.put(DATE_KEY, date.getTime());
        note.put(MESSAGE_KEY, request.getParameter("message"));

        notes.put(nextNoteID, note);

        nextNoteID++;

        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    @Override
    protected synchronized void doDelete(HttpServletRequest request, HttpServletResponse response) {
        notes.remove(Integer.parseInt(request.getParameter("id")));

        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }
}
