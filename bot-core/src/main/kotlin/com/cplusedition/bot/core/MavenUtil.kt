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
import java.util.*

open class MavenUtil {

    companion object {
        val MavenUt = MavenUtil()
        private val SEPCHAR = File.separatorChar
    }

    open class GA(
        val groupId: String,
        val artifactId: String
    ) : Comparable<GA> {

        val path: String
            get() {
                return "${groupId.replace('.', SEPCHAR)}$SEPCHAR$artifactId"
            }

        override fun compareTo(other: GA): Int {
            var x = groupId.compareTo(other.groupId)
            if (x == 0) {
                x = artifactId.compareTo(other.artifactId)
            }
            return x
        }

        override fun equals(other: Any?): Boolean {
            return (other is GA) && (this === other || compareTo(other) == 0)
        }

        override fun hashCode(): Int {
            var result = groupId.hashCode()
            result = 31 * result + artifactId.hashCode()
            return result
        }

        override fun toString(): String {
            return "$groupId:$artifactId"
        }

        companion object {
            /**
             * Create GA from path in forms:
             * group/artifact
             */
            fun fromPath(rpath: String): GA? {
                val a = rpath.split(SEPCHAR)
                if (a.size < 2) return null
                if (a.any { it.isEmpty() }) return null
                val index = a.lastIndex
                val artifact = a[index]
                val group = a.subList(0, index).join(".")
                return GA(group, artifact)
            }

            /**
             * Create GA from gav in forms:
             * group:artifact(:version)?
             */
            fun fromGA(gav: String): GA? {
                val a = gav.split(':')
                if (a.size < 2) return null
                if (a[0].isEmpty() || a[1].isEmpty()) return null
                return GA(a[0].replace(SEPCHAR, '.'), a[1])
            }

            /**
             * Create GA from either path or ga forms.
             */
            fun from(s: String): GA? {
                if (!s.contains(':') && s.contains(SEPCHAR)) {
                    return fromPath(s)
                }
                return fromGA(s)
            }

            /**
             * Like from() but throw an exception on error instead of return null.
             */
            fun of(s: String): GA {
                if (!s.contains(':') && s.contains(SEPCHAR)) {
                    return fromPath(s)!!
                }
                return fromGA(s)!!
            }

            fun read(ret: MutableCollection<GA>, file: File, onerror: Fun10<String> = {}) {
                Without.comments(file) {
                    val ga = from(it)
                    if (ga == null) {
                        onerror(it)
                    } else {
                        ret.add(ga)
                    }
                }
            }

            /**
             * @return true if no error.
             */
            fun write(file: File, gas: Collection<GA>): Boolean {
                return With.exceptionOrNull {
                    With.printWriter(file) { writer ->
                        for (ga in gas) {
                            writer.println("$ga")
                        }
                    }
                } == null
            }
        }
    }

    open class GAV(
        val ga: GA,
        val version: ArtifactVersion
    ) : Comparable<GAV> {

        constructor(group: String, artifact: String, version: String) : this(
            GA(group, artifact),
            ArtifactVersion.parse(version)
        )

        val groupId = ga.groupId
        val artifactId = ga.artifactId
        /** @return GAV in form: groupId:artifactId:version. */
        val gav: String by lazy {
            "$groupId:$artifactId:$version"
        }
        /** @return GAV in path form: groupId/artifactId/version. */
        val path: String
            get() {
                return "${ga.path}$SEPCHAR$version"
            }
        /** @return Artifact path in form: groupId/artifactId/version/artifactId-version. */
        val artifactPath: String by lazy {
            "${ga.path}$SEPCHAR$version$SEPCHAR$artifactId-$version"
        }

        fun artifactPath(suffix: String): String {
            return "$artifactPath$suffix"
        }

        fun artifactPath(ret: MutableCollection<String>, suffix: String) {
            ret.add("$artifactPath$suffix")
        }

        override fun compareTo(other: GAV): Int {
            var x = ga.compareTo(other.ga)
            if (x == 0) {
                x = version.compareTo(other.version)
            }
            return x
        }

        override fun equals(other: Any?): Boolean {
            if (other !is GAV) {
                return false
            }
            return compareTo(other) == 0
        }

        override fun hashCode(): Int {
            var result = groupId.hashCode()
            result = 31 * result + artifactId.hashCode()
            result = 31 * result + version.hashCode()
            return result
        }

        override fun toString(): String {
            return gav
        }

        object ReversedVersionComparator : Comparator<GAV> {
            override fun compare(o1: GAV?, o2: GAV?): Int {
                return when {
                    o2 == null -> if (o1 == null) 0 else -1
                    o1 == null -> 1
                    else -> o2.version.compareTo(o1.version)
                }
            }
        }

        companion object {
            /**
             * Create GAV from path in forms:
             * group/artifact/version
             * group/artifact/version/xxx.pom
             */
            fun fromPath(rpath: String): GAV? {
                val a = rpath.split(SEPCHAR)
                if (a.size < 3) return null
                if (a.any { it.isEmpty() }) return null
                var index = a.lastIndex
                var version = a[index--]
                if (version.endsWith(".pom")) {
                    version = a[index--]
                }
                val artifact = a[index]
                val group = a.subList(0, index).join(".")
                return GAV(group, artifact, version)
            }

            /**
             * Create GAV from gav in forms:
             * group:artifact:version
             * group:artifact:version:packaging
             */
            fun fromGAV(gav: String): GAV? {
                val a = gav.split(':')
                when (a.size) {
                    in 3..4 -> {
                        if (a.any { it.isEmpty() }) return null
                        return GAV(a[0].replace(SEPCHAR, '.'), a[1], a[2])
                    }
                    else -> return null
                }
            }

            /**
             * Create GAV from either path or gav forms.
             */
            fun from(s: String): GAV? {
                if (!s.contains(':') && s.contains(SEPCHAR)) {
                    return fromPath(s)
                }
                return fromGAV(s)
            }

            /**
             * Like from() but return GAV or throws Exception instead of GAV?.
             */
            fun of(s: String): GAV {
                if (!s.contains(':') && s.contains(SEPCHAR)) {
                    return fromPath(s)!!
                }
                return fromGAV(s)!!
            }

            fun read(ret: MutableCollection<GAV>, file: File, onerror: Fun10<String> = {}) {
                Without.comments(file) {
                    val gav = from(it)
                    if (gav == null) {
                        onerror(it)
                    } else {
                        ret.add(gav)
                    }
                }
            }

            /**
             * @return true if no error.
             */
            fun write(file: File, gavs: Collection<GAV>): Boolean {
                return With.exceptionOrNull {
                    With.printWriter(file) { writer ->
                        for (gav in gavs) {
                            writer.println(gav.gav)
                        }
                    }
                } == null
            }
        }
    }

