package com.tblf.parsing.traceReaders;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simplest reader, reading a single execution trace file using a {@link BufferedReader}
 */
public class TraceFileReader extends TraceReader {
    private static Logger LOGGER = Logger.getLogger(TraceFileReader.class.getName());
    private BufferedReader bufferedReader;

    public TraceFileReader() {
    }

    /**
     * Constructor initializing the reader
     * @param file
     */
    public TraceFileReader(File file) {
        setFile(file);
    }

    @Override
    public void setFile(File file) {
        super.setFile(file);

        try {
            if(!file.exists())
                file.createNewFile();

            bufferedReader = new BufferedReader(new FileReader(file));
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not read trace "+file.getAbsolutePath(), e);
        }
    }

    @Override
    public String read() {
        try {
            return bufferedReader.readLine();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not read line", e);
        }
        return null;
    }
}
