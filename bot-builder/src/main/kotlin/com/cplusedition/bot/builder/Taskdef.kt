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
import com.cplusedition.bot.core.ChecksumUtil.ChecksumKind
import com.cplusedition.bot.core.FileUtil.Companion.FileUt
import com.cplusedition.bot.core.TextUtil.Companion.TextUt
import com.cplusedition.bot.core.WithUtil.Companion.With
import java.io.File
import java.nio.file.attribute.FileTime
import java.security.MessageDigest
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.collections.ArrayList

//////////////////////////////////////////////////////////////////////

/** Tasks that does not require a builder. */
interface ICoreTask<T> {
    var log: ICoreLogger
    fun run(): T
}

/** Tasks that works with a builder. */
interface IBuilderTask<T> : ICoreTask<T> {
    var builder: IBuilder
}

//////////////////////////////////////////////////////////////////////

/** Tasks that do not depend on builder. */
abstract class CoreTask<T>(log: ICoreLogger? = null) : ICoreTask<T> {

    private var _log: ICoreLogger? = null
    final override var log: ICoreLogger
        get() {
            return _log!!
        }
        set(value) {
            _log = value
        }

    protected var quiet = false

    init {
        if (log != null) {
            this.log = log
        }
    }

    fun setQuiet(b: Boolean): CoreTask<T> {
        quiet = b
        return this
    }
}

//////////////////////////////////////////////////////////////////////

abstract class BuilderTask<T>(builder: IBuilder? = null) : IBuilderTask<T> {

    private var _builder: IBuilder? = null
    private var _log: ICoreLogger? = null
    final override var builder: IBuilder
        get() = _builder!!
        set(value) {
            _builder = value
            log = builder.log
        }
    override var log: ICoreLogger
        get() = _log!!
        set(value) {
            _log = value
        }
    protected var quiet = false

    init {
        if (builder != null) {
            this.builder = builder
        }
    }

    fun setQuiet(b: Boolean): BuilderTask<T> {
        quiet = b
        return this
    }
}

//////////////////////////////////////////////////////////////////////

open class ResultPrinter(private val withempty: Boolean) {
    val ret = StringPrintWriter()
    fun print(detail: Boolean, msg: String, list: Collection<String>) {
        if (withempty || list.isNotEmpty()) {
            ret.println("$msg: ${list.size}")
            if (detail) {
                ret.println(list)
            }
        }
    }

    override fun toString(): String {
        return ret.buffer.trimEnd().toString()
    }
}

//////////////////////////////////////////////////////////////////////

