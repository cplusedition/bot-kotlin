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

import com.cplusedition.bot.builder.*
import com.cplusedition.bot.builder.test.zzz.TestBase
import com.cplusedition.bot.core.*
import com.cplusedition.bot.core.ChecksumUtil.ChecksumKind
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.attribute.PosixFilePermission

class TestTaskdef01 : TestBase() {

    @Test
    fun testZip01() {
        log.enterX(this::testZip01) {
            val filesdir = testResDir.file("files")
            subtest("constructors") {
                assertEquals(5, task(Zip(tmpFile(), Fileset(filesdir, "dir1/**/*.txt"))).oks.size)
                assertEquals(16, task(Zip(tmpFile(), filesdir)).oks.size)
                assertEquals(5, task(Zip(tmpFile(), filesdir, "dir1/**/*.txt")).oks.size)
                assertEquals(4, task(Zip(tmpFile(), filesdir, "dir1/**/*.txt", "**/dir1a/**")).oks.size)
                assertEquals(14, task(Zip(tmpFile(), filesdir, null, "**/dir1a/**")).oks.size)
            }
            subtest("add") {
                assertEquals(5, task(Zip(tmpFile()).add(Fileset(filesdir, "dir1/**/*.txt"))).oks.size)
                assertEquals(11, task(Zip(tmpFile()).add(Fileset(filesdir).filesOnly())).oks.size)
                assertEquals(5, task(Zip(tmpFile()).add(Fileset(filesdir).dirsOnly())).oks.size)
                assertEquals(16, task(Zip(tmpFile()).add(filesdir)).oks.size)
                assertEquals(5, task(Zip(tmpFile()).add(filesdir, "dir1/**/*.txt")).oks.size)
                assertEquals(4, task(Zip(tmpFile()).add(filesdir, "dir1/**/*.txt", "**/dir1a/**")).oks.size)
                assertEquals(14, task(Zip(tmpFile()).add(filesdir, null, "**/dir1a/**")).oks.size)
            }
            subtest("with prefix") {
                for (rpath in task(Zip(tmpFile()).withPrefix(Fileset(filesdir, "dir1/**/*.txt"))).oks) {
                    assertTrue(rpath.startsWith("files/dir1/"))
                }
                for (rpath in task(Zip(tmpFile()).withPrefix("prefix", Fileset(filesdir, "dir1/**/*.txt"))).oks) {
                    assertTrue(rpath.startsWith("prefixdir1/"))
                }
            }
            subtest("Read errors") {
                val tmpdir = tmpDir()
                task(Copy(tmpdir, filesdir))
                val dir1 = tmpdir.file("dir1")
                dir1.ut.walk(bottomup = true) { file, _ ->
                    U.setNotReadable(file)
                }
                try {
                    val result = task(Zip(tmpFile(), tmpdir))
                    assertEquals(4, result.fails.size)
                    assertTrue(log.resetErrorCount() > 0)
                } finally {
                    dir1.ut.walk { file, _ ->
                        FileUt.setWorldReadonly(file)
                    }
                }
            }
        }
    }

    @Test
    fun testZipPreserveTimestamp01() {
        val filesdir = testResDir.file("files")
        subtest("preservetimestamp=true") {
            val zipfile = tmpFile()
            val result = task(Zip(zipfile, filesdir).preserveTimestamp(true))
            assertEquals(16, result.oks.size)
            val outdir = tmpDir()
            FileUt.unzip(outdir, zipfile)
            var count = 0
            outdir.ut.walk { file, rpath ->
                if (file.isFile || rpath.endsWith("empty.dir")) {
                    ++count
                    checkPreserveTimestamp(true, file, filesdir.file(rpath), 2000)
                }
            }
            assertEquals(12, count)
        }
        subtest("preservetimestamp=false") {
            val zipfile = tmpFile()
            val result = task(Zip(zipfile, filesdir).preserveTimestamp(false))
            assertEquals(16, result.oks.size)
            val outdir = tmpDir()
            FileUt.unzip(outdir, zipfile)
            var count = 0
            outdir.ut.walk { file, rpath ->
                if (file.isFile || rpath.endsWith("empty.dir")) {
                    ++count
                    checkPreserveTimestamp(false, file, filesdir.file(rpath), 2000)
                }
            }
            assertEquals(12, count)
        }
    }

