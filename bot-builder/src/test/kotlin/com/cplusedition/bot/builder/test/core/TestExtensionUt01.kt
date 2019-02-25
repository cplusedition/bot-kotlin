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
import com.cplusedition.bot.core.FileUtil.Companion.FileUt
import com.cplusedition.bot.core.ProcessUtil.Companion.ProcessUt
import com.cplusedition.bot.core.RandomUtil.Companion.RandomUt
import com.cplusedition.bot.core.StructUtil.Companion.StructUt
import com.cplusedition.bot.core.WithUtil.Companion.With
import com.cplusedition.bot.core.WithoutUtil.Companion.Without
import com.cplusedition.bot.core.XMLUtil.Companion.XMLUt
import org.junit.Assert.*
import org.junit.Test
import java.io.*
import java.nio.file.attribute.FileTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.locks.ReentrantLock
import java.util.zip.ZipEntry

class TestExtensionUt01 : TestBase() {

    @Test
    fun testCollection01() {
        subtest {
            assertTrue(1.isOdd())
            assertFalse(2.isOdd())
        }
        subtest {
            val list = mutableListOf(1, 2, 3)
            assertTrue(list.addAll(7, 8))
            assertEquals(5, list.size)
            assertEquals(8, list.removeLast())
            assertEquals(7, list.removeLast())
            assertEquals(3, list.removeLast())
            assertEquals(2, list.removeLast())
            assertEquals(1, list.removeLast())
            assertEquals(null, list.removeLast())
            assertEquals(null, list.removeLast())
        }
        subtest("join") {
            assertEquals("", listOf<String>().join(""))
            assertEquals("", listOf<String>().join("/"))
            assertEquals("1", listOf(1).join("/"))
            assertEquals("123", listOf(1, 2, 3).join(""))
            assertEquals("1/2", listOf(1, 2).join("/"))
            assertEquals("1/2/3", listOf(1, 2, 3).join("/"))
            assertEquals("", arrayOf<String>().join(""))
            assertEquals("", arrayOf<String>().join("/"))
            assertEquals("1", arrayOf(1).join("/"))
            assertEquals("123", arrayOf(1, 2, 3).join(""))
            assertEquals("1/2", arrayOf(1, 2).join("/"))
            assertEquals("1/2/3", arrayOf(1, 2, 3).join("/"))
        }
        subtest("joinln") {
            assertEquals("", listOf<String>().joinln())
            assertEquals("1", listOf(1).joinln())
            assertEquals("1\n2", listOf(1, 2).joinln())
            assertEquals("1\n2\n3", listOf(1, 2, 3).joinln())
            assertEquals("", arrayOf<String>().joinln())
            assertEquals("1", arrayOf(1).joinln())
            assertEquals("1\n2", arrayOf(1, 2).joinln())
            assertEquals("1\n2\n3", arrayOf(1, 2, 3).joinln())
        }
        subtest("joinPath") {
            assertEquals("", listOf<String>().joinPath())
            assertEquals("1", listOf(1).joinPath())
            assertEquals("1/2", listOf(1, 2).joinPath())
            assertEquals("1/2/3", listOf(1, 2, 3).joinPath())
            assertEquals("", arrayOf<String>().joinPath())
            assertEquals("1", arrayOf(1).joinPath())
            assertEquals("1/2", arrayOf(1, 2).joinPath())
            assertEquals("1/2/3", arrayOf(1, 2, 3).joinPath())
        }
        subtest {
            assertEquals("1/2", Pair(1, "2").join("/"))
        }
        subtest {
            val map = mapOf(1 to "1", 2 to "2", 3 to "3").map { k, v ->
                when (k) {
                    1 -> null
                    2 -> v
                    else -> "x"
                }
            }.add(
                mapOf(
                    8 to "9",
                    9 to "8"
                )
            )
            assertEquals(4, map.size)
            assertTrue(map[1] == null)
            assertTrue(map[2] == "2")
            assertTrue(map[3] == "x")
            assertTrue(map[4] == null)
            assertTrue(map[8] == "9")
            assertTrue(map[9] == "8")
        }
    }

