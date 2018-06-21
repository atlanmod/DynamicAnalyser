package com.tblf.business;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main {
    public static void main(String[] args) throws IOException {

        File project = new File(args[0]);
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

        new AnalysisLauncher(project).runImpactAnalysis();
    }
}
