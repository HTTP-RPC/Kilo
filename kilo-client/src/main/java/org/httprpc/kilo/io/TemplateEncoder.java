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

import org.httprpc.kilo.beans.BeanAdapter;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TimeZone;

import static org.httprpc.kilo.util.Collections.*;

/**
 * Applies a template document.
 */
public class TemplateEncoder extends Encoder<Object> {
    /**
     * Represents a modifier.
     */
    public interface Modifier {
        /**
         * Applies the modifier.
         *
         * @param value
         * The value to which the modifier is being be applied.
         *
         * @param argument
         * The modifier argument, or {@code null} if no argument was provided.
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

    // Markup modifier
    private static class MarkupModifier implements Modifier {
        @Override
        public Object apply(Object value, String argument, Locale locale, TimeZone timeZone) {
            if (value instanceof CharSequence text) {
                var stringBuilder = new StringBuilder();

                for (int i = 0, n = text.length(); i < n; i++) {
                    var c = text.charAt(i);

                    if (c == '<') {
                        stringBuilder.append("&lt;");
                    } else if (c == '>') {
                        stringBuilder.append("&gt;");
                    } else if (c == '&') {
                        stringBuilder.append("&amp;");
                    } else if (c == '"') {
                        stringBuilder.append("&quot;");
                    } else {
                        stringBuilder.append(c);
                    }
                }

                return stringBuilder.toString();
            } else {
                return value;
            }
        }
    }

    // Format modifier
    private static class FormatModifier implements Modifier {
        enum DateTimeType {
            DATE,
            TIME,
            DATE_TIME
        }

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

        @Override
        public Object apply(Object value, String argument, Locale locale, TimeZone timeZone) {
            if (argument == null) {
                return value;
            }

            return switch (argument) {
                case CURRENCY -> NumberFormat.getCurrencyInstance(locale).format(value);
                case PERCENT -> NumberFormat.getPercentInstance(locale).format(value);
                case SHORT_DATE -> format(value, DateTimeType.DATE, FormatStyle.SHORT, locale, timeZone);
                case MEDIUM_DATE -> format(value, DateTimeType.DATE, FormatStyle.MEDIUM, locale, timeZone);
                case LONG_DATE -> format(value, DateTimeType.DATE, FormatStyle.LONG, locale, timeZone);
                case FULL_DATE -> format(value, DateTimeType.DATE, FormatStyle.FULL, locale, timeZone);
                case ISO_DATE -> format(value, DateTimeType.DATE, null, null, timeZone);
                case SHORT_TIME -> format(value, DateTimeType.TIME, FormatStyle.SHORT, locale, timeZone);
                case MEDIUM_TIME -> format(value, DateTimeType.TIME, FormatStyle.MEDIUM, locale, timeZone);
                case LONG_TIME -> format(value, DateTimeType.TIME, FormatStyle.LONG, locale, timeZone);
                case FULL_TIME -> format(value, DateTimeType.TIME, FormatStyle.FULL, locale, timeZone);
                case ISO_TIME -> format(value, DateTimeType.TIME, null, null, timeZone);
                case SHORT_DATE_TIME -> format(value, DateTimeType.DATE_TIME, FormatStyle.SHORT, locale, timeZone);
                case MEDIUM_DATE_TIME -> format(value, DateTimeType.DATE_TIME, FormatStyle.MEDIUM, locale, timeZone);
                case LONG_DATE_TIME -> format(value, DateTimeType.DATE_TIME, FormatStyle.LONG, locale, timeZone);
                case FULL_DATE_TIME -> format(value, DateTimeType.DATE_TIME, FormatStyle.FULL, locale, timeZone);
                case ISO_DATE_TIME -> format(value, DateTimeType.DATE_TIME, null, null, timeZone);
                default -> String.format(locale, argument, value);
            };
        }

        static String format(Object value, DateTimeType dateTimeType, FormatStyle formatStyle, Locale locale, TimeZone timeZone) {
            if (value instanceof Number number) {
                value = new Date(number.longValue());
            }

            if (value instanceof Date date) {
                value = date.toInstant();
            }

            var zoneId = timeZone.toZoneId();

            var temporalAccessor = switch (value) {
                case Instant instant -> ZonedDateTime.ofInstant(instant, zoneId);
                case LocalDate localDate -> ZonedDateTime.of(LocalDateTime.of(localDate, LocalTime.MIDNIGHT), zoneId);
                case LocalTime localTime -> ZonedDateTime.of(LocalDateTime.of(LocalDate.now(), localTime), zoneId);
                case LocalDateTime localDateTime -> ZonedDateTime.of(localDateTime, zoneId);
                case null, default -> throw new UnsupportedOperationException("Value is not a temporal accessor.");
            };

            return switch (dateTimeType) {
                case DATE -> {
                    if (formatStyle != null) {
                        yield DateTimeFormatter.ofLocalizedDate(formatStyle).withLocale(locale).format(temporalAccessor);
                    } else {
                        yield DateTimeFormatter.ISO_OFFSET_DATE.format(temporalAccessor);
                    }
                }
                case TIME -> {
                    if (formatStyle != null) {
                        yield DateTimeFormatter.ofLocalizedTime(formatStyle).withLocale(locale).format(temporalAccessor);
                    } else {
                        yield DateTimeFormatter.ISO_OFFSET_TIME.format(temporalAccessor);
                    }
                }
                case DATE_TIME -> {
                    if (formatStyle != null) {
                        yield DateTimeFormatter.ofLocalizedDateTime(formatStyle).withLocale(locale).format(temporalAccessor);
                    } else {
                        yield DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(temporalAccessor);
                    }
                }
            };
        }
    }

    // Marker type enumeration
    private enum MarkerType {
        VARIABLE,
        CONDITIONAL_SECTION_START,
        REPEATING_SECTION_START,
        INVERTED_SECTION_START,
        SECTION_END,
        RESOURCE,
        INCLUDE,
        COMMENT
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
                    if (key.equals(KEY_REFERENCE)) {
                        return entry.getKey();
                    } else {
                        var value = entry.getValue();

                        if (value instanceof Map<?, ?> map) {
                            return map.get(key);
                        } else if (key.equals(SELF_REFERENCE)) {
                            return value;
                        } else {
                            return null;
                        }
                    }
                }

                @Override
                public boolean containsKey(Object key) {
                    if (key.equals(KEY_REFERENCE)) {
                        return true;
                    } else {
                        var value = entry.getValue();

                        if (value instanceof Map<?, ?> map) {
                            return map.containsKey(key);
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

    private Class<?> type;
    private String name;

    private Locale locale = Locale.getDefault();
    private TimeZone timeZone = TimeZone.getDefault();

    private ResourceBundle resourceBundle = null;

    private Modifier defaultModifier = new MarkupModifier();

    private Map<String, Modifier> modifiers = mapOf(
        entry("format", new FormatModifier())
    );

    private Deque<Map<?, ?>> dictionaries = new LinkedList<>();

    private Deque<String> sectionNames = new LinkedList<>();

    private static final int EOF = -1;

    private static final String KEY_REFERENCE = "~";
    private static final String SELF_REFERENCE = ".";

    /**
     * Constructs a new template encoder.
     *
     * @param type
     * The type used to resolve the template resource.
     *
     * @param name
     * The name of the template resource.
     */
    public TemplateEncoder(Class<?> type, String name) {
        if (type == null || name == null) {
            throw new IllegalArgumentException();
        }

        this.type = type;
        this.name = name;
    }

