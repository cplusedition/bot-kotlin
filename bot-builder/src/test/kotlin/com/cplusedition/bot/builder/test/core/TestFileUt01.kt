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

import com.cplusedition.bot.builder.Copy
import com.cplusedition.bot.builder.Fileset
import com.cplusedition.bot.builder.Fileset.RegexFilter
import com.cplusedition.bot.builder.test.zzz.TestBase
import com.cplusedition.bot.core.*
import com.cplusedition.bot.core.FileUtil.Companion.FileUt
import com.cplusedition.bot.core.ProcessUtil.Companion.ProcessUt
import com.cplusedition.bot.core.RandomUtil.Companion.RandomUt
import com.cplusedition.bot.core.WithUtil.Companion.With
import com.cplusedition.bot.core.WithoutUtil.Companion.Without
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.io.IOException

class TestFileUt01 : TestBase() {


    @Test
    fun testBasic01() {
        log.run {
            d("# $trashDir")
            d("# ${conf.builder.dir}")
            d("# $testResDir")
        }
        assertTrue(testResDir.file("files/empty.txt").exists())
        assertTrue(tmpDir().listOrEmpty().isEmpty())
    }

    @Test
    fun testChangeSuffix01() {
        assertEquals("b-test.css", File("b.html").changeSuffix("-test.css").name)
    }

    @Test
    fun testBasepath01() {
        fun check(path: String, dir: String?, name: String, base: String, suffix: String) {
            val basepath = Basepath.from(path)
            assertEquals(path, dir, basepath.dir)
            assertEquals(path, name, basepath.name)
            assertEquals(path, base, basepath.base)
            assertEquals(path, suffix, basepath.suffix)
            assertEquals(path, suffix.toLowerCase(), basepath.lcSuffix)
            assertEquals(path, dir, Basepath.dir(path))
            assertEquals(path, name, Basepath.name(path))
            assertEquals(path, base, Basepath.base(path))
            assertEquals(path, suffix, Basepath.suffix(path))
            assertEquals(path, suffix.toLowerCase(), Basepath.lcSuffix(path))
            assertEquals(path, basepath.ext, Basepath.ext(path))
            assertEquals(path, basepath.lcExt, Basepath.lcExt(path))
            assertEquals(path, basepath.ext, if (suffix.isEmpty()) null else suffix.substring(1))
            assertEquals(path, basepath.lcExt, if (suffix.isEmpty()) null else suffix.substring(1).toLowerCase())
            val file = basepath.file
            assertEquals(Basepath.from(file.absolutePath), Basepath.from(file))
            assertEquals(path, name, file.name)
            assertEquals(path, suffix, file.suffix)
            assertEquals(path, suffix.toLowerCase(), file.lcSuffix)
        }
        check("", null, "", "", "")
        check(".abc", null, ".abc", "", ".abc")
        check("abc", null, "abc", "abc", "")
        check("abc.", null, "abc.", "abc", ".")
        check("abc/", null, "abc", "abc", "")
        check("a/b/c.d/", "a/b", "c.d", "c", ".d")
        check("abc.123", null, "abc.123", "abc", ".123")
        check("abc.123.txt", null, "abc.123.txt", "abc.123", ".txt")
        check("/abc.123", "", "abc.123", "abc", ".123")
        check("/a/abc.123", "/a", "abc.123", "abc", ".123")
        check("/a/abc.123", "/a", "abc.123", "abc", ".123")
        check("/a/b/c/abc.123", "/a/b/c", "abc.123", "abc", ".123")
        check("a/abc.123", "a", "abc.123", "abc", ".123")
        check("a/abc.123/abc.txt", "a/abc.123", "abc.txt", "abc", ".txt")
    }

    @Test
    fun testBasepath02() {
        fun check(basepath: Basepath, dir: String?, name: String, base: String, suffix: String) {
            assertEquals(dir, basepath.dir)
            assertEquals(name, basepath.name)
            assertEquals(base, basepath.base)
            assertEquals(suffix, basepath.suffix)
        }
        check(Basepath.from("abc.123").changeName("123.abc"), null, "123.abc", "123", ".abc")
        check(Basepath.from("abc.123").changeName("123abc"), null, "123abc", "123abc", "")
        check(Basepath.from("abc123").changeName("123.abc"), null, "123.abc", "123", ".abc")
        check(Basepath.from("abc.123").changeBase("cde"), null, "cde.123", "cde", ".123")
        check(Basepath.from("abc.123").changeSuffix(".cde"), null, "abc.cde", "abc", ".cde")
        check(Basepath.from("a/abc/abc.123").changeName("123"), "a/abc", "123", "123", "")
        check(Basepath.from("a/abc/abc.123").changeBase("cde"), "a/abc", "cde.123", "cde", ".123")
        check(Basepath.from("a/abc/abc.123").changeSuffix("cde"), "a/abc", "abccde", "abccde", "")
        assertEquals(Basepath.from("a/abc"), Basepath.from("a/123.abc").changeName("abc"))
        assertEquals(Basepath.from("a/abc.abc"), Basepath.from("a/123.abc").changeBase("abc"))
        assertEquals(Basepath.from("a/123cde"), Basepath.from("a/123.abc").changeSuffix("cde"))
        assertEquals("a/abc", Basepath.changeName("a/123.abc", "abc"))
        assertEquals("a/abc.abc", Basepath.changeBase("a/123.abc", "abc"))
        assertEquals("a/123cde", Basepath.changeSuffix("a/123.abc", "cde"))
        assertEquals(File("a/abc"), File("a/123.abc").changeName("abc"))
        assertEquals(File("a/abc.abc"), File("a/123.abc").changeBase("abc"))
        assertEquals(File("a/123cde"), File("a/123.abc").changeSuffix("cde"))
    }

