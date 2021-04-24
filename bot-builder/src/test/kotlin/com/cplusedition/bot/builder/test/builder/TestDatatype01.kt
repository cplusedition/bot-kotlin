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
import com.cplusedition.bot.builder.Fileset.SelectorFilter
import com.cplusedition.bot.builder.test.zzz.TestBase
import com.cplusedition.bot.core.*
import org.junit.Assert
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

class TestDatatype01 : TestBase(true) {

    fun Sequence<Pair<File, String>>.toPathSet(): Set<String> {
        return map { it.second }.toSet()
    }

    fun Sequence<File>.toPathSet(basedir: File): Set<String?> {
        return map { it.ut.rpath(basedir) }.toSet()
    }

    fun Sequence<Pair<File, String>>.toPathList(): List<String> {
        return map { it.second }.toList()
    }

    fun Sequence<File>.toPathList(basedir: File): List<String?> {
        return map { it.ut.rpath(basedir) }.toList()
    }

    @Test
    fun testSelectorFilter01() {
        fun check(expected: String, selector: String) {
            assertEquals(selector, expected, SelectorFilter.map(arrayOf(selector), true)[0].toString())
        }

        fun count(expected: Int, fileset: Fileset) {
            val files = fileset.pairOfAny().toList()
            log.d("# expected: $expected")
            log.d(files.map { (_, path) -> path })
            assertEquals(expected, files.size)
        }

        fun count(expected: Int, files: Collection<String>) {
            log.d("# expected: $expected")
            log.d(files)
            assertEquals(expected, files.size)
        }

        subtest("Check if split works for splitting multiple selector patterns") {
            assertArrayEquals(SelectorFilter.split("").toTypedArray(), arrayOf(""))
            assertArrayEquals(SelectorFilter.split("a").toTypedArray(), arrayOf("a"))
            assertArrayEquals(SelectorFilter.split("a b").toTypedArray(), arrayOf("a", "b"))
            assertArrayEquals(SelectorFilter.split("a,b").toTypedArray(), arrayOf("a", "b"))
            assertArrayEquals(SelectorFilter.split("a ,b").toTypedArray(), arrayOf("a", "b"))
            assertArrayEquals(SelectorFilter.split("a , b").toTypedArray(), arrayOf("a", "b"))
            assertArrayEquals(SelectorFilter.split("a, b").toTypedArray(), arrayOf("a", "b"))
        }

        subtest {
            check("^$", "")
            check("^[^/]*$", "*")
            check("^.*$", "**")
            check("^.*$", "**/")
            check("^\\Qa\\E(\\Q/\\E.*)?$", "a/**")
            check("^(.*?/)?\\Qa\\E$", "**/a")
            check("^(.*?/)?\\Qa\\E(\\Q/\\E.*)?$", "**/a/**")
            check("^(.*?/)?\\Qa\\E[^/]*$", "**/a**")
            check("^\\Qdir1/dir\\E[^/]*$", "dir1/dir**")
            check("^\\Qa/\\E(.*?/)?\\Qa\\E$", "a/**/a")
            check("^(.*?/)?\\Qt.java\\E$", "**/t.java")
            check("^\\Qdir1\\E(\\Q/\\E.*)?$", "dir1/")
            check("^\\Qdir2/dir\\E[^/]*$", "dir2/dir**")
            check("^\\Qdir2/\\E[^/]*[^/]*\\Q.txt\\E$", "dir2/**.txt")
        }
        subtest {
            val filesdir = testResDir.file("files")
            val files3dir = testResDir.file("files3")

            fun checkall(expected: Int, includes: String?, excludes: String? = null) {
                val fileset = TreeSet(Fileset(filesdir, includes, excludes).pathOfAny().toList())
                log.d("## $includes, $excludes")
                count(expected, fileset)
            }

            fun checkall3(expected: Int, includes: String?, excludes: String? = null) {
                val fileset = TreeSet(Fileset(files3dir, includes, excludes).pathOfAny().toList())
                log.d("## $includes, $excludes")
                count(expected, fileset)
            }

            count(16, Fileset(filesdir))
            count(16, Fileset(filesdir, "**"))
            count(16, Fileset(filesdir, "**/"))
            count(7, Fileset(filesdir, "dir1/"))
            count(7, Fileset(filesdir, "**/dir1/"))
            count(7, Fileset(filesdir, "**/dir1/**"))
            count(7, Fileset(filesdir, "dir2/"))
            count(7, Fileset(filesdir, "**/dir2/"))
            count(7, Fileset(filesdir, "**/dir2/**"))
            count(4, Fileset(filesdir, "**/file?.txt"))
            count(4, Fileset(filesdir, "**/file??txt"))
            count(0, Fileset(filesdir, "**/file..txt"))
            count(8, Fileset(filesdir, "**/dir**"))
            count(14, Fileset(filesdir, "**/dir?/**"))
            count(3, Fileset(filesdir, "dir2/dir**"))
            //
            count(24, Fileset(files3dir, "dir2**/"))
            count(30, Fileset(files3dir, "**/dir2**/"))
            //
            checkall(16, null)
            checkall(0, "/")
            checkall(0, "/a")
            checkall(0, "a/")
            checkall(0, "a/a")
            checkall(7, "**/dir1/")
            checkall(7, "**/dir1/**")
            checkall(4, "dir2/dir*/**")
            checkall(3, "dir2/dir**")
            checkall(7, "dir1/**/file*.txt dir2/dir*/**")
            checkall(14, null, "**/dir1a/**")
            checkall(6, "**/dir1/**", "**/dir1/dir1a/*")
            checkall(5, "**/dir1/**", "**/dir1/dir1a/**")
            checkall(14, null, "**/dir1a/**")
            checkall(5, "dir1/**/*.txt")
            checkall(5, "dir1/**/*.txt")
            checkall(4, "dir1/**/*.txt", "**/dir1a/**")
            checkall(14, null, "**/dir1a/**")
            checkall(5, "dir1/**/*.txt")
            //
            checkall(4, "*")
            checkall(16, "*/")
            checkall(16, "**")
            checkall(16, "**/")
            checkall(0, "/**")
            checkall(7, "dir1/")
            checkall(7, "dir1/**")
            checkall(7, "**/dir1/")
            checkall(7, "**/dir1/**")
            checkall(7, "dir2/")
            checkall(7, "**/dir2/")
            checkall(7, "**/dir2/**")
            checkall(7, "**/dir2/**/")
            checkall(4, "**/file?.txt")
            checkall(4, "**/file??txt")
            checkall(0, "**/file..txt")
            checkall(14, "**/dir?/**")
            checkall(8, "**/dir**")
            checkall(8, "**/dir*")
            checkall(2, "dir**")
            checkall(2, "dir*")
            checkall(4, "**/dir**.txt")
            checkall(4, "**/dir*.txt")
            checkall(7, "dir2/**/")
            checkall(7, "dir2**/")
            checkall(4, "dir2/**.txt")
            // Check trailing / and **.
            checkall3(10, "*")
            checkall3(50, "**")
            checkall3(50, "*/")
            checkall3(50, "**/")
            checkall3(1, "dir2")
            checkall3(11, "dir2/")
            checkall3(7, "dir2/*")
            checkall3(11, "dir2/**")
            checkall3(4, "dir2*")
            checkall3(4, "dir2**")
            checkall3(24, "dir2*/")
            checkall3(24, "dir2**/")
            checkall3(24, "dir2*/**")
            checkall3(24, "dir2**/**")
            checkall3(10, "dir2/*/")
            checkall3(10, "dir2/*/**")
            checkall3(11, "dir2/**/")
            checkall3(11, "dir2/**/**")
            checkall3(4, "dir2/*.txt")
            checkall3(4, "dir2/**.txt")
            checkall3(7, "dir2/**/*.txt")
            checkall3(7, "dir2/**/**.txt")
            checkall3(3, "dir2/dir2*")
            checkall3(3, "dir2/dir2**")
            checkall3(5, "dir2/dir2*/")
            checkall3(5, "dir2/dir2**/")
            //
            checkall3(50, "**/*")
            checkall3(50, "**/**")
            checkall3(50, "**/*/")
            checkall3(50, "**/**/")
            checkall3(5, "**/dir2")
            checkall3(17, "**/dir2/")
            checkall3(11, "**/dir2/*")
            checkall3(17, "**/dir2/**")
            checkall3(18, "**/dir2*")
            checkall3(18, "**/dir2**")
            checkall3(30, "**/dir2*/")
            checkall3(30, "**/dir2**/")
            checkall3(30, "**/dir2*/**")
            checkall3(30, "**/dir2**/**")
            checkall3(13, "**/dir2/*/")
            checkall3(13, "**/dir2/*/**")
            checkall3(17, "**/dir2/**/")
            checkall3(17, "**/dir2/**/**")
            checkall3(8, "**/dir2/*.txt")
            checkall3(8, "**/dir2/**.txt")
            checkall3(10, "**/dir2/**/*.txt")
            checkall3(10, "**/dir2/**/**.txt")
            checkall3(7, "**/dir2/dir2*")
            checkall3(7, "**/dir2/dir2**")
            checkall3(8, "**/dir2/dir2*/")
            checkall3(8, "**/dir2/dir2**/")
        }
    }

