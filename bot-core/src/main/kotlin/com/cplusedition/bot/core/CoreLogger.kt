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

import com.cplusedition.bot.core.TextUtil.Companion.TextUt
import java.io.File
import java.io.IOException
import java.io.PrintStream
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.Future
import kotlin.reflect.KCallable
import kotlin.reflect.KClass

//////////////////////////////////////////////////////////////////////

interface ICoreLogger : ILog {

    val debugging: Boolean
    val prefix: String

    /**
     * @return The system time in ms when the logger is created.
     */
    val startTime: Long

    /**
     * @return The error Count.
     * Note that this method is synchronous.
     */
    val errorCount: Int

    /**
     * Reset the error count.
     * @return The error count before reset.
     */
    fun resetErrorCount(): Int

    /**
     * Wait for all previous log task to complete and flush output.
     * Note that this method is synchronous.
     */
    fun flush()

    /**
     * Suppress logging while executing the given code.
     */
    fun quiet(code: Fun00)

    /**
     * Save log to a file.
     */
    fun saveLog(file: File)

    /**
     * Get a copy of the log.
     * Note that this method is synchronous.
     */
    fun getLog(): List<String>

    /** Log an error, ie. increment errorCount, without any log message. */
    fun e()

    /** Shortcut for log.d(any.toString()). */
    fun d(any: Any)

    fun d(vararg msgs: String)
    fun i(vararg msgs: String)
    fun w(vararg msgs: String)
    fun e(vararg msgs: String)

    fun d(msgs: Iterable<String>)
    fun i(msgs: Iterable<String>)
    fun w(msgs: Iterable<String>)
    fun e(msgs: Iterable<String>)

    fun d(msgs: Iterator<String>)
    fun i(msgs: Iterator<String>)
    fun w(msgs: Iterator<String>)
    fun e(msgs: Iterator<String>)

    fun dfmt(format: String, vararg args: Any)
    fun ifmt(format: String, vararg args: Any)
    fun wfmt(format: String, vararg args: Any)
    fun efmt(format: String, vararg args: Any)

    /**
     * Debug message with timestamp.
     */
    fun dd(msg: String)

    /**
     * Info message with timestamp.
     */
    fun ii(msg: String)

    /**
     * Enter a new scope.
     * It debug mode, it log a message with timestamp.
     */
    fun enter(name: String? = null, msg: String? = null)

    /**
     * Enter a new scope, execute the code and leave the scope.
     * It debug mode, it log a message on enter and leave with timestamp.
     * The delta time in the leave message is the time elapsed since start of the scope.
     */
    fun enter(name: String? = null, msg: String? = null, code: Fun00)

    /**
     * Enter a new scope, execute the code, leave the scope and return the result.
     * It debug mode, it log a message on enter and leave with timestamp.
     * The delta time in the leave message is the time elapsed since start of the scope.
     */
    fun <T> enter(name: String? = null, msg: String? = null, code: Fun01<T>): T

    /**
     * Like enter(name, msg, code), but invoke leaveX() instead of leave() on leave.
     */
    fun enterX(name: String? = null, msg: String? = null, code: Fun00)

    /**
     * Like enter(name, msg, code), but invoke leaveX() instead of leave() on leave.
     */
    @Throws(IllegalStateException::class)
    fun <T> enterX(name: String? = null, msg: String? = null, code: Fun01<T>): T?

    /**
     * Leave a scope.
     * In debug mode, it log a message with delta and timestamp.
     * The delta time is time elapsed since start of the scope.
     */
    fun leave(msg: String? = null)

    /**
     * Like leave(msg), but throw an exception if there are errors logged.
     * Note that this method is synchronous.
     */
    @Throws(IllegalStateException::class)
    fun leaveX(msg: String? = null)

    // Shortcuts.

    fun enter(f: KCallable<*>, msg: String? = null) {
        enter(f.name, msg)
    }

    fun enter(f: KCallable<*>, msg: String? = null, code: Fun00) {
        enter(f.name, msg, code)
    }

    fun enterX(f: KCallable<*>, msg: String? = null, code: Fun00) {
        enterX(f.name, msg, code)
    }

    fun <T> enter(f: KCallable<*>, msg: String? = null, code: Fun01<T>): T {
        return enter(f.name, msg, code)
    }

    fun <T> enterX(f: KCallable<*>, msg: String? = null, code: Fun01<T>): T? {
        return enterX(f.name, msg, code)
    }

