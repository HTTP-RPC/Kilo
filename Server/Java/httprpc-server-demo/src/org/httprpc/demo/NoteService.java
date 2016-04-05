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

import java.util.List;
import java.util.Map;

import org.httprpc.Template;
import org.httprpc.WebService;

/**
 * Simple note management service demo.
 */
public class NoteService extends WebService {
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
        // TODO
    }

    /**
     * Deletes a note from the database.
     *
     * @param id
     * The note ID.
     */
    public void deleteNote(int id) {
        // TODO
    }

    /**
     * Lists all notes in the database.
     *
     * @return
     * A list of all notes.
     */
    @Template("note_list.html")
    public List<Map<String, ?>> listNotes() {
        // TODO
        return null;
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
        // TODO
        return null;
    }
}