    /**
     * Returns the locale.
     *
     * @return
     * The locale.
     */
    public Locale getLocale() {
        return locale;
    }

    /**
     * Sets the locale.
     *
     * @param locale
     * The locale.
     */
    public void setLocale(Locale locale) {
        if (locale == null) {
            throw new IllegalArgumentException();
        }

        this.locale = locale;
    }

    /**
     * Returns the time zone.
     *
     * @return
     * The time zone.
     */
    public TimeZone getTimeZone() {
        return timeZone;
    }

    /**
     * Sets the time zone.
     *
     * @param timeZone
     * The time zone.
     */
    public void setTimeZone(TimeZone timeZone) {
        if (timeZone == null) {
            throw new IllegalArgumentException();
        }

        this.timeZone = timeZone;
    }

    /**
     * Returns the resource bundle.
     *
     * @return
     * The resource bundle, or {@code null} if a resource bundle has not been
     * set.
     */
    public ResourceBundle getResourceBundle() {
        return resourceBundle;
    }

    /**
     * Sets the resource bundle.
     *
     * @param resourceBundle
     * The resource bundle, or {@code null} for no resource bundle.
     */
    public void setResourceBundle(ResourceBundle resourceBundle) {
        this.resourceBundle = resourceBundle;
    }

    /**
     * Returns the default modifier.
     *
     * @return
     * The default modifier.
     */
    public Modifier getDefaultModifier() {
        return defaultModifier;
    }

    /**
     * Sets the default modifier.
     *
     * @param defaultModifier
     * The default modifier.
     */
    public void setDefaultModifier(Modifier defaultModifier) {
        if (defaultModifier == null) {
            throw new IllegalArgumentException();
        }

        this.defaultModifier = defaultModifier;
    }

