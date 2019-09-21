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

package org.httprpc.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * Template encoder.
 */
public class TemplateEncoder {
    /**
     * Interface representing a modifier.
     */
    public interface Modifier {
        /**
         * Applies the modifier.
         *
         * @param value
         * The value to which the modifier is being be applied.
         *
         * @param argument
         * The modifier argument, or <tt>null</tt> if no argument was provided.
         *
         * @param locale
         * The locale in which the modifier is being applied.
         *
         * @return
         * The modified value.
         */
        public Object apply(Object value, String argument, Locale locale);
    }

    // Format modifier
    private static class FormatModifier implements Modifier {
        @Override
        public Object apply(Object value, String argument, Locale locale) {
            Object result;
            if (argument != null) {
                switch (argument) {
                    case "currency": {
                        result = NumberFormat.getCurrencyInstance(locale).format(value);

                        break;
                    }

                    case "percent": {
                        result = NumberFormat.getPercentInstance(locale).format(value);

                        break;
                    }

                    case "shortDate":
                    case "mediumDate":
                    case "longDate":
                    case "fullDate": {
                        if (value instanceof String) {
                            value = LocalDate.parse((String)value);
                        }

                        switch (argument) {
                            case "shortDate": {
                                if (value instanceof LocalDate) {
                                    result = ((LocalDate)value).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(locale));
                                } else {
                                    result = DateFormat.getDateInstance(DateFormat.SHORT, locale).format(value);
                                }

                                break;
                            }

                            case "mediumDate": {
                                if (value instanceof LocalDate) {
                                    result = ((LocalDate)value).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale));
                                } else {
                                    result = DateFormat.getDateInstance(DateFormat.MEDIUM, locale).format(value);
                                }

                                break;
                            }

                            case "longDate": {
                                if (value instanceof LocalDate) {
                                    result = ((LocalDate)value).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale));
                                } else {
                                    result = DateFormat.getDateInstance(DateFormat.LONG, locale).format(value);
                                }

                                break;
                            }

                            case "fullDate": {
                                if (value instanceof LocalDate) {
                                    result = ((LocalDate)value).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale));
                                } else {
                                    result = DateFormat.getDateInstance(DateFormat.FULL, locale).format(value);
                                }

