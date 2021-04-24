# **B**uild **O**n **T**est for Kotlin

## Introduction

**B**uild **O**n **T**est is a light framework for scripting in your favourite
type safe language, which is Kotlin in this case. There is also variants in 
Swift and Typescript.

The idea is to write scripts with your favourite type safe language that is 
well supported by the IDE, wrap it as a test method and launch it from the 
IDE through the unit test framework.

This project contains a simple build framework and some utilities for logging, 
working with file, text, and process, ... etc., to get you started.

With proper IDE support, you can easily modify, launch and see the result
instantly. Take advantage of all the coding and debugging support of the
IDE. Enjoy coding in a modern, safe and expressive language as well as the
quick modify / run cycle of a scripting language.

**NOTE** The sample builder assume pwd is the builder project directory. 
In IdeaIC, you may have to set the `Working directory` in the launch 
configuration to `$MODULE_WORKING_DIR$` in order to get the builder
working properly.

**NOTE** There is a catch. You almost always want to launch a single
test method (ie. script) at a time. However, the unit test framework may
more than happy to launch all the test methods in a class or a project when
you accidentally hit a wrong button. In many IDE, the workaround is to
annotate the test method with @Ignore so that it would only run when you
invoke the test explicitly. Example:

```
@Ignore
@Test
fun clean() {
    project.dir.file("generated").deleteRecursively()
}
```
It is also possible to ensure only single test is run through JUnit test rule. 
See the `bot-build/BuilderBase.singleTestChecker()` class rule for an example.

**NOTE** This is not a full-fledged build system or for developing standalone
command line applications. It is used for custom adhoc tasks that is not in 
the standard build system workflow and inside an IDE with proper language and 
unit test support.

**NOTE** The project has only been tested to work with IdeaIC and under Linux.

## License

Licensed under the [Apache](LICENSE) License.

Copyright (c) Cplusedition Limited. All rights reserved.