    /**
     * Associates a custom modifier with the template encoder.
     *
     * @param name
     * The modifier name.
     *
     * @param modifier
     * The custom modifier.
     */
    public void bind(String name, Modifier modifier) {
        if (name == null || modifier == null) {
            throw new IllegalArgumentException();
        }

        modifiers.put(name, modifier);
    }

    @Override
    public void write(Object value, Writer writer) throws IOException {
        if (writer == null) {
            throw new IllegalArgumentException();
        }

        if (value != null) {
            try (var inputStream = type.getResourceAsStream(name)) {
                Reader reader = new PagedReader(new InputStreamReader(inputStream, getCharset()));

                writer = new BufferedWriter(writer);

                try {
                    encode(BeanAdapter.adapt(value), writer, reader);
                } finally {
                    writer.flush();
                }
            }
        }
    }

    private void encode(Object root, Writer writer, Reader reader) throws IOException {
        Map<?, ?> dictionary;
        if (root instanceof Map<?, ?> map) {
            dictionary = map;
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
                    } else if (c == '$') {
                        markerType = MarkerType.RESOURCE;
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

                    if (markerBuilder.isEmpty()) {
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
                                if (modifierNameBuilder.isEmpty()) {
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
                        case VARIABLE -> {
                            var value = getMarkerValue(marker);

                            if (value == null) {
                                break;
                            }

                            for (var modifierName : modifierNames) {
                                var modifier = modifiers.get(modifierName);

                                if (modifier == null) {
                                    throw new IOException("Invalid modifier.");
                                }

                                value = modifier.apply(value, modifierArguments.get(modifierName), locale, timeZone);
                            }

                            value = defaultModifier.apply(value, null, locale, timeZone);

                            writer.append(value.toString());
                        }
                        case CONDITIONAL_SECTION_START -> {
                            sectionNames.push(marker);

                            var value = getMarkerValue(marker);

                            if (value != null
                                && (!(value instanceof Iterable<?> iterable) || iterable.iterator().hasNext())
                                && (!(value instanceof Boolean flag) || flag)) {
                                encode(value, writer, reader);
                            } else {
                                encode(null, new NullWriter(), reader);
                            }

                            sectionNames.pop();

                        }
                        case REPEATING_SECTION_START -> {
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

                            var iterator = switch (value) {
                                case null -> java.util.Collections.emptyIterator();
                                case Iterable<?> iterable -> iterable.iterator();
                                case Map<?, ?> map -> new MapIterator(map);
                                default -> throw new IOException("Invalid section element.");
                            };

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

                                    encode(element, writer, reader);

                                    if (iterator.hasNext()) {
                                        reader.reset();
                                    }

                                    i++;
                                }
                            } else {
                                encode(null, new NullWriter(), reader);
                            }

                            sectionNames.pop();

                        }
                        case INVERTED_SECTION_START -> {
                            sectionNames.push(marker);

                            var value = getMarkerValue(marker);

                            if (value == null
                                || (value instanceof Iterable<?> iterable && !iterable.iterator().hasNext())
                                || (value instanceof Boolean flag && !flag)) {
                                encode(value, writer, reader);
                            } else {
                                encode(null, new NullWriter(), reader);
                            }

                            sectionNames.pop();

                        }
                        case SECTION_END -> {
                            if (!sectionNames.peek().equals(marker)) {
                                throw new IOException("Invalid closing section marker.");
                            }

                            dictionaries.pop();

                            return;
                        }
                        case RESOURCE -> {
                            if (resourceBundle == null) {
                                throw new IllegalStateException("Resource bundle not specified.");
                            }

                            var value = defaultModifier.apply(resourceBundle.getObject(marker), null, locale, timeZone);

                            writer.append(value.toString());
                        }
                        case INCLUDE -> {
                            if (root == null) {
                                break;
                            }

                            try (var inputStream = type.getResourceAsStream(marker)) {
                                encode(dictionary, writer, new PagedReader(new InputStreamReader(inputStream)));
                            }
                        }
                        case COMMENT -> {
                            // No-op
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
        var path = new LinkedList<>(Arrays.asList(name.split("/")));

        for (var dictionary : dictionaries) {
            if (!dictionary.containsKey(path.peek())) {
                continue;
            }

            return valueAt(dictionary, path);
        }

        return null;
    }

    private static Object valueAt(Map<?, ?> root, Deque<?> path) {
        var value = root.get(path.pop());

        if (path.isEmpty()) {
            return value;
        }

        if (value == null) {
            return null;
        } else if (value instanceof Map<?, ?> map) {
            return valueAt(map, path);
        } else {
            throw new IllegalArgumentException("Value is not a map.");
        }
    }
}
