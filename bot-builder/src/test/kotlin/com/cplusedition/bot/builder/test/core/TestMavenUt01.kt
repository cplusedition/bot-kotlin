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
import com.cplusedition.bot.core.MavenUtil.*
import com.cplusedition.bot.core.RandomUtil.Companion.RandomUt
import com.cplusedition.bot.core.WithUtil.Companion.With
import com.cplusedition.bot.core.joinln
import org.junit.Assert.*
import org.junit.Test
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.ArrayList

class TestMavenUt01 : TestBase() {

    @Test
    fun testGA01() {
        fun check(groupid: String?, artifactid: String?, ga: GA?) {
            assertEquals("$ga", groupid, ga?.groupId)
            assertEquals("$ga", artifactid, ga?.artifactId)
        }
        subtest {
            check(null, null, GA.from(""))
            check(null, null, GA.from("com"))
            check(null, null, GA.from("com.bot"))
            check(null, null, GA.from("com:"))
            check(null, null, GA.from(":com"))
            check(null, null, GA.from("com/"))
            check(null, null, GA.from("/com"))
            check("a", "bot/core", GA.from("a:bot/core"))
            check("a.b", "bot-core", GA.from("a/b:bot-core"))
            check("com.bot", "bot-core", GA.from("com.bot:bot-core"))
            check("com", "bot", GA.from("com/bot"))
            check("com.cplusedition", "bot", GA.from("com/cplusedition/bot"))
            check("com.cplusedition.bot", "bot-core", GA.from("com/cplusedition/bot/bot-core"))
        }
        subtest {
            With.exceptionOrFail { GA.of("") }
            With.exceptionOrFail { GA.of("com") }
            With.exceptionOrFail { GA.of("com.bot") }
            With.exceptionOrFail { GA.of("com:") }
            With.exceptionOrFail { GA.of(":com") }
            With.exceptionOrFail { GA.of("com/") }
            With.exceptionOrFail { GA.of("/com") }
            check("a", "bot/core", GA.of("a:bot/core"))
            check("a.b", "bot-core", GA.of("a/b:bot-core"))
            check("com.bot", "bot-core", GA.of("com.bot:bot-core"))
            check("com", "bot", GA.of("com/bot"))
            check("com.cplusedition", "bot", GA.of("com/cplusedition/bot"))
            check("com.cplusedition.bot", "bot-core", GA.of("com/cplusedition/bot/bot-core"))
            check("com.cplusedition.bot", "bot-core", GA.of("com/cplusedition/bot:bot-core"))
            check("com.cplusedition.bot", "bot-core", GA.of("com.cplusedition.bot/bot-core"))
        }
        subtest {
            val set = TreeSet<GA>()
            assertTrue(set.add(GA.of("a/b/c")))
            assertFalse(set.add(GA.of("a/b/c")))
            assertTrue(set.add(GA.of("a/b/c:d:e")))
            assertFalse(set.add(GA.of("a/b/c:d")))
            assertTrue(set.add(GA.of("a:b")))
            assertTrue(set.add(GA.of("aa:b")))
            assertTrue(set.add(GA.of("a:bb")))
            assertFalse(set.add(GA.of("aa/b")))
            assertFalse(set.add(GA.of("a/bb")))
        }
        subtest {
            val set = HashSet<GA>()
            assertTrue(set.add(GA.of("a/b/c")))
            assertFalse(set.add(GA.of("a/b/c")))
            assertTrue(set.add(GA.of("a/b/c:d:e")))
            assertFalse(set.add(GA.of("a/b/c:d")))
            assertTrue(set.add(GA.of("a:b")))
            assertTrue(set.add(GA.of("aa:b")))
            assertTrue(set.add(GA.of("a:bb")))
            assertFalse(set.add(GA.of("aa/b")))
            assertFalse(set.add(GA.of("a/bb")))
        }
        subtest {
            assertEquals("com/cplusedition/bot/bot-core", GA.of("com.cplusedition.bot/bot-core").path)
            assertEquals("com.cplusedition.bot:bot-core", GA.of("com/cplusedition/bot/bot-core").toString())
        }
    }

