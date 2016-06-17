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
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URLDecoder;
import java.security.Principal;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

    private Class<?> serviceType = null;

    private Map<String, Map<String, Method>> methodMaps = new HashMap<>();

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

            RPC rpc = method.getAnnotation(RPC.class);

            if (rpc != null) {
                String path = rpc.path();

                Map<String, Method> methodMap = methodMaps.get(path);

                if (methodMap == null) {
                    methodMap = new HashMap<>();

                    methodMaps.put(path, methodMap);
                }

                methodMap.put(rpc.method().toLowerCase(), method);
            }
        }
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String pathInfo = request.getPathInfo();

        if (pathInfo != null) {
            // Look up method
            pathInfo = pathInfo.substring(1);

            Map<String, Method> methodMap = methodMaps.get(pathInfo);

            if (methodMap == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            Method method = methodMap.get(request.getMethod().toLowerCase());

            if (method == null) {
                response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                return;
            }

            // Execute method
            Object result;
            try {
                WebService service;
                if (!Modifier.isStatic(method.getModifiers())) {
                    try {
                        service = (WebService)serviceType.newInstance();
                    } catch (IllegalAccessException | InstantiationException exception) {
                        throw new RuntimeException(exception);
                    }

                    service.setLocale(request.getLocale());

                    Principal userPrincipal = request.getUserPrincipal();

                    if (userPrincipal != null) {
                        service.setUserName(userPrincipal.getName());
                        service.setUserRoles(new UserRoleSet(request));
                    }
                } else {
                    service = null;
                }

                try {
                    result = method.invoke(service, getArguments(method.getParameters(), request));
                } catch (IllegalAccessException | InvocationTargetException exception) {
                    throw new RuntimeException(exception);
                }
            } catch (RuntimeException exception) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }

            // Write response
            Class<?> returnType = method.getReturnType();

            if (returnType == Void.TYPE || returnType == Void.class) {
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            } else {
                response.setContentType(JSON_MIME_TYPE);

                writeValue(response.getWriter(), result, 0);
            }
        }
    }

    private static Object[] getArguments(Parameter[] parameters, HttpServletRequest request) {
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
                        list.add(getArgument(values[j], elementType));
                    }
                } else {
                    list = Collections.emptyList();
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
                            throw new IllegalArgumentException("Invalid map entry.");
                        }

                        String key, value;
                        try {
                            key = URLDecoder.decode(entry[0], UTF_8_ENCODING);
                            value = URLDecoder.decode(entry[1], UTF_8_ENCODING);
                        } catch (UnsupportedEncodingException exception) {
                            throw new RuntimeException(exception);
                        }

                        map.put(key, getArgument(value, valueType));
                    }
                } else {
                    map = Collections.emptyMap();
                }

                argument = map;
            } else {
                argument = getArgument(request.getParameter(name), type);
            }

            arguments[i] = argument;
        }

        return arguments;
    }

    private static Object getArgument(String value, Type type) {
        Object argument;
        if (type == String.class) {
            argument = value;
        } else if (type == Byte.TYPE) {
            argument = (value == null) ? 0 : Byte.parseByte(value);
        } else if (type == Byte.class) {
            argument = (value == null) ? null : Byte.parseByte(value);
        } else if (type == Short.TYPE) {
            argument = (value == null) ? 0 : Short.parseShort(value);
        } else if (type == Short.class) {
            argument = (value == null) ? null : Short.parseShort(value);
        } else if (type == Integer.TYPE) {
            argument = (value == null) ? 0 : Integer.parseInt(value);
        } else if (type == Integer.class) {
            argument = (value == null) ? null : Integer.parseInt(value);
        } else if (type == Long.TYPE) {
            argument = (value == null) ? 0 : Long.parseLong(value);
        } else if (type == Long.class) {
            argument = (value == null) ? null : Long.parseLong(value);
        } else if (type == Float.TYPE) {
            argument = (value == null) ? 0 : Float.parseFloat(value);
        } else if (type == Float.class) {
            argument = (value == null) ? null : Float.parseFloat(value);
        } else if (type == Double.TYPE) {
            argument = (value == null) ? 0 : Double.parseDouble(value);
        } else if (type == Double.class) {
            argument = (value == null) ? null : Double.parseDouble(value);
        } else if (type == Boolean.TYPE) {
            argument = (value == null) ? false : Boolean.parseBoolean(value);
        } else if (type == Boolean.class) {
            argument = (value == null) ? null : Boolean.parseBoolean(value);
        } else {
            throw new UnsupportedOperationException("Invalid parameter type.");
        }

        return argument;
    }

    private static void writeValue(PrintWriter writer, Object value, int depth) throws IOException {
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
