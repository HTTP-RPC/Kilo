package org.httprpc.serialization.template;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class URLEscapeModifier implements Modifier {
    private static final String UTF_8_ENCODING = "UTF-8";

    @Override
    public Object apply(Object value, String argument) {
        String result;
        try {
            result = URLEncoder.encode(value.toString(), UTF_8_ENCODING);
        } catch (UnsupportedEncodingException exception) {
            throw new RuntimeException(exception);
        }

        return result;
    }
}
