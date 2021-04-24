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

import com.cplusedition.bot.core.WithUtil.Companion.With
import com.cplusedition.bot.core.WithoutUtil.Companion.Without
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.*

open class ChecksumUtil {

    class SumAndPath(
            val sum: String,
            val path: String // Empty string if no path is missing.
    )

    class ExpectedAndActual(
            val expected: String,
            val actual: String
    ) {
        fun match(): Boolean {
            return actual == expected
        }
    }

    enum class ChecksumKind(val algorithm: String) {
        MD5("MD5"),
        SHA1("SHA1"),
        SHA256("SHA-256"),
        SHA512("SHA-512");

        override fun toString(): String {
            return algorithm
        }

        companion object {
            private val digesters = ThreadLocal<MutableMap<ChecksumKind, MessageDigest>>()

            fun get(name: String): ChecksumKind? {
                return try {
                    valueOf(name)
                } catch (e: Throwable) {
                    null
                }
            }

            /** @param hexlen Length of hex encoded checksum. */
            fun get(hexlen: Int): ChecksumKind? {
                return when (hexlen) {
                    32 -> MD5
                    40 -> SHA1
                    64 -> SHA256
                    128 -> SHA512
                    else -> null
                }
            }

            @Throws(IOException::class)
            fun readChecksum1(line: String, kind: ChecksumKind? = null): SumAndPath? {
                return FormatA.match(line, kind) ?: FormatB.match(line) ?: FormatC.match(line) ?: FormatD.match(line)
            }

            /** Like readChecksum() but allow sum only input. */
            @Throws(IOException::class)
            fun readChecksum0(line: String, kind: ChecksumKind? = null): SumAndPath {
                return FormatE.match(line) ?: FormatA.match(line, kind) ?: FormatB.match(line) ?: FormatC.match(line)
                ?: FormatD.match(line)
                ?: throw IOException("Invalid checksum line: $line")
            }
        }

        fun getDigester(): MessageDigest {
            var local = digesters.get()
            if (local == null) {
                local = TreeMap()
                digesters.set(local)
            }
            var digester = local[this]
            if (digester == null) {
                digester = MessageDigest.getInstance(algorithm)
                local[this] = digester
                return digester
            }
            digester.reset()
            return digester
        }

        object FormatA {
            private val format = Regex("(?s)^\\s*(\\w+)\\s*\\((.*)\\)\\s*= (\\S.*?)\\s*$") // kind(path) = sum
            fun match(line: String, kind: ChecksumKind?): SumAndPath? {
                val m = format.matchEntire(line) ?: return null
                val sumkind = m.groupValues[1].toUpperCase()
                if (kind != null && kind != get(sumkind)) {
                    throw IOException("Checksum kind mismatch: expected=kind, actual=$sumkind")
                }
                return SumAndPath(m.groupValues[3], m.groupValues[2])
            }
        }

        object FormatB {
            private val format = Regex("(?s)^\\s*\\(?(.*?)\\)?\\s*=\\s+(\\S.*?)\\s*$") // (path) = sum
            fun match(line: String): SumAndPath? {
                val m = format.matchEntire(line) ?: return null
                return SumAndPath(m.groupValues[2], m.groupValues[1])
            }
        }

        object FormatC {
            private val format = Regex("(?s)^\\s*([0-9A-Fa-f]+)(?:\\s\\*|\\*\\s|\\s\\s)(.*?)\\s*$") // sum  path
            fun match(line: String): SumAndPath? {
                val m = format.matchEntire(line) ?: return null
                return SumAndPath(m.groupValues[1], m.groupValues[2])
            }
        }

        object FormatD {
            private val format = Regex("(?s)^\\s*([0-9A-Fa-f]+)(?:\\s+(\\S.*?))?\\s*$") // sum path?
            fun match(line: String): SumAndPath? {
                val m = format.matchEntire(line) ?: return null
                return SumAndPath(m.groupValues[1], m.groupValues[2])
            }
        }

        object FormatE {
            private val format = Regex("(?s)^\\s*([0-9A-Fa-f]+)\\s*$") // sum
            fun match(line: String): SumAndPath? {
                val m = format.matchEntire(line) ?: return null
                return SumAndPath(m.groupValues[1], "")
            }
        }

        @Throws(IOException::class)
        fun read(sumfile: File, datafile: File): ExpectedAndActual {
            val expected = readChecksum1(sumfile) ?: throw IOException()
            val actual = digest(datafile)
            return ExpectedAndActual(expected.sum.toLowerCase(), actual)
        }

        @Throws(IOException::class)
        fun readChecksum1(sumfile: File): SumAndPath? {
            return readChecksum1(sumfile.readText(), this)
        }

        @Throws(IOException::class)
        fun readChecksums(sumfile: File, callback: (SumAndPath?, String) -> Unit) {
            for (line in sumfile.readLines()) {
                if (line.isEmpty()) continue
                callback(readChecksum1(line, this), line)
            }
        }

        @Throws(IOException::class)
        fun digest(datafile: File): String {
            val digister = this.getDigester()
            With.bytes(datafile, 256 * 1024) { b, n ->
                if (n > 0) {
                    digister.update(b, 0, n)
                }
            }
            return Hex.encode(digister.digest()).toString()
        }

        @Throws(IOException::class)
        fun verify(expected: String, datafile: File): Boolean {
            return expected.toLowerCase() == digest(datafile)
        }

        /** Like read() but allow sum only input. */
        @Throws(IOException::class)
        fun read0(sumfile: File, datafile: File): ExpectedAndActual {
            val kindstring = Basepath.ext(sumfile.name)?.toUpperCase() ?: throw IOException("Invalid checksum file: $sumfile")
            Without.throwableOrNull { valueOf(kindstring) } ?: throw IOException("Invalid checksum file: $sumfile")
            val expected = readChecksum01(sumfile)
            val actual = digest(datafile)
            return ExpectedAndActual(expected.sum.toLowerCase(), actual)
        }

        /** Like readChecksum1() but allow sum only input. */
        @Throws(IOException::class)
        fun readChecksum01(sumfile: File): SumAndPath {
            return readChecksum0(sumfile.readText())
        }

        /** Like readChecksums() but allow sum only input. */
        fun readChecksums0(sumfile: File, callback: (SumAndPath?, String) -> Unit) {
            for (line in sumfile.readLines()) {
                if (line.isEmpty()) continue
                callback(readChecksum0(line, this), line)
            }
        }
    }
}