    @Test
    fun testSelectorFilter02() {
        fun check(expected: String, selector: String) {
            assertEquals(selector, expected, SelectorFilter.map(arrayOf(selector), true, '\\')[0].toString())
        }

        fun count(expected: Int, list: Sequence<Pair<File, String>>, pat: String) {
            val filter = SelectorFilter(arrayOf(pat), true, '\\')
            val ret = ArrayList<String>()
            for ((file, rpath) in list) {
                if (filter.invoke(file, rpath)) {
                    ret.add(rpath)
                }
            }
            log.d("# expected: $expected")
            log.d(ret)
            assertEquals(expected, ret.size)
        }

        subtest {
            val regex = Regex(Regex.escape("/t/t".replace('/', '\\')))
            assertTrue("$regex", "\\t\\t".matches(regex))
        }
        subtest {
            check("^$", "")
            check("^[^\\\\]*$", "*")
            check("^.*$", "**")
            check("^.*$", "**/")
            check("^\\Qa\\E(\\Q\\\\E.*)?$", "a/**")
            check("^(.*?\\\\)?\\Qa\\E$", "**/a")
            check("^(.*?\\\\)?\\Qa\\E(\\Q\\\\E.*)?$", "**/a/**")
            check("^(.*?\\\\)?\\Qa\\E[^\\\\]*$", "**/a**")
            check("^\\Qdir1\\dir\\E[^\\\\]*$", "dir1/dir**")
            check("^\\Qa\\\\E(.*?\\\\)?\\Qa\\E$", "a/**/a")
            check("^(.*?\\\\)?\\Qt.java\\E$", "**/t.java")
            check("^\\Qdir1\\E(\\Q\\\\E.*)?$", "dir1/")
        }
        subtest {
            val filesdir = testResDir.file("files")
            val paths = Fileset(filesdir).pairOfAny().map { (file, rpath) -> Pair(file, rpath.replace("/", "\\")) }
            count(16, paths, "**")
            count(16, paths, "**/")
            count(7, paths, "dir1/")
            count(7, paths, "**/dir1/")
            count(7, paths, "**/dir1/**")
            count(7, paths, "dir2/")
            count(7, paths, "**/dir2/")
            count(7, paths, "**/dir2/**")
            count(4, paths, "**/file?.txt")
            count(4, paths, "**/file??txt")
            count(0, paths, "**/file..txt")
            count(14, paths, "**/dir?/**")
        }
    }

