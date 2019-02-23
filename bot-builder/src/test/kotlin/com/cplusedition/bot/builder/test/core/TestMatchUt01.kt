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
import com.cplusedition.bot.core.MatchUtil.Companion.MatchUt
import com.cplusedition.bot.core.WithUtil.Companion.With
import com.cplusedition.bot.core.file
import org.junit.Assert.*
import org.junit.Test

class TestMatchUt01 : TestBase() {

    @Test
    fun testMatchUt01() {
        subtest {
            assertTrue(MatchUt.matches("testing123 abc 1234", null as Regex?))
            assertTrue(MatchUt.matches("testing123 abc 1234", Regex(".*")))
            assertTrue(MatchUt.matches("testing123 abc 1234", Regex("^.*\\d+.*")))
            assertFalse(MatchUt.matches("testing123 abc 1234", Regex("^.*\\d+.*"), Regex("^.*testing.*")))
        }
        subtest {
            assertFalse(MatchUt.matches("testing123 abc 1234", Regex("\\d+")))
            assertTrue(MatchUt.find("testing123 abc 1234", Regex("\\d+")))
        }
    }

    @Test
    fun testMatchUtMatchArray01() {
        val file = testResDir.file("html/manual.html")
        val includes = MatchUt.compile("^\\s*<div.*", "^\\s*<li.*")
        val excludes = MatchUt.compile(".*</div>.*", ".*target.*")
        subtest {
            val matches = ArrayList<String>()
            With.lines(file) {
                if (MatchUt.matches(it, includes)) matches.add(it)
            }
            log.d(matches)
            assertEquals(49, matches.size)
        }
        subtest {
            val matches = ArrayList<String>()
            With.lines(file) {
                if (MatchUt.matches(it, includes, excludes)) matches.add(it)
            }
            log.d(matches)
            assertEquals(36, matches.size)
        }
    }

    @Test
    fun testMatchUtFindArray01() {
        val file = testResDir.file("html/manual.html")
        val includes = MatchUt.compile("<div>", "<li>")
        val excludes = MatchUt.compile("</div>", "target")
        subtest {
            val matches = ArrayList<String>()
            With.lines(file) {
                if (MatchUt.find(it, includes)) matches.add(it)
            }
            log.d(matches)
            assertEquals(16, matches.size)
        }
        subtest {
            val matches = ArrayList<String>()
            With.lines(file) {
                if (MatchUt.find(it, includes, excludes)) matches.add(it)
            }
            log.d(matches)
            assertEquals(7, matches.size)
        }
    }

    @Test
    fun testMatchUtMatchIterable01() {
        val file = testResDir.file("html/manual.html")
        val includes = MatchUt.compile("^\\s*<div.*", "^\\s*<li.*").asList()
        val excludes = MatchUt.compile(".*</div>.*", ".*target.*").asList()
        subtest {
            val matches = ArrayList<String>()
            With.lines(file) {
                if (MatchUt.matches(it, includes)) matches.add(it)
            }
            log.d(matches)
            assertEquals(49, matches.size)
        }
        subtest {
            val matches = ArrayList<String>()
            With.lines(file) {
                if (MatchUt.matches(it, includes, excludes)) matches.add(it)
            }
            log.d(matches)
            assertEquals(36, matches.size)
        }
    }

    @Test
    fun testMatchUtFindIterable01() {
        val file = testResDir.file("html/manual.html")
        val includes = MatchUt.compile("<div>", "<li>").asList()
        val excludes = MatchUt.compile("</div>", "target").asList()
        subtest {
            val matches = ArrayList<String>()
            With.lines(file) {
                if (MatchUt.find(it, includes)) matches.add(it)
            }
            log.d(matches)
            assertEquals(16, matches.size)
        }
        subtest {
            val matches = ArrayList<String>()
            With.lines(file) {
                if (MatchUt.find(it, includes, excludes)) matches.add(it)
            }
            log.d(matches)
            assertEquals(7, matches.size)
        }
    }
}