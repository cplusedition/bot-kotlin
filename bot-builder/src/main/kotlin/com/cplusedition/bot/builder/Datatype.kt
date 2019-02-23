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
import com.cplusedition.bot.builder.apache.SelectorUtils
import com.cplusedition.bot.core.FilePathCollector
import com.cplusedition.bot.core.FileUtil.Companion.FileUt
import com.cplusedition.bot.core.IFilePathPredicate
import com.cplusedition.bot.core.MatchUtil.Companion.MatchUt
import com.cplusedition.bot.core.file
import com.cplusedition.bot.core.listOrEmpty
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.coroutines.experimental.SequenceBuilder
import kotlin.coroutines.experimental.buildSequence

//////////////////////////////////////////////////////////////////////

interface IFilesetCollector<T> {

    /**
     * @param bottomup true to return sequence in bottom up order, ie. children before parent directory.
     * @param include(file, rpath) true of include the file/directory in the output sequence.
     * Note that rpath includes the basepath.
     */
    fun collect(
        bottomup: Boolean = false,
        includes: IFilePathPredicate? = null
    ): Sequence<T>

    /**
     * Collect only files.
     */
    fun files(bottomup: Boolean = false, includes: IFilePathPredicate? = null): Sequence<T> {
        return collect(bottomup) { file, rpath ->
            file.isFile && (includes == null || includes(file, rpath))
        }
    }

    /**
     * Collect only directories.
     */
    fun dirs(bottomup: Boolean = false, includes: IFilePathPredicate? = null): Sequence<T> {
        return collect(bottomup) { file, rpath ->
            file.isDirectory && (includes == null || includes(file, rpath))
        }
    }
}

interface IFileset {

    val dir: File

    fun <T> collector(collector: FilePathCollector<T>): IFilesetCollector<T>

    /**
     * @param bottomup If true, return result in bottom up order, ie. children before parent. Default is false.
     * @param includes File, rpath filter.
     * @return The file and relative path of files or dirs in the fileset.
     */
    fun collect(
        bottomup: Boolean = false,
        includes: IFilePathPredicate? = null
    ): Sequence<Pair<File, String>>

    /**
     * Shortcut walk() to returns only files, not directories.
     */
    fun files(bottomup: Boolean = false): Sequence<Pair<File, String>> {
        return collect(bottomup, FileUt.filePredicate)
    }

