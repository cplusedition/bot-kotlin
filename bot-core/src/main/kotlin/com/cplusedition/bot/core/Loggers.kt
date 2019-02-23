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

import java.io.PrintStream

interface ILog {
    fun d(msg: String, e: Throwable? = null)
    fun i(msg: String, e: Throwable? = null)
    fun w(msg: String, e: Throwable? = null)
    fun e(msg: String, e: Throwable? = null)
}

object SystemLogger : PrintStreamLogger(true, System.out, System.err), ILog

open class PrintStreamLogger(
    private val debugging: Boolean = true,
    private val out: PrintStream,
    private val err: PrintStream
) : ILog {

    override fun d(msg: String, e: Throwable?) {
        if (debugging) {
            out.println(msg)
            e?.printStackTrace(out)
        }
    }

    override fun i(msg: String, e: Throwable?) {
        out.println(msg)
        if (debugging && e != null) {
            e.printStackTrace(out)
        }
    }

    override fun w(msg: String, e: Throwable?) {
        out.println(msg)
        if (debugging && e != null) {
            e.printStackTrace(out)
        }
    }

    override fun e(msg: String, e: Throwable?) {
        err.println(msg)
        e?.printStackTrace(err)
    }
}

open class StringLogger(
    private val debugging: Boolean = true
) : ILog {

    private var out = StringPrintWriter()

    override fun d(msg: String, e: Throwable?) {
        if (this.debugging) {
            out.println(msg)
            e?.printStackTrace(out)
        }
    }

    override fun i(msg: String, e: Throwable?) {
        out.println(msg)
        if (this.debugging && e != null) {
            e.printStackTrace(out)
        }
    }

    override fun w(msg: String, e: Throwable?) {
        out.println(msg)
        if (this.debugging && e != null) {
            e.printStackTrace(out)
        }
    }

    override fun e(msg: String, e: Throwable?) {
        out.println(msg)
        e?.printStackTrace(out)
    }

    override fun toString(): String {
        return out.toString()
    }
}