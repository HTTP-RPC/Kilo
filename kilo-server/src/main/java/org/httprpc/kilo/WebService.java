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

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import org.httprpc.kilo.beans.BeanAdapter;
import org.httprpc.kilo.io.JSONDecoder;
import org.httprpc.kilo.io.JSONEncoder;
import org.httprpc.kilo.io.TextEncoder;
import org.httprpc.kilo.xml.ElementAdapter;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

import static org.httprpc.kilo.util.Collections.*;
import static org.httprpc.kilo.util.Iterables.*;
import static org.httprpc.kilo.util.Optionals.*;

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

        private Map<Class<?>, EnumerationDescriptor> enumerations = new TreeMap<>(Comparator.comparing(WebService::getTypeName));
        private Map<Class<?>, StructureDescriptor> structures = new TreeMap<>(Comparator.comparing(WebService::getTypeName));

        private ServiceDescriptor(String path, Class<? extends WebService> type) {
            this.path = path;

            description = map(type.getAnnotation(Description.class), Description::value);
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
        public List<EndpointDescriptor> getEndpoints() {
            return endpoints;
        }

        /**
         * Returns the enumerations defined by the service.
         *
         * @return
         * The service enumerations.
         */
        public List<EnumerationDescriptor> getEnumerations() {
            return new ArrayList<>(enumerations.values());
        }

        /**
         * Returns the structures defined by the service.
         *
         * @return
         * The service structures.
         */
        public List<StructureDescriptor> getStructures() {
            return new ArrayList<>(structures.values());
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
        public List<OperationDescriptor> getOperations() {
            return operations;
        }
    }

    /**
     * Describes a service operation.
     */
    public static class OperationDescriptor {
        private String method;
        private String description;
        private boolean formData;
        private boolean deprecated;

        private TypeDescriptor produces = null;

        private boolean parameters = false;

        private List<ParameterDescriptor> pathParameters = new LinkedList<>();
        private List<ParameterDescriptor> queryParameters = new LinkedList<>();

        private ParameterDescriptor bodyParameter = null;

        private OperationDescriptor(String method, Method handler) {
            this.method = method;

            description = map(handler.getAnnotation(Description.class), Description::value);

            formData = handler.getAnnotation(FormData.class) != null;
            deprecated = handler.getAnnotation(Deprecated.class) != null;
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
         * Indicates that the operation accepts form data.
         */
        public boolean isFormData() {
            return formData;
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
         * Indicates that the operation defines at least one parameter.
         *
         * @return
         * {@code true} if the operation defines at least one parameter;
         * {@code false}, otherwise.
         */
        public boolean getParameters() {
            return parameters;
        }

        /**
         * Returns the path parameters defined by the operation.
         *
         * @return
         * The operation's path parameters.
         */
        public List<ParameterDescriptor> getPathParameters() {
            return pathParameters;
        }

        /**
         * Returns the query parameters defined by the operation.
         *
         * @return
         * The operation's query parameters.
         */
        public List<ParameterDescriptor> getQueryParameters() {
            return queryParameters;
        }

        /**
         * Returns the body parameter defined by the operation.
         *
         * @return
         * The operation's body parameter, or {@code null} if the operation
         * does not define a body parameter.
         */
        public ParameterDescriptor getBodyParameter() {
            return bodyParameter;
        }
    }

    /**
     * Describes a parameter.
     */
    public static class ParameterDescriptor {
        private String name;
        private String description;

        private boolean required = false;

        private TypeDescriptor type = null;

        private ParameterDescriptor(Parameter parameter) {
            name = coalesce(map(parameter.getAnnotation(Name.class), Name::value), parameter::getName);

            description = map(parameter.getAnnotation(Description.class), Description::value);
        }

        /**
         * Returns the name of the parameter.
         *
         * @return
         * The parameter's name.
         */
        public String getName() {
            return name;
        }

        /**
         * Returns a description of the parameter.
         *
         * @return
         * The parameter's description, or {@code null} for no description.
         */
        public String getDescription() {
            return description;
        }

        /**
         * Indicates that the parameter is required.
         *
         * @return
         * {@code true} if the parameter is required; {@code false}, otherwise.
         */
        public boolean isRequired() {
            return required;
        }

        /**
         * Returns the type of the parameter.
         *
         * @return
         * The parameter's type.
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
            name = getTypeName(type);

            description = map(type.getAnnotation(Description.class), Description::value);
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
        public List<ConstantDescriptor> getValues() {
            return values;
        }
    }

    /**
     * Describes a constant.
     */
    public static class ConstantDescriptor {
        private String name;
        private String description;
        private boolean deprecated;

        private ConstantDescriptor(Field field) {
            Object constant;
            try {
                constant = field.get(null);
            } catch (IllegalAccessException exception) {
                throw new RuntimeException(exception);
            }

            name = constant.toString();

            description = map(field.getAnnotation(Description.class), Description::value);

            deprecated = field.getAnnotation(Deprecated.class) != null;
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

        /**
         * Indicates that the constant is deprecated.
         *
         * @return
         * {@code true} if the constant is deprecated; {@code false},
         * otherwise.
         */
        public boolean isDeprecated() {
            return deprecated;
        }
    }

    /**
     * Describes a structure.
     */
    public static class StructureDescriptor {
        private String name;
        private String description;

        private List<TypeDescriptor> supertypes = new LinkedList<>();
        private List<PropertyDescriptor> properties = new LinkedList<>();

        private StructureDescriptor(Class<?> type) {
            name = getTypeName(type);

            description = map(type.getAnnotation(Description.class), Description::value);
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
        public List<TypeDescriptor> getSupertypes() {
            return supertypes;
        }

        /**
         * Returns the properties defined by the structure.
         *
         * @return
         * The structure's properties.
         */
        public List<PropertyDescriptor> getProperties() {
            return properties;
        }
    }

    /**
     * Describes a property.
     */
    public static class PropertyDescriptor {
        private String name;
        private String description;
        private boolean required;
        private boolean deprecated;

        private TypeDescriptor type = null;

        private PropertyDescriptor(String name, Method accessor) {
            this.name = name;

            description = map(accessor.getAnnotation(Description.class), Description::value);

            required = accessor.getAnnotation(Required.class) != null;
            deprecated = accessor.getAnnotation(Deprecated.class) != null;
        }

        /**
         * Returns the name of the property.
         *
         * @return
         * The property's name.
         */
        public String getName() {
            return name;
        }

        /**
         * Returns a description of the property.
         *
         * @return
         * The property's description, or {@code null} for no description.
         */
        public String getDescription() {
            return description;
        }

        /**
         * Indicates that the property is required.
         *
         * @return
         * {@code true} if the property is required; {@code false}, otherwise.
         */
        public boolean isRequired() {
            return required;
        }

        /**
         * Indicates that the property is deprecated.
         *
         * @return
         * {@code true} if the property is deprecated; {@code false},
         * otherwise.
         */
        public boolean isDeprecated() {
            return deprecated;
        }

        /**
         * Returns the type of the property.
         *
         * @return
         * The property's type.
         */
        public TypeDescriptor getType() {
            return type;
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
                return getTypeName(type);
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
         * Indicates that the type is iterable.
         *
         * @return
         * {@code true} if the type is iterable; {@code false}, otherwise.
         */
        public boolean isIterable() {
            return false;
        }

        /**
         * Returns the element type.
         *
         * @return
         * The element type, or {@code null} if the type is not a list.
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

    /**
     * Indicates that a service method may not immediately fulfill submitted
     * requests.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Accepts {
    }

    /**
     * Indicates that a service method results in creation of a resource.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Creates {
    }

    /**
     * Indicates that a service method accepts form data.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface FormData {
    }

    private enum Verb {
        GET,
        POST,
        PUT,
        DELETE
    }

    private static class Resource {
        Map<String, Resource> resources = new TreeMap<>();
        Map<Verb, List<Method>> handlerLists = new TreeMap<>();
    }

    private Resource root;
    private ServiceDescriptor serviceDescriptor;

    /**
     * JSON MIME type.
     */
    public static final String APPLICATION_JSON = "application/json";

    /**
     * XML MIME type.
     */
    public static final String TEXT_XML = "text/xml";

    /**
     * HTML MIME type.
     */
    public static final String TEXT_HTML = "text/html";

    /**
     * CSV MIME type.
     */
    public static final String TEXT_CSV = "text/csv";

    /**
     * Plain text MIME type.
     */
    public static final String TEXT_PLAIN = "text/plain";

    private static final String APPLICATION_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";
    private static final String MULTIPART_FORM_DATA = "multipart/form-data";

    private static final String CONTENT_TYPE_FORMAT = "%s;charset=%s";

    private static final ThreadLocal<Connection> connection = new ThreadLocal<>();

    private static final ThreadLocal<HttpServletRequest> request = new ThreadLocal<>();
    private static final ThreadLocal<HttpServletResponse> response = new ThreadLocal<>();

    private static final Comparator<Method> handlerComparator;
    static {
        var nameComparator = Comparator.comparing(Method::getName);
        var parameterCountComparator = Comparator.comparing(Method::getParameterCount);

        handlerComparator = nameComparator.thenComparing(parameterCountComparator.reversed());
    }

    private static final Map<Class<? extends WebService>, WebService> instances = new HashMap<>();

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
     * Returns descriptors for all active services.
     *
     * @return
     * Descriptors for all active services, sorted by path.
     */
    public static synchronized SortedMap<String, ServiceDescriptor> getServiceDescriptors() {
        return sortedMapOf(mapAll(mapAll(instances.values(), WebService::getServiceDescriptor),
            serviceDescriptor -> entry(serviceDescriptor.getPath(), serviceDescriptor)
        ));
    }

    @Override
    public void init() throws ServletException {
        var type = getClass();

        var webServlet = type.getAnnotation(WebServlet.class);

        if (webServlet == null) {
            throw new ServletException("Missing web servlet annotation.");
        }

        var urlPattern = firstOf(iterableOf(webServlet.urlPatterns()));

        if (urlPattern == null) {
            throw new ServletException("Missing URL pattern.");
        }

        if (!(urlPattern.startsWith("/") && urlPattern.endsWith("/*"))) {
            throw new ServletException("Invalid URL pattern.");
        }

        var path = urlPattern.substring(0, urlPattern.length() - 2);

        root = new Resource();

        var methods = type.getMethods();

        for (var i = 0; i < methods.length; i++) {
            var handler = methods[i];

            var requestMethod = handler.getAnnotation(RequestMethod.class);

            if (requestMethod == null) {
                continue;
            }

            Verb verb;
            try {
                verb = Verb.valueOf(requestMethod.value().toUpperCase());
            } catch (Exception exception) {
                throw new ServletException("Invalid verb.");
            }

            var resource = root;

            var resourcePath = handler.getAnnotation(ResourcePath.class);

            if (resourcePath != null) {
                var components = resourcePath.value().split("/");

                for (var j = 0; j < components.length; j++) {
                    var component = components[j];

                    if (component.isEmpty()) {
                        throw new ServletException("Invalid resource path.");
                    }

                    resource = resource.resources.computeIfAbsent(component, key -> new Resource());
                }
            }

            resource.handlerLists.computeIfAbsent(verb, key -> new LinkedList<>()).add(handler);
        }

        sort(root);

        serviceDescriptor = new ServiceDescriptor(path, type);

        describeResource(path, root);

        synchronized (WebService.class) {
            instances.put(type, this);
        }
    }

    private static void sort(Resource root) {
        for (var handlerList : root.handlerLists.values()) {
            handlerList.sort(handlerComparator);
        }

        for (var resource : root.resources.values()) {
            sort(resource);
        }
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try (var connection = openConnection()) {
            if (connection != null) {
                connection.setAutoCommit(false);
            }

            setConnection(connection);

            try {
                process(request, response);

                if (connection != null) {
                    if (response.getStatus() / 100 == 2) {
                        connection.commit();
                    } else {
                        connection.rollback();
                    }
                }
            } catch (Exception exception) {
                if (connection != null) {
                    connection.rollback();
                }

                log(exception.getMessage(), exception);

                throw exception;
            } finally {
                if (connection != null) {
                    connection.setAutoCommit(true);
                }

                setConnection(null);
            }
        } catch (SQLException exception) {
            throw new ServletException(exception);
        }
    }

    /**
     * Opens a database connection.
     *
     * @return
     * A database connection, or {@code null} if the service does not require a
     * database connection.
     */
    protected Connection openConnection() throws SQLException {
        var dataSourceName = getDataSourceName();

        if (dataSourceName != null) {
            DataSource dataSource;
            try {
                var initialContext = new InitialContext();

                dataSource = (DataSource)initialContext.lookup(dataSourceName);
            } catch (NamingException exception) {
                throw new IllegalStateException(exception);
            }

            return dataSource.getConnection();
        } else {
            return null;
        }
    }

    /**
     * Returns the data source name.
     *
     * @return
     * The data source name, or {@code null} if the service does not require a
     * data source.
     */
    protected String getDataSourceName() {
        return null;
    }

    /**
     * Processes a service request.
     *
     * @param request
     * The servlet request.
     *
     * @param response
     * The servlet response.
     *
     * @throws ServletException
     * If an unexpected error occurs.
     *
     * @throws IOException
     * If an error occurs while decoding the request or encoding the response.
     */
    @SuppressWarnings("unchecked")
    protected void process(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        var resource = root;

        List<String> keys = new ArrayList<>();

        var pathInfo = request.getPathInfo();

        if (pathInfo != null) {
            var components = pathInfo.split("/");

            for (var i = 1; i < components.length; i++) {
                var component = components[i];

                var child = resource.resources.get(component);

                if (child == null) {
                    child = resource.resources.get("?");

                    if (child == null) {
                        response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                        return;
                    }

                    keys.add(component);
                }

                resource = child;
            }
        }

        Verb verb;
        try {
            verb = Verb.valueOf(request.getMethod().toUpperCase());
        } catch (Exception exception) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        var handlerList = resource.handlerLists.get(verb);

        if (handlerList == null) {
            response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        if (request.getCharacterEncoding() == null) {
            request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        }

        Map<String, List<?>> argumentMap = new HashMap<>();

        var parameterNames = request.getParameterNames();

        while (parameterNames.hasMoreElements()) {
            var name = parameterNames.nextElement();

            argumentMap.put(name, Arrays.asList(request.getParameterValues(name)));
        }

        var contentType = map(request.getContentType(), String::toLowerCase);

        if (contentType != null && contentType.startsWith(MULTIPART_FORM_DATA)) {
            for (var part : request.getParts()) {
                var submittedFileName = part.getSubmittedFileName();

                if (submittedFileName == null || submittedFileName.isEmpty()) {
                    continue;
                }

                var name = part.getName();

                var values = (List<Part>)argumentMap.get(name);

                if (values == null) {
                    values = new ArrayList<>();

                    argumentMap.put(name, values);
                }

                values.add(part);
            }
        }

        var empty = contentType == null
            || contentType.startsWith(APPLICATION_X_WWW_FORM_URLENCODED)
            || contentType.startsWith(MULTIPART_FORM_DATA);

        var handler = getHandler(handlerList, keys.size(), argumentMap.keySet(), empty);

        if (handler == null) {
            response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        Object[] arguments;
        try {
            arguments = getArguments(handler.getParameters(), keys, argumentMap, empty, request);
        } catch (Exception exception) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);

            reportError(response, exception);

            return;
        }

        WebService.request.set(request);
        WebService.response.set(response);

        Object result;
        try {
            result = handler.invoke(this, arguments);
        } catch (IllegalAccessException | InvocationTargetException exception) {
            var cause = exception.getCause();

            if (response.isCommitted()) {
                if (cause != null) {
                    log(cause.getMessage(), cause);
                }

                return;
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

            reportError(response, cause);

            return;
        } finally {
            WebService.request.remove();
            WebService.response.remove();
        }

        if (response.isCommitted()) {
            return;
        }

        if (result != null) {
            if (handler.getAnnotation(Accepts.class) != null) {
                response.setStatus(HttpServletResponse.SC_ACCEPTED);
            } else if (handler.getAnnotation(Creates.class) != null) {
                response.setStatus(HttpServletResponse.SC_CREATED);
            } else {
                response.setStatus(HttpServletResponse.SC_OK);
            }

            try {
                encodeResult(request, response, result);
            } catch (Exception exception) {
                log(exception.getMessage(), exception);
            }
        } else {
            var returnType = handler.getReturnType();

            if (returnType == Void.TYPE || returnType == Void.class) {
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
        }
    }

    private static Method getHandler(List<Method> handlerList, int keyCount, Set<String> argumentNames, boolean empty) {
        for (var handler : handlerList) {
            var parameters = handler.getParameters();

            var n = parameters.length;

            if (!empty) {
                n--;
            }

            if (keyCount > n) {
                continue;
            }

            if (argumentNames.isEmpty()) {
                return handler;
            }

            var c = 0;

            var argumentCount = argumentNames.size();

            for (var i = keyCount; i < n; i++) {
                var parameter = parameters[i];

                var name = coalesce(map(parameter.getAnnotation(Name.class), Name::value), parameter::getName);

                if (argumentNames.contains(name) && ++c == argumentCount) {
                    return handler;
                }
            }
        }

        return null;
    }

    private Object[] getArguments(Parameter[] parameters, List<String> keys, Map<String, List<?>> argumentMap, boolean empty, HttpServletRequest request) {
        var n = parameters.length;

        var arguments = new Object[n];

        if (!empty) {
            n--;
        }

        var keyCount = keys.size();

        for (var i = 0; i < n; i++) {
            var parameter = parameters[i];

            if (i < keyCount) {
                arguments[i] = BeanAdapter.coerce(keys.get(i), parameter.getType());
            } else {
                var name = coalesce(map(parameter.getAnnotation(Name.class), Name::value), parameter::getName);
                var type = parameter.getType();

                var values = coalesce(argumentMap.get(name), () -> emptyListOf(Object.class));

                Object argument;
                if (type.isArray()) {
                    var componentType = type.getComponentType();

                    argument = Array.newInstance(componentType, values.size());

                    var j = 0;

                    for (var value : values) {
                        Array.set(argument, j++, BeanAdapter.coerce(value, componentType));
                    }
                } else if (Collection.class.isAssignableFrom(type)) {
                    var parameterizedType = (ParameterizedType)parameter.getParameterizedType();
                    var elementType = (Class<?>)parameterizedType.getActualTypeArguments()[0];

                    Collection<Object> collection;
                    if (type == List.class) {
                        collection = new ArrayList<>(values.size());
                    } else if (Set.class.isAssignableFrom(type)) {
                        collection = (type == SortedSet.class) ? new TreeSet<>() : new LinkedHashSet<>(values.size());
                    } else {
                        throw new UnsupportedOperationException("Unsupported collection type.");
                    }

                    for (var value : values) {
                        collection.add(BeanAdapter.coerce(value, elementType));
                    }

                    argument = collection;
                } else {
                    var value = values.isEmpty() ? null : values.getLast();

                    if (parameter.getAnnotation(Required.class) != null && value == null) {
                        throw new IllegalArgumentException(String.format("Parameter \"%s\" is required.", parameter.getName()));
                    }

                    argument = BeanAdapter.coerce(value, type);
                }

                arguments[i] = argument;
            }
        }

        if (n < parameters.length) {
            var type = parameters[n].getParameterizedType();

            Object body;
            if (type == Void.class) {
                body = null;
            } else {
                try {
                    body = decodeBody(request, type);
                } catch (IOException exception) {
                    throw new UnsupportedOperationException(exception);
                }
            }

            arguments[n] = body;
        }

        return arguments;
    }

    /**
     * Returns the database connection.
     *
     * @return
     * The database connection.
     */
    protected static Connection getConnection() {
        return connection.get();
    }

    /**
     * Sets the database connection.
     *
     * @param connection
     * The database connection.
     */
    protected static void setConnection(Connection connection) {
        if (connection != null) {
            WebService.connection.set(connection);
        } else {
            WebService.connection.remove();
        }
    }

    /**
     * Returns the servlet request.
     *
     * @return
     * The servlet request.
     */
    protected static HttpServletRequest getRequest() {
        return request.get();
    }

    /**
     * Returns the servlet response.
     *
     * @return
     * The servlet response.
     */
    protected static HttpServletResponse getResponse() {
        return response.get();
    }

    /**
     * Decodes the body of a service request.
     *
     * @param request
     * The servlet request.
     *
     * @param type
     * The body type.
     *
     * @return
     * The decoded body.
     *
     * @throws IOException
     * If an error occurs while decoding the content.
     */
    protected Object decodeBody(HttpServletRequest request, Type type) throws IOException {
        if (type == Document.class) {
            var documentBuilder = ElementAdapter.newDocumentBuilder();

            try {
                return documentBuilder.parse(request.getInputStream());
            } catch (SAXException exception) {
                throw new IOException(exception);
            }
        } else {
            var jsonDecoder = new JSONDecoder();

            return BeanAdapter.coerceGeneric(jsonDecoder.read(request.getInputStream()), type);
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
     * The operation result.
     *
     * @throws IOException
     * If an error occurs while encoding the result.
     */
    protected void encodeResult(HttpServletRequest request, HttpServletResponse response, Object result) throws IOException {
        if (result instanceof Document document) {
            response.setContentType(String.format(CONTENT_TYPE_FORMAT, TEXT_XML, StandardCharsets.UTF_8));

            var transformer = ElementAdapter.newTransformer();

            try {
                transformer.transform(new DOMSource(document), new StreamResult(response.getOutputStream()));
            } catch (TransformerException exception) {
                throw new IOException(exception);
            }
        } else {
            response.setContentType(String.format(CONTENT_TYPE_FORMAT, APPLICATION_JSON, StandardCharsets.UTF_8));

            var jsonEncoder = new JSONEncoder(isCompact());

            jsonEncoder.write(result, response.getOutputStream());
        }
    }

    /**
     * Enables compact output.
     *
     * {@code true} if compact output is enabled; {@code false}, otherwise.
     */
    protected boolean isCompact() {
        return false;
    }

    /**
     * Reports an error.
     *
     * @param response
     * The servlet response.
     *
     * @param cause
     * The cause of the error.
     */
    protected void reportError(HttpServletResponse response, Throwable cause) throws IOException {
        var message = map(cause, Throwable::getMessage);

        if (message != null) {
            response.setContentType(String.format(CONTENT_TYPE_FORMAT, TEXT_PLAIN, StandardCharsets.UTF_8));

            var textEncoder = new TextEncoder();

            textEncoder.write(message, response.getOutputStream());
        }
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
        if (!resource.handlerLists.isEmpty()) {
            var endpoint = new EndpointDescriptor(path);

            var keyCount = 0;

            var components = path.split("/");

            for (var j = 0; j < components.length; j++) {
                if (components[j].equals("?")) {
                    keyCount++;
                }
            }

            for (var entry : resource.handlerLists.entrySet()) {
                for (var handler : entry.getValue()) {
                    var operation = new OperationDescriptor(entry.getKey().toString(), handler);

                    operation.produces = describeType(handler.getGenericReturnType());

                    var parameters = handler.getParameters();

                    var n = parameters.length;

                    operation.parameters = n > 0;

                    if ((operation.method.equals("POST") || operation.method.equals("PUT")) && handler.getAnnotation(FormData.class) == null) {
                        n--;
                    }

                    for (var i = 0; i < parameters.length; i++) {
                        var parameter = parameters[i];

                        var parameterDescriptor = new ParameterDescriptor(parameter);

                        if (i < keyCount) {
                            parameterDescriptor.required = true;

                            operation.pathParameters.add(parameterDescriptor);
                        } else if (i < n) {
                            parameterDescriptor.required = parameter.getAnnotation(Required.class) != null;

                            operation.queryParameters.add(parameterDescriptor);
                        } else {
                            parameterDescriptor.required = true;

                            operation.bodyParameter = parameterDescriptor;
                        }

                        parameterDescriptor.type = describeType(parameter.getParameterizedType());
                    }

                    endpoint.operations.add(operation);
                }
            }

            serviceDescriptor.endpoints.add(endpoint);
        }

        for (var entry : resource.resources.entrySet()) {
            describeResource(String.format("%s/%s", path, entry.getKey()), entry.getValue());
        }
    }

    private TypeDescriptor describeType(Type type) {
        if (type instanceof Class<?>) {
            return describeRawType((Class<?>)type);
        } else if (type instanceof ParameterizedType parameterizedType) {
            var rawType = (Class<?>)parameterizedType.getRawType();
            var actualTypeArguments = parameterizedType.getActualTypeArguments();

            if (Iterable.class.isAssignableFrom(rawType)) {
                return new IterableTypeDescriptor(describeType(actualTypeArguments[0]));
            } else if (Map.class.isAssignableFrom(rawType)) {
                return new MapTypeDescriptor(describeType(actualTypeArguments[0]), describeType(actualTypeArguments[1]));
            } else {
                throw new IllegalArgumentException("Unsupported parameterized type.");
            }
        } else {
            throw new IllegalArgumentException("Unsupported type.");
        }
    }

    private TypeDescriptor describeRawType(Class<?> type) {
        if (type.isPrimitive()
            || type == Object.class
            || type == Boolean.class
            || Number.class.isAssignableFrom(type)
            || String.class.isAssignableFrom(type)
            || type == Void.class
            || Date.class.isAssignableFrom(type)
            || type == Instant.class
            || type == LocalDate.class
            || type == LocalTime.class
            || type == LocalDateTime.class
            || type == Duration.class
            || type == Period.class
            || type == UUID.class
            || type == URI.class
            || type == Path.class
            || type == Part.class
            || type == Document.class) {
            return new TypeDescriptor(type, true);
        } else if (type.isArray()) {
            return new IterableTypeDescriptor(describeRawType(type.getComponentType()));
        } else if (Iterable.class.isAssignableFrom(type)) {
            return new IterableTypeDescriptor(describeRawType(Object.class));
        } else if (Map.class.isAssignableFrom(type)) {
            return new MapTypeDescriptor(describeRawType(Object.class), describeRawType(Object.class));
        } else if (type.isEnum()) {
            return describeEnumeration(type);
        } else {
            return describeStructure(type);
        }
    }

    private TypeDescriptor describeEnumeration(Class<?> type) {
        if (!serviceDescriptor.enumerations.containsKey(type)) {
            var enumeration = new EnumerationDescriptor(type);

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

        return new TypeDescriptor(type, false);
    }

    private TypeDescriptor describeStructure(Class<?> type) {
        if (!serviceDescriptor.structures.containsKey(type)) {
            var structure = new StructureDescriptor(type);

            serviceDescriptor.structures.put(type, structure);

            if (type.isInterface()) {
                var interfaces = type.getInterfaces();

                for (var i = 0; i < interfaces.length; i++) {
                    structure.supertypes.add(describeRawType(interfaces[i]));
                }
            } else {
                var baseType = type.getSuperclass();

                if (baseType != Object.class && baseType != Record.class) {
                    structure.supertypes.add(describeRawType(baseType));
                }
            }

            for (var entry : BeanAdapter.getProperties(type).entrySet()) {
                var accessor = entry.getValue().getAccessor();

                if (accessor.getDeclaringClass() != type) {
                    continue;
                }

                var propertyDescriptor = new PropertyDescriptor(entry.getKey(), accessor);

                propertyDescriptor.type = describeType(accessor.getGenericReturnType());

                structure.properties.add(propertyDescriptor);
            }
        }

        return new TypeDescriptor(type, false);
    }

    private static String getTypeName(Class<?> type) {
        return type.getCanonicalName().substring(type.getPackageName().length() + 1);
    }
}
