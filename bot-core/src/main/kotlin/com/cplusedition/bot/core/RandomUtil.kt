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

package com.cplusedition.bot.core

import java.io.IOException
import java.io.InputStream
import java.security.SecureRandom
import java.util.*

val RandomUt = RandomUtil.singleton

open class RandomUtil(private val random: Random) {

    companion object {
        // For Android Kotlin compatibility, which does not provide the withInitialValue() static method.
        private val singletons: ThreadLocal<RandomUtil> = object : ThreadLocal<RandomUtil>() {
            override fun initialValue(): RandomUtil {
                return RandomUtil(SecureRandom())
            }
        }
        val singleton: RandomUtil = singletons.get()!!
    }

    ////////////////////////////////////////////////////////////////////////

    fun getBool() = random.nextBoolean()

    fun getByte() = random.nextInt().toByte()

    fun getInt() = random.nextInt()

    fun getInt(max: Int) = if (max == 0) 0 else random.nextInt(max)

    fun getInt(min: Int, max: Int) = getInt(max - min) + min

    fun getLong() = random.nextLong()

    /** @return A position long value in range 0..<max. */
    fun getLong(max: Long): Long {
        if (max == 0L) return 0L
        if (max < 0) throw AssertionError()
        var ret = random.nextLong()
        if (ret == java.lang.Long.MIN_VALUE) {
            ++ret
        }
        return Math.abs(ret) % max
    }

    fun getLong(min: Long, max: Long) = getLong(max - min) + min

    fun getFloat() = random.nextFloat()

    /** @return A positive float value in range 0..<max. */
    fun getFloat(max: Float): Float {
        if (max == 0f) return 0f
        if (max < 0) throw AssertionError()
        return Math.abs(random.nextFloat()) % max
    }

    fun getFloat(min: Float, max: Float) = getFloat(max - min) + min

    fun getDouble() = random.nextDouble()

    /** @return A position double value in range 0..<max. */
    fun getDouble(max: Double): Double {
        if (max == 0.0) return 0.0
        if (max < 0) throw AssertionError()
        return Math.abs(random.nextDouble()) % max
    }

    fun getDouble(min: Double, max: Double) = getDouble(max - min) + min

    ////////////////////////////////////////////////////////////////////////

    fun get(ret: DoubleArray): DoubleArray {
        for (i in ret.indices) {
            ret[i] = random.nextDouble()
        }
        return ret
    }

    fun get(ret: FloatArray): FloatArray {
        for (i in ret.indices) {
            ret[i] = random.nextFloat()
        }
        return ret
    }

    fun get(ret: LongArray): LongArray {
        for (i in ret.indices) {
            ret[i] = random.nextLong()
        }
        return ret
    }

    fun get(ret: IntArray): IntArray {
        for (i in ret.indices) {
            ret[i] = random.nextInt()
        }
        return ret
    }

    fun get(ret: CharArray): CharArray {
        for (i in ret.indices) {
            ret[i] = getInt(0x10000).toChar()
        }
        return ret
    }

    fun get(ret: ShortArray): ShortArray {
        for (i in ret.indices) {
            ret[i] = random.nextInt().toShort()
        }
        return ret
    }

    fun get(ret: ByteArray): ByteArray {
        random.nextBytes(ret)
        return ret
    }

    fun get(ret: BooleanArray): BooleanArray {
        val len = (ret.size / 8) + 1
        val a = ByteArray(len)
        random.nextBytes(a)
        val bits = BitSet.valueOf(a)
        for (i in ret.indices) {
            ret[i] = bits.get(i)
        }
        return ret
    }

    fun get(ret: ByteArray, off: Int, len: Int) {
        val b = ByteArray(len)
        random.nextBytes(b)
        System.arraycopy(b, 0, ret, off, len)
    }

    fun get(ret: IntArray, max: Int): IntArray {
        for (i in ret.indices) {
            ret[i] = getInt(max)
        }
        return ret
    }

    fun get(ret: LongArray, max: Long): LongArray {
        for (i in ret.indices) {
            ret[i] = getLong(max)
        }
        return ret
    }

    ////////////////////////////////////////////////////////////////////////

    fun getASCII(predicate: (Char) -> Boolean): Char {
        while (true) {
            val c = getInt(0x80).toChar()
            if (predicate(c)) {
                return c
            }
        }
    }

    fun getString(min: Int, max: Int, factory: (Int) -> Char): String {
        val len = getInt(min, max)
        return String(CharArray(len, factory))
    }

    val letter: Char
        get() = getASCII { it in 'a'..'z' || it in 'A'..'Z' }

    val letterOrDigit: Char
        get() = getASCII { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' }

    fun getWord(min: Int, max: Int): String {
        return getString(min, max) { if (it == 0) letter else letterOrDigit }
    }

    fun getWords(count: Int, min: Int, max: Int): Array<String> {
        return Array(count) { getWord(min, max) }
    }

    fun getUniqueWord(min: Int, max: Int, set: Set<String>): String {
        while (true) {
            val s = getWord(min, max)
            if (!set.contains(s)) {
                return s
            }
        }
    }

    fun getUniqueWords(count: Int, min: Int, max: Int): Array<String> {
        val set = TreeSet<String>()
        return Array(count) { getUniqueWord(min, max, set) }
    }

    /**
     * Generate a random sequence from 0 to size.
     */
    fun randomSequence(size: Int): Array<Int> {
        val ret = Array(size) { i -> i }
        permutate(ret)
        return ret
    }

    /**
     * Generate a random permutation of given array in place.
     */
    fun <T> permutate(ret: Array<T>) {
        val size = ret.size
        for (i in ret.indices) {
            val index = getInt(size)
            val v = ret[i]
            ret[i] = ret[index]
            ret[index] = v
        }
    }

    /**
     * Generate a random permutation of given list in place.
     */
    fun <T> permutate(ret: MutableList<T>) {
        val size = ret.size
        for (i in ret.indices) {
            val index = getInt(size)
            val v = ret[i]
            ret[i] = ret[index]
            ret[index] = v
        }
    }

    /**
     * Generate a random permutation copy of given list.
     */
    fun <T> permutated(input: List<T>): List<T> {
        val ret = ArrayList(input)
        permutate(ret)
        return ret
    }

    fun sleep(min: Long, max: Long) {
        try {
            Thread.sleep(getLong(min, max))
        } catch (e: InterruptedException) {
            // Ignore
        }

    }

    ////////////////////////////////////////////////////////////////////////

    open class RandomInputStream(
            private val length: Int
    ) : InputStream() {

        private val random = RandomUt
        private var offset = 0

        @Throws(IOException::class)
        override fun read(b: ByteArray, off: Int, len: Int): Int {
            val avail = length - offset
            if (avail <= 0) return -1
            val ret = if (len > avail) avail else len
            random.get(b, off, ret)
            offset += ret
            return ret
        }

        @Throws(IOException::class)
        override fun read(): Int {
            if (offset >= length) {
                return -1
            }
            offset += 1
            return random.getInt() and 0xff
        }

    }

    ////////////////////////////////////////////////////////////////////////
}