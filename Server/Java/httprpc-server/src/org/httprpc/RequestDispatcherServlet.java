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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.Principal;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeMap;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.httprpc.beans.BeanAdapter;

/**
 * Servlet that dispatches HTTP-RPC web service requests.
 */
@MultipartConfig
public class RequestDispatcherServlet extends HttpServlet {
    private static final long serialVersionUID = 0;

    /**
     * Method descriptor.
     */
    public static final class MethodDescriptor {
        // Parameter descriptor list
        private static class ParameterDescriptorList extends AbstractList<ParameterDescriptor> {
            private Parameter[] parameters;
            private ResourceBundle resourceBundle;

            private ParameterDescriptorList(Parameter[] parameters, ResourceBundle resourceBundle) {
                this.parameters = parameters;
                this.resourceBundle = resourceBundle;
            }

            @Override
            public ParameterDescriptor get(int index) {
                return new ParameterDescriptor(parameters[index], resourceBundle);
            }

            @Override
            public int size() {
                return parameters.length;
            }
        }

        private Method method;
        private ResourceBundle resourceBundle;

        private MethodDescriptor(Method method, ResourceBundle resourceBundle) {
            this.method = method;
            this.resourceBundle = resourceBundle;
        }

        /**
         * Returns the method's name.
         *
         * @return
         * The name of the method.
         */
        public String getName() {
            return method.getName();
        }

        /**
         * Returns the method's description.
         *
         * @return
         * A localized description of the method.
         */
        public String getDescription() {
            String description = null;

            if (resourceBundle != null) {
                try {
                    description = resourceBundle.getString(method.getName());
                } catch (MissingResourceException exception) {
                    // No-op
                }
            }

            return description;
        }

        /**
         * Returns the method's parameters.
         *
         * @return
         * A list of the method's parameters.
         */
        public List<ParameterDescriptor> getParameters() {
            return new ParameterDescriptorList(method.getParameters(), resourceBundle);
        }

        /**
         * Returns the method's return type.
         *
         * @return
         * The type of the value returned by the method, or <tt>null</tt> if
         * the method does not return a value.
         */
        public String getReturns() {
            return getDescriptorType(method.getReturnType());
        }
    }

    /**
     * Parameter descriptor.
     */
    public static final class ParameterDescriptor {
        private Parameter parameter;
        private ResourceBundle resourceBundle;

        private ParameterDescriptor(Parameter parameter, ResourceBundle resourceBundle) {
            this.parameter = parameter;
            this.resourceBundle = resourceBundle;
        }

        /**
         * Returns the parameter's name.
         *
         * @return
         * The name of the parameter.
         */
        public String getName() {
            return parameter.getName();
        }

        /**
         * Returns the parameters's description.
         *
         * @return
         * A localized description of the parameters.
         */
        public String getDescription() {
            String description = null;

            if (resourceBundle != null) {
                Executable method = parameter.getDeclaringExecutable();

                try {
                    description = resourceBundle.getString(method.getName() + "_" + parameter.getName());
                } catch (MissingResourceException exception) {
                    // No-op
                }
            }

            return description;
        }

        /**
         * Returns the parameter's type.
         *
         * @return
         * The type of the parameter.
         */
        public String getType() {
            return getDescriptorType(parameter.getType());
        }
    }

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

    // Attachment collection
    private static class AttachmentCollection extends AbstractCollection<Attachment> {
        private HttpServletRequest request;

        public AttachmentCollection(HttpServletRequest request) {
            this.request = request;
        }

        @Override
        public int size() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Iterator<Attachment> iterator() {
            Iterator<Attachment> iterator;
            try {
                iterator = new Iterator<Attachment>() {
                    private Iterator<Part> parts = request.getParts().iterator();

                    @Override
                    public boolean hasNext() {
                        return parts.hasNext();
                    }

                    @Override
                    public Attachment next() {
                        if (!hasNext()) {
                            throw new NoSuchElementException();
                        }

                        return new Attachment() {
                            private Part part = parts.next();

                            @Override
                            public String getName() {
                                return part.getName();
                            }

                            @Override
                            public String getFileName() {
                                return part.getSubmittedFileName();
                            }

                            @Override
                            public String getContentType() {
                                return part.getContentType();
                            }

                            @Override
                            public long getSize() {
                                return part.getSize();
                            }

                            @Override
                            public InputStream getInputStream() throws IOException {
                                return part.getInputStream();
                            }
                        };
                    }
                };
            } catch (IOException | ServletException exception) {
                throw new RuntimeException(exception);
            }

            return iterator;
        }
    }

