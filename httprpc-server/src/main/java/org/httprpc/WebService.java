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

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import org.httprpc.io.TemplateEncoder;
import org.httprpc.util.ResourceBundleAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
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
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static org.httprpc.util.Collections.entry;
import static org.httprpc.util.Collections.listOf;
import static org.httprpc.util.Collections.mapOf;

/**
 * Abstract base class for web services.
 */
public abstract class WebService extends HttpServlet {
    /**
     * Describes a service instance.
     */
    public static class ServiceDescriptor {
        private String description;

        private List<EndpointDescriptor> endpoints = new LinkedList<>();

        private Map<Class<?>, EnumerationDescriptor> enumerations = new TreeMap<>(Comparator.comparing(Class::getSimpleName));
        private Map<Class<?>, StructureDescriptor> structures = new TreeMap<>(Comparator.comparing(Class::getSimpleName));

        private ServiceDescriptor(Class<? extends WebService> type) {
            description = Optional.ofNullable(type.getAnnotation(Description.class)).map(Description::value).orElse(null);
        }

        /**
         * Returns a description of the service.
         *
         * @return
         * The service description, or <code>null</code> for no description.
         */
        public String getDescription() {
            return description;
        }

        /**
         * Returns the endpoints provided by the service.
         *
         * @return
         * The service endpoints.
         */
        public Iterable<EndpointDescriptor> getEndpoints() {
            return endpoints;
        }

        /**
         * Returns the enumerations defined by the service.
         *
         * @return
         * The service enumerations.
         */
        public Iterable<EnumerationDescriptor> getEnumerations() {
            return enumerations.values();
        }

        /**
         * Returns the structures defined by the service.
         *
         * @return
         * The service structures.
         */
        public Iterable<StructureDescriptor> getStructures() {
            return structures.values();
        }
    }

    /**
     * Describes a service endpoint.
     */
    public static class EndpointDescriptor {
        private String path;
        private String description;
        private Iterable<String> keys;

        private List<OperationDescriptor> operations = new LinkedList<>();

        private EndpointDescriptor(String path, Endpoint endpoint) {
            this.path = path;

            if (endpoint != null) {
                description = endpoint.description();
                keys = Arrays.asList(endpoint.keys());
            } else {
                description = null;
                keys = null;
            }
        }

        /**
         * Returns the endpoint's path.
         *
         * @return
         * The endpoint's path.
         */
        public String getPath() {
            return path;
        }

        /**
         * Returns a description of the endpoint.
         *
         * @return
         * The endpoint's description, or <code>null</code> for no description.
         */
        public String getDescription() {
            return description;
        }

        /**
         * Returns the keys defined by the endpoint.
         *
         * @return
         * The endpoint's keys.
         */
        public Iterable<String> getKeys() {
            return keys;
        }

        /**
         * Returns the operations provided by the endpoint.
         *
         * @return
         * The endpoint's operations.
         */
        public Iterable<OperationDescriptor> getOperations() {
            return operations;
        }
    }

    /**
     * Describes a service operation.
     */
    public static class OperationDescriptor {
        private String method;
        private String description;

        private boolean deprecated;

        private TypeDescriptor consumes = null;
        private TypeDescriptor produces = null;

        private List<VariableDescriptor> parameters = new LinkedList<>();

        private OperationDescriptor(String method, Handler handler) {
            this.method = method;

            description = Optional.ofNullable(handler.method.getAnnotation(Description.class)).map(Description::value).orElse(null);

            deprecated = handler.method.getAnnotation(Deprecated.class) != null;
        }

        /**
         * Returns the HTTP method used to invoke the operation.
         *
         * @return
         * The HTTP method used to invoke the operation.
         */
        public String getMethod() {
            return method;
        }

        /**
         * Returns a description of the operation.
         *
         * @return
         * The operation's description, or <code>null</code> for no description.
         */
        public String getDescription() {
            return description;
        }

        /**
         * Indicates that the operation is deprecated.
         *
         * @return
         * <code>true</code> if the operation is deprecated; <code>false</code>,
         * otherwise.
         */
        public boolean isDeprecated() {
            return deprecated;
        }

        /**
         * Returns the type of content consumed by the operation.
         *
         * @return
         * The type of content consumed by the operation, or <code>null</code>
         * if the operation does not accept request content.
         */
        public TypeDescriptor getConsumes() {
            return consumes;
        }

        /**
         * Returns the type of content produced by the operation.
         *
         * @return
         * The type of content produced by the operation, or <code>null</code>
         * if the operation does not return response content.
         */
        public TypeDescriptor getProduces() {
            return produces;
        }

        /**
         * Returns the parameters defined by the operation.
         *
         * @return
         * The operation's parameters.
         */
        public Iterable<VariableDescriptor> getParameters() {
            return parameters;
        }
    }

