# Dynamic Analyser

#### Table of Contents
  * [Model driven dynamic impact analysis](#model-driven-dynamic-impact-analysis)
    + [Discovery](#discovery)
    + [Instrumentation](#instrumentation)
    + [Execution](#execution)
    + [Parsing](#parsing)
  * [API](#api)
    + [Generating the model](#generating-the-model)
      - [Instrumentation](#instrumentation-1)
      - [Runner](#runner)
      - [Parsing](#parsing-1)
    + [Performing a Regression Test Selection](#performing-a-regression-test-selection)
  * [Model](#model)
    + [MetaModel](#metamodel)
    
## Model driven dynamic impact analysis

This application generates a model of the dynamic aspects of a program.
This works using several steps: 

### Discovery 

The MoDisco framework generates a static model of a SourceCode. This step creates a model of the Abstract Syntax Tree (AST) of the source code, in a bi-directionnal model transformation. This model is then used for Impact Analysis purposes

### Instrumentation 

Either Source Code (SCI) or Byte code (BCI) instrumentations are available. SCI tends to be more accurate, but slower.

### Execution

Running the instrumented code generates an execution trace of this program's execution.

### Parsing 

Parsing the previously generated trace would enhance the modisco static model with dynamic informations

## API

### Generating the model

This phase is rather long, and is meant to be computed beforehand, more specifically in a context of Continuous Integration
In order to generate a model of the Java Project containined in the directory `directory`:

```
AnalysisLauncher analysisLauncher = new AnalysisLauncher(directory);
analysisLauncher.setInstrumentationType(InstrumentationType.SOURCECODE);
analysisLauncher.run();
```
For portability purposes, MoDisco is not included in the project. You have to generate the model by hand before anything. 
Consumer methods are available for that purpose:

```
analysisLauncher.applyBefore(Consumer<File> method);
```

This consumer method would be called before actually running the Instrumentation. It has to be a static method, that takes as a parameter the project to instrument as a java.io.File. 

A consumer method to call after the impact analysis is also available: 

```
analysisLauncher.applyAfter(Consumer<File> method);
```

This allows the user to run methods on the project once the Impact Analysis Model is generated.

#### Instrumentation

The module `instrumentation` contains the code used to instrument the code. The `InstrumenterBuilder` creates the `Instrumenter` used, regarding the parameters given:  

```
new InstrumenterBuilder()
.withSourceCodeInstrumenter()
.onDirectory(directory)
.withQueueExecutionTrace()
.withOutputDirectory(output)
.build() //Returns an Instrumenter, either SCI or BCI
.run(); //Instruments the given code
```

This previous sample instruments the entire code of a given directory. The compiled classes can then be loaded, and executed to generate traces. 

#### Runner 

The runner mainly used works using Maven. The `-noverify` option is given, in order to ignore the ByteCode modifications induced by the Instrumentation. The Instrumented code (either after a BCI, or the compiled instrumented Source code) is classloaded over the standard classes, and the following goal is run: 

`mvn -Djacoco.skip -DtestFailureIgnore=true surefire:test`

Hence, the instrumented code is used to run all the test cases, and the execution trace is generated. 

#### Parsing

The module `parsing` contains the code used to parse the previously generated execution traces. The current version of the code only computes the traces written in a single file. A faster version is coming soon, using fast messaging queues instead.

```
Resource r; //The Model conforming to the Metamodel defined in the Model module
r = new TraceParser(File theFile, File theOutputModel.xmi, ResourceSet moDiscoModel).parse();
```

This code would generate a XMI model representing the execution of the program. We use this model for RTS here, but the usage of MDE offers a easy reusability, for other purposes. 

### Performing a Regression Test Selection

The `GitCaller` class exists for that purpose. It takes the Version Control System folder (The one containing the .git folder), but also the ResourceSet containing the Impact Analysis Model. 

```
GitCaller gitCaller = new GitCaller(rootFolder, resourceSet);
gitCaller.compareCommits(firstRevision, lastRevision);

gitCaller.getImpactedTests(); //Returns all the test methods impacted by the changes induced by lastRevision
gitCaller.getNewTests(); //Returns all the new test methods added in lastRevision
```
The test cases gathered here can be executed using JUnit4 API, or the mvn Surefire plugin. 

## Model 

### MetaModel 

The Meta-model is available in the Model package. It has been generated for NeoEMF, but is compatible with standard XMI format. This meta-model is really simple, and represents the link between two elements in the MoDisco model (e.g. between a Statement and a Test Method for instance.) 

Examples coming soon. 



