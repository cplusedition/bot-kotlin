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

import com.cplusedition.bot.core.FileUtil.Companion.FileUt
import com.cplusedition.bot.core.MatchUtil.Companion.MatchUt
import com.cplusedition.bot.core.WithUtil.Companion.With
import java.io.*
import java.nio.charset.Charset
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.attribute.FileTime
import java.nio.file.attribute.PosixFilePermission
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.coroutines.experimental.SequenceBuilder
import kotlin.coroutines.experimental.buildSequence

private val SEPCHAR = File.separatorChar // ie. / in unix.

////////////////////////////////////////////////////////////////////////
// Extensions

val File.suffix: String
    get() {
        return Basepath.suffix(this.name)
    }

val File.lcSuffix: String
    get() {
        return Basepath.lcSuffix(this.name)
    }

fun File.changeSuffix(newsuffix: String): File {
    return File(parentFile, "${Basepath.base(name)}$newsuffix")
}

fun File.changeBase(newbase: String): File {
    return File(parentFile, "$newbase${Basepath.suffix(name)}")
}

fun File.changeName(newname: String): File {
    return File(parentFile, newname)
}

/**
 * @return Name of file entries under this directory, or an empty array.
 */
fun File.listOrEmpty(): Array<String> {
    return list() ?: EMPTY.stringArray
}

/**
 * @return File with cleaned up path.
 */
fun File.clean(): File {
    return File(FileUt.cleanPath(absolutePath).toString())
}

/**
 * @return File with cleaned up path.
 */
fun File.clean(vararg segments: String): File {
    return File(FileUt.cleanPath(file(*segments).absolutePath).toString())
}

fun File.file(vararg segments: String): File {
    return if (segments.isEmpty()) this else File(this, segments.joinPath())
}

fun File.mkparentOrNull(): File? {
    val parent = absoluteFile.parentFile ?: return null
    return if (parent.mkdirsOrNull() != null) this else null
}

fun File.mkparentOrFail(): File {
    return mkparentOrNull() ?: error(absolutePath)
}

/**
 * @return Self if parent exists or created, otherwise null.
 */
fun File.mkparentOrNull(vararg segments: String): File? {
    val ret = file(*segments)
    val parent = ret.absoluteFile.parentFile ?: return null
    return if (parent.mkdirsOrNull() != null) ret else null
}

/**
 * @return Self it parent exists or created, otherwise throw IllegalStateException
 */
fun File.mkparentOrFail(vararg segments: String): File {
    return mkparentOrNull(*segments) ?: error(absolutePath)
}

/**
 * @return The specified directory if it exists or created, otherwise null.
 */
fun File.mkdirsOrNull(): File? {
    return if (exists() && isDirectory || !exists() && mkdirs()) this else null
}

/**
 * @return The specified directory if exists or created, othewise throw IllegalStateException
 */
fun File.mkdirsOrFail(): File {
    return mkdirsOrNull() ?: error(absolutePath)
}

/**
 * @return The specified directory if it exists or created, otherwise null.
 */
fun File.mkdirsOrNull(vararg segments: String): File? {
    return file(*segments).mkdirsOrNull()
}

/**
 * @return The specified directory if exists or created, othewise throw IllegalStateException
 */
fun File.mkdirsOrFail(vararg segments: String): File {
    val ret = file(*segments)
    return ret.mkdirsOrNull() ?: error(ret.absolutePath)
}

/**
 * @return File if exists, otherwise null
 */
fun File.existsOrNull(): File? {
    return if (exists()) this else null
}

/**
 * @return File if exists, otherwise throw IllegalStateException
 */
fun File.existsOrFail(): File {
    return if (exists()) this else error(absolutePath)
}

/**
 * @return File if exists, otherwise null
 */
fun File.existsOrNull(vararg segments: String): File? {
    val ret = this.file(*segments)
    return if (ret.exists()) ret else null
}

/**
 * @return File if exists, otherwise throw IllegalStateException
 */
fun File.existsOrFail(vararg segments: String): File {
    val ret = this.file(*segments)
    return if (ret.exists()) ret else error(ret.absolutePath)
}

/**
 * Delete all files and directories under this directory recursively, but not this directory.
 * @return false if any delete failed, otherwise true.
 */
