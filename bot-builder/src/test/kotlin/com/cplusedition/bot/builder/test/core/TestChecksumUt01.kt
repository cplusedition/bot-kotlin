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
import com.cplusedition.bot.core.ChecksumUtil
import com.cplusedition.bot.core.ChecksumUtil.ChecksumKind
import com.cplusedition.bot.core.Hex
import com.cplusedition.bot.core.RandomUtil.Companion.RandomUt
import com.cplusedition.bot.core.WithUtil.Companion.With
import com.cplusedition.bot.core.file
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.security.MessageDigest

class TestChecksumUt01 : TestBase() {

    @Test
    fun testBasic01() {
        assertTrue(With.exception {
            ChecksumKind.valueOf("SHA123")
        } is IllegalArgumentException)
        assertEquals(ChecksumKind.MD5, ChecksumKind.valueOf("MD5"))
        assertEquals(ChecksumKind.SHA1, ChecksumKind.valueOf("SHA1"))
        assertEquals(ChecksumKind.SHA256, ChecksumKind.valueOf("SHA256"))
        assertEquals(ChecksumKind.SHA512, ChecksumKind.valueOf("SHA512"))
        assertEquals(ChecksumKind.SHA512, ChecksumKind.get("SHA512"))
        assertEquals(null, ChecksumKind.get("SHA-512"))
        assertEquals(ChecksumKind.MD5, ChecksumKind.get(ChecksumKind.MD5.algorithm))
        assertEquals(ChecksumKind.MD5.algorithm, ChecksumKind.MD5.toString())
    }