    @Test
    fun testFileset01() {
        val filesdir = testResDir.file("files")
        subtest("**") {
            assertEquals(
                    setOf(
                            "dir2/dir1a.txt",
                            "dir2/dir2a/file2a.txt",
                            "dir2/dir2only.txt",
                            "dir2/file1.txt",
                            "dir2/file2.txt"
                    ),
                    Fileset(filesdir, "dir2/**/*.txt").pairOfAny().toPathSet()
            )
            assertEquals(
                    setOf(
                            "dir2/dir1a.txt",
                            "dir2/dir2only.txt",
                            "dir2/file1.txt",
                            "dir2/file2.txt"
                    ),
                    Fileset(filesdir, "dir2/*.txt").pairOfAny().toPathSet()
            )
        }
        subtest("Multi") {
            // Not supporting multiple patterns in a single pattern string.
            assertEquals(
                    setOf<String>(),
                    Fileset(filesdir).includes("dir1/dir** dir2/dir**").pairOfAny().toPathSet()
            )
            assertEquals(
                    setOf(
                            "dir1/dir2a.txt",
                            "dir1/dir1a",
                            "dir1/dir1only.txt",
                            "dir2/dir1a.txt",
                            "dir2/dir2a",
                            "dir2/dir2only.txt"
                    ),
                    Fileset(filesdir).includes("dir1/dir*", "dir2/dir*").pairOfAny().toPathSet()
            )
        }
        subtest("Space separated") {
            val tmpdir = tmpDir()
            tmpdir.file("a b/a b c.t x t").mkparentOrFail().writeText("testing123")
            tmpdir.file("a b/a c/t.txt").mkparentOrFail().writeText("testing123")
            assertEquals(
                    setOf(
                            "a b",
                            "a b/a b c.t x t",
                            "a b/a c",
                            "a b/a c/t.txt"
                    ), Fileset(tmpdir).includes("a b", "a b/**").pairOfAny().toPathSet()
            )
            assertEquals(
                    setOf(
                            "a b/a b c.t x t",
                            "a b/a c"
                    ), Fileset(tmpdir).includes("a b/*").pairOfAny().toPathSet()
            )
            assertEquals(
                    setOf(
                            "a b/a b c.t x t",
                            "a b/a c/t.txt"
                    ), Fileset(tmpdir, "**").pairOfFiles().toPathSet()
            )
            assertEquals(
                    setOf(
                            "a b",
                            "a b/a c"
                    ), Fileset(tmpdir, "**/*").pairOfDirs().toPathSet()
            )
        }
    }

    @Test
    fun testFilesetCollect01() {
        val filesdir = testResDir.file("files")
        subtest("collect(preorder)") {
            assertEquals(16, Fileset(filesdir).pairOfAny().count())
            assertEquals(
                    setOf("dir2/dir1a.txt", "dir2/dir2a/file2a.txt", "dir2/dir2a", "dir1/dir2a.txt", "dir1/dir1a"),
                    Fileset(filesdir, "**/*a*", "**/file1*").pairOfAny().toPathSet()
            )
            assertEquals(
                    setOf(
                            "dir2/dir1a.txt",
                            "dir2/dir2a",
                            "dir2/dir2only.txt",
                            "dir2",
                            "dir1/dir2a.txt",
                            "dir1/dir1a",
                            "dir1/dir1only.txt",
                            "dir1",
                            "empty.dir",
                            "empty.txt"
                    ), Fileset(filesdir, null, "**/file*").pairOfAny().toPathSet()
            )
            val list = Fileset(filesdir).includes("**/*1*", "**/*2*").pairOfAny().toPathList()
            assertEquals(
                    setOf(
                            "dir2",
                            "dir2/dir1a.txt",
                            "dir2/dir2a",
                            "dir2/dir2a/file2a.txt",
                            "dir2/dir2only.txt",
                            "dir2/file1.txt",
                            "dir2/file2.txt",
                            "dir1",
                            "dir1/dir2a.txt",
                            "dir1/dir1a",
                            "dir1/dir1a/file1a.txt",
                            "dir1/dir1only.txt",
                            "dir1/file1.txt",
                            "dir1/file2.txt"
                    ),
                    list.toSet()
            )
            // Scan is same as preOrder.
            assertTrue(list.indexOf("dir1/dir1a") < list.indexOf("dir1/dir1a/file1a.txt"))
            assertTrue(list.indexOf("dir2/dir2a") < list.indexOf("dir2/dir2a/file2a.txt"))
        }
        subtest("collect(bottomUp)") {
            assertEquals(
                    setOf(
                            "dir2/dir1a.txt",
                            "dir2/dir2a",
                            "dir2/dir2only.txt",
                            "dir2",
                            "dir1/dir2a.txt",
                            "dir1/dir1a",
                            "dir1/dir1only.txt",
                            "dir1",
                            "empty.dir",
                            "empty.txt"
                    ),
                    Fileset(filesdir, null, "**/file*").pairOfAny(bottomup = true).toPathSet()
            )
            val list = Fileset(filesdir).includes("**/*1*", "**/*2*").pairOfAny(bottomup = true).toPathList()
            assertEquals(
                    setOf(
                            "dir2",
                            "dir2/dir1a.txt",
                            "dir2/dir2a",
                            "dir2/dir2a/file2a.txt",
                            "dir2/dir2only.txt",
                            "dir2/file1.txt",
                            "dir2/file2.txt",
                            "dir1",
                            "dir1/dir2a.txt",
                            "dir1/dir1a",
                            "dir1/dir1a/file1a.txt",
                            "dir1/dir1only.txt",
                            "dir1/file1.txt",
                            "dir1/file2.txt"
                    ),
                    list.toSet()
            )
            assertTrue(list.indexOf("dir2/dir2a") > list.indexOf("dir2/dir2a/file2a.txt"))
            assertTrue(list.indexOf("dir1/dir1a") > list.indexOf("dir1/dir1a/file1a.txt"))
        }
    }

