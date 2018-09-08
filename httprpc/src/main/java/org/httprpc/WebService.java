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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.httprpc.beans.BeanAdapter;

/**
 * Abstract base class for REST-based web services.
 */
public abstract class WebService extends HttpServlet {
    private static final long serialVersionUID = 0;

    private static class Resource {
        private static List<String> order = Arrays.asList("get", "post", "put", "patch", "delete");

        public final TreeMap<String, LinkedList<Method>> handlerMap = new TreeMap<>((verb1, verb2) -> {
            int i1 = order.indexOf(verb1);
            int i2 = order.indexOf(verb2);

            return Integer.compare((i1 == -1) ? order.size() : i1,  (i2 == -1) ? order.size() : i2);
        });

        public final TreeMap<String, Resource> resources = new TreeMap<>();

        public String key = null;
    }

    private Resource root = null;

    private ThreadLocal<HttpServletRequest> request = new ThreadLocal<>();
    private ThreadLocal<HttpServletResponse> response = new ThreadLocal<>();

    private ThreadLocal<ArrayList<String>> keyList = new ThreadLocal<>();
    private ThreadLocal<HashMap<String, String>> keyMap = new ThreadLocal<>();

    private static final String PATH_VARIABLE = "?";

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

                            if (component.startsWith(PATH_VARIABLE)) {
                                int k = PATH_VARIABLE.length();

                                if (component.length() > k) {
                                    if (component.charAt(k++) != ':') {
                                        throw new ServletException("Invalid path component.");
                                    }

                                    child.key = component.substring(k);

                                    component = PATH_VARIABLE;
                                }
                            }

