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

package com.cplusedition.bot.builder.test.builder

import com.cplusedition.bot.builder.*
import com.cplusedition.bot.builder.test.zzz.TestBase
import com.cplusedition.bot.core.FileUt
import com.cplusedition.bot.core.ICoreLogger
import com.cplusedition.bot.core.MavenUtil.GAV
import com.cplusedition.bot.core.WithUtil.Companion.With
import com.cplusedition.bot.core.WithoutUtil.Companion.Without
import com.cplusedition.bot.core.file
import com.cplusedition.bot.core.join
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class TestBuilder01 : TestBase() {

    @Test
    fun testIBasicBuilder01() {
        val top = Workspace.dir.name
        subtest {
            assertTrue(builderRes().exists())
            assertTrue(builderRes("src").exists())
            assertFalse(builderRes("notexists").exists())
            assertTrue(existingBuilderRes().exists())
            assertTrue(existingBuilderRes("src").exists())
            With.exceptionOrFail { existingBuilderRes("notexists") }
        }
        subtest {
            assertTrue(projectRes().exists())
            assertTrue(projectRes("src").exists())
            assertFalse(projectRes("notexists").exists())
            assertTrue(existingProjectRes().exists())
            assertTrue(existingProjectRes("src").exists())
            With.exceptionOrFail { existingProjectRes("notexists") }
        }
        subtest {
            builderAncestorTree("$top/bot-builder/src").exists()
            With.exceptionOrFail { builderAncestorTree("") }
            With.exceptionOrFail { builderAncestorTree("notexists") }
            With.exceptionOrFail { builderAncestorTree("bot-core") }
            With.exceptionOrFail { builderAncestorTree("bot-builder/src/notexists") }
        }
        subtest {
            projectAncestorTree("$top/bot-builder/src").exists()
            With.exceptionOrFail { projectAncestorTree("") }
            With.exceptionOrFail { projectAncestorTree("notexists") }
            With.exceptionOrFail { projectAncestorTree("bot-core") }
            With.exceptionOrFail { projectAncestorTree("bot-builder/src/notexists") }
        }
        subtest {
            builderAncestorSiblingTree("bot-core/src").exists()
            With.exceptionOrFail { builderAncestorSiblingTree("") }
            With.exceptionOrFail { builderAncestorSiblingTree("notexists") }
            With.exceptionOrFail { builderAncestorSiblingTree("bot-core/src/notexists") }
        }
        subtest {
            projectAncestorSiblingTree("bot-core/src").exists()
            With.exceptionOrFail { projectAncestorSiblingTree("") }
            With.exceptionOrFail { projectAncestorSiblingTree("notexists") }
            With.exceptionOrFail { projectAncestorSiblingTree("bot-core/src/notexists") }
        }
        subtest {
            val basicworkspace = object : BasicWorkspace() {
                val builderProject = KotlinProject(GAV.of("com.cplusedition.bot:bot-builder:1"), builderRes())
            }
            val builder = BasicBuilder(
                    BasicBuilderConf(
                            BasicProject(GAV.of("a/a/1.0")),
                            BasicProject(GAV.of("b/b/1.0")),
                            debugging = false,
                            workspace = basicworkspace
                    )
            )
            assertEquals(1, builder.conf.workspace.projects.size)

        }
    }

    @Test
    fun testBuilderConf01() {
        subtest {
            with(BasicBuilder(BasicBuilderConf())) {
                log.enter("test1") {
                    log.d("# debug")
                }
                val lines = log.getLog()
                assertEquals(0, lines.size)
                assertEquals("0", conf.builder.gav.version.toString())
                assertEquals("0", conf.project.gav.version.toString())
            }
        }
        subtest {
            with(
                    BasicBuilder(
                            BasicBuilderConf(
                                    BasicProject(GAV.of("a/a/1.0")),
                                    BasicProject(GAV.of("b/b/2.0")),
                                    debugging = false
                            )
                    )
            ) {
                log.enter("test1") {
                    log.d("# debug")
                }
                val lines = log.getLog()
                assertEquals(0, lines.size)
                assertEquals("2.0", conf.builder.gav.version.toString())
                assertEquals("1.0", conf.project.gav.version.toString())
            }
        }
        subtest {
            with(
                    BasicBuilder(
                            BasicBuilderConf(
                                    BasicProject(GAV.of("a/a/1.0")),
                                    BasicProject(GAV.of("b/b/1.0")),
                                    debugging = true
                            )
                    )
            ) {
                log.enter("test1") {
                    log.d("# debug")
                }
                val output = log.getLog().join("")
                val lines = output.trim().lines()
                assertEquals(5, lines.size)
                assertTrue(output.contains("START"))
                assertTrue(output.contains("OK"))
                assertTrue(output.contains(BasicBuilder::class.simpleName.toString()))
            }
        }
    }

    @Test
    fun testIBuilderLogger01() {
        with(DebugBuilder()) {
            subtest {
                val log = BuilderLogger(false, this::class.simpleName ?: "XXX")
                log.enter("test1") {
                    log.d("# debug")
                }
                val lines = log.getLog()
                assertEquals(0, lines.size)
            }
            subtest {
                val log = BuilderLogger(true, this::class.simpleName ?: "XXX")
                log.enter("test1") {
                    log.d("# debug")
                }
                val output = log.getLog().join("")
                val lines = output.trim().lines()
                assertEquals(5, lines.size)
                assertTrue(output.contains("START"))
                assertTrue(output.contains("OK"))
                assertTrue(output.contains(this::class.simpleName.toString()))
            }
        }
    }

    @Test
    fun testBasicProject01() {
        subtest("Check that BasicProject check project directory exists") {
            Without.exceptionOrFail {
                assertEquals("exists", BasicProject(GAV.of("group:exists:1.0"), FileUt.pwd()).gav.artifactId)
                assertEquals("exists-default", BasicProject(GAV.of("group:exists-default:1.0")).gav.artifactId)
            }
            With.exceptionOrFail {
                assertEquals("notexists", BasicProject(GAV.of("group:not-exists:1.0"), File("/notexists.dir")).gav.artifactId)
            }
        }
    }

    @Test
    fun testBasicWorkspace01() {
        val workspace = object : BasicWorkspace() {
            val coreProject =
                    KotlinProject(GAV.of("com.cplusedition.bot:bot-core:1"), builderAncestorSiblingTree("bot-core"))
            val builderProject = KotlinProject(GAV.of("com.cplusedition.bot:bot-builder:1"), builderRes())
        }
        subtest {
            assertEquals(2, workspace.projects.size)
            assertTrue(workspace.coreProject.dir.file("src").exists())
            assertTrue(workspace.builderProject.dir.file("src").exists())
        }
        subtest {
            val project = workspace.builderProject
            assertEquals("bot-builder", project.gav.artifactId)
            assertTrue(project.srcDir.exists())
            assertTrue(project.mainSrcs.size == 2)
            assertTrue(project.testSrcs.size == 2)
            //            assertTrue(project.mainRes.size == 1)
            //            assertTrue(project.testRes.size == 1)
            assertTrue(project.testResDir.exists())
        }
    }

    @Test
    fun testCoreTask01() {
        with(DebugBuilder()) {

            val message = "CoreTask: 123"
            val quietMessage = "!!! Quiet !!!"

            class Task(log: ICoreLogger? = null) : CoreTask<Int>(log) {

                override fun run(): Int {
                    if (quiet) {
                        log.d(quietMessage)
                    } else {
                        log.d(message)
                    }
                    return 123
                }

                fun ok(): Boolean {
                    return true
                }
            }
            subtest {
                assertEquals(123, task(Task()))
                assertTrue(log.getLog().join("").contains(message))
            }
            subtest {
                val task = Task(log)
                task.run()
                assertTrue(task.ok())
                assertTrue(log.getLog().join("").contains(message))
                assertFalse(log.getLog().join("").contains(quietMessage))
            }
            subtest {
                val task = Task()
                task.setQuiet(true)
                assertTrue(task0(task).ok())
                assertTrue(log.getLog().join("").contains(quietMessage))
            }
        }
    }

    @Test
    fun testBuilderTask01() {
        with(DebugBuilder()) {
            val quietMessage = "!!! Quiet !!!"

            class Task(builder: IBuilder? = null) : BuilderTask<Int>(builder) {

                override fun run(): Int {
                    log.d("${builder::class.simpleName}: 123")
                    if (quiet) {
                        log.d(quietMessage)
                    }
                    return 123
                }

                fun ok(): Boolean {
                    return true
                }
            }
            subtest {
                assertEquals(123, task(Task()))
                assertTrue(log.getLog().join("").contains(this::class.simpleName!!.toString()))
            }
            subtest {
                assertTrue(task0(Task()).ok())
                assertTrue(log.getLog().join("").contains(this::class.simpleName!!.toString()))
                assertFalse(log.getLog().join("").contains(quietMessage))
            }
            subtest {
                val task = Task(this)
                task.setQuiet(true)
                task.run()
                assertTrue(task.ok())
                assertTrue(log.getLog().join("").contains(quietMessage))
            }
        }
    }

    @Test
    fun testExpect01() {
        subtest("Expect.error()") {
            log.expectError {
                log.e("# ERROR: Expected error")
            }
            assertEquals(0, log.resetErrorCount())
            log.expectError {
            }
            assertEquals(1, log.resetErrorCount())
        }
        subtest {
            with(BasicBuilder(conf)) {
                log.expectError {
                    log.e("# ERROR: Expected error")
                }
                assertEquals(0, log.resetErrorCount())
                log.expectError {
                }
                assertEquals(1, log.resetErrorCount())
            }

        }
    }
}