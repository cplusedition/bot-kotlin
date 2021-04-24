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
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.EOFException

class TestIOUt01 : TestBase() {

    @Test
    fun testIOUtil01() {

        val b1 = byteArrayOf(1, 2, 3)
        val b2 = byteArrayOf(10, 11, 12, 13, 14, 15)

        fun setup(): ByteArray {
            val out = ByteArrayOutputStream()
            val w = ByteWriter(out)
            w.write(0xab)
            w.write(b1)
            w.write(b2, 1, 3)
            w.write16BE(0x8765)
            w.write16LE(0x6789)
            w.write32BE(-0x1876_5432)
            w.write32LE(0x3456_7890)
            w.write64BE(0x7654_3210_0123_4567L)
            w.write64LE(-0x2345_6789_9876_5432L)
            return out.toByteArray()
        }

        subtest {
            val data = setup()
            val r = ByteReader(ByteArrayInputStream(data))
            assertEquals(0xab, r.read())
            assertTrue(StructUt.equals(b1, r.read(ByteArray(3))))
            assertTrue(StructUt.equals(b2, 1, 3, r.read(ByteArray(3)), 0, 3))
            assertEquals(0x8765, r.u16BE())
            assertEquals(0x6789, r.u16LE())
            assertEquals(-0x1876_5432, r.i32BE()) // 0xe789abce
            assertEquals(0x3456_7890, r.i32LE())
            assertEquals(0x7654_3210_0123_4567L, r.i64BE())
            assertEquals(-0x2345_6789_9876_5432L, r.i64LE())
        }

        subtest {
            val data = setup()
            val r = ByteReader(ByteArrayInputStream(data))
            assertEquals(-85, r.i8())
            assertTrue(StructUt.equals(b1, r.read(ByteArray(3))))
            assertTrue(StructUt.equals(b2, 1, 3, r.read(ByteArray(3)), 0, 3))
            assertEquals(-30875, r.i16BE()) // 0xffff_8765
            assertEquals(0x6789, r.i16LE())
            assertEquals(3884559310, r.u32BE()) // 0xe789abce
            assertEquals(0x3456_7890, r.u32LE())
        }

        subtest {
            assertArrayEquals(
                byteArrayOf(0, 0, 0, 0x7f, -128, -1, 0, 0, 0, 0),
                ByteReader(byteArrayOf(0, 0x7f, -128, -1)).read(ByteArray(10), 2, 4)
            )
            assertEquals(0xff, ByteReader(byteArrayOf(-1)).u8())
            assertEquals("testing\u20221234", ByteReader("testing\u20221234".toByteArray()).utf8(14).toString())
            assertTrue(With.exceptionOrNull { ByteReader(byteArrayOf()).read() } is EOFException)
            assertTrue(With.exceptionOrNull { ByteReader(byteArrayOf(1, 2, 3)).i32BE() } is EOFException)
        }

        subtest {
            assertEquals(0, NumberReader(byteArrayOf(0)).i8())
            assertEquals(0x7f, NumberReader(byteArrayOf(0x7f)).i8())
            assertEquals(-128, NumberReader(byteArrayOf(-128)).i8())
            assertEquals(-1, NumberReader(byteArrayOf(-1)).i8())
            assertEquals(0, NumberReader(byteArrayOf(0)).u8())
            assertEquals(0x7f, NumberReader(byteArrayOf(0x7f)).u8())
            assertEquals(0x80, NumberReader(byteArrayOf(-128)).u8())
            assertEquals(0xff, NumberReader(byteArrayOf(-1)).u8())
        }
    }

    @Test
    fun testReadFully01() {
        subtest {
            val data = RandomUt.get(ByteArray(100))
            val input = ByteArrayInputStream(data)
            val output = ByteArray(0)
            IOUt.readFully(input, output)
            IOUt.skipFully(input, 0)
            IOUt.skipFully(input, 1)
            assertEquals(data[1], IOUt.readFully(input, ByteArray(1))[0])
        }
        subtest {
            val data = RandomUt.get(ByteArray(100))
            val input = ByteArrayInputStream(data)
            val output = ByteArray(123)
            assertTrue(With.exceptionOrNull { IOUt.readFully(input, output) } is EOFException)
        }
        subtest {
            val data = RandomUt.get(ByteArray(100))
            val input = ByteArrayInputStream(data)
            assertTrue(With.exceptionOrNull { IOUt.skipFully(input, 123) } is EOFException)
        }
        subtest {
            val data = RandomUt.get(ByteArray(1000))
            val input = ByteArrayInputStream(data)
            val output = ByteArray(1024)
            val n = IOUt.readAsMuchAsPossible(input, output, 0, output.size)
            assertEquals(1000, n)
        }
    }

    @Test
    fun testConversion01() {
        assertEquals(0x3412.toShort(), IOUt.htons(0x1234.toShort()))
        assertEquals(0x78563412, IOUt.htonl(0x12345678))
        assertEquals(0x00cdab7856341200L, IOUt.htonl(0x12345678abcd00L))
        assertEquals(0x3412.toShort(), IOUt.ntohs(0x1234.toShort()))
        assertEquals(0x78563412, IOUt.ntohl(0x12345678))
        assertEquals(0x00cdab7856341200L, IOUt.ntohl(0x12345678abcd00L))
        assertEquals(0x3412.toChar(), IOUt.swap(0x1234.toChar()))
        assertEquals(0x3412.toShort(), IOUt.swap(0x1234.toShort()))
        assertEquals(0x78563412, IOUt.swap(0x12345678))
        assertEquals(0x00cdab7856341200L, IOUt.swap(0x12345678abcd00L))
    }

    @Test
    fun testStringPrintWriter01() {
        subtest {
            fun fixture(w: StringPrintWriter) {
                subtest {
                    w.println("testing")
                    w.print(1234)
                    w.print(true)
                    w.close()
                    log.d("# $w")
                    assertEquals(16, w.buffer.length)
                    assertEquals(16, w.length)
                    assertEquals('s', w[2])
                    assertEquals("tin", w.subSequence(3, 6).toString())
                }
            }
            fixture(StringPrintWriter())
            fixture(StringPrintWriter(20))
        }
        subtest {
            val w = StringPrintWriter()
            val a = listOf("a", "b", "c")
            w.print(a)
            w.print(a.iterator())
            w.print(a.asSequence())
            val output = w.toString()
            assertEquals(output, "abcabcabc", output)
        }
        subtest {
            val w = StringPrintWriter()
            val a = listOf("a", "b", "c")
            w.println(a)
            w.println(a.iterator())
            w.println(a.asSequence())
            val output = w.toString()
            assertEquals(output, StructUt.concat(a, a, a, listOf("")).joinln(), output)
        }
    }
}