open class Zip(
    private val zipfile: File,
    vararg filesets: IFileset
) : CoreTask<Zip.Result>() {

    private val SEP = '/'
    private var verbose = false
    private var preserveTimestamp = true
    private val filesets = ArrayList<Pair<String, IFileset>>()
    private lateinit var _result: Result
    val result: Result get() = _result

    companion object {
        private val emptyByteArray = byteArrayOf()
    }

    constructor(zipfile: File, srcdir: File, include: String? = null, exclude: String? = null) : this(
        zipfile, Fileset(srcdir, include, exclude)
    )

    init {
        add(*filesets)
    }

    fun add(vararg filesets: IFileset): Zip {
        for (fileset in filesets) {
            this.filesets.add(Pair("", fileset))
        }
        return this
    }

    fun add(dir: File, include: String? = null, exclude: String? = null): Zip {
        this.filesets.add(Pair("", Fileset(dir, include, exclude)))
        return this
    }

    /**
     * Prefix entries with the name of the basedir of the fileset.
     */
    fun withPrefix(vararg filesets: IFileset): Zip {
        for (fileset in filesets) {
            this.filesets.add(Pair(fileset.dir.name + SEP, fileset))
        }
        return this
    }

    fun withPrefix(prefix: String, vararg filesets: IFileset): Zip {
        for (fileset in filesets) {
            this.filesets.add(Pair(prefix, fileset))
        }
        return this
    }

    fun preserveTimestamp(b: Boolean): Zip {
        this.preserveTimestamp = b
        return this
    }

    fun setVerbose(b: Boolean): Zip {
        this.verbose = b
        return this
    }

    override fun run(): Zip.Result {
        _result = Result(zipfile)
        With.zipOutputStream(zipfile) { out ->
            for ((prefix, fileset) in filesets) {
                fileset.walk { file, rpath ->
                    try {
                        val path = prefix + rpath
                        if (verbose) log.d(path)
                        zipentry(out, file, path, preserveTimestamp)
                        _result.oks.add(path)
                    } catch (e: Exception) {
                        log.e("# ERROR: $rpath", e)
                        _result.fails.add(rpath)
                    }
                }
            }
        }
        if (_result.oks.size == 0) {
            log.e("# ERROR: Empty fileset, zip file not created")
            zipfile.delete()
        } else if (_result.fails.size > 0) {
            log.e("# ERROR: File read errors, zip file not created")
            log.d(_result.fails)
            zipfile.delete()
        } else if (!quiet) {
            log.d("# Zip ${zipfile.name}: ${_result.oks.size} files, ${TextUt.decUnit4String(zipfile)}")
        }
        return _result
    }

    private fun zipentry(out: ZipOutputStream, file: File, rpath: String, preservetimestamp: Boolean) {
        val isfile = file.isFile
        val name = rpath.replace(FileUt.SEPCHAR, SEP)
        val entry = ZipEntry(if (isfile) name else "$name$SEP")
        if (preservetimestamp) {
            val time = FileTime.fromMillis(file.lastModified())
            entry.creationTime = time
            entry.lastModifiedTime = time
        }
        out.putNextEntry(entry)
        if (isfile) {
            FileUt.copy(out, file)
        }
    }

    class Result(val zipfile: File) {
        val oks = ArrayList<String>()
        val fails = ArrayList<String>()

        override fun toString(): String {
            return toString(false, true)
        }

        fun toString(oks: Boolean = false, fails: Boolean = true): String {
            return with(ResultPrinter(true)) {
                ret.println("# ${zipfile.name}: ${TextUt.decUnit4String(zipfile)}")
                print(oks, "# OK", this@Result.oks)
                print(fails, "# Failed", this@Result.fails)
                toString()
            }
        }
    }
}

//////////////////////////////////////////////////////////////////////

open class Copy(
    private val dstdir: File,
    vararg filesets: IFileset
) : CoreTask<Copy.Result>() {

    private val filesets = ArrayList<IFileset>()
    private var preserveTimestamp = true
    private lateinit var _result: Result
    val result: Result get() = _result

    constructor(dstdir: File, srcdir: File, include: String? = null, exclude: String? = null) : this(
        dstdir, Fileset(srcdir, include, exclude)
    )

    init {
        if (!dstdir.isDirectory) BU.fail("# Expecting a directory: $dstdir")
        add(*filesets)
    }

    fun preserveTimestamp(b: Boolean): Copy {
        this.preserveTimestamp = b
        return this
    }

    fun add(vararg filesets: IFileset): Copy {
        this.filesets.addAll(filesets)
        return this
    }

    fun add(dir: File, include: String? = null, exclude: String? = null): Copy {
        this.filesets.add(Fileset(dir, include, exclude))
        return this
    }

    override fun run(): Copy.Result {
        _result = Result()
        for (fileset in filesets) {
            fileset.walk { file, rpath ->
                val dstfile = File(dstdir, rpath)
                if (file.isDirectory) {
                    dstfile.mkdirs()
                } else if (file.isFile) {
                    try {
                        FileUt.copy(dstfile, file)
                        if (preserveTimestamp) dstfile.setLastModified(file.lastModified())
                        _result.copied.add(rpath)
                    } catch (e: Exception) {
                        log.e("# ERROR: Copy failed: $rpath")
                        _result.copyFailed.add(rpath)
                    }
                }
            }
        }
        if (!quiet) {
            log.d("# Copy: ${_result.copied.size} OK, ${_result.copyFailed.size} failed")
        }
        return _result
    }

    class Result {
        val copied = ArrayList<String>()
        val copyFailed = ArrayList<String>()

        override fun toString(): String {
            return toString(false, true)
        }

        fun toString(copied: Boolean = false, failed: Boolean = true): String {
            return with(ResultPrinter(true)) {
                print(copied, "# Copied", this@Result.copied)
                print(failed, "# Copy failed", copyFailed)
                toString()
            }
        }
    }
}