    @Test
    fun testFilesetWalk01() {
        val filesdir = testResDir.file("files")
        fun walk(fileset: Fileset, bottomup: Boolean = false): List<String> {
            val ret = mutableListOf<String>()
            fileset.walk(bottomup) { _, rpath -> ret.add(rpath) }
            return ret
        }
        subtest("collect(preorder)") {
            assertEquals(16, walk(Fileset(filesdir)).count())
            assertEquals(
                    setOf("dir2/dir1a.txt", "dir2/dir2a/file2a.txt", "dir2/dir2a", "dir1/dir2a.txt", "dir1/dir1a"),
                    walk(Fileset(filesdir, "**/*a*", "**/file1*")).toSet()
            )
            assertEquals(
                    setOf(
                            "dir2/dir1a.txt",
                            "dir2/dir2a",
                            "dir2/dir2only.txt",
                            "dir2",
                            "dir1/dir2a.txt",
                            "dir1/dir1a",
                            "dir1/dir1only.txt",
                            "dir1",
                            "empty.dir",
                            "empty.txt"
                    ), walk(Fileset(filesdir, null, "**/file*")).toSet()
            )
            val list = walk(Fileset(filesdir).includes("**/*1*", "**/*2*"))
            assertEquals(
                    setOf(
                            "dir2",
                            "dir2/dir1a.txt",
                            "dir2/dir2a",
                            "dir2/dir2a/file2a.txt",
                            "dir2/dir2only.txt",
                            "dir2/file1.txt",
                            "dir2/file2.txt",
                            "dir1",
                            "dir1/dir2a.txt",
                            "dir1/dir1a",
                            "dir1/dir1a/file1a.txt",
                            "dir1/dir1only.txt",
                            "dir1/file1.txt",
                            "dir1/file2.txt"
                    ),
                    list.toSet()
            )
            // Scan is same as preOrder.
            assertTrue(list.indexOf("dir1/dir1a") < list.indexOf("dir1/dir1a/file1a.txt"))
            assertTrue(list.indexOf("dir2/dir2a") < list.indexOf("dir2/dir2a/file2a.txt"))
        }
        subtest("collect(bottomUp)") {
            assertEquals(
                    setOf(
                            "dir2/dir1a.txt",
                            "dir2/dir2a",
                            "dir2/dir2only.txt",
                            "dir2",
                            "dir1/dir2a.txt",
                            "dir1/dir1a",
                            "dir1/dir1only.txt",
                            "dir1",
                            "empty.dir",
                            "empty.txt"
                    ),
                    walk(Fileset(filesdir, null, "**/file*"), bottomup = true).toSet()
            )
            val list = walk(Fileset(filesdir).includes("**/*1*", "**/*2*"), bottomup = true)
            assertEquals(
                    setOf(
                            "dir2",
                            "dir2/dir1a.txt",
                            "dir2/dir2a",
                            "dir2/dir2a/file2a.txt",
                            "dir2/dir2only.txt",
                            "dir2/file1.txt",
                            "dir2/file2.txt",
                            "dir1",
                            "dir1/dir2a.txt",
                            "dir1/dir1a",
                            "dir1/dir1a/file1a.txt",
                            "dir1/dir1only.txt",
                            "dir1/file1.txt",
                            "dir1/file2.txt"
                    ),
                    list.toSet()
            )
            assertTrue(list.indexOf("dir2/dir2a") > list.indexOf("dir2/dir2a/file2a.txt"))
            assertTrue(list.indexOf("dir1/dir1a") > list.indexOf("dir1/dir1a/file1a.txt"))
        }
    }