    private Class<?> serviceType = null;

    private TreeMap<String, Method> methodMap = new TreeMap<>();

    private HashMap<String, Method> templateMap = new HashMap<>();

    private static final String STRING_TYPE = "string";
    private static final String NUMBER_TYPE = "number";
    private static final String BOOLEAN_TYPE = "boolean";

    private static final String ARRAY_TYPE = "array";
    private static final String OBJECT_TYPE = "object";

    private static final String UNSUPPORTED_TYPE = "?";

    private static final String UTF_8_ENCODING = "UTF-8";

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

            if (serviceType.isAssignableFrom(method.getDeclaringClass())
                && !java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                if (methodMap.put(method.getName(), method) != null) {
                    throw new ServletException("Duplicate method name.");
                }

                Template[] templates = method.getAnnotationsByType(Template.class);

                for (int j = 0; j < templates.length; j++) {
                    if (templateMap.put(templates[j].value(), method) != null) {
                        throw new ServletException("Duplicate template name.");
                    }
                }
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        doPost(request, response);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String pathInfo = request.getPathInfo();

        if (pathInfo == null) {
            // Write method descriptor list
            ResourceBundle resourceBundle = null;

            try {
                resourceBundle = ResourceBundle.getBundle(serviceType.getName(), request.getLocale());
            } catch (MissingResourceException exception) {
                // No-op
            }

            LinkedList<MethodDescriptor> methodDescriptors = new LinkedList<>();

            for (Method method : methodMap.values()) {
                methodDescriptors.add(new MethodDescriptor(method, resourceBundle));
            }

            JSONSerializer serializer = new JSONSerializer();

            response.setContentType(serializer.getContentType());
            serializer.writeValue(response.getWriter(), BeanAdapter.adapt(methodDescriptors));
        } else {
            Locale locale = request.getLocale();

            // Look up service method
            pathInfo = pathInfo.substring(1);

            Method method = methodMap.get(pathInfo);

            Serializer serializer;
            if (method != null) {
                Class<?> returnType = method.getReturnType();

                if (returnType != Void.TYPE && returnType != Void.class) {
                    serializer = new JSONSerializer();
                } else {
                    serializer = null;
                }
            } else {
                method = templateMap.get(pathInfo);

                if (method == null) {
                    throw new ServletException("Method not found.");
                }

                String contentType = getServletContext().getMimeType(pathInfo);

                if (contentType == null) {
                    throw new ServletException("Unable to determine content type.");
                }

                serializer = new TemplateSerializer(serviceType, pathInfo, contentType, locale);
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
                                throw new ServletException("Invalid map entry.");
                            }

                            String key = URLDecoder.decode(entry[0], UTF_8_ENCODING);
                            String value = URLDecoder.decode(entry[1], UTF_8_ENCODING);

                            map.put(key, coerce(value, valueType));
                        }
                    } else {
                        map = Collections.EMPTY_MAP;
                    }

