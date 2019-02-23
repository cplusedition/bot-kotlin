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

import com.cplusedition.bot.builder.apache.SelectorUtils
import com.cplusedition.bot.builder.test.zzz.TestBase
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TestSelectorUtils01 : TestBase() {

    @Test
    fun testSelectorUtils01() {
        subtest {
            assertTrue(SelectorUtils.matchPath("", ""))
            assertTrue(SelectorUtils.matchPath("a", "a"))
            assertTrue(SelectorUtils.matchPath("a", "a/"))
            assertFalse(SelectorUtils.matchPath("a", "/a"))
            assertFalse(SelectorUtils.matchPath("a", "/a/"))
            assertTrue(SelectorUtils.matchPath("a/", "a"))
            assertFalse(SelectorUtils.matchPath("/a", "a"))
            assertFalse(SelectorUtils.matchPath("/a/", "a"))
            assertTrue(SelectorUtils.matchPath("a.txt", "a.txt"))
            assertFalse(SelectorUtils.matchPath("", "a"))
            assertFalse(SelectorUtils.matchPath("a", "A"))
            assertFalse(SelectorUtils.matchPath("a", "aA"))
            assertFalse(SelectorUtils.matchPath("a", "a.txt"))
            assertFalse(SelectorUtils.matchPath("a", "a/a"))
            //
            assertFalse(SelectorUtils.matchPath("*", ""))
            assertTrue(SelectorUtils.matchPath("*", "a.txt"))
            assertTrue(SelectorUtils.matchPath("*", "a.txt/"))
            assertFalse(SelectorUtils.matchPath("*", "/a.txt"))
            assertFalse(SelectorUtils.matchPath("*", "a/a.txt"))
            //
            assertTrue(SelectorUtils.matchPath("**", ""))
            assertTrue(SelectorUtils.matchPath("**", "a.txt"))
            assertTrue(SelectorUtils.matchPath("**", "a.txt/"))
            assertTrue(SelectorUtils.matchPath("**", "/a.txt"))
            assertTrue(SelectorUtils.matchPath("**", "/a.txt/"))
            assertTrue(SelectorUtils.matchPath("**", "a/a.txt"))
            assertTrue(SelectorUtils.matchPath("**", "/a/b/a.txt"))
            //
            assertTrue(SelectorUtils.matchPath("**/file.txt", "file.txt"))
            assertTrue(SelectorUtils.matchPath("**/file.txt", "a/file.txt"))
            assertTrue(SelectorUtils.matchPath("**/file.txt", "a/b/file.txt"))
            assertTrue(SelectorUtils.matchPath("**/file.txt", "/a/file.txt"))
            assertTrue(SelectorUtils.matchPath("**/file.txt", "/a/b/file.txt"))
            //
            assertFalse(SelectorUtils.matchPath("*/file.txt", "file.txt"))
            assertTrue(SelectorUtils.matchPath("*/file.txt", "a/file.txt"))
            assertFalse(SelectorUtils.matchPath("*/file.txt", "a/b/file.txt"))
            assertFalse(SelectorUtils.matchPath("*/file.txt", "/a/file.txt"))
            assertFalse(SelectorUtils.matchPath("*/file.txt", "/a/b/file.txt"))
            //
            assertFalse(SelectorUtils.matchPath("file.txt", "File.txt"))
            assertFalse(SelectorUtils.matchPath("*/file.txt", "a/File.txt"))
            assertTrue(SelectorUtils.matchPath("a/*.txt", "a/File.txt"))
            assertFalse(SelectorUtils.matchPath("a/*.txt", "a/b/File.txt"))
            assertTrue(SelectorUtils.matchPath("a/**/*.txt", "a/b/File.txt"))
            assertTrue(SelectorUtils.matchPath("**/*.txt", "a/b/File.txt"))
            assertTrue(SelectorUtils.matchPath("**/b/*.txt", "a/b/File.txt"))
            assertFalse(SelectorUtils.matchPath("**/b/*.txt", "a/b/c/File.txt"))
        }
    }
}