    @Test
    fun testZipWithDebug01() {
        with(DebugBuilder()) {
            val filesdir = testResDir.file("files")
            subtest("empty") {
                assertFalse(log.errorCount > 0)
                assertEquals(0, task(Zip(tmpFile())).oks.size)
                assertTrue(log.errorCount > 0)
                assertTrue(log.getLog().join("").contains("zip file not created"))
                log.resetErrorCount()
            }
            subtest("verbose") {
                task(Zip(tmpFile(), Fileset(filesdir, "dir2/**/*.txt")))
                assertFalse(log.getLog().join("").contains("dir2/dir2a/"))
                val result = task(Zip(tmpFile(), Fileset(filesdir, "dir2/**/*.txt")).setVerbose(true))
                assertTrue(log.getLog().join("").contains("dir2/dir2a/"))
                assertTrue(result.toString().contains(Regex(".*:\\s*[\\d.]+\\s+\\w?B")))
                assertTrue(result.toString(true).contains(Regex(".*:\\s*[\\d.]+\\s+\\w?B")))
            }
        }
    }

    @Test
    fun testCopy01() {
        log.enterX(this::testCopy01) {
            val filesdir = testResDir.file("files")
            subtest("constructors") {
                assertEquals(11, task(Copy(tmpDir(), Fileset(filesdir))).copied.size)
                assertEquals(11, task(Copy(tmpDir(), filesdir)).copied.size)
                assertEquals(5, task(Copy(tmpDir(), filesdir, "dir1/**/*.txt")).copied.size)
                assertEquals(4, task(Copy(tmpDir(), filesdir, "dir1/**/*.txt", "**/dir1a/**")).copied.size)
                assertEquals(10, task(Copy(tmpDir(), filesdir, null, "**/dir1a/**")).copied.size)
            }
            subtest("add") {
                assertEquals(11, task(Copy(tmpDir()).add(Fileset(filesdir))).copied.size)
                assertEquals(11, task(Copy(tmpDir()).add(filesdir)).copied.size)
                assertEquals(5, task(Copy(tmpDir()).add(filesdir, "dir1/**/*.txt")).copied.size)
                assertEquals(4, task(Copy(tmpDir()).add(filesdir, "dir1/**/*.txt", "**/dir1a/**")).copied.size)
                assertEquals(10, task(Copy(tmpDir()).add(filesdir, null,  "**/dir1a/**")).copied.size)
            }
            subtest("preserveTimestamp=true") {
                val tmpdir = tmpDir()
                for (rpath in task(Copy(tmpdir).add(Fileset(filesdir))).copied) {
                    assertEquals(filesdir.file(rpath).lastModified(), tmpdir.file(rpath).lastModified())
                }
            }
            subtest("preserveTimestamp=false") {
                val tmpdir = tmpDir()
                for (rpath in task(Copy(tmpdir).add(Fileset(filesdir)).preserveTimestamp(false)).copied) {
                    assertFalse(filesdir.file(rpath).lastModified() == tmpdir.file(rpath).lastModified())
                }
            }
            subtest("Copy fail") {
                val tmpdir = tmpDir()
                task(Copy(tmpdir, filesdir))
                val dir1 = tmpdir.file("dir1")
                dir1.deleteSubtreesOrFail()
                dir1.setReadOnly()
                try {
                    val result = task(Copy(tmpdir, filesdir))
                    log.d(result)
                    assertEquals(5, result.copyFailed.size)
                    assertEquals(5, log.resetErrorCount())
                } finally {
                    dir1.setWritable(true, true)
                }
            }
        }
    }

