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

package com.cplusedition.bot.builder

import com.cplusedition.bot.builder.BuilderUtil.Companion.BU
import com.cplusedition.bot.core.*
import com.cplusedition.bot.core.WithUtil.Companion.With
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.charset.Charset
import java.nio.file.attribute.FileTime
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.collections.ArrayList

//////////////////////////////////////////////////////////////////////

interface IFileset {

    val dir: File

    //    fun <T> collector(collector: FilePathCollector<T>): IFilesetCollector<T>

    /**
     * Walk the fileset, calling callback on each accepted file.
     *
     * @param bottomup If true, return result in depth first bottom up order, ie. children before parent. Default is false.
     * @param callback(file, rpath).
     */
    fun walk(bottomup: Boolean = false, callback: IFilePathCallback)

    /**
     * @param bottomup If true, return result in depth first bottom up order, ie. children before parent. Default is false.
     * @param includes (File, String) -> T? Incude return value in the output sequence if it is not null.
     * @return The seqeuence of T object as return by the collecltor..
     */
    fun <T> collect(bottomup: Boolean, includes: IFilePathCollector<T>): Sequence<T>

    fun <T> collect(includes: IFilePathCollector<T>): Sequence<T> {
        return collect(false, includes)
    }

    fun pairOfAny(bottomup: Boolean = false): Sequence<Pair<File, String>> {
        return collect(bottomup, FilePathCollectors::pairOfAny)
    }

    fun pairOfFiles(bottomup: Boolean = false): Sequence<Pair<File, String>> {
        return collect(bottomup, FilePathCollectors::pairOfFiles)
    }

    fun pairOfDirs(bottomup: Boolean = false): Sequence<Pair<File, String>> {
        return collect(bottomup, FilePathCollectors::pairOfDirs)
    }

    fun fileOfAny(bottomup: Boolean = false): Sequence<File> {
        return collect(bottomup, FilePathCollectors::fileOfAny)
    }

    fun fileOfFiles(bottomup: Boolean = false): Sequence<File> {
        return collect(bottomup, FilePathCollectors::fileOfFiles)
    }

    fun fileOfDirs(bottomup: Boolean = false): Sequence<File> {
        return collect(bottomup, FilePathCollectors::fileOfDirs)
    }

    fun pathOfAny(bottomup: Boolean = false): Sequence<String> {
        return collect(bottomup, FilePathCollectors::pathOfAny)
    }

    fun pathOfFiles(bottomup: Boolean = false): Sequence<String> {
        return collect(bottomup, FilePathCollectors::pathOfFiles)
    }

    fun pathOfDirs(bottomup: Boolean = false): Sequence<String> {
        return collect(bottomup, FilePathCollectors::pathOfDirs)
    }
}

//////////////////////////////////////////////////////////////////////

interface IFilemap {
    /** src -> dst mapping. */
    val mapping: Map<File, File>

    /**
     * src -> dst mapping where src has a modified timestamp later than dst.
     */
    fun modified(): Map<File, File>

    /** @return dst -> src mappping. */
    fun reversed(): Map<File, File>
}

//////////////////////////////////////////////////////////////////////