    @Test
    fun testDom01() {
        val file = testResDir.file("html/manual.html")
        val document = XMLUt.getDocumentBuilder().parse(file)
        assertEquals("html", document.documentElement.tagName)
        val body = document.documentElement.getElementsByTagName("body").elements().iterator().next()
        assertEquals(2, document.documentElement.childNodes.elements().count())
        assertEquals(92, ElementListIterable(body.getElementsByTagName("div")).count())
        assertEquals(3, document.documentElement.childNodes.nodes().count())
        assertEquals(0, document.documentElement.getElementsByTagName("XXX").elements().count())
        assertEquals(0, document.documentElement.getElementsByTagName("XXX").nodes().count())
    }

    @Test
    fun testWithStream01() {
        val file = testResDir.file("html/manual.html")
        subtest {
            With.inputStream(file) { input ->
                val data = FileUt.asBytes(input)
                assertEquals(19072, data.size)
                val tmpfile = tmpFile()
                With.outputStream(tmpfile) { output ->
                    output.write(data)
                }
                assertFalse(FileUt.diff(file, tmpfile))
            }
            assertEquals(19072, With.inputStream<Int>(file) {
                val data = FileUt.asBytes(it)
                var tmp: File? = null
                val size = With.tmpdir<Int> { tmpdir ->
                    val tmpfile = tmpdir.file("test")
                    tmp = tmpfile
                    val size = With.outputStream<Int>(tmpfile) {
                        it.write(data)
                        data.size
                    }
                    assertTrue(tmp!!.exists())
                    assertFalse(FileUt.diff(file, tmpfile))
                    size
                }
                assertFalse(tmp!!.exists())
                size
            })
            With.inputStream(FileInputStream(file)) { input ->
                val data = FileUt.asBytes(input)
                assertEquals(19072, data.size)
                val tmpfile = tmpFile()
                With.outputStream(FileOutputStream(tmpfile)) { output ->
                    output.write(data)
                }
                assertEquals(19072L, tmpfile.length())
            }
            assertEquals(19072, With.inputStream<Int>(FileInputStream(file)) {
                val data = FileUt.asBytes(it)
                var tmp: File? = null
                val size = With.tmpfile<Int> { tmpfile ->
                    tmp = tmpfile
                    val size = With.outputStream<Int>(FileOutputStream(tmpfile)) {
                        it.write(data)
                        data.size
                    }
                    assertTrue(tmp!!.exists())
                    assertFalse(FileUt.diff(file, tmpfile))
                    size
                }
                assertFalse(tmp!!.exists())
                size
            })
        }
        subtest {
            assertTrue(With.throwable { With.inputStream(file) { throw UnknownError() } } is UnknownError)
            assertTrue(With.throwable { With.inputStream<Int>(file) { throw UnknownError() } } is UnknownError)
            assertTrue(With.throwable { With.inputStream(FileInputStream(file)) { throw UnknownError() } } is UnknownError)
            assertTrue(With.throwable { With.inputStream<Int>(FileInputStream(file)) { throw UnknownError() } } is UnknownError)
            assertTrue(With.throwable { With.outputStream(tmpFile()) { throw UnknownError() } } is UnknownError)
            assertTrue(With.throwable { With.outputStream<Int>(tmpFile()) { throw UnknownError() } } is UnknownError)
            assertTrue(With.throwable { With.outputStream(FileOutputStream(tmpFile())) { throw UnknownError() } } is UnknownError)
            assertTrue(With.throwable { With.outputStream<Int>(FileOutputStream(tmpFile())) { throw UnknownError() } } is UnknownError)
        }
    }