    @Test
    fun testGAReadWrite01() {
        val oks = listOf(
            "a:bot/core",
            "a/b:bot-core",
            "com.bot:bot-core",
            "com/bot",
            "com/cplusedition/bot",
            "com/cplusedition/bot/bot-core"
        )
        subtest {
            val file = tmpFile()
            GA.write(file, oks.map { GA.of(it) })
            val ret = ArrayList<GA>()
            GA.read(ret, file)
            assertEquals(6, ret.size)
        }
        subtest {
            val file = tmpFile()
            val fails = listOf(
                "com",
                "com.bot",
                "com:",
                ":com",
                "com/",
                "/com"
            )
            val list = ArrayList<String>()
            list.addAll(oks)
            list.addAll(fails)
            RandomUt.permutate(list)
            file.writeText(list.joinln())
            val ret = ArrayList<GA>()
            var failcount = 0
            GA.read(ret, file) {
                assertTrue(fails.contains(it))
                ++failcount
            }
            assertEquals(fails.size, failcount)
            assertEquals(oks.size, ret.size)
        }
    }

    @Test
    fun testGAV01() {
        fun check(groupid: String?, artifactid: String?, version: String?, gav: GAV?) {
            assertEquals("$gav", groupid, gav?.groupId)
            assertEquals("$gav", artifactid, gav?.artifactId)
            assertEquals("$gav", version, gav?.version?.toString())
        }
        subtest {
            check(null, null, null, GAV.from(""))
            check(null, null, null, GAV.from(":"))
            check(null, null, null, GAV.from("/"))
            check(null, null, null, GAV.from("::"))
            check(null, null, null, GAV.from("//"))
            check(null, null, null, GAV.from("com"))
            check(null, null, null, GAV.from("com.bot"))
            check(null, null, null, GAV.from("com:"))
            check(null, null, null, GAV.from(":com"))
            check(null, null, null, GAV.from("com/"))
            check(null, null, null, GAV.from("/com"))
            check(null, null, null, GAV.from("com:bot:"))
            check(null, null, null, GAV.from(":bot:123"))
            check(null, null, null, GAV.from(":bot:123:"))
            check(null, null, null, GAV.from("com/bot"))
            check(null, null, null, GAV.from("/com/bot"))
            check(null, null, null, GAV.from("com/bot/"))
            check(null, null, null, GAV.from("/com/bot/"))
            check("a", "bot/core", "1.0", GAV.from("a:bot/core:1.0"))
            check("a.b", "bot-core", "1.0", GAV.from("a/b:bot-core:1.0"))
            check("com.bot", "bot-core", "1.0", GAV.from("com.bot:bot-core:1.0"))
            check("com", "bot", "1.0", GAV.from("com/bot/1.0"))
            check("com.cplusedition", "bot", "1.0", GAV.from("com/cplusedition/bot/1.0"))
            check("com.cplusedition.bot", "bot-core", "1.0", GAV.from("com/cplusedition/bot/bot-core/1.0"))
            check("com.cplusedition.bot", "bot-core", "1.0", GAV.from("com/cplusedition/bot:bot-core:1.0"))
            check("com.cplusedition.bot", "bot-core", "1.0", GAV.from("com.cplusedition.bot/bot-core/1.0"))
        }
        subtest {
            With.exceptionOrFail { GAV.of("") }
            With.exceptionOrFail { GAV.of("com") }
            With.exceptionOrFail { GAV.of("com.bot") }
            With.exceptionOrFail { GAV.of("com:") }
            With.exceptionOrFail { GAV.of(":com") }
            With.exceptionOrFail { GAV.of("com/") }
            With.exceptionOrFail { GAV.of("/com") }
            check("a", "bot/core", "1.0", GAV.of("a:bot/core:1.0"))
            check("a.b", "bot-core", "1.0", GAV.of("a/b:bot-core:1.0"))
            check("com.bot", "bot-core", "1.0", GAV.of("com.bot:bot-core:1.0"))
            check("com", "bot", "1.0", GAV.of("com/bot/1.0"))
            check("com.cplusedition", "bot", "1.0", GAV.of("com/cplusedition/bot/1.0"))
            check("com.cplusedition.bot", "bot-core", "1.0", GAV.of("com/cplusedition/bot/bot-core/1.0"))
            check("com.cplusedition.bot", "bot-core", "1.0", GAV.of("com/cplusedition/bot:bot-core:1.0"))
            check("com.cplusedition.bot", "bot-core", "1.0", GAV.of("com.cplusedition.bot/bot-core/1.0"))
            check(
                "com.cplusedition.bot",
                "bot-core",
                "1.0",
                GAV.of("com.cplusedition.bot/bot-core/1.0/bot-core-1.0.pom")
            )
        }
        subtest {
            val set = TreeSet<GAV>()
            assertTrue(set.add(GAV.of("a/b/c/1.0")))
            assertFalse(set.add(GAV.of("a/b/c/1.0")))
            assertTrue(set.add(GAV.of("a/b/c:d:1.0")))
            assertFalse(set.add(GAV.of("a/b/c:d:1.0")))
            assertTrue(set.add(GAV.of("a:b:1.0")))
            assertTrue(set.add(GAV.of("aa:b:1.0")))
            assertTrue(set.add(GAV.of("a:bb:1.0")))
            assertFalse(set.add(GAV.of("aa/b/1.0")))
            assertFalse(set.add(GAV.of("a/bb/1.0")))
            assertTrue(set.add(GAV.of("a:bb:1.1")))
            assertTrue(set.add(GAV.of("b:bb:1.9")))
            assertTrue(set.add(GAV.of("b:bb:2.0")))
            assertEquals(8, set.size)
            log.d("# Natural order")
            log.d(set.map { it.toString() })
            val reverse = TreeSet<GAV>(GAV.ReversedVersionComparator)
            reverse.addAll(set)
            log.d("# Reverse order")
            log.d(reverse.map { it.toString() })
            assertEquals(4, reverse.size)
            assertEquals("b:bb:2.0", reverse.first().toString())
        }
        subtest {
            val set = HashSet<GAV>()
            assertTrue(set.add(GAV.of("a/b/c/1.0")))
            assertFalse(set.add(GAV.of("a/b/c/1.0")))
            assertTrue(set.add(GAV.of("a/b/c:d:1.0")))
            assertFalse(set.add(GAV.of("a/b/c:d:1.0")))
            assertTrue(set.add(GAV.of("a:b:1.0")))
            assertTrue(set.add(GAV.of("aa:b:1.0")))
            assertTrue(set.add(GAV.of("a:bb:1.0")))
            assertFalse(set.add(GAV.of("aa/b/1.0")))
            assertFalse(set.add(GAV.of("a/bb/1.0")))
            assertTrue(set.add(GAV.of("a:bb:1.1")))
            assertTrue(set.add(GAV.of("a:bb:1.1-alpha")))
        }
        subtest {
            assertEquals("com.cplusedition.bot:bot-core:1.0", GAV.of("com/cplusedition/bot/bot-core/1.0").toString())
            val gav = GAV.of("com.cplusedition.bot/bot-core/1.0")
            assertEquals("com/cplusedition/bot/bot-core/1.0", gav.path)
            assertEquals("com.cplusedition.bot:bot-core:1.0", gav.gav)
            assertEquals("com/cplusedition/bot/bot-core/1.0/bot-core-1.0", gav.artifactPath)
            assertEquals("com/cplusedition/bot/bot-core/1.0/bot-core-1.0.pom", gav.artifactPath(".pom"))
            val list = ArrayList<String>()
            gav.artifactPath(list, ".pom")
            assertEquals("com/cplusedition/bot/bot-core/1.0/bot-core-1.0.pom", list.first())
        }
    }