fun File.deleteSubtrees(): Boolean {
    var ok = true
    for (name in listOrEmpty()) {
        if (!File(this, name).deleteRecursively()) ok = false
    }
    return ok
}

/**
 * @return A TreeWalker that can be used to walk the directory tree in various way.
 */
val File.walker: Treewalker
    get() = Treewalker(this)


////////////////////////////////////////////////////////////////////////

open class Basepath(
    var dir: String?,
    var name: String,
    var base: String,
    var suffix: String
) {
    companion object {

        fun from(file: File): Basepath {
            return from(file.absolutePath)
        }

        fun from(path: String): Basepath {
            // Remove trailing /, to be consistent with File(path).
            var end = path.length
            while (end > 0 && path[end - 1] == SEPCHAR) end -= 1
            var index = path.lastIndexOf(SEPCHAR, startIndex = end - 1)
            val dir: String?
            val name: String
            if (index < 0) {
                dir = null
                name = if (end == path.length) path else path.substring(0, end)
            } else {
                dir = path.substring(0, index)
                name = path.substring(index + 1, end)
            }
            index = name.lastIndexOf('.')
            return if (index < 0) Basepath(dir, name, name, "") else
                Basepath(dir, name, name.substring(0, index), name.substring(index))
        }

        fun dir(path: String): String? {
            var end = path.length
            while (end > 0 && path[end - 1] == SEPCHAR) end -= 1
            val index = path.lastIndexOf(SEPCHAR, startIndex = end - 1)
            return if (index < 0) null else path.substring(0, index)
        }

        fun name(path: String): String {
            var end = path.length
            while (end > 0 && path[end - 1] == SEPCHAR) end -= 1
            val index = path.lastIndexOf(SEPCHAR, startIndex = end - 1)
            return if (index < 0) {
                if (end == path.length) path else path.substring(0, end)
            } else path.substring(index + 1, end)
        }

        fun base(path: String): String {
            val name = name(path)
            val index = name.lastIndexOf('.')
            return if (index < 0) name else name.substring(0, index)
        }

        fun suffix(path: String): String {
            val name = name(path)
            val index = name.lastIndexOf('.')
            return if (index < 0) "" else name.substring(index)
        }

        fun lcSuffix(path: String): String {
            return suffix(path).toLowerCase()
        }

        fun ext(path: String): String? {
            val name = name(path)
            val index = name.lastIndexOf('.')
            return if (index < 0) null else name.substring(index + 1)
        }

        fun lcExt(path: String): String? {
            return ext(path)?.toLowerCase()
        }

        fun changeName(path: String, newname: String): String {
            return from(path).changeName(newname).toString()
        }

        fun changeBase(path: String, newbase: String): String {
            return from(path).changeBase(newbase).toString()
        }

        fun changeSuffix(path: String, newsuffix: String): String {
            return from(path).dirBase + newsuffix
        }
    }

    val lcSuffix: String get() = if (suffix.isEmpty()) suffix else suffix.toLowerCase()
    val ext: String? get() = if (suffix.isEmpty()) null else suffix.substring(1)
    val lcExt: String? get() = ext?.toLowerCase()
    val dirBase: String get() = if (dir == null) base else "$dir$SEPCHAR$base"
    val file get() = File(toString())

    fun changeName(newname: String): Basepath {
        return from(if (dir == null) newname else "$dir$SEPCHAR$newname")
    }

    fun changeBase(newbase: String): Basepath {
        return from(if (dir == null) "$newbase$suffix" else "$dir$SEPCHAR$newbase$suffix")
    }

    fun changeSuffix(newsuffix: String): Basepath {
        return from(if (dir == null) "$base$newsuffix" else "$dir$SEPCHAR$base$newsuffix")
    }

    override fun toString(): String {
        return dirBase + suffix
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Basepath) return false
        return dir == other.dir && name == other.name
    }

    override fun hashCode(): Int {
        var result = dir?.hashCode() ?: 0
        result = 31 * result + name.hashCode()
        result = 31 * result + base.hashCode()
        result = 31 * result + suffix.hashCode()
        return result
    }

}

////////////////////////////////////////////////////////////////////////

private object EMPTY {
    val stringArray = emptyArray<String>()
}

open class FileUtil {

    companion object {
        val FileUt = FileUtil()
    }

