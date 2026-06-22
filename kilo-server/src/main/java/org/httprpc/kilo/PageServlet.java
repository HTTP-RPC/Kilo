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

package org.httprpc.kilo;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.httprpc.kilo.beans.BeanAdapter;
import org.httprpc.kilo.io.TemplateEncoder;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.TimeZone;

import static org.httprpc.kilo.util.Collections.*;
import static org.httprpc.kilo.util.Iterables.*;
import static org.httprpc.kilo.util.Optionals.*;

/**
 * Abstract base class for page servlets.
 */
public abstract class PageServlet extends HttpServlet {
    private static final ThreadLocal<HttpServletRequest> request = new ThreadLocal<>();
    private static final ThreadLocal<HttpServletResponse> response = new ThreadLocal<>();

    @Override
    public void init() throws ServletException {
        var fields = getClass().getDeclaredFields();

        for (var i = 0; i < fields.length; i++) {
            var field = fields[i];

            var fieldType = field.getType();

            if (WebService.class.isAssignableFrom(fieldType) && field.getAnnotation(WebService.Instance.class) != null) {
                field.setAccessible(true);

                try {
                    field.set(this, WebService.instances.get(fieldType));
                } catch (IllegalAccessException exception) {
                    throw new UnsupportedOperationException(exception);
                }
            }
        }
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!request.getMethod().equalsIgnoreCase("GET")) {
            response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        try (var connection = openConnection()) {
            if (connection != null) {
                connection.setReadOnly(true);
            }

            WebService.setConnection(connection);

            try {
                process(request, response);
            } catch (Exception exception) {
                log(exception.getMessage(), exception);

                throw exception;
            } finally {
                if (connection != null) {
                    connection.setReadOnly(false);
                }
            }
        } catch (SQLException exception) {
            throw new ServletException(exception);
        } finally {
            WebService.setConnection(null);
        }
    }

    /**
     * Opens a database connection.
     *
     * @return
     * A database connection, or {@code null} if the page does not require a
     * database connection.
     */
    protected Connection openConnection() throws SQLException {
        return null;
    }

    /**
     * Processes a page request.
     *
     * @param request
     * The servlet request.
     *
     * @param response
     * The servlet response.
     *
     * @throws IOException
     * If an I/O error occurs while processing the request.
     */
    protected void process(HttpServletRequest request, HttpServletResponse response) throws IOException {
        PageServlet.request.set(request);
        PageServlet.response.set(response);

        Object result;
        try {
            try {
                result = execute();
            } catch (IllegalArgumentException | UnsupportedOperationException exception) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            } finally {
                PageServlet.request.remove();
                PageServlet.response.remove();
            }
        } catch (Exception exception) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

            log(exception.getMessage(), exception);

            return;
        }

        if (response.isCommitted()) {
            return;
        }

        encodeResult(request, response, result);
    }

    /**
     * Executes a page request.
     *
     * @return
     * The the result of executing the page request.
     */
    protected abstract Object execute() throws Exception;

    /**
     * Returns the database connection.
     *
     * @return
     * The database connection, or {@code null} if a database connection has
     * not been established.
     */
    protected static Connection getConnection() {
        return WebService.getConnection();
    }

    /**
     * Returns the servlet request.
     *
     * @return
     * The servlet request.
     */
    protected static HttpServletRequest getRequest() {
        return request.get();
    }

    /**
     * Returns the servlet response.
     *
     * @return
     * The servlet response.
     */
    protected static HttpServletResponse getResponse() {
        return response.get();
    }

    /**
     * Returns the request parameters.
     *
     * @param <P>
     * The type representing the parameters.
     *
     * @param type
     * The type representing the parameters.
     *
     * @return
     * The request parameters.
     */
    protected static <P> P getParameters(Class<P> type) {
        if (type == null) {
            throw new IllegalArgumentException();
        }

        var parameterMap = getRequest().getParameterMap();

        return BeanAdapter.coerce(mapOf(mapAll(BeanAdapter.getProperties(type).entrySet(), entry -> {
            var name = entry.getKey();
            var accessor = entry.getValue().getAccessor();

            var values = coalesce(map(parameterMap.get(name), Arrays::asList), () -> emptyListOf(String.class));

            Object value;
            if (Collection.class.isAssignableFrom(accessor.getReturnType())) {
                value = values;
            } else {
                value = firstOf(values);
            }

            return entry(name, value);
        })), type);
    }

    /**
     * Encodes the result of a page request.
     *
     * @param request
     * The servlet request.
     *
     * @param response
     * The servlet response.
     *
     * @param result
     * The value to encode.
     *
     * @throws IOException
     * If an error occurs while encoding the result.
     */
    protected void encodeResult(HttpServletRequest request, HttpServletResponse response, Object result) throws IOException {
        response.setContentType(WebService.TEXT_HTML);

        var type = getClass();

        var templateEncoder = new TemplateEncoder(type, String.format("%s.html", type.getSimpleName()));

        var locale = request.getLocale();

        templateEncoder.setLocale(locale);

        var timeZoneID = coalesce(request.getHeader("Time-Zone"), () -> "GMT");

        templateEncoder.setTimeZone(TimeZone.getTimeZone(timeZoneID));

        ResourceBundle resourceBundle;
        try {
            resourceBundle = ResourceBundle.getBundle(type.getName(), locale);
        } catch (MissingResourceException exception) {
            resourceBundle = null;
        }

        templateEncoder.setResourceBundle(resourceBundle);

        templateEncoder.write(result, response.getWriter());
    }
}