open class Fileset(
        final override val dir: File
) : IFileset {

    private val includePredicates = ArrayList<IFilePathPredicate>()
    private val excludePredicates = ArrayList<IFilePathPredicate>()
    private val ignoreDirPredicates = ArrayList<IFilePathPredicate>()

    /**
     * The path for the basedir used to create the rpath parameter
     * of the inculdes predicate. Default is "".
     */
    private var basepath: String = ""

    /**
     * @param include Include Selector patterns separated by " ,". If not specified, include everything.
     * @param exclude Optional exclude selector patterns separated by " ,". If not specified excludes nothing.
     * To specify pattern that includes " " or ",", use includes(vararg patterns) and excludes(vararg patterns) instead.
     */
    constructor(basedir: File, include: String? = null, exclude: String? = null) : this(basedir) {
        if (include != null) includes(*SelectorFilter.split(include).toTypedArray())
        if (exclude != null) excludes(*SelectorFilter.split(exclude).toTypedArray())
    }

    constructor(basedir: File, include: Regex?, exclude: Regex? = null) : this(basedir) {
        if (include != null) includes(include)
        if (exclude != null) excludes(exclude)
    }

    init {
        if (!dir.isDirectory) BU.fail("# Expecting a directory: $dir")
    }

    private fun acceptdir(file: File, rpath: String): Boolean {
        return (ignoreDirPredicates.isEmpty() || ignoreDirPredicates.none { it(file, rpath) })
    }

    private fun accept(file: File, rpath: String): Boolean {
        return (includePredicates.isEmpty() || includePredicates.any { it(file, rpath) })
                && (excludePredicates.isEmpty() || excludePredicates.none { it(file, rpath) })
    }

    /**
     * @param patterns Ant style selector pattern. Each string should contains a single pattern,
     * mutilple patterns in single string is not supported.
     */
    fun includes(vararg patterns: String): Fileset {
        return includes(SelectorFilter(patterns))
    }

    /**
     * @param patterns Ant style selector pattern. Each string should contains a single pattern,
     * mutilple patterns in single string is not supported.
     */
    fun excludes(vararg patterns: String): Fileset {
        return excludes(SelectorFilter(patterns))
    }

    fun includes(vararg regexs: Regex): Fileset {
        return includes(RegexFilter(*regexs))
    }

    fun excludes(vararg regexs: Regex): Fileset {
        return excludes(RegexFilter(*regexs))
    }

    /**
     * @param patterns Ant style selector pattern. Each string should contains a single pattern,
     * mutilple patterns in single string is not supported.
     */
    fun ignoresDir(vararg patterns: String): Fileset {
        return ignoresDir(SelectorFilter(patterns))
    }

    fun includes(vararg predicates: IFilePathPredicate): Fileset {
        includePredicates.addAll(predicates)
        return this
    }

    fun excludes(vararg predicates: IFilePathPredicate): Fileset {
        excludePredicates.addAll(predicates)
        return this
    }

    fun basepath(path: String): Fileset {
        this.basepath = path
        return this
    }

    fun filesOnly(): Fileset {
        excludePredicates.add { file, _ -> !file.isFile }
        return this
    }

    fun dirsOnly(): Fileset {
        excludePredicates.add { file, _ -> !file.isDirectory }
        return this
    }

    fun ignoresDir(predicate: IFilePathPredicate): Fileset {
        ignoreDirPredicates.add(predicate)
        return this
    }

    override fun <T> collect(bottomup: Boolean, includes: IFilePathCollector<T>): Sequence<T> {
        return U.collect(this, bottomup, includes)
    }

    override fun walk(bottomup: Boolean, callback: IFilePathCallback) {
        return U.walk(this, bottomup, callback)
    }

    open class RegexFilter(vararg regexs: Regex) : IFilePathPredicate {

        private val re: Array<out Regex> = regexs

        constructor(vararg regexs: String) : this(*MatchUt.compile(*regexs))

        override fun invoke(file: File, rpath: String): Boolean {
            return re.any { it.matches(rpath) }
        }
    }

    /**
     * Path matching using ant style path selector pattern with "**", "*" and "?".
     * Under the hood, the pattern is transformed into Regex by replacing
     *     "**"/ as "(.*?/)?"
     *     "*" as "[^/]*"
     *     "?" as "."
     * There are some special treament of trailing / and ** to emulate behaviour
     * of Ant selector. However, it may behave !! differently !! from standard
     * Ant pattern matcher in some cases. In particular with trailing / and **.
     * Keep your fingers crossed.
     * @param patterns Array of selector pattern, eg. ".gitignore" , ".git/".
     * Multiple patterns should be specified as individual element instead of seperated by " ,".
     * Pattern should always be written with / as separator which would be replaced with
     * File.separatorChar in the Regex.
     * @param sep The file path separator, '/' or '\\', default is File.separatorChar.
     */
    open class SelectorFilter(
            patterns: Array<out String>,
            caseSensitive: Boolean = true,
            sep: Char = File.separatorChar
    ) : RegexFilter(*map(patterns, caseSensitive, sep)) {

        constructor(vararg patterns: String) : this(patterns, true)

        constructor(caseSensitive: Boolean, vararg patterns: String) : this(patterns, caseSensitive)

        companion object {
            private val patsep = Regex("\\s*[\\s,]\\s*")
            fun map(patterns: Array<out String>, caseSensitive: Boolean, sep: Char = File.separatorChar): Array<Regex> {
                val ss = if (sep == '\\') "\\\\" else "/"
                val starstarpat = Regex("\\*\\*/")
                val starstar = "(.*?$ss)?"
                val star = "[^$ss]*"
                return patterns.map {
                    var s = it
                    while (s.endsWith("/**/") || s.endsWith("/**")) {
                        s = if (s.endsWith("/**/")) s.dropLast(3)
                        else s.dropLast(2)
                    }
                    var pat = if (s.endsWith("**/")) s.dropLast(3)
                    else if (s.endsWith("**")) s.dropLast(2)
                    else if (s.endsWith("/")) s.dropLast(1)
                    else s
                    pat = pat.split(starstarpat).map {
                        it.split("*").map {
                            it.split("?").map {
                                if (it.isEmpty()) it else Regex.escape(it.replace('/', sep))
                            }.join(".")
                        }.join(star)
                    }.join(starstar)
                    if (s.endsWith("**/") || s == "**") {
                        pat += ".*"
                    } else if (s.endsWith("/")) {
                        pat += "(\\Q$sep\\E.*)?"
                    } else if (s.endsWith("**")) {
                        pat += "[^$ss]*"
                    }
                    pat = "^$pat\$"
                    if (caseSensitive) Regex(pat) else Regex(pat, RegexOption.IGNORE_CASE)
                }.toTypedArray()
            }

            fun split(patterns: String): List<String> {
                return patterns.split(patsep)
            }
        }
    }

    private object U {

        fun <T> collect(
                fileset: Fileset,
                bottomup: Boolean,
                includes: IFilePathCollector<T>
        ): Sequence<T> {
            return sequence {
                collect1(
                        fileset.dir,
                        fileset.basepath,
                        bottomup,
                        fileset::acceptdir,
                        fileset::accept,
                        includes
                )
            }
        }

        private suspend fun <T> SequenceScope<T>.collect1(
                dir: File,
                dirpath: String,
                bottomup: Boolean,
                acceptdir: IFilePathPredicate,
                accept: IFilePathPredicate,
                collector: IFilePathCollector<T>
        ) {
            for (name in dir.listOrEmpty()) {
                val file = File(dir, name)
                val filepath = if (dirpath.isEmpty()) name else "$dirpath${FileUt.SEPCHAR}$name"
                if (!bottomup && accept(file, filepath)) {
                    collector(file, filepath)?.let {
                        yield(it)
                    }
                }
                if (file.isDirectory && acceptdir(file, filepath)) {
                    collect1(file, filepath, bottomup, acceptdir, accept, collector)
                }
                if (bottomup && accept(file, filepath)) {
                    collector(file, filepath)?.let {
                        yield(it)
                    }
                }
            }
        }

        fun walk(fileset: Fileset, bottomup: Boolean, callback: IFilePathCallback) {
            walk1(fileset.dir, fileset.basepath, fileset::acceptdir, fileset::accept, bottomup, callback)
        }

        private fun walk1(
                dir: File,
                dirpath: String,
                acceptdir: IFilePathPredicate,
                accept: IFilePathPredicate,
                bottomup: Boolean,
                callback: IFilePathCallback
        ) {
            for (name in dir.listOrEmpty()) {
                val file = File(dir, name)
                val filepath = if (dirpath.isEmpty()) name else "$dirpath${FileUt.SEPCHAR}$name"
                if (!bottomup && accept(file, filepath)) {
                    callback(file, filepath)
                }
                if (file.isDirectory && acceptdir(file, filepath)) {
                    walk1(file, filepath, acceptdir, accept, bottomup, callback)
                }
                if (bottomup && accept(file, filepath)) {
                    callback(file, filepath)
                }
            }
        }
    }
}

