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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLEncoder;
import java.security.Principal;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

/**
 * Servlet that dispatches HTTP-RPC web service requests.
 */
@MultipartConfig
public class RequestDispatcherServlet extends HttpServlet {
    private static final long serialVersionUID = 0;

    // Resource structure
    private static class Resource {
        public final HashMap<String, LinkedList<Method>> handlerMap = new HashMap<>();
        public final HashMap<String, Resource> resources = new HashMap<>();
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

    private Class<?> serviceType = null;
    private Resource root = null;

    @Override
    public void init() throws ServletException {
        // Load service class
        String serviceClassName = getServletConfig().getInitParameter("serviceClassName");

        try {
            serviceType = Class.forName(serviceClassName);
        } catch (ClassNotFoundException exception) {
            throw new ServletException(exception);
        }

        if (!WebService.class.isAssignableFrom(serviceType)) {
            throw new ServletException("Invalid service type.");
        }

        // Populate resource tree
        root = new Resource();

        Method[] methods = serviceType.getMethods();

        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];

            RPC rpc = method.getAnnotation(RPC.class);

            if (rpc != null) {
                Resource resource = root;

                String[] components = rpc.path().split("/");

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

                String key = rpc.method().toLowerCase();

                LinkedList<Method> handlerList = resource.handlerMap.get(key);

                if (handlerList == null) {
                    handlerList = new LinkedList<>();

                    resource.handlerMap.put(key, handlerList);
                }

                handlerList.add(method);
            }
        }
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Look up resource
        Resource resource = root;

        String pathInfo = request.getPathInfo();

        if (pathInfo != null) {
            String[] components = pathInfo.split("/");

            for (int j = 0; j < components.length; j++) {
                String component = components[j];

                if (component.length() == 0) {
                    continue;
                }

                resource = resource.resources.get(component);

                if (resource == null) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
            }
        }

        LinkedList<Method> handlerList = resource.handlerMap.get(request.getMethod().toLowerCase());

