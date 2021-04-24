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

import com.cplusedition.bot.core.ChecksumUtil.ChecksumKind
import com.cplusedition.bot.core.WithUtil.Companion.With
import java.io.*
import java.nio.charset.Charset
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.attribute.FileTime
import java.nio.file.attribute.PosixFilePermission
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.Comparator
import kotlin.collections.ArrayList

private val SEP = File.separatorChar // ie. '/' in unix.

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
    return File(parentFile, "${Basepath.nameWithoutSuffix(name)}$newsuffix")
}

fun File.changeNameWithoutSuffix(newbase: String): File {
    return File(parentFile, "$newbase${Basepath.suffix(name)}")
}

fun File.changeNameWithSuffix(newname: String): File {
    return File(parentFile, newname)
}

/**
 * @return Name of file entries under this directory, or an empty array.
 */
fun File.listOrEmpty(): Array<String> {
    return list() ?: EMPTY.stringArray
}

/**
 * @return File/directory entries under this directory, or an empty array.
 */
fun File.filesOrEmpty(): List<File> {
    return listOrEmpty().map { this.file(it) }
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
 * Delete all files and directories under this directory recursively, but not this directory.
 * @return this file if delete successfully, otherwise null
 */
fun File.deleteSubtreesOrNull(): File? {
    for (name in listOrEmpty()) {
        if (!File(this, name).deleteRecursively()) return null
    }
    return this
}

fun File.deleteTreeOrNull(): File? {
    if (!deleteRecursively()) return null
    return this
}

/**
 * Delete all files and directories under this directory recursively, but not this directory.
 * @return this file.
 * @throws If delete failed.
 */
fun File.deleteSubtreesOrFail(): File {
    for (name in listOrEmpty()) {
        if (!File(this, name).deleteRecursively()) error(absolutePath)
    }
    return this
}

fun File.deleteTreeOrFail(): File {
    if (!deleteRecursively()) error(absolutePath)
    return this
}

fun File.deleteOrFail(): File {
    if (exists() && !delete()) error(absolutePath)
    return this
}

val File.ut: FileUtExtension
    get() = FileUtExtension(this)

////////////////////////////////////////////////////////////////////////

open class Basepath(
        var dir: String?,
        var nameWithSuffix: String,
        var nameWithoutSuffix: String,
        var suffix: String
) {
    companion object {

        fun from(file: File): Basepath {
            return from(file.absolutePath)
        }

        fun from(dir: String?, nameWithSuffix: String): Basepath {
            val namesuffix = splitName((nameWithSuffix))
            return Basepath(dir, nameWithSuffix, namesuffix.first, namesuffix.second)
        }

        /// @param path A clean path.
        fun from(path: CharSequence): Basepath {
            // Remove trailing /, to be consistent with File(path).
            var end = path.length
            while (end > 0 && path[end - 1] == SEP) end -= 1
            var index = path.lastIndexOf(SEP, startIndex = end - 1)
            val dir: String?
            val name: String
            if (index < 0) {
                dir = null
                name = if (end == path.length) path.toString() else path.substring(0, end)
            } else {
                dir = path.substring(0, index)
                name = path.substring(index + 1, end)
            }
            index = name.lastIndexOf('.')
            return if (index <= 0) Basepath(dir, name, name, "") else
                Basepath(dir, name, name.substring(0, index), name.substring(index))
        }

        fun fromClean(dir: Basepath, rpath: String): Basepath {
            return fromClean(Basepath.joinPath(dir.toString(), rpath))
        }

        fun fromClean(path: String): Basepath {
            return from(FileUt.cleanPath(path))
        }

        fun clean(path: String): String {
            return FileUt.cleanPath(path).toString()
        }

        fun dir(path: String): String? {
            var end = path.length
            while (end > 0 && path[end - 1] == SEP) end -= 1
            val index = path.lastIndexOf(SEP, startIndex = end - 1)
            return if (index < 0) null else path.substring(0, index)
        }

        fun nameWithSuffix(path: String): String {
            var end = path.length
            while (end > 0 && path[end - 1] == SEP) end -= 1
            val index = path.lastIndexOf(SEP, startIndex = end - 1)
            return if (index < 0) {
                if (end == path.length) path else path.substring(0, end)
            } else path.substring(index + 1, end)
        }

        fun nameWithoutSuffix(path: String): String {
            val name = nameWithSuffix(path)
            val index = name.lastIndexOf('.')
            return if (index <= 0) name else name.substring(0, index)
        }

        fun suffix(path: String): String {
            val name = nameWithSuffix(path)
            val index = name.lastIndexOf('.')
            return if (index <= 0) "" else name.substring(index)
        }

        fun lcSuffix(path: String): String {
            return suffix(path).toLowerCase()
        }

        fun ext(path: String?): String? {
            if (path == null) return null
            val name = nameWithSuffix(path)
            val index = name.lastIndexOf('.')
            return if (index <= 0) null else name.substring(index + 1)
        }

        fun lcExt(path: String?): String? {
            return ext(path)?.toLowerCase()
        }

        fun changeNameWithSuffix(path: String, newname: String): String {
            return from(path).changeNameWithSuffix(newname).toString()
        }

        fun changeNameWithoutSuffix(path: String, newbase: String): String {
            return from(path).changeNameWithoutSuffix(newbase).toString()
        }

        fun changeSuffix(path: String, newsuffix: String): String {
            return from(path).dirAndNameWithoutSuffix + newsuffix
        }

        /// If dir is null return name. If dir empty return /name. If dir == "." return ./name
        fun joinPath(dir: String?, name: String): String {
            return if (dir == null) name
            else if (name.isEmpty()) dir
            else joinPath0(dir, name)
        }

        /// Like joinPath() but return a relative path if directory is null, empty or ".".
        fun joinRpath(dir: String?, name: String): String {
            return if (dir == null || dir.isEmpty() || dir == ".") name
            else if (name.isEmpty()) dir
            else joinPath0(dir, name)
        }

        /// Like joinPath() but return dir/ if name is empty.
        fun joinPath1(dir: String?, name: String): String {
            return if (dir == null) name
            else joinPath0(dir, name)
        }

        /// Like joinRpath() but return dir/ if name is empty.
        fun joinRpath1(dir: String?, name: String): String {
            return if (dir == null || dir.isEmpty() || dir == ".") name
            else joinPath0(dir, name)
        }

        fun joinPath0(dir: String, name: String): String {
            return if (dir.endsWith(File.separatorChar) || name.startsWith(File.separatorChar)) "$dir$name"
            else "$dir$SEP$name"
        }

        /** This differ from Basepath() in that it returns name as "" if input has trailing /. */
        fun splitPath1(path: String): Pair<String?, String> {
            val cleanpath = FileUt.cleanPath(path)
            val index = cleanpath.lastIndexOf(SEP)
            if (index < 0) return Pair(null, cleanpath.toString())
            return Pair(cleanpath.substring(0, index), cleanpath.substring(index + 1))
        }

        fun splitName(name: String): Pair<String, String> {
            val index = name.lastIndexOf('.')
            return if (index <= 0) Pair(name, "") else Pair(name.substring(0, index), name.substring(index + 1))
        }
    }

    val lcSuffix: String get() = if (suffix.isEmpty()) suffix else suffix.toLowerCase()
    val ext: String? get() = if (suffix.isEmpty()) null else suffix.substring(1)
    val lcExt: String? get() = ext?.toLowerCase()
    val dirAndNameWithoutSuffix: String get() = joinPath(dir, nameWithoutSuffix)
    val file get() = File(toString())

    fun changeNameWithSuffix(newname: String): Basepath {
        return from(joinPath(dir, newname))
    }

    fun changeNameWithoutSuffix(newbase: String): Basepath {
        return from(joinPath(dir, "$newbase$suffix"))
    }

    fun changeSuffix(newsuffix: String): Basepath {
        return from(joinPath(dir, "$nameWithoutSuffix$newsuffix"))
    }

    override fun toString(): String {
        return dirAndNameWithoutSuffix + suffix
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Basepath) return false
        return dir == other.dir && nameWithSuffix == other.nameWithSuffix
    }

    override fun hashCode(): Int {
        var result = dir?.hashCode() ?: 0
        result = 31 * result + nameWithSuffix.hashCode()
        result = 31 * result + nameWithoutSuffix.hashCode()
        result = 31 * result + suffix.hashCode()
        return result
    }

}

