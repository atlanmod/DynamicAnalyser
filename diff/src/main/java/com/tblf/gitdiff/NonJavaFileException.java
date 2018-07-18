package com.tblf.gitdiff;

import java.io.IOException;

public class NonJavaFileException extends IOException {
    public NonJavaFileException(String s) {
        super(s);
        this.setStackTrace(new StackTraceElement[]{});
    }
}
