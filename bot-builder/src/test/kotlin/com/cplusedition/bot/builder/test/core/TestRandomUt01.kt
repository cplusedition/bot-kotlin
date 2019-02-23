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
import com.cplusedition.bot.core.Fun10
import com.cplusedition.bot.core.RandomUtil
import com.cplusedition.bot.core.RandomUtil.Companion.RandomUt
import com.cplusedition.bot.core.StructUtil.Companion.StructUt
import org.junit.Assert.*
import org.junit.Test

class TestRandomUt01 : TestBase() {

    fun <T> fixture(desc: String, iter: Int, code: Fun10<MutableSet<T>>, done: Fun10<Set<T>>) {
        subtest(desc) {
            val set = mutableSetOf<T>()
            for (i in 0 until iter) {
                code(set)
            }
            log.d("# size=${set.size}")
            done(set)
        }
    }

    @Test
    fun testGetNumeric01() {
        val iters = 1000
        val uniques = iters - 10
        fixture<Byte>("getByte()", iters, {
            it.add(RandomUt.getByte())
        }, {
            assertTrue("${it.size}", it.size > 200)
        })
        //
        fixture<Int>("getInt()", iters, {
            it.add(RandomUt.getInt())
        }, {
            assertTrue("${it.size}", it.size >= uniques)
        })
        fixture<Int>("getInt(max)", 1000, {
            val s = RandomUt.getInt(100)
            assertTrue(s in 0..99)
            it.add(s)
        }, {
            assertTrue("${it.size}", it.size >= 95)
        })
        fixture<Int>("getInt(min, max)", 1000, {
            val s = RandomUt.getInt(-50, 50)
            assertTrue(s >= -50 && s < 50)
            it.add(s)
        }, {
            assertTrue("${it.size}", it.size >= 95)
        })
        //
        fixture<Long>("getLong()", 1000, {
            it.add(RandomUt.getLong())
        }, {
            assertTrue("${it.size}", it.size >= uniques)
        })
        fixture<Long>("getLong(max)", 1000, {
            val s = RandomUt.getLong(100)
            assertTrue(s in 0..99)
            it.add(s)
        }, {
            assertTrue("${it.size}", it.size >= 95)
        })
        fixture<Long>("getLong(min, max)", 1000, {
            val s = RandomUt.getLong(-50, 50)
            assertTrue(s >= -50 && s < 50)
            it.add(s)
        }, {
            assertTrue("${it.size}", it.size >= 95)
        })
        //
        fixture<Float>("getFloat()", 1000, {
            it.add(RandomUt.getFloat())
        }, {
            assertTrue("${it.size}", it.size >= uniques)
        })
        fixture<Float>("getFloat(max)", 1000, {
            val s = RandomUt.getFloat(100f)
            assertTrue(s >= 0f && s < 100f)
            it.add(s)
        }, {
            assertTrue("${it.size}", it.size >= uniques)
        })
        fixture<Float>("getFloat(min, max)", 1000, {
            val s = RandomUt.getFloat(-50f, 50f)
            assertTrue(s >= -50f && s < 50f)
            it.add(s)
        }, {
            assertTrue("${it.size}", it.size >= uniques)
        })
        //
        fixture<Double>("getDouble()", 1000, {
            it.add(RandomUt.getDouble())
        }, {
            assertTrue("${it.size}", it.size >= uniques)
        })
        fixture<Double>("getDouble(max)", 1000, {
            val s = RandomUt.getDouble(100.0)
            assertTrue(s >= 0 && s < 100.0)
            it.add(s)
        }, {
            assertTrue("${it.size}", it.size >= uniques)
        })
        fixture<Double>("getDouble(min, max)", 1000, {
            val s = RandomUt.getDouble(-50.0, 50.0)
            assertTrue(s >= -50.0 && s < 50.0)
            it.add(s)
        }, {
            assertTrue("${it.size}", it.size >= uniques)
        })
    }