    @Test
    fun testMkdirs01() {
        assertEquals(File("a/b/c.html").absoluteFile, File("a/c/.././b/c.html").clean())
        assertEquals("a/b/c.html", FileUt.rpathOrNull(File("a/c/.././b/c.html").clean(), FileUt.pwd()))
        assertEquals(null, FileUt.rpathOrNull(File("../a/c/.././b/c.html").clean(), FileUt.pwd()))
        Without.exceptionOrFail { FileUt.pwd().mkparentOrFail() }
        With.exceptionOrFail { File("/").mkparentOrFail() }
        Without.exceptionOrFail { FileUt.pwd().mkparentOrFail("test") }
        With.exceptionOrFail { File("/test").mkparentOrFail("..") }
        assertNotNull(FileUt.pwd().mkparentOrNull())
        assertNull(File("/").mkparentOrNull())
        assertNotNull(FileUt.pwd().mkparentOrNull("test"))
        assertNull(File("/test").mkparentOrNull(".."))
        Without.exceptionOrFail { tmpDir().mkdirsOrFail().existsOrFail() }
        Without.exceptionOrFail { File("/").mkdirsOrFail().existsOrFail() }
        With.exceptionOrFail { File("/notexists").mkdirsOrFail() }
        Without.exceptionOrFail { tmpDir().mkdirsOrFail("test/a/b.txt").existsOrFail() }
        With.exceptionOrFail { File("/").mkdirsOrFail("notexists") }
        assertNotNull(tmpDir().mkdirsOrNull()?.existsOrNull())
        assertNotNull(File("/").mkdirsOrNull()?.existsOrNull())
        assertNull(File("/notexists").mkdirsOrNull())
        assertNotNull(tmpDir().mkdirsOrNull("test")?.existsOrNull())
        assertNull(File("/").mkdirsOrNull("notexists/test"))
        assertNull(File("/").mkdirsOrNull("notexists/test"))
        val tmpdir = tmpDir()
        val tmptestdir = tmpdir.file("test")
        tmptestdir.mkdirsOrFail("a/b.txt")
        tmptestdir.mkdirsOrFail("c")
        tmptestdir.file("c/d.txt").writeText("xxx")
        Without.exceptionOrFail { tmpdir.existsOrFail("test/a/b.txt") }
        assertTrue(tmpdir.file("test").deleteSubtrees())
        assertNotNull(tmpdir.existsOrNull("test"))
        assertNull(tmpdir.existsOrNull("test/a"))
        assertNull(tmpdir.existsOrNull("test/c"))
        assertNull(tmpdir.existsOrNull("test/c/d.txt"))
        assertTrue(tmptestdir.listOrEmpty().isEmpty())
    }

    @Test
    fun testMkdirs02() {
        val tmpdir = tmpDir()
        assertEquals(System.getProperty("user.name"), FileUt.home().name)
        assertEquals(false, FileUt.home("notexists").isDirectory)
        assertEquals(FileUt.home(), FileUt.mkdirs(FileUt.home().absolutePath))
        assertTrue(FileUt.mkdirs(tmpdir.absolutePath) != null)
        assertTrue(FileUt.mkdirs(tmpdir.absolutePath, "a", "b", "c") != null)
        assertTrue(tmpdir.file("a", "b", "c").isDirectory)
        assertTrue(FileUt.mkdirs("/notexists") == null)
        assertTrue(FileUt.mkdirs("/", "notexists") == null)
        assertTrue(FileUt.mkdirs(tmpdir.absolutePath, "a", "b", "c") != null)
        assertTrue(FileUt.mkparent(tmpdir.absolutePath, "a", "b", "c") != null)
        assertTrue(FileUt.mkparent(tmpdir.absolutePath, "d", "e") != null)
        assertTrue(FileUt.mkparent(FileUt.root().absolutePath) == null)
        assertEquals(FileUt.root("home"), FileUt.cleanFile("/home/../test/.//../home/"))
        assertEquals(FileUt.root("home"), FileUt.cleanFile("/", "home", "..", "test", ".", "..", "home"))
    }

    @Test
    fun testCleanPath01() {
        assertEquals("", FileUt.cleanPath("").toString())
        assertEquals("/", FileUt.cleanPath("/").toString())
        assertEquals("/", FileUt.cleanPath("//").toString())
        assertEquals("/", FileUt.cleanPath("/////").toString())
        assertEquals("/", FileUt.cleanPath("/.").toString())
        assertEquals("/", FileUt.cleanPath("/./").toString())
        assertEquals("", FileUt.cleanPath(".").toString())
        assertEquals("", FileUt.cleanPath("./").toString())
        assertEquals("", FileUt.cleanPath("./.").toString())
        assertEquals("/", FileUt.cleanPath("/././/").toString())
        assertEquals("/", FileUt.cleanPath("//a/../").toString())
        assertEquals("/a/b/c", FileUt.cleanPath("/a/b/./c").toString())
        assertEquals("/a/b/c/", FileUt.cleanPath("/a/b/./c/").toString())
        assertEquals("/a/c/", FileUt.cleanPath("/a/b/../c/").toString())
        assertEquals("/a/c", FileUt.cleanPath("/a/b/../c").toString())
        assertEquals("/a/", FileUt.cleanPath("/a/b/..//.//c/..").toString())
        assertEquals("/a/", FileUt.cleanPath("/a/b/..//.//c/../").toString())
        assertEquals("..", FileUt.cleanPath("..").toString())
        assertEquals("../a", FileUt.cleanPath("../a").toString())
        assertEquals("../a/", FileUt.cleanPath("../a/.").toString())
        assertEquals("../a/", FileUt.cleanPath("../a/./").toString())
        assertEquals("../../", FileUt.cleanPath("../../.").toString())
        assertEquals("../..", FileUt.cleanPath("../a/b/../../..").toString())
        assertEquals("../../", FileUt.cleanPath("../a/b/../../..///").toString())
        assertEquals("..", FileUt.cleanPath("a/b/../../..").toString())
        assertEquals("../c", FileUt.cleanPath("a/b/../../../c").toString())
        assertEquals("/a/b", FileUt.cleanPath("////a////b").toString())
        assertEquals("/a/b/", FileUt.cleanPath("////a////b///").toString())
        assertEquals("a/b/c/d", FileUt.cleanPath("a/b/c/d").toString())
        assertEquals("/aaa/bb/cc/dd/", FileUt.cleanPath("/aaa/bb/cc/dd/").toString())
    }

