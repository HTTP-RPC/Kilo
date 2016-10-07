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

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Simple note management service demo.
 */
public class NoteService {
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
    public void addNote(String message) {
        synchronized (notes) {
            notes.put(nextNoteID, mapOf(
                entry(ID_KEY, nextNoteID),
                entry(DATE_KEY, new Date().getTime()),
                entry(MESSAGE_KEY, message))
            );

            nextNoteID++;
        }
    }

    /**
     * Removes a note.
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
     * Retrieves a list of all notes.
     *
     * @return
     * A list of all notes.
     */
    public List<Map<String, ?>> getNotes() {
        LinkedList<Map<String, ?>> noteList = new LinkedList<>();

        synchronized (notes) {
            for (Map<String, ?> note : notes.values()) {
                noteList.add(mapOf(
                    entry(ID_KEY, note.get(ID_KEY)),
                    entry(DATE_KEY, note.get(DATE_KEY)),
                    entry(MESSAGE_KEY, note.get(MESSAGE_KEY))
                ));
            }
        }

        return noteList;
    }

    @SafeVarargs
    public static List<?> listOf(Object...elements) {
        return Collections.unmodifiableList(Arrays.asList(elements));
    }

    @SafeVarargs
    public static <K> Map<K, ?> mapOf(Map.Entry<K, ?>... entries) {
        HashMap<K, Object> map = new HashMap<>();

        for (Map.Entry<K, ?> entry : entries) {
            map.put(entry.getKey(), entry.getValue());
        }

        return Collections.unmodifiableMap(map);
    }

    public static <K> Map.Entry<K, ?> entry(K key, Object value) {
        return new AbstractMap.SimpleImmutableEntry<>(key, value);
    }
}
