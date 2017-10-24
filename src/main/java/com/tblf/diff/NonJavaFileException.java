package com.tblf.diff;

import java.io.IOException;

public class NonJavaFileException extends IOException {
    public NonJavaFileException(String s) {
        super(s);
    }
}
