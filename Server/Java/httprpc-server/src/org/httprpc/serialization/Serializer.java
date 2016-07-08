package org.httprpc.serialization;

import java.io.IOException;
import java.io.PrintWriter;

public abstract class Serializer {
    public abstract String getContentType();
    public abstract void writeValue(PrintWriter writer, Object value) throws IOException;
}
