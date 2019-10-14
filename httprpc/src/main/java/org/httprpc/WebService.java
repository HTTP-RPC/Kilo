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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
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
import org.httprpc.io.JSONEncoder;

/**
 * Abstract base class for web services.
 */
public abstract class WebService extends HttpServlet {
    private static final long serialVersionUID = 0;

    private static class Resource {
        private static List<String> order = Arrays.asList("get", "post", "put", "delete");

        public final TreeMap<String, LinkedList<Handler>> handlerMap = new TreeMap<>((verb1, verb2) -> {
            int i1 = order.indexOf(verb1);
            int i2 = order.indexOf(verb2);

            return Integer.compare((i1 == -1) ? order.size() : i1,  (i2 == -1) ? order.size() : i2);
        });

        public final TreeMap<String, Resource> resources = new TreeMap<>();
    }

    private static class Handler {
        public final Method method;

        public final ArrayList<String> keys = new ArrayList<>();

        public Handler(Method method) {
            this.method = method;
        }
    }

    private static class PartURLConnection extends URLConnection {
        private Part part;

        public PartURLConnection(URL url, Part part) {
            super(url);

            this.part = part;
        }

        @Override
        public void connect() {
            // No-op
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return part.getInputStream();
        }
    }

    private static class PartURLStreamHandler extends URLStreamHandler {
        private Part part;

        public PartURLStreamHandler(Part part) {
            this.part = part;
        }

        @Override
        protected URLConnection openConnection(URL url) {
            return new PartURLConnection(url, part);
        }
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
                Handler handler = new Handler(method);

                Resource resource = root;

                ResourcePath resourcePath = method.getAnnotation(ResourcePath.class);

                if (resourcePath != null) {
                    String[] components = resourcePath.value().split("/");

                    for (int j = 0; j < components.length; j++) {
                        String component = components[j];

                        if (component.length() == 0) {
                            continue;
                        }

                        if (component.startsWith(PATH_VARIABLE)) {
                            int k = PATH_VARIABLE.length();

                            String key;
                            if (component.length() > k) {
                                if (component.charAt(k++) != ':') {
                                    throw new ServletException("Invalid path component.");
                                }

                                key = component.substring(k);

                                component = PATH_VARIABLE;
                            } else {
                                key = null;
                            }

                            handler.keys.add(key);
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

                LinkedList<Handler> handlerList = resource.handlerMap.get(verb);

                if (handlerList == null) {
                    handlerList = new LinkedList<>();

                    resource.handlerMap.put(verb, handlerList);
                }

                handlerList.add(handler);
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

            if (queryString != null && queryString.equals("api")) {
                describeService(request, response);
                return;
            }
        }

        Resource resource = root;

        ArrayList<String> keyList = new ArrayList<>();

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
                }

                resource = child;
            }
        }

        List<Handler> handlerList = resource.handlerMap.get(verb);

        if (handlerList == null) {
            super.service(request, response);
            return;
        }

        if (request.getCharacterEncoding() == null) {
            request.setCharacterEncoding(UTF_8);
        }

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

                ArrayList<URL> values = (ArrayList<URL>)parameterMap.get(name);

                if (values == null) {
                    values = new ArrayList<>();

                    parameterMap.put(name, values);
                }

