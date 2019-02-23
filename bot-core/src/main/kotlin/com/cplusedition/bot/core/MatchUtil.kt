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

open class MatchUtil {

    companion object {
        val MatchUt = MatchUtil()
    }

    fun compile(vararg regexs: String): Array<Regex> {
        return Array(regexs.size) { i -> Regex(regexs[i]) }
    }

    /**
     * Match input using Regex.matches().
     * @return true if entire input matches include and not matches exclude.
     * If the regex is null, it always match.
     */
    fun matches(input: String, include: Regex?, exclude: Regex? = null): Boolean {
        if (include != null && !include.matches(input)) return false
        if (exclude != null && exclude.matches(input)) return false
        return true
    }

    /**
     * Match input using Regex.matches().
     * @return true if input matches one of the includes and none of the excludes.
     * If the regex is null, it always match.
     */
    fun matches(input: String, includes: Iterable<Regex>?, excludes: Iterable<Regex>? = null): Boolean {
        if (includes != null && includes.none { it.matches(input) }) return false
        if (excludes != null && excludes.any { it.matches(input) }) return false
        return true
    }

    /**
     * Match input using Regex.matches().
     * @return true if input matches one of the includes and none of the excludes.
     * If the regex is null, it always match.
     */
    fun matches(input: String, includes: Array<Regex>?, excludes: Array<Regex>? = null): Boolean {
        if (includes != null && includes.none { it.matches(input) }) return false
        if (excludes != null && excludes.any { it.matches(input) }) return false
        return true
    }

    /**
     * Match input using Regex.find().
     * @return true if include is found in input and not found exclude.
     * If the regex is null, it always match.
     */
    fun find(input: String, include: Regex?, exclude: Regex? = null): Boolean {
        if (include != null && include.find(input) == null) return false
        if (exclude != null && exclude.find(input) != null) return false
        return true
    }

    /**
     * Match input using Regex.find().
     * @return true if one of the includes is found in input and none of the excludes found.
     * If the regex is null, it always match.
     */
    fun find(input: String, includes: Iterable<Regex>?, excludes: Iterable<Regex>? = null): Boolean {
        if (includes != null && includes.none { it.find(input) != null }) return false
        if (excludes != null && excludes.any { it.find(input) != null }) return false
        return true
    }

    /**
     * Match input using Regex.find().
     * @return true if one of the includes is found in input and none of the excludes found.
     * If the regex is null, it always match.
     */
    fun find(input: String, includes: Array<Regex>?, excludes: Array<Regex>? = null): Boolean {
        if (includes != null && includes.none { it.find(input) != null }) return false
        if (excludes != null && excludes.any { it.find(input) != null }) return false
        return true
    }
}