    @Test
    fun testCleanPathSegments01() {
        class Tester : FileUtil() {
            fun test() {
                assertEquals("", cleanPathSegments("").join("/"))
                assertEquals("", cleanPathSegments("/").join("/"))
                assertEquals("", cleanPathSegments("//").join("/"))
                assertEquals("", cleanPathSegments("/////").join("/"))
                assertEquals("", cleanPathSegments("/.").join("/"))
                assertEquals("", cleanPathSegments("/./").join("/"))
                assertEquals("", cleanPathSegments(".").join("/"))
                assertEquals("", cleanPathSegments("./").join("/"))
                assertEquals("", cleanPathSegments("./.").join("/"))
                assertEquals("", cleanPathSegments("/././/").join("/"))
                assertEquals("", cleanPathSegments("//a/../").join("/"))
                assertEquals("a/b/c", cleanPathSegments("/a/b/./c").join("/"))
                assertEquals("a/b/c", cleanPathSegments("/a/b/./c/").join("/"))
                assertEquals("a/c", cleanPathSegments("/a/b/../c/").join("/"))
                assertEquals("a/c", cleanPathSegments("/a/b/../c").join("/"))
                assertEquals("a", cleanPathSegments("/a/b/..//.//c/..").join("/"))
                assertEquals("a", cleanPathSegments("/a/b/..//.//c/../").join("/"))
                assertEquals("..", cleanPathSegments("..").join("/"))
                assertEquals("../a", cleanPathSegments("../a").join("/"))
                assertEquals("../a", cleanPathSegments("../a/.").join("/"))
                assertEquals("../a", cleanPathSegments("../a/./").join("/"))
                assertEquals("../..", cleanPathSegments("../../.").join("/"))
                assertEquals("../..", cleanPathSegments("../a/b/../../..").join("/"))
                assertEquals("../..", cleanPathSegments("../a/b/../../..///").join("/"))
                assertEquals("..", cleanPathSegments("a/b/../../..").join("/"))
                assertEquals("../c", cleanPathSegments("a/b/../../../c").join("/"))
                assertEquals("a/b", cleanPathSegments("////a////b").join("/"))
                assertEquals("a/b", cleanPathSegments("////a////b///").join("/"))
                assertEquals("a/b/c/d", cleanPathSegments("a/b/c/d").join("/"))
                assertEquals("aaa/bb/cc/dd", cleanPathSegments("/aaa/bb/cc/dd/").join("/"))
            }
        }
        Tester().test()
    }

    @Test
    fun testRpathOrNull01() {
        val base = tmpDir()
        assertEquals("", FileUt.rpathOrNull(base, base))
        assertEquals(base.name, FileUt.rpathOrNull(base, base.parentFile))
        assertEquals("a", FileUt.rpathOrNull(base.file("a"), base))
        assertEquals("a/b", FileUt.rpathOrNull(base.file("a", "b"), base))
        assertEquals("a/c", FileUt.rpathOrNull(base.file("a/b/..//./c/"), base))
        assertEquals(null, FileUt.rpathOrNull(base.parentFile, base))
        assertEquals(null, FileUt.rpathOrNull(base.parentFile.file("test"), base))
        assertEquals("", FileUt.rpathOrNull(base.parentFile.file(base.name), base))
        assertEquals("", FileUt.rpathOrNull(FileUt.root(), FileUt.root()))
        assertEquals("a", FileUt.rpathOrNull(FileUt.root("a"), FileUt.root()))
        assertEquals("a/b", FileUt.rpathOrNull(FileUt.root("a", "b"), FileUt.root()))
        assertEquals("a/b", FileUt.rpathOrNull(FileUt.root("a", "b", ""), FileUt.root()))
        assertEquals("a/b", FileUt.rpathOrNull(FileUt.root("a/b/"), FileUt.root()))
        assertEquals(null, FileUt.rpathOrNull(base.file(".."), base))
        assertEquals("", FileUt.rpathOrNull(base.file("a/../"), base))
        assertEquals(null, FileUt.rpathOrNull(base.file("a/../.."), base))
        assertEquals(null, FileUt.rpathOrNull(base.file("a/../../"), base))
        assertEquals(null, FileUt.rpathOrNull(FileUt.root(".."), FileUt.root()))
        assertEquals(null, FileUt.rpathOrNull(FileUt.root(".."), FileUt.root("..")))
        assertEquals(null, FileUt.rpathOrNull(FileUt.root(), FileUt.root("..")))
        assertEquals(null, FileUt.rpathOrNull(FileUt.root(), FileUt.root("a")))
    }

