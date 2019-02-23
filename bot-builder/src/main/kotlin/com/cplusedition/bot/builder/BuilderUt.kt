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

package com.cplusedition.bot.builder

import com.cplusedition.bot.core.FileUtil.Companion.FileUt
import com.cplusedition.bot.core.TextUtil
import com.cplusedition.bot.core.TextUtil.Companion.TextUt
import com.cplusedition.bot.core.listOrEmpty
import java.io.File
import kotlin.reflect.KCallable
import kotlin.reflect.KClass

//////////////////////////////////////////////////////////////////////

open class BuilderUtil {

    companion object {
        object BU : BuilderUtil()
    }

    /**
     * Walk up to the ancestors and return the first subtree that exists. For example:
     *      ancestorTree("workspace/opt/loca/bin")
     * look for an ancestor named workspace that contains a subtree opt/local/bin.
     *
     * @return The bottom most file/dir of the subtree or null if not found.
     * In the example above, it returns the bin directory.
     */
    fun ancestorTree(tree: String, dir: File? = null): File? {
        if (tree.isEmpty()) return null
        val (first, second) = TextUt.split2(File.separator, tree)
        var parent = dir ?: FileUt.pwd()
        while (true) {
            val p = parent.parentFile ?: return null
            parent = p
            if (p.name == first) {
                if (second == null) {
                    return p
                } else {
                    val subdir = File(p, second)
                    if (subdir.exists()) {
                        return subdir
                    }
                }
            }
        }
    }

    /**
     * Walk up the ancestor, look at its siblings and return the first subtree that exists. For example:
     *      ancestorSiblingTree("workspace/opt/loca/bin")
     * look for an ancestor with a child called workspace and contains a subtree opt/local/bin.
     *
     * @return The bottom most file/dir of the subtree or null if not found.
     * In the example above, it returns the bin directory.
     */
    fun ancestorSiblingTree(tree: String, dir: File? = null): File? {
        if (tree.isEmpty()) return null
        val (first, second) = TextUt.split2(File.separator, tree)
        var parent = dir ?: FileUt.pwd()
        while (true) {
            val p = parent.parentFile ?: return null
            parent = p
            for (name in p.listOrEmpty()) {
                if (name == first) {
                    if (second == null) {
                        return File(p, name)
                    } else {
                        val subdir = File(p, "$name${File.separator}$second")
                        if (subdir.exists()) {
                            return subdir
                        }
                    }
                }
            }
        }
    }

    /**
     * @return A human readable size string, eg 1210 kB, where k is 1000.
     */
    fun filesizeString(file: File): String {
        return filesizeString(file.length())
    }

    /**
     * @return A human readable size string, eg 1210 kB.
     */
    fun filesizeString(size: Long): String {
        return TextUtil.TextUt.decUnit4String(size) + "B"
    }

    fun fail(msg: String? = null): Nothing {
        error(msg ?: "")
    }

    fun fail(c: KCallable<*>): Nothing {
        error(c.name)
    }

    fun fail(c: KClass<*>): Nothing {
        error(c.simpleName ?: c.toString())
    }

    fun fail(c: Class<*>): Nothing {
        error(c.simpleName)
    }

}

//////////////////////////////////////////////////////////////////////

