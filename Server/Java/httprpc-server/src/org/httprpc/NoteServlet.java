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

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;

import org.jtemplate.DispatcherServlet;
import org.jtemplate.RequestMethod;

/**
 * Note servlet.
 */
@WebServlet(urlPatterns={"/notes/*"}, loadOnStartup=1)
@MultipartConfig
public class NoteServlet extends DispatcherServlet {
    private static final long serialVersionUID = 0;

    private static LinkedHashMap<Integer, Map<String, ?>> notes = new LinkedHashMap<>();

    private static int nextNoteID = 1;

    private static final String ID_KEY = "id";
    private static final String DATE_KEY = "date";
    private static final String MESSAGE_KEY = "message";

    /**
     * Adds a note.
     *
     * @param message
     * The note text.
     */
    @RequestMethod("POST")
    public synchronized void addNote(String message) {
        notes.put(nextNoteID, mapOf(
            entry(ID_KEY, nextNoteID),
            entry(DATE_KEY, new Date()),
            entry(MESSAGE_KEY, message)
        ));

        nextNoteID++;
    }

    /**
     * Removes a note.
     *
     * @param id
     * The note ID.
     */
    @RequestMethod("DELETE")
    public synchronized void deleteNote(int id) {
        notes.remove(id);
    }

    /**
     * Retrieves a list of all notes.
     *
     * @return
     * A list of all notes.
     */
    @RequestMethod("GET")
    public synchronized List<Map<String, ?>> getNotes() {
        return new ArrayList<>(NoteServlet.notes.values());
    }
}