    @Test
    fun testFilesetFiles01() {
        val filesdir = testResDir.file("files")
        subtest("files") {
            assertEquals(11, Fileset(filesdir).pairOfFiles().count())
            assertEquals(
                    setOf("dir2/dir1a.txt", "dir2/dir2a/file2a.txt", "dir1/dir2a.txt"),
                    Fileset(filesdir, "**/*a*", "**/file1*").pairOfFiles().toPathSet()
            )
            assertEquals(
                    setOf(
                            "dir2/dir1a.txt",
                            "dir2/file1.txt",
                            "dir1/dir1a/file1a.txt",
                            "dir1/dir1only.txt",
                            "dir1/file1.txt"
                    ),
                    Fileset(filesdir, "**/*1*").pairOfFiles().toPathSet()
            )
            assertEquals(
                    setOf("dir2/dir1a.txt", "dir2/dir2only.txt", "dir1/dir2a.txt", "dir1/dir1only.txt", "empty.txt"),
                    Fileset(filesdir, null, "**/file*").pairOfFiles().toPathSet()
            )
            assertEquals(
                    setOf(
                            "prefix/dir2/dir1a.txt",
                            "prefix/dir2/dir2only.txt",
                            "prefix/dir1/dir2a.txt",
                            "prefix/dir1/dir1only.txt",
                            "prefix/empty.txt"
                    ),
                    Fileset(filesdir, null, "**/file*").basepath("prefix").pairOfFiles().toPathSet()
            )
            assertEquals(
                    setOf(
                            "prefix/dir2/dir1a.txt",
                            "prefix/dir2/dir2only.txt",
                            "prefix/dir1/dir2a.txt",
                            "prefix/dir1/dir1only.txt",
                            "prefix/empty.txt"
                    ),
                    Fileset(filesdir, null, "**/file*").basepath("prefix").pairOfFiles(true).toPathSet()
            )
        }
    }

    @Test
    fun testFilesetDirs01() {
        val filesdir = testResDir.file("files")
        subtest("dirs") {
            assertEquals(5, Fileset(filesdir).pairOfDirs().count())
            assertEquals(
                    setOf("dir2/dir2a", "dir1/dir1a"),
                    Fileset(filesdir, "**/*a*", "**/file1*").pairOfDirs().toPathSet()
            )
            assertEquals(
                    setOf("dir1/dir1a", "dir1"),
                    Fileset(filesdir, "**/*1*").pairOfDirs().toPathSet()
            )
        }
        subtest("dirs(predorder)") {
            val list = Fileset(filesdir, null, "**/file*").pairOfDirs(bottomup = false).toPathList()
            assertEquals(setOf("dir2/dir2a", "dir2", "dir1/dir1a", "dir1", "empty.dir"), list.toSet())
            assertTrue(list.indexOf("dir1") < list.indexOf("dir1/dir1a"))
        }
        subtest("dirs(bottomUp)") {
            val list = Fileset(filesdir, null, "**/file*").pairOfDirs(bottomup = true).toPathList()
            assertEquals(setOf("dir2/dir2a", "dir2", "dir1/dir1a", "dir1", "empty.dir"), list.toSet())
            assertTrue(list.indexOf("dir1") > list.indexOf("dir1/dir1a"))
        }
        subtest("dirs(basepath, predorder)") {
            val list = Fileset(filesdir, null, "**/file*").basepath("prefix").pairOfDirs().toPathList()
            assertEquals(
                    setOf(
                            "prefix/dir2/dir2a",
                            "prefix/dir2",
                            "prefix/dir1/dir1a",
                            "prefix/dir1",
                            "prefix/empty.dir"
                    ), list.toSet()
            )
            assertTrue(list.indexOf("prefix/dir1") < list.indexOf("prefix/dir1/dir1a"))
        }
        subtest("dirs(basepath, bottomUp)") {
            val list = Fileset(filesdir, null, "**/file*").basepath("prefix").pairOfDirs(true).toPathList()
            assertEquals(
                    setOf(
                            "prefix/dir2/dir2a",
                            "prefix/dir2",
                            "prefix/dir1/dir1a",
                            "prefix/dir1",
                            "prefix/empty.dir"
                    ), list.toSet()
            )
            assertTrue(list.indexOf("prefix/dir1") > list.indexOf("prefix/dir1/dir1a"))
        }
    }

    @Test
    fun testFilesetCollectorCollect01() {
        val filesdir = testResDir.file("files")
        subtest("collect(preorder)") {
            assertEquals(16, Fileset(filesdir).pairOfAny().count())
            assertEquals(16, Fileset(filesdir).collect(FilePathCollectors::fileOfAny).count())
            assertEquals(16, Fileset(filesdir).collect(FilePathCollectors::pathOfAny).count())
            assertTrue(Fileset(filesdir).collect(FilePathCollectors::fileOfAny).all { it is File })
            assertTrue(Fileset(filesdir).collect(FilePathCollectors::pathOfAny).all { it is String })
            assertEquals(
                    setOf("dir2/dir1a.txt", "dir2/dir2a/file2a.txt", "dir2/dir2a", "dir1/dir2a.txt", "dir1/dir1a"),
                    Fileset(filesdir, "**/*a*", "**/file1*").collect(FilePathCollectors::pathOfAny).toSet()
            )
        }
    }

    @Test
    fun testFilesetCollectorFiles01() {
        val filesdir = testResDir.file("files")
        subtest("files") {
            assertEquals(11, Fileset(filesdir).pairOfFiles().count())
            assertEquals(11, Fileset(filesdir).fileOfFiles().count())
            assertEquals(11, Fileset(filesdir).pathOfFiles().count())
            assertTrue(Fileset(filesdir).fileOfFiles().all { it is File })
            assertTrue(Fileset(filesdir).pathOfFiles().all { it is String })
            assertEquals(
                    setOf("dir2/dir1a.txt", "dir2/dir2a/file2a.txt", "dir1/dir2a.txt"),
                    Fileset(
                            filesdir,
                            "**/*a*",
                            "**/file1*"
                    ).pathOfFiles().toSet()
            )
        }
    }

