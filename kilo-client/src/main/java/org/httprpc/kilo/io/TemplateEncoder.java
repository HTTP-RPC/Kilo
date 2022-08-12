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

package org.httprpc.kilo.io;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.TemporalAccessor;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import static org.httprpc.kilo.util.Collections.entry;
import static org.httprpc.kilo.util.Collections.mapOf;

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
         * The modifier argument, or <code>null</code> if no argument was provided.
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
        static final String ISO_DATE = "isoDate";

        static final String SHORT_TIME = "shortTime";
        static final String MEDIUM_TIME = "mediumTime";
        static final String LONG_TIME = "longTime";
        static final String FULL_TIME = "fullTime";
        static final String ISO_TIME = "isoTime";

        static final String SHORT_DATE_TIME = "shortDateTime";
        static final String MEDIUM_DATE_TIME = "mediumDateTime";
        static final String LONG_DATE_TIME = "longDateTime";
        static final String FULL_DATE_TIME = "fullDateTime";
        static final String ISO_DATE_TIME = "isoDateTime";

        enum DateTimeType {
            DATE,
            TIME,
            DATE_TIME
        }

        @Override
        public Object apply(Object value, String argument, Locale locale, TimeZone timeZone) {
            if (argument == null) {
                return value;
            }

            switch (argument) {
                case CURRENCY: {
                    return NumberFormat.getCurrencyInstance(locale).format(value);
                }

                case PERCENT: {
                    return NumberFormat.getPercentInstance(locale).format(value);
                }

                case SHORT_DATE: {
                    return format(value, DateTimeType.DATE, FormatStyle.SHORT, locale, timeZone);
                }

                case MEDIUM_DATE: {
                    return format(value, DateTimeType.DATE, FormatStyle.MEDIUM, locale, timeZone);
                }

                case LONG_DATE: {
                    return format(value, DateTimeType.DATE, FormatStyle.LONG, locale, timeZone);
                }

                case FULL_DATE: {
                    return format(value, DateTimeType.DATE, FormatStyle.FULL, locale, timeZone);
                }

                case ISO_DATE: {
                    return format(value, DateTimeType.DATE, null, null, timeZone);
                }

                case SHORT_TIME: {
                    return format(value, DateTimeType.TIME, FormatStyle.SHORT, locale, timeZone);
                }

                case MEDIUM_TIME: {
                    return format(value, DateTimeType.TIME, FormatStyle.MEDIUM, locale, timeZone);
                }

                case LONG_TIME: {
                    return format(value, DateTimeType.TIME, FormatStyle.LONG, locale, timeZone);
                }

                case FULL_TIME: {
                    return format(value, DateTimeType.TIME, FormatStyle.FULL, locale, timeZone);
                }

                case ISO_TIME: {
                    return format(value, DateTimeType.TIME, null, null, timeZone);
                }

                case SHORT_DATE_TIME: {
                    return format(value, DateTimeType.DATE_TIME, FormatStyle.SHORT, locale, timeZone);
                }

                case MEDIUM_DATE_TIME: {
                    return format(value, DateTimeType.DATE_TIME, FormatStyle.MEDIUM, locale, timeZone);
                }

                case LONG_DATE_TIME: {
                    return format(value, DateTimeType.DATE_TIME, FormatStyle.LONG, locale, timeZone);
                }

                case FULL_DATE_TIME: {
                    return format(value, DateTimeType.DATE_TIME, FormatStyle.FULL, locale, timeZone);
                }

                case ISO_DATE_TIME: {
                    return format(value, DateTimeType.DATE_TIME, null, null, timeZone);
                }

                default: {
                    return String.format(locale, argument, value);
                }
            }
        }

        static String format(Object value, DateTimeType dateTimeType, FormatStyle formatStyle, Locale locale, TimeZone timeZone) {
            if (value instanceof Number) {
                value = new Date(((Number)value).longValue());
            }

            if (value instanceof Date) {
                value = ((Date)value).toInstant();
            }

            if (value instanceof Instant) {
                value = ZonedDateTime.ofInstant((Instant)value, timeZone.toZoneId());
            }

            var temporalAccessor = (TemporalAccessor)value;

            switch (dateTimeType) {
                case DATE: {
                    if (formatStyle != null) {
                        return DateTimeFormatter.ofLocalizedDate(formatStyle).withLocale(locale).format(temporalAccessor);
                    } else {
                        return DateTimeFormatter.ISO_OFFSET_DATE.format(ZonedDateTime.from(temporalAccessor));
                    }
                }

                case TIME: {
                    if (formatStyle != null) {
                        return DateTimeFormatter.ofLocalizedTime(formatStyle).withLocale(locale).format(temporalAccessor);
                    } else {
                        return DateTimeFormatter.ISO_OFFSET_TIME.format(ZonedDateTime.from(temporalAccessor));
                    }
                }

                case DATE_TIME: {
                    if (formatStyle != null) {
                        return DateTimeFormatter.ofLocalizedDateTime(formatStyle).withLocale(locale).format(temporalAccessor);
                    } else {
                        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.from(temporalAccessor));
                    }
                }

                default: {
                    throw new UnsupportedOperationException();
                }
            }
        }
    }

    // URL escape modifier
    private static class URLEscapeModifier implements Modifier {
        @Override
        public Object apply(Object value, String argument, Locale locale, TimeZone timeZone) {
            return URLEncoder.encode(value.toString(), StandardCharsets.UTF_8);
        }
    }

    // JSON escape modifier
    private static class JSONEscapeModifier implements Modifier {
        @Override
        public Object apply(Object value, String argument, Locale locale, TimeZone timeZone) {
            if (value instanceof CharSequence) {
                var string = (CharSequence)value;

                var resultBuilder = new StringBuilder();

                for (int i = 0, n = string.length(); i < n; i++) {
                    var c = string.charAt(i);

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
            } else {
                return value;
            }
        }
    }

    // CSV escape modifier
    private static class CSVEscapeModifier implements Modifier {
        @Override
        public Object apply(Object value, String argument, Locale locale, TimeZone timeZone) {
            if (value instanceof CharSequence) {
                var string = (CharSequence)value;

                var resultBuilder = new StringBuilder();

                resultBuilder.append('"');

                for (int i = 0, n = string.length(); i < n; i++) {
                    var c = string.charAt(i);

                    if (c == '"') {
                        resultBuilder.append(c);
                    }

                    resultBuilder.append(c);
                }

                resultBuilder.append('"');

                return resultBuilder.toString();
            } else {
                return value;
            }
        }
    }

    // Markup escape modifier
    private static class MarkupEscapeModifier implements Modifier {
        @Override
        public Object apply(Object value, String argument, Locale locale, TimeZone timeZone) {
            if (value instanceof CharSequence) {
                var string = (CharSequence)value;

                var resultBuilder = new StringBuilder();

                for (int i = 0, n = string.length(); i < n; i++) {
                    var c = string.charAt(i);

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
            } else {
                return value;
            }
        }
    }

    // Marker type enumeration
    private enum MarkerType {
        CONDITIONAL_SECTION_START,
        REPEATING_SECTION_START,
        INVERTED_SECTION_START,
        SECTION_END,
        INCLUDE,
        COMMENT,
        VARIABLE
    }

    // Map iterator
    private static class MapIterator implements Iterator<Map<Object, Object>> {
        Iterator<? extends Map.Entry<?, ?>> iterator;

        MapIterator(Map<?, ?> map) {
            iterator = map.entrySet().iterator();
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public Map<Object, Object> next() {
            return new AbstractMap<>() {
                Entry<?, ?> entry = iterator.next();

                @Override
                public Object get(Object key) {
                    if (key == null) {
                        throw new IllegalArgumentException();
                    }

                    if (key.equals(KEY_REFERENCE)) {
                        return entry.getKey();
                    } else {
                        var value = entry.getValue();

                        if (value instanceof Map<?, ?>) {
                            return ((Map<?, ?>) value).get(key);
                        } else if (key.equals(SELF_REFERENCE)) {
                            return value;
                        } else {
                            return null;
                        }
                    }
                }

                @Override
                public boolean containsKey(Object key) {
                    if (key == null) {
                        throw new IllegalArgumentException();
                    }

                    if (key.equals(KEY_REFERENCE)) {
                        return true;
                    } else {
                        var value = entry.getValue();

                        if (value instanceof Map<?, ?>) {
                            return ((Map<?, ?>) value).containsKey(key);
                        } else {
                            return key.equals(SELF_REFERENCE);
                        }
                    }
                }

                @Override
                public Set<Entry<Object, Object>> entrySet() {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }

    private URL url;
    private Charset charset;
    private Map<String, Modifier> modifiers;
    private Modifier defaultEscapeModifier;

    private Deque<Map<?, ?>> dictionaries = new LinkedList<>();

    private Deque<String> sectionNames = new LinkedList<>();

    private static Map<String, Modifier> defaultModifiers = new HashMap<>();

    private static final int EOF = -1;

    private static final String KEY_REFERENCE = "~";
    private static final String SELF_REFERENCE = ".";

    static {
        defaultModifiers.put("format", new FormatModifier());

        defaultModifiers.put("url", new URLEscapeModifier());
        defaultModifiers.put("json", new JSONEscapeModifier());
        defaultModifiers.put("csv", new CSVEscapeModifier());

        var markupEscapeModifier = new MarkupEscapeModifier();

        defaultModifiers.put("xml", markupEscapeModifier);
        defaultModifiers.put("html", markupEscapeModifier);
    }

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

        modifiers = new HashMap<>(defaultModifiers);

        var path = url.getPath();

        var i = path.lastIndexOf('.');

        if (i != -1) {
            defaultEscapeModifier = modifiers.get(path.substring(i + 1));
        }
    }

    /**
     * Returns the modifier map.
     *
     * @return
     * The modifier map.
     */
    public Map<String, Modifier> getModifiers() {
        return modifiers;
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
            try (var inputStream = url.openStream()) {
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
            dictionary = mapOf(
                entry(SELF_REFERENCE, root)
            );
        }

        dictionaries.push(dictionary);

        var c = reader.read();

        while (c != EOF) {
            if (c == '{') {
                c = reader.read();

                if (c == EOF) {
                    continue;
                }

                if (c == '{') {
                    c = reader.read();

                    MarkerType markerType;
                    if (c == '?') {
                        markerType = MarkerType.CONDITIONAL_SECTION_START;
                    } else if (c == '#') {
                        markerType = MarkerType.REPEATING_SECTION_START;
                    } else if (c == '^') {
                        markerType = MarkerType.INVERTED_SECTION_START;
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

                    var markerBuilder = new StringBuilder();

                    while (c != ':' && c != '}' && c != EOF) {
                        markerBuilder.append((char)c);

                        c = reader.read();
                    }

                    if (markerBuilder.length() == 0) {
                        throw new IOException("Invalid marker.");
                    }

                    var marker = markerBuilder.toString();

                    Deque<String> modifierNames = new LinkedList<>();
                    Map<String, String> modifierArguments = new HashMap<>();

                    if (c == ':') {
                        var modifierNameBuilder = new StringBuilder();
                        var modifierArgumentBuilder = new StringBuilder();

                        var argument = false;

                        while (c != EOF) {
                            c = reader.read();

                            if (c == EOF) {
                                continue;
                            }

                            if (c == ':' || c == '}') {
                                if (modifierNameBuilder.length() == 0) {
                                    throw new IOException("Invalid modifier name.");
                                }

                                var modifierName = modifierNameBuilder.toString();

                                modifierNames.add(modifierName);
                                modifierArguments.put(modifierName, modifierArgumentBuilder.toString());

                                modifierNameBuilder.setLength(0);
                                modifierArgumentBuilder.setLength(0);

                                argument = false;

                                if (c == '}') {
                                    break;
                                }
                            } else if (c == '=') {
                                argument = true;
                            } else if (argument) {
                                modifierArgumentBuilder.append((char)c);
                            } else {
                                modifierNameBuilder.append((char)c);
                            }
                        }
                    }

                    if (c == EOF) {
                        throw new IOException("Unexpected end of character stream.");
                    }

                    c = reader.read();

                    if (c != '}') {
                        throw new IOException("Improperly terminated marker.");
                    }

                    switch (markerType) {
                        case CONDITIONAL_SECTION_START: {
                            sectionNames.push(marker);

                            var value = getMarkerValue(marker);

                            if (value != null
                                && (!(value instanceof Boolean) || ((Boolean)value))
                                && (!(value instanceof Iterable<?>) || ((Iterable<?>)value).iterator().hasNext())) {
                                writeRoot(value, writer, locale, timeZone, reader);
                            } else {
                                writeRoot(null, new NullWriter(), locale, timeZone, reader);
                            }

                            sectionNames.pop();

                            break;
                        }

                        case REPEATING_SECTION_START: {
                            String separator = null;

                            var n = marker.length();

                            if (marker.charAt(n - 1) == ']') {
                                var i = marker.lastIndexOf('[');

                                if (i != -1) {
                                    separator = marker.substring(i + 1, n - 1);

                                    marker = marker.substring(0, i);
                                }
                            }

                            sectionNames.push(marker);

                            var value = getMarkerValue(marker);

                            Iterator<?> iterator;
                            if (value == null) {
                                iterator = Collections.emptyIterator();
                            } else if (value instanceof Iterable<?>) {
                                iterator = ((Iterable<?>)value).iterator();
                            } else if (value instanceof Map<?, ?>) {
                                iterator = new MapIterator((Map<?, ?>)value);
                            } else {
                                throw new IOException("Invalid section element.");
                            }

                            if (iterator.hasNext()) {
                                var i = 0;

                                while (iterator.hasNext()) {
                                    var element = iterator.next();

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
                                writeRoot(null, new NullWriter(), locale, timeZone, reader);
                            }

                            sectionNames.pop();

                            break;
                        }

                        case INVERTED_SECTION_START: {
                            sectionNames.push(marker);

                            var value = getMarkerValue(marker);

                            if (value == null
                                || (value instanceof Boolean && !((Boolean)value))
                                || (value instanceof Iterable<?> && !((Iterable<?>)value).iterator().hasNext())) {
                                writeRoot(value, writer, locale, timeZone, reader);
                            } else {
                                writeRoot(null, new NullWriter(), locale, timeZone, reader);
                            }

                            sectionNames.pop();

                            break;
                        }

                        case SECTION_END: {
                            if (!sectionNames.peek().equals(marker)) {
                                throw new IOException("Invalid closing section marker.");
                            }

                            dictionaries.pop();

                            return;
                        }

                        case INCLUDE: {
                            if (root != null) {
                                var url = new URL(this.url, marker);

                                try (var inputStream = url.openStream()) {
                                    writeRoot(dictionary, writer, locale, timeZone, new PagedReader(new InputStreamReader(inputStream)));
                                }
                            }

                            break;
                        }

                        case COMMENT: {
                            // No-op
                            break;
                        }

                        case VARIABLE: {
                            var value = getMarkerValue(marker);

                            if (value != null) {
                                for (var modifierName : modifierNames) {
                                    var modifier = modifiers.get(modifierName);

                                    if (modifier != null) {
                                        value = modifier.apply(value, modifierArguments.get(modifierName), locale, timeZone);
                                    }
                                }

                                if (defaultEscapeModifier != null) {
                                    value = defaultEscapeModifier.apply(value, null, locale, timeZone);
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

        dictionaries.pop();
    }

    private Object getMarkerValue(String name) {
        List<String> path = new LinkedList<>(Arrays.asList(name.split("/")));

        for (var dictionary : dictionaries) {
            if (!dictionary.containsKey(path.get(0))) {
                continue;
            }

            return valueAt(dictionary, path);
        }

        return null;
    }

    private static Object valueAt(Map<?, ?> root, List<?> path) {
        var value = root.get(path.remove(0));

        if (path.isEmpty()) {
            return value;
        }

        if (!(value instanceof Map<?, ?>)) {
            throw new IllegalArgumentException("Value is not a map.");
        }

        return valueAt((Map<?, ?>)value, path);
    }

    /**
     * Returns the default modifier map.
     *
     * @return
     * The default modifier map.
     */
    public static Map<String, Modifier> getDefaultModifiers() {
        return defaultModifiers;
    }
}