    @Test
    fun testScan01() {
        val files = testResDir.file("files")
        subtest {
            val list = ArrayList<String>()
            FileUt.scan(files) { _, rpath ->
                list.add(rpath)
                true
            }
            log.d(list)
            assertEquals(16, list.size)
            assertTrue(list.indexOf("dir1") < list.indexOf("dir1/dir1a"))
            assertTrue(list.indexOf("dir1/dir1a") < list.indexOf("dir1/dir1a/file1a.txt"))
        }
        subtest("Ignore dir") {
            val list = ArrayList<String>()
            FileUt.scan(files) { _, rpath ->
                list.add(rpath)
                !rpath.startsWith("dir2")
            }
            log.d(list)
            assertEquals(10, list.size)
            assertTrue(list.indexOf("dir1") < list.indexOf("dir1/dir1a"))
            assertTrue(list.indexOf("dir1/dir1a") < list.indexOf("dir1/dir1a/file1a.txt"))
        }
        subtest("Ignore dir") {
            val list = ArrayList<String>()
            FileUt.scan(files) { _, rpath ->
                if (rpath.contains("dir2")) return@scan false
                list.add(rpath)
                true
            }
            log.d(list)
            assertEquals(8, list.size)
        }
        subtest("Ignore dir") {
            val list = ArrayList<String>()
            FileUt.scan(files, "prefix") { _, rpath ->
                if (rpath.contains("dir2")) return@scan false
                list.add(rpath)
                true
            }
            log.d(list)
            assertEquals(8, list.size)
            for (s in list) {
                assertTrue(s.startsWith("prefix/") && !s.startsWith("prefix//"))
            }
        }
    }

    @Test
    fun testWalk01() {
        val files = testResDir.file("files")
        subtest {
            val list = ArrayList<String>()
            files.walker.walk { _, rpath -> list.add(rpath) }
            log.d(list)
            assertEquals(16, list.size)
            assertTrue(list.indexOf("dir1") < list.indexOf("dir1/dir1a"))
            assertTrue(list.indexOf("dir1/dir1a") < list.indexOf("dir1/dir1a/file1a.txt"))
        }
        subtest {
            val list = ArrayList<String>()
            files.walker.walk { _, rpath ->
                if (!rpath.contains("dir2")) {
                    list.add(rpath)
                }
            }
            log.d(list)
            assertEquals(8, list.size)
        }
        subtest {
            val list = ArrayList<String>()
            files.walker.basepath("prefix").walk { _, rpath ->
                if (!rpath.contains("dir2")) {
                    list.add(rpath)
                }
            }
            log.d(list)
            assertEquals(8, list.size)
            for (s in list) {
                assertTrue(s.startsWith("prefix/") && !s.startsWith("prefix//"))
            }
        }
    }

    @Test
    fun testPreOrder01() {
        val files = testResDir.file("files")
        subtest {
            val list = ArrayList<String>()
            files.walker.walk { _, rpath ->
                list.add(rpath)
            }
            log.d(list)
            assertEquals(16, list.size)
            assertTrue(list.indexOf("dir1") < list.indexOf("dir1/dir1a"))
            assertTrue(list.indexOf("dir1/dir1a") < list.indexOf("dir1/dir1a/file1a.txt"))
        }
        subtest {
            val list = ArrayList<String>()
            files.walker.walk { _, rpath ->
                if (rpath.contains("dir2")) return@walk
                list.add(rpath)
            }
            log.d(list)
            assertEquals(8, list.size)
        }
        subtest {
            val list = ArrayList<String>()
            files.walker.basepath("prefix").walk { _, rpath ->
                if (rpath.contains("dir2")) return@walk
                list.add(rpath)
            }
            log.d(list)
            assertEquals(8, list.size)
            for (s in list) {
                assertTrue(s.startsWith("prefix/") && !s.startsWith("prefix//"))
            }
        }
    }

    @Test
    fun testBottomUp01() {
        val files = testResDir.file("files")
        subtest {
            val list = ArrayList<String>()
            files.walker.bottomUp().walk { _, rpath ->
                list.add(rpath)
            }
            log.d(list)
            assertEquals(16, list.size)
            assertTrue(list.indexOf("dir1") > list.indexOf("dir1/dir1a"))
            assertTrue(list.indexOf("dir1/dir1a") > list.indexOf("dir1/dir1a/file1a.txt"))
        }
        subtest {
            val list = ArrayList<String>()
            files.walker.bottomUp().walk { _, rpath ->
                if (rpath.contains("dir2")) return@walk
                list.add(rpath)
            }
            log.d(list)
            assertEquals(8, list.size)
        }
        subtest {
            val list = ArrayList<String>()
            files.walker.basepath("prefix").bottomUp().walk { _, rpath ->
                if (rpath.contains("dir2")) return@walk
                list.add(rpath)
            }
            log.d(list)
            assertEquals(8, list.size)
            for (s in list) {
                assertTrue(s.startsWith("prefix/") && !s.startsWith("prefix//"))
            }
        }
    }

    @Test
    fun testFiles01() {
        val files = testResDir.file("files")
        subtest {
            val list = ArrayList<String>()
            files.walker.files { _, rpath ->
                list.add(rpath)
            }
            log.d(list)
            assertEquals(11, list.size)
        }
        subtest {
            val list = ArrayList<String>()
            files.walker.files { _, rpath ->
                if (!rpath.contains("dir2")) {
                    list.add(rpath)
                }
            }
            log.d(list)
            assertEquals(5, list.size)
        }
        subtest {
            val list = ArrayList<String>()
            files.walker.basepath("prefix").files { _, rpath ->
                if (!rpath.contains("dir2")) {
                    list.add(rpath)
                }
            }
            log.d(list)
            assertEquals(5, list.size)
            for (s in list) {
                assertTrue(s.startsWith("prefix/") && !s.startsWith("prefix//"))
            }
        }
    }

