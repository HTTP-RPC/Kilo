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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jtemplate.TemplateEncoder;

/**
 * Note servlet.
 */
@WebServlet(urlPatterns={"/notes/*"}, loadOnStartup=1)
public class NoteServlet extends HttpServlet {
    private static final long serialVersionUID = 0;

    private static LinkedHashMap<Integer, Map<String, ?>> notes = new LinkedHashMap<>();

    private static int nextNoteID = 1;

    @Override
    protected synchronized void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");

        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("notes.json.txt"));

        encoder.writeValue(new ArrayList<>(notes.values()), response.getOutputStream());
    }

    @Override
    protected synchronized void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HashMap<String, Object> note = new HashMap<>();

        note.put("id", nextNoteID);
        note.put("date", new Date());
        note.put("message", request.getParameter("message"));

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