//////////////////////////////////////////////////////////////////////

/**
 * Like Copy but only copy files that are dffer if destination exists.
 */
open class CopyDiff(
    private val dstdir: File,
    vararg filesets: IFileset
) : CoreTask<CopyDiff.Result>() {

    private val filesets = ArrayList<IFileset>()
    private var preserveTimestamp = true
    private lateinit var _result: Result
    val result: Result get() = _result

    constructor(dstdir: File, srcdir: File, include: String? = null, exclude: String? = null) : this(
        dstdir, Fileset(srcdir, include, exclude)
    )

    init {
        if (!dstdir.isDirectory) BuilderUtil.Companion.BU.fail("# Expecting a directory: $dstdir")
        add(*filesets)
    }

    fun preserveTimestamp(b: Boolean): CopyDiff {
        this.preserveTimestamp = b
        return this
    }

    fun add(vararg filesets: IFileset): CopyDiff {
        this.filesets.addAll(filesets)
        return this
    }

    fun add(dir: File, include: String? = null, exclude: String? = null): CopyDiff {
        this.filesets.add(Fileset(dir, include, exclude))
        return this
    }

    override fun run(): CopyDiff.Result {
        _result = Result()
        for (fileset in filesets) {
            fileset.walk { file, rpath ->
                val dstfile = File(dstdir, rpath)
                if (file.isDirectory) {
                    dstfile.mkdirs()
                } else if (file.isFile) {
                    try {
                        if (dstfile.exists() && !FileUt.diff(dstfile, file)) {
                            _result.notCopied.add(rpath)
                        } else {
                            FileUt.copy(dstfile, file)
                            if (preserveTimestamp) dstfile.setLastModified(file.lastModified())
                            _result.copied.add(rpath)
                        }
                    } catch (e: Exception) {
                        log.e("# ERROR: Copy failed: $rpath")
                        _result.copyFailed.add(rpath)
                    }
                }
            }
        }
        if (!quiet) {
            log.d("# Copy: ${_result.copied.size} OK, ${_result.notCopied.size} not copied, ${_result.copyFailed.size} failed")
        }
        return _result
    }

    class Result {
        val notCopied = ArrayList<String>()
        val copied = ArrayList<String>()
        val copyFailed = ArrayList<String>()

        override fun toString(): String {
            return toString(false, true, true)
        }

        fun toString(notcopied: Boolean = false, copied: Boolean = true, failed: Boolean = true): String {
            return with(ResultPrinter(true)) {
                print(notcopied, "# Not copied", notCopied)
                print(copied, "# Copied", this@Result.copied)
                print(failed, "# Copy failed", copyFailed)
                toString()
            }
        }
    }
}

//////////////////////////////////////////////////////////////////////

/**
 * Like CopyDiff but delete extra destination files that are not part of the source fileset.
 */