    @Test
    fun testCopyDiff01() {
        log.enterX(this::testCopyDiff01) {
            val filesdir = testResDir.file("files")
            subtest("constructors") {
                assertEquals(11, task(CopyDiff(tmpDir(), Fileset(filesdir))).copied.size)
                assertEquals(11, task(CopyDiff(tmpDir(), filesdir)).copied.size)
                assertEquals(5, task(CopyDiff(tmpDir(), filesdir, "dir1/**/*.txt")).copied.size)
                assertEquals(4, task(CopyDiff(tmpDir(), filesdir, "dir1/**/*.txt", "**/dir1a/**")).copied.size)
                assertEquals(10, task(CopyDiff(tmpDir(), filesdir, null,  "**/dir1a/**")).copied.size)
            }
            subtest("add") {
                assertEquals(11, task(CopyDiff(tmpDir()).add(Fileset(filesdir))).copied.size)
                assertEquals(11, task(CopyDiff(tmpDir()).add(filesdir)).copied.size)
                assertEquals(5, task(CopyDiff(tmpDir()).add(filesdir, "dir1/**/*.txt")).copied.size)
                assertEquals(4, task(CopyDiff(tmpDir()).add(filesdir, "dir1/**/*.txt", "**/dir1a/**")).copied.size)
                assertEquals(10, task(CopyDiff(tmpDir()).add(filesdir, exclude = "**/dir1a/**")).copied.size)
            }
            subtest("preserveTimestamp=true") {
                val tmpdir = tmpDir()
                for (rpath in task(CopyDiff(tmpdir).add(Fileset(filesdir))).copied) {
                    assertEquals(filesdir.file(rpath).lastModified(), tmpdir.file(rpath).lastModified())
                }
            }
            subtest("preserveTimestamp=false") {
                val tmpdir = tmpDir()
                for (rpath in task(CopyDiff(tmpdir).add(Fileset(filesdir)).preserveTimestamp(false)).copied) {
                    assertFalse(filesdir.file(rpath).lastModified() == tmpdir.file(rpath).lastModified())
                }
            }
        }
    }

    @Test
    fun testCopyDiffWithDebug01() {
        with(DebugBuilder()) {
            val filesdir = testResDir.file("files")
            subtest("printstate") {
                val tmpdir = tmpDir()
                task(Copy(tmpdir).add(Fileset(filesdir))).copied.size
                task(Remove(tmpdir, "dir1/**"))
                val rpath = "dir2/file1.txt"
                val fromfile = filesdir.file(rpath)
                val tofile = tmpdir.file(rpath)
                tofile.writeText("xxxxxxx")
                assertTrue(FileUt.diff(fromfile, tofile))
                assertFalse(tmpdir.file("dir1/file1.txt").exists())
                val copydiff = task(CopyDiff(tmpdir, filesdir).preserveTimestamp(false))
                assertFalse(log.getLog().join("").contains("dir2/file1.txt"))
                log.d(copydiff.toString(true))
                assertTrue(log.getLog().join("").contains("dir2/file1.txt"))
                assertFalse(FileUt.diff(filesdir.file(rpath), tmpdir.file(rpath)))
                assertFalse(fromfile.lastModified() == tofile.lastModified())
                assertTrue(tmpdir.file("dir1/file1.txt").exists())
                assertEquals(
                        filesdir.file("dir2/file2.txt").lastModified(),
                        tmpdir.file("dir2/file2.txt").lastModified()
                )
            }
        }
    }

    @Test
    fun testCopyDiffWithDebug02() {
        with(DebugBuilder()) {
            val filesdir = testResDir.file("files")
            subtest("Copy fail") {
                val tmpdir = tmpDir()
                task(Copy(tmpdir, filesdir))
                val dir1 = tmpdir.file("dir1")
                dir1.deleteSubtreesOrFail()
                dir1.setReadOnly()
                task(Remove(tmpdir, "dir2/file1.txt"))
                try {
                    val result = task(CopyDiff(tmpdir, filesdir))
                    assertEquals(1, result.copied.size)
                    assertEquals(5, result.notCopied.size)
                    assertEquals(5, result.copyFailed.size)
                    // Not printing not copied.
                    assertFalse(result.toString().contains("dir2/file2.txt"))
                    // Printing copied and failed.
                    assertTrue(result.toString(true).contains("dir2/file1.txt"))
                    assertTrue(result.toString(true).contains("dir1/file2.txt"))
                    assertEquals(5, log.resetErrorCount())
                } finally {
                    dir1.setWritable(true, true)
                }
            }
        }
    }

    @Test
    fun testCopyDiff02() {
        log.enterX(this::testCopyDiff02) {
            val tmpdir = tmpDir()
            val filesdir = testResDir.file("files")
            subtest {
                val copy = task(Copy(tmpdir, Fileset(filesdir)))
                val total = copy.copied.size
                log.d(copy.copied)
                val removed = task(
                        Remove(Fileset(tmpdir).includes("empty*", "dir1/**").filesOnly())
                ).okCount
                assertEquals(6, removed)
                tmpdir.file("dir2/new.txt").writeText("testing 3435273")
                val files = Fileset(tmpdir, ".*").pairOfFiles().count()
                val dirs = Fileset(tmpdir, ".*").pairOfDirs().count()
                log.d("# $files files, $dirs dirs")
                val copydiff = task(CopyDiff(tmpdir, Fileset(filesdir)))
                val actual = copydiff.copied.size
                assertEquals(removed, actual)
                assertEquals(total - removed, copydiff.notCopied.size)
            }
        }
    }

