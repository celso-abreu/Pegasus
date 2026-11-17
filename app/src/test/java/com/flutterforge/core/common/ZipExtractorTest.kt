package com.flutterforge.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ZipExtractorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `extractSafely strips root folder and validates flutter files`() {
        val zipFile = tempFolder.newFile("repo.zip")
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            zos.putNextEntry(ZipEntry("owner-repo-sha123/pubspec.yaml"))
            zos.write("name: my_app\n".toByteArray())
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("owner-repo-sha123/lib/main.dart"))
            zos.write("void main() {}\n".toByteArray())
            zos.closeEntry()
        }

        val destDir = File(tempFolder.root, "out")
        val result = ZipExtractor.extractSafely(zipFile, destDir)

        assertTrue(result.hasPubspecYaml)
        assertTrue(result.hasLibMainDart)
        assertTrue(File(destDir, "pubspec.yaml").isFile)
        assertTrue(File(destDir, "lib/main.dart").isFile)
    }

    @Test
    fun `extractSafely blocks Zip Slip traversal`() {
        val zipFile = tempFolder.newFile("malicious.zip")
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            zos.putNextEntry(ZipEntry("owner-repo-sha123/../../evil.txt"))
            zos.write("pwned".toByteArray())
            zos.closeEntry()
        }

        val destDir = File(tempFolder.root, "safe_out")
        try {
            ZipExtractor.extractSafely(zipFile, destDir)
            fail("Expected SecurityException for Zip Slip entry")
        } catch (expected: SecurityException) {
            assertTrue(expected.message!!.contains("Zip Slip"))
        }
        assertEquals(false, File(tempFolder.root, "evil.txt").exists())
    }
}