    @Test
    fun testGetArray01() {
        log.enter(this::testGetArray01) {

            val iters = 10 * 1000

            fun <T> fixture(desc: String, iter: Int, code: Fun10<MutableSet<List<T>>>) {
                subtest(desc) {
                    val set = mutableSetOf<List<T>>()
                    for (i in 0 until iter) {
                        code(set)
                    }
                }
            }

            subtest("Sanity check") {
                val a1 = intArrayOf(1, 2, 3)
                val a2 = intArrayOf(1, 2, 3)
                val a3 = intArrayOf(2, 3, 4)
                val a4 = intArrayOf(2, 3)
                val a5 = intArrayOf(4, 3, 2)
                val a6 = intArrayOf(2, 3)
                val set = mutableSetOf<List<Int>>()
                // Sanity check that set<List<Int>> works.
                assertTrue(set.add(a1.asList()))
                assertFalse(set.add(a2.asList()))
                assertTrue(set.add(a3.asList()))
                assertTrue(set.add(a4.asList()))
                assertTrue(set.add(a5.asList()))
                assertFalse(set.add(a6.asList()))
            }

            fixture<Boolean>("get(BooleanArray)", iters) {
                // Note that this may fail if size==16
                val data = BooleanArray(64)
                RandomUt.get(data)
                assertTrue(it.add(data.asList()))
            }
            fixture<Byte>("get(ByteArray)", iters) {
                //# Note that this may fail if count: 4.
                //# OK for 1000*1000 iter if count:8.
                val data = ByteArray(8)
                RandomUt.get(data)
                assertTrue(it.add(data.asList()))
            }
            fixture<Short>("get(ShortArray)", iters) {
                val data = ShortArray(8)
                RandomUt.get(data)
                assertTrue(it.add(data.asList()))
            }
            fixture<Int>("get(IntArray)", iters) {
                val data = IntArray(8)
                RandomUt.get(data)
                assertTrue(it.add(data.asList()))
            }
            fixture<Long>("get(LongArray)", iters) {
                val data = LongArray(8)
                RandomUt.get(data)
                assertTrue(it.add(data.asList()))
            }
            fixture<Char>("get(CharArray)", iters) {
                val data = CharArray(8)
                RandomUt.get(data)
                assertTrue(it.add(data.asList()))
            }
            fixture<Float>("get(FloatArray)", iters) {
                val data = FloatArray(8)
                RandomUt.get(data)
                assertTrue(it.add(data.asList()))
            }
            fixture<Double>("get(DoubleArray)", iters) {
                val data = DoubleArray(8)
                RandomUt.get(data)
                assertTrue(it.add(data.asList()))
            }
        }
    }

    @Test
    fun testGetArray02() {
        subtest {
            val a = ByteArray(1000)
            RandomUt.get(a, 100, 100)
            val zero = 0.toByte()
            assertTrue(a[0] == zero)
            assertTrue(a[99] == zero)
            assertTrue(a[200] == zero)
            assertTrue(a[200] == zero)
            assertTrue(a[999] == zero)
            var count = 0
            for (i in 100 until 200) {
                if (a[i] == zero) ++count
            }
            log.d("$count")
            assertTrue("$count", count < 5)
        }
        subtest {
            val a = RandomUt.get(IntArray(100), 100)
            val set = mutableSetOf<Int>()
            for (v in a) {
                assertTrue(v in 0..99)
                set.add(v)
            }
            log.d("# set.size=${set.size}")
            assertTrue(set.size >= 50)
        }
        subtest {
            val a = RandomUt.get(LongArray(100), 100)
            val set = mutableSetOf<Long>()
            for (v in a) {
                assertTrue(v in 0..99)
                set.add(v)
            }
            log.d("# set.size=${set.size}")
            assertTrue(set.size >= 50)
        }
    }

    @Test
    fun testGetString01() {
        val iterations = 10 * 1000

        subtest("getWord(min ,max)") {
            val set = mutableSetOf<String>()
            for (i in 0 until iterations) {
                val s = RandomUt.getWord(8, 16)
                assertTrue(s, set.add(s))
            }
            log.d("# set.size=${set.size}")
            assertTrue("${set.size}", set.size >= iterations * 9 / 10)
        }
        subtest("getWords(count, min, max") {
            val set = mutableSetOf<String>()
            set.addAll(RandomUt.getWords(iterations, 8, 16))
            log.d("# set.size=${set.size}")
            assertTrue("${set.size}", set.size >= iterations * 9 / 10)
        }
        subtest("getUniqueWords( min ,max)") {
            val set = mutableSetOf<String>()
            for (i in 0 until iterations) {
                val s = RandomUt.getUniqueWord(8, 16, set)
                assertTrue(set.add(s))
            }
            assertTrue("${set.size}", set.size == iterations)
        }
        subtest("getUniqueWords(count, min ,max)") {
            val set = mutableSetOf<String>()
            set.addAll(RandomUt.getUniqueWords(iterations, 8, 16))
            assertTrue("${set.size}", set.size == iterations)
        }
    }

