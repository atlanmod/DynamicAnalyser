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



[![Build Status](https://travis-ci.com/orichalque/dynamic-analyser.svg?token=xAKoZhwQpQtJ2iQvzzQ8&branch=travis)](https://travis-ci.com/orichalque/dynamic-analyser)
