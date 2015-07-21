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

package vellum.webrpc;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet that hosts web RPC services.
 */
public class WebRPCServlet extends HttpServlet {
    private static final long serialVersionUID = 0;

    // HTTP servlet request roles
    private static class HttpServletRequestRoles implements Roles {
        private HttpServletRequest request;

        public HttpServletRequestRoles(HttpServletRequest request) {
            this.request = request;
        }

        @Override
        public boolean isUserInRole(String role) {
            return request.isUserInRole(role);
        }
    }

    private Class<?> serviceType = null;

    private HashMap<String, Method> methods = new HashMap<>();

    private static final String JSON_MIME_TYPE = "application/json; charset=UTF-8";

    @Override
    public void init() throws ServletException {
        String serviceClassName = getServletConfig().getInitParameter("serviceClassName");

        try {
            serviceType = Class.forName(serviceClassName);
        } catch (ClassNotFoundException exception) {
            throw new ServletException(exception);
        }

        if (!WebRPCService.class.isAssignableFrom(serviceType)) {
            throw new ServletException("Invalid service type.");
        }

        Method[] methods = serviceType.getMethods();

        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];

            if (method.getDeclaringClass() != Object.class) {
                this.methods.put(method.getName(), method);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        // Look up service method
        String pathInfo = request.getPathInfo();

        if (pathInfo == null) {
            throw new ServletException("Method name not specified.");
        }

        Method method = methods.get(pathInfo.substring(1));

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
            } else {
                argument = coerce(request.getParameter(name), type);
            }

            arguments[i] = argument;
        }

        // Execute method
        WebRPCService service;
        try {
            service = (WebRPCService)serviceType.newInstance();
        } catch (IllegalAccessException | InstantiationException exception) {
            throw new ServletException(exception);
        }

        service.initialize(request.getLocale(), request.getUserPrincipal(), new HttpServletRequestRoles(request));

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

            writeValue(response.getWriter(), result);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        doGet(request, response);
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

    private static void writeValue(Writer writer, Object value) throws IOException {
        if (value == null) {
            writer.append(null);
        } else if (value instanceof String) {
            String string = (String)value;

            writer.append('"');

            for (int i = 0, n = string.length(); i < n; i++) {
                char c = string.charAt(i);

                switch (c) {
                    case '"':
                    case '\\':
                    case '/': {
                        writer.append("\\" + c);
                        break;
                    }

                    case '\b': {
                        writer.append("\\b");
                        break;
                    }

                    case '\f': {
                        writer.append("\\f");
                        break;
                    }

                    case '\n': {
                        writer.append("\\n");
                        break;
                    }

                    case '\r': {
                        writer.append("\\r");
                        break;
                    }

                    case '\t': {
                        writer.append("\\t");
                        break;
                    }

                    default: {
                        writer.append(c);
                    }
                }
            }

            writer.append('"');
        } else if (value instanceof Number || value instanceof Boolean) {
            writer.append(String.valueOf(value));
        } else {
            try {
                if (value instanceof List<?>) {
                    List<?> list = (List<?>)value;

                    writer.append('[');

                    int i = 0;

                    for (Object element : list) {
                        if (i > 0) {
                            writer.append(',');
                        }

                        writeValue(writer, element);

                        i++;
                    }

                    writer.append(']');
                } else if (value instanceof Map<?, ?>) {
                    Map<?, ?> map = (Map<?, ?>)value;

                    writer.append("{");

                    int i = 0;

                    for (Map.Entry<?, ?> entry : map.entrySet()) {
                        if (i > 0) {
                            writer.append(',');
                        }

                        Object key = entry.getKey();

                        if (!(key instanceof String)) {
                            throw new IOException("Invalid key type.");
                        }

                        writeValue(writer, key);

                        writer.append(':');

                        writeValue(writer, entry.getValue());

                        i++;
                    }

                    writer.append("}");
                } else {
                    throw new IOException("Invalid value type.");
                }
            } finally {
                if (value instanceof AutoCloseable) {
                    try {
                        ((AutoCloseable)value).close();
                    } catch (Exception exception) {
                        throw new IOException(exception);
                    }
                }
            }
        }
    }
}
