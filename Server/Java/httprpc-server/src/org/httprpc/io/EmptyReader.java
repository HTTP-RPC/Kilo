package org.httprpc.io;

import java.io.Reader;

public class EmptyReader extends Reader {
    @Override
    public int read(char cbuf[], int off, int len) {
        return -1;
    }

    @Override
    public void reset() {
        // No-op
    }

    @Override
    public void close() {
        // No-op
    }
}