    @Test
    fun testFilesetCollectorDirs01() {
        val filesdir = testResDir.file("files")
        subtest("dirs") {
            assertEquals(5, Fileset(filesdir).pairOfDirs().count())
            assertEquals(5, Fileset(filesdir).fileOfDirs().count())
            assertEquals(5, Fileset(filesdir).pathOfDirs().count())
            assertTrue(Fileset(filesdir).fileOfDirs().all { it is File })
            assertTrue(Fileset(filesdir).pathOfDirs().all { it is String })
            assertEquals(
                    setOf("dir2/dir2a", "dir1/dir1a"),
                    Fileset(filesdir, "**/*a*", "**/file1*").pathOfDirs().toSet()
            )
        }
    }

    @Test
    fun testFilesetFileOfFiles01() {
        val filesdir = testResDir.file("files")
        subtest("files") {
            assertEquals(11, Fileset(filesdir).pairOfFiles().count())
            assertEquals(11, Fileset(filesdir).fileOfFiles().count())
            assertEquals(11, Fileset(filesdir).pathOfFiles().count())
            assertTrue(Fileset(filesdir).fileOfFiles().all { it is File })
            assertTrue(Fileset(filesdir).pathOfFiles().all { it is String })
            assertEquals(
                    setOf("dir2/dir1a.txt", "dir2/dir2a/file2a.txt", "dir1/dir2a.txt"),
                    Fileset(
                            filesdir,
                            "**/*a*",
                            "**/file1*"
                    ).pathOfFiles().toSet()
            )
        }
    }

    @Test
    fun testFilesetFileOrDirs01() {
        val filesdir = testResDir.file("files")
        subtest("dirs") {
            assertEquals(5, Fileset(filesdir).pairOfDirs().count())
            assertEquals(5, Fileset(filesdir).fileOfDirs().count())
            assertEquals(5, Fileset(filesdir).pathOfDirs().count())
            assertTrue(Fileset(filesdir).fileOfDirs().all { it is File })
            assertTrue(Fileset(filesdir).pathOfDirs().all { it is String })
            assertEquals(
                    setOf("dir2/dir2a", "dir1/dir1a"),
                    Fileset(filesdir, "**/*a*", "**/file1*").pathOfDirs().toSet()
            )
        }
    }

    @Test
    fun testIgnoresDir01() {
        val filesdir = testResDir.file("files")
        subtest {
            assertEquals(
                    setOf(
                            "dir1/dir2a.txt",
                            "dir1/dir1a/file1a.txt",
                            "dir1/dir1only.txt",
                            "dir1/file1.txt",
                            "dir1/file2.txt",
                            "empty.txt"
                    ),
                    Fileset(filesdir, "**/*.txt").ignoresDir("dir2").pairOfAny().toPathSet()
            )
            assertEquals(
                    setOf("dir2/dir1a.txt", "dir2/dir2only.txt", "dir2/file1.txt", "dir2/file2.txt", "empty.txt"),
                    Fileset(filesdir, "**/*.txt").ignoresDir("dir1", "**/dir*a").pairOfAny().toPathSet()
            )
        }
    }

    @Test
    fun testRegexFilter01() {
        val filesdir = testResDir.file("files")
        subtest {
            assertEquals(
                    setOf(
                            "dir2/dir1a.txt",
                            "dir2/dir2a",
                            "dir2/dir2only.txt",
                            "dir1/dir2a.txt",
                            "dir1/dir1a",
                            "dir1/dir1only.txt"
                    ),
                    Fileset(filesdir, Regex(".*dir\\d+/dir\\d+[^/]*")).pairOfAny().toPathSet()
            )
        }
        subtest {
            assertEquals(
                    setOf(
                            "dir2/dir2a",
                            "dir2/dir2only.txt",
                            "dir1/dir2a.txt"
                    ),
                    Fileset(filesdir, Regex(".*dir\\d+/dir\\d+[^/]*"), Regex(".*/dir1.*")).pairOfAny().toPathSet()
            )
        }
        subtest {
            assertEquals(
                    setOf(
                            "dir2/dir1a.txt",
                            "dir2/dir2a",
                            "dir2/dir2only.txt",
                            "dir1/dir2a.txt",
                            "dir1/dir1a",
                            "dir1/dir1only.txt"
                    ),
                    Fileset(filesdir).includes(
                            Regex(".*dir1/dir\\d+[^/]*"),
                            Regex(".*dir2/dir\\d+[^/]*")
                    ).pairOfAny().toPathSet()
            )
        }
        subtest {
            assertEquals(
                    setOf(
                            "dir2",
                            "dir2/dir2a/file2a.txt",
                            "dir2/file1.txt",
                            "dir2/file2.txt",
                            "dir1",
                            "dir1/dir1a/file1a.txt",
                            "dir1/file1.txt",
                            "dir1/file2.txt"
                    ),
                    Fileset(filesdir).excludes(
                            Regex(".*dir\\d+/dir\\d+[^/]*"),
                            Regex("empty.*")
                    ).pairOfAny().toPathSet()
            )
        }
        subtest {
            assertEquals(
                    setOf(
                            "dir2/dir1a.txt",
                            "dir2/dir2a",
                            "dir2/dir2only.txt",
                            "dir1/dir2a.txt",
                            "dir1/dir1a",
                            "dir1/dir1only.txt"
                    ),
                    Fileset(filesdir).includes(Fileset.RegexFilter(".*dir\\d+/dir\\d+[^/]*")).pairOfAny(bottomup = true).toPathSet()
            )
        }
        subtest {
            assertEquals(
                    setOf(
                            "dir2/dir1a.txt",
                            "dir2/dir2a",
                            "dir2/dir2only.txt",
                            "dir1/dir2a.txt",
                            "dir1/dir1a",
                            "dir1/dir1only.txt"
                    ),
                    Fileset(filesdir).includes(
                            Fileset.RegexFilter(
                                    ".*dir1/dir\\d+[^/]*",
                                    ".*dir2/dir\\d+[^/]*"
                            )
                    ).pairOfAny(bottomup = true).toPathSet()
            )
        }
    }