    @Test
    fun testGAvReadWrite01() {
        val oks = arrayOf(
            "com.cplusedition:bot:1.0.0",
            "com/cplusedition/bot/1.1",
            "com/cplusedition/bot/bot-core/1.2.0",
            "com/cplusedition/bot/bot-core/1.2.1-alpha-1",
            "com/cplusedition/bot/bot-core/1.2.1-alpha-9",
            "com/cplusedition/bot/bot-core/1.2.1-alpha-10",
            "com/cplusedition/bot/bot-core/1.2.1-beta-2",
            "com/cplusedition/bot/bot-core/1.2.1",
            "com/cplusedition/bot/bot-core/1.2.1.3",
            "com/cplusedition/bot/1.2.1.4",
            "com/cplusedition/bot/1.3.0",
            "com/cplusedition/bot/1.4.0"
        )
        subtest {
            val file = tmpFile()
            GAV.write(file, oks.map { GAV.of(it) })
            val ret = ArrayList<GAV>()
            GAV.read(ret, file)
            assertEquals(oks.size, ret.size)
        }
        subtest {
            val file = tmpFile()
            val fails = listOf(
                "com",
                "com.bot",
                "com:bot:",
                ":com:bot",
                "com/1.0",
                "/com"
            )
            val list = ArrayList<String>()
            list.addAll(oks)
            list.addAll(fails)
            RandomUt.permutate(list)
            file.writeText(list.joinln())
            val ret = ArrayList<GAV>()
            var failcount = 0
            GAV.read(ret, file) {
                assertTrue(it, fails.contains(it))
                ++failcount
            }
            assertEquals(fails.size, failcount)
            assertEquals(oks.size, ret.size)
        }
    }

