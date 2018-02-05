package com.tblf.utils;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Closer
        implements Closeable {
    private final List<Closeable> closeables = new ArrayList<Closeable>();

    // @Nullable is a JSR 305 annotation
    public <T extends Closeable> T add(final T closeable) {
        closeables.add(closeable);
        return closeable;
    }

    public void closeQuietly() {
        try {
            close();
        } catch (IOException ignored) {
        }
    }

    @Override
    public void close()
            throws IOException {
        IOException toThrow = null;
        final List<Closeable> l = new ArrayList<>(closeables);
        Collections.reverse(l);

        for (final Closeable closeable : l) {
            if (closeable == null)
                continue;
            try {
                closeable.close();
            } catch (IOException e) {
                if (toThrow == null)
                    toThrow = e;
            }
        }

        if (toThrow != null)
            throw toThrow;
    }

}