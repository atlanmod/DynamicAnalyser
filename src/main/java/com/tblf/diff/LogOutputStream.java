package com.tblf.diff;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Outputstream writing in the logs
 */
public class LogOutputStream extends OutputStream {
    /** The logger where to log the written bytes. */
    private Logger logger;

    /** The level. */
    private Level level;

    /** The internal memory for the written bytes. */
    private String mem;

    /**
     * Constructor for the LogOutputStream
     * @param logger a {@link Logger}
     * @param level the {@link Level}
     */
    public LogOutputStream(Logger logger, Level level) {
        this.logger = logger;
        this.level = level;
        mem = "";
    }

    @Override
    public void write (int b) {
        byte[] bytes = new byte[1];
        bytes[0] = (byte) (b & 0xff);
        mem = mem + new String(bytes);

        if (mem.endsWith ("\n")) {
            mem = mem.substring (0, mem.length () - 1);
            this.flush();
        }
    }

    @Override
    public void flush() {
        logger.log(level, mem);
        mem = "";
    }
}
