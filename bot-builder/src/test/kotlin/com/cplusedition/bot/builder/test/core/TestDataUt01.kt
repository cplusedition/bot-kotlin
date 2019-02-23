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
import com.cplusedition.bot.core.DateUtil.Companion.DateUt
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.*

class TestDataUt01 : TestBase() {

    @Test
    fun testBasic01() {
        val cal = Calendar.getInstance()
        cal.set(1997, 11, 1, 13, 23, 45)
        val date = cal.time
        assertEquals("19971201", DateUt.dateString(date))
        assertEquals("19971201", DateUt.dateString(date.time))
        assertEquals("132345", DateUt.timeString(date))
        assertEquals("132345", DateUt.timeString(date.time))
        assertEquals("19971201-132345", DateUt.datetimeString(date))
        assertEquals("19971201-132345", DateUt.datetimeString(date.time))
        assertEquals(8, DateUt.today.length)
        assertEquals(15, DateUt.now.length)
    }
}