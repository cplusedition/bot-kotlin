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

package com.cplusedition.bot.builder.test.core

import com.cplusedition.bot.builder.test.zzz.TestBase
import com.cplusedition.bot.core.*
import com.cplusedition.bot.core.WithUtil.Companion.With
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.util.*

class TestCoreLogger01 : TestBase() {

    @Test
    fun testBasic01() {
    }

    @Test
    fun testEnterLeave01() {
        fun setup(log: ICoreLogger): String {
            log.enter {
                log.enter(TestCoreLogger01::class) {
                    log.enter(this::testEnterLeave01) {
                        log.enter("testing") {
                            log.d("# debug")
                            log.i("# info")
                            log.w("# warn")
                            log.e("# error")
                            log.resetErrorCount()
                        }
                    }
                }
            }
            return log.getLog().join("")
        }
        subtest {
            val log = CoreLogger(debugging = true)
            val output = setup(log)
            assertTrue(output.contains("++ TestCoreLogger01"))
            assertTrue(output.contains("-- TestCoreLogger01"))
            assertTrue(output.contains("+++ testEnterLeave01"))
            assertTrue(output.contains("--- testEnterLeave01"))
            assertTrue(output.contains("++++ testing"))
            assertTrue(output.contains("---- testing"))
            assertTrue(output.contains("# debug"))
            assertTrue(output.contains("# info"))
            assertTrue(output.contains("# warn"))
            assertTrue(output.contains("# error"))
        }
        subtest {
            val log = CoreLogger(debugging = false)
            val output = setup(log)
            assertFalse(output.contains("++ TestCoreLogger01"))
            assertFalse(output.contains("-- TestCoreLogger01"))
            assertFalse(output.contains("+++ testEnterLeave01"))
            assertFalse(output.contains("--- testEnterLeave01"))
            assertFalse(output.contains("++++ testing"))
            assertFalse(output.contains("---- testing"))
            assertFalse(output.contains("# debug"))
            assertTrue(output.contains("# info"))
            assertTrue(output.contains("# warn"))
            assertTrue(output.contains("# error"))
        }
    }

