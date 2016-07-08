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

package org.httprpc.serialization;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.AbstractMap;
import java.util.Collections;
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

import org.httprpc.io.EmptyReader;
import org.httprpc.io.NullWriter;
import org.httprpc.io.PagedReader;
import org.httprpc.serialization.template.CSVEscapeModifier;
import org.httprpc.serialization.template.FormatModifier;
import org.httprpc.serialization.template.JSONEscapeModifier;
import org.httprpc.serialization.template.MarkupEscapeModifier;
import org.httprpc.serialization.template.Modifier;
import org.httprpc.serialization.template.URLEscapeModifier;

/**
 * Serializer that writes a value using a template.
 */
public class TemplateSerializer implements Serializer {
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