    @Test
    fun testDirs01() {
        val files = testResDir.file("files")
        subtest {
            val list = ArrayList<String>()
            files.walker.bottomUp().dirs { _, rpath ->
                list.add(rpath)
            }
            log.d(list)
            assertEquals(5, list.size)
            assertTrue(list.indexOf("dir1/dir1a") < list.indexOf("dir1"))
        }
        subtest {
            val list = ArrayList<String>()
            files.walker.dirs { _, rpath ->
                list.add(rpath)
            }
            log.d(list)
            assertEquals(5, list.size)
            assertTrue(list.indexOf("dir1/dir1a") > list.indexOf("dir1"))
        }
        subtest {
            val list = ArrayList<String>()
            files.walker.dirs { _, rpath ->
                if (!rpath.contains("dir2")) {
                    list.add(rpath)
                }
            }
            log.d(list)
            assertEquals(3, list.size)
        }
        subtest {
            val list = ArrayList<String>()
            files.walker.basepath("prefix").dirs { _, rpath ->
                if (!rpath.contains("dir2")) {
                    list.add(rpath)
                }
            }
            log.d(list)
            assertEquals(3, list.size)
            for (s in list) {
                assertTrue(s.startsWith("prefix/") && !s.startsWith("prefix//"))
            }
        }
    }

    @Test
    fun testCollectorCollect01() {
        val files = testResDir.file("files")
        subtest {
            assertEquals(16, files.walker.collector(::Pair).collect().count())
            assertEquals(16, files.walker.collector(FilePathCollectors::fileCollector).collect().count())
            assertEquals(16, files.walker.collector(FilePathCollectors::pathCollector).collect().count())
            assertEquals(7, files.walker.collector(FilePathCollectors::fileCollector).collect { _, rpath ->
                rpath.startsWith("dir1")
            }.count())
            assertEquals(7, files.walker.pathCollector().collect { _, rpath ->
                rpath.startsWith("dir1")
            }.count())
            assertTrue(files.walker.fileCollector().collect().all { it is File })
            assertTrue(files.walker.pathCollector().collect().all { it is String })
            val list = files.walker.pathCollector().collect()
            assertTrue(list.indexOf("dir1") < list.indexOf("dir1/dir1a"))
            assertTrue(list.indexOf("dir1/dir1a") < list.indexOf("dir1/dir1a/file1a.txt"))
        }
    }

    @Test
    fun testCollectorFiles01() {
        val files = testResDir.file("files")
        subtest {
            assertEquals(11, files.walker.collector().files().count())
            assertEquals(11, files.walker.fileCollector().files().count())
            assertEquals(11, files.walker.pathCollector().files().count())
            assertEquals(5, files.walker.fileCollector().files { _, rpath ->
                rpath.startsWith("dir1")
            }.count())
            assertEquals(5, files.walker.pathCollector().files { _, rpath ->
                rpath.startsWith("dir1")
            }.count())
            assertTrue(files.walker.fileCollector().files().all { it is File })
            assertTrue(files.walker.pathCollector().files().all { it is String })
        }
    }

    @Test
    fun testCollectorDirs01() {
        val files = testResDir.file("files")
        subtest {
            assertEquals(5, files.walker.collector().dirs().count())
            assertEquals(5, files.walker.fileCollector().dirs().count())
            assertEquals(5, files.walker.pathCollector().dirs().count())
            assertEquals(2, files.walker.fileCollector().dirs { _, rpath ->
                rpath.startsWith("dir1")
            }.count())
            assertEquals(2, files.walker.pathCollector().dirs { _, rpath ->
                rpath.startsWith("dir1")
            }.count())
            assertTrue(files.walker.fileCollector().dirs().all { it is File })
            assertTrue(files.walker.pathCollector().dirs().all { it is String })
            val list = files.walker.pathCollector().dirs()
            assertTrue(list.indexOf("dir1") < list.indexOf("dir1/dir1a"))
        }
    }

    @Test
    fun testFind01() {
        val srcdir = projectRes("src")
        val file = srcdir.walker.find { file, rpath ->
            file.isFile && rpath.endsWith("/Builder.kt")
        }
        val ignoresdir = { _: File, rpath: String -> !rpath.contains("main") }
        val builderkt = { _: File, rpath: String -> rpath.endsWith("/Builder.kt") }
        //
        assertNotNull(file)
        assertTrue("${file!!.length()}", file.length() > 1024)
        //
        assertNull(srcdir.walker.find { _, _ -> false })
        assertNull(srcdir.walker.basepath("prefix").find { _, _ -> false })
        assertNull(srcdir.walker.ignoresDir(ignoresdir).find { _, _ -> false })
        assertNull(srcdir.walker.basepath("prefix").ignoresDir(ignoresdir).find { _, _ -> false })
        //
        assertNotNull(srcdir.walker.find(accept = builderkt))
        assertNotNull(srcdir.walker.basepath("prefix").find(builderkt))
        assertNotNull(srcdir.walker.ignoresDir(ignoresdir).find(builderkt))
        assertNotNull(srcdir.walker.basepath("prefix").ignoresDir(ignoresdir).find(builderkt))
        //
        Without.exceptionOrFail { srcdir.walker.findOrFail(accept = builderkt) }
        Without.exceptionOrFail {
            srcdir.walker.basepath("prefix").findOrFail { _, rpath ->
                rpath.startsWith("prefix/") && rpath.endsWith("Builder.kt")
            }
        }
        Without.exceptionOrFail {
            srcdir.walker.ignoresDir(ignoresdir).findOrFail { _, rpath ->
                rpath.endsWith("Builder.kt")
            }
        }
        Without.exceptionOrFail {
            srcdir.walker.basepath("prefix").ignoresDir { _, rpath ->
                !rpath.contains("main")
            }.findOrFail { _, rpath ->
                rpath.startsWith("prefix/") && rpath.endsWith("Builder.kt")
            }
        }
        With.exceptionOrFail { srcdir.walker.findOrFail { _, _ -> false } }
        With.exceptionOrFail {
            srcdir.walker.ignoresDir { _, rpath ->
                rpath.startsWith("main")
            }.findOrFail(builderkt)
        }
        With.exceptionOrFail { srcdir.walker.basepath("prefix").findOrFail { _, _ -> false } }
        With.exceptionOrFail {
            srcdir.walker.basepath("prefix").ignoresDir { _, rpath ->
                rpath.startsWith("prefix")
            }.findOrFail(builderkt)
        }
        With.exceptionOrFail { file.walker.findOrFail { f, _ -> f.isFile } }
    }

