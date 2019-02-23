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

import com.cplusedition.bot.core.DateUtil.Companion.DateUt
import com.cplusedition.bot.core.FileUtil.Companion.FileUt
import com.cplusedition.bot.core.TextUtil.Companion.TextUt
import java.io.File
import java.io.IOException
import java.util.concurrent.*

open class ProcessUtil {

    companion object {
        val ProcessUt = ProcessUtil()
        private val pool = Executors.newCachedThreadPool()

    }

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

    fun async(task: Fun00): Future<*> {
        return pool.submit(task)
    }

    fun <V> async(task: Callable<V>): Future<V> {
        return pool.submit(task)
    }

    fun async(
        vararg cmdline: String
    ): Future<Process> {
        return async(FileUt.pwd(), cmdline, DateUt.DAY, TimeUnit.MILLISECONDS) { it }
    }

    fun async(
        workdir: File,
        vararg cmdline: String
    ): Future<Process> {
        return async(workdir, cmdline, DateUt.DAY, TimeUnit.MILLISECONDS) { it }
    }

    fun async(
        workdir: File,
        cmdline: Array<out String>,
        timeout: Long = DateUt.DAY,
        timeunit: TimeUnit = TimeUnit.MILLISECONDS
    ): Future<Process> {
        return async(workdir, cmdline, null, timeout, timeunit) { it }
    }

    fun <V> async(
        vararg cmdline: String,
        callback: Fun11<Process, V>
    ): Future<V> {
        return async(FileUt.pwd(), cmdline, DateUt.DAY, TimeUnit.MILLISECONDS, callback)
    }

    fun <V> async(
        workdir: File,
        vararg cmdline: String,
        callback: Fun11<Process, V>
    ): Future<V> {
        return async(workdir, cmdline, DateUt.DAY, TimeUnit.MILLISECONDS, callback)
    }

    fun <V> async(
        workdir: File,
        cmdline: Array<out String>,
        timeout: Long = DateUt.DAY,
        timeunit: TimeUnit = TimeUnit.MILLISECONDS,
        callback: Fun11<Process, V>
    ): Future<V> {
        return async(workdir, cmdline, null, timeout, timeunit, callback)
    }

    /**
     * @param callback(process): V Invoke when process is completed without error.
     * @return Future<V> where V is return type of the callback.
     */
    fun <V> async(
        workdir: File,
        cmdline: Array<out String>,
        env: Array<out String>?,
        timeout: Long = DateUt.DAY,
        timeunit: TimeUnit = TimeUnit.MILLISECONDS,
        callback: Fun11<Process, V>
    ): Future<V> {
        return pool.submit(Callable {
            val process = Runtime.getRuntime().exec(cmdline, env, workdir)
            try {
                if (!process.waitFor(timeout, timeunit)) {
                    throw TimeoutException()
                }
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

    @Throws(Exception::class)
    fun backtick(cmdline: List<String>): String? {
        return backtick(FileUt.pwd(), cmdline)
    }

    @Throws(Exception::class)
    fun backtick(workdir: File, cmdline: List<String>): String {
        return backtick(workdir, *cmdline.toTypedArray())
    }

    @Throws(Exception::class)
    fun backtick(vararg cmdline: String): String? {
        return backtick(FileUt.pwd(), *cmdline)
    }

    @Throws(Exception::class)
    fun backtick(workdir: File, vararg cmdline: String): String {
        return async(workdir, cmdline) { process ->
            val rc = process.exitValue()
            if (rc != 0) {
                val s = FileUt.asString(process.errorStream)
                val err = if (s.isEmpty()) s else "\n$s"
                throw IOException("# ERROR: rc=$rc$err")
            }
            return@async FileUt.asString(process.inputStream)
        }.get()
    }
}
