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

import java.util.*

open class StructUtil {

    companion object {
        val StructUt = StructUtil()
    }

    fun <T> concat(vararg a: Collection<T>): MutableList<T> {
        val ret = ArrayList<T>()
        for (c in a) {
            ret.addAll(c)
        }
        return ret
    }

    fun equals(b1: ByteArray, b2: ByteArray): Boolean {
        if (b1.size != b2.size) return false
        for (i in 0..b1.lastIndex) {
            if (b1[i] != b2[i]) return false
        }
        return true
    }

    fun equals(a: ByteArray, b: ByteArray, boff: Int, blen: Int): Boolean {
        if (a.size != blen) {
            return false
        }
        for (i in 0 until blen) {
            if (a[i] != b[boff + i]) {
                return false
            }
        }
        return true
    }

    fun equals(a: ByteArray, aoff: Int, alen: Int, b: ByteArray, boff: Int, blen: Int): Boolean {
        if (alen != blen) {
            return false
        }
        for (i in 0 until blen) {
            if (a[aoff + i] != b[boff + i]) {
                return false
            }
        }
        return true
    }

    fun <T> equals(b1: Array<T>, b2: Array<T>): Boolean {
        if (b1.size != b2.size) return false
        for (i in 0..b1.lastIndex) {
            if (b1[i] != b2[i]) return false
        }
        return true
    }

    fun <T> equals(a: Array<T>, b: Array<T>, boff: Int, blen: Int): Boolean {
        if (a.size != blen) {
            return false
        }
        for (i in 0 until blen) {
            if (a[i] != b[boff + i]) {
                return false
            }
        }
        return true
    }

    fun <T> equals(a: Array<T>, aoff: Int, alen: Int, b: Array<T>, boff: Int, blen: Int): Boolean {
        if (alen != blen) {
            return false
        }
        for (i in 0 until blen) {
            if (a[aoff + i] != b[boff + i]) {
                return false
            }
        }
        return true
    }

    fun <T> equals(a: Collection<T>, b: Collection<T>): Boolean {
        if (a.size != b.size) return false
        val ita = a.iterator()
        val itb = b.iterator()
        while (ita.hasNext()) {
            if (ita.next() != itb.next()) return false
        }
        return true
    }

    fun <T : Comparable<T>> diff(a: Set<T>, b: Set<T>): DiffStat<T> {
        val ret = DiffStat<T>()
        ret.bonly.addAll(b)
        for (e in a) {
            if (ret.bonly.remove(e)) {
                ret.sames.add(e)
            } else {
                ret.aonly.add(e)
            }
        }
        return ret
    }

    fun <T> diff(a: Map<String, T>, b: Map<String, T>): DiffStat<String> {
        val ret = DiffStat<String>()
        ret.bonly.addAll(b.keys)
        for ((ka, va) in a) {
            if (!ret.bonly.remove(ka)) {
                ret.aonly.add(ka)
                continue
            }
            val vb = b[ka]
            if (va == vb) {
                ret.sames.add(ka)
            } else {
                ret.diffs.add(ka)
            }
        }
        return ret
    }

    /**
     * @return -1 if a==null && b!=null,
     * 1 if a!=null && b==null,
     * 0, if a==null && b== null,
     * else return callback(a!!, b!!).
     */
    fun <T> compareNullable(a: T?, b: T?, callback: (T, T) -> Int): Int {
        return if (a == null) {
            if (b == null) 0 else -1
        } else {
            if (b == null) 1 else callback(a, b)
        }
    }
}

open class DiffStat<T : Comparable<T>> {
    val aonly = TreeSet<T>()
    val bonly = TreeSet<T>()
    val diffs = TreeSet<T>()
    val sames = TreeSet<T>()

    fun hasDiff(): Boolean {
        return aonly.size > 0 || bonly.size > 0 || diffs.size > 0
    }

    override fun toString(): String {
        return toString("A", "B")
    }

    fun toString(
        msg1: String,
        msg2: String,
        printsames: Boolean = false,
        printaonly: Boolean = true,
        printbonly: Boolean = true,
        printdiffs: Boolean = true
    ): String {
        val w = StringPrintWriter()
        if (printsames) {
            w.println("### Same: ${sames.size}")
            sames.forEach { w.println(it.toString()) }
        }
        if (printaonly) {
            w.println("### $msg1 only: ${aonly.size}")
            aonly.forEach { w.println(it.toString()) }
        }
        if (printbonly) {
            w.println("### $msg2 only: ${bonly.size}")
            bonly.forEach { w.println(it.toString()) }
        }
        if (printdiffs) {
            w.println("### Diff: ${diffs.size}")
            diffs.forEach { w.println(it.toString()) }
        }
        return w.toString()
    }
}
