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

package com.cplusedition.bot.builder.test.builder

import com.cplusedition.bot.builder.BuilderUtil.Companion.BU
import com.cplusedition.bot.builder.test.zzz.TestBase
import com.cplusedition.bot.core.WithUtil.Companion.With
import com.cplusedition.bot.core.file
import org.junit.Assert.assertEquals
import org.junit.Test

class TestBuilderUt01 : TestBase() {

    @Test
    fun testBuilderUt01() {
        subtest {
            With.exceptionOrFail { BU.fail() }
            With.exceptionOrFail { BU.fail(TestBuilderUt01::class) }
            With.exceptionOrFail { BU.fail(this::testBuilderUt01) }
            With.exceptionOrFail { BU.fail(TestBuilderUt01::class.java) }
        }
        subtest {
            val file = testResDir.file("html/manual.html")
            assertEquals(19072, file.length())
            assertEquals("19 kB", BU.filesizeString(file))
            assertEquals("19 kB", BU.filesizeString(file.length()))
        }
    }
}