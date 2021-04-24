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

import com.cplusedition.bot.core.IStepWatch.Companion.fmt
import java.util.*

interface IStepWatch {

    fun restart(): IStepWatch
    fun pause(): IStepWatch
    fun resume(): IStepWatch

    /** Total time elapsed in sec. */
    fun elapsedSec(): Float

    /** Total time elapsed in ms. */
    fun elapsedMs(): Long

    /** Delta time since last delta elapsed in sec. */
    fun deltaSec(): Float

    /** Delta time since last delta elapsed in ms. */
    fun deltaMs(): Long

    fun toString(msg: String): String
    fun toString(msg: String, count: Int, unit: String): String
    fun toString(msg: String, count: Long, unit: String): String
    fun toString(msg: String, count: Float, unit: String): String
    fun toStringf(format: String, vararg args: Any): String

    companion object {
        fun fmt(value: Float): String {
            return if (value >= 1000) TextUt.format("%6d", value.toInt()) else TextUt.format("%6.2f", value)
        }
    }
}

open class StepWatch : IStepWatch {

    ////////////////////////////////////////////////////////////////////////

    private var startTime: Long = System.currentTimeMillis()
    private var elapsed: Long = 0
    private var stepStartTime: Long = 0

    // Instance methods ////////////////////////////////////////////////////

    override fun restart(): StepWatch {
        startTime = System.currentTimeMillis()
        elapsed = 0
        stepStartTime = 0
        return this
    }

    override fun pause(): StepWatch {
        if (startTime < 0) {
            throw IllegalStateException()
        }
        elapsed += System.currentTimeMillis() - startTime
        startTime = -1
        return this
    }

    override fun resume(): StepWatch {
        startTime = System.currentTimeMillis()
        return this
    }

    /**
     * @return Time elapsed since start() in sec.
     */
    override fun elapsedSec(): Float {
        return elapsedMs() / 1000f
    }

    /**
     * @return Time elapsed since start() in ms.
     */
    override fun elapsedMs(): Long {
        return if (startTime < 0) elapsed else elapsed + (System.currentTimeMillis() - startTime)
    }

    override fun deltaSec(): Float {
        val e = elapsedMs()
        val delta = e - stepStartTime
        stepStartTime = e
        return delta / 1000f
    }

    override fun deltaMs(): Long {
        val e = elapsedMs()
        val time = e - stepStartTime
        stepStartTime = e
        return time
    }

    override fun toStringf(format: String, vararg args: Any): String {
        return toString(String.format(Locale.US, format, *args))
    }

    override fun toString(): String {
        val delta = deltaSec()
        return "${fmt(delta)}/${fmt(stepStartTime / 1000f)} s"
    }

    override fun toString(msg: String): String {
        val delta = deltaSec()
        return "${fmt(delta)}/${fmt(stepStartTime / 1000f)} s: $msg"
    }

    override fun toString(msg: String, count: Int, unit: String): String {
        return toString(msg, count.toLong(), unit)
    }

    override fun toString(msg: String, count: Long, unit: String): String {
        val delta = deltaSec()
        val rate = rate(count.toFloat(), delta)
        return String.format(
                "%s/%s s %10d $unit %10.2f $unit/s: %s",
                fmt(delta),
                fmt(stepStartTime / 1000f),
                count,
                rate,
                msg
        )
    }

    override fun toString(msg: String, count: Float, unit: String): String {
        val delta = deltaSec()
        val rate = rate(count, delta)
        return String.format(
                "%s/%s s %10.2f $unit %10.2f $unit/s: %s", fmt(delta), fmt(stepStartTime / 1000f), count, rate, msg
        )
    }

    companion object {

        const val TIMER_RESOLUTION = 1e-6f

        fun rate(count: Float, time: Float): Float {
            val elapsed = if (time < TIMER_RESOLUTION) TIMER_RESOLUTION else time
            return count / elapsed
        }
    }
}