    /**
     * Describes a variable.
     */
    public static class VariableDescriptor {
        private String name;
        private String description;

        private TypeDescriptor type = null;

        private VariableDescriptor(Parameter parameter) {
            name = parameter.getName();

            description = Optional.ofNullable(parameter.getAnnotation(Description.class)).map(Description::value).orElse(null);
        }

        private VariableDescriptor(String name, Method accessor) {
            this.name = name;

            description = Optional.ofNullable(accessor.getAnnotation(Description.class)).map(Description::value).orElse(null);
        }

        /**
         * Returns the name of the variable.
         *
         * @return
         * The variable's name.
         */
        public String getName() {
            return name;
        }

        /**
         * Returns a description of the variable.
         *
         * @return
         * The variable's description, or <code>null</code> for no description.
         */
        public String getDescription() {
            return description;
        }

        /**
         * Returns the type of the variable.
         *
         * @return
         * The variable's type.
         */
        public TypeDescriptor getType() {
            return type;
        }
    }

    /**
     * Describes an enumeration.
     */
    public static class EnumerationDescriptor {
        private String name;
        private String description;

        private List<ConstantDescriptor> values = new LinkedList<>();

        private EnumerationDescriptor(Class<?> type) {
            name = type.getSimpleName();

            description = Optional.ofNullable(type.getAnnotation(Description.class)).map(Description::value).orElse(null);
        }

        /**
         * Returns the name of the enumeration.
         *
         * @return
         * The enumeration's name.
         */
        public String getName() {
            return name;
        }

        /**
         * Returns a description of the enumeration.
         *
         * @return
         * The enumeration's description, or <code>null</code> for no description.
         */
        public String getDescription() {
            return description;
        }

        /**
         * Returns the values defined by the enumeration.
         *
         * @return
         * The enumeration's values.
         */
        public Iterable<ConstantDescriptor> getValues() {
            return values;
        }
    }

    /**
     * Describes a constant.
     */
    public static class ConstantDescriptor {
        private String name;
        private String description;

        private ConstantDescriptor(Field field) {
            Object constant;
            try {
                constant = field.get(null);
            } catch (IllegalAccessException exception) {
                throw new RuntimeException(exception);
            }

            name = constant.toString();

            description = Optional.ofNullable(field.getAnnotation(Description.class)).map(Description::value).orElse(null);
        }

        /**
         * Returns the name of the constant.
         *
         * @return
         * The constant's name.
         */
        public String getName() {
            return name;
        }

        /**
         * Returns a description of the constant.
         *
         * @return
         * The constant's description, or <code>null</code> for no description.
         */
        public String getDescription() {
            return description;
        }
    }

    /**
     * Describes a structure.
     */
    public static class StructureDescriptor {
        private String name;
        private String description;

        private List<TypeDescriptor> supertypes = new LinkedList<>();
        private List<VariableDescriptor> properties = new LinkedList<>();

        private StructureDescriptor(Class<?> type) {
            name = type.getSimpleName();

            description = Optional.ofNullable(type.getAnnotation(Description.class)).map(Description::value).orElse(null);
        }

        /**
         * Returns the name of the structure.
         *
         * @return
         * The structure's name.
         */
        public String getName() {
            return name;
        }

        /**
         * Returns a description of the structure.
         *
         * @return
         * The structure's description, or <code>null</code> for no description.
         */
        public String getDescription() {
            return description;
        }

        /**
         * Returns the structure's supertypes.
         *
         * @return
         * The structure's supertypes.
         */
        public Iterable<TypeDescriptor> getSupertypes() {
            return supertypes;
        }

        /**
         * Returns the properties defined by the structure.
         *
         * @return
         * The structure's properties.
         */
        public Iterable<VariableDescriptor> getProperties() {
            return properties;
        }
    }

    /**
     * Describes a type.
     */
    public static class TypeDescriptor {
        private Class<?> type;
        private boolean intrinsic;

        private TypeDescriptor(Class<?> type, boolean intrinsic) {
            this.type = type;
            this.intrinsic = intrinsic;
        }

        /**
         * Returns the name of the type represented by the descriptor.
         *
         * @return
         * The type name.
         */
        public String getName() {
            if (type.isPrimitive()) {
                return type.getName();
            } else {
                return type.getSimpleName();
            }
        }

        /**
         * Indicates that the type is intrinsic.
         *
         * @return
         * <code>true</code> if the type is intrinsic; <code>false</code>,
         * otherwise.
         */
        public boolean isIntrinsic() {
            return intrinsic;
        }

        /**
         * Indicates that the type is an iterable.
         *
         * @return
         * <code>true</code> if the type is an iterable; <code>false</code>,
         * otherwise.
         */
        public boolean isIterable() {
            return false;
        }

