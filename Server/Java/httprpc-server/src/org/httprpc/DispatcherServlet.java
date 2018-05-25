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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

/**
 * Abstract base class for HTTP-based web services.
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

    private ThreadLocal<List<String>> keys = new ThreadLocal<>();

    private static final String UTF_8 = "UTF-8";

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

        LinkedList<String> keys = new LinkedList<>();

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
                    child = resource.resources.get("?");

                    if (child == null) {
                        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        return;
                    }

                    keys.add(component);
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
            request.setCharacterEncoding(UTF_8);
        }

        LinkedList<File> files = new LinkedList<>();

        Map<String, ?> parameterMap = getParameterMap(request, files);

        Method method = getMethod(handlerList, parameterMap);

        if (method == null) {
            response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        // Invoke handler method
        this.request.set(request);
        this.response.set(response);

        this.keys.set(Collections.unmodifiableList(new ArrayList<>(keys)));

        try {
            Object result;
            try {
                result = method.invoke(this, getArguments(method, parameterMap));
            } catch (InvocationTargetException | IllegalAccessException exception) {
                if (response.isCommitted()) {
                    throw new ServletException(exception);
                } else {
                    Throwable cause = exception.getCause();

                    if (cause != null) {
                        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        response.setContentType(String.format("%s;charset=%s", "text/plain", UTF_8));

                        PrintWriter writer = response.getWriter();

                        writer.append(cause.getMessage());
                        writer.flush();

                        return;
                    } else {
                        throw new ServletException(exception);
                    }
                }
            }

            if (response.isCommitted()) {
                return;
            }

            Class<?> returnType = method.getReturnType();

            if (returnType != Void.TYPE && returnType != Void.class) {
                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType(String.format("%s;charset=%s", WebServiceProxy.APPLICATION_JSON, UTF_8));

                JSONEncoder jsonEncoder = new JSONEncoder();

                jsonEncoder.writeValue(result, response.getOutputStream());
            } else {
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            }
        } finally {
            this.request.set(null);
            this.response.set(null);

            this.keys.set(null);

            for (File file : files) {
                file.delete();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, ?> getParameterMap(HttpServletRequest request, List<File> files) throws ServletException, IOException {
        String contentType = request.getContentType();

        Map<String, Object> parameterMap;
        if (contentType != null && contentType.startsWith(WebServiceProxy.APPLICATION_JSON)) {
            JSONDecoder jsonDecoder = new JSONDecoder();

            parameterMap = (Map<String, Object>)jsonDecoder.readValue(request.getInputStream());
        } else {
            parameterMap = new HashMap<>();

            Enumeration<String> parameterNames = request.getParameterNames();

            while (parameterNames.hasMoreElements()) {
                String name = parameterNames.nextElement();

                parameterMap.put(name, Arrays.asList(request.getParameterValues(name)));
            }

            if (contentType != null && contentType.startsWith(WebServiceProxy.MULTIPART_FORM_DATA)) {
                for (Part part : request.getParts()) {
                    String submittedFileName = part.getSubmittedFileName();

                    if (submittedFileName == null || submittedFileName.length() == 0) {
                        continue;
                    }

                    String name = part.getName();

                    LinkedList<URL> urlList = (LinkedList<URL>)parameterMap.get(name);

                    if (urlList == null) {
                        urlList = new LinkedList<>();

                        parameterMap.put(name, urlList);
                    }

                    File file = File.createTempFile(part.getName(), "_" + submittedFileName);

                    part.write(file.getAbsolutePath());

                    urlList.add(file.toURI().toURL());

                    files.add(file);
                }
            }
        }

        return parameterMap;
    }

    private static Method getMethod(List<Method> handlerList, Map<String, ?> parameterMap) {
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

    @SuppressWarnings("unchecked")
    private static Object[] getArguments(Method method, Map<String, ?> parameterMap) throws IOException {
        Parameter[] parameters = method.getParameters();

        Object[] arguments = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];

            String name = parameter.getName();
            Class<?> type = parameter.getType();

            Object argument;
            if (type == List.class) {
                List<?> values = (List<?>)parameterMap.get(name);

                Type valueType = ((ParameterizedType)parameter.getParameterizedType()).getActualTypeArguments()[0];

                List<Object> list;
                if (values != null) {
                    list = new ArrayList<>(values.size());

                    for (Object value : values) {
                        list.add(getArgument(value, (valueType instanceof Class<?>) ? (Class<?>)valueType : Object.class));
                    }
                } else {
                    list = Collections.emptyList();
                }

                argument = list;
            } else if (type == Map.class) {
                Map<String, ?> values = (Map<String, ?>)parameterMap.get(name);

                Type valueType = ((ParameterizedType)parameter.getParameterizedType()).getActualTypeArguments()[1];

                Map<String, Object> map;
                if (values != null) {
                    map = new HashMap<>();

                    for (Map.Entry<String, ?> entry : values.entrySet()) {
                        map.put(entry.getKey(), getArgument(entry.getValue(), (valueType instanceof Class<?>) ? (Class<?>)valueType : Object.class));
                    }
                } else {
                    map = Collections.emptyMap();
                }

                argument = map;
            } else {
                Object value = parameterMap.get(name);

                if (value instanceof List<?>) {
                    value = ((List<?>)value).get(0);
                }

                argument = getArgument(value, type);
            }

            arguments[i] = argument;
        }

        return arguments;
    }

    private static Object getArgument(Object value, Class<?> type) {
        Object argument;
        if (type == String.class) {
            argument = (value == null) ? null : value.toString();
        } else if (type == Byte.TYPE || type == Byte.class) {
            if (value == null) {
                argument = (type == Byte.TYPE) ? 0 : null;
            } else if (value instanceof Number) {
                argument = ((Number)value).byteValue();
            } else {
                argument = Byte.parseByte(value.toString());
            }
        } else if (type == Short.TYPE || type == Short.class) {
            if (value == null) {
                argument = (type == Short.TYPE) ? 0 : null;
            } else if (value instanceof Number) {
                argument = ((Number)value).shortValue();
            } else {
                argument = Short.parseShort(value.toString());
            }
        } else if (type == Integer.TYPE || type == Integer.class) {
            if (value == null) {
                argument = (type == Integer.TYPE) ? 0 : null;
            } else if (value instanceof Number) {
                argument = ((Number)value).intValue();
            } else {
                argument = Integer.parseInt(value.toString());
            }
        } else if (type == Long.TYPE || type == Long.class) {
            if (value == null) {
                argument = (type == Long.TYPE) ? 0 : null;
            } else if (value instanceof Number) {
                argument = ((Number)value).longValue();
            } else {
                argument = Long.parseLong(value.toString());
            }
        } else if (type == Float.TYPE || type == Float.class) {
            if (value == null) {
                argument = (type == Float.TYPE) ? 0 : null;
            } else if (value instanceof Number) {
                argument = ((Number)value).floatValue();
            } else {
                argument = Float.parseFloat(value.toString());
            }
        } else if (type == Double.TYPE || type == Double.class) {
            if (value == null) {
                argument = (type == Double.TYPE) ? 0 : null;
            } else if (value instanceof Number) {
                argument = ((Number)value).doubleValue();
            } else {
                argument = Double.parseDouble(value.toString());
            }
        } else if (type == Boolean.TYPE || type == Boolean.class) {
            if (value == null) {
                argument = (type == Boolean.TYPE) ? false : null;
            } else {
                argument = Boolean.parseBoolean(value.toString());
            }
        } else {
            argument = value;
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

    /**
     * Returns the list of keys parsed from the request path.
     *
     * @return
     * The list of keys parsed from the request path.
     */
    protected List<String> getKeys() {
        return keys.get();
    }
}