    @Test
    fun testCollect01() {
        val files = testResDir.file("files")
        subtest {
            val s =
                files.walker.ignoresDir { _, rpath -> rpath != "dir1" }.collect { file, _ -> file.isFile }
            log.d(s.map { (_, rpath) -> rpath }.iterator())
            assertEquals(5, s.count())
        }
        subtest {
            val s = files.walker.collect { file, _ -> file.isFile }
            log.d(s.map { (_, rpath) -> rpath }.iterator())
            assertEquals(11, s.count())
        }
        subtest {
            val ignoresdir1 = { _: File, rpath: String -> rpath != "dir1" && !rpath.startsWith("dir1") }
            val ignoresdirdir1 = { _: File, rpath: String -> rpath != "dir/dir1" && !rpath.startsWith("dir/dir1") }
            val isfile = { file: File, _: String -> file.isFile }
            val isdir1file = { f: File, rpath: String -> f.isFile && rpath.startsWith("dir1/") }
            assertEquals(11, files.walker.collect(includes = isfile).count())
            assertEquals(5, files.walker.collect(includes = isdir1file).count())
            //
            assertEquals(11, files.walker.collect { f, _ -> FileUt.fileFilter.accept(f) }.count())
            assertEquals(5, files.walker.collect { f, _ -> FileUt.notFileFilter.accept(f) }.count())
            assertEquals(5, files.walker.collect { f, _ -> FileUt.dirFilter.accept(f) }.count())
            assertEquals(11, files.walker.collect { f, _ -> FileUt.notDirFilter.accept(f) }.count())
            assertEquals(16, files.walker.collect { f, _ -> FileUt.everythingFilter.accept(f) }.count())
            //
            assertEquals(6, files.walker.ignoresDir(ignoresdir1).collect(FileUt.filePredicate).count())
            assertEquals(4, files.walker.ignoresDir(ignoresdir1).collect(FileUt.notFilePredicate).count())
            assertEquals(4, files.walker.ignoresDir(ignoresdir1).collect(FileUt.dirPredicate).count())
            assertEquals(6, files.walker.ignoresDir(ignoresdir1).collect(FileUt.notDirPredicate).count())
            assertEquals(10, files.walker.ignoresDir(ignoresdir1).collect(FileUt.everythingPredicate).count())
            //
            assertEquals(11, files.walker.basepath("dir").collect(FileUt.filePredicate).count())
            assertEquals(5, files.walker.basepath("dir").collect(FileUt.notFilePredicate).count())
            assertEquals(5, files.walker.basepath("dir").collect(FileUt.dirPredicate).count())
            assertEquals(11, files.walker.basepath("dir").collect(FileUt.notDirPredicate).count())
            assertEquals(16, files.walker.basepath("dir").collect(FileUt.everythingPredicate).count())
            assertTrue(files.walker.basepath("dir").collect(FileUt.everythingPredicate).all { (_, rpath) ->
                rpath.startsWith("dir/")
            })
            //
            assertEquals(
                6, files.walker.basepath("dir").ignoresDir(ignoresdirdir1)
                    .collect(FileUt.filePredicate).count()
            )
            assertEquals(
                4, files.walker.basepath("dir").ignoresDir(ignoresdirdir1)
                    .collect(FileUt.notFilePredicate).count()
            )
            assertEquals(
                4, files.walker.basepath("dir").bottomUp().ignoresDir(ignoresdirdir1)
                    .collect(FileUt.dirPredicate).count()
            )
            assertEquals(
                6, files.walker.basepath("dir").bottomUp().ignoresDir(ignoresdirdir1)
                    .collect(FileUt.notDirPredicate).count()
            )
            assertEquals(
                10, files.walker.basepath("dir").bottomUp().ignoresDir(ignoresdirdir1)
                    .collect(FileUt.everythingPredicate).count()
            )
        }
    }

    @Test
    fun testCopy01() {
        val tmpdir = tmpDir()
        val to = tmpdir.file("t")
        val file1 = tmpdir.file("file1.txt")
        val from = testResDir.file("files/dir2/file1.txt")
        subtest {
            assertFalse(to.exists())
            FileUt.copy(to, from)
            assertTrue(to.exists())
            assertTrue(With.exception { FileUt.copy(FileUt.root("notexists"), from) } is IOException)
            With.inputStream(from) { input1 ->
                val fromtext = FileUt.asString(input1)
                With.inputStream(to) { input2 ->
                    val tobytes = FileUt.asBytes(input2)
                    assertEquals(fromtext, String(tobytes))
                }
            }
        }
        subtest {
            FileUt.remove(listOf(file1))
            assertFalse(file1.exists())
            FileUt.copyto(tmpdir, from)
            assertTrue(file1.exists())

        }
        subtest {
            FileUt.remove(listOf(file1))
            assertFalse(file1.exists())
            FileUt.copyto(tmpdir, listOf(from))
            assertTrue(to.exists())
        }
        subtest {
            val s = from.parentFile.walker.collect(FileUt.filePredicate)
            val tmp = tmpDir()
            FileUt.copyto(tmp, s.map { it.first })
            val out = tmp.walker.collect(FileUt.filePredicate).map { it.first }
            assertEquals(s.count(), out.count())
            FileUt.remove(out)
            assertEquals(0, tmp.walker.collect(FileUt.filePredicate).count())
        }
    }

