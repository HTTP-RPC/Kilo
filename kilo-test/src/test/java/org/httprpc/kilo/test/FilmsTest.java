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

package org.httprpc.kilo.test;

import org.httprpc.kilo.WebServiceProxy;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

public class FilmsTest {
    private static final URI baseURI = URI.create("http://localhost:8080/kilo-test/");

    @Test
    public void testFilms() throws IOException {
        var filmServiceProxy = WebServiceProxy.of(FilmServiceProxy.class, baseURI);

        var films = filmServiceProxy.getFilms("n*");

        assertFalse(films.isEmpty());

        var film = filmServiceProxy.getFilm(films.get(0).getID());

        assertEquals('n', Character.toLowerCase(film.getTitle().charAt(0)));

        assertFalse(film.getActors().isEmpty());
        assertFalse(film.getCategories().isEmpty());
    }
}