        /**
         * Returns the element type.
         *
         * @return
         * The element type, or <code>null</code> if the type is not an
         * iterable.
         */
        public TypeDescriptor getElementType() {
            return null;
        }

        /**
         * Indicates that the type is a map.
         *
         * @return
         * <code>true</code> if the type is a map; <code>false</code>, otherwise.
         */
        public boolean isMap() {
            return false;
        }

        /**
         * Returns the key type.
         *
         * @return
         * The key type, or <code>null</code> if the type is not a map.
         */
        public TypeDescriptor getKeyType() {
            return null;
        }

        /**
         * Returns the value type.
         *
         * @return
         * The value type, or <code>null</code> if the type is not a map.
         */
        public TypeDescriptor getValueType() {
            return null;
        }
    }

    /**
     * Describes an iterable type.
     */
    public static class IterableTypeDescriptor extends TypeDescriptor {
        private TypeDescriptor elementType;

        private IterableTypeDescriptor(TypeDescriptor elementType) {
            super(Iterable.class, true);

            this.elementType = elementType;
        }

        @Override
        public boolean isIterable() {
            return true;
        }

        @Override
        public TypeDescriptor getElementType() {
            return elementType;
        }
    }

    /**
     * Describes a map type.
     */
    public static class MapTypeDescriptor extends TypeDescriptor {
        private TypeDescriptor keyType;
        private TypeDescriptor valueType;

        private MapTypeDescriptor(TypeDescriptor keyType, TypeDescriptor valueType) {
            super(Map.class, true);

            this.keyType = keyType;
            this.valueType = valueType;
        }

        @Override
        public boolean isMap() {
            return true;
        }

        @Override
        public TypeDescriptor getKeyType() {
            return keyType;
        }

        @Override
        public TypeDescriptor getValueType() {
            return valueType;
        }
    }

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

    private ThreadLocal<HttpServletRequest> request = new ThreadLocal<>();
    private ThreadLocal<HttpServletResponse> response = new ThreadLocal<>();

    private ThreadLocal<List<String>> keyList = new ThreadLocal<>();
    private ThreadLocal<Map<String, String>> keyMap = new ThreadLocal<>();

    private ThreadLocal<Object> body = new ThreadLocal<>();

    private ServiceDescriptor serviceDescriptor = null;

