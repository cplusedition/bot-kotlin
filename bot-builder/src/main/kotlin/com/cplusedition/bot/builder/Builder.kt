/*
 *  Copyright (c) 2018, Cplusedition Limited.  All rights reserved.
 *
 *  This file is licensed to you under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.cplusedition.bot.builder

import com.cplusedition.bot.builder.BuilderUtil.Companion.BU
import com.cplusedition.bot.core.*
import com.cplusedition.bot.core.DateUtil.Companion.DateUt
import com.cplusedition.bot.core.FileUtil.Companion.FileUt
import com.cplusedition.bot.core.MavenUtil.GAV
import java.io.File
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.javaField

//////////////////////////////////////////////////////////////////////

interface IBuilderWorkspace {
    val projects: Collection<IProject>
}

//////////////////////////////////////////////////////////////////////

interface IProject {
    /** The directory that contains the target project. */
    val dir: File
    /** A GAV string that can be parsed by GAV.from(). */
    val gav: GAV
}

//////////////////////////////////////////////////////////////////////

interface IBuilderConf {
    val debugging: Boolean
    val workspace: IBuilderWorkspace
    /** The builder project that contains this builder. */
    val builder: IProject
    /** The target project that this builder works on. */
    val project: IProject
}

//////////////////////////////////////////////////////////////////////

interface IBuilder {
    val conf: IBuilderConf
    val log: ICoreLogger

    /**
     * @param segments Path segments relative to builder project directory.
     * @return A file under builder project directory.
     */
    fun builderRes(vararg segments: String): File

    /**
     * @param segments Path segments relative to builder project directory.
     * @return A file under builder project directory if exists, otherwise throw an exception.
     */
    @Throws(IllegalStateException::class)
    fun existingBuilderRes(vararg segments: String): File

    /**
     * @param treepath A path relative to one of the ancestors of the builder project directory.
     * @return The specified file if exists, otherwise throw an exception.
     *
     * For example, if builder project is bot-builder, then
     *     buildAncestorTree("bot/bot-core/src")
     * returns the source directory of the bot-core project if it exists.
     */
    @Throws(IllegalStateException::class)
    fun builderAncestorTree(treepath: String): File

    /**
     * @param treepath A path relative to one of the sibling of the ancestors of the builder project directory.
     * @return The specified file if exists, otherwise throw an exception.
     *
     * For example, if builder project is bot-builder, then
     *     buildAncestorSiblingTree("bot-core/src")
     * returns the source directory of the bot-core project if it exists.
     */
    @Throws(IllegalStateException::class)
    fun builderAncestorSiblingTree(treepath: String): File

    /**
     * Similar to buildeRes() but relative to the target project directory.
     */
    fun projectRes(vararg segments: String): File

    /**
     * Similar to existingBuildeRes() but relative to the target project directory.
     */
    fun existingProjectRes(vararg segments: String): File

    /**
     * Similar to builderAncestorTree() but relative to the target project directory.
     */
    fun projectAncestorTree(treepath: String): File

    /**
     * Similar to builderAncestorSiblingTree() but relative to the target project directory.
     */
    fun projectAncestorSiblingTree(treepath: String): File

    /**
     * Setup and run the given task.
     * @return The task.
     */
    fun <R, T : ICoreTask<R>> task0(task: T): T

    /**
     * Setup and run the given task.
     * @return The task.
     */
    fun <R, T : IBuilderTask<R>> task0(task: T): T

    /**
     * Setup and run the given task.
     * @return The result of run().
     */
    fun <R, T : ICoreTask<R>> task(task: T): R

    /**
     * Setup and run the given task.
     * @return The result of run().
     */
    fun <R, T : IBuilderTask<R>> task(task: T): R
}

interface IBasicBuilder : IBuilder {

    override fun builderRes(vararg segments: String): File {
        return conf.builder.dir.clean(*segments)
    }

    override fun existingBuilderRes(vararg segments: String): File {
        return builderRes(*segments).existsOrFail()
    }

    override fun builderAncestorTree(treepath: String): File {
        return BU.ancestorTree(treepath, conf.builder.dir)?.clean() ?: BU.fail(treepath)
    }

    override fun builderAncestorSiblingTree(treepath: String): File {
        return BU.ancestorSiblingTree(treepath, conf.builder.dir)?.clean() ?: BU.fail(treepath)
    }

    override fun projectRes(vararg segments: String): File {
        return conf.project.dir.clean(*segments)
    }

    override fun existingProjectRes(vararg segments: String): File {
        return projectRes(*segments).existsOrFail()
    }

    override fun projectAncestorTree(treepath: String): File {
        return BU.ancestorTree(treepath, conf.project.dir)?.clean() ?: BU.fail(treepath)
    }

    override fun projectAncestorSiblingTree(treepath: String): File {
        return BU.ancestorSiblingTree(treepath, conf.project.dir)?.clean() ?: BU.fail(treepath)
    }

    override fun <R, T : ICoreTask<R>> task0(task: T): T {
        task.log = log
        task.run()
        return task
    }

    override fun <R, T : IBuilderTask<R>> task0(task: T): T {
        task.builder = this
        task.run()
        return task
    }

