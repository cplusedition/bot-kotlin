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

import com.cplusedition.bot.build.BuilderBase.Workspace.botBuildProject
import com.cplusedition.bot.build.BuilderBase.Workspace.botBuilderProject
import com.cplusedition.bot.build.BuilderBase.Workspace.botCoreProject
import com.cplusedition.bot.build.BuilderBase.Workspace.botProject
import com.cplusedition.bot.builder.BuilderUtil.Companion.BU
import com.cplusedition.bot.builder.Checksum
import com.cplusedition.bot.builder.Fileset
import com.cplusedition.bot.builder.KotlinProject
import com.cplusedition.bot.builder.Zip
import com.cplusedition.bot.core.*
import com.cplusedition.bot.core.ChecksumUtil.ChecksumKind.SHA256
import com.cplusedition.bot.core.DateUtil.Companion.DateUt
import com.cplusedition.bot.core.FileUtil.Companion.FileUt
import com.cplusedition.bot.core.ProcessUtil.Companion.ProcessUt
import com.cplusedition.bot.core.StructUtil.Companion.StructUt
import com.cplusedition.bot.core.WithUtil.Companion.With
import org.junit.Ignore
import org.junit.Test

class ReleaseBuilder : BuilderBase(true) {

    @Ignore
    @Test
    fun distSrcZip() {
        log.enterX(this::distSrcZip) {
            val zipfile = Dist.zipfile.mkparentOrFail()
            task(Zip(zipfile).withPrefix(*Dist.modules).add(Dist.top).preserveTimestamp(false))
            task(Checksum.single(SHA256, zipfile))
        }
    }

    /**
     * Demonstrate using ProcessUt.
     */
    @Ignore
    @Test
    fun distSrcZipUsingZipCommand() {
        log.enterX(this::distSrcZipUsingZipCommand) {
            val zipfile = Dist.zipfile.mkparentOrFail()
            zipfile.delete()
            val cmdline = mutableListOf("zip", "-ry", zipfile.absolutePath)
            val cmdsize = cmdline.size
            for (fileset in Dist.modules) {
                fileset.walk { _, rpath ->
                    cmdline.add("${fileset.dir.name}${FileUt.SEPCHAR}$rpath")
                }
            }
            Dist.top.walk { _, rpath ->
                cmdline.add(rpath)
            }
            // log.d(cmdline)
            log.d(ProcessUt.backtick(botProject.dir, cmdline))
            log.d("# Zip ${zipfile.name}: ${cmdline.size - cmdsize} files, ${BU.filesizeString(zipfile)}")
            task(Checksum.single(SHA256, zipfile))
        }
    }

    @Ignore
    @Test
    fun updateReleaseVersion() {
        var count = 0
        val versionPat = Regex("""(?m)^\s*version\s*'([\d.]+)'\s*$""")
        val kotlinVersionPat = Regex("""(?m)^(\s*ext\.kotlin_version\s*=\s*)'([^']+)'""")
        for (project in Workspace.projects) {
            val buildgradle = project.dir.file("build.gradle")
            if (!buildgradle.exists()) continue
            val modified = With.rewriteText(buildgradle) {
                it.replace(versionPat, "version '$VERSION'")
                    .replace(kotlinVersionPat, "$1'$KOTLIN_VERSION'")
            }
            var msg = "# ${project.gav.artifactId}"
            if (modified) {
                ++count
                msg += ": modified"
            }
            log.d(msg)
        }
        log.i("## Updated $count files")
    }

    @Ignore
    @Test
    fun fixCopyrights() {
        val copyright = Workspace.dir.existsOrFail("COPYRIGHT").readText()
        val regex = Regex("(?si)\\s*/\\*.*?Cplusedition Limited.*?\\s+All rights reserved.*?\\*/\\s*")
        for (project in Workspace.projects) {
            if (project !is KotlinProject) continue
            log.i("### ${project.gav.artifactId}")
            val modified = ArrayList<String>()
            for (dir in StructUt.concat(project.mainSrcs, project.testSrcs)) {
                dir.walker.files { file, rpath ->
                    if (!rpath.endsWith(".kt")) return@files
                    val text = file.readText()
                    if (text.startsWith(copyright)) return@files
                    val output = copyright + regex.replace(text, "")
                    file.writeText(output)
                    log.i("$rpath: modified")
                    modified.add(rpath)
                }
            }
            log.i("## Updated ${modified.size} files")
        }
    }

    object Dist {
        val zipfile = botBuildProject.dir.file("dist/${botProject.gav.artifactId}-$VERSION-${DateUt.today}-src.zip")
        val srcs = arrayOf(
            "src/main/kotlin/**/*.kt",
            "src/test/kotlin/**/*.kt",
            "build.gradle",
            ".gitignore"
        )
        val modules = arrayOf(
            Fileset(botBuildProject.dir).includes(*srcs),
            Fileset(botCoreProject.dir).includes(*srcs),
            Fileset(botBuilderProject.dir).includes(*srcs).includes("src/test/resources/**")
        )
        val top = Fileset(botProject.dir).includes(*srcs).includes(
            "settings.gradle",
            "COPYRIGHT",
            "LICENSE.txt",
            "README.md",
            "docs/**/*.md"
        )
    }
}
