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
import com.cplusedition.bot.core.StructUtil.Companion.StructUt
import org.junit.Assert.*
import org.junit.Test

class TestStructUt01 : TestBase() {

    @Test
    fun testEquals01() {
        subtest {
            with(StructUt) {
                val a = byteArrayOf(1, 2, 3, 4)
                val b = byteArrayOf(1, 2, 4, 4)
                val c = byteArrayOf(1, 2, 3)
                assertTrue(equals(a, a))
                assertFalse(equals(a, b))
                assertFalse(equals(a, c))
                assertFalse(equals(a, c, 0, 2))
                assertTrue(equals(c, a, 0, 3))
                assertFalse(equals(c, b, 0, 3))
                assertFalse(equals(a, 0, 1, b, 0, 2))
                assertFalse(equals(a, 2, 1, b, 2, 1))
                assertTrue(equals(a, 3, 1, b, 2, 1))
            }
        }
        subtest {
            with(StructUt) {
                val a = arrayOf(1, 2, 3, 4)
                val b = arrayOf(1, 2, 4, 4)
                val c = arrayOf(1, 2, 3)
                assertTrue(equals(a, a))
                assertFalse(equals(a, b))
                assertFalse(equals(a, c))
                assertFalse(equals(a, c, 0, 2))
                assertTrue(equals(c, a, 0, 3))
                assertFalse(equals(c, b, 0, 3))
                assertFalse(equals(a, 0, 1, b, 0, 2))
                assertFalse(equals(a, 2, 1, b, 2, 1))
                assertTrue(equals(a, 3, 1, b, 2, 1))
            }
        }
        subtest {
            with(StructUt) {
                assertEquals(0, compareNullable<String>(null, null) { _, _ -> 1 })
                assertEquals(-1, compareNullable(null, "b") { _, _ -> 1 })
                assertEquals(1, compareNullable("a", null) { _, _ -> -1 })
                assertEquals(1, compareNullable("a", "b") { _, _ -> 1 })
                assertEquals(-1, compareNullable("a", "b") { a, b -> a.compareTo(b) })
                assertEquals(1, compareNullable("b", "abc") { a, b -> a.compareTo(b) })
            }
        }
    }

    @Test
    fun testDiffList01() {
        with(StructUt) {
            val a = listOf("a", "b", "c")
            val b = listOf("a", "b", "c", null)
            val c = listOf("a", "b", "c", "d")
            val d = listOf("a", null, "c")
            assertTrue(equals(a, a))
            assertFalse(equals(a, b))
            assertTrue(equals(b, b))
            assertFalse(equals(b, c))
            assertTrue(equals(d, d))
            assertFalse(equals(a, d))
            assertFalse(equals(c, d))
            assertFalse(equals(b, d))
        }
    }

    @Test
    fun testDiffSet01() {
        val a = setOf("aonly", "a", "c")
        val b = setOf("b", "bonly", "c")
        val ret = StructUt.diff(a, b)
        assertFalse(StructUt.diff(a, a).hasDiff())
        with(ret) {
            assertEquals(2, aonly.size)
            assertEquals(2, bonly.size)
            assertEquals(1, sames.size)
            assertEquals(0, diffs.size)
            assertTrue(hasDiff())
            if (true) {
                val s = toString()
                log.d(s)
                assertTrue(s, s.contains("A only"))
                assertTrue(s, s.contains("A only"))
                assertFalse(s, s.contains("Same"))
            }
            if (true) {
                val s = toString("a", "b", printsames = true)
                log.d(s)
                assertTrue(s, s.contains("a only: 2"))
                assertTrue(s, s.contains("b only: 2"))
                assertTrue(s, s.contains("Same: 1"))
            }
        }
    }

    @Test
    fun testDiffMap01() {
        val a = mapOf(
            "ksame" to "vsame",
            "kaonly" to "va",
            "kdiff" to "va"
        )
        val b = mapOf(
            "ksame" to "vsame",
            "kbonly" to "vb",
            "kdiff" to "vb"
        )
        assertFalse(StructUt.diff(a, a).hasDiff())
        val ret = StructUt.diff(a, b)
        with(ret) {
            assertEquals(1, sames.size)
            assertEquals(1, diffs.size)
            assertEquals(1, aonly.size)
            assertEquals(1, bonly.size)
            assertEquals("ksame", sames.first())
            assertEquals("kdiff", diffs.first())
            assertEquals("kaonly", aonly.first())
            assertEquals("kbonly", bonly.first())
            assertTrue(hasDiff())
        }
    }

}