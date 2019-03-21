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
import com.cplusedition.bot.core.FileUtil.Companion.FileUt
import com.cplusedition.bot.core.MavenUtil.GAV
import com.cplusedition.bot.core.file
import org.junit.After
import org.junit.Before

open class BuilderBase(debugging: Boolean) : BasicBuilder(Conf(debugging)) {

    companion object {
        const val GROUP = "com.cplusedition.bot"
        const val VERSION = "1.1.2"
        const val KOTLIN_VERSION = "1.2.61"
    }

    class Conf(debugging: Boolean) : BasicBuilderConf(
        Workspace.botBuildProject,
        Workspace.botBuildProject,
        debugging,
        Workspace
    )

    object Workspace : BasicWorkspace() {
        val dir = FileUt.pwd("..")
        val botProject = BasicProject(GAV.of("$GROUP:${dir.name}:$VERSION"), dir)
        val botCoreProject = KotlinProject(GAV.of("$GROUP:bot-core:$VERSION"), dir.file("bot-core"))
        val botBuilderProject = KotlinProject(GAV.of("$GROUP:bot-builder:$VERSION"), dir.file("bot-builder"))
        val botBuildProject = KotlinProject(GAV.of("$GROUP:bot-build:$VERSION"), dir.file("bot-build"))
    }

    @Before
    fun setup() {
    }

    @After
    fun teardown() {
        log.flush()
    }
}