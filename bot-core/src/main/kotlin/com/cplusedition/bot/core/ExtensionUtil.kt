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
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.*
import java.nio.charset.Charset
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.locks.Lock
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

fun Int.isOdd(): Boolean {
    return (this and 0x01) != 0
}

fun <T> MutableList<T>.removeLast(): T? {
    if (isEmpty()) {
        return null
    }
    val ret = this.last()
    this.removeAt(this.lastIndex)
    return ret
}

fun <T> MutableList<T>.addAll(vararg elements: T): Boolean {
    return addAll(elements)
}

fun <T> Iterator<T>.join(sep: CharSequence): String {
    if (!hasNext()) return ""
    val first = next().toString()
    if (!hasNext()) return first
    val b = StringBuilder(first)
    if (sep.isEmpty()) {
        for (t in this) b.append(t.toString())
    } else {
        for (t in this) {
            b.append(sep)
            b.append(t.toString())
        }
    }
    return b.toString()
}

fun <T> Iterable<T>.join(sep: CharSequence): String {
    return this.iterator().join(sep)
}

fun <T> Iterable<T>.joinln(): String {
    return this.iterator().join(TextUt.LB)
}

/**
 * @return A filepath joined by File.separator.
 */
fun <T> Iterable<T>.joinPath(): String {
    return this.iterator().join(File.separator)
}

fun <K, V> Map<K, V>.map(mapper: (K, V) -> V?): MutableMap<K, V> {
    val ret = mutableMapOf<K, V>()
    for ((k, v) in entries) {
        val value = mapper(k, v) ?: continue
        ret[k] = value
    }
    return ret
}

fun <K, V> MutableMap<K, V>.add(map: Map<K, V>): MutableMap<K, V> {
    for ((k, v) in map.entries) {
        if (v != null) {
            this[k] = v
        }
    }
    return this
}

fun <K, V> MutableMap<K, V>.add(key: K, value: V): MutableMap<K, V> {
    if (value != null) {
        this[key] = value
    }
    return this
}

fun <T> Array<T>.join(sep: CharSequence): String {
    return this.iterator().join(sep)
}

fun <T> Array<T>.joinln(): String {
    return this.iterator().join(TextUt.LB)
}

/**
 * @return Filepath joined by File.separator.
 */
fun <T> Array<T>.joinPath(): String {
    return this.iterator().join(File.separator)

}

fun <F, S> Pair<F, S>.join(sep: CharSequence): String {
    return "$first$sep$second"
}

fun NodeList.elements(): Iterable<Element> {
    return NodeListElementIterable(this)
}

fun NodeList.nodes(): Iterable<Node> {
    return NodeListIterable(this)
}

class ElementListIterable(
        private val list: NodeList
) : Iterable<Element>, Iterator<Element> {
    var length = list.length
    private var index = 0
    override fun hasNext(): Boolean {
        return index < length
    }

    override fun next(): Element {
        return list.item(index++) as Element
    }

    override fun iterator(): Iterator<Element> {
        return this
    }
}

class NodeListElementIterable(
        private val list: NodeList
) : Iterable<Element>, Iterator<Element> {
    var length = list.length
    private var index = 0
    private var current: Element? = null

    init {
        next1()
    }

    override fun hasNext(): Boolean {
        return current != null
    }

    override fun next(): Element {
        val ret = current!!
        next1()
        return ret
    }

    override fun iterator(): Iterator<Element> {
        return this
    }

    private fun next1() {
        for (i in index until length) {
            val e = list.item(i)
            if (e is Element) {
                index = i + 1
                current = e
                return
            }
        }
        index = length
        current = null

    }
}

class NodeListIterable(
        private val list: NodeList
) : Iterable<Node>, Iterator<Node> {
    var length = list.length
    private var index = 0
    override fun hasNext(): Boolean {
        return index < length
    }

    override fun next(): Node {
        return list.item(index++)
    }

    override fun iterator(): Iterator<Node> {
        return this
    }

}

open class WithUtil {

    companion object {
        val With = WithUtil()
    }

    fun exceptionOrFalse(code: Fun00): Boolean {
        return try {
            code()
            false
        } catch (e: Exception) {
            true
        }
    }

    /**
     * @return The exception or null.
     */
    fun exceptionOrNull(code: Fun00): Exception? {
        return try {
            code()
            null
        } catch (e: Exception) {
            e
        }
    }

    /**
     * If code throws an exception, ignores it.
     * If code does not throw an exception, throw an exception.
     */
    fun exceptionOrFail(code: Fun00) {
        try {
            code()
        } catch (e: Exception) {
            // Expected exception
            return
        }
        throw IllegalStateException()
    }

    /**
     * @return the throwable thrown by the code or null.
     */
    fun throwableOrNull(code: Fun00): Throwable? {
        return try {
            code()
            null
        } catch (e: Throwable) {
            e
        }
    }

    /**
     * If code throws a Throwable, ignores it.
     * If code does not throw a Throwable, throw an exception.
     */
    fun throwableOrFail(code: Fun00) {
        try {
            code()
        } catch (e: Throwable) {
            // Expected throwable
            return
        }
        throw IllegalStateException()
    }

