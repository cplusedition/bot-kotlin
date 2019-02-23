# **B**uild **O**n **T**est User Guide

## bot-core

This module contains some utility classes for logging (CoreLogger),
working with file (FileUtil, Treewalker, ... etc), text (TextUtil), random
(RandomUtil) and process (ProcessUtil), ... etc.

## bot-builder

This module contains a simple build framework with Workspace, Builder,
Project and Task. The taskdef file contains a few standard tasks
for copy, remove, zip, create and verify checksums, ... etc. The tasks use
the Fileset class for file selection and filtering.

For some IDE, you may have workspace, project and modules. But we only
have Workspace and Project here.

**IBuilderWorkspace** contains definition of the projects (IProject). It is just
a convenient centralized place for the project definitions. You may use an
empty workspace or put your project definition else where.

**IProject** contains information about projects/modules. The only
mandatory information is the **G**roup:**A**rtifactId:**V**erson and the
project directory for the project.

**IBuilder** Each builder is basically a test class with test methods that
perform tasks on the target project. It provide a logger and some
convenient methods to access the builder and target projects. Typically,
you have a module that contains the builders for the other modules in the
project, that is called the builder project. The project that the builder works
on is the target project.

**IBuilderConf** Each IBuilder is constructed with an IBuidlerConf argument
to configure the builder. Here you specify the builder and target projects,
the debugging flag and the workspace.

**ICoreTask** A runnable with a logger. See the taskdef file for some
sample task definitions.

**IBuilderTask** A runnable with an associated builder.

Again, these framework classes are just for convenient. You may not even
need any of these to write your scripts.

This module also contain the tests for both the bot-core and bot-builder
modules. The tests should be launched with the bot-builder project
directory as working directory.

## bot-build

This module contains builders for managing this project. It also work as a
simple example on how to use the `bot-builder` framework.

For example, invoke the `ReleaseBuilder.distSrcZip()`` method as a unit
test in the IDE create the source zip file and checksum. invoke the
`TestBuilder.debugOn()`` or `debugOff()`` to turn debug on and off in all
the tests respectively.