                    argument = map;
                } else {
                    argument = coerce(request.getParameter(name), type);
                }

                arguments[i] = argument;
            }

            // Execute method
            WebService service;
            try {
                service = (WebService)serviceType.newInstance();
            } catch (IllegalAccessException | InstantiationException exception) {
                throw new ServletException(exception);
            }

            service.setLocale(locale);

            Principal userPrincipal = request.getUserPrincipal();

            if (userPrincipal != null) {
                service.setUserName(userPrincipal.getName());
                service.setUserRoles(new UserRoleSet(request));
            }

            String contentType = request.getContentType();

            Iterable<Attachment> attachments;
            if (contentType != null && contentType.startsWith("multipart/form-data")) {
                attachments = new AttachmentCollection(request);
            } else {
                attachments = Collections.emptyList();
            }

            service.setAttachments(attachments);

            Object result;
            try {
                result = method.invoke(service, arguments);
            } catch (IllegalAccessException | InvocationTargetException exception) {
                throw new ServletException(exception);
            }

            // Write response
            if (serializer != null) {
                response.setContentType(serializer.getContentType());
                serializer.writeValue(response.getWriter(), result);
            }
        }
    }

    private static String getDescriptorType(Class<?> type) {
        String descriptorType;
        if (type == String.class) {
            descriptorType = STRING_TYPE;
        } else if (type == Byte.TYPE
            || type == Short.TYPE
            || type == Integer.TYPE
            || type == Float.TYPE
            || type == Double.TYPE
            || Number.class.isAssignableFrom(type)) {
            descriptorType = NUMBER_TYPE;
        } else if (type == Boolean.TYPE || type == Boolean.class) {
            descriptorType = BOOLEAN_TYPE;
        } else if (List.class.isAssignableFrom(type)) {
            descriptorType = ARRAY_TYPE;
        } else if (Map.class.isAssignableFrom(type)) {
            descriptorType = OBJECT_TYPE;
        } else if (type == Void.TYPE || type == Void.class) {
            descriptorType = null;
        } else {
            descriptorType = UNSUPPORTED_TYPE;
        }

        return descriptorType;
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
}

// Paged reader
class PagedReader extends Reader {
    private Reader reader;
    private int pageSize;

    private int position = 0;
    private int count = 0;

    private boolean endOfFile = false;

    private ArrayList<char[]> pages = new ArrayList<>();
    private LinkedList<Integer> marks = new LinkedList<>();

    private static int DEFAULT_PAGE_SIZE = 1024;
    private static int EOF = -1;

    public PagedReader(Reader reader) {
        this(reader, DEFAULT_PAGE_SIZE);
    }

    public PagedReader(Reader reader, int pageSize) {
        if (reader == null) {
            throw new IllegalArgumentException();
        }

        this.reader = reader;
        this.pageSize = pageSize;
    }

    @Override
    public int read() throws IOException {
        int c;
        if (position < count) {
            c = pages.get(position / pageSize)[position % pageSize];

            position++;
        } else if (!endOfFile) {
            c = reader.read();

            if (c != EOF) {
                if (position / pageSize == pages.size()) {
                    pages.add(new char[pageSize]);
                }

                pages.get(pages.size() - 1)[position % pageSize] = (char)c;

                position++;
                count++;
            } else {
                endOfFile = true;
            }
        } else {
            c = EOF;
        }

        return c;
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        int c = 0;
        int n = 0;

        for (int i = off; i < cbuf.length && n < len; i++) {
            c = read();

            if (c == EOF) {
                break;
            }

            cbuf[i] = (char)c;

            n++;
        }

        return (c == EOF && n == 0) ? EOF : n;
    }

    @Override
    public boolean ready() throws IOException {
        return (position < count) || reader.ready();
    }

    @Override
    public void mark(int readAheadLimit) {
        marks.push(position);
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    @Override
    public void reset() {
        if (marks.isEmpty()) {
            position = 0;
        } else {
            position = marks.pop();
        }
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }
}

// Empty reader
class EmptyReader extends Reader {
    @Override
    public int read(char cbuf[], int off, int len) {
        return -1;
    }

    @Override
    public void reset() {
        // No-op
    }

    @Override
    public void close() {
        // No-op
    }
}

// Null writer
class NullWriter extends Writer {
    @Override
    public void write(char[] cbuf, int off, int len) {
        // No-op
    }

    @Override
    public void flush() {
        // No-op
    }

    @Override
    public void close() {
        // No-op
    }
}

// Abstract base class for serializers
abstract class Serializer {
    public abstract String getContentType();
    public abstract void writeValue(PrintWriter writer, Object value) throws IOException;
}

// JSON serializer
class JSONSerializer extends Serializer {
    private int depth = 0;

    private static final String JSON_MIME_TYPE = "application/json";

    @Override
    public String getContentType() {
        return JSON_MIME_TYPE;
    }

    @Override
    public void writeValue(PrintWriter writer, Object value) throws IOException {
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

                    indent(writer);

                    writeValue(writer, element);

                    i++;
                }

                depth--;

                writer.append("\n");

                indent(writer);

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

                    indent(writer);

                    writer.append("\"" + key + "\": ");

