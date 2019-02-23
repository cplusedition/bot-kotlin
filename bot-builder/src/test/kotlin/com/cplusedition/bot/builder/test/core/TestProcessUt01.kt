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
import com.cplusedition.bot.core.DateUtil.Companion.DateUt
import com.cplusedition.bot.core.FileUtil.Companion.FileUt
import com.cplusedition.bot.core.ProcessUtil.Companion.ProcessUt
import com.cplusedition.bot.core.WithUtil.Companion.With
import com.cplusedition.bot.core.WithoutUtil.Companion.Without
import com.cplusedition.bot.core.file
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

class TestProcessUt01 : TestBase() {

    @Test
    fun testExec01() {
        val files = testResDir.file("files")
        subtest {
            val process = ProcessUt.exec("ls", "-1", files.absolutePath)
            assertNotNull(process)
            if (process != null) {
                assertEquals(0, process.waitFor())
            }
            assertNull(ProcessUt.exec("/notexists"))
        }
        subtest {
            val process = ProcessUt.exec(files, "ls", "-1")
            assertNotNull(process)
            if (process != null) {
                assertEquals(0, process.waitFor())
                val output = FileUt.asString(process.inputStream)
                assertTrue(output.contains("dir1"))
                assertTrue(output.contains("dir2"))
                assertTrue(output.contains("empty.dir"))
            }
        }
    }

    @Test
    fun testAsync01() {
        val files = testResDir.file("files")
        subtest {
            ProcessUt.async("ls", "-1", files.absolutePath) {
                val lines = FileUt.asStringList(it.inputStream)
                log.d(lines)
                assertTrue(lines.contains("dir1"))
                assertTrue(lines.contains("empty.dir"))
            }.get()
        }
        subtest {
            ProcessUt.async(files, "ls", "-1") {
                val lines = FileUt.asStringList(it.inputStream)
                log.d(lines)
                assertTrue(lines.contains("dir1"))
                assertTrue(lines.contains("empty.dir"))
            }.get()
        }
        subtest {
            Without.throwableOrFail {
                ProcessUt.async(files, "ls", "-1") {
                    val lines = FileUt.asStringList(it.inputStream)
                    log.d(lines)
                    assertTrue(lines.contains("dir2"))
                    assertTrue(lines.contains("empty.dir"))
                }.get()
            }
        }
        subtest {
            assertEquals("OK", ProcessUt.async("ls", "-1") {
                "OK"
            }.get())
            /// Check that callback is not called on error.
            var ok = true
            assertTrue(With.exception {
                ProcessUt.async("/notexists") {
                    ok = false
                    "OK"
                }.get()
            } is ExecutionException)
            assertTrue(ok)
        }
        subtest {
            assertEquals("OK", ProcessUt.async(Callable { "OK" }).get())
            assertTrue(With.exception {
                ProcessUt.async(Callable { throw IOException() }).get()
            } is ExecutionException)
        }
    }

    @Test
    fun testAsyncError01() {
        val e = With.exception {
            ProcessUt.async("notexists") { _ ->
                throw AssertionError()
            }.get()
        }
        val msg = e.toString()
        log.d(msg)
        assertTrue(e is ExecutionException)
        assertTrue(msg.contains("java.io.IOException:"))
        assertTrue(msg.contains("Cannot run program"))
        assertTrue(msg.contains("notexists"))
    }