    @Test
    fun testCopy02() {
        val m = 1000 * 1000
        subtest {
            for (size in if (suite.lengthy)
                arrayOf(0, 1, 2, 10, 16, 256, 1000, 1024, m, 10 * m, 100 * m) else
                arrayOf(0, 1, 2, 10, 16, 256, 1000, 1024, m, 10 * m)) {
                val file1 = tmpFile()
                val file2 = tmpFile()
                try {
                    val data = RandomUt.get(ByteArray(size))
                    file1.writeBytes(data)
                    FileUt.copy(file2, file1)
                    assertFalse("$size", FileUt.diff(file1, file2))
                } finally {
                    file1.delete()
                    file2.delete()
                }
            }
        }
    }

    @Test
    fun testZipEverything01() {
        val srcdir = projectRes("src")
        val expectedfiles = FileUt.count(srcdir) { it.isFile }
        val expecteddirs = FileUt.count(srcdir) { it.isDirectory }
        val zipfile = tmpFile(suffix = ".zip")
        val outdir = tmpDir()
        try {
            FileUt.zip(zipfile, srcdir)
            log.d("# length=${zipfile.length()}")
            assertTrue(zipfile.length() > 10 * 1024)
            ProcessUt.backtick("unzip", "-d", outdir.absolutePath, zipfile.absolutePath)
            val actualfiles = FileUt.count(outdir) { it.isFile }
            val actualdirs = FileUt.count(outdir) { it.isDirectory }
            log.d("# expected files=$expectedfiles, dirs=$expecteddirs")
            log.d("# actual files=$actualfiles, dirs=$actualdirs")
            assertEquals(expectedfiles, actualfiles)
            assertEquals(expecteddirs, actualdirs)
            outdir.walker.walk { file, rpath ->
                if (file.name == "TestFileUtil01.kt") {
                    assertTrue(file.exists())
                    assertTrue(file.length() > 1024)
                }
                if (file.isFile) {
                    assertFalse(FileUt.diff(file, srcdir.file(rpath)))
                }
            }
        } finally {
            zipfile.delete()
            outdir.deleteRecursively()
        }
    }

    @Test
    fun testZipEverything03() {
        val fromdir = testResDir.file("files")
        val expectedfiles = FileUt.count(fromdir) { it.isFile }
        val expecteddirs = FileUt.count(fromdir) { it.isDirectory }
        val zipfile = tmpFile(suffix = ".zip")
        val outdir = tmpDir()
        // Zip files only
        assertEquals(expectedfiles, FileUt.zip(zipfile, fromdir, ".*"))
        // Zip everything
        val count = FileUt.zip(zipfile, fromdir)
        assertEquals("$count", expectedfiles + expecteddirs, count)
        log.d("# length=${zipfile.length()}")
        assertFalse(outdir.file("empty.dir").exists())
        FileUt.unzip(outdir, zipfile) { true }
        val actualfiles = FileUt.count(outdir) { it.isFile }
        val actualdirs = FileUt.count(outdir) { it.isDirectory }
        log.d("# expected files=$expectedfiles, dirs=$expecteddirs")
        log.d("# actual files=$actualfiles, dirs=$actualdirs")
        assertEquals(expectedfiles, actualfiles)
        assertEquals(expecteddirs, actualdirs)
        assertTrue(outdir.file("empty.dir").exists())
        outdir.walker.files { file, rpath ->
            assertFalse(FileUt.diff(file, fromdir.file(rpath)))
        }
    }

    @Test
    fun testUnzip01() {
        val fromdir = testResDir.file("files")
        val expectedfiles = FileUt.count(fromdir) { it.isFile }
        val expecteddirs = FileUt.count(fromdir) { it.isDirectory }
        val zipfile = tmpDir().file("t.zip")
        val outdir = tmpDir()
        val cmdline = mutableListOf<String>("zip", "-ry", zipfile.absolutePath, ".")
        // fromdir.walker.files { _, rpath -> cmdline.add(rpath) }
        log.d("# cmdline=$cmdline")
        log.d(ProcessUt.backtick(fromdir, cmdline))
        log.d("# length=${zipfile.length()}")
        assertTrue(zipfile.exists())
        assertFalse(outdir.file("empty.dir").exists())
        FileUt.unzip(outdir, zipfile) { true }
        val actualfiles = FileUt.count(outdir) { it.isFile }
        val actualdirs = FileUt.count(outdir) { it.isDirectory }
        log.d("# expected files=$expectedfiles, dirs=$expecteddirs")
        log.d("# actual files=$actualfiles, dirs=$actualdirs")
        assertEquals(expectedfiles, actualfiles)
        assertEquals(expecteddirs, actualdirs)
        assertTrue(outdir.file("empty.dir").exists())
        outdir.walker.files { file, rpath ->
            assertFalse(FileUt.diff(file, fromdir.file(rpath)))
        }
    }

    @Test
    fun testZipWithRegex01() {
        val tmpdir = tmpDir()
        val fromdir = testResDir.file("files")
        val outdir = tmpdir.file("out")
        val zipfile = tmpdir.file("t.zip")
        val includes = "^.*file\\d+.*"
        val excludes = "^.*file1.txt"
        assertFalse(zipfile.exists())
        val count = FileUt.zip(zipfile, fromdir, includes, excludes)
        assertEquals(4, count)
        assertTrue(zipfile.exists())
        FileUt.unzip(outdir, zipfile) { true }
        val expected = Fileset(fromdir).includes(RegexFilter(includes)).excludes(RegexFilter(excludes)).collect()
        for ((_, rpath) in expected) {
            log.d("# $rpath")
        }
        assertEquals(expected.count(), outdir.walker.collect(FileUt.filePredicate).count())
    }