    private val BUFSIZE = 16 * 1024
    val SEP = File.separator
    val SEPCHAR = File.separatorChar // ie. / in unix.
    val ROOT = File("", "")
    val HOME = File(System.getProperty("user.home"))
    val PWD = File(System.getProperty("user.dir"))
    val everythingFilter: FileFilter = FileFilter { true }
    val fileFilter: FileFilter = FileFilter { it.isFile }
    val notFileFilter: FileFilter = FileFilter { !it.isFile }
    val dirFilter: FileFilter = FileFilter { it.isDirectory }
    val notDirFilter: FileFilter = FileFilter { !it.isDirectory }
    val everythingPredicate: IFilePathPredicate = { _, _ -> true }
    val filePredicate: IFilePathPredicate = { file, _ -> file.isFile }
    val notFilePredicate: IFilePathPredicate = { file, _ -> !file.isFile }
    val dirPredicate: IFilePathPredicate = { file, _ -> file.isDirectory }
    val notDirPredicate: IFilePathPredicate = { file, _ -> !file.isDirectory }

    val filesystem = FileSystems.getDefault()
    /** Permission for world read only directory: rwXr-Xr-X */
    val permissionsWorldReadonlyDir = setOf(
        PosixFilePermission.OWNER_READ,
        PosixFilePermission.OWNER_WRITE,
        PosixFilePermission.OWNER_EXECUTE,
        PosixFilePermission.GROUP_READ,
        PosixFilePermission.GROUP_EXECUTE,
        PosixFilePermission.OTHERS_READ,
        PosixFilePermission.OTHERS_EXECUTE
    )
    /** Permission for world read only file: rw-r--r-- */
    val permissionsWorldReadonlyFile = setOf(
        PosixFilePermission.OWNER_READ,
        PosixFilePermission.OWNER_WRITE,
        PosixFilePermission.GROUP_READ,
        PosixFilePermission.OTHERS_READ
    )
    /** Permission for world read only directory: rwX----- */
    val permissionsOwnerOnlyDir = setOf(
        PosixFilePermission.OWNER_READ,
        PosixFilePermission.OWNER_WRITE,
        PosixFilePermission.OWNER_EXECUTE
    )
    /** Permission for world read only file: rw------- */
    val permissionsOwnerOnlyFile = setOf(
        PosixFilePermission.OWNER_READ,
        PosixFilePermission.OWNER_WRITE
    )

    @Throws(IOException::class)
    fun setPermission(permissions: Set<PosixFilePermission>, file: File) {
        Files.setPosixFilePermissions(filesystem.getPath(file.absolutePath), permissions)
    }

    fun setPermission(permissions: Set<PosixFilePermission>, files: Iterator<File>) {
        files.forEach { setPermission(permissions, it) }
    }

    /**
     * Set file permission to rwx------ for directory, rw------- for file.
     *
     * @throws IOException If any operation failed.
     */
    @Throws(IOException::class)
    fun setOwnerOnly(vararg files: File) {
        for (file in files) {
            if (file.isDirectory) {
                setPermission(permissionsOwnerOnlyDir, file)
            } else if (file.isFile) {
                setPermission(permissionsOwnerOnlyFile, file)
            }
        }
    }

    /**
     * Set file attributes to rwXr-Xr-X.
     * @return The number of file entries updated.
     * @throws AssertionError If any operation failed.
     */
    fun setWorldReadonly(vararg files: File): Int {
        var count = 0
        for (file in files) {
            try {
                if (file.isDirectory) {
                    Files.setPosixFilePermissions(filesystem.getPath(file.absolutePath), permissionsWorldReadonlyDir)
                } else if (file.isFile) {
                    Files.setPosixFilePermissions(filesystem.getPath(file.absolutePath), permissionsWorldReadonlyFile)
                }
            } catch (e: Throwable) {
                continue
            }
            ++count
        }
        return count
    }

    fun home(vararg segments: String): File {
        return if (segments.isEmpty()) HOME else HOME.clean(*segments)
    }

    fun pwd(vararg segments: String): File {
        return if (segments.isEmpty()) PWD else PWD.clean(*segments)
    }

    fun root(vararg segments: String): File {
        return if (segments.isEmpty()) ROOT else ROOT.clean(*segments)
    }