                    writeValue(writer, entry.getValue());

                    i++;
                }

                depth--;

                writer.append("\n");

                indent(writer);

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

    private void indent(Writer writer) throws IOException {
        for (int i = 0; i < depth; i++) {
            writer.append("  ");
        }
    }
}

// Template serializer
class TemplateSerializer extends Serializer {
    private enum MarkerType {
        SECTION_START,
        SECTION_END,
        INCLUDE,
        RESOURCE,
        COMMENT,
        VARIABLE
    }

    private Class<?> serviceType;
    private String templateName;
    private String contentType;
    private Locale locale;

    private ResourceBundle resourceBundle = null;

    private Map<String, Reader> includes = new HashMap<>();
    private LinkedList<Map<String, Reader>> context = new LinkedList<>();

    private static HashMap<String, Modifier> modifiers = new HashMap<>();

    static {
        modifiers.put("format", new FormatModifier());
        modifiers.put("^url", new URLEscapeModifier());
        modifiers.put("^html", new MarkupEscapeModifier());
        modifiers.put("^xml", new MarkupEscapeModifier());
        modifiers.put("^json", new JSONEscapeModifier());
        modifiers.put("^csv", new CSVEscapeModifier());

        try (InputStream inputStream = TemplateSerializer.class.getResourceAsStream("/META-INF/httprpc/modifiers.properties")) {
            if (inputStream != null) {
                Properties mappings = new Properties();

                for (Map.Entry<Object, Object> mapping : mappings.entrySet()) {
                    String name = mapping.getKey().toString();

                    Class<?> type;
                    try {
                        type = Class.forName(mapping.getValue().toString());
                    } catch (ClassNotFoundException exception) {
                        throw new RuntimeException(exception);
                    }

                    if (type != null && Modifier.class.isAssignableFrom(type)) {
                        Modifier modifier;
                        try {
                            modifier = (Modifier)type.newInstance();
                        } catch (IllegalAccessException | InstantiationException exception) {
                            throw new RuntimeException(exception);
                        }

                        modifiers.put(name, modifier);
                    }
                }
            }
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    private static final int EOF = -1;

    public TemplateSerializer(Class<?> serviceType, String templateName, String contentType, Locale locale) {
        this.serviceType = serviceType;
        this.templateName = templateName;
        this.contentType = contentType;
        this.locale = locale;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public void writeValue(PrintWriter writer, Object value) throws IOException {
        if (value != null) {
            try (InputStream inputStream = serviceType.getResourceAsStream(templateName)) {
                if (inputStream == null) {
                    throw new IOException("Template not found.");
                }

                int i = templateName.lastIndexOf(".");

                if (i == -1) {
                    throw new IllegalStateException();
                }

                String baseName = serviceType.getPackage().getName() + "." + templateName.substring(0, i);

                try {
                    resourceBundle = ResourceBundle.getBundle(baseName, locale);
                } catch (MissingResourceException exception) {
                    // No-op
                }

                writeTemplate(writer, value, new PagedReader(new InputStreamReader(inputStream)));
            }
        }
    }

    private void writeTemplate(PrintWriter writer, Object root, Reader reader) throws IOException {
        if (writer.checkError()) {
            throw new IOException("Error writing to output stream.");
        }

        if (!(root instanceof Map<?, ?>)) {
            root = WebService.mapOf(WebService.entry(".", root));
        }

        Map<?, ?> dictionary = (Map<?, ?>)root;

        int c = reader.read();

        while (c != EOF) {
            if (c == '{') {
                c = reader.read();

                if (c == '{') {
                    c = reader.read();

                    MarkerType markerType;
                    if (c == '#') {
                        markerType = MarkerType.SECTION_START;
                    } else if (c == '/') {
                        markerType = MarkerType.SECTION_END;
                    } else if (c == '>') {
                        markerType = MarkerType.INCLUDE;
                    } else if (c == '@') {
                        markerType = MarkerType.RESOURCE;
                    } else if (c == '!') {
                        markerType = MarkerType.COMMENT;
                    } else {
                        markerType = MarkerType.VARIABLE;
                    }

                    if (markerType != MarkerType.VARIABLE) {
                        c = reader.read();
                    }

                    StringBuilder markerBuilder = new StringBuilder();

                    while (c != '}' && c != EOF) {
                        markerBuilder.append((char)c);

                        c = reader.read();
                    }

                    if (c == EOF) {
                        throw new IOException("Unexpected end of character stream.");
                    }

                    c = reader.read();

                    if (c != '}') {
                        throw new IOException("Improperly terminated marker.");
                    }

                    String marker = markerBuilder.toString();

                    if (marker.length() == 0) {
                        throw new IOException("Invalid marker.");
                    }

                    switch (markerType) {
                        case SECTION_START: {
                            context.push(includes);

                            Object value = dictionary.get(marker);

                            if (value == null) {
                                value = Collections.emptyList();
                            }

                            if (!(value instanceof List<?>)) {
                                throw new IOException("Invalid section element.");
                            }

                            List<?> list = (List<?>)value;

                            try {
                                Iterator<?> iterator = list.iterator();

                                if (iterator.hasNext()) {
                                    includes = new HashMap<>();

                                    while (iterator.hasNext()) {
                                        Object element = iterator.next();

                                        if (iterator.hasNext()) {
                                            reader.mark(0);
                                        }

                                        writeTemplate(writer, element, reader);

                                        if (iterator.hasNext()) {
                                            reader.reset();
                                        }
                                    }
                                } else {
                                    includes = new AbstractMap<String, Reader>() {
                                        @Override
                                        public Reader get(Object key) {
                                            return new EmptyReader();
                                        }

                                        @Override
                                        public Set<Entry<String, Reader>> entrySet() {
                                            throw new UnsupportedOperationException();
                                        }
                                    };

                                    writeTemplate(new PrintWriter(new NullWriter()), Collections.emptyMap(), reader);
                                }
                            } finally {
                                if (list instanceof AutoCloseable) {
                                    try {
                                        ((AutoCloseable)list).close();
                                    } catch (Exception exception) {
                                        throw new IOException(exception);
                                    }
                                }
                            }

                            includes = context.pop();

                            break;
                        }

                        case SECTION_END: {
                            // No-op
                            return;
                        }

                        case INCLUDE: {
                            Reader include = includes.get(marker);

                            if (include == null) {
                                try (InputStream inputStream = serviceType.getResourceAsStream(marker)) {
                                    if (inputStream == null) {
                                        throw new IOException("Include not found.");
                                    }

                                    include = new PagedReader(new InputStreamReader(inputStream));

                                    writeTemplate(writer, dictionary, include);

                                    includes.put(marker, include);
                                }
                            } else {
                                include.reset();

                                writeTemplate(writer, dictionary, include);
                            }

                            break;
                        }

                        case RESOURCE: {
                            String value;
                            if (resourceBundle != null) {
                                value = resourceBundle.getString(marker);
                            } else {
                                value = marker;
                            }

                            writer.append(value);

                            break;
                        }

                        case COMMENT: {
                            // No-op
                            break;
                        }

                        case VARIABLE: {
                            String[] components = marker.split(":");

                            String key = components[0];

                            Object value;
                            if (key.equals(".")) {
                                value = dictionary.get(key);
                            } else {
                                value = dictionary;

                                String[] path = key.split("\\.");

                                for (int i = 0; i < path.length; i++) {
                                    if (!(value instanceof Map<?, ?>)) {
                                        throw new IOException("Invalid path.");
                                    }

                                    value = ((Map<?, ?>)value).get(path[i]);

                                    if (value == null) {
                                        break;
                                    }
                                }
                            }

                            if (value != null) {
                                if (!(value instanceof String || value instanceof Number || value instanceof Boolean)) {
                                    throw new IOException("Invalid variable element.");
                                }

                                if (components.length > 1) {
                                    for (int i = 1; i < components.length; i++) {
                                        String component = components[i];

                                        int j = component.indexOf('=');

                                        String name, argument;
                                        if (j == -1) {
                                            name = component;
                                            argument = null;
                                        } else {
                                            name = component.substring(0, j);
                                            argument = component.substring(j + 1);
                                        }

                                        Modifier modifier = modifiers.get(name);

                                        if (modifier != null) {
                                            value = modifier.apply(value, argument);
                                        }
                                    }
                                }

                                writer.append(value.toString());
                            }

                            break;
                        }

                        default: {
                            throw new UnsupportedOperationException();
                        }
                    }
                } else {
                    writer.append('{');
                    writer.append((char)c);
                }
            } else {
                writer.append((char)c);
            }

            c = reader.read();
        }
    }
}

// Format modifier
class FormatModifier implements Modifier {
    @Override
    public Object apply(Object value, String argument) {
        Object result;
        if (argument != null) {
            if (argument.equals("currency")) {
                result = NumberFormat.getCurrencyInstance().format(value);
            } else if (argument.equals("percent")) {
                result = NumberFormat.getPercentInstance().format(value);
            } else if (argument.equals("fullDate")) {
                result = DateFormat.getDateInstance(DateFormat.FULL).format(new Date((Long)value));
            } else if (argument.equals("longDate")) {
                result = DateFormat.getDateInstance(DateFormat.LONG).format(new Date((Long)value));
            } else if (argument.equals("mediumDate")) {
                result = DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date((Long)value));
            } else if (argument.equals("shortDate")) {
                result = DateFormat.getDateInstance(DateFormat.SHORT).format(new Date((Long)value));
            } else if (argument.equals("fullTime")) {
                result = DateFormat.getTimeInstance(DateFormat.FULL).format(new Date((Long)value));
            } else if (argument.equals("longTime")) {
                result = DateFormat.getTimeInstance(DateFormat.LONG).format(new Date((Long)value));
            } else if (argument.equals("mediumTime")) {
                result = DateFormat.getTimeInstance(DateFormat.MEDIUM).format(new Date((Long)value));
            } else if (argument.equals("shortTime")) {
                result = DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date((Long)value));
            } else {
                result = String.format(argument, value);
            }
        } else {
            result = value;
        }

        return result;
    }
}

// URL escape modifier
class URLEscapeModifier implements Modifier {
    private static final String UTF_8_ENCODING = "UTF-8";

