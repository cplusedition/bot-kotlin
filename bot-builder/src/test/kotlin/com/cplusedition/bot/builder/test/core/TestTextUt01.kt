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
import com.cplusedition.bot.core.Hex
import com.cplusedition.bot.core.RandomUtil.Companion.RandomUt
import com.cplusedition.bot.core.TextUtil.Companion.TextUt
import com.cplusedition.bot.core.WithUtil.Companion.With
import com.cplusedition.bot.core.file
import org.junit.Assert
import org.junit.Assert.*
import org.junit.Test
import java.util.*

class TestTextUt01 : TestBase() {

    @Test
    fun testBasic01() {
        subtest {
            assertTrue(TextUt.isEmpty(null))
            assertTrue(TextUt.isEmpty(""))
            assertFalse(TextUt.isEmpty(" "))
            assertFalse(TextUt.isEmpty("a"))
        }
    }

    @Test
    fun testHex01() {
        subtest {
            for (i in 0..10) {
                val data = RandomUt.get(ByteArray(16))
                // log.d("# data=${Hex.toString(data)}")
                val hex = Hex.encode(data)
                log.d("# hex=$hex")
                val bin = Hex.decode(hex)
                // log.d("# bin=${Hex.toString(bin)}")
                Assert.assertTrue(Arrays.equals(bin, data))
            }
            With.exceptionOrFail {
                Hex.decode("01234")
            }
            With.exceptionOrFail {
                Hex.decode("abcdefgh")
            }
        }
        subtest {
            val input = byteArrayOf(1, 2, 4, 3)
            val output = Hex.toString(input)
            assertEquals("0x01, 0x02, 0x04, 0x03", output)
        }
    }

    @Test
    fun testSplit01() {
        subtest {
            assertEquals("", TextUt.split2("12", "").first)
            assertEquals(null, TextUt.split2("12", "").second)
            assertEquals("134", TextUt.split2("12", "134").first)
            assertEquals(null, TextUt.split2("12", "134").second)
            assertEquals("testing", TextUt.split2("12", "testing123").first)
            assertEquals("3", TextUt.split2("12", "testing123").second)
        }
        subtest {
            assertEquals(listOf("TESTING", "3AND", "34", "3"), TextUt.split(
                "12", "testing123and12341312121"
            ) {
                it.toUpperCase()
            })
            assertEquals(listOf("a", "b"), TextUt.split("/", "/a//b//") { it })
        }
    }

    @Test
    fun testDecUnit01() {
        subtest {
            with(TextUt) {
                assertEquals("0", Pair(0L, ""), decUnit4(0))
                assertEquals("1", Pair(1L, ""), decUnit4(1))
                assertEquals("445", Pair(445L, ""), decUnit4(445))
                assertEquals("9999", Pair(9999L, ""), decUnit4(9999))
                assertEquals("10001", Pair(10L, "k"), decUnit4(10000))
                assertEquals("10001", Pair(10L, "k"), decUnit4(10001))
                assertEquals("10444", Pair(10L, "k"), decUnit4(10444))
                assertEquals("10445", Pair(11L, "k"), decUnit4(10445))
                assertEquals("9999444", Pair(9999L, "k"), decUnit4(9999444))
                assertEquals("10444000", Pair(10L, "m"), decUnit4(10444000))
                assertEquals("10444445", Pair(11L, "m"), decUnit4(10444445))
                //
                assertEquals("-0", Pair(0L, ""), decUnit4(-0))
                assertEquals("-1", Pair(-1L, ""), decUnit4(-1))
                assertEquals("-445", Pair(-445L, ""), decUnit4(-445))
                assertEquals("-9999", Pair(-9999L, ""), decUnit4(-9999))
                assertEquals("-10001", Pair(-10L, "k"), decUnit4(-10000))
                assertEquals("-10001", Pair(-10L, "k"), decUnit4(-10001))
                assertEquals("-10444", Pair(-10L, "k"), decUnit4(-10444))
                assertEquals("-10445", Pair(-11L, "k"), decUnit4(-10445))
                assertEquals("-9999444", Pair(-9999L, "k"), decUnit4(-9999444))
                assertEquals("-10444000", Pair(-10L, "m"), decUnit4(-10444000))
                assertEquals("-10444445", Pair(-11L, "m"), decUnit4(-10444445))
                //
                assertEquals("0", "0", decUnit4String(0))
                assertEquals("1", "1", decUnit4String(1))
                assertEquals("445", "445", decUnit4String(445))
                assertEquals("9999", "9999", decUnit4String(9999))
                assertEquals("10001", "10 k", decUnit4String(10000))
                assertEquals("10001", "10 k", decUnit4String(10001))
                assertEquals("10444", "10 k", decUnit4String(10444))
                assertEquals("10445", "11 k", decUnit4String(10445))
                assertEquals("9999444", "9999 k", decUnit4String(9999444))
                assertEquals("10444000", "10 m", decUnit4String(10444000))
                assertEquals("10444445", "11 m", decUnit4String(10444445))
                //
                assertEquals("-0", "0", decUnit4String(-0))
                assertEquals("-1", "-1", decUnit4String(-1))
                assertEquals("-445", "-445", decUnit4String(-445))
                assertEquals("-9999", "-9999", decUnit4String(-9999))
                assertEquals("-10001", "-10 k", decUnit4String(-10000))
                assertEquals("-10001", "-10 k", decUnit4String(-10001))
                assertEquals("-10444", "-10 k", decUnit4String(-10444))
                assertEquals("-10445", "-11 k", decUnit4String(-10445))
                assertEquals("-9999444", "-9999 k", decUnit4String(-9999444))
                assertEquals("-10444000", "-10 m", decUnit4String(-10444000))
                assertEquals("-10444445", "-11 m", decUnit4String(-10444445))
            }
        }
        subtest {
            assertEquals("19 kB", TextUt.decUnit4String(testResDir.file("html/manual.html")))
        }
        subtest {
            val units = arrayOf("", "K", "M")
            assertEquals(Pair(0f, ""), TextUt.valueUnit(units, 10000f, 1000f, 0f))
            assertEquals(Pair(1f, ""), TextUt.valueUnit(units, 10000f, 1000f, 1f))
            assertEquals(Pair(10f, ""), TextUt.valueUnit(units, 10000f, 1000f, 10f))
            assertEquals(Pair(100f, ""), TextUt.valueUnit(units, 10000f, 1000f, 100f))
            assertEquals(Pair(1000f, ""), TextUt.valueUnit(units, 10000f, 1000f, 1000f))
            assertEquals(Pair(10f, "K"), TextUt.valueUnit(units, 10000f, 1000f, 10000f))
            assertEquals(Pair(100f, "K"), TextUt.valueUnit(units, 10000f, 1000f, 100000f))
            assertEquals(Pair(1000f, "K"), TextUt.valueUnit(units, 10000f, 1000f, 1000000f))
            assertEquals(Pair(9999f, "K"), TextUt.valueUnit(units, 10000f, 1000f, 9999000f))
            assertEquals(Pair(9999.9f, "K"), TextUt.valueUnit(units, 10000f, 1000f, 9999900f))
            assertEquals(Pair(9999.999f, "K"), TextUt.valueUnit(units, 10000f, 1000f, 9999999f))
            assertEquals(Pair(10f, "M"), TextUt.valueUnit(units, 10000f, 1000f, 10000000f))
        }
    }

}
