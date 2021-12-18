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

import org.httprpc.beans.BeanAdapter;
import org.httprpc.io.JSONDecoder;
import org.httprpc.io.JSONEncoder;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.emptyList;
import static org.httprpc.util.Collections.listOf;

/**
 * Abstract base class for web services.
 */
public abstract class WebService extends HttpServlet {
    private static class Resource {
        static List<String> order = listOf("get", "post", "put", "delete");

        final Map<String, List<Handler>> handlerMap = new TreeMap<>((verb1, verb2) -> {
            int i1 = order.indexOf(verb1);
            int i2 = order.indexOf(verb2);

            return Integer.compare((i1 == -1) ? order.size() : i1, (i2 == -1) ? order.size() : i2);
        });

        final Map<String, Resource> resources = new TreeMap<>();
    }

    private static class Handler {
        final Method method;

        final List<String> keys = new ArrayList<>();

        Handler(Method method) {
            this.method = method;
        }
    }

    private static class PartURLConnection extends URLConnection {
        Part part;

        PartURLConnection(URL url, Part part) {
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
        Part part;

        PartURLStreamHandler(Part part) {
            this.part = part;
        }

        @Override
        protected URLConnection openConnection(URL url) {
            return new PartURLConnection(url, part);
        }
    }

    private Resource root = null;

    private Set<Class<?>> enumerations = new TreeSet<>(Comparator.comparing(Class::getSimpleName));
    private Map<Class<?>, Map<String, BeanAdapter.Property>> dataTypes = new TreeMap<>(Comparator.comparing(Class::getSimpleName));

    private ThreadLocal<HttpServletRequest> request = new ThreadLocal<>();
    private ThreadLocal<HttpServletResponse> response = new ThreadLocal<>();

    private ThreadLocal<List<String>> keyList = new ThreadLocal<>();
    private ThreadLocal<Map<String, String>> keyMap = new ThreadLocal<>();

    private ThreadLocal<Object> body = new ThreadLocal<>();

    private static ConcurrentHashMap<Class<?>, WebService> services = new ConcurrentHashMap<>();

    private static final String UTF_8 = "UTF-8";

    /**
     * Returns a service instance.
     *
     * @param type
     * The service type.
     *
     * @param <T>
     * The service type.
     *
     * @return
     * The service instance, or <code>null</code> if no service of the given type
     * exists.
     */
    @SuppressWarnings("unchecked")
    public static <T extends WebService> T getInstance(Class<T> type) {
        return (T)services.get(type);
    }

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

                        if (component.startsWith(ResourcePath.PATH_VARIABLE_PREFIX)) {
                            int k = ResourcePath.PATH_VARIABLE_PREFIX.length();

                            String key;
                            if (component.length() > k) {
                                if (component.charAt(k++) != ':') {
                                    throw new ServletException("Invalid path variable.");
                                }

                                key = component.substring(k);

                                component = ResourcePath.PATH_VARIABLE_PREFIX;
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

                List<Handler> handlerList = resource.handlerMap.get(verb);

                if (handlerList == null) {
                    handlerList = new LinkedList<>();

                    resource.handlerMap.put(verb, handlerList);
                }

                handlerList.add(handler);
            }
        }

        sort(root);

        Class<? extends WebService> type = getClass();

        if (getClass().getAnnotation(WebServlet.class) != null) {
            services.put(type, this);
        }
    }