    @Test
    fun testUnzipWithIncludesExcludes01() {
        val tmpdir = tmpDir()
        val fromdir = testResDir.file("files")
        val outdir = tmpdir.file("out")
        val zipfile = tmpdir.file("t.zip")
        val includes = "^.*file\\d+.*"
        val excludes = "^.*file1.txt"
        assertFalse(zipfile.exists())
        val from = Fileset(fromdir).includes(RegexFilter(includes)).excludes(RegexFilter(excludes)).collect()
            .map { (_, rpath) -> rpath }
        log.d("## From: ${from.count()}")
        for (rpath in from) {
            log.d("# $rpath")
        }
        val count = FileUt.zip(zipfile, fromdir, from)
        assertEquals(4, count)
        assertTrue(zipfile.exists())
        FileUt.unzip(outdir, zipfile, includes, "^.*file2a.txt")
        val to = outdir.walker.collect(FileUt.filePredicate).map { it.second }
        log.d("## To: ${to.count()}")
        for (rpath in to) {
            log.d("# $rpath")
        }
        assertEquals(from.count() - 1, to.count())
    }

    @Test
    fun testZipPreserveTimestampTrue01() {
        val tmpdir = tmpDir()
        val fromdir = testResDir.file("files")
        val outdir = tmpdir.file("out")
        val zipfile = tmpdir.file("t.zip")
        assertFalse(zipfile.exists())
        val count = FileUt.zip(zipfile, fromdir, true)
        assertEquals(16, count)
        assertTrue(zipfile.exists())
        FileUt.unzip(outdir, zipfile) { true }
        assertTrue(outdir.file("empty.dir").exists())
        var checked = 0
        outdir.walker.walk { file, rpath ->
            // Note that while directory entry has perserved timestamp, the
            // directory is modified when file is created in it.
            if (file.isFile || rpath.endsWith("empty.dir")) {
                ++checked
                checkPreserveTimestamp(true, file, fromdir.file(rpath), 2000)
            }
        }
        assertEquals(12, checked)
    }

    @Test
    fun testZipPreserveTimestampFalse01() {
        val tmpdir = tmpDir()
        val fromdir = testResDir.file("files")
        val outdir = tmpdir.file("out")
        val zipfile = tmpdir.file("t.zip")
        assertFalse(zipfile.exists())
        val count = FileUt.zip(zipfile, fromdir, false)
        assertEquals(16, count)
        assertTrue(zipfile.exists())
        FileUt.unzip(outdir, zipfile) { true }
        assertTrue(outdir.file("empty.dir").exists())
        var checked = 0
        outdir.walker.walk { file, rpath ->
            // Note that while directory entry has perserved timestamp, the
            // directory is modified when file is created in it.
            if (file.isFile || rpath.endsWith("empty.dir")) {
                ++checked
                checkPreserveTimestamp(false, file, fromdir.file(rpath), 2000)
            }
        }
        assertEquals(12, checked)
    }

    @Test
    fun testDiffDir01() {
        val dir1 = testResDir.file("files/dir1")
        val dir2 = testResDir.file("files/dir2")
        val stat = FileUt.diffDir(dir1, dir2)
        log.d("# stat:", stat.toString("dir1", "dir2", printsames = true))
        assertEquals(3, stat.aonly.size)
        assertEquals(3, stat.bonly.size)
        assertEquals(1, stat.sames.size)
        assertEquals(1, stat.diffs.size)
        val tmpdir = tmpDir()
        val file1 = tmpdir.file("file1")
        val file2 = tmpdir.file("file2")
        file1.writeText("file1111")
        file2.writeText("file211")
        assertTrue(FileUt.diff(file1, file2))
    }

    @Test
    fun testSetPermission01() {
        subtest {
            val dir = tmpDir()
            val file = tmpFile("test", ".xxx", dir)
            file.writeText("testing")
            val before = ProcessUt.backtick(dir, "ls", "-al")
            log.d("# Before")
            log.d(before)
            assertFalse(before.contains("drwx------"))
            assertFalse(before.contains("-rw-------"))
            FileUt.setOwnerOnly(dir, file)
            val after = ProcessUt.backtick(dir, "ls", "-al")
            log.d("# After")
            log.d(after)
            assertTrue(after.contains("drwx------"))
            assertTrue(after.contains("-rw-------"))
        }
        subtest {
            val tmpdir = tmpDir()
            task(Copy(tmpdir, testResDir.file("files")))
            val fileset = Fileset(tmpdir).includes("dir1/**")
            FileUt.setWorldReadonly(*fileset.collect().map { it.first }.toList().toTypedArray())
            fileset.collect().forEach { (file, rpath) ->
                val output = ProcessUt.backtick(tmpdir, "ls", "-ald", rpath)
                assertTrue(output, output.contains(if (file.isDirectory) "drwxr-xr-x" else "-rw-r--r--"))
            }
            FileUt.setPermission(FileUt.permissionsOwnerOnlyDir, fileset.dirs().map { it.first }.iterator())
            FileUt.setPermission(FileUt.permissionsOwnerOnlyFile, fileset.files().map { it.first }.iterator())
            fileset.collect().forEach { (file, rpath) ->
                val output = ProcessUt.backtick(tmpdir, "ls", "-ald", rpath)
                assertTrue(output, output.contains(if (file.isDirectory) "drwx------" else "-rw-------"))
            }
        }
    }
}