    fun enter(c: KClass<*>, msg: String? = null) {
        enter(c.simpleName ?: c.toString(), msg)
    }

    fun enter(c: KClass<*>, msg: String? = null, code: Fun00) {
        enter(c.simpleName ?: c.toString(), msg, code)
    }

    fun enterX(c: KClass<*>, msg: String? = null, code: Fun00) {
        enterX(c.simpleName ?: c.toString(), msg, code)
    }

    fun <T> enter(c: KClass<*>, msg: String? = null, code: Fun01<T>): T {
        return enter(c.simpleName ?: c.toString(), msg, code)
    }

    fun <T> enterX(c: KClass<*>, msg: String? = null, code: Fun01<T>): T? {
        return enterX(c.simpleName ?: c.toString(), msg, code)
    }

    /**
     * Check that at least one error occurs in code().
     * If so, clear the error status, otherwise log an error.
     */
    fun expectError(msg: String = "# ERROR: Expecting error, but not occurred", code: Fun00) {
        enter {
            code()
            if (resetErrorCount() == 0) {
                e(msg)
            }
        }
    }
}

//////////////////////////////////////////////////////////////////////

/**
 * A thread safe logger.
 * Note that unless otherwise specified, all calls are asynchronous.
 * In general, methods that retreive status, eg. errorCount, are synchronous.
 */