    @Test
    fun testArtifactVersion01() {
        subtest {
            val versions = arrayOf(
                "3.2.0.cr2",
                "3.2.0.ga",
                "3.2.1.ga",
                "3.3.1.ga",
                "3.3.2.GA",
                "3.4.0.GA",
                "3.5.0-Beta-2",
                "3.5.0-CR-1",
                "3.5.0-Final",
                "3.5.0-SP1",
                "3.5.0-SP2",
                "3.5.1-Final",
                "3.5.2-Final",
                "3.5.5-Final",
                "3.6.0.Final",
                "3.6.3.Final",
                "3.6.5.Final",
                "4.0.0.Beta1"
            )
            val set = TreeSet<ArtifactVersion>()
            for (version in versions) {
                val v = ArtifactVersion.parse(version)
                assertTrue(v.qualifier ?: "null", v.qualifier != null)
                set.add(v)
            }
            assertEquals(versions.size, set.size)
            val list = set.toList()
            log.d(list.map(this::tostring))
            for (i in 0 until versions.size) {
                assertEquals(versions[i], list[i].toString())
            }
            val hashset = HashSet<ArtifactVersion>(set)
            assertEquals(set.size, hashset.size)
        }
        subtest {
            val versions = arrayOf(
                "1-alpha-1",
                "1",
                "1.ga",
                "1.1-alpha-1",
                "1.1",
                "1.1.ga",
                "1.2.0",
                "1.2.1-alpha-1",
                "1.2.1-alpha-9",
                "1.2.1-alpha-10",
                "1.2.1-beta-2",
                "1.2.1",
                "1.2.1.ga",
                "1.2.1.3",
                "1.2.1.4.alpha",
                "1.2.1.4+1",
                "1.2.1.4",
                "1.2.1.4.ga",
                "1.2.1.4-sp1",
                "1.3.0",
                "1.4.0"
            )
            val set = TreeSet<ArtifactVersion>()
            for (v in versions) {
                val ver = ArtifactVersion.parse(v)
                set.add(ver)
            }
            debugPrint(set)
            assertEquals(21, set.size.toLong())
            assertEquals(1, set.last().majorVersion)
            assertEquals(4, set.last().minorVersion)
            assertEquals(0, set.last().incrementalVersion)
            assertEquals(0, set.last().buildNumber)
            assertEquals(null, set.last().qualifier)
            for (i in versions.indices) {
                assertEquals(versions[i], set.pollFirst()!!.toString())
            }
            assertTrue(ArtifactVersion.parse("1") == ArtifactVersion.parse("1.0"))
            assertFalse(ArtifactVersion.parse("1a") == ArtifactVersion.parse("1.0"))
        }
        subtest {
            val v1 = ArtifactVersion.parse("1.2.1-alpha-10")
            val v2 = ArtifactVersion.parse("1.2.1-alpha-9")
            assertEquals(1, v1.majorVersion)
            assertEquals(2, v1.minorVersion)
            assertEquals(1, v1.incrementalVersion)
            assertEquals("-alpha", v1.qualifier)
            assertEquals(10, v1.buildNumber)
            assertEquals(1, v1.compareTo(v2))
        }
    }

