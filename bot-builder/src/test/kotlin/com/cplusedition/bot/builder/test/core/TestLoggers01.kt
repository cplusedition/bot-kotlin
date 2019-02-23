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

package com.cplusedition.bot.builder.test.core

import com.cplusedition.bot.builder.test.zzz.TestBase
import com.cplusedition.bot.core.ILog
import com.cplusedition.bot.core.PrintStreamLogger
import com.cplusedition.bot.core.StringLogger
import com.cplusedition.bot.core.SystemLogger
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class TestLoggers01 : TestBase() {

    @Test
    fun testSystemLogger01() {
        fun check(logger: ILog) {
            logger.d("debug")
            logger.i("info")
            logger.w("warn")
            logger.e("error")
            logger.d("debug", Throwable("throwable_d"))
            logger.i("info", Throwable("throwable_i"))
            logger.w("warn", Throwable("throwable_w"))
            logger.e("error", Throwable("throwable_e"))
        }
        log.enterX {
            subtest {
                assertTrue(SystemLogger is ILog)
            }
            subtest {
                val out = ByteArrayOutputStream()
                val err = ByteArrayOutputStream()
                val logger = PrintStreamLogger(out = PrintStream(out), err = PrintStream(err))
                check(logger)
                val output = out.toString()
                val error = err.toString()
                assertTrue(output.contains("debug"))
                assertTrue(output.contains("info"))
                assertTrue(output.contains("warn"))
                assertFalse(output.contains("error"))
                assertTrue(output.contains("throwable_d"))
                assertTrue(output.contains("throwable_i"))
                assertTrue(output.contains("throwable_w"))
                assertFalse(output.contains("throwable_e"))
                //
                assertFalse(error.contains("debug"))
                assertFalse(error.contains("info"))
                assertFalse(error.contains("warn"))
                assertTrue(error.contains("error"))
                assertFalse(error.contains("throwable_d"))
                assertFalse(error.contains("throwable_i"))
                assertFalse(error.contains("throwable_w"))
                assertTrue(error.contains("throwable_e"))
            }
            subtest {
                val out = ByteArrayOutputStream()
                val err = ByteArrayOutputStream()
                val logger = PrintStreamLogger(true, PrintStream(out), PrintStream(err))
                check(logger)
                val output = out.toString()
                val error = err.toString()
                assertTrue(output.contains("debug"))
                assertTrue(output.contains("info"))
                assertTrue(output.contains("warn"))
                assertFalse(output.contains("error"))
                assertTrue(output.contains("throwable_d"))
                assertTrue(output.contains("throwable_i"))
                assertTrue(output.contains("throwable_w"))
                assertFalse(output.contains("throwable_e"))
                //
                assertFalse(error.contains("debug"))
                assertFalse(error.contains("info"))
                assertFalse(error.contains("warn"))
                assertTrue(error.contains("error"))
                assertFalse(error.contains("throwable_d"))
                assertFalse(error.contains("throwable_i"))
                assertFalse(error.contains("throwable_w"))
                assertTrue(error.contains("throwable_e"))
            }
            subtest {
                val out = ByteArrayOutputStream()
                val err = ByteArrayOutputStream()
                val logger = PrintStreamLogger(false, PrintStream(out), PrintStream(err))
                check(logger)
                val output = out.toString()
                val error = err.toString()
                assertFalse(output.contains("debug"))
                assertTrue(output.contains("info"))
                assertTrue(output.contains("warn"))
                assertFalse(output.contains("error"))
                assertFalse(output.contains("throwable_d"))
                assertFalse(output.contains("throwable_i"))
                assertFalse(output.contains("throwable_w"))
                assertFalse(output.contains("throwable_e"))
                //
                assertFalse(error.contains("debug"))
                assertFalse(error.contains("info"))
                assertFalse(error.contains("warn"))
                assertTrue(error.contains("error"))
                assertFalse(error.contains("throwable_d"))
                assertFalse(error.contains("throwable_i"))
                assertFalse(error.contains("throwable_w"))
                assertTrue(error.contains("throwable_e"))
            }
            subtest {
                val logger = StringLogger()
                check(logger)
                val output = logger.toString()
                assertTrue(output.contains("debug"))
                assertTrue(output.contains("info"))
                assertTrue(output.contains("warn"))
                assertTrue(output.contains("error"))
                assertTrue(output.contains("throwable_d"))
                assertTrue(output.contains("throwable_i"))
                assertTrue(output.contains("throwable_w"))
                assertTrue(output.contains("throwable_e"))
            }
            subtest {
                val logger = StringLogger(true)
                check(logger)
                val output = logger.toString()
                assertTrue(output.contains("debug"))
                assertTrue(output.contains("info"))
                assertTrue(output.contains("warn"))
                assertTrue(output.contains("error"))
                assertTrue(output.contains("throwable_d"))
                assertTrue(output.contains("throwable_i"))
                assertTrue(output.contains("throwable_w"))
                assertTrue(output.contains("throwable_e"))
            }
            subtest {
                val logger = StringLogger(false)
                check(logger)
                val output = logger.toString()
                assertFalse(output.contains("debug"))
                assertTrue(output.contains("info"))
                assertTrue(output.contains("warn"))
                assertTrue(output.contains("error"))
                assertFalse(output.contains("throwable_d"))
                assertFalse(output.contains("throwable_i"))
                assertFalse(output.contains("throwable_w"))
                assertTrue(output.contains("throwable_e"))
            }
        }
    }
}