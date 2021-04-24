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
import com.cplusedition.bot.core.file
import com.cplusedition.bot.core.ut
import org.junit.Assert
import org.junit.Test

class TestFileUtExtension01 : TestBase() {
    @Test
    fun testScan01() {
        val files = testResDir.file("files")
        subtest {
            val list = ArrayList<String>()
            files.ut.scan { _, rpath ->
                list.add(rpath)
                true
            }
            log.d(list)
            Assert.assertEquals(16, list.size)
            Assert.assertTrue(list.indexOf("dir1") < list.indexOf("dir1/dir1a"))
            Assert.assertTrue(list.indexOf("dir1/dir1a") < list.indexOf("dir1/dir1a/file1a.txt"))
        }
        subtest("Ignore dir") {
            val list = ArrayList<String>()
            files.ut.scan { _, rpath ->
                list.add(rpath)
                !rpath.startsWith("dir2")
            }
            log.d(list)
            Assert.assertEquals(10, list.size)
            Assert.assertTrue(list.indexOf("dir1") < list.indexOf("dir1/dir1a"))
            Assert.assertTrue(list.indexOf("dir1/dir1a") < list.indexOf("dir1/dir1a/file1a.txt"))
        }
        subtest("Ignore dir") {
            val list = ArrayList<String>()
            files.ut.scan { _, rpath ->
                if (rpath.contains("dir2")) return@scan false
                list.add(rpath)
                true
            }
            log.d(list)
            Assert.assertEquals(8, list.size)
        }
        subtest("Ignore dir") {
            val list = ArrayList<String>()
            files.ut.scan("prefix") { _, rpath ->
                if (rpath.contains("dir2")) return@scan false
                list.add(rpath)
                true
            }
            log.d(list)
            Assert.assertEquals(8, list.size)
            for (s in list) {
                Assert.assertTrue(s.startsWith("prefix/") && !s.startsWith("prefix//"))
            }
        }
    }

}