    fun close(file: Closeable?) {
        val f = file ?: return
        try {
            f.close()
        } catch (e: Throwable) {
            // Ignore
        }
    }

    fun file(vararg segments: String): File {
        return File(segments.joinPath())
    }

    /**
     * @return The specified directory if it exists or created, otherwise null.
     */
    fun mkdirs(vararg segments: String): File? {
        val ret = file(*segments)
        return if (ret.exists() || ret.mkdirs()) ret else null
    }

    /**
     * @return The specified file if its parent exists or created, otherwise null.
     */
    fun mkparent(vararg segments: String): File? {
        return file(*segments).mkparentOrNull()
    }

    fun cleanFile(vararg segments: String): File {
        return File(cleanPath(file(*segments).absolutePath).toString())
    }

    fun cleanPath(path: String): StringBuilder {
        return cleanPath(StringBuilder(path))
    }

    /**
     * Remove duplicated /, /./ and /../
     */
    fun cleanPath(b: StringBuilder): StringBuilder {
        var c: Char
        var last = -1
        var len = 0
        val max = b.length
        var i = 0
        val sep = SEPCHAR
        while (i < max) {
            c = b[i]
            if ((last == sep.toInt() || len == 0) && c == '.' && (i + 1 < max && b[i + 1] == sep || i + 1 >= max)) {
                // s~^\./~~
                ++i
                ++i
                continue
            }
            if (last == sep.toInt() && c == sep) {
                // s~//~/~
                ++i
                continue
            }
            if (last == sep.toInt() && c == '.' && i + 1 < max && b[i + 1] == '.' && len >= 2 && (i + 2 >= max || b[i + 2] == sep)) {
                val index = b.lastIndexOf(SEPCHAR, len - 2)
                if ("../" != b.substring(index + 1, len)) {
                    len = index + 1
                    ++i
                    ++i
                    continue
                }
            }
            b.setCharAt(len++, c)
            last = c.toInt()
            ++i
        }
        b.setLength(len)
        return b
    }

    /**
     * Specialize cleanPath() for rpathOrNull.
     * Result may not be same as cleanPath(), but good for rpathOrNull.
     */
    protected /* for testing */ fun cleanPathSegments(b: CharSequence): List<String> {
        var c: Char
        val blen = b.length
        var i = 0
        val sep = SEPCHAR
        val ret = ArrayList<String>()
        val buf = StringBuilder()
        while (i < blen) {
            c = b[i]
            if (buf.isEmpty()) {
                if (c == sep) {
                    // s~//~/~
                    ++i
                    continue
                }
                if (c == '.') {
                    if (i + 1 < blen && b[i + 1] == sep || i + 1 >= blen) {
                        // s~^\./~~
                        ++i
                        ++i
                        continue
                    }
                    if (i + 1 < blen && b[i + 1] == '.' && (i + 2 >= blen || b[i + 2] == sep)) {
                        // /../
                        val retsize = ret.size
                        if (retsize > 0 && ".." != ret.last()) {
                            ret.removeLast()
                            ++i
                            ++i
                            continue
                        }
                    }
                }
            }
            if (c == sep) {
                ret.add(buf.toString())
                buf.setLength(0)
            } else {
                buf.append(c)
            }
            ++i
        }
        if (buf.isNotEmpty()) {
            ret.add(buf.toString())
        }
        return ret
    }

    /**
     * @return Relative path without leading / or null if file is not under base.
     */
    fun rpathOrNull(file: File, base: File): String? {
        val f = cleanPathSegments(file.absolutePath)
        val b = cleanPathSegments(base.absolutePath)
        if (f.contains("..") || b.contains("..")) return null
        val blen = b.size
        val flen = f.size
        var i = 0
        while (i < blen && i < flen) {
            if (b[i] != f[i]) {
                break
            }
            ++i
        }
        if (i < blen) return null
        return f.subList(i, flen).join(SEP)
    }

    @Throws(IOException::class)
    fun copy(dst: File, src: File) {
        With.inputStream(src) { input ->
            dst.mkparentOrNull() ?: throw IOException()
            With.outputStream(dst) { output ->
                copy(output, input)
            }
        }
    }

    fun copy(dst: File, src: InputStream) {
        dst.mkparentOrNull() ?: throw IOException()
        With.outputStream(dst) {
            copy(it, src)
        }
    }