    private static void sort(Resource root) {
        for (List<Handler> handlers : root.handlerMap.values()) {
            handlers.sort(Comparator.comparing(handler -> handler.method.getName()));
        }

        for (Resource resource : root.resources.values()) {
            sort(resource);
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

        List<String> keyList = new ArrayList<>();

        if (pathInfo != null) {
            String[] components = pathInfo.split("/");

            for (int i = 0; i < components.length; i++) {
                String component = components[i];

                if (component.length() == 0) {
                    continue;
                }

                Resource child = resource.resources.get(component);

                if (child == null) {
                    child = resource.resources.get(ResourcePath.PATH_VARIABLE_PREFIX);

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

                List<URL> values = (List<URL>)parameterMap.get(name);

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

        Map<String, String> keyMap = new HashMap<>();

        for (int i = 0, n = keyList.size(); i < n; i++) {
            String key = handler.keys.get(i);

            if (key != null) {
                keyMap.put(key, keyList.get(i));
            }
        }

        Content content = handler.method.getAnnotation(Content.class);

        Object body;
        if (content != null) {
            try {
                body = decodeBody(request, content.value());
            } catch (Exception exception) {
                response.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
                return;
            }
        } else {
            body = null;
        }

        this.request.set(request);
        this.response.set(response);

        this.keyList.set(keyList);
        this.keyMap.set(keyMap);

        this.body.set(body);

        Object result;
        try {
            result = handler.method.invoke(this, getArguments(handler.method, parameterMap));
        } catch (IllegalAccessException | InvocationTargetException exception) {
            if (response.isCommitted()) {
                throw new ServletException(exception);
            }

            Throwable cause = exception.getCause();

            if (cause == null) {
                throw new ServletException(exception);
            }

            int status;
            if (cause instanceof IllegalArgumentException || cause instanceof UnsupportedOperationException) {
                status = HttpServletResponse.SC_FORBIDDEN;
            } else if (cause instanceof NoSuchElementException) {
                status = HttpServletResponse.SC_NOT_FOUND;
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
        } finally {
            this.request.set(null);
            this.response.set(null);

            this.keyList.set(null);
            this.keyMap.set(null);

            this.body.set(null);
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
            encodeResult(response, result);
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
                    String name = parameters[k].getName();

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

            List<?> values = parameterMap.get(parameter.getName());

            Class<?> type = parameter.getType();

            Object argument;
            if (type == List.class) {
                Type elementType = ((ParameterizedType)parameter.getParameterizedType()).getActualTypeArguments()[0];

                List<Object> list;
                if (values != null) {
                    list = new ArrayList<>(values.size());

                    for (Object value : values) {
                        list.add(BeanAdapter.adapt(value, elementType));
                    }
                } else {
                    list = emptyList();
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
        return getKey(index, String.class);
    }

    /**
     * Returns the value of a key in the request path.
     *
     * @param <T>
     * The key type.
     *
     * @param index
     * The index of the key to return.
     *
     * @param type
     * The key type.
     *
     * @return
     * The key value.
     */
    protected <T> T getKey(int index, Class<T> type) {
        return BeanAdapter.adapt(keyList.get().get(index), type);
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
        return getKey(name, String.class);
    }

    /**
     * Returns the value of a key in the request path.
     *
     * @param <T>
     * The key type.
     *
     * @param name
     * The name of the key to return.
     *
     * @param type
     * The key type.
     *
     * @return
     * The key value.
     */
    protected <T> T getKey(String name, Class<T> type) {
        return BeanAdapter.adapt(keyMap.get().get(name), type);
    }

    /**
     * Returns the decoded body content associated with the current request.
     *
     * @param <T>
     * The body type.
     *
     * @return
     * The decoded body content, or <code>null</code> if no content was provided.
     */
    @SuppressWarnings("unchecked")
    protected <T> T getBody() {
        return (T)body.get();
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
     * <code>true</code> if the method should be invoked; <code>false</code>,
     * otherwise.
     */
    protected boolean isAuthorized(HttpServletRequest request, Method method) {
        return true;
    }

    /**
     * Decodes the body of a service request.
     *
     * @param request
     * The servlet request.
     *
     * @param type
     * The content type.
     *
     * @return
     * The decoded body content.
     *
     * @throws IOException
     * If an exception occurs while decoding the content.
     */
    protected Object decodeBody(HttpServletRequest request, Class<?> type) throws IOException {
        String contentType = request.getContentType();

        if (contentType != null && !contentType.startsWith("application/json")) {
            throw new UnsupportedOperationException();
        }

        JSONDecoder jsonDecoder = new JSONDecoder();

        return BeanAdapter.adapt(jsonDecoder.read(request.getInputStream()), type);
    }

    /**
     * Encodes the result of a service operation.
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
    protected void encodeResult(HttpServletResponse response, Object result) throws IOException {
        response.setContentType(String.format("application/json;charset=%s", UTF_8));

        JSONEncoder jsonEncoder = new JSONEncoder(isCompact());

        jsonEncoder.write(BeanAdapter.adapt(result), response.getOutputStream());
    }

    /**
     * Enables or disables compact output.
     *
     * @return
     * <code>true</code> if the encoded output should be compact; <code>false</code>,
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

            Description serviceDescription = getClass().getAnnotation(Description.class);

            if (serviceDescription != null) {
                xmlStreamWriter.writeStartElement("p");
                xmlStreamWriter.writeCharacters(serviceDescription.value());
                xmlStreamWriter.writeEndElement();
            }

            describeResource(request.getServletPath(), root, xmlStreamWriter);

            if (enumerations.size() > 0) {
                xmlStreamWriter.writeEmptyElement("hr");
            }

            for (Class<?> type : enumerations) {
                String name = type.getSimpleName();

                xmlStreamWriter.writeStartElement("h3");

                xmlStreamWriter.writeStartElement("a");
                xmlStreamWriter.writeAttribute("id", name);
                xmlStreamWriter.writeCharacters(name);
                xmlStreamWriter.writeEndElement();

                xmlStreamWriter.writeEndElement();

                Description typeDescription = type.getAnnotation(Description.class);

                if (typeDescription != null) {
                    xmlStreamWriter.writeStartElement("p");
                    xmlStreamWriter.writeCharacters(typeDescription.value());
                    xmlStreamWriter.writeEndElement();
                }

                xmlStreamWriter.writeStartElement("ul");

                Field[] fields = type.getDeclaredFields();

                for (int i = 0; i < fields.length; i++) {
                    Field field = fields[i];

                    if (!field.isEnumConstant()) {
                        continue;
                    }

                    Object constant;
                    try {
                        constant = field.get(null);
                    } catch (IllegalAccessException exception) {
                        throw new RuntimeException(exception);
                    }

                    xmlStreamWriter.writeStartElement("li");

                    xmlStreamWriter.writeStartElement("code");
                    xmlStreamWriter.writeCharacters(constant.toString());
                    xmlStreamWriter.writeEndElement();

                    Description constantDescription = field.getAnnotation(Description.class);

                    if (constantDescription != null) {
                        xmlStreamWriter.writeCharacters(" - ");
                        xmlStreamWriter.writeCharacters(constantDescription.value());
                    }

                    xmlStreamWriter.writeEndElement();
                }

                xmlStreamWriter.writeEndElement();
            }

            if (dataTypes.size() > 0) {
                xmlStreamWriter.writeEmptyElement("hr");
            }

            for (Map.Entry<Class<?>, Map<String, BeanAdapter.Property>> typeEntry : dataTypes.entrySet()) {
                Class<?> type = typeEntry.getKey();

                String name = type.getSimpleName();

                xmlStreamWriter.writeStartElement("h3");

                xmlStreamWriter.writeStartElement("a");
                xmlStreamWriter.writeAttribute("id", name);
                xmlStreamWriter.writeCharacters(name);
                xmlStreamWriter.writeEndElement();

                if (type.isInterface()) {
                    Class<?>[] interfaces = type.getInterfaces();

                    if (interfaces.length > 0) {
                        xmlStreamWriter.writeCharacters(" : ");

                        for (int i = 0; i < interfaces.length; i++) {
                            if (i > 0) {
                                xmlStreamWriter.writeCharacters(", ");
                            }

                            String interfaceName = interfaces[i].getSimpleName();

                            xmlStreamWriter.writeStartElement("a");
                            xmlStreamWriter.writeAttribute("href", "#" + interfaceName);
                            xmlStreamWriter.writeCharacters(interfaceName);
                            xmlStreamWriter.writeEndElement();
                        }
                    }
                } else {
                    Class<?> baseType = type.getSuperclass();

                    if (baseType != Object.class) {
                        String baseTypeName = baseType.getSimpleName();

                        xmlStreamWriter.writeCharacters(" : ");

                        xmlStreamWriter.writeStartElement("a");
                        xmlStreamWriter.writeAttribute("href", "#" + baseTypeName);
                        xmlStreamWriter.writeCharacters(baseTypeName);
                        xmlStreamWriter.writeEndElement();
                    }
                }

                xmlStreamWriter.writeEndElement();

                Description typeDescription = type.getAnnotation(Description.class);

                if (typeDescription != null) {
                    xmlStreamWriter.writeStartElement("p");
                    xmlStreamWriter.writeCharacters(typeDescription.value());
                    xmlStreamWriter.writeEndElement();
                }

                xmlStreamWriter.writeStartElement("ul");

                for (Map.Entry<String, BeanAdapter.Property> propertyEntry : typeEntry.getValue().entrySet()) {
                    Method accessor = propertyEntry.getValue().getAccessor();

                    if (accessor == null || accessor.getDeclaringClass() != type) {
                        continue;
                    }

                    xmlStreamWriter.writeStartElement("li");

                    xmlStreamWriter.writeStartElement("code");

                    xmlStreamWriter.writeCharacters(propertyEntry.getKey());
                    xmlStreamWriter.writeCharacters(": ");

                    describeType(accessor.getGenericReturnType(), xmlStreamWriter);

                    xmlStreamWriter.writeEndElement();

                    Description propertyDescription = accessor.getAnnotation(Description.class);

                    if (propertyDescription != null) {
                        xmlStreamWriter.writeCharacters(" - ");
                        xmlStreamWriter.writeCharacters(propertyDescription.value());
                    }

                    xmlStreamWriter.writeEndElement();
                }

                xmlStreamWriter.writeEndElement();
            }

            xmlStreamWriter.writeEndElement();
            xmlStreamWriter.writeEndElement();

            xmlStreamWriter.close();
        } catch (XMLStreamException exception) {
            throw new IOException(exception);
        }
    }

    private void describeResource(String path, Resource resource, XMLStreamWriter xmlStreamWriter) throws XMLStreamException {
        if (!resource.handlerMap.isEmpty()) {
            xmlStreamWriter.writeStartElement("h2");
            xmlStreamWriter.writeCharacters(path);
            xmlStreamWriter.writeEndElement();

            for (Map.Entry<String, List<Handler>> entry : resource.handlerMap.entrySet()) {
                for (Handler handler : entry.getValue()) {
                    xmlStreamWriter.writeStartElement("pre");

                    Deprecated deprecated = handler.method.getAnnotation(Deprecated.class);

                    if (deprecated != null) {
                        xmlStreamWriter.writeStartElement("del");
                    }

                    xmlStreamWriter.writeCharacters(entry.getKey().toUpperCase());

                    Content content = handler.method.getAnnotation(Content.class);

                    if (content != null) {
                        xmlStreamWriter.writeCharacters(" (");

                        describeType(content.value(), xmlStreamWriter);

                        xmlStreamWriter.writeCharacters(")");
                    }

                    xmlStreamWriter.writeCharacters(" -> ");

                    describeType(handler.method.getGenericReturnType(), xmlStreamWriter);

                    if (deprecated != null) {
                        xmlStreamWriter.writeEndElement();
                    }

                    xmlStreamWriter.writeEndElement();

                    Description methodDescription = handler.method.getAnnotation(Description.class);

                    if (methodDescription != null) {
                        xmlStreamWriter.writeStartElement("p");
                        xmlStreamWriter.writeCharacters(methodDescription.value());
                        xmlStreamWriter.writeEndElement();
                    }

                    Parameter[] parameters = handler.method.getParameters();

                    xmlStreamWriter.writeStartElement("ul");

                    for (int i = 0; i < parameters.length; i++) {
                        Parameter parameter = parameters[i];

                        xmlStreamWriter.writeStartElement("li");

                        xmlStreamWriter.writeStartElement("code");
                        xmlStreamWriter.writeCharacters(parameter.getName());
                        xmlStreamWriter.writeCharacters(": ");

                        describeType(parameter.getParameterizedType(), xmlStreamWriter);

                        xmlStreamWriter.writeEndElement();

                        Description parameterDescription = parameter.getAnnotation(Description.class);

                        if (parameterDescription != null) {
                            xmlStreamWriter.writeCharacters(" - ");
                            xmlStreamWriter.writeCharacters(parameterDescription.value());
                        }

                        xmlStreamWriter.writeEndElement();
                    }

                    xmlStreamWriter.writeEndElement();
                }
            }
        }

        for (Map.Entry<String, Resource> entry : resource.resources.entrySet()) {
            describeResource(path + "/" + entry.getKey(), entry.getValue(), xmlStreamWriter);
        }
    }

    private void describeType(Type type, XMLStreamWriter xmlStreamWriter) throws XMLStreamException {
        if (type instanceof Class<?>) {
            describeType((Class<?>)type, xmlStreamWriter);
        } else if (type instanceof WildcardType) {
            describeType(((WildcardType)type).getUpperBounds()[0], xmlStreamWriter);
        } else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType)type;

            Type rawType = parameterizedType.getRawType();

            if (rawType instanceof Class<?> && Iterable.class.isAssignableFrom((Class<?>)rawType)) {
                if (xmlStreamWriter != null) {
                    xmlStreamWriter.writeCharacters("[");
                }

                describeType(parameterizedType.getActualTypeArguments()[0], xmlStreamWriter);

                if (xmlStreamWriter != null) {
                    xmlStreamWriter.writeCharacters("]");
                }
            } else if (rawType == Map.class) {
                Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();

                if (xmlStreamWriter != null) {
                    xmlStreamWriter.writeCharacters("[");
                }

                describeType(actualTypeArguments[0], xmlStreamWriter);

                if (xmlStreamWriter != null) {
                    xmlStreamWriter.writeCharacters(": ");
                }

                describeType(actualTypeArguments[1], xmlStreamWriter);

                if (xmlStreamWriter != null) {
                    xmlStreamWriter.writeCharacters("]");
                }
            } else {
                throw new IllegalArgumentException();
            }
        } else {
            throw new IllegalArgumentException();
        }
    }

    private void describeType(Class<?> type, XMLStreamWriter xmlStreamWriter) throws XMLStreamException {
        if (type.isArray()) {
            throw new IllegalArgumentException();
        }

        String description;
        if (type == Object.class) {
            description = "any";
        } else if (type == Void.TYPE || type == Void.class) {
            description = "void";
        } else if (type == Byte.TYPE || type == Byte.class) {
            description = "byte";
        } else if (type == Short.TYPE || type == Short.class) {
            description = "short";
        } else if (type == Integer.TYPE || type == Integer.class) {
            description = "integer";
        } else if (type == Long.TYPE || type == Long.class) {
            description = "long";
        } else if (type == Float.TYPE || type == Float.class) {
            description = "float";
        } else if (type == Double.TYPE || type == Double.class) {
            description = "double";
        } else if (Number.class.isAssignableFrom(type)) {
            description = "number";
        } else if (type == Boolean.TYPE || type == Boolean.class) {
            description = "boolean";
        } else if (CharSequence.class.isAssignableFrom(type)) {
            description = "string";
        } else if (Enum.class.isAssignableFrom(type)) {
            enumerations.add(type);

            if (xmlStreamWriter != null) {
                String name = type.getSimpleName();

                xmlStreamWriter.writeStartElement("a");
                xmlStreamWriter.writeAttribute("href", "#" + name);
                xmlStreamWriter.writeCharacters(name);
                xmlStreamWriter.writeEndElement();
            }

            return;
        } else if (Date.class.isAssignableFrom(type)) {
            description = "date";
        } else if (type == Instant.class) {
            description = "instant";
        } else if (type == LocalDate.class) {
            description = "date-local";
        } else if (type == LocalTime.class) {
            description = "time-local";
        } else if (type == LocalDateTime.class) {
            description = "datetime-local";
        } else if (type == Duration.class) {
            description = "duration";
        } else if (type == Period.class) {
            description = "period";
        } else if (type == UUID.class) {
            description = "uuid";
        } else if (type == URL.class) {
            description = "url";
        } else {
            if (Iterable.class.isAssignableFrom(type)) {
                describeType(BeanAdapter.typeOf(Iterable.class, Object.class), xmlStreamWriter);
            } else if (Map.class.isAssignableFrom(type)) {
                describeType(BeanAdapter.typeOf(Map.class, Object.class, Object.class), xmlStreamWriter);
            } else {
                if (!dataTypes.containsKey(type)) {
                    Map<String, BeanAdapter.Property> properties = BeanAdapter.getProperties(type);

                    dataTypes.put(type, properties);

                    if (type.isInterface()) {
                        Class<?>[] interfaces = type.getInterfaces();

                        for (int i = 0; i < interfaces.length; i++) {
                            describeType(interfaces[i], null);
                        }
                    } else {
                        Class<?> baseType = type.getSuperclass();

                        if (baseType != Object.class) {
                            describeType(baseType, null);
                        }
                    }

                    for (Map.Entry<String, BeanAdapter.Property> entry : properties.entrySet()) {
                        Method accessor = entry.getValue().getAccessor();

                        if (accessor == null || accessor.getDeclaringClass() != type) {
                            continue;
                        }

                        describeType(accessor.getGenericReturnType(), null);
                    }
                }

                if (xmlStreamWriter != null) {
                    String name = type.getSimpleName();

                    xmlStreamWriter.writeStartElement("a");
                    xmlStreamWriter.writeAttribute("href", "#" + name);
                    xmlStreamWriter.writeCharacters(name);
                    xmlStreamWriter.writeEndElement();
                }
            }

            return;
        }

        if (xmlStreamWriter != null) {
            xmlStreamWriter.writeCharacters(description);
        }
    }
}