    @Test
    fun testFilepathset01() {
        val filesdir = testResDir.file("files")
        subtest {
            val list = Filepathset(
                    filesdir,
                    "dir2/notexists",
                    "dir2/dir1a.txt",
                    "dir2/dir2a",
                    "dir2/dir2only.txt",
                    "dir2/dir2a/file2a.txt",
                    "notexists.txt"
            ).includes(
                    "dir1/dir1a",
                    "dir1/dir2a.txt",
                    "dir1/dir1a/file1a.txt",
                    "notexists1"
            ).pairOfAny(bottomup = true).toPathList()
            assertEquals(
                    setOf(
                            "dir1/dir2a.txt",
                            "dir2/dir1a.txt",
                            "dir2/dir2a/file2a.txt",
                            "dir1/dir1a",
                            "dir2/dir2a",
                            "dir1/dir1a/file1a.txt",
                            "dir2/dir2only.txt"
                    ), list.toSet()
            )
            assertTrue(list.indexOf("dir2/dir2a") > list.indexOf("dir2/dir2a/file2a.txt"))
            assertTrue(list.indexOf("dir1/dir1a") > list.indexOf("dir1/dir1a/file1a.txt"))
        }
        subtest {
            val list = Filepathset(
                    filesdir,
                    "dir2/notexists",
                    "dir2/dir1a.txt",
                    "dir2/dir2a",
                    "dir2/dir2only.txt",
                    "dir2/dir2a/file2a.txt",
                    "notexists.txt"
            ).includes(
                    "dir1/dir1a",
                    "dir1/dir2a.txt",
                    "dir1/dir1a/file1a.txt",
                    "notexists1"
            ).pairOfAny().toPathList()
            assertEquals(
                    setOf(
                            "dir1/dir2a.txt",
                            "dir2/dir1a.txt",
                            "dir2/dir2a/file2a.txt",
                            "dir1/dir1a",
                            "dir2/dir2a",
                            "dir1/dir1a/file1a.txt",
                            "dir2/dir2only.txt"
                    ), list.toSet()
            )
            assertTrue(list.indexOf("dir2/dir2a") < list.indexOf("dir2/dir2a/file2a.txt"))
            assertTrue(list.indexOf("dir1/dir1a") < list.indexOf("dir1/dir1a/file1a.txt"))
        }
        subtest {
            assertEquals(
                    setOf(
                            "dir1/dir2a.txt",
                            "dir2/dir1a.txt",
                            "dir2/dir2a",
                            "dir2/dir2only.txt"
                    ),
                    Filepathset(
                            filesdir,
                            "dir2/notexists",
                            "dir2/dir1a.txt",
                            "dir2/dir2a",
                            "dir2/dir2only.txt",
                            "notexists.txt"
                    ).includes(
                            "dir1/dir2a.txt",
                            "notexists1"
                    ).pairOfAny().toPathSet()
            )
            assertEquals(
                    setOf(
                            "dir1/dir2a.txt",
                            "dir2/dir1a.txt",
                            "dir2/dir2only.txt"
                    ),
                    Filepathset(
                            filesdir,
                            "dir2/notexists",
                            "dir2/dir1a.txt",
                            "dir2/dir2a",
                            "dir2/dir2only.txt",
                            "notexists.txt"
                    ).includes(
                            "dir1/dir2a.txt",
                            "notexists1"
                    ).pairOfFiles().toPathSet()
            )
            assertEquals(
                    setOf(
                            "dir2/dir2a"
                    ),
                    Filepathset(
                            filesdir,
                            "dir2/notexists",
                            "dir2/dir1a.txt",
                            "dir2/dir2a",
                            "dir2/dir2only.txt",
                            "notexists.txt"
                    ).includes(
                            "dir1/dir2a.txt",
                            "notexists1"
                    ).pairOfDirs().toPathSet()
            )
        }
    }