    fun copy(dst: OutputStream, src: File) {
        With.inputStream(src) {
            copy(dst, it)
        }
    }

    @Throws(IOException::class)
    fun copy(output: OutputStream, input: InputStream) {
        copy(BUFSIZE, output, input)
    }

    @Throws(IOException::class)
    fun copy(bufsize: Int, output: OutputStream, input: InputStream) {
        val b = ByteArray(bufsize)
        while (true) {
            val n = input.read(b)
            if (n < 0) break
            output.write(b, 0, n)
        }
    }

    fun copyto(dstdir: File, vararg srcfiles: File): Int {
        return copyto(dstdir, srcfiles.iterator())
    }

    fun copyto(dstdir: File, srcfiles: Collection<File>): Int {
        return copyto(dstdir, srcfiles.iterator())
    }

    fun copyto(dstdir: File, srcfiles: Sequence<File>): Int {
        return copyto(dstdir, srcfiles.iterator())
    }

    fun copyto(dstdir: File, srcfiles: Iterator<File>): Int {
        if (!dstdir.isDirectory) error("# Expecting a directory: $dstdir")
        var count = 0
        for (srcfile in srcfiles) {
            copy(File(dstdir, srcfile.name), srcfile)
            ++count
        }
        return count
    }

    @Throws(IOException::class)
    fun asString(input: InputStream, charset: Charset = Charsets.UTF_8): String {
        return InputStreamReader(input, charset).readText()
    }

    @Throws(IOException::class)
    fun asStringList(input: InputStream, charset: Charset = Charsets.UTF_8): List<String> {
        return InputStreamReader(input, charset).readLines()
    }

    /**
     * Read input file as raw bytes.
     */
    @Throws(IOException::class)
    fun asBytes(input: InputStream): ByteArray {
        return input.readBytes()
    }

    /**
     * Delete the given files.
     * @throws AssertionError If failed to delete a file.
     */
    fun remove(files: Iterable<File>): Int {
        var count = 0
        files.forEach {
            if (it.exists() && it.delete()) ++count
        }
        return count
    }

    /**
     * Delete the given files.
     * @throws AssertionError If failed to delete a file.
     */
    fun remove(files: Sequence<File>): Int {
        var count = 0
        files.forEach {
            if (it.exists() && it.delete()) ++count
        }
        return count
    }

    /**
     * Zip only file in the given basedir with rpath matching the given
     * regular expressions. No directory entries are created.
     */
    @Throws(IOException::class)
    fun zip(
        zipfile: File,
        basedir: File,
        include: String,
        exclude: String? = null,
        preservetimestamp: Boolean = true
    ): Int {
        return zip(
            zipfile,
            basedir,
            Regex(include),
            if (exclude != null) Regex(exclude) else null,
            preservetimestamp
        )
    }

    /**
     * Zip only file in the given basedir with rpath matching the given
     * regular expressions. No directory entries are created.
     */
    @Throws(IOException::class)
    fun zip(
        zipfile: File,
        basedir: File,
        include: Regex,
        exclude: Regex? = null,
        preservetimestamp: Boolean = true
    ): Int {
        var count = 0
        zip(zipfile, basedir, preservetimestamp) { file, rpath ->
            if (file.isFile) {
                val accept = MatchUt.matches(rpath, include, exclude)
                if (accept) ++count
                accept
            } else true
        }
        return count
    }

    /**
     * Zip everything in the given basedir, including directory entries.
     */
    @Throws(IOException::class)
    fun zip(zipfile: File, basedir: File, preservetimestamp: Boolean = true): Int {
        return zip(zipfile, basedir, preservetimestamp) { _, _ -> true }
    }

    /**
     * Zip file/directory in the given basedir with FileUt.scan(basedir, accept).  If file is a directory
     * and accept returns false, then the directory and all its decendents are ignored.
     *
     * @param accept(file, rpath) Return true to include file/directory in the zip output.
     */
    @Throws(IOException::class)
    fun zip(zipfile: File, basedir: File, preservetimestamp: Boolean = true, accept: IFilePathPredicate): Int {
        var count = 0
        With.zipOutputStream(zipfile) { out ->
            FileUt.scan(basedir) { file, rpath ->
                val yes = accept(file, rpath)
                if (yes) {
                    zipentry(out, preservetimestamp, file, rpath)
                    ++count
                }
                yes
            }
        }
        return count
    }

