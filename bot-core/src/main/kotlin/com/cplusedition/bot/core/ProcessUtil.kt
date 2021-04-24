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

import java.io.*
import java.util.concurrent.*

object ProcessUt : ProcessUtil()

open class ProcessUtil {

    fun sleep(ms: Long) {
        try {
            Thread.sleep(ms)
        } catch (e: Throwable) {
            throw AssertionError(e)
        }
    }

    fun exec(vararg cmdline: String): Process? {
        return exec(FileUt.pwd(), null, *cmdline)
    }

    fun exec(workdir: File, vararg cmdline: String): Process? {
        return exec(workdir, null, *cmdline)
    }

    fun exec(workdir: File, env: Array<out String>?, vararg cmdline: String): Process? {
        return try {
            Runtime.getRuntime().exec(cmdline, env, workdir)
        } catch (e: Throwable) {
            null
        }
    }

    @Throws(Exception::class)
    fun backtick(cmd: String, args: List<String>): String {
        return backtick(FileUt.pwd(), cmd, args)
    }

    @Throws(Exception::class)
    fun backtick(workdir: File, cmd: String, args: List<String>): String {
        return backtick(workdir, cmd, *args.toTypedArray())
    }

    @Throws(Exception::class)
    fun backtick(cmd: String, vararg args: String): String {
        return backtick(FileUt.pwd(), cmd, *args)
    }

    @Throws(Exception::class)
    fun backtick(workdir: File, cmd: String, vararg args: String): String {
        val out = ByteArrayOutputStream()
        val err = ByteArrayOutputStream()
        return ProcessUtBuilder(workdir, cmd, *args)
                .out(out)
                .err(err)
                .async { process ->
                    val rc = process.exitValue()
                    if (rc != 0) {
                        val outs = out.toString("UTF-8")
                        val errs = err.toString("UTF-8")
                        throw IOException("# ERROR: rc=$rc\n$outs\n$errs")
                    }
                    out.toString("UTF-8")
                }.get()
    }

    @Throws(Exception::class)
    fun backtick(out: OutputStream, workdir: File, cmd: String, args: List<String>): Int {
        return backtick(out, workdir, cmd, *args.toTypedArray())
    }

    @Throws(Exception::class)
    fun backtick(out: OutputStream, workdir: File, cmd: String, vararg args: String): Int {
        val err = ByteArrayOutputStream()
        return ProcessUtBuilder(workdir, cmd, *args)
                .out(out)
                .err(err)
                .async { process ->
                    val rc = process.exitValue()
                    if (rc != 0) {
                        val errs = err.toString("UTF-8")
                        throw IOException("# ERROR: rc=$rc\n$errs")
                    }
                    rc
                }.get()
    }
}

class ProcessUtBuilder(
        private var workdir: File,
        private val cmd: String,
        private vararg val args: String
) {
    private var env: Array<out String>? = null
    private var timeout = DateUt.DAY
    private var timeunit = TimeUnit.MILLISECONDS
    private var out: OutputStream? = null
    private var err: OutputStream? = null
    private var input: InputStream? = null

    constructor(cmd: String, vararg args: String) : this(FileUt.pwd(), cmd, *args)

    companion object {
        private val pool = Executors.newCachedThreadPool()
    }

    fun workdir(dir: File): ProcessUtBuilder {
        this.workdir = dir
        return this
    }

    fun env(vararg env: String): ProcessUtBuilder {
        this.env = env
        return this
    }

    fun timeout(value: Long, unit: TimeUnit = TimeUnit.MILLISECONDS): ProcessUtBuilder {
        this.timeout = value
        this.timeunit = unit
        return this
    }

    fun out(out: OutputStream): ProcessUtBuilder {
        this.out = out
        return this
    }

    fun err(err: OutputStream): ProcessUtBuilder {
        this.err = err
        return this
    }

    fun input(input: InputStream): ProcessUtBuilder {
        this.input = input
        return this
    }

    private fun okOrFail(rc: Int, out: ByteArrayOutputStream, err: ByteArrayOutputStream) {
        if (rc == 0) return
        val w = StringPrintWriter()
        w.println("# rc=$rc")
        val outs = out.toByteArray().inputStream().reader().readText()
        if (outs.isNotEmpty()) w.println(outs)
        val errs = err.toByteArray().inputStream().reader().readText()
        if (errs.isNotEmpty()) w.println(errs)
        throw AssertionError(w.toString())
    }

    fun asyncOrFail(): Future<Unit> {
        val out = ByteArrayOutputStream()
        val err = ByteArrayOutputStream()
        this.out = out
        this.err = err
        return async { process ->
            okOrFail(process.exitValue(), out, err)
        }
    }

    fun <V, O : OutputStream> asyncOrFail(out: O, callback: Fun11<O, V>): Future<V> {
        this.out = out
        return async { process ->
            if (process.exitValue() != 0) throw AssertionError("${process.exitValue()}")
            callback(out)
        }
    }

    fun <V, O : OutputStream, E : OutputStream> asyncOrFail(out: O, err: E, callback: Fun21<O, E, V>): Future<V> {
        this.out = out
        this.err = err
        return async { process ->
            if (process.exitValue() != 0) throw AssertionError("${process.exitValue()}")
            callback(out, err)
        }
    }

    fun async(): Future<Int> {
        return async { process ->
            process.exitValue()
        }
    }

    fun <V> async(callback: Fun11<Process, V>): Future<V> {
        return pool.submit(Callable {
            val cmdline = arrayOf(cmd, *args)
            val process = Runtime.getRuntime().exec(cmdline, env, workdir)
            try {
                val outmon = pool.submit {
                    process.inputStream.use {
                        FileUt.copyByteWise(out ?: NullOutputStream(), it)
                    }
                }
                val errmon = pool.submit {
                    process.errorStream.use {
                        FileUt.copyByteWise(err ?: NullOutputStream(), it)
                    }
                }
                this.input?.let { input ->
                    process.outputStream.use {
                        FileUt.copy(DEFAULT_BUFFER_SIZE, it, input)
                    }
                }
                if (!process.waitFor(timeout, timeunit)) {
                    throw TimeoutException()
                }
                outmon.get(timeout, timeunit)
                errmon.get(timeout, timeunit)
                return@Callable callback(process)
            } catch (e: Throwable) {
                try {
                    process.destroyForcibly().waitFor()
                } catch (_: Throwable) {
                    // Ignore
                }
                throw e
            }
        })
    }
}

class NullOutputStream : OutputStream() {
    override fun write(b: Int) {
    }

    override fun write(b: ByteArray) {
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
    }
}

