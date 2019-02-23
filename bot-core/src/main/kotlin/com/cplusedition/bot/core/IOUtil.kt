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

import com.cplusedition.bot.core.IOUtil.Companion.IOUt
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset

open class StringPrintWriter : PrintWriter, CharSequence {

    private val stringWriter: StringWriter

    val buffer: StringBuffer
        get() = stringWriter.buffer
    override val length: Int get() = stringWriter.buffer.length

    constructor() : this(StringWriter())

    constructor(initsize: Int) : this(StringWriter(initsize))

    constructor(w: StringWriter) : super(w) {
        stringWriter = w
    }

    override operator fun get(index: Int): Char {
        return stringWriter.buffer[index]
    }

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
        return stringWriter.buffer.subSequence(startIndex, endIndex)
    }

    fun print(msgs: Iterator<String>) {
        if (msgs.hasNext()) print(msgs.join(""))
    }

    fun print(msgs: Iterable<String>) {
        print(msgs.iterator())
    }

    fun print(msgs: Sequence<String>) {
        print(msgs.iterator())
    }

    fun println(msgs: Iterator<String>) {
        if (msgs.hasNext()) println(msgs.join(TextUtil.TextUt.LB))
    }

    fun println(msgs: Iterable<String>) {
        println(msgs.iterator())
    }

    fun println(msgs: Sequence<String>) {
        println(msgs.iterator())
    }

    override fun toString(): String {
        return stringWriter.toString()
    }

}

////////////////////////////////////////////////////////////////////////

open class NumberReader(
    var b: ByteArray
) {

    private fun ishl(offset: Int, shift: Int): Int {
        return b[offset].toInt() shl shift
    }

    private fun u8shl(offset: Int, shift: Int): Int {
        return u8(offset) shl shift
    }

    private fun lu8shl(offset: Int, shift: Int): Long {
        return lu8(offset) shl shift
    }

    private fun u8(offset: Int): Int {
        return (b[offset].toInt() and 0xff)
    }

    private fun lu8(offset: Int): Long {
        return (b[offset].toLong() and 0xffL)
    }

    private fun u32BE(offset: Int): Long {
        return (lu8shl(offset, 24)
                or lu8shl(offset + 1, 16)
                or lu8shl(offset + 2, 8)
                or lu8(offset + 3))
    }

    private fun u32LE(offset: Int): Long {
        return (lu8(offset)
                or lu8shl(offset + 1, 8)
                or lu8shl(offset + 2, 16)
                or lu8shl(offset + 3, 24))
    }

    fun i8(): Int {
        return b[0].toInt()
    }

    fun u8(): Int {
        return u8(0)
    }

    fun i16BE(): Int {
        return (ishl(0, 8) or u8(1))
    }

    fun i16LE(): Int {
        return (u8(0) or ishl(1, 8))
    }

    fun u16BE(): Int {
        return (i16BE() and 0xffff)
    }

    fun u16LE(): Int {
        return (i16LE() and 0xffff)
    }

    fun i32BE(): Int {
        return (ishl(0, 24)
                or u8shl(1, 16)
                or u8shl(2, 8)
                or u8(3))
    }

    fun i32LE(): Int {
        return (u8(0)
                or u8shl(1, 8)
                or u8shl(2, 16)
                or ishl(3, 24))
    }

    fun u32BE(): Long {
        return u32BE(0)
    }

    fun u32LE(): Long {
        return u32LE(0)
    }

    fun i64BE(): Long {
        return ((u32BE(0) shl 32) or u32BE(4))
    }

    fun i64LE(): Long {
        return (u32LE(0) or (u32LE(4) shl 32))
    }
}

////////////////////////////////////////////////////////////////////