    @Test
    fun testEnterLeave02() {
        subtest {
            val log = CoreLogger(debugging = true)
            assertEquals("OK", log.enter<String> {
                "OK"
            })
            assertEquals("OK", log.enter<String>("name", "msg") {
                "OK"
            })
            assertEquals("OK", log.enterX<String> {
                "OK"
            })
            assertEquals("OK", log.enterX<String>("name", "msg") {
                "OK"
            })
            log.enter("error cleared")
            log.enter("fail on error")
            log.enter("continue on error")
            log.e()
            assertEquals(1, log.errorCount)
            log.leave()
            assertEquals(1, log.errorCount)
            assertTrue(With.throwableOrNull { log.leaveX() } is IllegalStateException)
            log.resetErrorCount()
            log.leaveX()
            assertEquals(0, log.errorCount)
        }
        subtest {
            val log = CoreLogger(debugging = true)
            log.enter(this::testEnterLeave02)
            log.enter(this::testEnterLeave02, "msg")
            log.enter(TestCoreLogger01::class)
            log.enter(TestCoreLogger01::class, "msg")
            log.enter(this::testEnterLeave02)
            log.enter(this::testEnterLeave02, "msg")
            log.enter(TestCoreLogger01::class)
            log.enter(TestCoreLogger01::class, "msg")
            log.e("error")
            log.leave("msg")
            log.leave()
            log.leave("msg")
            log.leave()
            assertTrue(With.throwableOrNull { log.leaveX("msg") } is java.lang.IllegalStateException)
            log.resetErrorCount()
            log.leaveX()
            log.leaveX("msg")
            log.leaveX()
            val output = log.getLog().join("")
            assertTrue(output.contains("+ testEnterLeave02"))
            assertTrue(output.contains("++ testEnterLeave02: msg"))
            assertTrue(output.contains("+++ TestCoreLogger01"))
            assertTrue(output.contains("++++ TestCoreLogger01: msg"))
            assertTrue(output.contains("+++++ testEnterLeave02"))
            assertTrue(output.contains("++++++ testEnterLeave02: msg"))
            assertTrue(output.contains("+++++++ TestCoreLogger01"))
            assertTrue(output.contains("++++++++ TestCoreLogger01: msg"))
            assertTrue(output.contains("- testEnterLeave02"))
            assertTrue(output.contains("-- testEnterLeave02: msg"))
            assertTrue(output.contains("--- TestCoreLogger01"))
            assertTrue(output.contains("---- TestCoreLogger01: msg"))
            assertTrue(output.contains("----- testEnterLeave02"))
            assertTrue(output.contains("------ testEnterLeave02: msg"))
            assertTrue(output.contains("------- TestCoreLogger01"))
            assertTrue(output.contains("-------- TestCoreLogger01: msg"))
        }
        subtest {
            val log = CoreLogger(debugging = true)
            log.enterX(this::testEnterLeave02) {
                log.enterX(this::testEnterLeave02, "msg") {
                    log.enterX(TestCoreLogger01::class) {
                        assertTrue(With.throwableOrNull {
                            log.enterX(TestCoreLogger01::class, "msg") {
                                log.enter(this::testEnterLeave02) {
                                    log.enter(this::testEnterLeave02, "msg") {
                                        log.enter(TestCoreLogger01::class) {
                                            log.enter(TestCoreLogger01::class, "msg") {
                                                log.e("error")
                                            }
                                        }
                                    }
                                }
                            }
                        } is java.lang.IllegalStateException)
                        assertEquals(1, log.errorCount)
                        log.resetErrorCount()
                    }
                    assertEquals(0, log.errorCount)
                }
            }
            val output = log.getLog().join("")
            assertTrue(output.contains("+ testEnterLeave02"))
            assertTrue(output.contains("++ testEnterLeave02: msg"))
            assertTrue(output.contains("+++ TestCoreLogger01"))
            assertTrue(output.contains("++++ TestCoreLogger01: msg"))
            assertTrue(output.contains("+++++ testEnterLeave02"))
            assertTrue(output.contains("++++++ testEnterLeave02: msg"))
            assertTrue(output.contains("+++++++ TestCoreLogger01"))
            assertTrue(output.contains("++++++++ TestCoreLogger01: msg"))
            assertTrue(output.contains("- testEnterLeave02"))
            assertTrue(output.contains("-- testEnterLeave02: msg"))
            assertTrue(output.contains("--- TestCoreLogger01"))
            assertTrue(output.contains("---- TestCoreLogger01: msg"))
            assertTrue(output.contains("----- testEnterLeave02"))
            assertTrue(output.contains("------ testEnterLeave02: msg"))
            assertTrue(output.contains("------- TestCoreLogger01"))
            assertTrue(output.contains("-------- TestCoreLogger01: msg"))
        }
    }