//////////////////////////////////////////////////////////////////////

open class Filepathset(
        final override val dir: File
) : IFileset {

    private val rpaths = TreeSet<String>()

    /**
     * @param basedir
     * @param rpaths File paths relative to basedir.
     */
    constructor(basedir: File, vararg rpaths: String) : this(basedir) {
        this.rpaths.addAll(rpaths)
    }

    init {
        if (!dir.isDirectory) BU.fail("# Expecting a directory: $dir")
    }

    fun includes(vararg rpaths: String): Filepathset {
        this.rpaths.addAll(rpaths)
        return this
    }

    override fun <T> collect(bottomup: Boolean, includes: IFilePathCollector<T>): Sequence<T> {
        return U.collect(this, bottomup, includes)
    }

    override fun walk(bottomup: Boolean, callback: IFilePathCallback) {
        U.walk(this, bottomup, callback)
    }

    private object U {

        fun walk(
                filepathset: Filepathset,
                bottomup: Boolean,
                callback: IFilePathCallback
        ) {
            for (rpath in (if (bottomup) filepathset.rpaths.reversed() else filepathset.rpaths)) {
                val file = filepathset.dir.file(rpath)
                if (file.exists()) {
                    callback(file, rpath)
                }
            }
        }

        fun <T> collect(
                filepathset: Filepathset,
                bottomup: Boolean,
                includes: IFilePathCollector<T>
        ): Sequence<T> {
            return collect1(
                    filepathset.dir,
                    (if (bottomup) filepathset.rpaths.reversed() else filepathset.rpaths),
                    includes
            )
        }

        private fun <T> collect1(
                dir: File,
                rpaths: Collection<String>,
                collector: IFilePathCollector<T>
        ): Sequence<T> {
            return sequence {
                for (rpath in rpaths) {
                    val file = dir.file(rpath)
                    if (file.exists()) {
                        collector(file, rpath)?.let {
                            yield(it)
                        }
                    }
                }
            }
        }
    }
}