open class CopyMirror(
    private val dstdir: File,
    private val fileset: IFileset
) : CoreTask<CopyMirror.Result>() {

    private var preservePredicates = ArrayList<IFilePathPredicate>()
    private var preserveTimestamp = true
    private lateinit var _result: Result
    val result: Result get() = _result

    constructor(dstdir: File, srcdir: File, include: String? = null, exclude: String? = null) : this(
        dstdir,
        Fileset(srcdir, include, exclude)
    )

    init {
        if (!dstdir.isDirectory) BU.fail("# Expecting a directory: $dstdir")
    }

    fun preserveTimestamp(b: Boolean): CopyMirror {
        this.preserveTimestamp = b
        return this
    }

    fun preserve(predicate: IFilePathPredicate): CopyMirror {
        this.preservePredicates.add(predicate)
        return this
    }

    override fun run(): CopyMirror.Result {
        _result = Result()
        fileset.walk { file, rpath ->
            try {
                val dstfile = File(dstdir, rpath)
                if (file.isDirectory) {
                    dstfile.mkdirs()
                } else if (file.isFile) {
                    if (dstfile.exists() && !FileUt.diff(dstfile, file)) {
                        _result.notCopied.add(rpath)
                    } else {
                        FileUt.copy(dstfile, file)
                        if (preserveTimestamp) dstfile.setLastModified(file.lastModified())
                        _result.copied.add(rpath)
                    }
                }
            } catch (e: Exception) {
                log.e("# ERROR: Copy failed: $rpath")
                _result.copyFailed.add(rpath)
            }
        }
        val srcdir = fileset.dir
        dstdir.walker.bottomUp().walk { file, rpath ->
            if (srcdir.file(rpath).exists()) return@walk
            if (file.isDirectory) {
                _result.extraDirs.add(rpath)
                if (preservePredicates.any { it(file, rpath) }) return@walk
                if (file.listOrEmpty().isEmpty() && file.delete()) _result.extraDirsRemoved.add(rpath)
                else _result.extraDirsRemoveFailed.add(rpath)
            } else {
                _result.extraFiles.add(rpath)
                if (preservePredicates.any { it(file, rpath) }) return@walk
                if (file.delete()) _result.extraFilesRemoved.add(rpath) else _result.extraFilesRemoveFailed.add(rpath)
            }
        }
        if (!quiet) {
            with(_result) {
                log.d("# Copy: ${copied.size} OK, ${notCopied.size} not copied, ${copyFailed.size} failed")
                log.d(
                    "# Remove extras: ${extraFilesRemoved.size} files OK, ${extraDirsRemoved.size} dirs OK, " +
                            "${extraFilesRemoveFailed.size} files failed, ${extraDirsRemoveFailed.size} dirs failed"
                )
            }
        }
        with(_result.extraFilesRemoveFailed) {
            if (size > 0) {
                log.e("# ERROR: Removing $size extra files")
                log.d(this)
            }
        }
        with(_result.extraDirsRemoveFailed) {
            if (size > 0) {
                log.e("# ERROR: Removing $size extra dirs")
                log.d(this)
            }
        }
        return _result
    }

    class Result {
        val extraFiles = TreeSet<String>(reverseOrder())
        val extraDirs = TreeSet<String>(reverseOrder())
        val extraFilesRemoved = TreeSet<String>(reverseOrder())
        val extraDirsRemoved = TreeSet<String>(reverseOrder())
        val extraFilesRemoveFailed = TreeSet<String>(reverseOrder())
        val extraDirsRemoveFailed = TreeSet<String>(reverseOrder())
        val notCopied = ArrayList<String>()
        val copied = ArrayList<String>()
        val copyFailed = ArrayList<String>()

        override fun toString(): String {
            return toString(false)
        }

        /** Default prints everything except not copied. */
        fun toString(
            notcopied: Boolean = false,
            extras: Boolean = true,
            extrasremoved: Boolean = true,
            copied: Boolean = true,
            failed: Boolean = true,
            withempty: Boolean = true
        ): String {
            return tostring(
                notcopied,
                extras,
                extrasremoved,
                copied,
                failed,
                withempty
            )
        }

        /** Default prints nothing. */
        fun toString0(
            notcopied: Boolean = false,
            extras: Boolean = false,
            extrasremoved: Boolean = false,
            copied: Boolean = false,
            failed: Boolean = false,
            withempty: Boolean = true
        ): String {
            return tostring(
                notcopied,
                extras,
                extrasremoved,
                copied,
                failed,
                withempty
            )
        }

        private fun tostring(
            notcopied: Boolean,
            extras: Boolean,
            extrasremoved: Boolean,
            copied: Boolean,
            failed: Boolean,
            withempty: Boolean
        ): String {
            return with(ResultPrinter(withempty)) {
                print(notcopied, "# Not copied", notCopied)
                print(extras, "# Extra files", extraFiles)
                print(extras, "# Extra dirs", extraDirs)
                print(extrasremoved, "# Extra files removed", extraFilesRemoved)
                print(extrasremoved, "# Extra dirs removed", extraDirsRemoved)
                print(failed, "# Extra files remove failed", extraFilesRemoveFailed)
                print(failed, "# Extra dirs remove failed", extraDirsRemoveFailed)
                print(copied, "# Copied", this@Result.copied)
                print(failed, "# Copy failed", copyFailed)
                toString()
            }
        }
    }
}

