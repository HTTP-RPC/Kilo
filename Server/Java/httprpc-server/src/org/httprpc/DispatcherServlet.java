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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Abstract base class for dispatcher servlets.
 */
public abstract class DispatcherServlet extends HttpServlet {
    private static final long serialVersionUID = 0;

    // Resource structure
    private static class Resource {
        public final HashMap<String, List<Method>> handlerMap = new HashMap<>();
        public final HashMap<String, Resource> resources = new HashMap<>();

        @Override
        public String toString() {
            return handlerMap.keySet().toString() + "; " + resources.toString();
        }
    }

    private Resource root = null;

    private ThreadLocal<HttpServletRequest> request = new ThreadLocal<>();
    private ThreadLocal<HttpServletResponse> response = new ThreadLocal<>();

    @Override
    public void init() throws ServletException {
        root = new Resource();

        Method[] methods = getClass().getMethods();

        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];

            RequestMethod requestMethod = method.getAnnotation(RequestMethod.class);

            if (requestMethod != null) {
                Resource resource = root;

                ResourcePath resourcePath = method.getAnnotation(ResourcePath.class);

                if (resourcePath != null) {
                    String[] components = resourcePath.value().split("/");

                    for (int j = 0; j < components.length; j++) {
                        String component = components[j];

                        if (component.length() == 0) {
                            continue;
                        }

                        Resource child = resource.resources.get(component);

                        if (child == null) {
                            child = new Resource();

                            resource.resources.put(component, child);
                        }

                        resource = child;
                    }
                }

                String verb = requestMethod.value().toLowerCase();

                List<Method> handlerList = resource.handlerMap.get(verb);

                if (handlerList == null) {
                    handlerList = new LinkedList<>();

                    resource.handlerMap.put(verb, handlerList);
                }

                handlerList.add(method);
            }
        }
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Look up handler list
        Resource resource = root;

        String pathInfo = request.getPathInfo();

        if (pathInfo != null) {
            String[] components = pathInfo.split("/");

            for (int i = 0; i < components.length; i++) {
                String component = components[i];

                if (component.length() == 0) {
                    continue;
                }

                Resource child = resource.resources.get(component);

                if (child == null) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }

                resource = child;
            }
        }

        List<Method> handlerList = resource.handlerMap.get(request.getMethod().toLowerCase());

        if (handlerList == null) {
            response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        // Look up handler method
        if (request.getCharacterEncoding() == null) {
            request.setCharacterEncoding("UTF-8");
        }

        Map<String, List<String>> parameterMap = getParameterMap(request);

        Method method = getMethod(handlerList, parameterMap);

        if (method == null) {
            response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        // Invoke handler method
        this.request.set(request);
        this.response.set(response);

        try {
            Object result;
            try {
                result = method.invoke(this, getArguments(method, parameterMap));
            } catch (InvocationTargetException | IllegalAccessException exception) {
                throw new ServletException(exception);
            }

            if (response.isCommitted()) {
                return;
            }

            Class<?> returnType = method.getReturnType();

            if (returnType != Void.TYPE && returnType != Void.class) {
                response.setContentType("application/json;charset=UTF-8");

                JSONEncoder jsonEncoder = new JSONEncoder();

                jsonEncoder.writeValue(result, response.getOutputStream());
            }
        } finally {
            this.request.set(null);
            this.response.set(null);
        }
    }

    private static Map<String, List<String>> getParameterMap(HttpServletRequest request) {
        Map<String, List<String>> parameterMap = new HashMap<>();

        Enumeration<String> parameterNames = request.getParameterNames();

        while (parameterNames.hasMoreElements()) {
            String name = parameterNames.nextElement();
            String[] values = request.getParameterValues(name);

            LinkedList<String> valueList = new LinkedList<>();

            for (int i = 0; i < values.length; i++) {
                valueList.add(values[i]);
            }

            parameterMap.put(name, valueList);
        }

        return parameterMap;
    }

    private static Method getMethod(List<Method> handlerList, Map<String, List<String>> parameterMap) {
        Method method = null;

        int n = parameterMap.size();

        int i = Integer.MAX_VALUE;

        for (Method handler : handlerList) {
            Parameter[] parameters = handler.getParameters();

            if (parameters.length >= n) {
                int j = 0;

                for (int k = 0; k < parameters.length; k++) {
                    String name = parameters[k].getName();

                    if (!(parameterMap.containsKey(name))) {
                        j++;
                    }
                }

                if (parameters.length - j == n && j < i) {
                    method = handler;

                    i = j;
                }
            }
        }

        return method;
    }

    private static Object[] getArguments(Method method, Map<String, List<String>> parameterMap) throws IOException {
        Parameter[] parameters = method.getParameters();

        Object[] arguments = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];

            String name = parameter.getName();
            Class<?> type = parameter.getType();

            Object argument;
            if (type == List.class) {
                ParameterizedType parameterizedType = (ParameterizedType)parameter.getParameterizedType();
                Type elementType = parameterizedType.getActualTypeArguments()[0];

                List<String> valueList = parameterMap.get(name);

                List<Object> list;
                if (valueList != null) {
                    int n = valueList.size();

                    list = new ArrayList<>(n);

                    for (String value : valueList) {
                        list.add(getArgument(value, elementType));
                    }
                } else {
                    list = Collections.emptyList();
                }

                argument = Collections.unmodifiableList(list);
            } else {
                List<String> valueList = parameterMap.get(name);

                String value;
                if (valueList != null) {
                    value = valueList.get(0);
                } else {
                    value = null;
                }

                argument = getArgument(value, type);
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
        } else if (type == Date.class) {
            argument = new Date(Long.parseLong(value));
        } else if (type == LocalDate.class) {
            argument = LocalDate.parse(value);
        } else if (type == LocalTime.class) {
            argument = LocalTime.parse(value);
        } else if (type == LocalDateTime.class) {
            argument = LocalDateTime.parse(value);
        } else {
            throw new UnsupportedOperationException("Invalid parameter type.");
        }

        return argument;
    }

    /**
     * Returns the servlet request.
     *
     * @return
     * The servlet request.
     */
    protected HttpServletRequest getRequest() {
        return request.get();
    }

    /**
     * Returns the servlet response.
     *
     * @return
     * The servlet response.
     */
    protected HttpServletResponse getResponse() {
        return response.get();
    }
}
