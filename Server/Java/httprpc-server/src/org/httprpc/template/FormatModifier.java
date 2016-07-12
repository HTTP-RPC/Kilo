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

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Format modifier.
 */
public class FormatModifier implements Modifier {
    @Override
    public Object apply(Object value, String argument, Locale locale) {
        Object result;
        if (argument != null) {
            if (argument.equals("currency")) {
                result = NumberFormat.getCurrencyInstance(locale).format(value);
            } else if (argument.equals("percent")) {
                result = NumberFormat.getPercentInstance(locale).format(value);
            } else if (argument.equals("fullDate")) {
                result = DateFormat.getDateInstance(DateFormat.FULL, locale).format(new Date((Long)value));
            } else if (argument.equals("longDate")) {
                result = DateFormat.getDateInstance(DateFormat.LONG, locale).format(new Date((Long)value));
            } else if (argument.equals("mediumDate")) {
                result = DateFormat.getDateInstance(DateFormat.MEDIUM, locale).format(new Date((Long)value));
            } else if (argument.equals("shortDate")) {
                result = DateFormat.getDateInstance(DateFormat.SHORT, locale).format(new Date((Long)value));
            } else if (argument.equals("fullTime")) {
                result = DateFormat.getTimeInstance(DateFormat.FULL, locale).format(new Date((Long)value));
            } else if (argument.equals("longTime")) {
                result = DateFormat.getTimeInstance(DateFormat.LONG, locale).format(new Date((Long)value));
            } else if (argument.equals("mediumTime")) {
                result = DateFormat.getTimeInstance(DateFormat.MEDIUM, locale).format(new Date((Long)value));
            } else if (argument.equals("shortTime")) {
                result = DateFormat.getTimeInstance(DateFormat.SHORT, locale).format(new Date((Long)value));
            } else {
                result = String.format(locale, argument, value);
            }
        } else {
            result = value;
        }

        return result;
    }
}