//////////////////////////////////////////////////////////////////////

open class Remove(vararg filesets: IFileset) : CoreTask<Remove.Result>() {
    private val filesets = ArrayList<IFileset>()
    private lateinit var _result: Result
    val result: Result get() = _result

    constructor(dir: File, include: String? = null, exclude: String? = null) : this(Fileset(dir, include, exclude))

    init {
        this.filesets.addAll(filesets)
    }

    fun add(vararg filesets: IFileset): Remove {
        this.filesets.addAll(filesets)
        return this
    }

    fun add(dir: File, include: String? = null, exclude: String? = null): Remove {
        return add(Fileset(dir, include, exclude))
    }

    /**
     * @return Number of files successfullly removed.
     */
    override fun run(): Remove.Result {
        _result = Result()
        if (filesets.size == 1) {
            filesets.first().walk(bottomup = true) { file, rpath ->
                if (file.isFile) removefile(_result, file, rpath)
                else if (file.isDirectory) removedir(_result, file, rpath)
            }
        } else if (filesets.size > 1) {
            filesets.forEach {
                it.walk { file, rpath ->
                    if (file.isFile) removefile(_result, file, rpath)
                }
            }
            filesets.forEach {
                it.walk(bottomup = true) { file, rpath ->
                    if (file.isDirectory) removedir(_result, file, rpath)
                }
            }
        }
        with(_result) {
            if (filesFailed.size + dirsFailed.size > 0) {
                log.e(
                    "# ERROR: Remove: ${filesOK.size} files OK, ${dirsOK.size} dirs OK, " +
                            "${filesFailed.size} files failed, ${dirsFailed.size} dirs failed"
                )
            } else if (!quiet) {
                log.d("# Remove: ${filesOK.size} files OK, ${dirsOK.size} dirs OK")
            }
        }
        return _result
    }

    private fun removefile(result: Result, file: File, rpath: String) {
        ++result.total
        if (file.delete()) {
            result.filesOK.add(rpath)
        } else {
            result.filesFailed.add(rpath)
        }
    }

    private fun removedir(result: Result, file: File, rpath: String) {
        ++result.total
        if (file.listOrEmpty().isEmpty() && file.delete()) {
            result.dirsOK.add(rpath)
        } else {
            result.dirsFailed.add(rpath)
        }
    }

    class Result {
        val filesOK = ArrayList<String>()
        val dirsOK = ArrayList<String>()
        val filesFailed = ArrayList<String>()
        val dirsFailed = ArrayList<String>()
        var total = 0
        val okCount get() = filesOK.size + dirsOK.size
        val failedCount get() = filesFailed.size + dirsFailed.size

        override fun toString(): String {
            return toString(false, true)
        }

        fun toString(oks: Boolean = false, fails: Boolean = true): String {
            return with(ResultPrinter(true)) {
                print(oks, "# Remove file OK", filesOK)
                print(oks, "# Remove dir OK", dirsOK)
                print(fails, "# Remove file failed", filesFailed)
                print(fails, "# Remove dir failed", dirsFailed)
                toString()
            }
        }
    }
}

//////////////////////////////////////////////////////////////////////

