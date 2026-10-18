package net.gmmhub.haproxy

import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import java.io.File

/** Where the Node.js executable that runs the language server comes from. */
internal sealed interface NodeLookup {
    data class Found(val executable: File) : NodeLookup
    data class ConfiguredPathInvalid(val path: String) : NodeLookup
    data object NotOnPath : NodeLookup
}

internal object HaproxyNode {
    fun resolve(
        configuredPath: String,
        findOnPath: () -> File? = { PathEnvironmentVariableUtil.findExecutableInPathOnAnyOS("node") },
    ): NodeLookup {
        if (configuredPath.isNotBlank()) {
            val file = File(configuredPath.trim())
            return if (file.isFile && file.canExecute()) NodeLookup.Found(file) else NodeLookup.ConfiguredPathInvalid(configuredPath.trim())
        }
        return findOnPath()?.let { NodeLookup.Found(it) } ?: NodeLookup.NotOnPath
    }
}
