package net.gmmhub.haproxy

import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import java.io.IOException

/** Decides which files get HAProxy support: by name first, then by section headers in generic files. */
internal object HaproxyFiles {
    /**
     * File types that only mean "some text with a .cfg/.conf extension". Content detection never
     * touches a file another plugin claimed with a specific type.
     */
    private val GENERIC_FILE_TYPES = setOf("PLAIN_TEXT", "UNKNOWN", "Ini", "Properties", TEXTMATE_FILE_TYPE)

    private const val PREFIX_BYTES = 64 * 1024

    private val DETECTION = Key.create<Pair<Long, Boolean>>("haproxy.content.detection")

    fun isHaproxyConfig(file: VirtualFile): Boolean =
        !file.isDirectory && (matchesByName(file) || isDetectedByContent(file))

    fun matchesByName(file: VirtualFile): Boolean = HaproxyDetection.matchesByName(file.name, file.parent?.name)

    fun nameBasedFileTypeName(file: VirtualFile): String =
        FileTypeManager.getInstance().getFileTypeByFileName(file.nameSequence).name

    private fun isDetectedByContent(file: VirtualFile): Boolean {
        if (!file.isInLocalFileSystem || !HaproxyDetection.isContentCandidate(file.name)) return false
        if (nameBasedFileTypeName(file) !in GENERIC_FILE_TYPES) return false

        val stamp = file.modificationStamp
        file.getUserData(DETECTION)?.let { (cachedStamp, detected) -> if (cachedStamp == stamp) return detected }
        val detected = HaproxyDetection.looksLikeHaproxyConfig(readPrefix(file))
        file.putUserData(DETECTION, stamp to detected)
        return detected
    }

    // HAProxy configs are ASCII; asking the file for its charset would recurse into the file type lookup.
    private fun readPrefix(file: VirtualFile): String =
        try {
            file.inputStream.use { String(it.readNBytes(PREFIX_BYTES), Charsets.UTF_8) }
        } catch (_: IOException) {
            ""
        }
}

internal const val TEXTMATE_FILE_TYPE = "textmate"
