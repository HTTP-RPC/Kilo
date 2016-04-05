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

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.httprpc.Template;
import org.httprpc.WebService;

/**
 * Simple note management service demo.
 */
public class NoteService extends WebService {
    private static LinkedHashMap<Integer, Map<String, ?>> notes = new LinkedHashMap<>();

    private static int nextNoteID = 1;

    private static final String ID_KEY = "id";
    private static final String DATE_KEY = "date";
    private static final String SUBJECT_KEY = "subject";
    private static final String BODY_KEY = "body";

    /**
     * Adds a note to the database.
     *
     * @param subject
     * The note subject.
     *
     * @param body
     * The note body.
     */
    public void addNote(String subject, String body) {
        synchronized (notes) {
            notes.put(nextNoteID, mapOf(
                entry(ID_KEY, nextNoteID),
                entry(DATE_KEY, new Date().getTime()),
                entry(SUBJECT_KEY, subject),
                entry(BODY_KEY, body))
            );

            nextNoteID++;
        }
    }

    /**
     * Deletes a note from the database.
     *
     * @param id
     * The note ID.
     */
    public void deleteNote(int id) {
        synchronized (notes) {
            notes.remove(id);
        }
    }

    /**
     * Lists all notes in the database.
     *
     * @return
     * A list of all notes.
     */
    @Template("note_list.html")
    public List<Map<String, ?>> listNotes() {
        LinkedList<Map<String, ?>> noteList = new LinkedList<>();

        synchronized (notes) {
            for (Map<String, ?> note : notes.values()) {
                noteList.add(mapOf(
                    entry(ID_KEY, note.get(ID_KEY)),
                    entry(DATE_KEY, note.get(DATE_KEY)),
                    entry(SUBJECT_KEY, note.get(SUBJECT_KEY))
                ));
            }
        }

        return noteList;
    }

    /**
     * Retrieves a note detail.
     *
     * @param id
     * The note ID.
     *
     * @return
     * The note detail.
     */
    @Template("note_detail.html")
    public Map<String, ?> getNoteDetail(int id) {
        Map<String, ?> noteDetail;
        synchronized (notes) {
            noteDetail = notes.get(id);
        }

        return noteDetail;
    }
}
