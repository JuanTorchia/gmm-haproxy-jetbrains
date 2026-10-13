package net.gmmhub.haproxy

import com.intellij.openapi.vfs.VirtualFile

/** Mirrors the VS Code 0.6.x filename rules. TODO: header-based detection for arbitrary *.cfg files. */
internal object HaproxyFiles {
    private val EXACT_NAMES = setOf("haproxy.cfg", "haproxy.conf")
    private val PREFIXED_CFG = Regex("""haproxy.*\.cfg""")

    fun isHaproxyConfig(file: VirtualFile): Boolean {
        val name = file.name
        if (name in EXACT_NAMES || PREFIXED_CFG.matches(name)) return true
        val inHaproxyDir = file.parent?.name == "haproxy"
        return inHaproxyDir && (name.endsWith(".cfg") || name.endsWith(".conf"))
    }
}
