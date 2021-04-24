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

package com.cplusedition.bot.build

import com.cplusedition.bot.build.BuilderBase.Workspace.botBuilderProject
import com.cplusedition.bot.builder.CoreTask
import com.cplusedition.bot.core.Fun11
import com.cplusedition.bot.core.ut
import org.junit.Ignore
import org.junit.Test

class TestBuilder : BuilderBase(true) {

    @Ignore
    @Test
    fun debugOff() {
        val regex = Regex("""(?m)^(\s*class\s+\w+\s*:\s*TestBase\()\s*true\s*\)""")
        task(SetDebug { regex.replace(it, "$1)") })
    }

    @Ignore
    @Test
    fun debugOn() {
        val regex = Regex("""(?m)^(\s*class\s+\w+\s*:\s*TestBase\()\s*\)""")
        task(SetDebug { regex.replace(it, "$1true)") })
    }

    class SetDebug(val code: Fun11<String, String>) : CoreTask<Unit>() {
        override fun run() {
            for (dir in botBuilderProject.testSrcs) {
                log.d("### $dir")
                var count = 0
                dir.ut.files { file, rpath ->
                    if (!rpath.endsWith(".kt")) return@files
                    val content = file.readText()
                    val output = code(content)
                    if (output != content) {
                        file.writeText(output)
                        log.d("# $rpath: modified")
                        ++count
                    }
                }
                log.d("## $count modified")
            }
        }
    }
}
