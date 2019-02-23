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
import com.cplusedition.bot.core.ProcessUtil.Companion.ProcessUt
import com.cplusedition.bot.core.StepWatch
import com.cplusedition.bot.core.WithUtil.Companion.With
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class TestStepWatch01 : TestBase() {

    @Test
    fun test01() {
        subtest {
            val timer = StepWatch()
            if (true) {
                ProcessUt.sleep(100)
                val deltams = timer.deltaMs()
                assertTrue("$deltams", deltams in 99..199)
            }
            if (true) {
                ProcessUt.sleep(100)
                val deltasec = timer.deltaSec()
                assertTrue("$deltasec", deltasec < 0.2f && deltasec >= 0.099f)
                val elapsedms = timer.elapsedMs()
                val elapsedsec = timer.elapsedSec()
                assertTrue(
                    "$elapsedms, $elapsedsec",
                    elapsedms in 199..299 && elapsedsec >= 0.199f && elapsedsec < 0.3f
                )
            }
        }
    }

    @Test
    fun test02() {
        subtest {
            val timer = StepWatch()
            if (true) {
                ProcessUt.sleep(200)
                val deltams = timer.deltaMs()
                assertTrue("$deltams", deltams in 199..299)
            }
            if (true) {
                ProcessUt.sleep(200)
                val deltasec = timer.deltaSec()
                assertTrue("$deltasec", deltasec < 0.3f && deltasec >= 0.199f)
                val elapsedms = timer.elapsedMs()
                val elapsedsec = timer.elapsedSec()
                assertTrue(
                    "$elapsedms, $elapsedsec",
                    elapsedms in 399..499 && elapsedsec >= 0.399f && elapsedsec < 0.5f
                )
            }
            if (true) {
                val delta = timer.deltaMs()
                assertTrue("$delta", delta in 0..99)
            }
            if (true) {
                timer.pause()
                ProcessUt.sleep(200)
                val delta = timer.deltaMs()
                assertTrue("$delta", delta in 0..99)
                val elapsedms = timer.elapsedMs()
                val elapsedsec = timer.elapsedSec()
                assertTrue(
                    "$elapsedms, $elapsedsec",
                    elapsedms in 399..499 && elapsedsec >= 0.399f && elapsedsec < 0.5f
                )
            }
            if (true) {
                assertTrue(With.exception { timer.pause() } is IllegalStateException)
            }
            if (true) {
                timer.resume()
                ProcessUt.sleep(200)
                val delta = timer.deltaMs()
                assertTrue("$delta", delta in 199..299)
                val elapsedms = timer.elapsedMs()
                val elapsedsec = timer.elapsedSec()
                assertTrue(
                    "$elapsedms, $elapsedsec",
                    elapsedms in 599..699 && elapsedsec >= 0.599f && elapsedsec < 0.7f
                )
            }
            if (true) {
                timer.restart()
                ProcessUt.sleep(200)
                val delta = timer.deltaMs()
                assertTrue("$delta", delta in 199..299)
                val elapsedms = timer.elapsedMs()
                val elapsedsec = timer.elapsedSec()
                assertTrue(
                    "$elapsedms, $elapsedsec",
                    elapsedms in 199..299 && elapsedsec >= 0.199f && elapsedsec < 0.3f
                )
            }
        }
    }

    @Test
    fun testToString01() {
        fun check(s: String, deltamin: Double, deltamax: Double, elapsedmin: Double, elapsedmax: Double) {
            val m = Regex("^\\s*([\\d.]+)/\\s*([\\d.]+).*").matchEntire(s)
            if (m == null) fail()
            else {
                val n1 = m.groupValues[1].toDouble()
                val n2 = m.groupValues[2].toDouble()
                assertTrue("$s: $n1", n1 >= deltamin && n1 < deltamax)
                assertTrue("$s: $n2", n2 >= elapsedmin && n2 < elapsedmax)
            }
        }

        fun checkrate(s: String, min: Double, max: Double) {
            val m = Regex(".*\\s+([\\d.]+)\\s+count/s.*").matchEntire(s)
            if (m == null) fail()
            else {
                val rate = m.groupValues[1].toDouble()
                assertTrue("$s: $rate", rate >= min && rate < max)
            }
        }
        subtest {
            val timer = StepWatch()
            if (true) {
                ProcessUt.sleep(200)
                val s = timer.toString()
                log.d("#1: $s")
                check(s, 0.199, 0.3, 0.199, 0.3)
            }
            if (true) {
                ProcessUt.sleep(200)
                val s = timer.toString("testing123")
                log.d("#2: $s")
                assertTrue(s, s.endsWith("testing123"))
                check(s, 0.199, 0.3, 0.399, 0.5)
            }
            if (true) {
                val msg = "rate:float"
                val s = timer.toString(msg, 100f, "count")
                log.d("#3: $s")
                checkrate(s, 5000.0, 2e8) // max ~100/1e-6
                assertTrue(s, s.contains("count/s"))
                assertTrue(s, s.contains(msg))
            }
            if (true) {
                timer.pause()
                ProcessUt.sleep(200)
                val msg = "rate:long"
                val s = timer.toString(msg, 100, "count")
                log.d("#4: $s")
                checkrate(s, 5000.0, 2e8) // max ~100/1e-6
                assertTrue(s, s.contains("count/s"))
                assertTrue(s, s.contains(msg))
            }
            if (true) {
                timer.resume()
                ProcessUt.sleep(200)
                val s = timer.toStringf("format: %d of %d", timer.deltaMs(), timer.elapsedMs())
                log.d("#5: $s")
                assertTrue(s, s.contains("format: 2"))
                assertTrue(s, s.contains("of 6"))
            }
            if (true) {
                val msg = "rate:float"
                ProcessUt.sleep(200)
                val s = timer.toString(msg, 100f, "count")
                log.d("#6: $s")
                checkrate(s, 250.0, 750.0) // actual ~500
                assertTrue(s, s.contains("count/s"))
                assertTrue(s, s.contains(msg))
            }
            if (true) {
                ProcessUt.sleep(200)
                val msg = "rate:long"
                val s = timer.toString(msg, 100, "count")
                log.d("#7: $s")
                checkrate(s, 250.0, 750.0) // actual ~500
                assertTrue(s, s.contains("count/s"))
                assertTrue(s, s.contains(msg))
            }
        }
    }
}