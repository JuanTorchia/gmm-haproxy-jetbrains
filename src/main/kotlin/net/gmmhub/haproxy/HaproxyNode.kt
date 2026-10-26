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
        findOnPath: () -> File? = { findOnPath("node", PathEnvironmentVariableUtil.getPathVariableValue()) },
    ): NodeLookup {
        if (configuredPath.isNotBlank()) {
            val file = File(configuredPath.trim())
            return if (file.isFile && file.canExecute()) NodeLookup.Found(file) else NodeLookup.ConfiguredPathInvalid(configuredPath.trim())
        }
        return findOnPath()?.let { NodeLookup.Found(it) } ?: NodeLookup.NotOnPath
    }

    // PathEnvironmentVariableUtil's finders are deprecated from 263 and their replacement (findFirst) does not exist in 261.
    fun findOnPath(
        name: String,
        pathValue: String?,
        // Empty outside Windows, where the bare name is the executable.
        extensions: List<String> = PathEnvironmentVariableUtil.getWindowsExecutableFileExtensions().ifEmpty { listOf("") },
    ): File? =
        PathEnvironmentVariableUtil.getPathDirs(pathValue.orEmpty()).asSequence()
            .flatMap { dir -> extensions.asSequence().map { File(dir, name + it) } }
            .firstOrNull { it.isFile && it.canExecute() }
}
