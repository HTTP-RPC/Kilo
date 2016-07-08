package org.httprpc.serialization.template;

public class CSVEscapeModifier implements Modifier {
    @Override
    public Object apply(Object value, String argument) {
        StringBuilder resultBuilder = new StringBuilder();

        String string = value.toString();

        for (int i = 0, n = string.length(); i < n; i++) {
            char c = string.charAt(i);

            if (c == '"' || c == '\\') {
                resultBuilder.append("\\" + c);
            } else {
                resultBuilder.append(c);
            }
        }

        return resultBuilder.toString();
    }
}