    @Test
    fun testCopyMirror01() {
        log.enterX(this::testCopyMirror01) {
            val filesdir = testResDir.file("files")
            subtest("constructors") {
                assertEquals(11, task(CopyMirror(tmpDir(), Fileset(filesdir))).copied.size)
                assertEquals(11, task(CopyMirror(tmpDir(), filesdir)).copied.size)
                assertEquals(5, task(CopyMirror(tmpDir(), filesdir, "dir1/**/*.txt")).copied.size)
                assertEquals(4, task(CopyMirror(tmpDir(), filesdir, "dir1/**/*.txt", "**/dir1a/**")).copied.size)
                assertEquals(10, task(CopyMirror(tmpDir(), filesdir, null,  "**/dir1a/**")).copied.size)
            }
            subtest("preserveTimestamp=true") {
                val tmpdir = tmpDir()
                for (rpath in task(CopyMirror(tmpdir, Fileset(filesdir))).copied) {
                    assertEquals(filesdir.file(rpath).lastModified(), tmpdir.file(rpath).lastModified())
                }
            }
            subtest("preserveTimestamp=false") {
                val tmpdir = tmpDir()
                for (rpath in task(CopyMirror(tmpdir, Fileset(filesdir)).preserveTimestamp(false)).copied) {
                    assertFalse(filesdir.file(rpath).lastModified() == tmpdir.file(rpath).lastModified())
                }
            }
        }
    }

    @Test
    fun testCopyMirror02() {
        val filesdir = testResDir.file("files")
        subtest("Preserve") {
            val tmpdir = tmpDir()
            task(Copy(tmpdir, Fileset(filesdir)))
            task(Remove(Fileset(tmpdir).includes("empty*", "dir1", "dir1/**")))
            val tmpdir2 = tmpDir()
            task(Copy(tmpdir2, Fileset(filesdir)))
            val task = task(CopyMirror(tmpdir2, Fileset(tmpdir)).preserve { file, _ -> file.isDirectory })
            assertEquals(6, task.extraFilesRemoved.size)
            assertEquals(0, task.extraDirsRemoved.size)
        }
        subtest("Copy fail") {
            val tmpdir = tmpDir()
            task(Copy(tmpdir, filesdir))
            val dir1 = tmpdir.file("dir1")
            dir1.deleteSubtreesOrFail()
            dir1.setReadOnly()
            try {
                val result = task(CopyMirror(tmpdir, filesdir))
                log.d(result)
                assertEquals(5, result.copyFailed.size)
                assertEquals(5, log.resetErrorCount())
            } finally {
                dir1.setWritable(true, true)
            }
        }
        subtest("Remove extras fail") {
            val tmpdir1 = tmpDir()
            val tmpdir2 = tmpDir()
            task(Copy(tmpdir1, filesdir))
            task(Copy(tmpdir2, filesdir))
            val dir1 = tmpdir2.file("dir1")
            dir1.setReadOnly()
            tmpdir1.file("dir1").deleteRecursively()
            try {
                val result = task(CopyMirror(tmpdir2, tmpdir1))
                log.d(result)
                assertEquals(4, result.extraFilesRemoveFailed.size)
                assertEquals(2, result.extraDirsRemoveFailed.size)
                assertEquals(6, log.resetErrorCount())
            } finally {
                dir1.setWritable(true, true)
            }
        }
    }

