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

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.security.Principal;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

/**
 * Servlet that dispatches HTTP-RPC web service requests.
 */
@MultipartConfig
public class RequestDispatcherServlet extends HttpServlet {
    private static final long serialVersionUID = 0;

    // User role set
    private static class UserRoleSet extends AbstractSet<String> {
        private HttpServletRequest request;

        public UserRoleSet(HttpServletRequest request) {
            this.request = request;
        }

        @Override
        public boolean contains(Object object) {
            return request.isUserInRole(object.toString());
        }

        @Override
        public int size() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Iterator<String> iterator() {
            throw new UnsupportedOperationException();
        }
    }

    // Attachment collection
    private static class AttachmentCollection extends AbstractCollection<Attachment> {
        private HttpServletRequest request;

        public AttachmentCollection(HttpServletRequest request) {
            this.request = request;
        }

        @Override
        public int size() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Iterator<Attachment> iterator() {
            Iterator<Attachment> iterator;
            try {
                iterator = new Iterator<Attachment>() {
                    private Iterator<Part> parts = request.getParts().iterator();

                    @Override
                    public boolean hasNext() {
                        return parts.hasNext();
                    }

                    @Override
                    public Attachment next() {
                        if (!hasNext()) {
                            throw new NoSuchElementException();
                        }

                        return new Attachment() {
                            private Part part = parts.next();

                            @Override
                            public String getName() {
                                return part.getName();
                            }

                            @Override
                            public String getFileName() {
                                return part.getSubmittedFileName();
                            }

                            @Override
                            public String getContentType() {
                                return part.getContentType();
                            }

                            @Override
                            public long getSize() {
                                return part.getSize();
                            }

                            @Override
                            public InputStream getInputStream() throws IOException {
                                return part.getInputStream();
                            }
                        };
                    }
                };
            } catch (IOException | ServletException exception) {
                throw new RuntimeException(exception);
            }

            return iterator;
        }
    }

    private Class<?> serviceType = null;

    private HashMap<String, Method> methodMap = new HashMap<>();

    private static final String UTF_8_ENCODING = "UTF-8";

    private static final String JSON_MIME_TYPE = "application/json";

    @Override
    public void init() throws ServletException {
        String serviceClassName = getServletConfig().getInitParameter("serviceClassName");

        try {
            serviceType = Class.forName(serviceClassName);
        } catch (ClassNotFoundException exception) {
            throw new ServletException(exception);
        }

        if (!WebService.class.isAssignableFrom(serviceType)) {
            throw new ServletException("Invalid service type.");
        }

        Method[] methods = serviceType.getMethods();

        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];

            if (serviceType.isAssignableFrom(method.getDeclaringClass())
                && !java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                methodMap.put(method.getName(), method);
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        doPost(request, response);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String pathInfo = request.getPathInfo();

        if (pathInfo != null) {
            Locale locale = request.getLocale();

            // Look up service method
            pathInfo = pathInfo.substring(1);

            Method method = methodMap.get(pathInfo);

            if (method == null) {
                throw new ServletException("Method not found.");
            }

            // Construct arguments
            Parameter[] parameters = method.getParameters();

            Object[] arguments = new Object[parameters.length];

            for (int i = 0; i < parameters.length; i++) {
                Parameter parameter = parameters[i];

                String name = parameter.getName();
                Class<?> type = parameter.getType();

                Object argument;
                if (type == List.class) {
                    String[] values = request.getParameterValues(name);

                    List<Object> list;
                    if (values != null) {
                        ParameterizedType parameterizedType = (ParameterizedType)parameter.getParameterizedType();
                        Type elementType = parameterizedType.getActualTypeArguments()[0];

                        int n = values.length;

                        list = new ArrayList<>(n);

                        for (int j = 0; j < n; j++) {
                            list.add(coerce(values[j], elementType));
                        }
                    } else {
                        list = Collections.EMPTY_LIST;
                    }

                    argument = list;
                } else if (type == Map.class) {
                    String[] values = request.getParameterValues(name);

                    Map<String, Object> map;
                    if (values != null) {
                        ParameterizedType parameterizedType = (ParameterizedType)parameter.getParameterizedType();
                        Type valueType = parameterizedType.getActualTypeArguments()[1];

                        map = new LinkedHashMap<>();

                        for (int j = 0; j < values.length; j++) {
                            String[] entry = values[j].split(":");

                            if (entry.length != 2) {
                                throw new ServletException("Invalid map entry.");
                            }

                            String key = URLDecoder.decode(entry[0], UTF_8_ENCODING);
                            String value = URLDecoder.decode(entry[1], UTF_8_ENCODING);

                            map.put(key, coerce(value, valueType));
                        }
                    } else {
                        map = Collections.EMPTY_MAP;
                    }

                    argument = map;
                } else {
                    argument = coerce(request.getParameter(name), type);
                }

                arguments[i] = argument;
            }

            // Execute method
            WebService service;
            try {
                service = (WebService)serviceType.newInstance();
            } catch (IllegalAccessException | InstantiationException exception) {
                throw new ServletException(exception);
            }

            service.setLocale(locale);

            Principal userPrincipal = request.getUserPrincipal();

            if (userPrincipal != null) {
                service.setUserName(userPrincipal.getName());
                service.setUserRoles(new UserRoleSet(request));
            }

            String contentType = request.getContentType();

            Iterable<Attachment> attachments;
            if (contentType != null && contentType.startsWith("multipart/form-data")) {
                attachments = new AttachmentCollection(request);
            } else {
                attachments = Collections.emptyList();
            }

            service.setAttachments(attachments);

            Object result;
            try {
                result = method.invoke(service, arguments);
            } catch (IllegalAccessException | InvocationTargetException exception) {
                throw new ServletException(exception);
            }

            // Write response
            Class<?> returnType = method.getReturnType();

            if (returnType != Void.TYPE && returnType != Void.class) {
                response.setContentType(JSON_MIME_TYPE);

                writeValue(response.getWriter(), result, 0);
            }
        }
    }