open class Checksum(
    val sumfile: File,
    val kind: ChecksumKind,
    private val fileset: IFileset
) : CoreTask<Checksum.Result>() {

    private lateinit var _result: Result
    val result: Result get() = _result

    constructor(
        sumfile: File,
        kind: ChecksumKind,
        srcdir: File,
        include: String? = null,
        exclude: String? = null
    ) : this(
        sumfile, kind, Fileset(srcdir, include, exclude)
    )

    override fun run(): Checksum.Result {
        _result = Result(sumfile, kind)
        WithUtil.With.bufferedWriter(sumfile) { writer ->
            fileset.walk { file, rpath ->
                if (!file.isFile) { return@walk }
                try {
                    val digester = MessageDigest.getInstance(kind.algorithm)
                    WithUtil.With.bytes(file, 4 * 1024 * 1024) { buf, len ->
                        digester.update(buf, 0, len)
                    }
                    val digest = digester.digest()
                    val line = "${Hex.encode(digest)}  $rpath"
                    writer.appendln(line)
                    _result.oks.add(rpath)
                } catch (e: Exception) {
                    log.e("# ERROR: $rpath", e)
                    _result.fails.add(rpath)
                }
            }
        }
        if (_result.fails.size > 0) {
            log.e("# ERROR: $kind: ${_result.oks.size} OK, ${_result.fails.size} failed, checksum file not created")
            log.d(_result.fails)
            sumfile.delete()
        } else if (!quiet) {
            log.d("# $kind: ${_result.oks.size} OK")
        }
        return _result
    }

    companion object {
        fun single(kind: ChecksumKind, datafile: File): Checksum {
            val outfile = datafile.resolveSibling(datafile.name + ".${kind.name.toLowerCase()}")
            return Checksum(outfile, kind, Filepathset(datafile.parentFile, datafile.name))
        }
    }

    class Result(val sumfile: File, val kind: ChecksumKind) {
        var oks = ArrayList<String>()
        var fails = ArrayList<String>()
        override fun toString(): String {
            return "# $kind ${oks.size} OK" + (if (fails.size == 0) "" else ", ${fails.size} failed")
        }
    }
}

//////////////////////////////////////////////////////////////////////

open class VerifyChecksum(
    private val sumfile: File,
    private val kind: ChecksumKind,
    private val basedir: File? = null
) : CoreTask<VerifyChecksum.Result>() {

    private val BUFSIZE = 256 * 1024
    private var _allowNotExists = false
    private lateinit var _result: Result
    val result: Result get() = _result

    fun allowNotExists(): VerifyChecksum {
        this._allowNotExists = true
        return this
    }

    override fun run(): VerifyChecksum.Result {
        _result = Result(kind)
        val dir = basedir ?: sumfile.parentFile
        kind.readChecksums(sumfile) { sumandpath, line ->
            ++_result.total
            if (sumandpath == null) {
                log.e("# ERROR: Invalid checksum line: $line")
                _result.invalids.add(line)
                return@readChecksums
            }
            val rpath = sumandpath.path
            val file = dir.file(rpath)
            if (!file.exists()) {
                if (_allowNotExists) {
                    log.d("# IGNORE: data file not found: $rpath")
                } else {
                    log.e("# ERROR: Data file not found: $rpath")
                }
                _result.notexists.add(rpath)
                return@readChecksums
            }
            try {
                val sum = sumandpath.sum
                val expected = Hex.decode(sum)
                val digester = MessageDigest.getInstance(kind.algorithm)
                WithUtil.With.bytes(file, BUFSIZE) { buf, len ->
                    digester.update(buf, 0, len)
                }
                val actual = digester.digest()
                if (!Arrays.equals(expected, actual)) {
                    log.e("# ERROR: Checksum failed: $rpath")
                    log.d("# Expected: ${Hex.encode(expected)}")
                    log.d("# Actual  : ${Hex.encode(actual)}")
                    _result.fails.add(rpath)
                } else {
                    _result.oks.add(rpath)
                }
            } catch (e: Throwable) {
                log.e("# ERROR: $line", e)
                _result.fails.add(rpath)
            }
        }
        if (!quiet) {
            log.d(_result.toString())
        }
        return _result
    }

    class Result(private val kind: ChecksumKind) {
        val invalids = ArrayList<String>()
        val notexists = ArrayList<String>()
        val fails = ArrayList<String>()
        val oks = ArrayList<String>()
        var total = 0

        override fun toString(): String {
            return "# Verify $kind: $total total, ${oks.size} OK, ${notexists.size} not exists, ${invalids.size} invalid, ${fails.size} failed"
        }
    }
}

//////////////////////////////////////////////////////////////////////