open class CoreLogger(
    final override val debugging: Boolean,
    final override val startTime: Long = System.currentTimeMillis(),
    final override val prefix: String = "####",
    out: PrintStream = System.out,
    err: PrintStream = System.err
) : ICoreLogger {

    ////////////////////////////////////////////////////////////////////////

    interface ILifecycleListener {
        fun onStart(msg: String, starttime: Long, logger: Fun10<String>)
        fun onDone(msg: String, endtime: Long, errors: Int, logger: Fun10<String>)
    }

    ////////////////////////////////////////////////////////////////////////

    private class Delegate(
        private val debugging: Boolean,
        private var startTime: Long,
        private val prefix: String,
        private val out: PrintStream,
        private val err: PrintStream
    ) {

        private class Info(
            val name: String?,
            val errorCount: Int,
            val startTime: Long
        )

        private val callStack = Stack<Info>()
        private val quietStack = Stack<Boolean>()
        private val logs = ArrayList<String>()
        private val listeners = ArrayList<ILifecycleListener>()
        private val executor = Executors.newSingleThreadExecutor()
        private val prefixEnter = "$prefix +++++++"
        private val prefixTimestamp = "$prefix        "

        private var quiet = false
        private var errorCount = 0

        fun getErrorCount(): Int {
            return executor.submit(Callable<Int> {
                errorCount
            }).get()
        }

        fun resetErrorCount(): Int {
            return executor.submit(Callable<Int> {
                val ret = errorCount
                errorCount = 0
                return@Callable ret
            }).get()
        }

        fun flush() {
            executor.submit {
                flushall()
            }.get()
        }

        fun log(msg: String, e: Throwable? = null, timestamp: Boolean, error: Boolean = false) {
            val time = if (timestamp) System.currentTimeMillis() else null
            executor.submit {
                if (error) {
                    ++errorCount
                    flushall()
                    log1(err, msg, e, time, null)
                    err.flush()
                } else {
                    flushall()
                    log1(out, msg, e, time, null)
                    out.flush()
                }
            }
        }

        fun e() {
            executor.submit {
                ++errorCount
            }
        }

        fun enter(name: String?, msg: String?) {
            val time = System.currentTimeMillis()
            executor.submit {
                enter1(name, msg, time)
            }
        }

        fun leave(msg: String?) {
            val time = System.currentTimeMillis()
            executor.submit {
                leave1(msg, time)
            }
        }

        @Throws(IllegalStateException::class)
        fun leaveX(msg: String?) {
            val time = System.currentTimeMillis()
            try {
                executor.submit {
                    if (errorCount > 0) {
                        val info = callStack.peek()
                        leave1(msg, time)
                        throw java.lang.IllegalStateException(info.name)
                    } else {
                        leave1(msg, time)
                    }
                }.get()
            } catch (e: ExecutionException) {
                throw e.cause ?: e
            }
        }

        fun quiet(code: Fun00) {
            executor.submit {
                quietStack.push(quiet)
                quiet = true
            }
            try {
                code()
            } finally {
                executor.submit {
                    quiet = quietStack.pop()
                }
            }
        }

        @Throws(IOException::class)
        fun saveLog(file: File): Future<*> {
            return executor.submit {
                file.mkparentOrFail()
                // Make sure there is a terminating line break.
                if (logs.last().isNotEmpty()) {
                    logs.add("")
                }
                file.writeText(logs.joinln())
            }
        }

        fun getLog(): List<String> {
            return executor.submit(Callable {
                ArrayList(logs)
            }).get()
        }

        @Throws
        fun addLifecycleListener(listener: ILifecycleListener) {
            executor.submit {
                listeners.add(listener)
            }
        }

        @Throws
        fun removeLifecycleListener(listener: ILifecycleListener) {
            executor.submit {
                listeners.remove(listener)
            }
        }

        ////////////////////////////////////////////////////////////////////////

        private fun flushall() {
            out.flush()
            err.flush()
        }

        private fun log1(out: PrintStream, msg: String, e: Throwable? = null, start: Long?, end: Long?) {
            if (quiet) {
                return
            }
            val s = when {
                start != null && end != null ->
                    "$prefix ${IStepWatch.fmt((end - start) / 1000f)}/${IStepWatch.fmt((end - startTime) / 1000f)} s: $msg"
                start != null ->
                    "$prefixTimestamp${IStepWatch.fmt((start - startTime) / 1000f)} s: $msg"
                end != null ->
                    "$prefixEnter${IStepWatch.fmt((end - startTime) / 1000f)} s: $msg"
                else -> msg
            }
            if (e == null) {
                smartlog(out, s)
            } else {
                val w = StringPrintWriter()
                if (s.endsWith(TextUt.LB)) {
                    w.print(s)
                } else if (s.isNotEmpty()) {
                    w.println(s)
                }
                e.printStackTrace(w)
                val str = w.toString()
                out.print(str)
                logs.add(str)
            }
        }

        private fun smartlog(out: PrintStream, msg: String) {
            if (msg.endsWith(TextUt.LB)) {
                out.print(msg)
                logs.add(msg)
            } else if (!msg.isEmpty()) {
                out.println(msg)
                logs.add(msg)
                logs.add(TextUt.LB)
            }
        }

        private fun enter1(name: String?, msg: String?, time: Long) {
            callStack.push(Info(name, errorCount, time))
            if (debugging && callStack.size == 1) {
                listeners.forEach {
                    it.onStart(name ?: "", startTime) { s ->
                        log1(out, s, null, null, time)
                    }
                }
            }
            errorCount = 0
            if (debugging && name != null) {
                val b = StringBuilder()
                for (i in 0 until callStack.size) {
                    b.append('+')
                }
                if (!b.isEmpty()) b.append(' ')
                b.append(name)
                if (msg != null) b.append(": $msg")
                log1(out, b.toString(), null, null, time)
            }
        }

        private fun leave1(msg: String?, time: Long) {
            val info = callStack.pop()
            errorCount += info.errorCount
            if (debugging && info.name != null) {
                val b = StringBuilder()
                for (i in 0..callStack.size) {
                    b.append('-')
                }
                if (!b.isEmpty()) b.append(' ')
                b.append(info.name)
                if (msg != null) b.append(": $msg")
                log1(out, b.toString(), null, info.startTime, time)
            }
            if (callStack.isEmpty()) {
                listeners.forEach {
                    it.onDone(info.name ?: "", time, errorCount) { s ->
                        log1(out, s, null, null, time)
                    }
                }
            }
        }
    }

////////////////////////////////////////////////////////////////////////

    private val delegate = Delegate(debugging, startTime, prefix, out, err)

    /** @return The errorCount. Note that this method is synchronous. */
    override val errorCount: Int
        get() = delegate.getErrorCount()

////////////////////////////////////////////////////////////////////////

    override fun resetErrorCount(): Int {
        return delegate.resetErrorCount()
    }

    override fun flush() {
        delegate.flush()
    }

    override fun quiet(code: Fun00) {
        delegate.quiet(code)
    }

    @Throws(IOException::class)
    override fun saveLog(file: File) {
        delegate.saveLog(file)
    }

    /**
     * @ return The log entries.
     * Note that this method is synchronous.
     */

    override fun getLog(): List<String> {
        return delegate.getLog()
    }

    fun addLifecycleListener(listener: ILifecycleListener) {
        delegate.addLifecycleListener(listener)
    }

    fun removeLifecycleListener(listener: ILifecycleListener) {
        delegate.removeLifecycleListener(listener)
    }

////////////////////////////////////////////////////////////////////////

    override fun d(msg: String, e: Throwable?) {
        if (debugging) {
            delegate.log(msg, e, false)
        }
    }

    override fun i(msg: String, e: Throwable?) {
        val ex = if (debugging) e else null
        delegate.log(msg, ex, false)
    }

    override fun w(msg: String, e: Throwable?) {
        delegate.log(msg, e, false)
    }

    override fun e(msg: String, e: Throwable?) {
        delegate.log(msg, e, false, error = true)
    }

    override fun e() {
        delegate.e()
    }

    override fun d(any: Any) {
        d(any.toString())
    }

    override fun dd(msg: String) {
        if (debugging) {
            delegate.log(msg, null, true, false)
        }
    }

    override fun ii(msg: String) {
        delegate.log(msg, null, true, false)
    }

////////////////////////////////////////////////////////////////////////

    override fun d(msgs: Iterator<String>) {
        if (debugging && msgs.hasNext()) {
            delegate.log(msgs.join(TextUt.LB), null, false)
        }
    }

    override fun i(msgs: Iterator<String>) {
        if (msgs.hasNext()) {
            delegate.log(msgs.join(TextUt.LB), null, false)
        }
    }

    override fun w(msgs: Iterator<String>) {
        if (msgs.hasNext()) {
            delegate.log(msgs.join(TextUt.LB), null, false)
        }
    }

    override fun e(msgs: Iterator<String>) {
        if (msgs.hasNext()) {
            delegate.log(msgs.join(TextUt.LB), null, false, true)
        }
    }

////////////////////////////////////////////////////////////////////////

    override fun dfmt(format: String, vararg args: Any) {
        if (debugging) {
            d(String.format(format, *args))
        }
    }

    override fun ifmt(format: String, vararg args: Any) {
        i(String.format(format, *args))
    }

    override fun wfmt(format: String, vararg args: Any) {
        w(String.format(format, *args))
    }

    override fun efmt(format: String, vararg args: Any) {
        e(String.format(format, *args))
    }

    override fun d(msgs: Iterable<String>) {
        d(msgs.iterator())
    }

    override fun i(msgs: Iterable<String>) {
        i(msgs.iterator())
    }

    override fun w(msgs: Iterable<String>) {
        w(msgs.iterator())
    }

    override fun e(msgs: Iterable<String>) {
        e(msgs.iterator())
    }

    /**
     * Print multi-line debug messages without timestamp
     */
    override fun d(vararg msgs: String) {
        d(msgs.iterator())
    }

    /**
     * Print multi-line info messages without timestamp
     */
    override fun i(vararg msgs: String) {
        i(msgs.iterator())
    }

    /**
     * Print multi-line warn messages without timestamp
     */
    override fun w(vararg msgs: String) {
        w(msgs.iterator())
    }

    /**
     * Print multi-line error messages without timestamp
     */
    override fun e(vararg msgs: String) {
        e(msgs.iterator())
    }

    ////////////////////////////////////////////////////////////////////////

    override fun enter(name: String?, msg: String?) {
        delegate.enter(name, msg)
    }

    override fun enter(name: String?, msg: String?, code: Fun00) {
        delegate.enter(name, msg)
        try {
            code()
        } finally {
            delegate.leave(msg)
        }
    }

    override fun <T> enter(name: String?, msg: String?, code: Fun01<T>): T {
        delegate.enter(name, msg)
        try {
            return code()
        } finally {
            delegate.leave(msg)
        }
    }

    @Throws(IllegalStateException::class)
    override fun enterX(name: String?, msg: String?, code: Fun00) {
        delegate.enter(name, msg)
        try {
            code()
        } catch (e: Throwable) {
            this.e(name ?: "", e)
        } finally {
            leaveX(msg)
        }
    }

    @Throws(Exception::class)
    override fun <T> enterX(name: String?, msg: String?, code: Fun01<T>): T? {
        delegate.enter(name, msg)
        return try {
            code()
        } catch (e: Throwable) {
            this.e(name ?: "", e)
            null
        } finally {
            leaveX(msg)
        }
    }

    override fun leave(msg: String?) {
        delegate.leave(msg)
    }

    @Throws(IllegalStateException::class)
    override fun leaveX(msg: String?) {
        delegate.leaveX(msg)
    }

////////////////////////////////////////////////////////////////////////
}