    /**
     * Parse maven version numbers.
     */
    open class ArtifactVersion constructor(
        val unparsed: String,
        val majorVersion: Int,
        val minorVersion: Int,
        val incrementalVersion: Int,
        val extraVersion: Int,
        val buildNumber: Int,
        val qualifier: String?
    ) : Comparable<ArtifactVersion> {

        override fun compareTo(other: ArtifactVersion): Int {
            fun weight(qualifier: String?): Int {
                var q = qualifier ?: return 0
                if (q.startsWith("-")) {
                    q = q.substring(1)
                }
                q = q.toLowerCase()
                if ("ga" == q || "final" == q || "fcs" == q) {
                    return 1
                }
                if (q.startsWith("sp") && q.length > 2) {
                    return Without.exceptionOrNull {
                        Integer.parseInt(q.substring(2)) + 1
                    } ?: -1
                }
                return -1
            }

            var result = majorVersion - other.majorVersion
            if (result == 0) {
                result = minorVersion - other.minorVersion
                if (result == 0) {
                    result = incrementalVersion - other.incrementalVersion
                    if (result == 0) {
                        result = extraVersion - other.extraVersion
                        if (result == 0) {
                            val otherQualifier = other.qualifier
                            val w = weight(qualifier)
                            val ow = weight(otherQualifier)
                            if (w != ow) {
                                return if (w > ow) 1 else -1
                            }
                            if (w != 0 && ow != 0) {
                                result = U.compareQualifier(qualifier, otherQualifier)
                            }
                            if (result == 0) {
                                result = buildNumber - other.buildNumber
                            }
                        }
                    }
                }
            }
            return result
        }

        override fun equals(other: Any?): Boolean {
            return other != null
                    && (other is ArtifactVersion)
                    && (this === other || compareTo(other) == 0)
        }

        override fun hashCode(): Int {
            return unparsed.hashCode()
        }

        override fun toString(): String {
            return unparsed
        }

        object ReversedComparator : Comparator<ArtifactVersion> {
            override fun compare(o1: ArtifactVersion?, o2: ArtifactVersion?): Int {
                return when {
                    o2 == null -> if (o1 == null) 0 else -1
                    o1 == null -> 1
                    else -> o2.compareTo(o1)
                }
            }
        }

        companion object {

            val EMPTY = ArtifactVersion("", 0, 0, 0, 0, 0, "")

            fun parse(version: String): ArtifactVersion {
                return try {
                    U.parse1(version)
                } catch (e: Exception) {
                    ArtifactVersion(version, 0, 0, 0, 0, 0, version)
                }
            }

            fun sort(versions: Iterable<String>): List<String> {
                val map = TreeMap<ArtifactVersion, String>()
                for (version in versions) {
                    Without.exceptionOrNull {
                        map.put(parse(version), version)
                    }
                }
                return ArrayList(map.values)
            }
        }

        private object K {
            const val ZERO = '0'.toInt()
            const val NINE = '9'.toInt()
            const val UA = 'A'.toInt()
            const val UZ = 'Z'.toInt()
            const val LA = 'a'.toInt()
            const val LZ = 'z'.toInt()
            const val TILE = '~'.toInt()
        }

        private object U {
            fun isdigit(c: Int): Boolean {
                return c in K.ZERO..K.NINE
            }

            internal fun compareQualifier(ver1: CharSequence?, ver2: CharSequence?): Int {
                if (ver1 == null) return if (ver2 == null) 0 else -1
                if (ver2 == null) return 1
                return compareQualifier1(ver1, ver2)
            }

            internal fun compareQualifier1(ver1: CharSequence, ver2: CharSequence): Int {
                fun weight(c: Int): Int {
                    return if (c == K.TILE) -2
                    else if (c == -1
                        || c in K.ZERO..K.NINE
                        || c in K.UA..K.UZ
                        || c in K.LA..K.LZ
                    )
                        c
                    else
                        c + 256
                }

                val v1 = StringScanner(ver1)
                val v2 = StringScanner(ver2)
                while (true) {
                    var c1 = v1.get()
                    var c2 = v2.get()
                    if (isdigit(c1) && isdigit(c2)) {
                        // Skip leading 0
                        while (c1 == K.ZERO) {
                            c1 = v1.get()
                        }
                        // Skip leading 0
                        while (c2 == K.ZERO) {
                            c2 = v2.get()
                        }
                        var r = 0
                        while (isdigit(c1) && isdigit(c2)) {
                            if (r == K.ZERO) {
                                r = c1 - c2
                            }
                            c1 = v1.get()
                            c2 = v2.get()
                        }
                        if (isdigit(c1)) {
                            // v1 is numeric and v2 is not, v1 is larger
                            return 1
                        }
                        if (isdigit(c2)) {
                            // v2 is numeric and v1 is not, v2 is larger
                            return -1
                        }
                        if (r != 0) {
                            return r
                        }
                    }
                    c1 = weight(c1)
                    c2 = weight(c2)
                    val r = c1 - c2
                    if (r != 0) {
                        return r
                    }
                    // If we reach end of either string, it is a tie.
                    if (c1 == -1 || c2 == -1) {
                        return 0
                    }
                }
            }

            internal fun parse1(version: String): ArtifactVersion {
                var majorVersion = 0
                var minorVersion = 0
                var incrementalVersion = 0
                var extraVersion = 0
                var buildNumber = 0
                var qualifier: String? = null
                val index = version.lastIndexOf('-') // Check for build number, eg. 1.0-aplha-9
                val part1: String
                var part2: String? = null
                if (index < 0) {
                    part1 = version
                } else {
                    part1 = version.substring(0, index)
                    part2 = version.substring(index + 1)
                }
                val dot = '.'.toInt()
                val s = VersionScanner(part1)
                while (true) {
                    var n = s.nextInt()
                    if (n < 0) {
                        break
                    }
                    majorVersion = n
                    if (s.remaining() == 0) {
                        break
                    }
                    if (s.get() != dot) {
                        s.unget()
                        break
                    }
                    n = s.nextInt()
                    if (n < 0) {
                        break
                    }
                    minorVersion = n
                    if (s.remaining() == 0) {
                        break
                    }
                    if (s.get() != dot) {
                        s.unget()
                        break
                    }
                    n = s.nextInt()
                    if (n < 0) {
                        break
                    }
                    incrementalVersion = n
                    if (s.remaining() == 0) {
                        break
                    }
                    if (s.get() != dot) {
                        s.unget()
                        break
                    }
                    n = s.nextInt()
                    if (n < 0) {
                        break
                    }
                    extraVersion = n
                    if (s.remaining() == 0) {
                        break
                    }
                    if (s.get() != dot) {
                        s.unget()
                    }
                    break
                }
                if (part2 != null) {
                    val ss = VersionScanner(part2)
                    val n = ss.nextInt()
                    if (ss.remaining() == 0) {
                        buildNumber = n
                        part2 = null
                    }
                }
                if (s.remaining() > 0 || part2 != null) {
                    qualifier = (if (s.remaining() > 0) s.remain() else "") + if (part2 == null) "" else "-$part2"
                }
                return ArtifactVersion(
                    version,
                    majorVersion,
                    minorVersion,
                    incrementalVersion,
                    extraVersion,
                    buildNumber,
                    qualifier
                )
            }

            private class StringScanner(private val source: CharSequence) {
                private val length = source.length
                private var index = 0

                /**
                 * @return The next char as integer value, -1 if end of string.
                 */
                fun get(): Int {
                    return if (index < length) source[index++].toInt() else -1
                }

                fun unget() {
                    if (index == 0) {
                        throw IndexOutOfBoundsException()
                    }
                    --index
                }

                fun index(): Int {
                    return index
                }

                fun remaining(): Int {
                    return length - index
                }
            }

            private class VersionScanner internal constructor(private val source: String) {
                private val scanner = StringScanner(source)
                fun nextInt(): Int {
                    var ret = -1
                    while (scanner.remaining() > 0) {
                        val c = scanner.get()
                        if (!isdigit(c)) {
                            scanner.unget()
                            break
                        }
                        ret = (if (ret > 0) ret * 10 else 0) + (c - K.ZERO)
                    }
                    return ret
                }

                fun get(): Int {
                    return scanner.get()
                }

                fun unget() {
                    scanner.unget()
                }

                fun remaining(): Int {
                    return scanner.remaining()
                }

                fun remain(): String {
                    return source.substring(scanner.index(), source.length)
                }
            }
        }
    }
}