    private static Object coerce(String value, Type type) throws ServletException {
        Object argument;
        if (value == null || type == String.class) {
            argument = value;
        } else if (type == Byte.TYPE || type == Byte.class) {
            argument = Byte.parseByte(value);
        } else if (type == Short.TYPE || type == Short.class) {
            argument = Short.parseShort(value);
        } else if (type == Integer.TYPE || type == Integer.class) {
            argument = Integer.parseInt(value);
        } else if (type == Long.TYPE || type == Long.class) {
            argument = Long.parseLong(value);
        } else if (type == Float.TYPE || type == Float.class) {
            argument = Float.parseFloat(value);
        } else if (type == Double.TYPE || type == Double.class) {
            argument = Double.parseDouble(value);
        } else if (type == BigInteger.class) {
            argument = new BigInteger(value);
        } else if (type == BigDecimal.class) {
            argument = new BigDecimal(value);
        } else if (type == Boolean.TYPE || type == Boolean.class) {
            argument = Boolean.parseBoolean(value);
        } else {
            throw new ServletException("Invalid parameter type.");
        }

        return argument;
    }

    public void writeValue(PrintWriter writer, Object value, int depth) throws IOException {
        if (writer.checkError()) {
            throw new IOException("Error writing to output stream.");
        }

        if (value == null) {
            writer.append(null);
        } else if (value instanceof String) {
            String string = (String)value;

            writer.append("\"");

            for (int i = 0, n = string.length(); i < n; i++) {
                char c = string.charAt(i);

                if (c == '"' || c == '\\') {
                    writer.append("\\" + c);
                } else if (c == '\b') {
                    writer.append("\\b");
                } else if (c == '\f') {
                    writer.append("\\f");
                } else if (c == '\n') {
                    writer.append("\\n");
                } else if (c == '\r') {
                    writer.append("\\r");
                } else if (c == '\t') {
                    writer.append("\\t");
                } else {
                    writer.append(c);
                }
            }

            writer.append("\"");
        } else if (value instanceof Number || value instanceof Boolean) {
            writer.append(String.valueOf(value));
        } else if (value instanceof List<?>) {
            List<?> list = (List<?>)value;

            try {
                writer.append("[");

                depth++;

                int i = 0;

                for (Object element : list) {
                    if (i > 0) {
                        writer.append(",");
                    }

                    writer.append("\n");

                    indent(writer, depth);

                    writeValue(writer, element, depth);

                    i++;
                }

                depth--;

                writer.append("\n");

                indent(writer, depth);

                writer.append("]");
            } finally {
                if (list instanceof AutoCloseable) {
                    try {
                        ((AutoCloseable)list).close();
                    } catch (Exception exception) {
                        throw new IOException(exception);
                    }
                }
            }
        } else if (value instanceof Map<?, ?>) {
            Map<?, ?> map = (Map<?, ?>)value;

            try {
                writer.append("{");

                depth++;

                int i = 0;

                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (i > 0) {
                        writer.append(",");
                    }

                    writer.append("\n");

                    Object key = entry.getKey();

                    if (!(key instanceof String)) {
                        throw new IOException("Invalid key type.");
                    }

                    indent(writer, depth);

                    writer.append("\"" + key + "\": ");

                    writeValue(writer, entry.getValue(), depth);

                    i++;
                }

                depth--;

                writer.append("\n");

                indent(writer, depth);

                writer.append("}");
            } finally {
                if (map instanceof AutoCloseable) {
                    try {
                        ((AutoCloseable)map).close();
                    } catch (Exception exception) {
                        throw new IOException(exception);
                    }
                }
            }
        } else {
            throw new IOException("Invalid value type.");
        }
    }

    private static void indent(Writer writer, int depth) throws IOException {
        for (int i = 0; i < depth; i++) {
            writer.append("  ");
        }
    }
}