                values.add(new URL("part", null, -1, submittedFileName, new PartURLStreamHandler(part)));
            }
        }

        Handler handler = getHandler(handlerList, parameterMap);

        if (handler == null) {
            response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        if (!isAuthorized(request, handler.method)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        HashMap<String, String> keyMap = new HashMap<>();

        for (int i = 0, n = keyList.size(); i < n; i++) {
            String key = handler.keys.get(i);

            if (key != null) {
                keyMap.put(key, keyList.get(i));
            }
        }

        this.request.set(request);
        this.response.set(response);

        this.keyList.set(keyList);
        this.keyMap.set(keyMap);

        Object result;
        try {
            result = handler.method.invoke(this, getArguments(handler.method, parameterMap));
        } catch (InvocationTargetException | IllegalAccessException exception) {
            if (response.isCommitted()) {
                throw new ServletException(exception);
            } else {
                Throwable cause = exception.getCause();

                if (cause != null) {
                    int status;
                    if (cause instanceof IllegalArgumentException) {
                        status = HttpServletResponse.SC_FORBIDDEN;
                    } else if (cause instanceof IllegalStateException) {
                        status = HttpServletResponse.SC_CONFLICT;
                    } else {
                        status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
                    }

                    response.setStatus(status);

                    String message = cause.getMessage();

                    if (message != null) {
                        response.setContentType(String.format("text/plain;charset=%s", UTF_8));

                        PrintWriter writer = response.getWriter();

                        writer.append(message);

                        writer.flush();
                    }

                    return;
                } else {
                    throw new ServletException(exception);
                }
            }
        } finally {
            this.request.set(null);
            this.response.set(null);

            this.keyList.set(null);
            this.keyMap.set(null);
        }

        if (response.isCommitted()) {
            return;
        }

        Class<?> returnType = handler.method.getReturnType();

        if (returnType == Void.TYPE || returnType == Void.class) {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        } else if (result == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } else {
            response.setStatus(HttpServletResponse.SC_OK);

            encodeResult(request, response, result);
        }
    }

    private static Handler getHandler(List<Handler> handlerList, Map<String, List<?>> parameterMap) {
        Handler handler = null;

        int n = parameterMap.size();

        int i = Integer.MAX_VALUE;

        for (Handler option : handlerList) {
            Parameter[] parameters = option.method.getParameters();

            if (parameters.length >= n) {
                int j = 0;

                for (int k = 0; k < parameters.length; k++) {
                    String name = getName(parameters[k]);

                    if (!(parameterMap.containsKey(name))) {
                        j++;
                    }
                }

                if (parameters.length - j == n && j < i) {
                    handler = option;

                    i = j;
                }
            }
        }

        return handler;
    }

    private static Object[] getArguments(Method method, Map<String, List<?>> parameterMap) {
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
                        list.add(BeanAdapter.adapt(value, elementType));
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

                argument = BeanAdapter.adapt(value, type);
            }

            arguments[i] = argument;
        }

        return arguments;
    }

    private static String getName(Parameter parameter) {
        RequestParameter requestParameter = parameter.getAnnotation(RequestParameter.class);

        return (requestParameter == null) ? parameter.getName() : requestParameter.value();
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

    /**
     * Determines if the current request is authorized.
     *
     * @param request
     * The servlet request.
     *
     * @param method
     * The method to be invoked.
     *
     * @return
     * <tt>true</tt> if the method should be invoked; <tt>false</tt>,
     * otherwise.
     */
    protected boolean isAuthorized(HttpServletRequest request, Method method) {
        return true;
    }

    /**
     * Encodes the result of a service operation.
     *
     * @param request
     * The servlet request.
     *
     * @param response
     * The servlet response.
     *
     * @param result
     * The method result.
     *
     * @throws IOException
     * If an exception occurs while encoding the result.
     */
    protected void encodeResult(HttpServletRequest request, HttpServletResponse response, Object result) throws IOException {
        response.setContentType(String.format("application/json;charset=%s", UTF_8));

        JSONEncoder jsonEncoder = new JSONEncoder(isCompact());

        jsonEncoder.write(BeanAdapter.adapt(result), response.getOutputStream());
    }

    /**
     * Enables or disables compact output.
     *
     * @return
     * <tt>true</tt> if the encoded output should be compact; <tt>false</tt>,
     * otherwise.
     */
    protected boolean isCompact() {
        return false;
    }

    private void describeService(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType(String.format("text/html;charset=%s", UTF_8));

        XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();

        try {
            XMLStreamWriter xmlStreamWriter = xmlOutputFactory.createXMLStreamWriter(response.getWriter());

            xmlStreamWriter.writeStartElement("html");
            xmlStreamWriter.writeStartElement("head");
            xmlStreamWriter.writeStartElement("style");

            try (InputStream inputStream = WebService.class.getResourceAsStream("api.css")) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    xmlStreamWriter.writeCharacters(line + "\n");
                }
            }

            xmlStreamWriter.writeEndElement();
            xmlStreamWriter.writeEndElement();

            xmlStreamWriter.writeStartElement("body");

            TreeMap<Class<?>, String> structures = new TreeMap<>(Comparator.comparing(Class::getSimpleName));

            Class<?> serviceType = getClass();

            ResourceBundle resourceBundle;
            try {
                resourceBundle = ResourceBundle.getBundle(serviceType.getCanonicalName(), request.getLocale());
            } catch (MissingResourceException exception) {
                resourceBundle = null;
            }

            if (resourceBundle != null) {
                String description;
                try {
                    description = resourceBundle.getString(serviceType.getSimpleName());
                } catch (MissingResourceException exception) {
                    description = null;
                }

                if (description != null) {
                    xmlStreamWriter.writeStartElement("p");
                    xmlStreamWriter.writeCharacters(description);
                    xmlStreamWriter.writeEndElement();
                }
            }

            describeResource(request.getServletPath(), root, structures, xmlStreamWriter, resourceBundle);

            for (Map.Entry<Class<?>, String> entry : structures.entrySet()) {
                Class<?> type = entry.getKey();

                String name = type.getSimpleName();

                xmlStreamWriter.writeStartElement("h3");
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

    private void describeResource(String path, Resource resource, TreeMap<Class<?>, String> structures,
        XMLStreamWriter xmlStreamWriter, ResourceBundle resourceBundle) throws XMLStreamException {
        if (!resource.handlerMap.isEmpty()) {
            xmlStreamWriter.writeStartElement("h2");
            xmlStreamWriter.writeCharacters(path);
            xmlStreamWriter.writeEndElement();

            for (Map.Entry<String, LinkedList<Handler>> entry : resource.handlerMap.entrySet()) {
                for (Handler handler : entry.getValue()) {
                    Parameter[] parameters = handler.method.getParameters();

                    xmlStreamWriter.writeStartElement("pre");

                    String verb = entry.getKey().toUpperCase();

                    if (handler.method.getAnnotation(Deprecated.class) == null) {
                        xmlStreamWriter.writeCharacters(verb);
                    } else {
                        xmlStreamWriter.writeStartElement("del");
                        xmlStreamWriter.writeCharacters(verb);
                        xmlStreamWriter.writeEndElement();
                    }

                    xmlStreamWriter.writeCharacters(" (");

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

                    Type type = handler.method.getGenericReturnType();
                    Response response = handler.method.getAnnotation(Response.class);

                    if ((type == Void.class || type == Void.TYPE) && response != null) {
                        xmlStreamWriter.writeCharacters(response.value());
                    } else {
                        xmlStreamWriter.writeCharacters(BeanAdapter.describe(type, structures));
                    }

                    xmlStreamWriter.writeEndElement();

                    if (resourceBundle != null) {
                        String methodName = handler.method.getName();

                        String methodDescription;
                        try {
                            methodDescription = resourceBundle.getString(methodName);
                        } catch (MissingResourceException exception) {
                            methodDescription = null;
                        }

                        if (methodDescription != null) {
                            xmlStreamWriter.writeStartElement("p");
                            xmlStreamWriter.writeCharacters(methodDescription);

                            if (parameters.length > 0) {
                                xmlStreamWriter.writeStartElement("ul");

                                for (int i = 0; i < parameters.length; i++) {
                                    Parameter parameter = parameters[i];

                                    String parameterName = getName(parameter);

                                    xmlStreamWriter.writeStartElement("li");

                                    xmlStreamWriter.writeStartElement("strong");
                                    xmlStreamWriter.writeCharacters(parameterName);
                                    xmlStreamWriter.writeEndElement();

                                    String parameterDescription;
                                    try {
                                        parameterDescription = resourceBundle.getString(methodName + "." + parameterName);
                                    } catch (MissingResourceException exception) {
                                        parameterDescription = null;
                                    }

                                    if (parameterDescription != null) {
                                        xmlStreamWriter.writeEntityRef("nbsp");
                                        xmlStreamWriter.writeCharacters(parameterDescription);
                                    }

                                    xmlStreamWriter.writeEndElement();
                                }

                                xmlStreamWriter.writeEndElement();
                            }

                            xmlStreamWriter.writeEndElement();
                        }
                    }
                }
            }
        }

        for (Map.Entry<String, Resource> entry : resource.resources.entrySet()) {
            describeResource(path + "/" + entry.getKey(), entry.getValue(), structures, xmlStreamWriter, resourceBundle);
        }
    }
}