    /**
     * Shortcut walk() to returns only directories.
     */
    fun dirs(bottomup: Boolean = false): Sequence<Pair<File, String>> {
        return collect(bottomup, FileUt.dirPredicate)
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
     * @param basepath The path for the basedir used to create the rpath parameter
     * of the inculdes predicate. Default is "".
     */
    private var basepath: String = ""

    /**
     * @param include Include regex.
     * @param exclude Optional exnclude regex. If not specified excludes nothing.
     */
    constructor(basedir: File, include: String? = null, exclude: String? = null) : this(basedir) {
        if (include != null) includes(include)
        if (exclude != null) excludes(exclude)
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

    override fun <T> collector(collector: FilePathCollector<T>): IFilesetCollector<T> {
        return FilesetCollector(this, collector)
    }

    override fun collect(
        bottomup: Boolean,
        includes: IFilePathPredicate?
    ): Sequence<Pair<File, String>> {
        return collector(::Pair).collect(bottomup, includes)
    }

    fun includes(vararg patterns: String): Fileset {
        return includes(SelectorFilter(patterns))
    }

    fun excludes(vararg pattern: String): Fileset {
        return excludes(SelectorFilter(pattern))
    }

    fun includes(vararg regexs: Regex): Fileset {
        return includes(RegexFilter(*regexs))
    }

    fun excludes(vararg regexs: Regex): Fileset {
        return excludes(RegexFilter(*regexs))
    }

    fun ignoresDir(vararg pattern: String): Fileset {
        return ignoresDir(SelectorFilter(pattern))
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

    open class RegexFilter(vararg regexs: Regex) : IFilePathPredicate {

        private val re: Array<out Regex> = regexs

        constructor(vararg regexs: String) : this(*MatchUt.compile(*regexs))

        override fun invoke(file: File, rpath: String): Boolean {
            return re.any { it.matches(rpath) }
        }
    }

    /** Path matching using ant path selectors. */
    open class SelectorFilter(val patterns: Array<out String>, val caseSensitive: Boolean = true) : IFilePathPredicate {

        constructor(vararg patterns: String) : this(patterns)

        override fun invoke(file: File, rpath: String): Boolean {
            return patterns.any { SelectorUtils.matchPath(it, rpath, caseSensitive) }
        }
    }

    open class FilesetCollector<T>(
        private val fileset: Fileset,
        private val collector: FilePathCollector<T>
    ) : IFilesetCollector<T> {

        override fun collect(
            bottomup: Boolean,
            includes: IFilePathPredicate?
        ): Sequence<T> {
            return buildSequence {
                collect1(
                    fileset.dir,
                    fileset.basepath,
                    bottomup,
                    collector,
                    fileset::acceptdir,
                    fileset::accept,
                    includes
                )
            }
        }

        private suspend fun <T> SequenceBuilder<T>.collect1(
            dir: File,
            dirpath: String,
            bottomup: Boolean,
            collector: FilePathCollector<T>,
            acceptdir: IFilePathPredicate,
            accept: IFilePathPredicate,
            predicate: IFilePathPredicate?
        ) {
            for (name in dir.listOrEmpty()) {
                val file = File(dir, name)
                val filepath = if (dirpath.isEmpty()) name else "$dirpath${File.separatorChar}$name"
                if (!bottomup && (predicate == null || predicate(file, filepath)) && accept(file, filepath)) {
                    yield(collector(file, filepath))
                }
                if (file.isDirectory && acceptdir(file, filepath)) {
                    collect1(file, filepath, bottomup, collector, acceptdir, accept, predicate)
                }
                if (bottomup && (predicate == null || predicate(file, filepath)) && accept(file, filepath)) {
                    yield(collector(file, filepath))
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

    override fun <T> collector(collector: FilePathCollector<T>): IFilesetCollector<T> {
        return Collector(this, collector)
    }

    override fun collect(
        bottomup: Boolean,
        includes: IFilePathPredicate?
    ): Sequence<Pair<File, String>> {
        return collector(::Pair).collect(bottomup, includes)
    }

    private class Collector<T>(
        private val filepathset: Filepathset,
        private val collector: FilePathCollector<T>
    ) : IFilesetCollector<T> {
        override fun collect(bottomup: Boolean, includes: IFilePathPredicate?): Sequence<T> {
            return collect1(
                filepathset.dir,
                "",
                (if (bottomup) filepathset.rpaths.reversed() else filepathset.rpaths),
                collector,
                includes
            )
        }

        private fun <T> collect1(
            dir: File,
            dirpath: String,
            rpaths: Collection<String>,
            collector: FilePathCollector<T>,
            predicate: IFilePathPredicate?
        ): Sequence<T> {
            return buildSequence {
                for (rpath in rpaths) {
                    val file = dir.file(if (dirpath.isEmpty()) rpath else "$dirpath${FileUt.SEP}$rpath")
                    if (file.exists() && (predicate == null || predicate(file, rpath))) {
                        yield(collector(file, rpath))
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
        mapping.putAll(src.collect().map { (srcfile, rpath) -> Pair(srcfile, File(dstdir, rpath)) })
        return this
    }

    fun add(src: IFileset, transform: (File, String) -> File): Filemap {
        mapping.putAll(src.collect().map { (srcfile, rpath) -> Pair(srcfile, transform(srcfile, rpath)) })
        return this
    }

    override fun modified(): Map<File, File> {
        return mapping.filter { (src, dst) -> src.exists() && (!dst.exists() || src.lastModified() > dst.lastModified()) }
    }

    override fun reversed(): Map<File, File> {
        val ret = TreeMap<File, File>()
        mapping.forEach { (k, v) -> ret[v] = k }
        return ret
    }
}

//////////////////////////////////////////////////////////////////////