    fun <T : Closeable, R> closeable(target: T, code: Fun11<T, R>): R {
        return target.use(code)
    }

    //    fun <T : Closeable, R> closeable(target: T, code: Fun11<T, R>): R {
    //        try {
    //            return code(target)
    //        } finally {
    //            FileUt.close(target)
    //        }
    //    }

    @Throws(IOException::class)
    fun <R> inputStream(file: File, code: Fun11<InputStream, R>): R {
        return FileInputStream(file).use(code)
    }

    @Throws(IOException::class)
    fun <R> outputStream(file: File, code: Fun11<OutputStream, R>): R {
        return FileOutputStream(file).use(code)
    }

    @Throws(IOException::class)
    fun <R> printWriter(file: File, code: Fun11<PrintWriter, R>): R {
        return PrintWriter(file).use(code)
    }

    @Throws(IOException::class)
    fun zipInputStream(zipfile: File, code: Fun20<ZipInputStream, ZipEntry>) {
        ZipInputStream(BufferedInputStream(FileInputStream(zipfile))).use { zipinput ->
            while (true) {
                val entry = zipinput.nextEntry ?: break
                code(zipinput, entry)
            }
        }
    }

    @Throws(IOException::class)
    fun zipOutputStream(zipfile: File, code: Fun10<ZipOutputStream>) {
        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipfile))).use(code)
    }

    @Throws(IOException::class)
    fun bufferedWriter(file: File, charset: Charset = Charsets.UTF_8, code: Fun10<BufferedWriter>) {
        BufferedWriter(OutputStreamWriter(FileOutputStream(file), charset)).use(code)
    }

    @Throws(IOException::class)
    fun bufferedReader(file: File, charset: Charset = Charsets.UTF_8, code: Fun10<BufferedReader>) {
        BufferedReader(InputStreamReader(FileInputStream(file), charset)).use(code)
    }

    @Throws(IOException::class)
    fun bytes(file: File, bufsize: Int = K.BUFSIZE, code: Fun20<ByteArray, Int>) {
        FileInputStream(file).use { input ->
            val buf = ByteArray(bufsize)
            while (true) {
                val n = input.read(buf)
                if (n < 0) {
                    break
                }
                code(buf, n)
            }
        }
    }

    @Throws(IOException::class)
    fun lines(file: File, charset: Charset = Charsets.UTF_8, code: Fun10<String>) {
        BufferedReader(InputStreamReader(FileInputStream(file), charset)).use { reader ->
            while (true) {
                val line = reader.readLine() ?: break
                code(line)
            }
        }
    }

    /**
     * Rewrite a file with a text content transform.
     *
     * @param code(String): String?.
     */
    @Throws(IOException::class)
    fun rewriteText(file: File, charset: Charset = Charsets.UTF_8, code: Fun11<String, String>): Boolean {
        val input = file.readText(charset)
        val output = code(input)
        val modified = (output != input)
        if (modified) {
            file.writeText(output, charset)
        }
        return modified
    }

    /**
     * Rewrite a file with a line by line transform.
     *
     * @param code(String): String?.
     */
    @Throws(IOException::class)
    fun rewriteLines(file: File, charset: Charset = Charsets.UTF_8, code: Fun11<String, String?>) {
        backup(file) { dst, src ->
            bufferedReader(src, charset) { reader ->
                bufferedWriter(dst, charset) { writer ->
                    while (true) {
                        val line = reader.readLine() ?: break
                        val output = code(line)
                        if (output != null) writer.appendln(output)
                    }
                }
            }
        }
    }

    /**
     *  If code() does not return null then fail.
     */
    @Throws(IllegalStateException::class)
    fun nullOrFail(code: Fun01<String?>) {
        val error = code() ?: return
        throw IllegalStateException(error)
    }

    /**
     * @param code(tmpdir)
     * @throws Exception If operation fail.
     */
    @Throws(Exception::class)
    fun tmpdir(dir: File? = null, code: Fun10<File>) {
        val tmpdir = createTempDir(directory = dir)
        try {
            code(tmpdir)
        } finally {
            tmpdir.deleteRecursively()
        }
    }

    /**
     * @param code(tmpfile)
     * @throws Exception If operation fail.
     */
    @Throws(Exception::class)
    fun tmpfile(suffix: String = "tmp", dir: File? = null, code: Fun10<File>) {
        val tmpfile = createTempFile(suffix = suffix, directory = dir)
        try {
            code(tmpfile)
        } finally {
            tmpfile.delete()
        }
    }

    /**
     * @param code(tmpdir): T
     * @throws Exception If operation fail.
     */
    @Throws(Exception::class)
    fun <T> tmpdir(dir: File? = null, code: Fun11<File, T>): T {
        val tmpdir = createTempDir(directory = dir)
        try {
            return code(tmpdir)
        } finally {
            tmpdir.deleteRecursively()
        }
    }

    /**
     * @param code(tmpfile): T
     * @throws Exception If operation fail.
     */
    @Throws(Exception::class)
    fun <T> tmpfile(suffix: String? = null, dir: File? = null, code: Fun11<File, T>): T {
        val tmpfile = createTempFile(suffix = suffix, directory = dir)
        try {
            return code(tmpfile)
        } finally {
            tmpfile.delete()
        }
    }

    /**
     * Create a tmpfile, call code(tmpfile, file) with tmpfile as dst and input file as src.
     * If operation succeed, ie. without throwing exception, copy tmpfile to input file.
     * Delete the tmpfile in either case,
     *
     * @param code(dstfile, srcfile)
     * @throws Exception If operation fail.
     */
    @Throws(Exception::class)
    fun backup(outfile: File, code: Fun20<File, File>) {
        val tmpfile = createTempFile()
        try {
            code(tmpfile, outfile)
            FileUt.copy(outfile, tmpfile)
        } finally {
            tmpfile.delete()
        }
    }

    /**
     * Delete the backupfile if it exists and copy the outfile to the backupfile.
     * Perform the code() operation and write to the outfile.
     * If operation fail, restore outfile from the backup and the backup get deleted.
     * If operation suceed, the result stays at the outfile and the backupfile contains content
     * of the original outfile.
     *
     * @param code(dstfile, srcfile)
     * @throws Exception If operation fail.
     */
    @Throws(Exception::class)
    fun backup(outfile: File, backupfile: File, code: Fun20<File, File>) {
        if (backupfile.exists() && !backupfile.delete()) throw IOException("Delete backup failed")
        if (outfile.exists() && !outfile.renameTo(backupfile)) {
            FileUt.copy(backupfile, outfile)
        }
        try {
            code(outfile, backupfile)
        } catch (e: Throwable) {
            outfile.delete()
            if (backupfile.exists() && !backupfile.renameTo(outfile)) {
                FileUt.copy(outfile, backupfile)
                backupfile.delete()
            }
            throw IOException(e)
        }
    }

    /**
     * Execute the code that generate a list of values, shuffle it and add the shuffled list to ret.
     */
    fun <V> shuffle(ret: MutableList<V>, code: Fun10<MutableList<V>>) {
        val buf = ArrayList<V>()
        code(buf)
        buf.shuffle()
        ret.addAll(buf)
    }

    fun <V> lock(lock: Lock, code: Fun01<V>): V {
        lock.lock()
        try {
            return code()
        } finally {
            lock.unlock()
        }
    }

    @Throws(TimeoutException::class, InterruptedException::class)
    fun sync(
            count: Int = 1,
            timeout: Long = DateUt.DAY,
            timeunit: TimeUnit = TimeUnit.MILLISECONDS,
            code: Fun10<Fun00>
    ) {
        val done = CountDownLatch(count)
        code { done.countDown() }
        if (!done.await(timeout, timeunit)) throw TimeoutException()
    }

    @Throws(TimeoutException::class, InterruptedException::class)
    fun <V> sync(
            count: Int = 1,
            timeout: Long = DateUt.DAY,
            timeunit: TimeUnit = TimeUnit.MILLISECONDS,
            code: Fun10<Fun10<V>>
    ): V {
        val done = CountDownLatch(count)
        var ret: V? = null
        code { result ->
            ret = result
            done.countDown()
        }
        if (!done.await(timeout, timeunit)) throw TimeoutException()
        return ret!!
    }
}