//////////////////////////////////////////////////////////////////////

open class Filemap : IFilemap {

    override val mapping = TreeMap<File, File>()

    fun add(src: File, dst: File): Filemap {
        mapping[src] = dst
        return this
    }

    fun add(src: IFileset, dstdir: File): Filemap {
        mapping.putAll(src.collect { file, rpath ->
            Pair(file, File(dstdir, rpath))
        })
        return this
    }

    fun add(src: IFileset, transform: (File, String) -> File): Filemap {
        mapping.putAll(src.collect { file, rpath ->
            Pair(file, transform(file, rpath))
        })
        return this
    }

    override fun modified(): Map<File, File> {
        return mapping.filter { (src, dst) ->
            src.exists() && (!dst.exists() || src.lastModified() > dst.lastModified())
        }
    }

    override fun reversed(): Map<File, File> {
        val ret = TreeMap<File, File>()
        mapping.forEach { (k, v) -> ret[v] = k }
        return ret
    }
}

//////////////////////////////////////////////////////////////////////

open class ZipBuilder(val out: OutputStream) : OutputStream() {
    private val zipout: ZipOutputStream = ZipOutputStream(out)
    private var entry: ZipEntry? = null

    constructor(file: File) : this(FileOutputStream(file))

    fun add(file: File, rpath: String) {
        putNewEntry(file, rpath)
        With.inputStream(file) { input ->
            FileUt.copy(zipout, input)
        }
        closeEntry()
    }

    fun add(data: ByteArray, rpath: String) {
        putNewEntry(rpath)
        write(data)
        closeEntry()
    }

    fun add(data: ByteArray, offset: Int, len: Int, rpath: String) {
        putNewEntry(rpath)
        write(data, offset, len)
        closeEntry()
    }

    fun add(content: String, rpath: String) {
        putNewEntry(rpath)
        write(content)
        closeEntry()
    }

    @Throws(IOException::class)
    fun putNewEntry(rpath: String): ZipEntry {
        if (entry != null) {
            zipout.closeEntry()
        }
        val e = ZipEntry(rpath)
        e.method = ZipEntry.DEFLATED
        entry = e
        zipout.putNextEntry(e)
        return e
    }

    @Throws(IOException::class)
    fun putNewEntry(file: File, rpath: String) {
        val e = putNewEntry(rpath)
        e.lastModifiedTime = FileTime.fromMillis(file.lastModified())
        e.size = file.length()
    }

    private fun closeEntry() {
        if (entry != null) {
            zipout.closeEntry()
            entry = null
        }
    }

    override fun write(b: Int) {
        zipout.write(b)
    }

    @Throws(IOException::class)
    override fun write(data: ByteArray, offset: Int, len: Int) {
        zipout.write(data, offset, len)
    }

    @Throws(IOException::class)
    override fun write(data: ByteArray) {
        zipout.write(data)
    }

    @Throws(IOException::class)
    fun write(data: String, charset: Charset = Charsets.UTF_8) {
        if (data.isEmpty()) return
        val buf = charset.encode(data)
        if (!buf.hasArray()) throw AssertionError()
        val array = buf.array()
        zipout.write(array, buf.arrayOffset(), buf.limit())
    }

    override fun close() {
        try {
            closeEntry()
        } finally {
            FileUt.closeOrFail(zipout)
            FileUt.closeOrFail(out)
        }
    }
}

//////////////////////////////////////////////////////////////////////
