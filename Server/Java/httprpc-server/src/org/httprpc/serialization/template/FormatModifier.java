package org.httprpc.serialization.template;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Date;

public class FormatModifier implements Modifier {
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