        if (handlerList == null) {
            response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        // Set character encoding
        if (request.getCharacterEncoding() == null) {
            request.setCharacterEncoding("UTF-8");
        }

        // Populate parameter map
        HashMap<String, LinkedList<String>> parameterMap = new HashMap<>();

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

        // Populate file map
        HashMap<String, LinkedList<File>> fileMap = new HashMap<>();

        String contentType = request.getContentType();

        if (contentType != null && contentType.startsWith("multipart/form-data")) {
            for (Part part : request.getParts()) {
                String submittedFileName = part.getSubmittedFileName();

                if (submittedFileName == null || submittedFileName.length() == 0) {
                    continue;
                }

                String name = part.getName();

                LinkedList<File> fileList = fileMap.get(name);

                if (fileList == null) {
                    fileList = new LinkedList<>();
                    fileMap.put(name, fileList);
                }

                File file = File.createTempFile(part.getName(), "_" + part.getSubmittedFileName());
                part.write(file.getAbsolutePath());

                fileList.add(file);
            }
        }

        // Invoke handler method
        Method method = getMethod(handlerList, parameterMap, fileMap);

        try {
            Object result;
            try {
                // TODO Import Modifier?
                WebService service;
                if (!java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                    try {
                        service = (WebService)serviceType.newInstance();
                    } catch (IllegalAccessException | InstantiationException exception) {
                        throw new RuntimeException(exception);
                    }

                    service.setLocale(request.getLocale());

                    Principal userPrincipal = request.getUserPrincipal();

                    if (userPrincipal != null) {
                        service.setUserName(userPrincipal.getName());
                        service.setUserRoles(new UserRoleSet(request));
                    }
                } else {
                    service = null;
                }

                try {
                    result = method.invoke(service, getArguments(method, parameterMap, fileMap));
                } catch (IllegalAccessException | InvocationTargetException exception) {
                    throw new RuntimeException(exception);
                }
            } catch (RuntimeException exception) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }

            // Write response
            Class<?> returnType = method.getReturnType();

            if (returnType == Void.TYPE || returnType == Void.class) {
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            } else {
                JSONSerializer serializer = new JSONSerializer();

                response.setCharacterEncoding("UTF-8");
                response.setContentType(serializer.getContentType());

                serializer.writeValue(response.getWriter(), result);
            }
        } finally {
            // Delete files
            for (LinkedList<File> fileList : fileMap.values()) {
                for (File file : fileList) {
                    file.delete();
                }
            }
        }
    }

    private static Method getMethod(LinkedList<Method> handlerList, HashMap<String, LinkedList<String>> parameterMap,
        HashMap<String, LinkedList<File>> fileMap) {
        Method method = null;

        int n = -1;

        for (Method handler : handlerList) {
            Parameter[] parameters = handler.getParameters();

            int count = 0;

            for (int i = 0; i < parameters.length; i++) {
                String name = parameters[i].getName();

                if (parameterMap.containsKey(name) || fileMap.containsKey(name)) {
                    count++;
                }
            }

            if (count > n) {
                n = count;

                method = handler;
            }
        }

        return method;
    }

    private static Object[] getArguments(Method method, HashMap<String, LinkedList<String>> parameterMap,
        HashMap<String, LinkedList<File>> fileMap) throws IOException {
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

                List<Object> list;
                if (elementType == URL.class) {
                    LinkedList<File> fileList = fileMap.get(name);

                    if (fileList != null) {
                        list = new ArrayList<>(fileList.size());

                        for (File file : fileList) {
                            list.add(file.toURI().toURL());
                        }
                    } else {
                        list = Collections.emptyList();
                    }
                } else {
                    LinkedList<String> valueList = parameterMap.get(name);

                    if (valueList != null) {
                        int n = valueList.size();

                        list = new ArrayList<>(n);

                        for (String value : valueList) {
                            list.add(getArgument(value, elementType));
                        }
                    } else {
                        list = Collections.emptyList();
                    }
                }

                argument = list;
            } else {
                if (type == URL.class) {
                    LinkedList<File> fileList = fileMap.get(name);

                    if (fileList != null) {
                        argument = fileList.getFirst().toURI().toURL();
                    } else {
                        argument = null;
                    }
                } else {
                    LinkedList<String> valueList = parameterMap.get(name);

                    String value;
                    if (valueList != null) {
                        value = valueList.getFirst();
                    } else {
                        value = null;
                    }

                    argument = getArgument(value, type);
                }
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
        } else {
            throw new UnsupportedOperationException("Invalid parameter type.");
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

    @Override
    public String getContentType() {
        return "application/json";
    }

    @Override
    public void writeValue(PrintWriter writer, Object value) throws IOException {
        if (writer.checkError()) {
            throw new IOException("Error writing to output stream.");
        }

        if (value == null) {
            writer.append(null);
        } else if (value instanceof CharSequence) {
            CharSequence string = (CharSequence)value;

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
        COMMENT,
        VARIABLE
    }

    private Class<?> serviceType;
    private String templateName;
    private String contentType;
    private Locale locale;
    private Map<String, Object> context;

    private ResourceBundle resourceBundle = null;

    private Map<String, Reader> includes = new HashMap<>();
    private LinkedList<Map<String, Reader>> history = new LinkedList<>();

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

    private static final String RESOURCE_PREFIX = "@";
    private static final String CONTEXT_PREFIX = "$";

    public TemplateSerializer(Class<?> serviceType, String templateName, String contentType, Locale locale, Map<String, Object> context) {
        this.serviceType = serviceType;
        this.templateName = templateName;
        this.contentType = contentType;
        this.locale = locale;
        this.context = context;
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
            root = Collections.singletonMap(".", root);
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
                            history.push(includes);

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

                            includes = history.pop();

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
                            } else if (key.startsWith(RESOURCE_PREFIX)) {
                                key = key.substring(RESOURCE_PREFIX.length());

                                if (resourceBundle != null) {
                                    try {
                                        value = resourceBundle.getString(key);
                                    } catch (MissingResourceException exception) {
                                        value = null;
                                    }
                                } else {
                                    value = null;
                                }

                                if (value == null) {
                                    value = key;
                                }

                            } else if (key.startsWith(CONTEXT_PREFIX)) {
                                key = key.substring(CONTEXT_PREFIX.length());

                                value = context.get(key);

                                if (value == null) {
                                    value = key;
                                }
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
