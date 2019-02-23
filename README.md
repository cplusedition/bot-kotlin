# **B**uild **O**n **T**est

## Introduction

**BOT** is a light framework for scripting in your favourite type safe
language, which is Kotlin in this case. There is also a variant in Swift.
This project contains a simple build framework and some pretty decent
utilities for logging, working with file, text, and process, ... etc., to get
you started. However, that is not the key.

The key is two ideas that it builds on. First, you can write scripts with your
favourite type safe language that is well supported by the IDE. Second,
you can launch the script directly from the IDE through the unit test
framework by wrapping it as a test method.

With proper IDE support, you can easily modify, launch and see the result
instantly. Take advantage of all the coding and debugging support of the
IDE. Enjoy coding in a modern, safe and expressive language as well as the
quick modify / run cycle of a scripting language.

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

**NOTE** This is not a full-fledged build system or for standalone command
line applications. It is used for custom tasks that is not in the standard build
system workflow and inside an IDE with proper language and unit test
support.

**NOTE** The project has only been tested to work under Linux.

## License

Copyright (c) Cplusedition Limited. All rights reserved.

Licensed under the [Apache](LICENSE.txt) License.
