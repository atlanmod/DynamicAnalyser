package com.tblf.business;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class Main {
    public static void main(String[] args) throws IOException {
        Logger rootLogger = LogManager.getLogManager().getLogger("");
        rootLogger.setLevel(Level.FINE);
        for (Handler h : rootLogger.getHandlers()) {
            h.setLevel(Level.FINE);
        }

        File project = new File("/home/thibault/Documents/git/Dynamic-Analyser/diff/src/test/resources/fullprojects/SimpleProject");
        File script = new File("management/src/main/resources/modelbuilder.sh");

        if (!script.exists())
            throw new IOException("Script not found");

        String command = "sh "+script.getAbsolutePath()+" "+project.getAbsolutePath();
        System.out.println("Executing: "+command);
        try {
            Process process = Runtime.getRuntime().exec(command);

            StringBuffer output = new StringBuffer();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            System.out.println("### " + output);
        } catch (IOException e) {
            e.printStackTrace();
        }

        new AnalysisLauncher(project).run();
    }
}
