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

/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
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

package com.cplusedition.bot.builder.apache

import java.io.File


object FileUtils {

    private val ON_NETWARE = false
    private val ON_DOS = false

    /**
     * Verifies that the specified filename represents an absolute path.
     * Differs from new java.io.File("filename").isAbsolute() in that a path
     * beginning with a double file separator--signifying a Windows UNC--must
     * at minimum match "\\a\b" to be considered an absolute path.
     * @param filename the filename to be checked.
     * @return true if the filename represents an absolute path.
     * @throws java.lang.NullPointerException if filename is null.
     * @since Ant 1.6.3
     */
    fun isAbsolutePath(filename: String): Boolean {
        var filename = filename
        val len = filename.length
        if (len == 0) {
            return false
        }
        val sep = File.separatorChar
        filename = filename.replace('/', sep).replace('\\', sep)
        val c = filename[0]
        if (!(ON_DOS || ON_NETWARE)) {
            return c == sep
        }
        if (c == sep) {
            // CheckStyle:MagicNumber OFF
            if (!(ON_DOS && len > 4 && filename[1] == sep)) {
                return false
            }
            // CheckStyle:MagicNumber ON
            val nextsep = filename.indexOf(sep, 2)
            return nextsep > 2 && nextsep + 1 < len
        }
        val colon = filename.indexOf(':')
        return (Character.isLetter(c) && colon == 1
                && filename.length > 2 && filename[2] == sep) || ON_NETWARE && colon > 0
    }

    /**
     * Dissect the specified absolute path.
     *
     * @param path the path to dissect.
     * @return String[] {root, remaining path}.
     * @throws java.lang.NullPointerException if path is null.
     * @since Ant 1.7
     */
    fun dissect(path: String): Array<String> {
        var path = path
        val sep = File.separatorChar
        path = path.replace('/', sep).replace('\\', sep)

        // make sure we are dealing with an absolute path
        if (!isAbsolutePath(path)) {
            throw IllegalStateException("$path is not an absolute path")
        }
        var root: String? = null
        val colon = path.indexOf(':')
        if (colon > 0 && (ON_DOS || ON_NETWARE)) {

            var next = colon + 1
            root = path.substring(0, next)
            val ca = path.toCharArray()
            root += sep
            //remove the initial separator; the root has it.
            next = if (ca[next] == sep) next + 1 else next

            val sbPath = StringBuffer()
            // Eliminate consecutive slashes after the drive spec:
            for (i in next until ca.size) {
                if (ca[i] != sep || ca[i - 1] != sep) {
                    sbPath.append(ca[i])
                }
            }
            path = sbPath.toString()
        } else if (path.length > 1 && path[1] == sep) {
            // UNC drive
            var nextsep = path.indexOf(sep, 2)
            nextsep = path.indexOf(sep, nextsep + 1)
            root = if (nextsep > 2) path.substring(0, nextsep + 1) else path
            path = path.substring(root.length)
        } else {
            root = File.separator
            path = path.substring(1)
        }
        return arrayOf(root!!, path)
    }
}
