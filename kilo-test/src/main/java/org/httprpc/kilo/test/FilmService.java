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

import jakarta.servlet.annotation.WebServlet;
import org.httprpc.kilo.Description;
import org.httprpc.kilo.RequestMethod;
import org.httprpc.kilo.ResourcePath;
import org.httprpc.kilo.beans.BeanAdapter;
import org.httprpc.kilo.sql.QueryBuilder;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.httprpc.kilo.util.Collections.*;
import static org.httprpc.kilo.util.Optionals.*;

@WebServlet(urlPatterns = {"/films/*"}, loadOnStartup = 1)
@Description("Film example service.")
public class FilmService extends AbstractDatabaseService {
    @Override
    protected Connection openConnection() throws SQLException {
        DataSource dataSource;
        try {
            var initialContext = new InitialContext();

            dataSource = (DataSource)initialContext.lookup("java:comp/env/jdbc/SakilaDB");
        } catch (NamingException exception) {
            throw new IllegalStateException(exception);
        }

        return dataSource.getConnection();
    }

    @RequestMethod("GET")
    @Description("Returns a list of all films.")
    public List<Film> getFilms(
        @Description("An optional name pattern to match. An asterisk may be used as a wildcard.") String match
    ) throws SQLException {
        var queryBuilder = QueryBuilder.select(Film.class);

        if (match != null) {
            queryBuilder.filterByIndexLike("match");
        }

        queryBuilder.ordered(true);

        try (var statement = queryBuilder.prepare(getConnection());
            var results = queryBuilder.executeQuery(statement, mapOf(
                entry("match", map(match, value -> value.replace('*', '%')))
            ))) {
            return results.stream().map(result -> BeanAdapter.coerce(result, Film.class)).toList();
        }
    }

    @RequestMethod("GET")
    @ResourcePath("?")
    @Description("Returns detailed information about a specific film.")
    public FilmDetail getFilm(
        @Description("The film ID.") Integer filmID
    ) throws SQLException {
        var queryBuilder = QueryBuilder.select(FilmDetail.class).filterByPrimaryKey("filmID");

        FilmDetail film;
        try (var statement = queryBuilder.prepare(getConnection());
            var results = queryBuilder.executeQuery(statement, mapOf(
                entry("filmID", filmID)
            ))) {
            film = results.stream().findFirst().map(result -> BeanAdapter.coerce(result, FilmDetail.class)).orElseThrow();
        }

        film.setActors(getActors(filmID));
        film.setCategories(getCategories(filmID));

        return film;
    }

    private List<Actor> getActors(Integer filmID) throws SQLException {
        var queryBuilder = QueryBuilder.select(Actor.class)
            .join(FilmActor.class, Actor.class)
            .filterByForeignKey(FilmActor.class, Film.class, "filmID")
            .ordered(true);

        try (var statement = queryBuilder.prepare(getConnection());
            var results = queryBuilder.executeQuery(statement, mapOf(
                entry("filmID", filmID)
            ))) {
            return results.stream().map(result -> BeanAdapter.coerce(result, Actor.class)).toList();
        }
    }

    private List<Category> getCategories(Integer filmID) throws SQLException {
        var queryBuilder = QueryBuilder.select(Category.class)
            .join(FilmCategory.class, Category.class)
            .filterByForeignKey(FilmCategory.class, Film.class, "filmID")
            .ordered(true);

        try (var statement = queryBuilder.prepare(getConnection());
            var results = queryBuilder.executeQuery(statement, mapOf(
                entry("filmID", filmID)
            ))) {
            return results.stream().map(result -> BeanAdapter.coerce(result, Category.class)).toList();
        }
    }
}