    @Test
    fun testSaveLog01() {
        subtest {
            fun check(output: String) {
                assertTrue(output.contains("+ testEnterLeave02"))
                assertTrue(output.contains("++ testEnterLeave02: msg"))
                assertTrue(output.contains("+++ TestCoreLogger01"))
                assertTrue(output.contains("++++ TestCoreLogger01: msg"))
                assertTrue(output.contains("+++++ testEnterLeave02"))
                assertTrue(output.contains("++++++ testEnterLeave02: msg"))
                assertTrue(output.contains("+++++++ TestCoreLogger01"))
                assertTrue(output.contains("++++++++ TestCoreLogger01: msg"))
                assertTrue(output.contains("- testEnterLeave02"))
                assertTrue(output.contains("-- testEnterLeave02: msg"))
                assertTrue(output.contains("--- TestCoreLogger01"))
                assertTrue(output.contains("---- TestCoreLogger01: msg"))
                assertTrue(output.contains("----- testEnterLeave02"))
                assertTrue(output.contains("------ testEnterLeave02: msg"))
                assertTrue(output.contains("------- TestCoreLogger01"))
                assertTrue(output.contains("-------- TestCoreLogger01: msg"))
            }

            val log = CoreLogger(debugging = true)
            assertEquals("OK",
                log.enterX<String?>(this::testEnterLeave02) {
                    log.enterX<String?>(this::testEnterLeave02, "msg") {
                        log.enterX<String?>(TestCoreLogger01::class) {
                            log.enterX<String>(TestCoreLogger01::class, "msg") {
                                log.enter<String>(this::testEnterLeave02) {
                                    log.enter<String>(this::testEnterLeave02, "msg") {
                                        log.enter<String>(TestCoreLogger01::class) {
                                            log.enter<String>(TestCoreLogger01::class, "msg") {
                                                "OK"
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                })
            val file = tmpFile()
            log.saveLog(file)
            /// Note that this is a sync call and thus make sure savelog() is completed.
            if(true) {
                val text = log.getLog().join("")
                check(text)
                assertEquals(17, text.split("\n").size)
            }
            if (true) {
                val text = file.readText()
                val lines = text.split("\n")
                log.d("# lines: ${lines.size}")
                for(line in lines) {
                    log.d("|$line|")
                }
                check(text)
                assertEquals(17, lines.size)
            }
        }
    }

    @Test
    fun testLog01() {
        subtest {
            val log = CoreLogger(debugging = true)
            ProcessUt.sleep(100)
            log.enter {
                log.dd("step1")
                log.ii("stepii1")
                ProcessUt.sleep(100)
                log.dd("step2")
                log.ii("stepii2")
            }
            val output = log.getLog().join("")
            assertTrue(output.contains(Regex(".*[\\d.]+ s: step1")))
            assertTrue(output.contains(Regex(".*[\\d.]+ s: step2")))
            assertTrue(output.contains(Regex(".*[\\d.]+ s: stepii1")))
            assertTrue(output.contains(Regex(".*[\\d.]+ s: stepii2")))
            val match = Regex("([\\d.]+) s: stepii2").find(output)
            assertNotNull(match)
            if (match != null) {
                val group1 = match.groupValues[1]
                /// Check that time relative to start of logger instead of start of scope.
                assertTrue(group1, group1.toDouble() >= 0.19)
            }
        }
        subtest {
            val log = CoreLogger(debugging = false)
            log.enter {
                log.dd("step1")
                log.ii("stepii1")
                ProcessUt.sleep(100)
                log.dd("step2")
                log.ii("stepii2")
            }
            val output = log.getLog().join("")
            assertFalse(output.contains(Regex("[\\d.]+ s: step1")))
            assertFalse(output.contains(Regex("[\\d.]+ s: step2")))
            assertTrue(output.contains(Regex("[\\d.]+ s: stepii1")))
            assertTrue(output.contains(Regex("[\\d.]+ s: stepii2")))
        }
        subtest {
            val log = CoreLogger(debugging = true)
            log.enter {
                log.e("msg1")
                log.e("msg2", IOException("Expected IOException"))
                assertEquals(2, log.errorCount)
                log.resetErrorCount()
            }
        }
        subtest {
            val log = CoreLogger(debugging = true)
            assertTrue(With.exceptionOrNull { log.enterX { throw IOException("Expected IOException") } } is java.lang.IllegalStateException)
            assertEquals(1, log.errorCount)
            assertTrue(With.exceptionOrNull { log.enterX<String> { throw IOException("Expected IOException") } } is java.lang.IllegalStateException)
            assertEquals(2, log.errorCount)
            log.resetErrorCount()
        }
    }

    @Test
    fun testLogMulti01() {
        subtest {
            val log = CoreLogger(debugging = true)
            val a = listOf("a", "b", "c")
            val b = listOf("A", "B", "C")
            log.d("1", "2", "3")
            log.d(a)
            log.d(b.iterator())
            log.i("1", "2", "3")
            log.i(a)
            log.i(b.iterator())
            log.w("1", "2", "3")
            log.w(a)
            log.w(b.iterator())
            log.e("1", "2", "3")
            log.e(a)
            log.e(b.iterator())
            log.resetErrorCount()
            val set = TreeSet<String>()
            for (s in log.getLog().join("").lines()) {
                set.add(s)
            }
            assertEquals(10, set.size)
            assertTrue(set.contains("1"))
            assertTrue(set.contains("2"))
            assertTrue(set.contains("3"))
            assertTrue(set.contains("a"))
            assertTrue(set.contains("b"))
            assertTrue(set.contains("c"))
            assertTrue(set.contains("A"))
            assertTrue(set.contains("B"))
            assertTrue(set.contains("C"))
        }
        subtest {
            val log = CoreLogger(debugging = true)
            log.enter {
                log.dfmt("%s", "debug formatted")
                log.ifmt("%s", "info formatted")
                log.wfmt("%s", "warn formatted")
                log.efmt("%s", "error formatted")
                assertEquals(1, log.errorCount)
                log.resetErrorCount()
            }
            val output = log.getLog()
            assertTrue(output.contains("debug formatted\n"))
            assertTrue(output.contains("info formatted\n"))
            assertTrue(output.contains("warn formatted\n"))
            assertTrue(output.contains("error formatted\n"))
        }
    }

    @Test
    fun testQuiet01() {
        val log = CoreLogger(debugging = true)
        log.enter("normal") {
            log.quiet {
                log.enter("quiet") {
                    log.d("quiet d")
                    log.i("quiet i")
                    ProcessUt.sleep(100)
                    log.w("quiet w")
                    log.e("quiet e")
                    log.resetErrorCount()
                }
            }
        }
        val output = log.getLog().join("")
        val lines = output.trim().lines()
        assertEquals(2, lines.size)
        assertTrue(output.contains("+ normal"))
        assertTrue(output.contains("- normal"))
        assertFalse(output.contains("quiet"))
        assertFalse(output.contains("quiet"))
    }

    @Test
    fun testLifecycleListener01() {
        val log = CoreLogger(debugging = true)
        var count = 0
        val listener = object : CoreLogger.ILifecycleListener {
            override fun onDone(msg: String, endtime: Long, errors: Int, logger: Fun10<String>) {
                ++count
            }

            override fun onStart(msg: String, starttime: Long, logger: Fun10<String>) {
                logger("# start")
                ++count
            }
        }
        log.addLifecycleListener(listener)
        log.enter {
        }
        /// getLog() is sync
        assertEquals(1, log.getLog().join("").trim().lines().size)
        assertEquals(2, count)
        log.removeLifecycleListener(listener)
        log.enter("testing") {}
        /// getLog() is sync
        assertEquals(3, log.getLog().join("").trim().lines().size)
        assertEquals(2, count)
    }

    @Test
    fun testSmart01() {
        val log = CoreLogger(debugging = true)
        log.d("")
        log.i("")
        log.w("")
        log.e("")
        assertEquals("", log.getLog().join(""))
        log.d("\n")
        log.i("\n")
        log.w("\n")
        log.e("\n")
        assertEquals("\n\n\n\n", log.getLog().join(""))
        log.d("d")
        log.i("i")
        log.w("w")
        log.e("e")
        assertEquals("\n\n\n\nd\ni\nw\ne\n", log.getLog().join(""))
        log.d("d\n")
        log.i("i\n")
        log.w("w\n")
        log.e("e\n")
        assertEquals("\n\n\n\nd\ni\nw\ne\nd\ni\nw\ne\n", log.getLog().join(""))
    }
}