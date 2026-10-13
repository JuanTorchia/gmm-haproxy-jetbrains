package net.gmmhub.haproxy

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.extensions.PluginAware
import com.intellij.openapi.extensions.PluginDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServerSupportProvider
import com.intellij.platform.lsp.api.ProjectWideLspServerDescriptor
import org.eclipse.lsp4j.ConfigurationItem
import java.io.File
import java.nio.file.Path

class HaproxyLspServerSupportProvider : LspServerSupportProvider, PluginAware {
    private lateinit var plugin: PluginDescriptor

    override fun setPluginDescriptor(pluginDescriptor: PluginDescriptor) {
        plugin = pluginDescriptor
    }

    override fun fileOpened(
        project: Project,
        file: VirtualFile,
        serverStarter: LspServerSupportProvider.LspServerStarter,
    ) {
        if (!HaproxyFiles.isHaproxyConfig(file)) return
        val node = PathEnvironmentVariableUtil.findInPath("node")
        if (node == null) {
            notifyNodeMissing(project)
            return
        }
        val serverScript = HaproxyPlugin.serverScript(plugin.pluginPath)
        serverStarter.ensureServerStarted(HaproxyLspServerDescriptor(project, node, serverScript))
    }

    private fun notifyNodeMissing(project: Project) {
        if (project.getUserData(NODE_MISSING_NOTIFIED) == true) return
        project.putUserData(NODE_MISSING_NOTIFIED, true)
        NotificationGroupManager.getInstance()
            .getNotificationGroup("HAProxy Config")
            .createNotification(
                "HAProxy language server unavailable",
                "Node.js was not found on PATH. Install Node.js (LTS) and restart the IDE to enable " +
                    "HAProxy validation, completion, hover, and navigation. Syntax highlighting still works.",
                NotificationType.WARNING,
            )
            .notify(project)
    }

    private companion object {
        val NODE_MISSING_NOTIFIED = Key.create<Boolean>("haproxy.node.missing.notified")
    }
}

private class HaproxyLspServerDescriptor(
    project: Project,
    private val node: File,
    private val serverScript: Path,
) : ProjectWideLspServerDescriptor(project, "HAProxy") {

    override fun isSupportedFile(file: VirtualFile): Boolean = HaproxyFiles.isHaproxyConfig(file)

    override fun createCommandLine(): GeneralCommandLine =
        GeneralCommandLine(node.path, serverScript.toString(), "--stdio")

    override fun getLanguageId(file: VirtualFile): String = "haproxy"

    override fun getWorkspaceConfiguration(item: ConfigurationItem): Any? =
        if (item.section == "haproxy") HaproxySettings.asLspConfiguration() else null
}