    private static final Map<Class<?>, WebService> instances = new HashMap<>();

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
    public static synchronized <T extends WebService> T getInstance(Class<T> type) {
        return (T)instances.get(type);
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
            synchronized (instances) {
                instances.put(type, this);
            }
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
            String api = request.getParameter("api");

            if (api != null) {
                ServiceDescriptor serviceDescriptor = getServiceDescriptor(request.getServletPath());

                if (api.isEmpty() || api.equals("html")) {
                    response.setContentType(String.format("text/html;charset=%s", UTF_8));

                    TemplateEncoder templateEncoder = new TemplateEncoder(WebService.class.getResource("api.html"));

                    ResourceBundle resourceBundle = ResourceBundle.getBundle(WebService.class.getPackage().getName() + ".api", request.getLocale());

                    templateEncoder.write(mapOf(
                        entry("labels", new ResourceBundleAdapter(resourceBundle)),
                        entry("service", new BeanAdapter(serviceDescriptor))
                    ), response.getOutputStream());
                } else if (api.equals("json")) {
                    encodeResult(response, serviceDescriptor);
                } else {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                }

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
            this.request.remove();
            this.response.remove();

            this.keyList.remove();
            this.keyMap.remove();

            this.body.remove();
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
                        list.add(BeanAdapter.coerce(value, elementType));
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

                argument = BeanAdapter.coerce(value, type);
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
        return BeanAdapter.coerce(keyList.get().get(index), type);
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
        return BeanAdapter.coerce(keyMap.get().get(name), type);
    }

    /**
     * Returns the decoded body content associated with the current request.
     *
     * @param <T>
     * The body type.
     *
     * @return
     * The decoded body content.
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

        return BeanAdapter.coerce(jsonDecoder.read(request.getInputStream()), type);
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

    /**
     * Returns the service descriptor.
     *
     * @param servletPath
     * The servlet path.
     *
     * @return
     * The service descriptor.
     */
    public synchronized ServiceDescriptor getServiceDescriptor(String servletPath) {
        if (servletPath == null) {
            throw new IllegalArgumentException();
        }

        if (serviceDescriptor == null) {
            Class<? extends WebService> type = getClass();

            serviceDescriptor = new ServiceDescriptor(type);

            describeResource(servletPath, root, Arrays.stream(type.getAnnotationsByType(Endpoint.class))
                .collect(Collectors.toMap(endpoint -> servletPath + "/" + endpoint.path(), Function.identity())));
        }

        return serviceDescriptor;
    }

    private void describeResource(String path, Resource resource, Map<String, Endpoint> endpoints) {
        if (!resource.handlerMap.isEmpty()) {
            EndpointDescriptor endpoint = new EndpointDescriptor(path, endpoints.get(path));

            for (Map.Entry<String, List<Handler>> entry : resource.handlerMap.entrySet()) {
                for (Handler handler : entry.getValue()) {
                    OperationDescriptor operation = new OperationDescriptor(entry.getKey().toUpperCase(), handler);

                    Content content = handler.method.getAnnotation(Content.class);

                    if (content != null) {
                        operation.consumes = describeType(content.value());
                    }

                    operation.produces = describeType(handler.method.getGenericReturnType());

                    Parameter[] parameters = handler.method.getParameters();

                    for (int i = 0; i < parameters.length; i++) {
                        Parameter parameter = parameters[i];

                        VariableDescriptor parameterDescriptor = new VariableDescriptor(parameter);

                        parameterDescriptor.type = describeType(parameter.getParameterizedType());

                        operation.parameters.add(parameterDescriptor);
                    }

                    endpoint.operations.add(operation);
                }
            }

            serviceDescriptor.endpoints.add(endpoint);
        }

        for (Map.Entry<String, Resource> entry : resource.resources.entrySet()) {
            describeResource(path + "/" + entry.getKey(), entry.getValue(), endpoints);
        }
    }

    private TypeDescriptor describeType(Type type) {
        if (type instanceof Class<?>) {
            return describeType((Class<?>)type);
        } else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType)type;

            Type rawType = parameterizedType.getRawType();
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();

            if (rawType instanceof Class<?> && Iterable.class.isAssignableFrom((Class<?>)rawType)) {
                return new IterableTypeDescriptor(describeType(actualTypeArguments[0]));
            } else if (rawType == Map.class) {
                return new MapTypeDescriptor(describeType(actualTypeArguments[0]), describeType(actualTypeArguments[1]));
            } else {
                throw new IllegalArgumentException();
            }
        } else {
            throw new IllegalArgumentException();
        }
    }

    private TypeDescriptor describeType(Class<?> type) {
        if (type.isArray()) {
            throw new IllegalArgumentException();
        }

        if (type.isPrimitive()
            || type == Object.class
            || type == Boolean.class
            || Number.class.isAssignableFrom(type)
            || CharSequence.class.isAssignableFrom(type)
            || type == Void.class
            || Date.class.isAssignableFrom(type)
            || type == Instant.class
            || type == LocalDate.class
            || type == LocalTime.class
            || type == LocalDateTime.class
            || type == Duration.class
            || type == Period.class
            || type == UUID.class
            || type == URL.class) {
            return new TypeDescriptor(type, true);
        } else if (Iterable.class.isAssignableFrom(type)) {
            return describeType(BeanAdapter.typeOf(Iterable.class, Object.class));
        } else if (Map.class.isAssignableFrom(type)) {
            return describeType(BeanAdapter.typeOf(Map.class, Object.class, Object.class));
        } else {
            if (type.isEnum()) {
                EnumerationDescriptor enumeration = serviceDescriptor.enumerations.get(type);

                if (enumeration == null) {
                    enumeration = new EnumerationDescriptor(type);

                    serviceDescriptor.enumerations.put(type, enumeration);

                    Field[] fields = type.getDeclaredFields();

                    for (int i = 0; i < fields.length; i++) {
                        Field field = fields[i];

                        if (!field.isEnumConstant()) {
                            continue;
                        }

                        enumeration.values.add(new ConstantDescriptor(field));
                    }
                }
            } else {
                StructureDescriptor structure = serviceDescriptor.structures.get(type);

                if (structure == null) {
                    structure = new StructureDescriptor(type);

                    serviceDescriptor.structures.put(type, structure);

                    if (type.isInterface()) {
                        Class<?>[] interfaces = type.getInterfaces();

                        for (int i = 0; i < interfaces.length; i++) {
                            structure.supertypes.add(describeType(interfaces[i]));
                        }
                    } else {
                        Class<?> baseType = type.getSuperclass();

                        if (baseType != Object.class) {
                            structure.supertypes.add(describeType(baseType));
                        }
                    }

                    for (Map.Entry<String, BeanAdapter.Property> entry : BeanAdapter.getProperties(type).entrySet()) {
                        Method accessor = entry.getValue().getAccessor();

                        if (accessor == null || accessor.getDeclaringClass() != type) {
                            continue;
                        }

                        VariableDescriptor propertyDescriptor = new VariableDescriptor(entry.getKey(), accessor);

                        propertyDescriptor.type = describeType(accessor.getGenericReturnType());

                        structure.properties.add(propertyDescriptor);
                    }
                }
            }

            return new TypeDescriptor(type, false);
        }
    }
}
