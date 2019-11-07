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

import org.httprpc.beans.BeanAdapter;

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
import java.util.TimeZone;

/**
 * Template encoder.
 */
public class TemplateEncoder extends Encoder<Object> {
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
         * The locale for which the modifier is being applied.
         *
         * @param timeZone
         * The time zone for which the modifier is being applied.
         *
         * @return
         * The modified value.
         */
        Object apply(Object value, String argument, Locale locale, TimeZone timeZone);
    }

    // Format modifier
    private static class FormatModifier implements Modifier {
        static final String CURRENCY = "currency";
        static final String PERCENT = "percent";

        static final String SHORT_DATE = "shortDate";
        static final String MEDIUM_DATE = "mediumDate";
        static final String LONG_DATE = "longDate";
        static final String FULL_DATE = "fullDate";

        static final String SHORT_TIME = "shortTime";
        static final String MEDIUM_TIME = "mediumTime";
        static final String LONG_TIME = "longTime";
        static final String FULL_TIME = "fullTime";

        static final String SHORT_DATE_TIME = "shortDateTime";
        static final String MEDIUM_DATE_TIME = "mediumDateTime";
        static final String LONG_DATE_TIME = "longDateTime";
        static final String FULL_DATE_TIME = "fullDateTime";

        @Override
        public Object apply(Object value, String argument, Locale locale, TimeZone timeZone) {
            if (argument == null) {
                return value;
            }

            Object result;
            switch (argument) {
                case CURRENCY: {
                    result = NumberFormat.getCurrencyInstance(locale).format(value);

                    break;
                }

                case PERCENT: {
                    result = NumberFormat.getPercentInstance(locale).format(value);

                    break;
                }

                case SHORT_DATE:
                case MEDIUM_DATE:
                case LONG_DATE:
                case FULL_DATE: {
                    if (value instanceof String) {
                        value = LocalDate.parse((String)value);
                    }

                    switch (argument) {
                        case SHORT_DATE: {
                            if (value instanceof LocalDate) {
                                result = ((LocalDate)value).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(locale));
                            } else {
                                result = getDateInstance(DateFormat.SHORT, locale, timeZone).format(value);
                            }

                            break;
                        }

                        case MEDIUM_DATE: {
                            if (value instanceof LocalDate) {
                                result = ((LocalDate)value).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale));
                            } else {
                                result = getDateInstance(DateFormat.MEDIUM, locale, timeZone).format(value);
                            }

                            break;
                        }

                        case LONG_DATE: {
                            if (value instanceof LocalDate) {
                                result = ((LocalDate)value).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale));
                            } else {
                                result = getDateInstance(DateFormat.LONG, locale, timeZone).format(value);
                            }

                            break;
                        }

                        case FULL_DATE: {
                            if (value instanceof LocalDate) {
                                result = ((LocalDate)value).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale));
                            } else {
                                result = getDateInstance(DateFormat.FULL, locale, timeZone).format(value);
                            }

                            break;
                        }

                        default: {
                            throw new UnsupportedOperationException();
                        }
                    }

                    break;
                }

                case SHORT_TIME:
                case MEDIUM_TIME:
                case LONG_TIME:
                case FULL_TIME: {
                    if (value instanceof String) {
                        value = LocalTime.parse((String)value);
                    }

                    switch (argument) {
                        case SHORT_TIME: {
                            if (value instanceof LocalTime) {
                                result = ((LocalTime)value).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale));
                            } else {
                                result = getTimeInstance(DateFormat.SHORT, locale, timeZone).format(value);
                            }

                            break;
                        }

                        case MEDIUM_TIME: {
                            if (value instanceof LocalTime) {
                                result = ((LocalTime)value).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM).withLocale(locale));
                            } else {
                                result = getTimeInstance(DateFormat.MEDIUM, locale, timeZone).format(value);
                            }

                            break;
                        }

                        case LONG_TIME: {
                            if (value instanceof LocalTime) {
                                result = ((LocalTime)value).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.LONG).withLocale(locale));
                            } else {
                                result = getTimeInstance(DateFormat.LONG, locale, timeZone).format(value);
                            }

                            break;
                        }

                        case FULL_TIME: {
                            if (value instanceof LocalTime) {
                                result = ((LocalTime)value).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.FULL).withLocale(locale));
                            } else {
                                result = getTimeInstance(DateFormat.FULL, locale, timeZone).format(value);
                            }

                            break;
                        }

                        default: {
                            throw new UnsupportedOperationException();
                        }
                    }

                    break;
                }

                case SHORT_DATE_TIME:
                case MEDIUM_DATE_TIME:
                case LONG_DATE_TIME:
                case FULL_DATE_TIME: {
                    if (value instanceof String) {
                        value = LocalDateTime.parse((String)value);
                    }

                    switch (argument) {
                        case SHORT_DATE_TIME: {
                            if (value instanceof LocalDateTime) {
                                result = ((LocalDateTime)value).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(locale));
                            } else {
                                result = getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale, timeZone).format(value);
                            }

                            break;
                        }

                        case MEDIUM_DATE_TIME: {
                            if (value instanceof LocalDateTime) {
                                result = ((LocalDateTime)value).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(locale));
                            } else {
                                result = getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, locale, timeZone).format(value);
                            }

                            break;
                        }

                        case LONG_DATE_TIME: {
                            if (value instanceof LocalDateTime) {
                                result = ((LocalDateTime)value).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG).withLocale(locale));
                            } else {
                                result = getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, locale, timeZone).format(value);
                            }

                            break;
                        }

                        case FULL_DATE_TIME: {
                            if (value instanceof LocalDateTime) {
                                result = ((LocalDateTime)value).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL).withLocale(locale));
                            } else {
                                result = getDateTimeInstance(DateFormat.FULL, DateFormat.FULL, locale, timeZone).format(value);
                            }

                            break;
                        }

                        default: {
                            throw new UnsupportedOperationException();
                        }
                    }

                    break;
                }

                default: {
                    result = String.format(locale, argument, value);

                    break;
                }
            }

            return result;
        }

        static DateFormat getDateInstance(int style, Locale locale, TimeZone timeZone) {
            DateFormat dateFormat = DateFormat.getDateInstance(style, locale);

            dateFormat.setTimeZone(timeZone);

            return dateFormat;
        }

        static DateFormat getTimeInstance(int style, Locale locale, TimeZone timeZone) {
            DateFormat dateFormat = DateFormat.getTimeInstance(style, locale);

            dateFormat.setTimeZone(timeZone);

            return dateFormat;
        }

        static DateFormat getDateTimeInstance(int dateStyle, int timeStyle, Locale locale, TimeZone timeZone) {
            DateFormat dateFormat = DateFormat.getDateTimeInstance(dateStyle, timeStyle, locale);

            dateFormat.setTimeZone(timeZone);

            return dateFormat;
        }
    }

    // URL escape modifier
    private static class URLEscapeModifier implements Modifier {
        @Override
        public Object apply(Object value, String argument, Locale locale, TimeZone timeZone) {
            String result;
            try {
                result = URLEncoder.encode(value.toString(), "UTF-8");
            } catch (UnsupportedEncodingException exception) {
                throw new RuntimeException(exception);
            }

            return result;
        }
    }

    // JSON escape modifier
    private static class JSONEscapeModifier implements Modifier {
        @Override
        public Object apply(Object value, String argument, Locale locale, TimeZone timeZone) {
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
    private static class CSVEscapeModifier implements TemplateEncoder.Modifier {
        @Override
        public Object apply(Object value, String argument, Locale locale, TimeZone timeZone) {
            StringBuilder resultBuilder = new StringBuilder();

            String string = value.toString();

            for (int i = 0, n = string.length(); i < n; i++) {
                char c = string.charAt(i);

                if (c == '"') {
                    resultBuilder.append(c);
                }

                resultBuilder.append(c);
            }

            return resultBuilder.toString();
        }
    }

    // Markup escape modifier
    private static class MarkupEscapeModifier implements Modifier {
        @Override
        public Object apply(Object value, String argument, Locale locale, TimeZone timeZone) {
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
    private Map<String, ?> context = Collections.emptyMap();

    private Map<String, Reader> includes = new HashMap<>();
    private LinkedList<Map<String, Reader>> history = new LinkedList<>();

    private static HashMap<String, Modifier> modifiers = new HashMap<>();

    static {
        modifiers.put("format", new FormatModifier());

        modifiers.put("^url", new URLEscapeModifier());

        modifiers.put("^json", new JSONEscapeModifier());
        modifiers.put("^csv", new CSVEscapeModifier());

        MarkupEscapeModifier markupEscapeModifier = new MarkupEscapeModifier();

        modifiers.put("^html", markupEscapeModifier);
        modifiers.put("^xml", markupEscapeModifier);
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
        super(charset);

        if (url == null) {
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
    public Map<String, ?> getContext() {
        return context;
    }

    /**
     * Sets the template context.
     *
     * @param context
     * The template context.
     */
    public void setContext(Map<String, ?> context) {
        if (context == null) {
            throw new IllegalArgumentException();
        }

        this.context = context;
    }

    @Override
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
        write(value, outputStream, locale, TimeZone.getDefault());
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
     * @param timeZone
     * The time zone to use when writing the value.
     *
     * @throws IOException
     * If an exception occurs.
     */
    public void write(Object value, OutputStream outputStream, Locale locale, TimeZone timeZone) throws IOException {
        if (outputStream == null) {
            throw new IllegalArgumentException();
        }

        Writer writer = new OutputStreamWriter(outputStream, getCharset());

        write(value, writer, locale, timeZone);

        writer.flush();
    }

    @Override
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
        write(value, writer, locale, TimeZone.getDefault());
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
     * @param timeZone
     * The time zone to use when writing the value.
     *
     * @throws IOException
     * If an exception occurs.
     */
    public void write(Object value, Writer writer, Locale locale, TimeZone timeZone) throws IOException {
        if (writer == null) {
            throw new IllegalArgumentException();
        }

        if (locale == null) {
            throw new IllegalArgumentException();
        }

        if (timeZone == null) {
            throw new IllegalArgumentException();
        }

        if (value != null) {
            try (InputStream inputStream = url.openStream()) {
                Reader reader = new PagedReader(new InputStreamReader(inputStream, getCharset()));

                writer = new BufferedWriter(writer);

                writeRoot(value, writer, locale, timeZone, reader);

                writer.flush();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void writeRoot(Object root, Writer writer, Locale locale, TimeZone timeZone, Reader reader) throws IOException {
        Map<String, ?> dictionary;
        if (root instanceof Map<?, ?>) {
            dictionary = (Map<String, ?>)root;
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

                                    writeRoot(element, writer, locale, timeZone, reader);

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

                                writeRoot(Collections.emptyMap(), new NullWriter(), locale, timeZone, reader);
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

                                    writeRoot(dictionary, writer, locale, timeZone, include);

                                    includes.put(marker, include);
                                }
                            } else {
                                include.reset();

                                writeRoot(dictionary, writer, locale, timeZone, include);
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
                                if (baseName != null) {
                                    ResourceBundle resourceBundle = ResourceBundle.getBundle(baseName, locale);

                                    value = resourceBundle.getString(key.substring(RESOURCE_PREFIX.length()));
                                } else {
                                    value = null;
                                }
                            } else if (key.startsWith(CONTEXT_PREFIX)) {
                                value = BeanAdapter.valueAt(context, key.substring(CONTEXT_PREFIX.length()));
                            } else {
                                value = BeanAdapter.valueAt(dictionary, key);
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
                                            value = modifier.apply(value, argument, locale, timeZone);
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