    @Test
    fun testCopyMirrorLog01() {
        with(DebugBuilder()) {
            fun checkPrintStat(summary: Boolean, detail: Boolean, logs: List<String>) {
                assertEquals(summary, logs.any { it == "# Not copied: 5" })
                assertEquals(summary, logs.any { it == "# Extra files: 6" })
                assertEquals(summary, logs.any { it == "# Extra dirs: 3" })
                assertEquals(summary, logs.any { it == "# Extra files removed: 6" })
                assertEquals(summary, logs.any { it == "# Extra dirs removed: 3" })
                assertEquals(summary, logs.any { it == "# Copied: 1" })
                assertEquals(detail, logs.any { it == "empty.txt" })
                assertEquals(detail, logs.any { it == "dir1/dir1a/file1a.txt" })
            }

            val filesdir = testResDir.file("files")
            subtest("Logging") {
                val tmpdir = tmpDir()
                val copy = task(Copy(tmpdir, Fileset(filesdir)))
                assertEquals(11, copy.copied.size)
                val remove = task(Remove(Fileset(tmpdir).includes("empty*", "dir1/**")))
                assertEquals(6, remove.filesOK.size)
                assertEquals(3, remove.dirsOK.size)
                tmpdir.file("dir2/a/b/new.txt").mkparentOrFail().writeText("testing 3435273")
                tmpdir.file("dir3/a").mkdirs()
                val tmpdir2 = tmpDir()
                task0(Copy(tmpdir2, Fileset(filesdir)))
                val result = task(CopyMirror(tmpdir2, Fileset(tmpdir)))
                log.d(result.toString())
                checkPrintStat(false, false, result.toString().lines())
                log.d(result.toString0())
                checkPrintStat(true, false, result.toString0().lines())
                checkPrintStat(true, true, result.toString(true).lines())
                assertEquals(true, result.toString(false).lines().any { it.startsWith("# Not copied:") })
                assertEquals(1, result.copied.size)
                assertEquals(copy.copied.size - remove.filesOK.size, result.notCopied.size)
                assertEquals(6, result.extraFiles.size)
                assertEquals(3, result.extraDirs.size)
                assertEquals(6, result.extraFilesRemoved.size)
                assertEquals(3, result.extraDirsRemoved.size)
                assertTrue(tmpdir2.file("dir3/a").exists())
            }
        }
    }

    @Test
    fun testRemove01() {
        log.enterX(this::testRemove01) {
            val filesdir = testResDir.file("files")
            subtest("constructor(Fileset)") {
                val tmpdir = tmpDir()
                val expected = task(Copy(tmpdir, Fileset(filesdir))).copied.size
                assertEquals(expected, Fileset(tmpdir).pairOfFiles().count())
                val actual = task(Remove(Fileset(tmpdir).filesOnly())).okCount
                assertEquals(expected, actual)
            }
            subtest("constructor(File, String, String)") {
                val tmpdir = tmpDir()
                task(Copy(tmpdir, Fileset(filesdir)))
                val expected = Fileset(tmpdir).pairOfAny().count()
                val actual = task(Remove(tmpdir, "**", null)).okCount
                assertEquals(expected, actual)
            }
            subtest("add(Fileset)") {
                val tmpdir = tmpDir()
                task(Copy(tmpdir, Fileset(filesdir)))
                val expected = Fileset(tmpdir).pairOfDirs().count()
                task(Remove().add(Fileset(tmpdir).filesOnly()))
                val actual = task(Remove(Fileset(tmpdir).dirsOnly())).okCount
                assertEquals(expected, actual)
            }
            subtest("add(File, String, String)") {
                val tmpdir = tmpDir()
                task(Copy(tmpdir, Fileset(filesdir)))
                val expected = Fileset(tmpdir).pairOfAny().count()
                val actual = task(Remove().add(tmpdir, "**", null)).okCount
                assertEquals(expected, actual)
            }
            subtest("Multi filesets") {
                val tmpdir = tmpDir()
                task(Copy(tmpdir, Fileset(filesdir)))
                val remove = task(
                        Remove(
                                Fileset(tmpdir).includes("empty*"),
                                Fileset(tmpdir).includes("dir1/**")
                        )
                )
                assertEquals(9, remove.total)
                assertEquals(9, remove.okCount)
                assertEquals(0, remove.failedCount)
                assertEquals(6, remove.filesOK.size)
                assertEquals(3, remove.dirsOK.size)
                assertEquals(0, remove.filesFailed.size)
                assertEquals(0, remove.dirsFailed.size)
            }
        }
    }

