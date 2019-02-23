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
import com.cplusedition.bot.core.ReflectUtil.Companion.ReflectUt
import org.junit.Assert.*
import org.junit.Test

class TestReflectUt01 : TestBase() {

    object TestObject {
        val boolean = true
        val integer = 123
        val long = 345L
        val float = 0.123f
        val double = 0.456
        val string = "abc"

        fun boolean(): Boolean {
            return false
        }

        fun string(): String {
            return string
        }

    }

    @Test
    fun testReflectUt01() {
        subtest {
            assertEquals(null, ReflectUt.getDeclaredPropertyValue(TestObject, "notexists"))
            assertEquals(null, ReflectUt.getBooleanProperty(TestObject, "notexists"))
            assertEquals(null, ReflectUt.getIntProperty(TestObject, "notexists"))
            assertEquals(null, ReflectUt.getLongProperty(TestObject, "notexists"))
            assertEquals(null, ReflectUt.getFloatProperty(TestObject, "notexists"))
            assertEquals(null, ReflectUt.getDoubleProperty(TestObject, "notexists"))
            assertEquals("abc", ReflectUt.getStringProperty(TestObject, "string"))
            assertEquals("abc", ReflectUt.getDeclaredPropertyValue(TestObject, "string"))
            assertEquals(true, ReflectUt.getBooleanProperty(TestObject, "boolean"))
            assertEquals(123, ReflectUt.getIntProperty(TestObject, "integer"))
            assertEquals(345L, ReflectUt.getLongProperty(TestObject, "long"))
            assertEquals(0.123f, ReflectUt.getFloatProperty(TestObject, "float"))
            assertEquals(0.456, ReflectUt.getDoubleProperty(TestObject, "double"))
            assertEquals("abc", ReflectUt.getStringProperty(TestObject, "string"))
        }
    }

    @Test
    fun testObjectProperties01() {
        subtest {
            assertNotNull(ReflectUt.objectProperty(TestObject, "string"))
            assertEquals(6, ReflectUt.objectProperties(TestObject).count())
            var found = false
            ReflectUt.objectProperties(TestObject) { property, _ ->
                if ("float" == property.name) {
                    val value = property.call(TestObject) as Float
                    assertEquals(0.123f, value)
                    found = true
                }
            }
            assertTrue(found)
        }
        subtest {
            var found = false
            ReflectUt.objectProperties(TestObject) { property, _, value ->
                if ("float" == property.name) {
                    assertEquals(0.123f, value)
                    found = true
                }
            }
            assertTrue(found)
        }
    }
}