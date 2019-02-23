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

/**
 *
 * This is a utility class used by selectors and DirectoryScanner. The
 * functionality more properly belongs just to selectors, but unfortunately
 * DirectoryScanner exposed these as protected methods. Thus we have to
 * support any subclasses of DirectoryScanner that may access these methods.
 *
 *
 * This is a Singleton.
 *
 * @since 1.5
 */
object SelectorUtils {

    /**
     * The pattern that matches an arbitrary number of directories.
     * @since Ant 1.8.0
     */
    val DEEP_TREE_MATCH = "**"

    /**
     * Tests whether or not a given path matches a given pattern.
     *
     * If you need to call this method multiple times with the same
     * pattern you should rather use TokenizedPath
     *
     * @see TokenizedPath
     *
     *
     * @param pattern The pattern to match against. Must not be
     * `null`.
     * @param str     The path to match, as a String. Must not be
     * `null`.
     *
     * @return `true` if the pattern matches against the string,
     * or `false` otherwise.
     */
    fun matchPath(pattern: String, str: String): Boolean {
        val patDirs = tokenizePathAsArray(pattern)
        return matchPath(
            patDirs,
            tokenizePathAsArray(str),
            true
        )
    }

    /**
     * Tests whether or not a given path matches a given pattern.
     *
     * If you need to call this method multiple times with the same
     * pattern you should rather use TokenizedPattern
     *
     * @see TokenizedPattern
     *
     *
     * @param pattern The pattern to match against. Must not be
     * `null`.
     * @param str     The path to match, as a String. Must not be
     * `null`.
     * @param isCaseSensitive Whether or not matching should be performed
     * case sensitively.
     *
     * @return `true` if the pattern matches against the string,
     * or `false` otherwise.
     */
    fun matchPath(
        pattern: String, str: String,
        isCaseSensitive: Boolean
    ): Boolean {
        val patDirs = tokenizePathAsArray(pattern)
        return matchPath(
            patDirs,
            tokenizePathAsArray(str),
            isCaseSensitive
        )
    }

    /**
     * Core implementation of matchPath.  It is isolated so that it
     * can be called from TokenizedPattern.
     */
    internal fun matchPath(
        tokenizedPattern: Array<String>, strDirs: Array<String>,
        isCaseSensitive: Boolean
    ): Boolean {
        var patIdxStart = 0
        var patIdxEnd = tokenizedPattern.size - 1
        var strIdxStart = 0
        var strIdxEnd = strDirs.size - 1

        // up to first '**'
        while (patIdxStart <= patIdxEnd && strIdxStart <= strIdxEnd) {
            val patDir = tokenizedPattern[patIdxStart]
            if (patDir == DEEP_TREE_MATCH) {
                break
            }
            if (!match(patDir, strDirs[strIdxStart], isCaseSensitive)) {
                return false
            }
            patIdxStart++
            strIdxStart++
        }
        if (strIdxStart > strIdxEnd) {
            // String is exhausted
            for (i in patIdxStart..patIdxEnd) {
                if (tokenizedPattern[i] != DEEP_TREE_MATCH) {
                    return false
                }
            }
            return true
        } else {
            if (patIdxStart > patIdxEnd) {
                // String not exhausted, but pattern is. Failure.
                return false
            }
        }

        // up to last '**'
        while (patIdxStart <= patIdxEnd && strIdxStart <= strIdxEnd) {
            val patDir = tokenizedPattern[patIdxEnd]
            if (patDir == DEEP_TREE_MATCH) {
                break
            }
            if (!match(patDir, strDirs[strIdxEnd], isCaseSensitive)) {
                return false
            }
            patIdxEnd--
            strIdxEnd--
        }
        if (strIdxStart > strIdxEnd) {
            // String is exhausted
            for (i in patIdxStart..patIdxEnd) {
                if (tokenizedPattern[i] != DEEP_TREE_MATCH) {
                    return false
                }
            }
            return true
        }

        while (patIdxStart != patIdxEnd && strIdxStart <= strIdxEnd) {
            var patIdxTmp = -1
            for (i in patIdxStart + 1..patIdxEnd) {
                if (tokenizedPattern[i] == DEEP_TREE_MATCH) {
                    patIdxTmp = i
                    break
                }
            }
            if (patIdxTmp == patIdxStart + 1) {
                // '**/**' situation, so skip one
                patIdxStart++
                continue
            }
            // Find the pattern between padIdxStart & padIdxTmp in str between
            // strIdxStart & strIdxEnd
            val patLength = patIdxTmp - patIdxStart - 1
            val strLength = strIdxEnd - strIdxStart + 1
            var foundIdx = -1
            strLoop@ for (i in 0..strLength - patLength) {
                for (j in 0 until patLength) {
                    val subPat = tokenizedPattern[patIdxStart + j + 1]
                    val subStr = strDirs[strIdxStart + i + j]
                    if (!match(subPat, subStr, isCaseSensitive)) {
                        continue@strLoop
                    }
                }

                foundIdx = strIdxStart + i
                break
            }

            if (foundIdx == -1) {
                return false
            }

            patIdxStart = patIdxTmp
            strIdxStart = foundIdx + patLength
        }

        for (i in patIdxStart..patIdxEnd) {
            if (tokenizedPattern[i] != DEEP_TREE_MATCH) {
                return false
            }
        }

        return true
    }

