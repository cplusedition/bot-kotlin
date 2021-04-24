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

package com.cplusedition.bot.builder.test.zzz

import com.cplusedition.bot.builder.*
import com.cplusedition.bot.builder.test.zzz.TestBase.SuiteConf.Companion.QUICK
import com.cplusedition.bot.core.*
import org.junit.After
import org.junit.Assert
import org.junit.Before
import java.io.File
import kotlin.math.abs

/**
 * Base class for tests.
 */
open class TestBase(debugging: Boolean = false) :
        TestBuilder(BasicBuilderConf(debugging = debugging), TestLogger(debugging)) {

    companion object {
        const val GROUP = "com.cplusedition.bot"
        const val VERSION = "1"
    }

    //////////////////////////////////////////////////////////////////////

    class SuiteConf(
            val lengthy: Boolean = false,
            val performance: Boolean = false,
            val screenshots: Boolean = false
    ) {
        companion object {
            val QUICK = SuiteConf()
            val LENGTHY = SuiteConf(lengthy = true)
            val STANDARD = SuiteConf(lengthy = true, performance = true)
            val FULL = SuiteConf(lengthy = true, performance = true, screenshots = true)
        }
    }

    val suite = QUICK

    object Workspace : BasicWorkspace() {
        val dir = FileUt.pwd("..")
        val botCoreProject = KotlinProject(
                MavenUtil.GAV.of("$GROUP:bot-core:$VERSION"),
                dir.file("bot-core")
        )
        val botBuilderProject = KotlinProject(
                MavenUtil.GAV.of("$GROUP:bot-builder:$VERSION"),
                dir.file("bot-builder")
        )
    }

    //////////////////////////////////////////////////////////////////////

    val testResDir: File get() = builderRes("src/test/resources").existsOrFail()

    //////////////////////////////////////////////////////////////////////

    @Before
    override fun beforeTest() {
        super.beforeTest()
    }

    @After
    override fun afterTest() {
        super.afterTest()
    }

    fun lengthy(desc: String = "", code: Fun00) {
        if (suite.lengthy) {
            subtest(desc, code)
        } else {
            log.d("# Ignored lengthy test: $desc")
        }
    }

    fun checkPreserveTimestamp(preserve: Boolean, tofile: File, fromfile: File, limit: Long) {
        val expected = fromfile.lastModified()
        val actual = tofile.lastModified()
        val delta = abs(actual - expected)
        log.d("# expected=${DateUt.datetimeString(expected)}, actual=${DateUt.datetimeString(actual)}, delta=$delta")
        Assert.assertTrue("$delta", if (preserve) delta < limit else delta > limit)

    }
}

//////////////////////////////////////////////////////////////////////