    @Test
    fun testArtifactVersionCompare01() {
        fun parse(version: String): ArtifactVersion {
            return ArtifactVersion.parse(version)
        }

        fun compare(v1: String, v2: String): Int {
            return parse(v1).compareTo(parse(v2))
        }

        subtest {
            assertEquals(-1, compare("1.0-alpha-9", "1.0.0-alpha-10"))
            assertEquals(-1, compare("1.0-alpha-9", "1.0.0-beta-1"))
        }
    }

    @Test
    fun testArtifactVersionCompare02() {
        subtest {
            val versions = arrayOf(
                "3.2.0.cr2",
                "3.2.0.ga",
                "3.2.1.ga",
                "3.3.1.ga",
                "3.3.2.GA",
                "3.4.0.GA",
                "3.5.0-Beta-2",
                "3.5.0-CR-1",
                "3.5.0-Final",
                "3.5.1-Final",
                "3.5.2-Final",
                "3.5.5-Final",
                "3.6.0.Final",
                "3.6.3.Final",
                "3.6.5.Final",
                "4.0.0.Beta1"
            )
            val ret = ArtifactVersion.sort(versions.asIterable())
            log.d(ret)
            assertEquals("3.2.0.cr2", ret[0])
            assertEquals("4.0.0.Beta1", ret[ret.size - 1])
        }
        subtest {
            val versions = arrayOf(
                "3.2.0.cr2",
                "3.2.0.ga",
                "3.2.1.ga",
                "3.3.1.ga",
                "3.3.2.GA",
                "3.4.0.GA",
                "3.5.0-Beta-2",
                "3.5.0-CR-1",
                "3.5.0-Final",
                "3.5.1-Final",
                "3.5.2-Final",
                "3.5.5-Final",
                "3.6.0.Final",
                "3.6.3.Final",
                "3.6.5.Final",
                "4.0.0.Beta1"
            )
            val map = TreeMap<ArtifactVersion, String>()
            for (version in versions) {
                map[ArtifactVersion.parse(
                    //                    hackVersion(
                    version
                    //                    )
                )
                ] = version
            }
            log.d("# Versions:")
            log.d(map.keys.map { tostring(it) })
            var release: String? = null
            val reverse = TreeMap<ArtifactVersion, String>(ArtifactVersion.ReversedComparator)
            reverse.putAll(map)
            for (e in reverse.entries) {
                val v = e.key
                if (isRelease(v)) {
                    release = e.value
                    break
                }
            }
            assertNotNull(release)
            log.d("# Release: $release")
            assertEquals("4.0.0.Beta1", map.lastEntry().value)
            assertEquals("3.2.0.cr2", map.firstEntry().value)
            assertEquals("3.6.5.Final", release)
        }
    }

    val releases = setOf("", "-final", "-ga", "final", "ga")
    val sp = Pattern.compile("-sp\\d+")

    private fun isRelease(v: ArtifactVersion): Boolean {
        val q = v.qualifier
        return q == null || releases.contains(q.toLowerCase()) || sp.matcher(q).matches()
    }

    @Test
    fun test01() {
        val versions = arrayOf("1.0-beta-3.0.1", "1.0.0", "1.0.1", "1.0.2-alpha-20")
        val incrementals = intArrayOf(0, 0, 1, 2)
        val set = TreeSet<ArtifactVersion>()
        for (ver in versions) {
            set.add(ArtifactVersion.parse(ver))
        }
        debugPrint(set)
        for (i in versions.indices) {
            val ver = set.pollFirst()
            assertEquals(versions[i], ver!!.toString())
            assertEquals(incrementals[i], ver.incrementalVersion)
        }
    }

    private fun debugPrint(versions: Collection<ArtifactVersion>) {
        if (log.debugging) {
            log.d(versions.map(this::tostring))
        }
    }

    private fun tostring(it: ArtifactVersion): String {
        return "[${it.majorVersion}, ${it.minorVersion}, ${it.incrementalVersion}, ${it.extraVersion}, ${it.qualifier}, ${it.buildNumber}]"
    }
}