    @Test
    fun testWithBuffered01() {
        val file = testResDir.file("html/manual.html")
        subtest {
            val tmpfile = tmpFile()
            With.printWriter(tmpfile) {
                it.print(file.readText())
            }
            assertFalse(FileUt.diff(file, tmpfile))
        }
        subtest {
            val tmpfile = tmpFile()
            With.bufferedReader(file) { r ->
                With.bufferedWriter(tmpfile) { w ->
                    while (true) {
                        val line = r.readLine() ?: break
                        w.appendln(line)
                    }
                }
            }
            assertEquals(file.readText().trim(), tmpfile.readText().trim())
        }
        subtest {
            val tmpfile = tmpFile()
            With.bufferedWriter(tmpfile, Charsets.UTF_16) { w ->
                With.bufferedReader(file, Charsets.UTF_8) { r ->
                    while (true) {
                        val line = r.readLine() ?: break
                        w.appendln(line)
                    }
                }
            }
            assertEquals(file.readText().trim(), tmpfile.readText(Charsets.UTF_16).trim())
            val tmpfile2 = tmpFile()
            With.bufferedWriter(tmpfile2, Charsets.UTF_16) { w ->
                With.lines(tmpfile, Charsets.UTF_16) {
                    w.appendln(it)
                }
            }
            assertFalse(FileUt.diff(tmpfile, tmpfile2))
        }
    }

    @Test
    fun testWithBytes01() {
        val file = testResDir.file("html/manual.html")
        subtest {
            var dir: File? = null
            With.tmpdir { tmpdir ->
                dir = tmpdir
                assertTrue(dir!!.exists())
                val buf = ByteArrayOutputStream()
                With.bytes(file) { b, len ->
                    buf.write(b, 0, len)
                }
                assertArrayEquals(file.readBytes(), buf.toByteArray())
            }
            assertTrue(dir != null && !dir!!.exists())
        }
        subtest {
            var tmp: File? = null
            With.tmpfile { tmpfile ->
                tmp = tmpfile
                val buf = ByteArrayOutputStream()
                With.bytes(file, 16000) { b, len ->
                    buf.write(b, 0, len)
                }
                assertTrue(tmp!!.exists())
                assertArrayEquals(file.readBytes(), buf.toByteArray())
            }
            assertFalse(tmp!!.exists())
        }
        subtest {
            val tmpfile = tmpFile()
            With.bufferedWriter(tmpfile) { w ->
                With.lines(file) { line ->
                    w.appendln(line)
                }
            }
            assertEquals(file.readText().trim(), tmpfile.readText().trim())
            var first = true
            With.rewriteLines(tmpfile) {
                if (first) {
                    first = false
                    "12345"
                } else null
            }
            assertEquals("12345\n", tmpfile.readText())
            With.rewriteText(tmpfile) {
                "abcd"
            }
            assertEquals("abcd", tmpfile.readText())
        }
    }

    @Test
    fun testWithTmp01() {
        subtest {
            var tmp: File? = null
            assertTrue(With.throwable {
                With.tmpdir {
                    tmp = it
                    assertTrue(tmp!!.exists())
                    throw UnknownError()
                }
            } is UnknownError)
            assertFalse(tmp!!.exists())
        }
        subtest {
            var tmp: File? = null
            assertTrue(With.throwable {
                With.tmpfile {
                    tmp = it
                    it.writeText("123")
                    assertTrue(tmp!!.exists())
                    throw UnknownError()
                }
            } is UnknownError)
            assertFalse(tmp!!.exists())
        }
        subtest {
            var tmp: File? = null
            assertTrue(With.throwable {
                With.tmpdir<Int> {
                    tmp = it
                    assertTrue(tmp!!.exists())
                    throw UnknownError()
                }
            } is UnknownError)
            assertFalse(tmp!!.exists())
        }
        subtest {
            var tmp: File? = null
            assertTrue(With.throwable {
                With.tmpfile<Int> {
                    tmp = it
                    it.writeText("123")
                    assertTrue(tmp!!.exists())
                    throw UnknownError()
                }
            } is UnknownError)
            assertFalse(tmp!!.exists())
        }
    }

    @Test
    fun testWithZip01() {
        subtest {
            val zipfile = tmpFile(suffix = ".zip")
            var expected = 0
            With.zipOutputStream(zipfile) {
                testResDir.walker.files { file, rpath ->
                    ++expected
                    val entry = ZipEntry(rpath)
                    entry.lastModifiedTime = FileTime.fromMillis(file.lastModified())
                    it.putNextEntry(entry)
                    FileUt.copy(it, file)
                }
            }
            assertTrue(zipfile.exists())
            var found = false
            var count = 0
            With.zipInputStream(zipfile) { input, entry ->
                ++count
                if (entry.name.endsWith("manual.html")) found = true
                val b = input.readBytes()
                assertArrayEquals(File(testResDir, entry.name).readBytes(), b)
            }
            assertEquals(expected, count)
            assertTrue(found)
        }
    }

