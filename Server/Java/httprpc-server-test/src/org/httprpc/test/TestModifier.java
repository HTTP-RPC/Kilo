package org.httprpc.test;

import org.httprpc.Modifier;

public class TestModifier implements Modifier {
    @Override
    public Object apply(Object value, String argument) {
        String result = value.toString();

        if (argument != null) {
            if (argument.equals("upper")) {
                result = result.toUpperCase();
            } else if (argument.equals("lower")) {
                result = result.toLowerCase();
            } else {
                throw new UnsupportedOperationException();
            }
        }

        return result;
    }
}
