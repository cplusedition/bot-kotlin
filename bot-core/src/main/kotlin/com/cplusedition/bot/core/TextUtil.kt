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

import java.io.File
import java.util.*

object TextUt : TextUtil()

open class TextUtil {

    val LB = System.getProperty("line.separator")!!
    val DEC_SIZE_UNIT = arrayOf("", "k", "m", "g", "t")

    fun isEmpty(s: CharSequence?): Boolean {
        return s == null || s.isEmpty()
    }

    /**
     * @return Values with 4 or less digits, 1000 as divider, rounding up.
     */
    fun decUnit4(size: Long): Pair<Long, String> {
        return valueUnit(DEC_SIZE_UNIT, 10000, 1000, 555, size)
    }

    /** Like decUnit4(size) but return a String instead of a Pair. */
    fun decUnit4String(size: Long, suffix: String? = ""): String {
        val sizeunit = decUnit4(size)
        val s = sizeunit.second + suffix
        return if (s.isEmpty()) "${sizeunit.first}" else "${sizeunit.first} $s"
    }

    fun decUnit4String(file: File): String {
        return decUnit4String(file.length(), "B")
    }

    /**
     * @return Value, unit Pair where value is less than max, eg. return at most 4 digits if max is 10000.
     */
    fun valueUnit(units: Array<String>, max: Float, divider: Float, size: Float): Pair<Float, String> {
        val positive = size >= 0
        var value = if (positive) size else -size
        var unit = 0
        val len = units.size
        while (unit < len - 1) {
            if (value < max) {
                break
            }
            value /= divider
            ++unit
        }
        value = if (positive) value else -value
        return Pair(value, units[unit])
    }

    /**
     * @return Value, unit Pair where value is less than max, eg. return at most 4 digits if max is 10000.
     */
    fun valueUnit(units: Array<String>, max: Long, divider: Long, rounding: Long, size: Long): Pair<Long, String> {
        val positive = size >= 0
        var value = if (positive) size else -size
        var unit = 0
        val len = units.size
        while (unit < len - 1) {
            if (value < max) {
                break
            }
            value = (value + rounding) / divider
            ++unit
        }
        value = if (positive) value else -value
        return Pair(value, units[unit])
    }

    /**
     * Split at the first occurence of the sep.
     *
     * @return (s, null) if sep not found.
     */
    fun split2(sep: String, s: String): Pair<String, String?> {
        val index = s.indexOf(sep)
        if (index < 0) return Pair(s, null)
        return Pair(s.substring(0, index), s.substring(index + sep.length))
    }

    /**
     * Split using StringTokenizer, ignore empty segments. Example: split("/", "a//b/")
     * returns ("a", "b")
     */
    fun split(delimiters: String, s: String, transform: IStringTransformer): List<String> {
        val t = StringTokenizer(s, delimiters)
        val ret = ArrayList<String>()
        while (t.hasMoreTokens()) {
            val value = transform(t.nextToken())
            if (value != null) ret.add(value)
        }
        return ret
    }

    /** Format using ROOT locale, ie. locale independent. */
    fun format(format: String, vararg args: Any): String {
        return String.format(Locale.ROOT, format, *args)
    }

    fun toLowerCase(value: String, locale: Locale = Locale.ROOT): String {
        return value.toLowerCase(locale)
    }

    fun toUpperCase(value: String, locale: Locale = Locale.ROOT): String {
        return value.toUpperCase(locale)
    }
}

open class Hex {
    companion object {
        const val LOWER = "0123456789abcdef"
        const val UPPER = "0123456789ABCDEF"

        @Throws(NumberFormatException::class)
        fun decode(hex: CharSequence): ByteArray {
            val len = hex.length
            if (len.isOdd()) throw NumberFormatException(hex.toString())
            var i = 0
            var o = 0
            val ret = ByteArray(len / 2)
            while (i < len) {
                ret[o++] = ((decode(hex[i++]) shl 4) + decode(hex[i++])).toByte()
            }
            return ret
        }

        fun encode(bytes: ByteArray, uppercase: Boolean = false): CharSequence {
            val ret = StringBuilder()
            val hex = if (uppercase) UPPER else LOWER
            for (b in bytes) {
                val n = b.toInt() and 0xff
                ret.append(hex[n ushr 4])
                ret.append(hex[n and 0x0f])
            }
            return ret
        }

        fun toString(array: ByteArray): String {
            val ret = StringBuilder()
            for (b in array) {
                if (ret.isNotEmpty()) ret.append(", ")
                val n = b.toInt() and 0xff
                ret.append("0x" + (n.toString(16).padStart(2, '0')))
            }
            return ret.toString()
        }

        @Throws(NumberFormatException::class)
        fun decode(c: Char): Int {
            val n = c.toInt()
            return if (n in 0x61..0x66) 10 + n - 0x61
            else if (n in 0x41..0x46) 10 + n - 0x41
            else if (n in 0x30..0x39) n - 0x30
            else throw NumberFormatException(n.toString())
        }
    }
}