    @Test
    fun testWithError01() {
        Without.exceptionOrFail { With.nullOrFail { null } }
        With.exceptionOrFail { With.nullOrFail { "error" } }
        assertTrue(With.exception { throw IOException() } is IOException)
        assertTrue(With.exception {} == null)
        assertTrue(With.throwable { With.exception { throw UnknownError() } } is UnknownError)
        assertTrue(With.throwable {} == null)
        With.exceptionOrFail { With.exceptionOrFail { } }
        With.exceptionOrFail { throw IOException() }
        With.throwableOrFail { With.throwableOrFail { } }
        With.throwableOrFail { Without.exceptionOrFail { throw Error() } }
    }

    @Test
    fun testWithBackup01() {
        subtest {
            With.tmpfile { outfile ->
                With.tmpfile { backupfile ->
                    outfile.bufferedWriter().use { it.write("123testing") }
                    With.backup(outfile, backupfile) { _, _ ->
                        outfile.bufferedWriter().use { it.write("testing123") }
                    }
                    assertTrue(outfile.exists())
                    assertTrue(backupfile.exists())
                    assertEquals("123testing", backupfile.readText())
                    assertEquals("testing123", outfile.readText())
                }
            }
        }

        subtest {
            With.tmpfile { outfile ->
                With.tmpfile { backupfile ->
                    outfile.bufferedWriter().use { it.write("123testing") }
                    With.exceptionOrFail {
                        With.backup(outfile, backupfile) { _, _ ->
                            throw IOException("Expected exception")
                        }
                    }
                    assertTrue(outfile.exists())
                    assertFalse(backupfile.exists())
                    assertEquals("123testing", outfile.readText())
                }
            }
        }
    }

    @Test
    fun testWithBackup02() {
        subtest {
            With.tmpfile { outfile ->
                With.tmpfile { backupfile ->
                    outfile.bufferedWriter().use { it.write("123testing") }
                    With.backup(outfile, backupfile) { dstfile, srcfile ->
                        val s = srcfile.readText()
                        dstfile.bufferedWriter().use {
                            it.write(s)
                            it.write("testing123")
                        }
                    }
                    assertTrue(outfile.exists())
                    assertTrue(backupfile.exists())
                    assertEquals("123testing", backupfile.readText())
                    assertEquals("123testingtesting123", outfile.readText())
                }
            }
        }

        subtest {
            With.tmpfile { outfile ->
                With.tmpfile { backupfile ->
                    outfile.bufferedWriter().use { it.write("123testing") }
                    With.exceptionOrFail {
                        With.backup(outfile, backupfile) { dstfile, _ ->
                            dstfile.bufferedWriter().use { it.write("You see me not") }
                            throw IOException("Expected exception")
                        }
                    }
                    assertTrue(outfile.exists())
                    assertFalse(backupfile.exists())
                    assertEquals("123testing", outfile.readText())
                }
            }
        }
    }

    @Test
    fun testWithBackup03() {
        subtest {
            With.tmpfile { outfile ->
                outfile.bufferedWriter().use { it.write("123testing") }
                With.backup(outfile) { dstfile, srcfile ->
                    val s = srcfile.readText()
                    dstfile.bufferedWriter().use {
                        it.write(s)
                        it.write("testing123")
                    }
                }
                assertTrue(outfile.exists())
                assertEquals("123testingtesting123", outfile.readText())
            }
        }

        subtest {
            With.tmpfile { outfile ->
                outfile.bufferedWriter().use { it.write("123testing") }
                With.exceptionOrFail {
                    With.backup(outfile) { dstfile, srcfile ->
                        assertEquals("123testing", srcfile.readText())
                        dstfile.bufferedWriter().use { it.write("You see me not") }
                        throw IOException("Expected exception")
                    }
                }
                assertTrue(outfile.exists())
                assertEquals("123testing", outfile.readText())
            }
        }
    }

