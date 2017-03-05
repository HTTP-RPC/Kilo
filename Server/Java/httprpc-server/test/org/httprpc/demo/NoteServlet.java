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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.annotation.WebServlet;

import org.httprpc.DispatcherServlet;
import org.httprpc.RequestMethod;

/**
 * Note demo servlet.
 */
@WebServlet(urlPatterns={"/notes/*"}, loadOnStartup=1)
public class NoteServlet extends DispatcherServlet {
    private static final long serialVersionUID = 0;

    private static LinkedHashMap<Integer, Map<String, ?>> notes = new LinkedHashMap<>();

    private static int nextNoteID = 1;

    @RequestMethod("GET")
    public synchronized List<?> getNotes() {
        return new ArrayList<>(notes.values());
    }

    @RequestMethod("POST")
    public synchronized void addNote(String message) {
        if (message == null) {
            throw new IllegalArgumentException();
        }

        HashMap<String, Object> note = new HashMap<>();

        Date date = new Date();

        note.put("id", nextNoteID);
        note.put("date", date.getTime());
        note.put("message", message);

        notes.put(nextNoteID, note);

        nextNoteID++;
    }

    @RequestMethod("DELETE")
    public synchronized void removeNote(int id) {
        notes.remove(id);
    }
}