    /**
     * Zip file/directory with the given rpaths in the given basedir if it exists.
     * Note that if File(basedir, rpath) is a directory, the directory is included
     * as a directory entry in the zip file, but not its descendents.
     */
    @Throws(IOException::class)
    fun zip(zipfile: File, basedir: File, rpaths: Sequence<String>, preservetimestamp: Boolean = true): Int {
        var count = 0
        With.zipOutputStream(zipfile) { out ->
            for (rpath in rpaths) {
                val file = File(basedir, rpath)
                if (file.exists()) {
                    zipentry(out, preservetimestamp, file, rpath)
                    ++count
                }
            }
        }
        return count
    }

    private fun zipentry(out: ZipOutputStream, preservetimestamp: Boolean, file: File, rpath: String) {
        val isfile = file.isFile
        val name = rpath.replace(FileUt.SEPCHAR, '/')
        val entry = ZipEntry(if (isfile) name else "$name/")
        if (preservetimestamp) {
            val time = FileTime.fromMillis(file.lastModified())
            entry.creationTime = time
            entry.lastModifiedTime = time
        }
        out.putNextEntry(entry)
        if (isfile) {
            copy(out, file)
        }
    }

    @Throws(IOException::class)
    fun unzip(
        outdir: File,
        zipfile: File,
        include: String,
        exclude: String? = null,
        preservetimestamp: Boolean = true
    ): Int {
        return unzip(
            outdir,
            zipfile,
            Regex(include),
            if (exclude != null) Regex(exclude) else null,
            preservetimestamp
        )
    }

    @Throws(IOException::class)
    fun unzip(
        outdir: File,
        zipfile: File,
        include: Regex,
        exclude: Regex? = null,
        preservetimestamp: Boolean = true
    ): Int {
        var count = 0
        unzip(outdir, zipfile, preservetimestamp) {
            val yes = MatchUt.matches(it.name, include, exclude)
            if (yes) ++count
            yes
        }
        return count
    }

    @Throws(IOException::class)
    fun unzip(
        outdir: File,
        zipfile: File,
        preservetimestamp: Boolean = true,
        accept: ((ZipEntry) -> Boolean)? = null
    ): Int {
        var count = 0
        With.zipInputStream(zipfile) { input, entry ->
            val yes = (accept == null || accept(entry))
            if (yes) {
                val file = File(outdir, entry.name)
                if (entry.isDirectory) {
                    file.mkdirsOrNull() ?: throw IOException(entry.name)
                } else {
                    try {
                        copy(file, input)
                    } finally {
                        input.closeEntry()
                    }
                }
                if (preservetimestamp) {
                    val timestamp = entry.lastModifiedTime ?: entry.creationTime
                    if (timestamp != null) {
                        file.setLastModified(timestamp.toMillis())
                    }
                }
                ++count
            }
        }
        return count
    }

    fun count(dir: File, predicate: (File) -> Boolean): Int {
        var count = 0
        dir.walker.walk { file, _ ->
            if (predicate(file)) ++count
        }
        return count
    }

    /**
     * Diff files, ignoring empty directories, in the given directories.
     */
    @Throws(IOException::class)
    fun diffDir(dir1: File, dir2: File): DiffStat<String> {
        val stat = DiffStat<String>()
        dir1.walker.files { file1, rpath ->
            val file2 = File(dir2, rpath)
            when {
                !file2.isFile -> stat.aonly.add(rpath)
                diff(file1, file2) -> stat.diffs.add(rpath)
                else -> stat.sames.add(rpath)
            }
        }
        dir2.walker.files { _, rpath ->
            if (!File(dir1, rpath).isFile) {
                stat.bonly.add(rpath)
            }
        }
        return stat
    }

    @Throws(IOException::class)
    fun diff(file1: File, file2: File): Boolean {
        var diff = false
        With.inputStream(file1) { input1 ->
            With.inputStream(file2) { input2 ->
                diff = diff(input1, input2)
            }
        }
        return diff
    }