    @Test
    fun testRewrite01() {
        subtest {
            val outfile = tmpFile()
            try {
                outfile.bufferedWriter().use { it.write("123testing\nline2\nline3\nline4") }
                Without.exceptionOrFail {
                    With.rewriteLines(outfile) X@{ line ->
                        if (line.contains("testing")) return@X null
                        "updated $line"
                    }
                }
                val lines = outfile.readLines()
                assertEquals(3, lines.size)
                assertEquals("updated line4", lines.last())
            } finally {
                outfile.delete()
            }
        }

        subtest {
            val outfile = tmpFile()
            val input = "123testing\nline2\nline3\nline4"
            try {
                outfile.bufferedWriter().use { it.write(input) }
                With.exceptionOrFail {
                    With.rewriteLines(outfile) X@{ line ->
                        if (line.contains("testing")) return@X null
                        throw IOException()
                    }
                }
                assertEquals(input, outfile.readText())
            } finally {
                outfile.delete()
            }
        }
    }

    @Test
    fun testWithShuffle01() {
        val ret = ArrayList<String>()
        val data = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
        With.shuffle(ret) {
            it.addAll(data)
        }
        log.d(ret)
        assertFalse(StructUt.equals(data, ret))
    }

    @Test
    fun testWithLock01() {
        val lock = ReentrantLock()
        val ret = mutableListOf(1, 2, 3, 4, 5)
        val out = ArrayList<Int>()
        val pool = Executors.newCachedThreadPool()
        val done = CountDownLatch(5)
        for (i in 0 until 5) {
            pool.submit {
                With.lock(lock) {
                    RandomUt.sleep(25, 30)
                    val v = ret.removeLast()
                    out.add(v!!)
                    log.d("$v")
                    done.countDown()
                }
            }
        }
        done.await()
        assertEquals(0, ret.size)
        assertTrue(StructUt.equals(listOf(5, 4, 3, 2, 1), out))
    }

    @Test
    fun testWithSync01() {
        subtest {
            val ret = ArrayList<Int>()
            val pool = Executors.newCachedThreadPool()
            With.sync {
                pool.submit {
                    RandomUt.sleep(50, 100)
                    ret.add(1)
                    it()
                }
            }
            With.sync {
                pool.submit {
                    ret.add(2)
                    it()
                }
            }
            assertTrue(StructUt.equals(listOf(1, 2), ret))
        }
        subtest {
            val ret = ArrayList<Int>()
            val pool = Executors.newCachedThreadPool()
            ret.add(With.sync<Int> {
                pool.submit {
                    ProcessUt.sleep(50)
                    it(1)
                }
            })
            ret.add(With.sync<Int> {
                pool.submit {
                    it(2)
                }
            })
            assertTrue(StructUt.equals(listOf(1, 2), ret))
        }
    }

    @Test
    fun testWithoutException01() {
        assertTrue(Without.exception { } != null)
        assertTrue(Without.exception { 1 } == 1)
        Without.exceptionOrFail { 1 }
        assertNull(Without.exception { throw Exception() })
        assertTrue(Without.throwable { } != null)
        assertEquals(123, Without.throwable { 123 })
        Without.throwableOrFail { 123 }
        assertNull(Without.throwable { throw UnknownError() })
        With.exceptionOrFail { Without.exceptionOrFail { throw Exception() } }
        With.exceptionOrFail { Without.throwableOrFail { throw Exception() } }
    }

    @Test
    fun testWithoutComment01() {
        With.tmpfile {
            it.writeText("no comment\n\n# with comment\nline 1\n\n\n  # line2")
            var nocomments = 0
            Without.comments(it) {
                ++nocomments
            }
            assertEquals(2, nocomments)
        }
        With.tmpfile {
            it.writeText("// comment\n\nno comment\n// with comment\nline 1\n\n\n  // line2")
            var nocomments = 0
            Without.comments(it, "//") {
                ++nocomments
            }
            assertEquals(2, nocomments)

        }
    }
}