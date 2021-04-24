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
import com.cplusedition.bot.core.WithoutUtil.Companion.Without
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.IOException
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
            ProcessUtBuilder("ls", "-1", files.absolutePath).asyncOrFail(ByteArrayOutputStream()) { out ->
                val lines = out.toByteArray().inputStream().reader().readLines()
                log.d(lines)
                assertTrue(lines.contains("dir1"))
                assertTrue(lines.contains("empty.dir"))
            }.get()
        }
        subtest {
            val out = ByteArrayOutputStream()
            ProcessUtBuilder(files, "ls", "-1").out(out).async {
                val lines = out.toByteArray().inputStream().reader().readLines()
                log.d(lines)
                assertTrue(lines.contains("dir1"))
                assertTrue(lines.contains("empty.dir"))
            }.get()
        }
        subtest {
            Without.throwableOrFail {
                ProcessUtBuilder(files, "ls", "-1").asyncOrFail(ByteArrayOutputStream()) { out ->
                    val lines = out.toByteArray().inputStream().reader().readLines()
                    log.d(lines)
                    assertTrue(lines.contains("dir2"))
                    assertTrue(lines.contains("empty.dir"))
                }.get()
            }
        }
        subtest {
            assertEquals("OK", ProcessUtBuilder("ls", "-1").async {
                "OK"
            }.get())
            /// Check that callback is not called on error.
            var ok = true
            assertTrue(With.exceptionOrNull {
                ProcessUtBuilder("/notexists").async {
                    ok = false
                    "OK"
                }.get()
            } is ExecutionException)
            assertTrue(ok)
        }
        subtest {
            assertEquals("OK", ProcessUtBuilder("true").async { "OK" }.get())
            assertTrue(With.exceptionOrNull {
                ProcessUtBuilder("true").async { throw IOException() }.get()
            } is ExecutionException)
        }
    }

    @Test
    fun testAsyncError01() {
        val e = With.exceptionOrNull {
            ProcessUtBuilder("notexists").async {
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
        ProcessUtBuilder(
                FileUt.root(),
                "ls", "-1", files.absolutePath
        ).asyncOrFail(ByteArrayOutputStream()) { out ->
            val lines = out.toByteArray().inputStream().reader().readLines()
            log.d(lines)
            assertTrue(lines.contains("dir1"))
        }.get()
        ProcessUtBuilder(
                FileUt.root(),
                "ls", "-1", files.absolutePath
        ).timeout(1, TimeUnit.HOURS).asyncOrFail(ByteArrayOutputStream()) { out ->
            val lines = out.toByteArray().inputStream().reader().readLines()
            log.d(lines)
            assertTrue(lines.contains("dir2"))
        }.get()
        ProcessUtBuilder(
                files,
                "ls", "-1"
        ).timeout(10, TimeUnit.SECONDS).asyncOrFail(ByteArrayOutputStream()) { out ->
            val lines = out.toByteArray().inputStream().reader().readLines()
            log.d(lines)
            assertTrue(lines.contains("empty.dir"))
        }.get()
        ProcessUtBuilder(
                FileUt.root(),
                "ls", "-1", files.absolutePath
        ).timeout(10000, TimeUnit.MILLISECONDS).asyncOrFail(ByteArrayOutputStream()) { out ->
            val lines = out.toByteArray().inputStream().reader().readLines()
            log.d(lines)
            assertTrue(lines.contains("dir1"))
        }.get()
        ProcessUtBuilder(
                FileUt.root(),
                "ls", "-1", files.absolutePath
        ).env().asyncOrFail(ByteArrayOutputStream()) { out ->
            val lines = out.toByteArray().inputStream().reader().readLines()
            log.d(lines)
            assertTrue(lines.contains("dir2"))
        }.get()
        ProcessUtBuilder(
                FileUt.root(),
                "ls", "-1", files.absolutePath
        ).timeout(10000).env().asyncOrFail(ByteArrayOutputStream()) { out ->
            val lines = out.toByteArray().inputStream().reader().readLines()
            log.d(lines)
            assertTrue(lines.contains("dir1"))
        }.get()
        ProcessUtBuilder(
                FileUt.root(),
                "ls", "-1", files.absolutePath
        ).env().timeout(1, TimeUnit.MINUTES).asyncOrFail(ByteArrayOutputStream()) { out ->
            val lines = out.toByteArray().inputStream().reader().readLines()
            log.d(lines)
            assertTrue(lines.contains("dir2"))
        }.get()
        ProcessUtBuilder(
                FileUt.root(),
                "ls", "-1", files.absolutePath
        ).env("DEBUG=true").asyncOrFail(ByteArrayOutputStream()) { out ->
            val lines = out.toByteArray().inputStream().reader().readLines()
            log.d(lines)
            assertTrue(lines.contains("empty.dir"))
        }.get()
    }

    @Test
    fun testAsyncError02() {
        val e = With.exceptionOrNull {
            ProcessUtBuilder("notexists").asyncOrFail().get()
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
            val e = With.exceptionOrNull {
                ProcessUtBuilder(FileUt.pwd(), "sleep", "2")
                        .timeout(100, TimeUnit.MICROSECONDS).asyncOrFail().get()
            }
            val msg = e.toString()
            log.d(msg)
            assertTrue(e is ExecutionException)
            assertTrue(msg.contains("java.util.concurrent.TimeoutException"))
        }
        subtest {
            val e = With.exceptionOrNull {
                ProcessUtBuilder(FileUt.pwd(), "sleep", "2")
                        .timeout(100, TimeUnit.MICROSECONDS).asyncOrFail().get()
            }
            val msg = e.toString()
            log.d(msg)
            assertTrue(e is ExecutionException)
            assertTrue(msg.contains("java.util.concurrent.TimeoutException"))
        }
        // Check the other shortcut methods for coverage.
        subtest {
            assertTrue(With.exceptionOrNull {
                ProcessUtBuilder(FileUt.root(), "sleep", "2")
                        .timeout(100, TimeUnit.MICROSECONDS).asyncOrFail().get()
            }.toString().contains("java.util.concurrent.TimeoutException"))
        }
    }

    @Test
    fun testAsyncError03() {
        subtest {
            assertTrue(With.exceptionOrNull {
                ProcessUtBuilder("true").async {
                    throw AssertionError()
                }.get()
            } is ExecutionException)
        }
        subtest {
            try {
                val rc = ProcessUtBuilder("false").async().get()
                assertEquals(1, rc)
            } catch (e: Throwable) {
                fail()
            }
        }
        subtest {
            try {
                ProcessUtBuilder("false").asyncOrFail().get()
            } catch (e: Throwable) {
                val msg = "$e"
                log.d(msg)
                assertTrue(msg.contains("rc=1"))
                assertTrue(msg.contains("AssertionError"))
            }
        }
        subtest {
            try {
                ProcessUtBuilder("notexists").asyncOrFail().get()
            } catch (e: Throwable) {
                val msg = "$e"
                log.d(msg)
                assertTrue(msg.contains("No such file"))
                assertTrue(msg.contains("notexists"))
            }
        }

    }

    @Test
    fun testBacktick01() {
        subtest {
            val files = testResDir.file("files")
            val ret = ProcessUt.backtick("ls", "-1", files.absolutePath)
            log.d(ret)
            assertTrue(Regex("(?m)^empty\\.dir$").find(ret) != null)
        }
        subtest {
            val files = testResDir.file("files")
            val ret = ProcessUt.backtick("ls", listOf("-1", files.absolutePath))
            log.d(ret)
            assertTrue(Regex("(?m)^empty\\.dir$").find(ret) != null)
        }
        subtest {
            val data = RandomUt.getWords(100 * 1000, 0, 40).joinln()
            val output = ByteArrayOutputStream()
            output.use { out ->
                data.byteInputStream().use { input ->
                    val ret = ProcessUtBuilder("cat").input(input).out(out).async().get()
                    assertEquals(0, ret)
                }
            }
            log.d("# output.size()=${output.size()}")
            assertEquals(data, output.toString())
        }
        subtest {
            val file = testResDir.file("html/manual.html")
            val output = ByteArrayOutputStream()
            output.use { out ->
                val ret = ProcessUt.backtick(out, FileUt.pwd(), "cat", file.absolutePath)
                assertEquals(0, ret)
            }
            log.d("# output.size()=${output.size()}")
            assertEquals(file.readText(), output.toString())
        }
        subtest {
            val ret = ProcessUt.backtick("find", listOf("/usr/lib/"))
            val lines = ret.lines().size
            log.d("# ret.lines=${lines}, ret.length=${ret.length}")
            assertTrue("$lines", lines > 10 * 1000)
            assertTrue("${ret.length}", ret.length > 1000 * 1000)
        }
    }
}