                                break;
                            }

                            default: {
                                throw new RuntimeException();
                            }
                        }

                        break;
                    }

                    case "shortTime":
                    case "mediumTime":
                    case "longTime":
                    case "fullTime": {
                        if (value instanceof String) {
                            value = LocalTime.parse((String)value);
                        }

                        switch (argument) {
                            case "shortTime": {
                                if (value instanceof LocalTime) {
                                    result = ((LocalTime)value).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale));
                                } else {
                                    result = DateFormat.getTimeInstance(DateFormat.SHORT, locale).format(value);
                                }

                                break;
                            }

                            case "mediumTime": {
                                if (value instanceof LocalTime) {
                                    result = ((LocalTime)value).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM).withLocale(locale));
                                } else {
                                    result = DateFormat.getTimeInstance(DateFormat.MEDIUM, locale).format(value);
                                }

                                break;
                            }

                            case "longTime": {
                                if (value instanceof LocalTime) {
                                    result = ((LocalTime)value).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.LONG).withLocale(locale));
                                } else {
                                    result = DateFormat.getTimeInstance(DateFormat.LONG, locale).format(value);
                                }

                                break;
                            }

                            case "fullTime": {
                                if (value instanceof LocalTime) {
                                    result = ((LocalTime)value).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.FULL).withLocale(locale));
                                } else {
                                    result = DateFormat.getTimeInstance(DateFormat.FULL, locale).format(value);
                                }

                                break;
                            }

                            default: {
                                throw new RuntimeException();
                            }
                        }

                        break;
                    }

                    case "shortDateTime":
                    case "mediumDateTime":
                    case "longDateTime":
                    case "fullDateTime": {
                        if (value instanceof String) {
                            value = LocalDateTime.parse((String)value);
                        }

                        switch (argument) {
                            case "shortDateTime": {
                                if (value instanceof LocalDateTime) {
                                    result = ((LocalDateTime)value).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(locale));
                                } else {
                                    result = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale).format(value);
                                }

                                break;
                            }

                            case "mediumDateTime": {
                                if (value instanceof LocalDateTime) {
                                    result = ((LocalDateTime)value).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(locale));
                                } else {
                                    result = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, locale).format(value);
                                }

                                break;
                            }

                            case "longDateTime": {
                                if (value instanceof LocalDateTime) {
                                    result = ((LocalDateTime)value).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG).withLocale(locale));
                                } else {
                                    result = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, locale).format(value);
                                }

                                break;
                            }

                            case "fullDateTime": {
                                if (value instanceof LocalDateTime) {
                                    result = ((LocalDateTime)value).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL).withLocale(locale));
                                } else {
                                    result = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL, locale).format(value);
                                }

                                break;
                            }

                            default: {
                                throw new RuntimeException();
                            }
                        }

                        break;
                    }

                    default: {
                        result = String.format(locale, argument, value);

                        break;
                    }
                }
            } else {
                result = value;
            }

            return result;
        }
    }

    // URL escape modifier
    private static class URLEscapeModifier implements Modifier {
        @Override
        public Object apply(Object value, String argument, Locale locale) {
            String result;
            try {
                result = URLEncoder.encode(value.toString(), "UTF-8");
            } catch (UnsupportedEncodingException exception) {
                throw new RuntimeException(exception);
            }

            return result;
        }
    }

    // HTML escape modifier
    private static class HTMLEscapeModifier implements Modifier {
        @Override
        public Object apply(Object value, String argument, Locale locale) {
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

    // Marker type enumeration
    private enum MarkerType {
        SECTION_START,
        SECTION_END,
        INCLUDE,
        COMMENT,
        VARIABLE
    }

    private URL url;
    private Charset charset;

    private String baseName = null;
    private HashMap<String, Object> context = new HashMap<>();

    private Map<String, Reader> includes = new HashMap<>();
    private LinkedList<Map<String, Reader>> history = new LinkedList<>();

    private static HashMap<String, Modifier> modifiers = new HashMap<>();

    static {
        modifiers.put("format", new FormatModifier());
        modifiers.put("^html", new HTMLEscapeModifier());
        modifiers.put("^url", new URLEscapeModifier());
    }

    private static final int EOF = -1;

    private static final String RESOURCE_PREFIX = "@";
    private static final String CONTEXT_PREFIX = "$";

    /**
     * Constructs a new template encoder.
     *
     * @param url
     * The URL of the template.
     */
    public TemplateEncoder(URL url) {
        this(url, StandardCharsets.UTF_8);
    }

    /**
     * Constructs a new template encoder.
     *
     * @param url
     * The URL of the template.
     *
     * @param charset
     * The character encoding used by the template.
     */
    public TemplateEncoder(URL url, Charset charset) {
        if (url == null) {
            throw new IllegalArgumentException();
        }

        if (charset == null) {
            throw new IllegalArgumentException();
        }

        this.url = url;
        this.charset = charset;
    }

    /**
     * Returns the URL of the template.
     *
     * @return
     * The URL of the template.
     */
    public URL getURL() {
        return url;
    }

    /**
     * Returns the character encoding used by the encoder.
     *
     * @return
     * The character encoding used by the encoder.
     */
    public Charset getCharset() {
        return charset;
    }

    /**
     * Returns the base name of the template's resource bundle.
     *
     * @return
     * The base name of the template's resource bundle, or <tt>null</tt> if no
     * base name has been set.
     */
    public String getBaseName() {
        return baseName;
    }

    /**
     * Sets the base name of the template's resource bundle.
     *
     * @param baseName
     * The base name of the template's resource bundle, or <tt>null</tt> for no
     * base name.
     */
    public void setBaseName(String baseName) {
        this.baseName = baseName;
    }

    /**
     * Returns the template context.
     *
     * @return
     * The template context.
     */
    public Map<String, Object> getContext() {
        return context;
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
     * @throws IOException
     * If an exception occurs.
     */
    public void write(Object value, OutputStream outputStream) throws IOException {
        write(value, outputStream, Locale.getDefault());
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
    public void write(Object value, OutputStream outputStream, Locale locale) throws IOException {
        Writer writer = new OutputStreamWriter(outputStream, getCharset());

        write(value, writer, locale);

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
    public void write(Object value, Writer writer) throws IOException {
        write(value, writer, Locale.getDefault());
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
    public void write(Object value, Writer writer, Locale locale) throws IOException {
        if (value != null) {
            try (InputStream inputStream = url.openStream()) {
                Reader reader = new PagedReader(new InputStreamReader(inputStream, getCharset()));

                writer = new BufferedWriter(writer);

                writeRoot(value, writer, locale, reader);

                writer.flush();
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
                            String separator = null;

                            int n = marker.length();

                            if (marker.charAt(n - 1) == ']') {
                                int i = marker.lastIndexOf('[');

                                if (i != -1) {
                                    separator = marker.substring(i + 1, n - 1);

                                    marker = marker.substring(0, i);
                                }
                            }

                            history.push(includes);

                            Object value = dictionary.get(marker);

                            if (value == null) {
                                value = Collections.emptyList();
                            }

                            if (!(value instanceof Iterable<?>)) {
                                throw new IOException("Invalid section element.");
                            }

                            Iterator<?> iterator = ((Iterable<?>)value).iterator();

                            if (iterator.hasNext()) {
                                includes = new HashMap<>();

                                int i = 0;

                                while (iterator.hasNext()) {
                                    Object element = iterator.next();

                                    if (iterator.hasNext()) {
                                        reader.mark(0);
                                    }

                                    if (i > 0 && separator != null) {
                                        writer.append(separator);
                                    }

                                    writeRoot(element, writer, locale, reader);

                                    if (iterator.hasNext()) {
                                        reader.reset();
                                    }

                                    i++;
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
                            if (key.startsWith(RESOURCE_PREFIX)) {
                                if (baseName != null) {
                                    ResourceBundle resourceBundle = ResourceBundle.getBundle(baseName, locale);

                                    value = resourceBundle.getString(key.substring(RESOURCE_PREFIX.length()));
                                } else {
                                    value = null;
                                }
                            } else if (key.startsWith(CONTEXT_PREFIX)) {
                                value = context.get(key.substring(CONTEXT_PREFIX.length()));
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
    public int read(char[] cbuf, int off, int len) {
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