    @Test
    fun testRemoveLog01() {
        with(DebugBuilder()) {
            val filesdir = testResDir.file("files")
            subtest("Logging") {
                fun checkPrintStat(summary: Boolean, logs: List<String>) {
                    assertEquals(summary, logs.any { it == "# Remove file OK: 6" })
                    assertEquals(summary, logs.any { it == "# Remove dir OK: 3" })
                    assertEquals(summary, logs.any { it == "# Remove file failed: 0" })
                    assertEquals(summary, logs.any { it == "# Remove dir failed: 0" })
                }

                val tmpdir = tmpDir()
                task(Copy(tmpdir, Fileset(filesdir)))
                val result = task(Remove(Fileset(tmpdir).includes("empty*", "dir1/**")))
                checkPrintStat(false, result.toString().lines())
                checkPrintStat(true, result.toString(true).lines())
                assertEquals(9, result.total)
                assertEquals(9, result.okCount)
                assertEquals(0, result.failedCount)
                assertEquals(6, result.filesOK.size)
                assertEquals(3, result.dirsOK.size)
                assertEquals(0, result.filesFailed.size)
                assertEquals(0, result.dirsFailed.size)
            }
        }
    }

    @Test
    fun testRemoveFai01() {
        with(DebugBuilder()) {
            val filesdir = testResDir.file("files")
            fun checkPrintStat(summary: Boolean, detail: Boolean, logs: List<String>) {
                assertEquals(summary, logs.any { it == "# Remove file OK: 5" })
                assertEquals(summary, logs.any { it == "# Remove dir OK: 1" })
                assertEquals(summary, logs.any { it == "# Remove file failed: 1" })
                assertEquals(summary, logs.any { it == "# Remove dir failed: 2" })
                assertEquals(detail, logs.any { it == "empty.dir" })
                assertEquals(detail, logs.any { it == "dir1/dir1a/file1a.txt" })
            }

            val tmpdir = tmpDir()
            task(Copy(tmpdir, Fileset(filesdir)))
            tmpdir.file("dir1").ut.walk(bottomup = true) { file, _ ->
                file.setReadOnly()
            }
            try {
                val result = task(Remove(Fileset(tmpdir).includes("empty*", "dir1/**")))
                assertEquals(9, result.total)
                assertEquals(6, result.okCount)
                assertEquals(3, result.failedCount)
                assertEquals(5, result.filesOK.size)
                assertEquals(1, result.dirsOK.size)
                assertEquals(1, result.filesFailed.size)
                assertEquals(2, result.dirsFailed.size)
                checkPrintStat(false, false, result.toString().lines())
                checkPrintStat(true, true, result.toString(true).lines())
            } finally {
                tmpdir.file("dir1").ut.walk { file, _ ->
                    file.setWritable(true, true)
                }
                log.resetErrorCount()
            }
        }
    }

    @Test
    fun testChecksum01() {
        log.enterX(this::testChecksum01) {
            val filesdir = testResDir.file("files")
            subtest("Basic") {
                val sumfile = tmpFile(suffix = ".sha1")
                task(Checksum(sumfile, ChecksumKind.SHA1, Fileset(filesdir)))
                sumfile.existsOrFail()
                val task = task(VerifyChecksum(sumfile, ChecksumKind.SHA1, filesdir))
                assertEquals(11, task.total)
                assertEquals(11, task.oks.size)
                assertEquals(0, task.fails.size)
                assertEquals(0, task.notexists.size)
                assertEquals(0, task.invalids.size)
            }
            subtest("Constructor") {
                for (kind in ChecksumKind.values()) {
                    val sumfile = tmpFile(suffix = ".txt")
                    task(Checksum(sumfile, kind, Fileset(filesdir)))
                    sumfile.existsOrFail()
                    assertEquals(11, task(VerifyChecksum(sumfile, kind, filesdir)).oks.size)
                }
                for (kind in ChecksumKind.values()) {
                    val sumfile = tmpFile(suffix = ".txt")
                    task(Checksum(sumfile, kind, filesdir))
                    sumfile.existsOrFail()
                    assertEquals(11, task(VerifyChecksum(sumfile, kind, filesdir)).oks.size)
                }
                for (kind in ChecksumKind.values()) {
                    val sumfile = tmpFile(suffix = ".txt")
                    task(Checksum(sumfile, kind, filesdir, "**/dir1/**"))
                    sumfile.existsOrFail()
                    assertEquals(5, task(VerifyChecksum(sumfile, kind, filesdir)).oks.size)
                }
                for (kind in ChecksumKind.values()) {
                    val sumfile = tmpFile(suffix = ".txt")
                    task(Checksum(sumfile, kind, filesdir, "**/dir1/**", "**/dir1/dir1a/**"))
                    sumfile.existsOrFail()
                    assertEquals(4, task(VerifyChecksum(sumfile, kind, filesdir)).oks.size)
                }
            }
            subtest("Big data") {
                val datafile = tmpFile()
                datafile.writeBytes(RandomUt.get(ByteArray(RandomUt.getInt(4 * 1000 * 1000, 6 * 1000 * 1000))))
                for (kind in ChecksumKind.values()) {
                    val task = task(Checksum.single(kind, datafile))
                    task.sumfile.existsOrFail()
                    assertEquals(1, task(VerifyChecksum(task.sumfile, kind)).oks.size)
                }
            }
            subtest("Checksum fail") {
                val tmpdir = tmpDir()
                val sumfile = tmpFile()
                task(Copy(tmpdir, filesdir))
                val dir1 = tmpdir.file("dir1")
                dir1.ut.walk(bottomup = true) { file, _ ->
                    U.setNotReadable(file)
                }
                try {
                    val result = task(Checksum(sumfile, ChecksumKind.SHA256, tmpdir))
                    assertEquals(6, result.oks.size)
                    assertEquals(4, result.fails.size)
                    val output = result.toString()
                    assertTrue(output, output.contains("${result.oks.size} OK"))
                    assertTrue(output, output.contains("${result.fails.size} failed"))
                    assertTrue(output, output.contains("${ChecksumKind.SHA256}"))
                    assertTrue(log.resetErrorCount() > 0)
                } finally {
                    dir1.ut.walk { file, _ ->
                        FileUt.setWorldReadonly(file)
                    }
                }
            }
        }
    }

