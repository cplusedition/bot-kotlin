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
import com.cplusedition.bot.core.FilePathCollectors
import com.cplusedition.bot.core.FileUtil.Companion.FileUt
import com.cplusedition.bot.core.file
import com.cplusedition.bot.core.mkparentOrFail
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TestDatatype01 : TestBase() {

    fun Sequence<Pair<File, String>>.toPathSet(): Set<String> {
        return map { it.second }.toSet()
    }

    fun Sequence<Pair<File, String>>.toPathList(): List<String> {
        return map { it.second }.toList()
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
                Fileset(filesdir, "dir2/**/*.txt").collect().toPathSet()
            )
            assertEquals(
                setOf(
                    "dir2/dir1a.txt",
                    "dir2/dir2only.txt",
                    "dir2/file1.txt",
                    "dir2/file2.txt"
                ),
                Fileset(filesdir, "dir2/*.txt").collect().toPathSet()
            )
        }
        subtest("Multi") {
            // Not supporting multiple patterns in a single pattern string.
            assertEquals(
                setOf<String>(),
                Fileset(filesdir).includes("dir1/dir** dir2/dir**").collect().toPathSet()
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
                Fileset(filesdir).includes("dir1/dir**", "dir2/dir**").collect().toPathSet()
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
                ), Fileset(tmpdir, "a b/**").collect().toPathSet()
            )
            assertEquals(
                setOf(
                    "a b/a b c.t x t",
                    "a b/a c"
                ), Fileset(tmpdir, "a b/*").collect().toPathSet()
            )
            assertEquals(
                setOf(
                    "a b/a b c.t x t",
                    "a b/a c/t.txt"
                ), Fileset(tmpdir, "**").files().toPathSet()
            )
            assertEquals(
                setOf(
                    "a b",
                    "a b/a c"
                ), Fileset(tmpdir, "**/*").dirs().toPathSet()
            )
        }
    }

    @Test
    fun testFilesetCollect01() {
        val filesdir = testResDir.file("files")
        subtest("collect(preorder)") {
            assertEquals(16, Fileset(filesdir).collect().count())
            assertEquals(
                setOf("dir2/dir1a.txt", "dir2/dir2a/file2a.txt", "dir2/dir2a", "dir1/dir2a.txt", "dir1/dir1a"),
                Fileset(filesdir, "**/*a*", "**/file1*").collect().toPathSet()
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
                ), Fileset(filesdir, null, "**/file*").collect().toPathSet()
            )
            val list = Fileset(filesdir).includes("**/*1*", "**/*2*").collect().toPathList()
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
                Fileset(filesdir, null, "**/file*").collect(bottomup = true).toPathSet()
            )
            val list = Fileset(filesdir).includes("**/*1*", "**/*2*").collect(bottomup = true).toPathList()
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
            assertEquals(11, Fileset(filesdir).files().count())
            assertEquals(
                setOf("dir2/dir1a.txt", "dir2/dir2a/file2a.txt", "dir1/dir2a.txt"),
                Fileset(filesdir, "**/*a*", "**/file1*").files().toPathSet()
            )
            assertEquals(
                setOf(
                    "dir2/dir1a.txt",
                    "dir2/file1.txt",
                    "dir1/dir1a/file1a.txt",
                    "dir1/dir1only.txt",
                    "dir1/file1.txt"
                ),
                Fileset(filesdir, "**/*1*").files().toPathSet()
            )
            assertEquals(
                setOf("dir2/dir1a.txt", "dir2/dir2only.txt", "dir1/dir2a.txt", "dir1/dir1only.txt", "empty.txt"),
                Fileset(filesdir, null, "**/file*").files().toPathSet()
            )
            assertEquals(
                setOf(
                    "prefix/dir2/dir1a.txt",
                    "prefix/dir2/dir2only.txt",
                    "prefix/dir1/dir2a.txt",
                    "prefix/dir1/dir1only.txt",
                    "prefix/empty.txt"
                ),
                Fileset(filesdir, null, "**/file*").basepath("prefix").files().toPathSet()
            )
            assertEquals(
                setOf(
                    "prefix/dir2/dir1a.txt",
                    "prefix/dir2/dir2only.txt",
                    "prefix/dir1/dir2a.txt",
                    "prefix/dir1/dir1only.txt",
                    "prefix/empty.txt"
                ),
                Fileset(filesdir, null, "**/file*").basepath("prefix").files(true).toPathSet()
            )
        }
    }

    @Test
    fun testFilesetDirs01() {
        val filesdir = testResDir.file("files")
        subtest("dirs") {
            assertEquals(5, Fileset(filesdir).dirs().count())
            assertEquals(
                setOf("dir2/dir2a", "dir1/dir1a"),
                Fileset(filesdir, "**/*a*", "**/file1*").dirs().toPathSet()
            )
            assertEquals(
                setOf("dir1/dir1a", "dir1"),
                Fileset(filesdir, "**/*1*").dirs().toPathSet()
            )
        }
        subtest("dirs(predorder)") {
            val list = Fileset(filesdir, null, "**/file*").dirs(bottomup = false).toPathList()
            assertEquals(setOf("dir2/dir2a", "dir2", "dir1/dir1a", "dir1", "empty.dir"), list.toSet())
            assertTrue(list.indexOf("dir1") < list.indexOf("dir1/dir1a"))
        }
        subtest("dirs(bottomUp)") {
            val list = Fileset(filesdir, null, "**/file*").dirs(bottomup = true).toPathList()
            assertEquals(setOf("dir2/dir2a", "dir2", "dir1/dir1a", "dir1", "empty.dir"), list.toSet())
            assertTrue(list.indexOf("dir1") > list.indexOf("dir1/dir1a"))
        }
        subtest("dirs(basepath, predorder)") {
            val list = Fileset(filesdir, null, "**/file*").basepath("prefix").dirs().toPathList()
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
            val list = Fileset(filesdir, null, "**/file*").basepath("prefix").dirs(true).toPathList()
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
            assertEquals(16, Fileset(filesdir).collector(::Pair).collect().count())
            assertEquals(16, Fileset(filesdir).collector(FilePathCollectors::fileCollector).collect().count())
            assertEquals(16, Fileset(filesdir).collector(FilePathCollectors::pathCollector).collect().count())
            assertTrue(Fileset(filesdir).collector(FilePathCollectors::fileCollector).collect().all { it is File })
            assertTrue(Fileset(filesdir).collector(FilePathCollectors::pathCollector).collect().all { it is String })
            assertEquals(
                setOf("dir2/dir1a.txt", "dir2/dir2a/file2a.txt", "dir2/dir2a", "dir1/dir2a.txt", "dir1/dir1a"),
                Fileset(filesdir, "**/*a*", "**/file1*").collector(FilePathCollectors::pathCollector).collect().toSet()
            )
        }
    }

    @Test
    fun testFilesetCollectorFiles01() {
        val filesdir = testResDir.file("files")
        subtest("files") {
            assertEquals(11, Fileset(filesdir).collector(::Pair).files().count())
            assertEquals(11, Fileset(filesdir).collector(FilePathCollectors::fileCollector).files().count())
            assertEquals(11, Fileset(filesdir).collector(FilePathCollectors::pathCollector).files().count())
            assertTrue(Fileset(filesdir).collector(FilePathCollectors::fileCollector).files().all { it is File })
            assertTrue(Fileset(filesdir).collector(FilePathCollectors::pathCollector).files().all { it is String })
            assertEquals(
                setOf("dir2/dir1a.txt", "dir2/dir2a/file2a.txt", "dir1/dir2a.txt"),
                Fileset(
                    filesdir,
                    "**/*a*",
                    "**/file1*"
                ).collector(FilePathCollectors::pathCollector).files().toSet()
            )
        }
    }

    @Test
    fun testFilesetCollectorDirs01() {
        val filesdir = testResDir.file("files")
        subtest("dirs") {
            assertEquals(5, Fileset(filesdir).collector(::Pair).dirs().count())
            assertEquals(5, Fileset(filesdir).collector(FilePathCollectors::fileCollector).dirs().count())
            assertEquals(5, Fileset(filesdir).collector(FilePathCollectors::pathCollector).dirs().count())
            assertTrue(Fileset(filesdir).collector(FilePathCollectors::fileCollector).dirs().all { it is File })
            assertTrue(Fileset(filesdir).collector(FilePathCollectors::pathCollector).dirs().all { it is String })
            assertEquals(
                setOf("dir2/dir2a", "dir1/dir1a"),
                Fileset(filesdir, "**/*a*", "**/file1*").collector(FilePathCollectors::pathCollector).dirs().toSet()
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
                Fileset(filesdir, "**/*.txt").ignoresDir("dir2").collect().toPathSet()
            )
            assertEquals(
                setOf("dir2/dir1a.txt", "dir2/dir2only.txt", "dir2/file1.txt", "dir2/file2.txt", "empty.txt"),
                Fileset(filesdir, "**/*.txt").ignoresDir("dir1", "**/dir*a").collect().toPathSet()
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
                Fileset(filesdir, Regex(".*dir\\d+/dir\\d+[^/]*")).collect().toPathSet()
            )
        }
        subtest {
            assertEquals(
                setOf(
                    "dir2/dir2a",
                    "dir2/dir2only.txt",
                    "dir1/dir2a.txt"
                ),
                Fileset(filesdir, Regex(".*dir\\d+/dir\\d+[^/]*"), Regex(".*/dir1.*")).collect().toPathSet()
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
                ).collect().toPathSet()
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
                ).collect().toPathSet()
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
                Fileset(filesdir).includes(Fileset.RegexFilter(".*dir\\d+/dir\\d+[^/]*")).collect(bottomup = true).toPathSet()
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
                ).collect(bottomup = true).toPathSet()
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
            ).collect(bottomup = true).toPathList()
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
            ).collect().toPathList()
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
                ).collect().toPathSet()
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
                ).files().toPathSet()
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
                ).dirs().toPathSet()
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
            assertTrue(list.indexOf("dir2/dir2a") > list.indexOf("dir2/dir2a/file2a.txt"))
            assertTrue(list.indexOf("dir1/dir1a") > list.indexOf("dir1/dir1a/file1a.txt"))
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
                        "${FileUt.rpathOrNull(k, filesdir.parentFile)} -> ${FileUt.rpathOrNull(
                            v,
                            tmpdir.parentFile
                        )}"
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
            Fileset(srcdir, "dir2/file*.txt").collect(bottomup = true)
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