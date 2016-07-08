package org.httprpc.serialization.template;

public class JSONEscapeModifier implements Modifier {
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