    @Test
    fun testChecksumUt01() {
        fun formata(kind: ChecksumKind, hex: String, data: ByteArray) {
            val datafile = tmpFile()
            val sumfile = tmpFile()
            try {
                datafile.writeBytes(data)
                sumfile.writeText("${kind.name}($sumfile.name) = $hex")
                val line = sumfile.readText()
                val expected =
                    if (RandomUt.getBool()) ChecksumKind.readChecksum1(line, kind) else ChecksumKind.readChecksum1(line)
                if (expected == null) fail()
                else {
                    val actual = Hex.encode(kind.getDigester().digest(datafile.readBytes())).toString()
                    val ret = ChecksumUtil.ExpectedAndActual(expected.sum.toLowerCase(), actual)
                    assertEquals(hex.toLowerCase(), ret.expected)
                    assertEquals(ret.expected, ret.actual)
                    assertTrue(ret.match())
                }
            } finally {
                datafile.delete()
                sumfile.delete()
            }
        }

        fun formatax(kind: ChecksumKind, hex: String, data: ByteArray) {
            val datafile = tmpFile()
            val sumfile = tmpFile()
            try {
                datafile.writeBytes(data)
                sumfile.writeText("SHA123($sumfile.name) = $hex")
                val line = sumfile.readText()
                assertTrue(With.exception { ChecksumKind.readChecksum1(line, kind) } is IOException)
            } finally {
                datafile.delete()
                sumfile.delete()
            }
        }

        fun formatb(kind: ChecksumKind, hex: String, data: ByteArray) {
            val datafile = tmpFile()
            val sumfile = tmpFile()
            try {
                datafile.writeBytes(data)
                val s1 = if (RandomUt.getBool()) "*" else " "
                val s2 = if (s1 == " " && RandomUt.getBool()) "*" else " "
                sumfile.writeText("$hex$s1$s2${datafile.name}")
                val ret = kind.read(sumfile, datafile)
                assertEquals(hex.toLowerCase(), ret.expected)
                assertEquals(ret.expected, ret.actual)
                assertTrue(ret.match())
            } finally {
                datafile.delete()
                sumfile.delete()
            }
        }

        fun formatc(kind: ChecksumKind, hex: String, data: ByteArray) {
            val datafile = tmpFile()
            val sumfile = tmpFile()
            try {
                datafile.writeBytes(data)
                sumfile.writeText("(${sumfile.absolutePath}) = $hex")
                val ret = kind.read(sumfile, datafile)
                assertEquals(hex.toLowerCase(), ret.expected)
                assertEquals(ret.expected, ret.actual)
                assertTrue(ret.match())
            } finally {
                datafile.delete()
                sumfile.delete()
            }
        }

        fun formatd(kind: ChecksumKind, hex: String, data: ByteArray) {
            val datafile = tmpFile()
            val sumfile = tmpFile()
            try {
                val withpath = RandomUt.getBool()
                val s = if (withpath) " ${RandomUt.getWord(1, RandomUt.getInt(1, 32))}" else ""
                datafile.writeBytes(data)
                sumfile.writeText("$hex$s")
                val sumandpath = kind.readChecksum1(sumfile)
                if (sumandpath == null) fail()
                else {
                    assertEquals(hex, sumandpath.sum)
                    if (withpath) {
                        assertTrue(sumandpath.path.isNotEmpty())
                    } else {
                        assertTrue(sumandpath.path.isEmpty())
                    }
                    val ret = kind.read(sumfile, datafile)
                    assertEquals(hex.toLowerCase(), ret.expected)
                    assertEquals(ret.expected, ret.actual)
                    assertTrue(ret.match())
                }
            } finally {
                datafile.delete()
                sumfile.delete()
            }
        }

        fun formatInvalid(kind: ChecksumKind, hex: String, data: ByteArray) {
            val datafile = tmpFile()
            val sumfile = tmpFile()
            try {
                datafile.writeBytes(data)
                sumfile.writeText("${sumfile.name} $hex")
                assertTrue(With.exception { kind.read(sumfile, datafile) } is IOException)
            } finally {
                datafile.delete()
                sumfile.delete()
            }
        }
        log.enterX {
            for (size in arrayOf(0, 1, 2, 5, 10, 16, 32, 100, 256, 1000, 1000 * 1000)) {
                for (kind in ChecksumKind.values()) {
                    val data = RandomUt.get(ByteArray(size))
                    val expected = MessageDigest.getInstance(kind.algorithm).digest(data)
                    val hex = Hex.encode(expected, RandomUt.getBool()).toString()
                    formata(kind, hex, data)
                    formatb(kind, hex, data)
                    formatc(kind, hex, data)
                    formatd(kind, hex, data)
                    formatax(kind, hex, data)
                    formatInvalid(kind, hex, data)
                }
            }
        }
    }

    @Test
    fun testDigest01() {
        val file = testResDir.file("html/manual.html")
        assertEquals("f9e79cada87767af0d9da92da49e2c70", ChecksumKind.MD5.digest(file))
        assertEquals("87d22119a93b89d77892f8e2a9a08c2efcdd74c1", ChecksumKind.SHA1.digest(file))
        assertEquals(
            "1d610e8f2041bf41702c8227bde26ea698346a355e07247781b4c104f60f0d57",
            ChecksumKind.SHA256.digest(file)
        )
        assertEquals(
            "c7ec3ddffaae9799299bb6f417e2ead153726f52a06af83cf635a849273f64d523cab88acab39523ec07b348ca76aa73d5d50cef821a4aaa734fa52fe0333a1c",
            ChecksumKind.SHA512.digest(file)
        )
        assertTrue(ChecksumKind.MD5.verify("f9e79cada87767af0d9da92da49e2c70", file))
        assertTrue(ChecksumKind.SHA1.verify("87d22119a93b89d77892f8e2a9a08c2efcdd74c1", file))
        assertTrue(ChecksumKind.SHA256.verify("1d610e8f2041bf41702c8227bde26ea698346a355e07247781b4c104f60f0d57", file))
        assertTrue(
            ChecksumKind.SHA512.verify(
                "c7ec3ddffaae9799299bb6f417e2ead153726f52a06af83cf635a849273f64d523cab88acab39523ec07b348ca76aa73d5d50cef821a4aaa734fa52fe0333a1c",
                file
            )
        )
    }
}