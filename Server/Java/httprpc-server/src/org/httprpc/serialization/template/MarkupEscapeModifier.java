package org.httprpc.serialization.template;

public class MarkupEscapeModifier implements Modifier {
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
