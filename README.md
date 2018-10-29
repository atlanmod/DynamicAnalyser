# Dynamic Analyser

This Framework proposes a high level approach for instrumenting and running a program, in order to gather dynamic informations.
It is able to instrument both source-code and byte-code with existing frameworks, proposes a set of *tracers* that are used to gather execution traces, and works at the model level with EMF, and the Mondo project's Hawk framework.

## Running the analysis

The `AnalysisLauncher` is used towards that purpose. This class configures the analysis, and runs it.

Several points can be specified here:

- First, the `processor` that will be used for instrumenting the code. This processor can be either an ASM ClassVisitor, or a Spoon AbstractProcessor. The type of the processor defines the instrumentation used. Several processors can be specified. The processor have to use a Tracer, if a deeper analysis is wanted.

- Second, the Trace type. Running the code will write a trace, that can be either a regular trace in a file, or in a queue, or to a MQTT broker for analysing on a different machine.

- Third, the parsing behavior. This class has to extend the `ParsingBehavior` class, and override the `parse()` method. This class will be used once the program's execution is done, in order to analyse the trace, and define a behavior to do with it.

- Fourth, `before` and `after` Consumers. Those are anonymous classes that are optional, but can be called before instrumenting the program, and after running it, if more computations are required.

Examples are available in the test suites, [here](https://github.com/atlanmod/DynamicAnalyser/blob/7d9e6aba19fc9782c5dd5e3ac25a2e92b18c3de1/management/src/test/java/com/tblf/business/AnalysisLauncherTest.java#L192) and [here](https://github.com/atlanmod/DynamicAnalyser/blob/7d9e6aba19fc9782c5dd5e3ac25a2e92b18c3de1/management/src/test/java/com/tblf/business/AnalysisLauncherTest.java#L237)
