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

package org.httprpc.template;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import org.httprpc.Encoder;

/**
 * Template processing engine.
 */
public class TemplateEncoder implements Encoder {
    // Marker type enumeration
    private enum MarkerType {
        SECTION_START,
        SECTION_END,
        INCLUDE,
        COMMENT,
        VARIABLE
    }

    private URL url;
    private String contentType;
    private String baseName;

    private HashMap<String, Object> context = new HashMap<>();

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
    }

    private static final int EOF = -1;

    private static final String UTF_8_ENCODING = "UTF-8";

    private static final String CONTEXT_PREFIX = "$";
    private static final String RESOURCE_PREFIX = "@";

    /**
     * Constructs a new template engine.
     *
     * @param url
     * The URL of the template.
     *
     * @param contentType
     * The MIME type of the template.
     */
    public TemplateEncoder(URL url, String contentType) {
        this(url, contentType, null);
    }

    /**
     * Constructs a new template engine.
     *
     * @param url
     * The URL of the template.
     *
     * @param contentType
     * The MIME type of the template.
     *
     * @param baseName
     * The base name of the template's resource bundle.
     */
    public TemplateEncoder(URL url, String contentType, String baseName) {
        if (url == null) {
            throw new IllegalArgumentException();
        }

        if (contentType == null) {
            throw new IllegalArgumentException();
        }

        this.url = url;
        this.contentType = contentType;
        this.baseName = baseName;
    }

    /**
     * Returns the engine context.
     *
     * @return
     * The engine context.
     */
    public Map<String, Object> getContext() {
        return context;
    }

    @Override
    public String getContentType(Object value) {
        return String.format("%s;charset=%s", contentType, UTF_8_ENCODING);
    }

    @Override
    public void writeValue(Object value, OutputStream outputStream) throws IOException {
        writeValue(value, outputStream, Locale.getDefault());
    }

    /**
     * Writes a value to an output stream.
     *
     * @param value
     * The value to encode.
     *
     * @param outputStream
     * The output stream to write to.
     *
     * @param locale
     * The locale to use when writing the value.
     *
     * @throws IOException
     * If an exception occurs.
     */
    public void writeValue(Object value, OutputStream outputStream, Locale locale) throws IOException {
        Writer writer = new BufferedWriter(new OutputStreamWriter(outputStream, Charset.forName(UTF_8_ENCODING)));
        writeValue(value, writer, locale);

        writer.flush();
    }

    /**
     * Writes a value to a character stream.
     *
     * @param value
     * The value to encode.
     *
     * @param writer
     * The character stream to write to.
     *
     * @throws IOException
     * If an exception occurs.
     */
    public void writeValue(Object value, Writer writer) throws IOException {
        writeValue(value, writer, Locale.getDefault());
    }

    /**
     * Writes a value to a character stream.
     *
     * @param value
     * The value to encode.
     *
     * @param writer
     * The character stream to write to.
     *
     * @param locale
     * The locale to use when writing the value.
     *
     * @throws IOException
     * If an exception occurs.
     */
    public void writeValue(Object value, Writer writer, Locale locale) throws IOException {
        if (value != null) {
            try (InputStream inputStream = url.openStream()) {
                Reader reader = new PagedReader(new InputStreamReader(inputStream, Charset.forName(UTF_8_ENCODING)));

                writeRoot(value, writer, locale, reader);
            }
        }
    }

    private void writeRoot(Object root, Writer writer, Locale locale, Reader reader) throws IOException {
        Map<?, ?> dictionary;
        if (root instanceof Map<?, ?>) {
            dictionary = (Map<?, ?>)root;
        } else {
            dictionary = Collections.singletonMap(".", root);
        }

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

                                        writeRoot(element, writer, locale, reader);

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

                                    writeRoot(Collections.emptyMap(), new NullWriter(), locale, reader);
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
                                URL url = new URL(this.url, marker);

                                try (InputStream inputStream = url.openStream()) {
                                    include = new PagedReader(new InputStreamReader(inputStream));

                                    writeRoot(dictionary, writer, locale, include);

                                    includes.put(marker, include);
                                }
                            } else {
                                include.reset();

                                writeRoot(dictionary, writer, locale, include);
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
                            if (key.startsWith(CONTEXT_PREFIX)) {
                                value = context.get(key.substring(CONTEXT_PREFIX.length()));
                            } else if (key.startsWith(RESOURCE_PREFIX) && baseName != null) {
                                value = ResourceBundle.getBundle(baseName, locale).getString(key.substring(RESOURCE_PREFIX.length()));
                            } else if (key.equals(".")) {
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
                                            value = modifier.apply(value, argument, locale);
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

    /**
     * Returns the modifier map.
     *
     * @return
     * The modifier map.
     */
    public static Map<String, Modifier> getModifiers() {
        return modifiers;
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