    @Test
    fun testGetInt01() {
        val set = mutableSetOf<Int>()
        var fails = 0
        //# Note that checking for unique number for 200*1000 iter may fail.
        //# Allow repeat once seems to work OK for 1000*1000 iter.
        //# Using 200*1000 iter here to be safe.
        for (iter in 0 until 200 * 1000) {
            val s = RandomUt.getInt()
            if (!set.add(s)) ++fails
        }
        assertTrue("$fails", fails <= 20)
    }

    @Test
    fun testGetLong() {
        val set = mutableSetOf<Long>()
        for (iter in 0 until 100 * 1000) {
            val s = RandomUt.getLong()
            assertTrue(set.add(s))
        }
    }

    @Test
    fun testGetIntMinMax01() {
        fun check(min: Int, max: Int) {
            for (iter in 0 until 1000) {
                val s = RandomUt.getInt(min, max)
                assertTrue("$iter: $s", s in min..(max - 1))
            }
        }
        check(0, 1)
        check(100, 102)
        check(100, 200)
        for (i in 0 until 100) {
            var min = RandomUt.getInt(0, Int.MAX_VALUE)
            var max = RandomUt.getInt(0, Int.MAX_VALUE)
            if (min > max) {
                val t = min
                min = max
                max = t
            }
            check(min, max)
        }
    }

    @Test
    fun testGetIntMinMax02() {
        val iters = 1000
        val limit = iters * 9 / 10
        fun check(min: Int, max: Int, count: Int? = null) {
            val set = mutableSetOf<Int>()
            for (iter in 0 until iters) {
                val s = RandomUt.getInt(min, max)
                assertTrue("$iter: $s", s in min..(max - 1))
                set.add(s)
            }
            if (count != null) {
                assertTrue(set.size >= count)
            }
        }
        check(0, 1, 1)
        check(-1, 0, 1)
        check(-1, 1, 2)
        check(1, 100, 95)
        check(-100, -1, 95)
        check(-2, 2, 4)
        check(-10, 10, 20)
        check(0, Int.MAX_VALUE, limit)
        check(Int.MIN_VALUE + 1, 0, limit)
        check(Int.MIN_VALUE / 2, Int.MAX_VALUE / 2, limit)
        // check(Int.MIN_VALUE, Int.MAX_VALUE, LIMIT)
        for (i in 0 until 500) {
            val max = RandomUt.getInt(0, Int.MAX_VALUE)
            check(0, max)
        }
    }

    @Test
    fun testPermutate01() {
        val iters = 100
        val data = Array(100) { index -> index }
        subtest {
            for (i in 0 until iters) {
                val copy1 = data.clone()
                RandomUt.permutate(copy1)
                assertFalse(StructUt.equals(data, copy1))
            }
        }
        subtest {
            for (i in 0 until iters) {
                val copy1 = mutableListOf(data)
                RandomUt.permutate(copy1)
                assertFalse(StructUt.equals(data.asList(), copy1))
            }
        }
        subtest {
            for (i in 0 until iters) {
                val list = data.asList()
                val copy = RandomUt.permutated(list)
                assertFalse(StructUt.equals(list, copy))
            }
        }
        subtest {
            val set = mutableSetOf<List<Int>>()
            for (i in 0 until iters) {
                val a = RandomUt.randomSequence(100)
                assertTrue(set.add(a.asList()))
            }
            log.d("# set.size=${set.size}")
            assertEquals("${set.size}", iters, set.size)
        }
    }

    @Test
    fun testRandomInputStream01() {
        subtest {
            val count = 1000
            val input = RandomUtil.RandomInputStream(count)
            val set = mutableSetOf<Int>()
            for (i in 0 until count) {
                val v = input.read()
                assertTrue(v in 0..255)
                set.add(v)
            }
            log.d("# set.size=${set.size}")
            assertTrue("# set.size=${set.size}", set.size > 200)
            assertEquals(-1, input.read())
        }
        subtest {
            val count = 1000
            val input = RandomUtil.RandomInputStream(count)
            val a = ByteArray(500)
            assertEquals(200, input.read(a, 0, 200))
            assertEquals(300, input.read(a, 0, 300))
            assertEquals(500, input.read(a))
            assertEquals(-1, input.read())
            val set = mutableSetOf<Byte>()
            set.addAll(a.asIterable())
            log.d("# set.size=${set.size}")
            assertTrue("# set.size=${set.size}", set.size > 200)
        }
    }
}
