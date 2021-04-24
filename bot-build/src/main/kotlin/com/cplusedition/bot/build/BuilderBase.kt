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

package com.cplusedition.bot.build

import com.cplusedition.bot.builder.*
import com.cplusedition.bot.core.FileUt
import com.cplusedition.bot.core.MavenUtil.GAV
import com.cplusedition.bot.core.file
import org.junit.After
import org.junit.Before
import org.junit.ClassRule
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.io.File

open class BuilderBase(conf: IBuilderConf) : BasicBuilder(conf) {

    constructor(project: IProject, debugging: Boolean) : this(BasicBuilderConf(
            project,
            Workspace.botBuildProject,
            debugging,
            Workspace))

    constructor (debugging: Boolean) : this(Conf(debugging))

    companion object {
        const val GROUP = "com.cplusedition.bot"
        const val VERSION = "1.3.71"

        object SingleTestChecker : TestRule {
            override fun apply(base: Statement, description: Description): Statement {
                if (description.testCount() > 1) {
                    return object : Statement() {
                        override fun evaluate() {
                            System.err.println("!!! Please run a single test at a time !!!")
                            System.exit(1)
                        }
                    }
                }
                return base
            }
        }

        /// For JUnit tests, this avoid having to add @Ignore to every test.
        @ClassRule
        @JvmStatic
        public fun singleTestChecker(): TestRule {
            return SingleTestChecker
        }
    }

    class Conf(debugging: Boolean) : BasicBuilderConf(
            Workspace.botBuildProject,
            Workspace.botBuildProject,
            debugging,
            Workspace
    )

    object Workspace : BasicWorkspace() {
        /// The system property override allow configuring the workspace without assumption on the current work directory.
        /// This is also useful for launching this builder from another project.
        val topdir = System.getProperty("BOT_KOTLIN_DIR")?.let { File(it) } ?: FileUt.pwd("..")
        val botProject = BasicProject(GAV.of("$GROUP:${topdir.name}:$VERSION"), topdir)
        val botCoreProject = KotlinProject(GAV.of("$GROUP:bot-core:$VERSION"), topdir.file("bot-core"))
        val botBuilderProject = KotlinProject(GAV.of("$GROUP:bot-builder:$VERSION"), topdir.file("bot-builder"))
        val botBuildProject = KotlinProject(GAV.of("$GROUP:bot-build:$VERSION"), topdir.file("bot-build"))
    }

    @Before
    fun setup() {
        log.enter()
    }

    @After
    fun teardown() {
        log.leaveX()
        log.flush()
    }
}