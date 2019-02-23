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
import java.util.*

open class DateUtil {

    companion object {
        val DateUt = DateUtil()
    }

    val SECOND: Long = 1000
    val MINUTE = 60 * SECOND
    val HOUR = 60 * MINUTE
    val DAY = 24 * HOUR

    /**
     * @return The simple date string in form YYYYMMDD.
     */
    val today get() = dateString(Date())

    /**
     * @return The simple datetime string in form YYYYMMDD-hhmmss.
     */
    val now get() = datetimeString(Date())

    /**
     * @return Simple date string in form YYYYMMDD.
     */
    fun dateString(date: Date): String {
        return TextUt.format("%1\$tY%1\$tm%1\$td", date)
    }

    /**
     * @return Simple date string in form YYYYMMDD.
     */
    fun dateString(date: Long): String {
        return TextUt.format("%1\$tY%1\$tm%1\$td", date)
    }

    /**
     * @return Simple time string in form hhmmss
     */
    fun timeString(date: Date): String {
        return TextUt.format("%1\$tH%1\$tM%1\$tS", date)
    }

    /**
     * @return Simple time string in form hhmmss
     */
    fun timeString(date: Long): String {
        return TextUt.format("%1\$tH%1\$tM%1\$tS", date)
    }

    /**
     * @return Simple date time string in form YYYYMMDD-hhmmss
     */
    fun datetimeString(date: Date): String {
        return TextUt.format("%1\$tY%1\$tm%1\$td-%1\$tH%1\$tM%1\$tS", date)
    }

    /**
     * @return Simple date time string in form YYYYMMDD-hhmmss
     */
    fun datetimeString(date: Long): String {
        return TextUt.format("%1\$tY%1\$tm%1\$td-%1\$tH%1\$tM%1\$tS", date)
    }
}