    /**
     * Tests whether or not a string matches against a pattern.
     * The pattern may contain two special characters:<br></br>
     * '*' means zero or more characters<br></br>
     * '?' means one and only one character
     *
     * @param pattern The pattern to match against.
     * Must not be `null`.
     * @param str     The string which must be matched against the pattern.
     * Must not be `null`.
     * @param caseSensitive Whether or not matching should be performed
     * case sensitively.
     *
     *
     * @return `true` if the string matches against the pattern,
     * or `false` otherwise.
     */
    @JvmOverloads
    fun match(
        pattern: String, str: String,
        caseSensitive: Boolean = true
    ): Boolean {
        val patArr = pattern.toCharArray()
        val strArr = str.toCharArray()
        var patIdxStart = 0
        var patIdxEnd = patArr.size - 1
        var strIdxStart = 0
        var strIdxEnd = strArr.size - 1
        var ch: Char

        var containsStar = false
        for (i in patArr.indices) {
            if (patArr[i] == '*') {
                containsStar = true
                break
            }
        }

        if (!containsStar) {
            // No '*'s, so we make a shortcut
            if (patIdxEnd != strIdxEnd) {
                return false // Pattern and string do not have the same size
            }
            for (i in 0..patIdxEnd) {
                ch = patArr[i]
                if (ch != '?') {
                    if (different(caseSensitive, ch, strArr[i])) {
                        return false // Character mismatch
                    }
                }
            }
            return true // String matches against pattern
        }

        if (patIdxEnd == 0) {
            return true // Pattern contains only '*', which matches anything
        }

        // Process characters before first star
        while (true) {
            ch = patArr[patIdxStart]
            if (ch == '*' || strIdxStart > strIdxEnd) {
                break
            }
            if (ch != '?') {
                if (different(caseSensitive, ch, strArr[strIdxStart])) {
                    return false // Character mismatch
                }
            }
            patIdxStart++
            strIdxStart++
        }
        if (strIdxStart > strIdxEnd) {
            // All characters in the string are used. Check if only '*'s are
            // left in the pattern. If so, we succeeded. Otherwise failure.
            return allStars(patArr, patIdxStart, patIdxEnd)
        }

        // Process characters after last star
        while (true) {
            ch = patArr[patIdxEnd]
            if (ch == '*' || strIdxStart > strIdxEnd) {
                break
            }
            if (ch != '?') {
                if (different(caseSensitive, ch, strArr[strIdxEnd])) {
                    return false // Character mismatch
                }
            }
            patIdxEnd--
            strIdxEnd--
        }
        if (strIdxStart > strIdxEnd) {
            // All characters in the string are used. Check if only '*'s are
            // left in the pattern. If so, we succeeded. Otherwise failure.
            return allStars(patArr, patIdxStart, patIdxEnd)
        }

        // process pattern between stars. padIdxStart and patIdxEnd point
        // always to a '*'.
        while (patIdxStart != patIdxEnd && strIdxStart <= strIdxEnd) {
            var patIdxTmp = -1
            for (i in patIdxStart + 1..patIdxEnd) {
                if (patArr[i] == '*') {
                    patIdxTmp = i
                    break
                }
            }
            if (patIdxTmp == patIdxStart + 1) {
                // Two stars next to each other, skip the first one.
                patIdxStart++
                continue
            }
            // Find the pattern between padIdxStart & padIdxTmp in str between
            // strIdxStart & strIdxEnd
            val patLength = patIdxTmp - patIdxStart - 1
            val strLength = strIdxEnd - strIdxStart + 1
            var foundIdx = -1
            strLoop@ for (i in 0..strLength - patLength) {
                for (j in 0 until patLength) {
                    ch = patArr[patIdxStart + j + 1]
                    if (ch != '?') {
                        if (different(
                                caseSensitive, ch,
                                strArr[strIdxStart + i + j]
                            )
                        ) {
                            continue@strLoop
                        }
                    }
                }

                foundIdx = strIdxStart + i
                break
            }

            if (foundIdx == -1) {
                return false
            }

            patIdxStart = patIdxTmp
            strIdxStart = foundIdx + patLength
        }

        // All characters in the string are used. Check if only '*'s are left
        // in the pattern. If so, we succeeded. Otherwise failure.
        return allStars(patArr, patIdxStart, patIdxEnd)
    }

    private fun allStars(chars: CharArray, start: Int, end: Int): Boolean {
        for (i in start..end) {
            if (chars[i] != '*') {
                return false
            }
        }
        return true
    }

    private fun different(
        caseSensitive: Boolean, ch: Char, other: Char
    ): Boolean {
        return if (caseSensitive)
            ch != other
        else
            Character.toUpperCase(ch) != Character.toUpperCase(other)
    }

    /**
     * Same as [tokenizePath][.tokenizePath] but hopefully faster.
     */
    /*package*/ internal fun tokenizePathAsArray(path: String): Array<String> {
        var path = path
        var root: String? = null
        if (FileUtils.isAbsolutePath(path)) {
            val s = FileUtils.dissect(path)
            root = s[0]
            path = s[1]
        }
        val sep = File.separatorChar
        var start = 0
        val len = path.length
        var count = 0
        for (pos in 0 until len) {
            if (path[pos] == sep) {
                if (pos != start) {
                    count++
                }
                start = pos + 1
            }
        }
        if (len != start) {
            count++
        }
        val l = ArrayList<String>(count + if (root == null) 0 else 1)

        if (root != null) {
            //            l[0] = root
            //            count = 1
            //        } else {
            //            count = 0
            l.add(root)
        }
        start = 0
        for (pos in 0 until len) {
            if (path[pos] == sep) {
                if (pos != start) {
                    val tok = path.substring(start, pos)
                    //                    l[count++] = tok
                    l.add(tok)
                }
                start = pos + 1
            }
        }
        if (len != start) {
            val tok = path.substring(start)
            //            l[count/*++*/] = tok
            l.add(tok)
        }
        return l.toTypedArray()
    }

}