                            resource.resources.put(component, child);
                        }

                        resource = child;
                    }
                }

                String verb = requestMethod.value().toLowerCase();

                LinkedList<Method> handlerList = resource.handlerMap.get(verb);

                if (handlerList == null) {
                    handlerList = new LinkedList<>();

                    resource.handlerMap.put(verb, handlerList);
                }

                handlerList.add(method);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String verb = request.getMethod().toLowerCase();
        String pathInfo = request.getPathInfo();

        if (verb.equals("get") && pathInfo == null) {
            String queryString = request.getQueryString();

            if (queryString != null && queryString.equals("description")) {
                describeService(request, response);
                return;
            }
        }

        Resource resource = root;

        ArrayList<String> keyList = new ArrayList<>();
        HashMap<String, String> keyMap = new HashMap<>();

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
                        super.service(request, response);
                        return;
                    }

                    keyList.add(component);

                    if (child.key != null) {
                        keyMap.put(child.key, component);
                    }
                }

                resource = child;
            }
        }

        List<Method> handlerList = resource.handlerMap.get(verb);

        if (handlerList == null) {
            super.service(request, response);
            return;
        }

        if (request.getCharacterEncoding() == null) {
            request.setCharacterEncoding(UTF_8);
        }

        this.request.set(request);
        this.response.set(response);

        this.keyList.set(keyList);
        this.keyMap.set(keyMap);

        LinkedList<File> files = new LinkedList<>();

        try {
            Map<String, List<?>> parameterMap = new HashMap<>();

            Enumeration<String> parameterNames = request.getParameterNames();

            while (parameterNames.hasMoreElements()) {
                String name = parameterNames.nextElement();

                parameterMap.put(name, Arrays.asList(request.getParameterValues(name)));
            }

            String contentType = request.getContentType();

            if (contentType != null && contentType.startsWith("multipart/form-data")) {
                for (Part part : request.getParts()) {
                    String submittedFileName = part.getSubmittedFileName();

                    if (submittedFileName == null || submittedFileName.length() == 0) {
                        continue;
                    }

                    String name = part.getName();

                    ArrayList<File> values = (ArrayList<File>)parameterMap.get(name);

                    if (values == null) {
                        values = new ArrayList<>();

                        parameterMap.put(name, values);
                    }

                    File file = File.createTempFile(part.getName(), "_" + submittedFileName);

                    files.add(file);
                    values.add(file);

                    part.write(file.getAbsolutePath());
                }
            }

            Method method = getMethod(handlerList, parameterMap);

            if (method == null) {
                response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                return;
            }

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
                        response.setContentType(String.format("text/plain;charset=%s", UTF_8));

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
                response.setContentType(String.format("application/json;charset=%s", UTF_8));

                JSONEncoder jsonEncoder = new JSONEncoder();

                jsonEncoder.writeValue(BeanAdapter.adapt(result), response.getOutputStream());
            } else {
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            }
        } finally {
            this.request.set(null);
            this.response.set(null);

            this.keyList.set(null);
            this.keyMap.set(null);

            for (File file : files) {
                file.delete();
            }
        }
    }

    private static Method getMethod(List<Method> handlerList, Map<String, List<?>> parameterMap) {
        Method method = null;

        int n = parameterMap.size();

        int i = Integer.MAX_VALUE;

        for (Method handler : handlerList) {
            Parameter[] parameters = handler.getParameters();

            if (parameters.length >= n) {
                int j = 0;

                for (int k = 0; k < parameters.length; k++) {
                    String name = getName(parameters[k]);

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

    private static Object[] getArguments(Method method, Map<String, List<?>> parameterMap) throws IOException {
        Parameter[] parameters = method.getParameters();

        Object[] arguments = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];

            String name = getName(parameter);

            Class<?> type = parameter.getType();

            List<?> values = parameterMap.get(name);

            Object argument;
            if (type == List.class) {
                Type elementType = ((ParameterizedType)parameter.getParameterizedType()).getActualTypeArguments()[0];

                if (!(elementType instanceof Class<?>)) {
                    throw new UnsupportedOperationException("Unsupported argument type.");
                }

                List<Object> list;
                if (values != null) {
                    list = new ArrayList<>(values.size());

                    for (Object value : values) {
                        list.add(getArgument(value, (Class<?>)elementType));
                    }
                } else {
                    list = Collections.emptyList();
                }

                argument = list;
            } else {
                Object value;
                if (values != null) {
                    value = values.get(values.size() - 1);
                } else {
                    value = null;
                }

                argument = getArgument(value, type);
            }

            arguments[i] = argument;
        }

        return arguments;
    }

    private static String getName(Parameter parameter) {
        RequestParameter requestParameter = parameter.getAnnotation(RequestParameter.class);

        return (requestParameter == null) ? parameter.getName() : requestParameter.value();
    }

    private static Object getArgument(Object value, Class<?> type) {
        Object argument;
        if (type == URL.class) {
            if (value == null) {
                argument = null;
            } else if (value instanceof File) {
                try {
                    argument = ((File)value).toURI().toURL();
                } catch (MalformedURLException exception) {
                    throw new RuntimeException(exception);
                }
            } else {
                throw new IllegalArgumentException();
            }
        } else {
            argument = BeanAdapter.adapt(value, type);
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
     * Returns the value of a key in the request path.
     *
     * @param index
     * The index of the key to return.
     *
     * @return
     * The key value.
     */
    protected String getKey(int index) {
        return keyList.get().get(index);
    }

    /**
     * Returns the value of a key in the request path.
     *
     * @param name
     * The name of the key to return.
     *
     * @return
     * The key value.
     */
    protected String getKey(String name) {
        return keyMap.get().get(name);
    }

    private void describeService(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType(String.format("text/html;charset=%s", UTF_8));

        XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();

        try {
            XMLStreamWriter xmlStreamWriter = xmlOutputFactory.createXMLStreamWriter(response.getWriter());

            xmlStreamWriter.writeStartElement("html");
            xmlStreamWriter.writeStartElement("body");

            TreeMap<Class<?>, String> structures = new TreeMap<>((type1, type2) -> {
                return type1.getSimpleName().compareTo(type2.getSimpleName());
            });

            describeResource(request.getServletPath(), root, structures, xmlStreamWriter);

            for (Map.Entry<Class<?>, String> entry : structures.entrySet()) {
                Class<?> type = entry.getKey();

                if (type == URL.class) {
                    continue;
                }

                String name = type.getSimpleName();

                xmlStreamWriter.writeStartElement("h3");
                xmlStreamWriter.writeAttribute("id", name);
                xmlStreamWriter.writeCharacters(name);
                xmlStreamWriter.writeEndElement();

                xmlStreamWriter.writeStartElement("pre");
                xmlStreamWriter.writeCharacters(entry.getValue());
                xmlStreamWriter.writeEndElement();
            }

            xmlStreamWriter.writeEndElement();
            xmlStreamWriter.writeEndElement();

            xmlStreamWriter.close();
        } catch (XMLStreamException exception) {
            throw new IOException(exception);
        }
    }

    private void describeResource(String path, Resource resource, TreeMap<Class<?>, String> structures, XMLStreamWriter xmlStreamWriter) throws XMLStreamException {
        if (!resource.handlerMap.isEmpty()) {
            xmlStreamWriter.writeStartElement("h2");
            xmlStreamWriter.writeCharacters(path);
            xmlStreamWriter.writeEndElement();

            for (Map.Entry<String, LinkedList<Method>> entry : resource.handlerMap.entrySet()) {
                for (Method method : entry.getValue()) {
                    xmlStreamWriter.writeStartElement("pre");

                    xmlStreamWriter.writeCharacters(entry.getKey().toUpperCase() + " (");

                    Parameter[] parameters = method.getParameters();

                    for (int i = 0; i < parameters.length; i++) {
                        Parameter parameter = parameters[i];

                        if (i > 0) {
                            xmlStreamWriter.writeCharacters(", ");
                        }

                        xmlStreamWriter.writeCharacters(getName(parameter) + ": ");

                        Type type = parameter.getParameterizedType();

                        if (type == URL.class) {
                            xmlStreamWriter.writeCharacters("file");
                        } else if (type instanceof ParameterizedType
                            && ((ParameterizedType)type).getRawType() == List.class
                            && ((ParameterizedType)type).getActualTypeArguments()[0] == URL.class) {
                            xmlStreamWriter.writeCharacters("[file]");
                        } else {
                            xmlStreamWriter.writeCharacters(BeanAdapter.describe(type, structures));
                        }
                    }

                    xmlStreamWriter.writeCharacters(") -> ");

                    Type type = method.getGenericReturnType();
                    Response response = method.getAnnotation(Response.class);

                    if ((type == Void.class || type == Void.TYPE) && response != null) {
                        xmlStreamWriter.writeCharacters(response.value());
                    } else {
                        String description = BeanAdapter.describe(type, structures);

                        if (structures.containsKey(type)) {
                            xmlStreamWriter.writeStartElement("a");
                            xmlStreamWriter.writeAttribute("href", "#" + description);
                            xmlStreamWriter.writeCharacters(description);
                            xmlStreamWriter.writeEndElement();
                        } else {
                            xmlStreamWriter.writeCharacters(description);
                        }
                    }

                    xmlStreamWriter.writeEndElement();
                }
            }
        }

        for (Map.Entry<String, Resource> entry : resource.resources.entrySet()) {
            String component = entry.getKey();

            Resource child = entry.getValue();

            if (child.key != null) {
                component += ":" + child.key;
            }

            describeResource(path + "/" + component, child, structures, xmlStreamWriter);
        }
    }
}