    @Throws(IOException::class)
    fun diff(input1: InputStream, input2: InputStream): Boolean {
        val b1 = ByteArray(BUFSIZE)
        val b2 = ByteArray(BUFSIZE)
        while (true) {
            val n1 = input1.read(b1)
            val n2 = input2.read(b2)
            if (n1 != n2) {
                return true
            }
            if (n1 < 0) {
                return false
            }
            for (i in 0 until n1) {
                if (b1[i] != b2[i]) {
                    return true
                }
            }
        }
    }

    /**
     * Pre-order walk the given directory recursively.
     * This is similar to Treewalker.walk() but the predicate do the job of both the
     * ignoresDir and callback in Treewalker.walk().
     *
     * @param basepath The initial directory path, eg. "".
     * @param accept(file, rpath) If return false and file is a directory,
     * skip scanning descendants of the directory. Note that the basepath
     * is included in the rpath parameter.
     */
    @Throws(Exception::class)
    fun scan(dir: File, basepath: String = "", accept: IFilePathPredicate) {
        for (name in dir.listOrEmpty()) {
            val file = File(dir, name)
            val rpath = if (basepath.isEmpty()) name else "$basepath${com.cplusedition.bot.core.SEPCHAR}$name"
            if (accept(file, rpath) && file.isDirectory) {
                scan(file, rpath, accept)
            }
        }
    }
}

////////////////////////////////////////////////////////////////////

typealias  FilePathCollector<T> = (file: File, rpath: String) -> T

object FilePathCollectors {
    fun filePathCollector(file: File, rpath: String): Pair<File, String> {
        return Pair(file, rpath)
    }

    fun fileCollector(file: File, rpath: String): File {
        return file
    }

    fun pathCollector(file: File, rpath: String): String {
        return rpath
    }
}

interface IFilePathCollector<T> {
    fun collect(includes: IFilePathPredicate? = null): Sequence<T>

    fun files(includes: IFilePathPredicate? = null): Sequence<T> {
        return collect { file, rpath -> file.isFile && (includes == null || includes(file, rpath)) }
    }

    fun dirs(includes: IFilePathPredicate? = null): Sequence<T> {
        return collect { file, rpath -> file.isDirectory && (includes == null || includes(file, rpath)) }
    }
}

open class Treewalker(private val dir: File) {
    private var basepath = ""
    private var bottomUp = false
    private var ignoresDir: IFilePathPredicate? = null

    /**
     * Use the given path as path for the initial directory to create relative
     * paths for the callbacks. Default is "".
     */
    fun basepath(path: String): Treewalker {
        this.basepath = path
        return this
    }

    /**
     * Walk in bottom up order, ie. children before parent. Default is top down.
     */
    fun bottomUp(): Treewalker {
        this.bottomUp = true
        return this
    }

    /**
     * Do not recurse into directories that the given predicate returns true.
     * However, the directory itself would be visited.
     */
    fun ignoresDir(predicate: IFilePathPredicate): Treewalker {
        this.ignoresDir = predicate
        return this
    }

    /**
     * Walk the given directory recursively.
     * Invoke the given callback on each file/directory visited.
     *
     * @param callback(file, rpath)
     */
    fun walk(callback: IFilePathCallback) {
        U.walk1(dir, basepath, bottomUp, ignoresDir, callback)
    }

    /**
     * Shortcut for walk1() that invoke callback on directories only.
     */
    @Throws(Exception::class)
    fun dirs(callback: IFilePathCallback) {
        walk { file, rpath ->
            if (file.isDirectory) callback(file, rpath)
        }
    }

    /**
     * Shortcut for walk1() that invoke callback on files only.
     */
    @Throws(Exception::class)
    fun files(callback: IFilePathCallback) {
        walk { file, rpath ->
            if (file.isFile) callback(file, rpath)
        }
    }

    /**
     * Like walk1() but it stop searching and return the first file
     * with which the predicate returns true.
     */
    fun find(accept: IFilePathPredicate): File? {
        return U.find1(dir, basepath, bottomUp, ignoresDir, accept)
    }

    fun findOrFail(accept: IFilePathPredicate): File {
        return find(accept) ?: error(dir.absolutePath)
    }

    /**
     * For example, to collect Pair(rpath, filesize):
     *     collector { file, rpath -> Pair(rpath, file.length()) }.collect()
     *
     * @param collector A FilePathCollector that returns a T object.
     * @return Sequence<T> as returned by the collector.
     */
    fun <T> collector(collector: FilePathCollector<T>): TreewalkerCollector<T> {
        return TreewalkerCollector(this, collector)
    }

