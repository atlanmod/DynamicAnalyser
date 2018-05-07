# MDE4RTS
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

## API: generating the model

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
analysisLauncher.applyBefore(A_Class::discoverModel);
```

This consumer method would be called before actually running the Instrumentation. It has to be a static method, that takes as a parameter the project to instrument as a java.io.File

```
analysisLauncher.applyAfter(Consumer<File> method);
```

Also exist, in order to run methods on the project once the Impact Analysis Model is generated.

## API: performing a Regression Test Selection

The `GitCaller` class exists for that purpose:

```
GitCaller gitCaller = new GitCaller(rootFolder, resourceSet);
gitCaller.compareCommits(firstRevision, lastRevision);

gitCaller.getImpactedTests(); //Returns all the test methods impacted by the changes induced by lastRevision
gitCaller.getNewTests(); //Returns all the new test methods added in lastRevision
```
The test cases gathered here can be executed using JUnit4 API, or the mvn Surefire plugin. 

[![Build Status](https://travis-ci.com/orichalque/dynamic-analyser.svg?token=xAKoZhwQpQtJ2iQvzzQ8&branch=travis)](https://travis-ci.com/orichalque/dynamic-analyser)
