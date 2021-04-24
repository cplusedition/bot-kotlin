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
import com.cplusedition.bot.core.Fun11
import com.cplusedition.bot.core.XMLUt
import com.cplusedition.bot.core.elements
import com.cplusedition.bot.core.file
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

class TestXMLUt01 : TestBase() {

    @Test
    fun testTextContext01() {
        fun fixture(code: Fun11<Element, Boolean>): Boolean {
            val doc = XMLUt.parse(testResDir.file("html/manual.html"))
            for (e in doc.getElementsByTagName("div").elements()) {
                val name = e.getAttribute("name")
                if (name != null && name == "x-annotation") {
                    if (code(e)) {
                        return true
                    }
                }
            }
            return false
        }
        subtest {
            assertTrue(fixture {
                "View PDF" == XMLUt.textContent1(it, "span")
            })
        }
        subtest {
            assertFalse(fixture {
                "View PDF" == XMLUt.textContent1(it, "audio")
            })
        }
    }
}