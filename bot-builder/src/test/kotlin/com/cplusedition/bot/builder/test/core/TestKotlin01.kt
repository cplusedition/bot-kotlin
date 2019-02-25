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

import com.cplusedition.bot.builder.BuilderLogger
import com.cplusedition.bot.builder.test.zzz.TestBase
import org.junit.Assert.*
import org.junit.Test

class TestKotlin01 : TestBase() {

    companion object {

        var count = 0

        fun usage(): Int {
            ++count
            return count
        }

        val usage1 = usage()
        val usage2: Int get() = usage()
    }

    /**
     * Q: Is there a different between = expr and get() = expr.
     * A: Yes, = is an assignment while get() would be executed on each access.
     */
    @Test
    fun testLazy01() {
        val s1 = usage1
        val s2 = usage1
        val s3 = usage2
        val s4 = usage2
        log.d("# s1=$s1, s2=$s2, s3=$s3, s4=$s4")
        assertEquals(1, s1)
        assertEquals(1, s2)
        assertEquals(2, s3)
        assertEquals(3, s4)
    }


    /** Check that initializer is executed only once. */
    @Test
    fun testInitializer01() {
        class T {
            var count = 0
            val log = {
                ++count
                val classname = this::class.simpleName
                BuilderLogger(true, classname ?: "BasicBuilder")
            }()
        }

        val t = T()
        t.log.i("# testing#1")
        t.log.w("# testing#2")
        assertEquals(1, t.count)
    }

    /**
     * Q: Do String.split() preserve empty tokens.
     * A: Yes
     */
    @Test
    fun testSplit01() {
        assertEquals(3, "a::b".split(':').size)
    }

    /**
     * Q: Is null is Type return false.
     * A:Yes
     */
    @Test
    fun testIs01() {
        val anull: TestKotlin01? = null
        assertEquals(false, anull is TestKotlin01)
    }

    @Test
    fun testGenerator01() {
        class Node(
            val parent: Node?,
            val name: String
        )

        val node1 = Node(null, "a")
        val node2 = Node(node1, "b")
        val node3 = Node(node2, "c")
        val node4 = Node(node3, "d")

        fun Node.parents(): Sequence<Node> {
            return sequence<Node> {
                var n: Node? = parent
                while (n != null) {
                    yield(n)
                    n = n.parent
                }
            }
        }

        fun Node.walkUp(code: (Node?) -> Boolean): Node? {
            if (parent == null) return null
            if (code(parent)) return parent
            return parent.walkUp(code)
        }

        val ret = node4.parents().firstOrNull {
            "a" == it.name
        }
        assertEquals("a", ret?.name)

        val ret2 = node3.walkUp {
            it != null && "a" == it.name
        }
        assertEquals("a", ret2?.name)
    }

    /**
     * Q: Do kotlin a == b works when a or b or both are null?
     * A: Yes
     */
    @Test
    fun testEqualOperator01() {
        fun equal(a: String?, b: String?): Boolean {
            return a == b
        }

        fun notequal(a: String?, b: String?): Boolean {
            return a != b
        }
        assertTrue(equal(null, null))
        assertFalse(equal("a", null))
        assertFalse(equal(null, "b"))
        assertTrue(equal("a", "a"))
        assertFalse(equal("a", "b"))
        //
        assertFalse(notequal(null, null))
        assertTrue(notequal("a", null))
        assertTrue(notequal(null, "b"))
        assertFalse(notequal("a", "a"))
        assertTrue(notequal("a", "b"))
    }

}