    @Test
    fun testFilepathsetWalk01() {
        val filesdir = testResDir.file("files")
        fun walk(fileset: Filepathset, bottomup: Boolean = false): List<String> {
            val ret = mutableListOf<String>()
            fileset.walk(bottomup) { _, rpath -> ret.add(rpath) }
            return ret
        }
        subtest {
            assertEquals(
                    setOf(
                            "dir1/dir2a.txt",
                            "dir2/dir1a.txt",
                            "dir2/dir2a",
                            "dir2/dir2only.txt"
                    ),
                    walk(
                            Filepathset(
                                    filesdir,
                                    "dir2/notexists",
                                    "dir2/dir1a.txt",
                                    "dir2/dir2a",
                                    "dir2/dir2only.txt",
                                    "notexists.txt"
                            ).includes(
                                    "dir1/dir2a.txt",
                                    "notexists1"
                            )
                    ).toSet()
            )
        }
        subtest {
            val list = walk(
                    Filepathset(
                            filesdir,
                            "dir2/notexists",
                            "dir2/dir1a.txt",
                            "dir2/dir2a",
                            "dir2/dir2only.txt",
                            "dir2/dir2a/file2a.txt",
                            "notexists.txt"
                    ).includes(
                            "dir1/dir1a",
                            "dir1/dir2a.txt",
                            "dir1/dir1a/file1a.txt",
                            "notexists1"
                    ), bottomup = true
            )
            assertEquals(
                    setOf(
                            "dir1/dir2a.txt",
                            "dir2/dir1a.txt",
                            "dir2/dir2a/file2a.txt",
                            "dir1/dir1a",
                            "dir2/dir2a",
                            "dir1/dir1a/file1a.txt",
                            "dir2/dir2only.txt"
                    ), list.toSet()
            )
            assertTrue(list.toString(), list.indexOf("dir2/dir2a") > list.indexOf("dir2/dir2a/file2a.txt"))
            assertTrue(list.toString(), list.indexOf("dir1/dir1a") > list.indexOf("dir1/dir1a/file1a.txt"))
        }
        subtest {
            val list = walk(
                    Filepathset(
                            filesdir,
                            "dir2/notexists",
                            "dir2/dir1a.txt",
                            "dir2/dir2a",
                            "dir2/dir2only.txt",
                            "dir2/dir2a/file2a.txt",
                            "notexists.txt"
                    ).includes(
                            "dir1/dir1a",
                            "dir1/dir2a.txt",
                            "dir1/dir1a/file1a.txt",
                            "notexists1"
                    )
            )
            assertEquals(
                    setOf(
                            "dir1/dir2a.txt",
                            "dir2/dir1a.txt",
                            "dir2/dir2a/file2a.txt",
                            "dir1/dir1a",
                            "dir2/dir2a",
                            "dir1/dir1a/file1a.txt",
                            "dir2/dir2only.txt"
                    ), list.toSet()
            )
            assertTrue(list.indexOf("dir2/dir2a") < list.indexOf("dir2/dir2a/file2a.txt"))
            assertTrue(list.indexOf("dir1/dir1a") < list.indexOf("dir1/dir1a/file1a.txt"))
        }
    }

    @Test
    fun testFilemap01() {
        val filesdir = testResDir.file("files")
        subtest {
            val tmpdir = tmpDir()
            val filemap = Filemap().add(filesdir.file(), tmpdir.file())
                    .add(Fileset(filesdir, "dir1/**/file*.txt"), tmpdir)
                    .add(Fileset(filesdir, "dir2/dir*/**"), { _, rpath -> tmpdir.file("$rpath/test") })
            if (log.debugging) {
                for ((k, v) in filemap.mapping) {
                    log.d(
                            "${FileUt.rpathOrNull(k, filesdir.parentFile)} -> ${
                                FileUt.rpathOrNull(
                                        v,
                                        tmpdir.parentFile
                                )
                            }"
                    )
                }
            }
            assertEquals(8, filemap.mapping.size)
            assertEquals(3, filemap.mapping.keys.filter { it.path.contains("files/dir1/") }.size)
            assertEquals(3, filemap.reversed().values.filter { it.path.contains("files/dir1/") }.size)
        }
        subtest {
            val srcdir = tmpDir()
            val dstdir = tmpDir()
            fun debugprint(msg: String, map: Map<File, File>) {
                if (log.debugging) {
                    log.d("$msg: ${map.size}")
                    map.forEach { k, v ->
                        val src = FileUt.rpathOrNull(k, srcdir.parentFile)
                        val dst = FileUt.rpathOrNull(v, dstdir.parentFile)
                        log.d("$src -> $dst")
                    }
                }
            }
            task(Copy(srcdir, filesdir))
            task(Copy(dstdir, filesdir))
            task(Remove(dstdir, "dir1/dir1a/**"))
            val now = System.currentTimeMillis()
            Fileset(srcdir, "dir2/file*.txt").pairOfAny(bottomup = true)
                    .forEach { (file, _) -> file.setLastModified(now) }
            val filemap = Filemap().add(srcdir.file(), dstdir.file())
                    .add(Fileset(srcdir, "**/file*.txt"), dstdir)
                    .add(Fileset(srcdir, "dir2/**"), dstdir)
                    .add(Fileset(srcdir, "dir2/dir2a/*"), { _, rpath -> dstdir.file("$rpath/test") })
            debugprint("# mapping", filemap.mapping)
            val modified = filemap.modified()
            debugprint("# modified", modified)
            assertEquals(11, filemap.mapping.size)
            assertEquals(4, modified.size)
            val dstfile = filemap.mapping[srcdir.file("dir2/dir2a/file2a.txt")]
            assertTrue(dstfile != null && dstfile.path.endsWith("dir2/dir2a/file2a.txt/test"))
            for ((src, dst) in modified) {
                assertTrue(src.path.contains("/dir2/") || src.path.contains("/dir1/dir1a/"))
                assertTrue(dst.path.endsWith(".txt") || dst.path.endsWith("/test"))
            }
        }
    }
}