open class ByteReader(
    val input: InputStream
) {

    constructor(array: ByteArray) : this(ByteArrayInputStream(array))

    var buf = ByteArray(8)
    var b = NumberReader(buf)

    private fun fully(len: Int): NumberReader {
        IOUt.readFully(input, buf, 0, len)
        return b
    }

    @Throws(IOException::class)
    fun read(): Int {
        val b = input.read()
        if (b < 0) {
            throw EOFException()
        }
        return b
    }

    @Throws(IOException::class)
    fun read(ret: ByteArray): ByteArray {
        IOUt.readFully(input, ret)
        return ret
    }

    @Throws(IOException::class)
    fun read(ret: ByteArray, off: Int, len: Int): ByteArray {
        IOUt.readFully(input, ret, off, len)
        return ret
    }

    @Throws(IOException::class)
    fun i8(): Int {
        return read().toByte().toInt()
    }

    @Throws(IOException::class)
    fun u8(): Int {
        return read()
    }

    @Throws(IOException::class)
    fun i16BE(): Int {
        return fully(2).i16BE()
    }

    @Throws(IOException::class)
    fun i16LE(): Int {
        return fully(2).i16LE()
    }

    @Throws(IOException::class)
    fun u16BE(): Int {
        return fully(2).u16BE()
    }

    @Throws(IOException::class)
    fun u16LE(): Int {
        return fully(2).u16LE()
    }

    @Throws(IOException::class)
    fun i32BE(): Int {
        return fully(4).i32BE()
    }

    @Throws(IOException::class)
    fun i32LE(): Int {
        return fully(4).i32LE()
    }

    @Throws(IOException::class)
    fun u32BE(): Long {
        return fully(4).u32BE()
    }

    @Throws(IOException::class)
    fun u32LE(): Long {
        return fully(4).u32LE()
    }

    @Throws(IOException::class)
    fun i64BE(): Long {
        return fully(8).i64BE()
    }

    @Throws(IOException::class)
    fun i64LE(): Long {
        return fully(8).i64LE()
    }

    @Throws(IOException::class)
    fun utf8(len: Int): CharSequence {
        val b = ByteArray(len)
        IOUt.readFully(input, b)
        return IOUt.UTF_8.decode(ByteBuffer.wrap(b))
    }

    ////////////////////////////////////////////////////////////////////////

}

open class NumberWriter(
    val b: ByteArray
) {

    private fun shr(value: Int, shift: Int): Byte {
        return (value shr shift).toByte()
    }

    private fun shr(value: Long, shift: Int): Byte {
        return (value shr shift).toByte()
    }

    fun i16BE(value: Int) {
        b[0] = shr(value, 8)
        b[1] = value.toByte()
    }

    fun i16LE(value: Int) {
        b[0] = value.toByte()
        b[1] = shr(value, 8)
    }

    fun i32BE(value: Int) {
        b[0] = shr(value, 24)
        b[1] = shr(value, 16)
        b[2] = shr(value, 8)
        b[3] = value.toByte()
    }

    fun i32LE(value: Int) {
        b[0] = value.toByte()
        b[1] = shr(value, 8)
        b[2] = shr(value, 16)
        b[3] = shr(value, 24)
    }

    fun i64BE(value: Long) {
        b[0] = shr(value, 56)
        b[1] = shr(value, 48)
        b[2] = shr(value, 40)
        b[3] = shr(value, 32)
        b[4] = shr(value, 24)
        b[5] = shr(value, 16)
        b[6] = shr(value, 8)
        b[7] = value.toByte()
    }

    fun i64LE(value: Long) {
        b[0] = value.toByte()
        b[1] = shr(value, 8)
        b[2] = shr(value, 16)
        b[3] = shr(value, 24)
        b[4] = shr(value, 32)
        b[5] = shr(value, 40)
        b[6] = shr(value, 48)
        b[7] = shr(value, 56)
    }
}

////////////////////////////////////////////////////////////////////////