open class WithoutUtil {

    companion object {
        val Without = WithoutUtil()
    }

    /**
     * @return False if code() throws an exception, otherwise true.
     * Example: let ok = Without.exceptionOrFalse { code }
     */
    fun exceptionOrFalse(code: Fun00): Boolean {
        return try {
            code()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * @return Result of the given block or null if there is an exception.
     * Example: let value = Without.exception(Int.parse(s)) ?: -1
     */
    fun <T> exceptionOrNull(code: () -> T): T? {
        return try {
            code()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * @return Result of the given block or null if there is an exception.
     * Example: let value = Without.exception(Int.parse(s)) ?: -1
     */
    fun <T> exceptionOrFail(code: () -> T): T {
        try {
            return code()
        } catch (e: Exception) {
            throw IllegalStateException(e)
        }
    }

    /**
     * @return Result of the given block or null if there is an exception.
     * Example: let value = Without.throwable(Int.parse(s)) ?: -1
     */
    fun <T> throwableOrNull(code: () -> T): T? {
        return try {
            code()
        } catch (e: Throwable) {
            null
        }
    }

    /**
     * @return Result of the given block or null if there is an exception.
     * Example: let value = Without.throwable(Int.parse(s)) ?: -1
     */
    fun <T> throwableOrFail(code: () -> T): T? {
        try {
            return code()
        } catch (e: Throwable) {
            throw IllegalStateException(e)
        }
    }

    fun comments(file: File, prefix: String = "#", code: (String) -> Unit) {
        With.lines(file) {
            // TODO Skip space instead of trim().
            val s = it.trim()
            if (s.isNotEmpty() && !s.startsWith(prefix)) {
                code(it)
            }
        }
    }
}

private object K {
    const val BUFSIZE = 8192
}
