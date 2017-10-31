package com.tblf.junitrunner;

import org.junit.runner.Result;

/**
 * Created by Thibault on 21/09/2017.
 */
public class RunnerUtils {

    public static String results(Result result) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Success: ");
        stringBuilder.append(result.getRunCount() - result.getFailureCount());
        stringBuilder.append("\n");
        stringBuilder.append("Failure: ");
        stringBuilder.append(result.getFailureCount());
        stringBuilder.append("\n");
        if (result.getFailureCount() > 0) {
            result.getFailures().forEach(failure -> {
                stringBuilder.append(failure.getException());
                stringBuilder.append("\n");
            });
        }

        return stringBuilder.toString();
    }
}