open class ByteWriter(
    val out: OutputStream
) {
    val b = ByteArray(8)
    val w = NumberWriter(b)

    @Throws(IOException::class)
    fun write(b: Int) {
        out.write(b)
    }

    @Throws(IOException::class)
    fun write(a: ByteArray) {
        out.write(a, 0, a.size)
    }

    @Throws(IOException::class)
    fun write(a: ByteArray, start: Int, len: Int) {
        out.write(a, start, len)
    }

    @Throws(IOException::class)
    fun write16BE(value: Int) {
        w.i16BE(value)
        out.write(b, 0, 2)
    }

    @Throws(IOException::class)
    fun write16LE(value: Int) {
        w.i16LE(value)
        out.write(b, 0, 2)
    }

    @Throws(IOException::class)
    fun write32BE(value: Int) {
        w.i32BE(value)
        out.write(b, 0, 4)
    }

    @Throws(IOException::class)
    fun write32LE(value: Int) {
        w.i32LE(value)
        out.write(b, 0, 4)
    }

    @Throws(IOException::class)
    fun write64BE(value: Long) {
        w.i64BE(value)
        out.write(b, 0, 8)
    }

    @Throws(IOException::class)
    fun write64LE(value: Long) {
        w.i64LE(value)
        out.write(b, 0, 8)
    }
}

////////////////////////////////////////////////////////////////////////

open class IOUtil {

    companion object {
        val IOUt = IOUtil()
    }

    val isNativeBigEndian = ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN
    val UTF_8 = Charset.forName("UTF-8")

    ////////////////////////////////////////////////////////////////////////

    @Throws(IOException::class)
    fun readFully(input: InputStream, a: ByteArray): ByteArray {
        readFully(input, a, 0, a.size)
        return a
    }

    @Throws(IOException::class)
    fun readFully(input: InputStream, a: ByteArray, offset: Int, length: Int) {
        var off = offset
        var len = length
        while (len > 0) {
            val n = input.read(a, off, len)
            if (n < 0) {
                throw EOFException("Expected=" + len + ", actual=" + (len - len))
            }
            len -= n
            off += n
        }
    }

    /** Like readFully(), except that it allow partial reads if EOF is reached, and returns number of bytes read.  */
    @Throws(IOException::class)
    fun readAsMuchAsPossible(input: InputStream, a: ByteArray, offset: Int, length: Int): Int {
        var off = offset
        var len = length
        val olen = len
        while (len > 0) {
            val n = input.read(a, off, len)
            if (n < 0) {
                break
            }
            len -= n
            off += n
        }
        return olen - len
    }

    @Throws(IOException::class)
    fun skipFully(input: InputStream, length: Long) {
        var remaining = length
        //// HACK: Default implementation should has try to skip as much as possible before return.
        //// But just in case some implementation do not loop.
        var n: Long
        while (remaining > 0) {
            n = input.skip(remaining)
            if (n <= 0) {
                throw EOFException("Expected=" + length + ", actual=" + (length - remaining))
            }
            remaining -= n
        }
    }

    ////////////////////////////////////////////////////////////////////////

    fun htons(v: Short): Short {
        return if (isNativeBigEndian) v else swap(v)
    }

    fun htonl(v: Int): Int {
        return if (isNativeBigEndian) v else swap(v)
    }

    fun htonl(v: Long): Long {
        return if (isNativeBigEndian) v else swap(v)
    }

    fun ntohs(v: Short): Short {
        return if (isNativeBigEndian) v else swap(v)
    }

    fun ntohl(v: Int): Int {
        return if (isNativeBigEndian) v else swap(v)
    }

    fun ntohl(v: Long): Long {
        return if (isNativeBigEndian) v else swap(v)
    }

    fun swap(x: Short): Short {
        val ix = x.toInt()
        return (ix shl 8 or ix.ushr(8)).toShort()
    }

    fun swap(x: Char): Char {
        return (x.toInt() shl 8 or x.toInt().ushr(8)).toChar()
    }

    fun swap(x: Int): Int {
        return x shl 24 or (x and 0x0000ff00 shl 8) or (x and 0x00ff0000).ushr(8) or x.ushr(24)
    }

    fun swap(x: Long): Long {
        return swap(x.toInt()).toLong() shl 32 or (swap(x.ushr(32).toInt()).toLong() and 0xffffffffL)
    }
}

////////////////////////////////////////////////////////////////////