    @Override
    public Object apply(Object value, String argument) {
        String result;
        try {
            result = URLEncoder.encode(value.toString(), UTF_8_ENCODING);
        } catch (UnsupportedEncodingException exception) {
            throw new RuntimeException(exception);
        }

        return result;
    }
}

// Markup escape modifier
class MarkupEscapeModifier implements Modifier {
    @Override
    public Object apply(Object value, String argument) {
        StringBuilder resultBuilder = new StringBuilder();

        String string = value.toString();

        for (int i = 0, n = string.length(); i < n; i++) {
            char c = string.charAt(i);

            if (c == '<') {
                resultBuilder.append("&lt;");
            } else if (c == '>') {
                resultBuilder.append("&gt;");
            } else if (c == '&') {
                resultBuilder.append("&amp;");
            } else if (c == '"') {
                resultBuilder.append("&quot;");
            } else {
                resultBuilder.append(c);
            }
        }

        return resultBuilder.toString();
    }
}

// JSON escape modifier
class JSONEscapeModifier implements Modifier {
    @Override
    public Object apply(Object value, String argument) {
        StringBuilder resultBuilder = new StringBuilder();

        String string = value.toString();

        for (int i = 0, n = string.length(); i < n; i++) {
            char c = string.charAt(i);

            if (c == '"' || c == '\\') {
                resultBuilder.append("\\" + c);
            } else if (c == '\b') {
                resultBuilder.append("\\b");
            } else if (c == '\f') {
                resultBuilder.append("\\f");
            } else if (c == '\n') {
                resultBuilder.append("\\n");
            } else if (c == '\r') {
                resultBuilder.append("\\r");
            } else if (c == '\t') {
                resultBuilder.append("\\t");
            } else {
                resultBuilder.append(c);
            }
        }

        return resultBuilder.toString();
    }
}

// CSV escape modifier
class CSVEscapeModifier implements Modifier {
    @Override
    public Object apply(Object value, String argument) {
        StringBuilder resultBuilder = new StringBuilder();

        String string = value.toString();

        for (int i = 0, n = string.length(); i < n; i++) {
            char c = string.charAt(i);

            if (c == '"' || c == '\\') {
                resultBuilder.append("\\" + c);
            } else {
                resultBuilder.append(c);
            }
        }

        return resultBuilder.toString();
    }
}