    /** A shorthand for collection(FilePathCollectors.filePathCollector) */
    fun collector(): TreewalkerCollector<Pair<File, String>> {
        return TreewalkerCollector(this, ::Pair)
    }

    /** A shorthand for collection(FliePathCollectors::pathCollector) */
    fun pathCollector(): TreewalkerCollector<String> {
        return TreewalkerCollector(this, FilePathCollectors::pathCollector)
    }

    /** A shorthand for collection(FliePathCollectors::fileCollector) */
    fun fileCollector(): TreewalkerCollector<File> {
        return TreewalkerCollector(this, FilePathCollectors::fileCollector)
    }

    /**
     * A shorthand for collector(:;Pair).collect(includes).
     *
     * @return Sequence<Pair<File, String>> File/directory where includes() return true.
     */
    fun collect(
        includes: IFilePathPredicate?
    ): Sequence<Pair<File, String>> {
        return collector(::Pair).collect(includes)
    }

    private object U {
        fun walk1(
            dir: File,
            rpath: String,
            bottomup: Boolean,
            ignoresdir: IFilePathPredicate?,
            callback: IFilePathCallback
        ) {
            for (name in dir.listOrEmpty()) {
                val file = File(dir, name)
                val filepath = if (rpath.isEmpty()) name else "$rpath$SEPCHAR$name"
                if (!bottomup) callback(file, filepath)
                if (file.isDirectory && (ignoresdir == null || !ignoresdir(file, filepath))) {
                    walk1(file, filepath, bottomup, ignoresdir, callback)
                }
                if (bottomup) callback(file, filepath)
            }
        }

        fun find1(
            dir: File,
            rpath: String,
            bottomup: Boolean,
            ignoresdir: IFilePathPredicate?,
            accept: IFilePathPredicate
        ): File? {
            for (name in dir.listOrEmpty()) {
                val file = File(dir, name)
                val filepath = if (rpath.isEmpty()) name else "$rpath$SEPCHAR$name"
                if (!bottomup && accept(file, filepath)) return file
                if (file.isDirectory && (ignoresdir == null || !ignoresdir(file, filepath))) {
                    val ret = find1(file, filepath, bottomup, ignoresdir, accept)
                    if (ret != null) return ret
                }
                if (bottomup && accept(file, filepath)) return file
            }
            return null
        }

        /**
         * Walk the directory recursively.
         *
         * @return Sequence<Pair<File, String>> File/directory where includes() return true.
         */
        fun <T> collect(
            dir: File,
            dirpath: String,
            bottomup: Boolean,
            ignoresdir: IFilePathPredicate?,
            collector: FilePathCollector<T>,
            includes: IFilePathPredicate?
        ): Sequence<T> {
            return buildSequence {
                collect1(dir, dirpath, bottomup, ignoresdir, collector, includes)
            }
        }

        private suspend fun <T> SequenceBuilder<T>.collect1(
            dir: File,
            dirpath: String,
            bottomup: Boolean,
            ignoresdir: IFilePathPredicate?,
            collector: FilePathCollector<T>,
            includes: IFilePathPredicate?
        ) {
            for (name in dir.listOrEmpty()) {
                val file = File(dir, name)
                val filepath = if (dirpath.isEmpty()) name else "$dirpath$SEPCHAR$name"
                if (!bottomup && (includes == null || includes(file, filepath))) {
                    yield(collector(file, filepath))
                }
                if (file.isDirectory && (ignoresdir == null || !ignoresdir(file, filepath))) {
                    collect1(file, filepath, bottomup, ignoresdir, collector, includes)
                }
                if (bottomup && (includes == null || includes(file, filepath))) {
                    yield(collector(file, filepath))
                }
            }
        }
    }

    open class TreewalkerCollector<T>(
        private val walker: Treewalker,
        private val collector: FilePathCollector<T>
    ) : IFilePathCollector<T> {
        override fun collect(includes: IFilePathPredicate?): Sequence<T> {
            return U.collect(walker.dir, walker.basepath, walker.bottomUp, walker.ignoresDir, collector, includes)
        }
    }
}

////////////////////////////////////////////////////////////////////