    override fun <R, T : ICoreTask<R>> task(task: T): R {
        task.log = log
        return task.run()
    }

    override fun <R, T : IBuilderTask<R>> task(task: T): R {
        task.builder = this
        return task.run()
    }
}

//////////////////////////////////////////////////////////////////////

open class EmptyWorkspace : IBuilderWorkspace {
    override val projects = ArrayList<IProject>(0)
}

//////////////////////////////////////////////////////////////////////

open class BasicWorkspace : IBuilderWorkspace {
    private val lazyProjects = lazy {
        this::class.declaredMemberProperties.filter {
            IProject::class.java.isAssignableFrom(it.javaField?.type)
        }.map {
            it.call(this) as IProject
        }
    }
    override val projects get() = lazyProjects.value
}

//////////////////////////////////////////////////////////////////////

/**
 * A basic IBuilderConf with the following defaults:
 *  - Project gav is "group:project:0".
 *  - Project directory is the current directory.
 *  - Debugging is false.
 *  - Builder gav is "group:builder:0"
 *  - Builder directory is current directory.
 *  - An empty workspace.
 */

open class BasicBuilderConf(
    override val project: IProject,
    override val builder: IProject = project,
    override val debugging: Boolean = false,
    override val workspace: IBuilderWorkspace = EmptyWorkspace()
) : IBuilderConf {
    constructor(
        projectgav: GAV = GAV.of("group:project:0"),
        projectdir: File = FileUt.pwd(),
        debugging: Boolean = false,
        buildergav: GAV = GAV.of("group:builder:0"),
        builderdir: File = FileUt.pwd(),
        workspace: IBuilderWorkspace = EmptyWorkspace()
    ) : this(BasicProject(projectgav, projectdir), BasicProject(buildergav, builderdir), debugging, workspace)
}

//////////////////////////////////////////////////////////////////////

open class BuilderLogger(
    debugging: Boolean,
    classname: String
) : CoreLogger(debugging) {
    init {
        addLifecycleListener(object : CoreLogger.ILifecycleListener {
            override fun onStart(msg: String, starttime: Long, logger: Fun10<String>) {
                if (debugging) {
                    logger("#### Class $classname START: ${DateUt.datetimeString(starttime)}")
                }
            }

            override fun onDone(msg: String, endtime: Long, errors: Int, logger: Fun10<String>) {
                if (debugging) {
                    val ok = if (errors == 0) "OK" else "FAIL"
                    logger("#### Class $classname $ok: ${DateUt.datetimeString(endtime)}")
                }
            }
        })
    }
}

//////////////////////////////////////////////////////////////////////

open class BasicBuilder(
    override val conf: IBuilderConf
) : IBasicBuilder {
    override val log = {
        val classname = this::class.simpleName
        BuilderLogger(conf.debugging, classname ?: "BasicBuilder")
    }()
}

//////////////////////////////////////////////////////////////////////

open class BasicProject(
    override val gav: GAV,
    override val dir: File = FileUt.pwd()
) : IProject {
    val srcDir get() = dir.file("src")
    val buildDir get() = dir.file("build")
    val outDir get() = dir.file("out")
    val trashDir get() = dir.file("trash")
}

open class KotlinProject(gav: GAV, dir: File = FileUt.pwd()) : BasicProject(gav, dir) {
    val mainSrcs
        get() = mutableListOf(
            dir.file("src/main/java"),
            dir.file("src/main/kotlin")
        )
    val testSrcs
        get() = mutableListOf(
            dir.file("src/test/java"),
            dir.file("src/test/kotlin")
        )
    val mainRes
        get() = mutableListOf(
            dir.file("src/main/resources")
        )
    val testRes
        get() = mutableListOf(
            dir.file("src/test/resources")
        )
}

//////////////////////////////////////////////////////////////////////

open class TestLogger(debugging: Boolean) : CoreLogger(debugging)

open class DebugBuilder : IBasicBuilder {
    override val conf = BasicBuilderConf(debugging = true)
    override val log = TestLogger(debugging = true)
}

/**
 * Basic builder for tests.
 */
open class TestBuilder(
    override val conf: IBuilderConf,
    override val log: ICoreLogger = TestLogger(conf.debugging)
) : IBasicBuilder {

    protected val trashDir: File get() = builderRes("trash").mkdirsOrFail()
    private var tmpdir: File = createTempDir(suffix = "", directory = trashDir)

    //////////////////////////////////////////////////////////////////////

    open fun beforeTest() {
        log.enter()
        tmpdir.deleteRecursively()
        tmpdir = createTempDir(suffix = "", directory = trashDir)
    }

    open fun afterTest() {
        tmpdir.deleteRecursively()
        log.leaveX()
    }

    fun tmpDir(dir: File = tmpdir): File {
        return createTempDir(suffix = "", directory = dir)
    }

    fun tmpFile(prefix: String = "tmp", suffix: String? = null, dir: File = tmpdir): File {
        return createTempFile(prefix, suffix, directory = dir)
    }

    fun subtest(desc: String = "", code: Fun00) {
        log.enter(desc) {
            code()
        }
    }
}

//////////////////////////////////////////////////////////////////////

