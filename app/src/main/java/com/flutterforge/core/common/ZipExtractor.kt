package com.flutterforge.core.common

import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

data class ZipExtractionResult(
    val rootDir: File,
    val totalSizeBytes: Long,
    val hasPubspecYaml: Boolean,
    val hasLibMainDart: Boolean
)

object ZipExtractor {

    /**
     * Extracts a GitHub zipball safely while defending against Zip Slip attacks
     * (canonical path validation) and stripping the top-level archive folder
     * (`owner-repo-commitsha/`) so `pubspec.yaml` resides directly in [destDir].
     */
    fun extractSafely(zipFile: File, destDir: File): ZipExtractionResult {
        if (destDir.exists()) {
            destDir.deleteRecursively()
        }
        destDir.mkdirs()

        val canonicalDestPath = destDir.canonicalPath
        val buffer = ByteArray(8192)
        var totalExtractedBytes = 0L

        ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val rawName = entry.name
                val strippedName = stripGitHubRootFolder(rawName)
                if (strippedName.isNotEmpty()) {
                    val outFile = File(destDir, strippedName)
                    val canonicalOutPath = outFile.canonicalPath
                    if (!canonicalOutPath.startsWith("$canonicalDestPath${File.separator}")) {
                        throw SecurityException("Zip Slip blocked for entry: $rawName")
                    }

                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        FileOutputStream(outFile).use { fos ->
                            var read: Int
                            while (zis.read(buffer).also { read = it } != -1) {
                                fos.write(buffer, 0, read)
                                totalExtractedBytes += read
                            }
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        val hasPubspec = File(destDir, "pubspec.yaml").isFile
        val hasMainDart = File(destDir, "lib/main.dart").isFile

        return ZipExtractionResult(
            rootDir = destDir,
            totalSizeBytes = totalExtractedBytes,
            hasPubspecYaml = hasPubspec,
            hasLibMainDart = hasMainDart
        )
    }

    private fun stripGitHubRootFolder(entryName: String): String {
        val normalized = entryName.replace('\\', '/').trimStart('/')
        val firstSlash = normalized.indexOf('/')
        return if (firstSlash == -1) "" else normalized.substring(firstSlash + 1)
    }
}
