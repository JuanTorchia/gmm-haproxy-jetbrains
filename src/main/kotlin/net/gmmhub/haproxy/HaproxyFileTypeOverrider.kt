package net.gmmhub.haproxy

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.impl.FileTypeOverrider
import com.intellij.openapi.vfs.VirtualFile

/**
 * Moves HAProxy files that a generic file type claims (PyCharm maps `*.cfg` to INI) to the
 * TextMate file type, so a foreign parser does not report errors next to the HAProxy diagnostics.
 */
class HaproxyFileTypeOverrider : FileTypeOverrider {
    override fun getOverriddenFileType(file: VirtualFile): FileType? {
        if (!HaproxyDetection.isContentCandidate(file.name)) return null
        if (HaproxyFiles.nameBasedFileTypeName(file) == TEXTMATE_FILE_TYPE) return null
        if (!HaproxyFiles.isHaproxyConfig(file)) return null
        return FileTypeManager.getInstance().findFileTypeByName(TEXTMATE_FILE_TYPE)
    }
}
