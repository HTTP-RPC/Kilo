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

import org.httprpc.kilo.beans.BeanAdapter;
import org.httprpc.kilo.io.JSONDecoder;
import org.httprpc.kilo.io.JSONEncoder;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import org.httprpc.kilo.io.TemplateEncoder;
import org.httprpc.kilo.util.Optionals;
import org.httprpc.kilo.util.ResourceBundleAdapter;

import java.io.IOException;
import java.io.InputStream;
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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.ResourceBundle;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.httprpc.kilo.util.Collections.entry;
import static org.httprpc.kilo.util.Collections.listOf;
import static org.httprpc.kilo.util.Collections.mapOf;

/**
 * Abstract base class for web services.
 */
public abstract class WebService extends HttpServlet {
    /**
     * Describes a service instance.
     */
    public static class ServiceDescriptor {
        private String path;
        private String description;

        private List<EndpointDescriptor> endpoints = new LinkedList<>();

        private Map<Class<?>, EnumerationDescriptor> enumerations = new TreeMap<>(Comparator.comparing(Class::getSimpleName));
        private Map<Class<?>, StructureDescriptor> structures = new TreeMap<>(Comparator.comparing(Class::getSimpleName));

        private ServiceDescriptor(String path, Class<? extends WebService> type) {
            this.path = path;

            description = Optionals.map(type.getAnnotation(Description.class), Description::value);
        }

        /**
         * Returns the path to the service.
         *
         * @return
         * The service's path.
         */
        public String getPath() {
            return path;
        }