////////////////////////////////////////////////////////////////////////

private object EMPTY {
    val stringArray = emptyArray<String>()
    val fileArray = emptyArray<File>()
}

object FileUt : FileUtil()

open class FileUtil {

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

    var lastModifiedComparator = Comparator<File> { o1, o2 ->
        if (o1 == null) {
            if (o2 == null) 0 else -1
        } else if (o2 == null) {
            1
        } else {
            o1.lastModified().compareTo(o2.lastModified())
        }
    }

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
            setOwnerOnly1(file)
        }
    }

    fun setOwnerOnly1(file: File): File {
        if (file.isDirectory) {
            setPermission(permissionsOwnerOnlyDir, file)
        } else if (file.isFile) {
            setPermission(permissionsOwnerOnlyFile, file)
        }
        return file
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

    fun closeOrFail(file: Closeable?) {
        try {
            file?.close()
        } catch (e: Throwable) {
            throw AssertionError(e)
        }
    }

    fun closeAndIgnoreError(file: Closeable?) {
        try {
            file?.close()
        } catch (e: Throwable) {
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

    fun mkdirsOrNull(file: File): File? {
        return if (file.exists() || file.mkdirs()) file else null
    }

    fun mkdirsOrFail(file: File): File {
        return if (file.exists() || file.mkdirs()) file else throw AssertionError()
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
     * @return Relative path without leading / or null if file is not under basedir.
     */
    fun rpathOrNull(file: File, basedir: File): String? {
        val f = cleanPathSegments(file.absolutePath)
        val b = cleanPathSegments(basedir.absolutePath)
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

    /**
     * @return Relative path without leading /, allowing .. if file is not under basedir.
     */
    fun rpath(file: File, basedir: File): String? {
        return rpath(file.absolutePath, basedir.absolutePath)
    }

    fun rpath(file: String, basedir: String): String? {
        val f = cleanPathSegments(file)
        val b = cleanPathSegments(basedir)
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
        val ret = ArrayList(f.subList(i, flen))
        if (i == blen) {
            return ret.join(SEP)
        }
        while (++i <= blen) {
            ret.add(0, "..")
        }
        return ret.join(SEP)
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

    @Throws(IOException::class)
    fun copyByteWise(output: OutputStream, input: InputStream) {
        while (true) {
            val b = input.read()
            if (b < 0) break
            output.write(b)
        }
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

    @Throws(IOException::class)
    fun copydiff(dst: File, src: File): Boolean {
        if (!src.exists()) throw IOException()
        if (dst.exists() && !diff(dst, src)) return false
        With.inputStream(src) { input ->
            dst.mkparentOrNull() ?: throw IOException()
            With.outputStream(dst) { output ->
                copy(output, input)
            }
        }
        return true
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
        dir.ut.walk { file, _ ->
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
        dir1.ut.walk { file1, rpath ->
            if (!file1.isFile) return@walk
            val file2 = File(dir2, rpath)
            when {
                !file2.isFile -> stat.aonly.add(rpath)
                diff(file1, file2) -> stat.diffs.add(rpath)
                else -> stat.sames.add(rpath)
            }
        }
        dir2.ut.walk { file, rpath ->
            if (!file.isFile) return@walk
            if (!File(dir1, rpath).isFile) {
                stat.bonly.add(rpath)
            }
        }
        return stat
    }

    /**
     * Diff files exists in dir1, ignoring extras in dir2.
     */
    @Throws(IOException::class)
    fun diffDir1(dir1: File, dir2: File): DiffStat<String> {
        val stat = DiffStat<String>()
        dir1.ut.walk { file1, rpath ->
            if (!file1.isFile) return@walk
            val file2 = File(dir2, rpath)
            when {
                !file2.isFile -> stat.aonly.add(rpath)
                diff(file1, file2) -> stat.diffs.add(rpath)
                else -> stat.sames.add(rpath)
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
     * Walk the given directory recursively.
     * Invoke the given predicate on each file/directory visited.
     * Recurse into a directory only if predicate return true for the directory.
     * Note that bottomUp and ignoresDir has no effect. It always scan
     * preorder.
     *
     * @param predicate(File, String) -> Boolean
     */
    fun scan(dir: File, basepath: String = "", predicate: IFilePathPredicate) {
        U.scan1(dir, basepath, predicate)
    }

    /**
     * Walk the given directory recursively.
     * Invoke the given callback on each file/directory visited.
     *
     * @param callback(file, rpath)
     */
    fun walk(dir: File, basepath: String = "", bottomup: Boolean = false, ignoresdir: IFilePathPredicate? = null, callback: IFilePathCallback) {
        U.walk1(dir, basepath, bottomup, ignoresdir, callback)
    }

    fun <T> walk3(ret: T, dir: File, basepath: String = "", ignoresdir: IFilePathPredicate? = null, callback: Fun31<T, File, String, T>) {
        U.walk3(ret, dir, basepath, ignoresdir, callback)
    }

    /**
     * Like walk() but call callback only on files.
     */
    fun files(dir: File, basepath: String = "", bottomup: Boolean = false, ignoresdir: IFilePathPredicate? = null, callback: IFilePathCallback) {
        U.walk1(dir, basepath, bottomup, ignoresdir) { file, rpath ->
            if (file.isFile) callback(file, rpath)
        }
    }

    /**
     * Like walk() but call callback only on directories.
     */
    fun dirs(dir: File, basepath: String = "", bottomup: Boolean = false, ignoresdir: IFilePathPredicate? = null, callback: IFilePathCallback) {
        U.walk1(dir, basepath, bottomup, ignoresdir) { file, rpath ->
            if (file.isDirectory) callback(file, rpath)
        }
    }

    fun <T> collects(dir: File, basepath: String = "", bottomup: Boolean = false, ignoresdir: IFilePathPredicate? = null, includes: IFilePathCollector<T>): Sequence<T> {
        return U.collect1(dir, basepath, bottomup, ignoresdir, includes)
    }

    /**
     * Like walk1() but it stop searching and return the first file
     * with which the predicate returns true.
     */
    fun find(dir: File, basepath: String = "", bottomup: Boolean = false, ignoresdir: IFilePathPredicate? = null, accept: IFilePathPredicate): File? {
        return U.find1(dir, basepath, bottomup, ignoresdir, accept)
    }

    fun findOrFail(dir: File, basepath: String = "", bottomup: Boolean = false, ignoresdir: IFilePathPredicate? = null, accept: IFilePathPredicate): File {
        return find(dir, basepath, bottomup, ignoresdir, accept) ?: error(dir.absolutePath)
    }

    @Throws(IOException::class)
    fun findDupBySHA1(files: Sequence<File>): MutableMap<String, NavigableSet<File>> {
        val ret = TreeMap<String, NavigableSet<File>>()
        for (file in files) {
            val digest = ChecksumKind.SHA1.digest(file)
            var a: NavigableSet<File>? = ret[digest]
            if (a == null) {
                a = TreeSet(lastModifiedComparator)
                ret[digest] = a
            }
            a.add(file)
        }
        return ret
    }

    fun findDup(dir: File): Map<File, NavigableSet<File>> {
        val files = dir.ut.collects(FilePathCollectors::fileOfAny)
        val bymd5 = findDupBySHA1(files)
        val ret = TreeMap<File, NavigableSet<File>>()
        for ((_, list) in bymd5) {
            while (true) {
                val file = list.pollFirst() ?: break
                val dups = TreeSet(lastModifiedComparator)
                for (other in list) {
                    if (!FileUt.diff(file, other)) {
                        dups.add(other)
                    }
                }
                for (dup in dups) list.remove(dup)
                ret[file] = dups
            }
        }
        return ret
    }
}

////////////////////////////////////////////////////////////////////

object FilePathCollectors {
    fun pairOfAny(file: File, rpath: String): Pair<File, String> {
        return Pair(file, rpath)
    }

    fun pairOfFiles(file: File, rpath: String): Pair<File, String>? {
        return if (file.isFile) Pair(file, rpath) else null
    }

    fun pairOfDirs(file: File, rpath: String): Pair<File, String>? {
        return if (file.isDirectory) Pair(file, rpath) else null
    }

    fun fileOfAny(file: File, @Suppress("UNUSED_PARAMETER") rpath: String): File {
        return file
    }

    fun fileOfFiles(file: File, @Suppress("UNUSED_PARAMETER") rpath: String): File? {
        return if (file.isFile) file else null
    }

    fun fileOfDirs(file: File, @Suppress("UNUSED_PARAMETER") rpath: String): File? {
        return if (file.isDirectory) file else null
    }

    fun pathOfAny(@Suppress("UNUSED_PARAMETER") file: File, rpath: String): String {
        return rpath
    }

    fun pathOfFiles(file: File, rpath: String): String? {
        return if (file.isFile) rpath else null
    }

    fun pathOfDirs(file: File, rpath: String): String? {
        return if (file.isDirectory) rpath else null
    }
}

//////////////////////////////////////////////////////////////////////

open class FileUtExtension(private val receiver: File) {

    fun scan(basepath: String = "", predicate: IFilePathPredicate) {
        FileUt.scan(receiver, basepath, predicate)
    }

    fun walk(basepath: String = "", bottomup: Boolean = false, ignoresdir: IFilePathPredicate? = null, callback: IFilePathCallback) {
        FileUt.walk(receiver, basepath, bottomup, ignoresdir, callback)
    }

    fun <T> walk3(ret: T, basepath: String = "", ignoresdir: IFilePathPredicate? = null, callback: Fun31<T, File, String, T>) {
        FileUt.walk3(ret, receiver, basepath, ignoresdir, callback)
    }

    fun files(basepath: String = "", bottomup: Boolean = false, ignoresdir: IFilePathPredicate? = null, callback: IFilePathCallback) {
        FileUt.files(receiver, basepath, bottomup, ignoresdir, callback)
    }

    fun dirs(basepath: String = "", bottomup: Boolean = false, ignoresdir: IFilePathPredicate? = null, callback: IFilePathCallback) {
        FileUt.dirs(receiver, basepath, bottomup, ignoresdir, callback)
    }

    fun <T> collects(basepath: String = "", bottomup: Boolean = false, ignoresdir: IFilePathPredicate? = null, collector: IFilePathCollector<T>): Sequence<T> {
        return FileUt.collects(receiver, basepath, bottomup, ignoresdir, collector)
    }

    fun <T> collects(basepath: String = "", collector: IFilePathCollector<T>): Sequence<T> {
        return FileUt.collects(receiver, basepath, false, null, collector)
    }

    fun <T> collects(collector: IFilePathCollector<T>): Sequence<T> {
        return FileUt.collects(receiver, "", false, null, collector)
    }

    fun collects(basepath: String = "", bottomup: Boolean = false, ignoresdir: IFilePathPredicate? = null): Sequence<Pair<File, String>> {
        return FileUt.collects(receiver, basepath, bottomup, ignoresdir, FilePathCollectors::pairOfAny)
    }

    fun find(basepath: String = "", bottomup: Boolean = false, ignoresdir: IFilePathPredicate? = null, accept: IFilePathPredicate): File? {
        return FileUt.find(receiver, basepath, bottomup, ignoresdir, accept)
    }

    fun findOrFail(basepath: String = "", bottomup: Boolean = false, ignoresdir: IFilePathPredicate? = null, accept: IFilePathPredicate): File {
        return FileUt.findOrFail(receiver, basepath, bottomup, ignoresdir, accept)
    }

    fun findDup(): Map<File, NavigableSet<File>> {
        return FileUt.findDup(receiver)
    }

    fun findDupBySHA1(): MutableMap<String, NavigableSet<File>> {
        return FileUt.findDupBySHA1(FileUt.collects(receiver, includes = FilePathCollectors::fileOfAny))
    }

    fun copyas(dst: File) {
        FileUt.copy(dst, receiver)
    }

    fun copyto(dstdir: File) {
        FileUt.copyto(dstdir, receiver)
    }

    fun diff(other: File): Boolean {
        return FileUt.diff(receiver, other)
    }

    fun diffDir(otherdir: File): DiffStat<String> {
        return FileUt.diffDir(receiver, otherdir)
    }

    fun rpath(basedir: File): String? {
        return FileUt.rpath(receiver, basedir)
    }
}

private object U {
    fun scan1(
            dir: File,
            rpath: String,
            predicate: IFilePathPredicate
    ) {
        for (name in dir.listOrEmpty()) {
            val file = File(dir, name)
            val filepath = if (rpath.isEmpty()) name else "$rpath$SEP$name"
            if (predicate(file, filepath) && file.isDirectory) {
                scan1(file, filepath, predicate)
            }
        }
    }

    fun walk1(
            dir: File,
            rpath: String,
            bottomup: Boolean,
            ignoresdir: IFilePathPredicate?,
            callback: IFilePathCallback
    ) {
        for (name in dir.listOrEmpty()) {
            val file = File(dir, name)
            val filepath = if (rpath.isEmpty()) name else "$rpath$SEP$name"
            if (!bottomup) callback(file, filepath)
            if (file.isDirectory && (ignoresdir == null || !ignoresdir(file, filepath))) {
                walk1(file, filepath, bottomup, ignoresdir, callback)
            }
            if (bottomup) callback(file, filepath)
        }
    }

    fun <T> walk3(
            ret: T,
            dir: File,
            rpath: String,
            ignoresdir: IFilePathPredicate?,
            callback: Fun31<T, File, String, T>
    ) {
        for (name in dir.listOrEmpty()) {
            var ret1 = ret
            val file = File(dir, name)
            val filepath = if (rpath.isEmpty()) name else "$rpath$SEP$name"
            ret1 = callback(ret1, file, filepath)
            if (file.isDirectory && (ignoresdir == null || !ignoresdir(file, filepath))) {
                walk3(ret1, file, filepath, ignoresdir, callback)
            }
        }
    }

    /**
     * Walk the directory recursively.
     *
     * @return Sequence<Pair<File, String>> File/directory where includes() return true.
     */
    fun <T> collect1(
            dir: File,
            dirpath: String,
            bottomup: Boolean,
            ignoresdir: IFilePathPredicate?,
            collector: IFilePathCollector<T>
    ): Sequence<T> {
        return sequence {
            collect2(dir, dirpath, bottomup, ignoresdir, collector)
        }
    }

    private suspend fun <T> SequenceScope<T>.collect2(
            dir: File,
            dirpath: String,
            bottomup: Boolean,
            ignoresdir: IFilePathPredicate?,
            collector: IFilePathCollector<T>
    ) {
        for (name in dir.listOrEmpty()) {
            val file = File(dir, name)
            val filepath = if (dirpath.isEmpty()) name else "$dirpath$SEP$name"
            if (!bottomup) {
                collector(file, filepath)?.let {
                    yield(it)
                }
            }
            if (file.isDirectory && (ignoresdir == null || !ignoresdir(file, filepath))) {
                collect2(file, filepath, bottomup, ignoresdir, collector)
            }
            if (bottomup) {
                collector(file, filepath)?.let {
                    yield(it)
                }
            }
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
            val filepath = if (rpath.isEmpty()) name else "$rpath$SEP$name"
            if (!bottomup && accept(file, filepath)) return file
            if (file.isDirectory && (ignoresdir == null || !ignoresdir(file, filepath))) {
                val ret = find1(file, filepath, bottomup, ignoresdir, accept)
                if (ret != null) return ret
            }
            if (bottomup && accept(file, filepath)) return file
        }
        return null
    }

    //#BEGIN OBSOLETED
    //    /**
    //     * Walk the directory recursively.
    //     *
    //     * @return Sequence<Pair<File, String>> File/directory where includes() return true.
    //     */
    //    fun <T> collect(
    //            dir: File,
    //            dirpath: String,
    //            bottomup: Boolean,
    //            ignoresdir: IFilePathPredicate?,
    //            collector: FilePathCollector<T>,
    //            includes: IFilePathPredicate?
    //    ): Sequence<T> {
    //        return sequence {
    //            collect1(dir, dirpath, bottomup, ignoresdir, collector, includes)
    //        }
    //    }
    //
    //    private suspend fun <T> SequenceScope<T>.collect1(
    //            dir: File,
    //            dirpath: String,
    //            bottomup: Boolean,
    //            ignoresdir: IFilePathPredicate?,
    //            collector: FilePathCollector<T>,
    //            includes: IFilePathPredicate?
    //    ) {
    //        for (name in dir.listOrEmpty()) {
    //            val file = File(dir, name)
    //            val filepath = if (dirpath.isEmpty()) name else "$dirpath$SEPCHAR$name"
    //            if (!bottomup && (includes == null || includes(file, filepath))) {
    //                yield(collector(file, filepath))
    //            }
    //            if (file.isDirectory && (ignoresdir == null || !ignoresdir(file, filepath))) {
    //                collect1(file, filepath, bottomup, ignoresdir, collector, includes)
    //            }
    //            if (bottomup && (includes == null || includes(file, filepath))) {
    //                yield(collector(file, filepath))
    //            }
    //        }
    //    }
    //#END OBSOLETED
}

////////////////////////////////////////////////////////////////////