    @Test
    fun testAsync02() {
        val files = testResDir.file("files")
        // Check default parameters.
        ProcessUt.async(
            FileUt.root(),
            arrayOf("ls", "-1", files.absolutePath),
            env = null
        ) {
            val lines = FileUt.asStringList(it.inputStream)
            log.d(lines)
            assertTrue(lines.contains("dir1"))
        }.get()
        ProcessUt.async(
            FileUt.root(),
            arrayOf("ls", "-1", files.absolutePath),
            env = null,
            timeout = DateUt.HOUR
        ) {
            val lines = FileUt.asStringList(it.inputStream)
            log.d(lines)
            assertTrue(lines.contains("dir2"))
        }.get()
        ProcessUt.async(
            files,
            arrayOf("ls", "-1"),
            env = null,
            timeunit = TimeUnit.SECONDS
        ) {
            val lines = FileUt.asStringList(it.inputStream)
            log.d(lines)
            assertTrue(lines.contains("empty.dir"))
        }.get()
        ProcessUt.async(
            FileUt.root(),
            arrayOf("ls", "-1", files.absolutePath),
            env = null,
            timeout = 10,
            timeunit = TimeUnit.SECONDS
        ) {
            val lines = FileUt.asStringList(it.inputStream)
            log.d(lines)
            assertTrue(lines.contains("dir1"))
        }.get()
        ProcessUt.async(
            FileUt.root(),
            arrayOf("ls", "-1", files.absolutePath),
            env = arrayOf()
        ) {
            val lines = FileUt.asStringList(it.inputStream)
            log.d(lines)
            assertTrue(lines.contains("dir2"))
        }.get()
        ProcessUt.async(
            FileUt.root(),
            arrayOf("ls", "-1", files.absolutePath),
            env = arrayOf(),
            timeout = DateUt.HOUR
        ) {
            val lines = FileUt.asStringList(it.inputStream)
            log.d(lines)
            assertTrue(lines.contains("dir1"))
        }.get()
        ProcessUt.async(
            FileUt.root(),
            arrayOf("ls", "-1", files.absolutePath),
            env = arrayOf(),
            timeunit = TimeUnit.SECONDS
        ) {
            val lines = FileUt.asStringList(it.inputStream)
            log.d(lines)
            assertTrue(lines.contains("dir2"))
        }.get()
        ProcessUt.async(
            FileUt.root(),
            arrayOf("ls", "-1", files.absolutePath),
            env = arrayOf(),
            timeout = 10,
            timeunit = TimeUnit.SECONDS
        ) {
            val lines = FileUt.asStringList(it.inputStream)
            log.d(lines)
            assertTrue(lines.contains("empty.dir"))
        }.get()
    }

    @Test
    fun testAsyncError02() {
        val e = With.exception {
            ProcessUt.async("notexists").get()
        }
        val msg = e.toString()
        log.d(msg)
        assertTrue(e is ExecutionException)
        assertTrue(msg.contains("java.io.IOException:"))
        assertTrue(msg.contains("Cannot run program"))
        assertTrue(msg.contains("notexists"))
    }

    @Test
    fun testAsyncTimeout01() {
        subtest {
            val e = With.exception {
                ProcessUt.async(FileUt.pwd(), arrayOf("sleep", "2"), 100, TimeUnit.MICROSECONDS).get()
            }
            val msg = e.toString()
            log.d(msg)
            assertTrue(e is ExecutionException)
            assertTrue(msg.contains("java.util.concurrent.TimeoutException"))
        }
        subtest {
            val e = With.exception {
                ProcessUt.async(FileUt.pwd(), arrayOf("sleep", "2"), 100, TimeUnit.MICROSECONDS).get()
            }
            val msg = e.toString()
            log.d(msg)
            assertTrue(e is ExecutionException)
            assertTrue(msg.contains("java.util.concurrent.TimeoutException"))
        }
        // Check the other shortcut methods for coverage.
        subtest {
            assertTrue(With.exception {
                ProcessUt.async(
                    FileUt.root(),
                    arrayOf("sleep", "2"),
                    100,
                    TimeUnit.MICROSECONDS
                ).get()
            }.toString().contains("java.util.concurrent.TimeoutException"))
        }
        subtest {
            assertTrue(With.exception {
                ProcessUt.async(
                    FileUt.root(),
                    arrayOf("sleep", "2"),
                    null,
                    100,
                    TimeUnit.MICROSECONDS
                ) {}.get()
            }.toString().contains("java.util.concurrent.TimeoutException"))
        }
        subtest {
            assertTrue(With.exception {
                ProcessUt.async(
                    FileUt.root(),
                    arrayOf("sleep", "2"),
                    100,
                    TimeUnit.MICROSECONDS
                ).get()
            }.toString().contains("java.util.concurrent.TimeoutException"))
        }
        subtest {
            assertTrue(With.exception {
                ProcessUt.async(
                    FileUt.root(),
                    arrayOf("sleep", "2"),
                    null,
                    100,
                    TimeUnit.MICROSECONDS
                ) { }.get()
            }.toString().contains("java.util.concurrent.TimeoutException"))
        }
    }

    @Test
    fun testAsyncError03() {
        assertTrue(With.exception {
            ProcessUt.async {
                throw AssertionError()
            }.get()
        } is ExecutionException)
    }

    @Test
    fun testBacktick01() {
        subtest {
            val files = testResDir.file("files")
            val ret = ProcessUt.backtick("ls", "-1", files.absolutePath)
            if (ret == null) fail()
            else {
                log.d(ret)
                assertTrue(Regex("(?m)^empty\\.dir$").find(ret) != null)
            }
        }
        subtest {
            val files = testResDir.file("files")
            val ret = ProcessUt.backtick(listOf("ls", "-1", files.absolutePath))
            if (ret == null) fail()
            else {
                log.d(ret)
                assertTrue(Regex("(?m)^empty\\.dir$").find(ret) != null)
            }
        }
    }
}