    @Test
    fun testVerifyChecksum01() {
        val manual = testResDir.file("html/manual.html")
        subtest("Check verify error") {
            for (kind in ChecksumKind.values()) {
                val sumfile = tmpFile()
                sumfile.writeText("12345678901234567890123456789012  ${manual.name}")
                val task = task(VerifyChecksum(sumfile, kind, manual.parentFile))
                assertEquals(1, task.fails.size)
                assertEquals(1, log.resetErrorCount())

            }
        }
        subtest("Check not exists") {
            for (kind in ChecksumKind.values()) {
                val sumfile = tmpFile()
                sumfile.writeText("12345678901234567890123456789012  ${manual.name}.notexists")
                val task = task(VerifyChecksum(sumfile, kind, manual.parentFile))
                assertEquals(1, task.notexists.size)
                assertEquals(1, log.resetErrorCount())
            }
        }
        subtest("Check allow not exists") {
            for (kind in ChecksumKind.values()) {
                val sumfile = tmpFile()
                sumfile.writeText("12345678901234567890123456789012  ${manual.name}.notexists")
                val task = task(VerifyChecksum(sumfile, kind, manual.parentFile).allowNotExists())
                assertEquals(1, task.notexists.size)
                assertEquals(0, log.errorCount)
            }
        }
        subtest("Check invalid") {
            for (kind in ChecksumKind.values()) {
                val sumfile = tmpFile()
                sumfile.writeText("()  ${manual.name}")
                val task = task(VerifyChecksum(sumfile, kind, manual.parentFile))
                assertEquals(1, task.invalids.size)
                assertEquals(1, log.resetErrorCount())
            }
        }
        subtest("Read fail") {
            val filesdir = testResDir.file("files")
            val tmpdir = tmpDir()
            val sumfile = tmpFile()
            val dir1 = tmpdir.file("dir1")
            task(Copy(tmpdir, filesdir))
            try {
                task(Checksum(sumfile, ChecksumKind.SHA1, tmpdir))
                dir1.ut.walk(bottomup = true) { file, _ ->
                    FileUt.setPermission(
                            setOf(
                                    PosixFilePermission.OWNER_WRITE,
                                    PosixFilePermission.OWNER_EXECUTE
                            ), file
                    )
                }
                val result = task(VerifyChecksum(sumfile, ChecksumKind.SHA1, tmpdir))
                assertEquals(5, result.fails.size)
                assertTrue(log.resetErrorCount() > 0)
            } finally {
                dir1.ut.walk { file, _ ->
                    FileUt.setWorldReadonly(file)
                }
            }
        }
    }

    private object U {
        fun setNotReadable(file: File) {
            FileUt.setPermission(
                    setOf(
                            PosixFilePermission.OWNER_WRITE,
                            PosixFilePermission.OWNER_EXECUTE
                    ), file
            )
        }
    }
}