        /**
         * Returns a description of the service.
         *
         * @return
         * The service description, or {@code null} for no description.
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

        private List<OperationDescriptor> operations = new LinkedList<>();

        private EndpointDescriptor(String path) {
            this.path = path;
        }

        /**
         * Returns the path to the endpoint.
         *
         * @return
         * The endpoint's path.
         */
        public String getPath() {
            return path;
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
        private Iterable<String> keys;

        private boolean deprecated;

        private TypeDescriptor consumes = null;
        private TypeDescriptor produces = null;

        private List<VariableDescriptor> parameters = new LinkedList<>();

        private OperationDescriptor(String method, Handler handler) {
            this.method = method;

            description = Optionals.map(handler.method.getAnnotation(Description.class), Description::value);
            keys = Optionals.map(handler.method.getAnnotation(Keys.class), keys -> Arrays.asList(keys.value()));

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
         * The operation's description, or {@code null} for no description.
         */
        public String getDescription() {
            return description;
        }

        /**
         * Returns the keys defined by the operation.
         *
         * @return
         * The endpoint's keys.
         */
        public Iterable<String> getKeys() {
            return keys;
        }

        /**
         * Indicates that the operation is deprecated.
         *
         * @return
         * {@code true} if the operation is deprecated; {@code false},
         * otherwise.
         */
        public boolean isDeprecated() {
            return deprecated;
        }

        /**
         * Returns the type of content consumed by the operation.
         *
         * @return
         * The type of content consumed by the operation, or {@code null} if
         * the operation does not accept request content.
         */
        public TypeDescriptor getConsumes() {
            return consumes;
        }

        /**
         * Returns the type of content produced by the operation.
         *
         * @return
         * The type of content produced by the operation, or {@code null} if
         * the operation does not return response content.
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
        private boolean required;

        private String description;

        private TypeDescriptor type = null;

        private VariableDescriptor(Parameter parameter) {
            name = parameter.getName();

            if (parameter.getType() == List.class) {
                required = true;
            } else {
                required = parameter.getAnnotation(Required.class) != null;
            }

            description = Optionals.map(parameter.getAnnotation(Description.class), Description::value);
        }

        private VariableDescriptor(String name, Method accessor) {
            this.name = name;

            required = accessor.getAnnotation(Required.class) != null;

            description = Optionals.map(accessor.getAnnotation(Description.class), Description::value);
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
         * Indicates that the variable is required.
         *
         * @return
         * {@code true} if the variable is required; {@code false}, otherwise.
         */
        public boolean isRequired() {
            return required;
        }

        /**
         * Returns a description of the variable.
         *
         * @return
         * The variable's description, or {@code null} for no description.
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

            description = Optionals.map(type.getAnnotation(Description.class), Description::value);
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
         * The enumeration's description, or {@code null} for no description.
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

            description = Optionals.map(field.getAnnotation(Description.class), Description::value);
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
         * The constant's description, or {@code null} for no description.
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

            description = Optionals.map(type.getAnnotation(Description.class), Description::value);
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
         * The structure's description, or {@code null} for no description.
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
         * {@code true} if the type is intrinsic; {@code false}, otherwise.
         */
        public boolean isIntrinsic() {
            return intrinsic;
        }

        /**
         * Indicates that the type is an iterable.
         *
         * @return
         * {@code true} if the type is an iterable; {@code false}, otherwise.
         */
        public boolean isIterable() {
            return false;
        }

        /**
         * Returns the element type.
         *
         * @return
         * The element type, or {@code null} if the type is not an iterable.
         */
        public TypeDescriptor getElementType() {
            return null;
        }

        /**
         * Indicates that the type is a map.
         *
         * @return
         * {@code true} if the type is a map; {@code false}, otherwise.
         */
        public boolean isMap() {
            return false;
        }

        /**
         * Returns the key type.
         *
         * @return
         * The key type, or {@code null} if the type is not a map.
         */
        public TypeDescriptor getKeyType() {
            return null;
        }

        /**
         * Returns the value type.
         *
         * @return
         * The value type, or {@code null} if the type is not a map.
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
            var i1 = order.indexOf(verb1);
            var i2 = order.indexOf(verb2);

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

    private static final Map<Class<? extends WebService>, WebService> instances = new HashMap<>();

    private static final String UTF_8 = "UTF-8";

    /**
     * Returns a service instance.
     *
     * @param <T>
     * The service type.
     *
     * @param type
     * The service type.
     *
     * @return
     * The service instance, or {@code null} if no service of the given type
     * exists.
     */
    @SuppressWarnings("unchecked")
    public static synchronized <T extends WebService> T getInstance(Class<T> type) {
        return (T)instances.get(type);
    }

    /**
     * Returns a list of descriptors for all active services.
     *
     * @return
     * A list of active service descriptors.
     */
    public static synchronized List<ServiceDescriptor> getServiceDescriptors() {
        return instances.values().stream()
            .map(WebService::getServiceDescriptor)
            .sorted(Comparator.comparing(WebService.ServiceDescriptor::getPath))
            .collect(Collectors.toList());
    }

    /**
     * Initializes the service instance.
     * {@inheritDoc}
     */
    @Override
    public void init() throws ServletException {
        var type = getClass();

        var webServlet = type.getAnnotation(WebServlet.class);

        var urlPatterns = webServlet.urlPatterns();

        if (urlPatterns.length == 0) {
            throw new ServletException("At least one URL pattern is required.");
        }

        var path = urlPatterns[0];

        if (!path.startsWith("/") && (path.length() == 1 || path.endsWith("/*"))) {
            throw new ServletException("Invalid URL pattern.");
        }

        path = path.substring(0, path.length() - 2);

        root = new Resource();

        var methods = getClass().getMethods();

        for (var i = 0; i < methods.length; i++) {
            var method = methods[i];

            var requestMethod = method.getAnnotation(RequestMethod.class);

            if (requestMethod != null) {
                var handler = new Handler(method);

                var resource = root;

                var resourcePath = method.getAnnotation(ResourcePath.class);

                if (resourcePath != null) {
                    var components = resourcePath.value().split("/");

                    for (var j = 0; j < components.length; j++) {
                        var component = components[j];

                        if (component.length() == 0) {
                            continue;
                        }

                        if (component.startsWith(ResourcePath.PATH_VARIABLE_PREFIX)) {
                            var k = ResourcePath.PATH_VARIABLE_PREFIX.length();

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

                        var child = resource.resources.get(component);

                        if (child == null) {
                            child = new Resource();

                            resource.resources.put(component, child);
                        }

                        resource = child;
                    }
                }

                var verb = requestMethod.value().toLowerCase();

                var handlerList = resource.handlerMap.get(verb);

                if (handlerList == null) {
                    handlerList = new LinkedList<>();

                    resource.handlerMap.put(verb, handlerList);
                }

                handlerList.add(handler);
            }
        }

        sort(root);

        serviceDescriptor = new ServiceDescriptor(path, type);

        describeResource(path, root);

        if (getClass().getAnnotation(WebServlet.class) != null) {
            synchronized (WebService.class) {
                instances.put(type, this);
            }
        }
    }

    private static void sort(Resource root) {
        for (var handlers : root.handlerMap.values()) {
            Comparator<Handler> methodNameComparator = Comparator.comparing(handler -> handler.method.getName());
            Comparator<Handler> methodParameterCountComparator = Comparator.comparing(handler -> handler.method.getParameterCount());

            handlers.sort(methodNameComparator.thenComparing(methodParameterCountComparator.reversed()));
        }

        for (var resource : root.resources.values()) {
            sort(resource);
        }
    }

    /**
     * Processes a service request.
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        var verb = request.getMethod().toLowerCase();
        var pathInfo = request.getPathInfo();

        if (verb.equals("get") && pathInfo == null) {
            var api = request.getParameter("api");

            if (api != null) {
                var accept = request.getHeader("Accept");

                if (accept != null && accept.equalsIgnoreCase("application/json")) {
                    response.setContentType(String.format("application/json;charset=%s", UTF_8));

                    var jsonEncoder = new JSONEncoder();

                    jsonEncoder.write(new BeanAdapter(serviceDescriptor), response.getOutputStream());
                } else {
                    response.setContentType(String.format("text/html;charset=%s", UTF_8));

                    var templateEncoder = new TemplateEncoder(WebService.class.getResource("api.html"));

                    var resourceBundle = ResourceBundle.getBundle(WebService.class.getPackage().getName() + ".api", request.getLocale());

                    templateEncoder.write(mapOf(
                        entry("labels", new ResourceBundleAdapter(resourceBundle)),
                        entry("service", new BeanAdapter(serviceDescriptor))
                    ), response.getOutputStream());
                }

                response.flushBuffer();

                return;
            }
        }

        var resource = root;

        List<String> keyList = new ArrayList<>();

        if (pathInfo != null) {
            var components = pathInfo.split("/");

            for (var i = 0; i < components.length; i++) {
                var component = components[i];

                if (component.length() == 0) {
                    continue;
                }

                var child = resource.resources.get(component);

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

        var handlerList = resource.handlerMap.get(verb);

        if (handlerList == null) {
            super.service(request, response);
            return;
        }

        if (request.getCharacterEncoding() == null) {
            request.setCharacterEncoding(UTF_8);
        }

        Map<String, List<?>> parameterMap = new HashMap<>();

        var parameterNames = request.getParameterNames();

        while (parameterNames.hasMoreElements()) {
            var name = parameterNames.nextElement();

            parameterMap.put(name, Arrays.asList(request.getParameterValues(name)));
        }

        var contentType = request.getContentType();

        if (contentType != null && contentType.startsWith("multipart/form-data")) {
            for (var part : request.getParts()) {
                var submittedFileName = part.getSubmittedFileName();

                if (submittedFileName == null || submittedFileName.length() == 0) {
                    continue;
                }

                var name = part.getName();

                var values = (List<URL>)parameterMap.get(name);

                if (values == null) {
                    values = new ArrayList<>();

                    parameterMap.put(name, values);
                }

                values.add(new URL("part", null, -1, submittedFileName, new PartURLStreamHandler(part)));
            }
        }

        var handler = getHandler(handlerList, parameterMap);

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
            var key = handler.keys.get(i);

            if (key != null) {
                keyMap.put(key, keyList.get(i));
            }
        }

        Object[] arguments;
        try {
            arguments = getArguments(handler.method, parameterMap);
        } catch (Exception exception) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, exception);
            return;
        }

        var content = handler.method.getAnnotation(Content.class);

        Object body;
        if (content != null) {
            var type = content.type();

            if (type.getTypeParameters().length > 0) {
                throw new ServletException("Unsupported content type.");
            }

            try {
                body = decodeBody(request, type, content.multiple());
            } catch (Exception exception) {
                sendError(response, HttpServletResponse.SC_BAD_REQUEST, exception);
                return;
            }

            if (body == null) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
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
            result = handler.method.invoke(this, arguments);
        } catch (IllegalAccessException | InvocationTargetException exception) {
            if (response.isCommitted()) {
                throw new ServletException(exception);
            }

            var cause = exception.getCause();

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

            sendError(response, status, cause);

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

        var returnType = handler.method.getReturnType();

        if (returnType == Void.TYPE || returnType == Void.class) {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        } else if (result == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } else {
            encodeResult(request, response, result);
        }
    }

    private static Handler getHandler(List<Handler> handlerList, Map<String, List<?>> parameterMap) {
        Handler handler = null;

        var n = parameterMap.size();

        var i = Integer.MAX_VALUE;

        for (var option : handlerList) {
            var parameters = option.method.getParameters();

            if (parameters.length >= n) {
                var j = 0;

                for (var k = 0; k < parameters.length; k++) {
                    var name = parameters[k].getName();

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
        var parameters = method.getParameters();

        var arguments = new Object[parameters.length];

        for (var i = 0; i < parameters.length; i++) {
            var parameter = parameters[i];

            var name = parameter.getName();
            var type = parameter.getType();

            var values = parameterMap.get(name);

            Object argument;
            if (type == List.class) {
                List<?> list;
                if (values != null) {
                    var elementType = ((ParameterizedType)parameter.getParameterizedType()).getActualTypeArguments()[0];

                    list = BeanAdapter.coerce(values, List.class, elementType);
                } else {
                    list = listOf();
                }

                argument = list;
            } else {
                Object value;
                if (values != null) {
                    value = values.get(values.size() - 1);
                } else {
                    value = null;
                }

                if (parameter.getAnnotation(Required.class) != null && value == null) {
                    throw new IllegalArgumentException(String.format("Parameter \"%s\" is required.", name));
                }

                argument = BeanAdapter.coerce(value, type);
            }

            arguments[i] = argument;
        }

        return arguments;
    }

    private void sendError(HttpServletResponse response, int status, Throwable cause) throws IOException {
        response.setStatus(status);

        var message = cause.getMessage();

        if (message != null) {
            response.setContentType(String.format("text/plain;charset=%s", UTF_8));
            response.getWriter().append(message);
            response.flushBuffer();
        }
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
     * @return
     * The decoded request body.
     */
    protected Object getBody() {
        return this.body.get();
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
     * {@code true} if the method should be invoked; {@code false}, otherwise.
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
     * @param multiple
     * Indicates that the body is expected to contain a list of values of the
     * given type.
     *
     * @return
     * The decoded body content.
     *
     * @throws IOException
     * If an exception occurs while decoding the content.
     */
    protected Object decodeBody(HttpServletRequest request, Class<?> type, boolean multiple) throws IOException {
        var jsonDecoder = new JSONDecoder();

        var body = jsonDecoder.read(request.getInputStream());

        if (multiple) {
            return BeanAdapter.coerce(body, List.class, type);
        } else {
            return BeanAdapter.coerce(body, type);
        }
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

        var jsonEncoder = new JSONEncoder(isCompact());

        jsonEncoder.write(BeanAdapter.adapt(result), response.getOutputStream());
    }

    /**
     * Enables or disables compact output.
     *
     * @return
     * {@code true} if the encoded output should be compact; {@code false},
     * otherwise.
     */
    protected boolean isCompact() {
        return false;
    }

    /**
     * Returns the service descriptor.
     *
     * @return
     * The service descriptor.
     */
    public ServiceDescriptor getServiceDescriptor() {
        return serviceDescriptor;
    }

    private void describeResource(String path, Resource resource) {
        if (!resource.handlerMap.isEmpty()) {
            var endpoint = new EndpointDescriptor(path);

            for (var entry : resource.handlerMap.entrySet()) {
                for (var handler : entry.getValue()) {
                    var operation = new OperationDescriptor(entry.getKey().toUpperCase(), handler);

                    var content = handler.method.getAnnotation(Content.class);

                    if (content != null) {
                        if (content.multiple()) {
                            operation.consumes = describeType(BeanAdapter.typeOf(List.class, content.type()));
                        } else {
                            operation.consumes = describeType(content.type());
                        }
                    }

                    operation.produces = describeType(handler.method.getGenericReturnType());

                    var parameters = handler.method.getParameters();

                    for (var i = 0; i < parameters.length; i++) {
                        var parameter = parameters[i];

                        var parameterDescriptor = new VariableDescriptor(parameter);

                        parameterDescriptor.type = describeType(parameter.getParameterizedType());

                        operation.parameters.add(parameterDescriptor);
                    }

                    endpoint.operations.add(operation);
                }
            }

            serviceDescriptor.endpoints.add(endpoint);
        }

        for (var entry : resource.resources.entrySet()) {
            describeResource(path + "/" + entry.getKey(), entry.getValue());
        }
    }

    private TypeDescriptor describeType(Type type) {
        if (type instanceof Class<?>) {
            return describeType((Class<?>)type);
        } else if (type instanceof ParameterizedType) {
            var parameterizedType = (ParameterizedType)type;

            var rawType = parameterizedType.getRawType();
            var actualTypeArguments = parameterizedType.getActualTypeArguments();

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
                var enumeration = serviceDescriptor.enumerations.get(type);

                if (enumeration == null) {
                    enumeration = new EnumerationDescriptor(type);

                    serviceDescriptor.enumerations.put(type, enumeration);

                    var fields = type.getDeclaredFields();

                    for (var i = 0; i < fields.length; i++) {
                        var field = fields[i];

                        if (!field.isEnumConstant()) {
                            continue;
                        }

                        enumeration.values.add(new ConstantDescriptor(field));
                    }
                }
            } else {
                var structure = serviceDescriptor.structures.get(type);

                if (structure == null) {
                    structure = new StructureDescriptor(type);

                    serviceDescriptor.structures.put(type, structure);

                    if (type.isInterface()) {
                        var interfaces = type.getInterfaces();

                        for (var i = 0; i < interfaces.length; i++) {
                            structure.supertypes.add(describeType(interfaces[i]));
                        }
                    } else {
                        var baseType = type.getSuperclass();

                        if (baseType != Object.class) {
                            structure.supertypes.add(describeType(baseType));
                        }
                    }

                    for (var entry : BeanAdapter.getProperties(type).entrySet()) {
                        var accessor = entry.getValue().getAccessor();

                        if (accessor == null || accessor.getDeclaringClass() != type) {
                            continue;
                        }

                        var propertyDescriptor = new VariableDescriptor(entry.getKey(), accessor);

                        propertyDescriptor.type = describeType(accessor.getGenericReturnType());

                        structure.properties.add(propertyDescriptor);
                    }
                }
            }

            return new TypeDescriptor(type, false);
        }
    }
}
