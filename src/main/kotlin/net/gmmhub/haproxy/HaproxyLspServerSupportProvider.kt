package net.gmmhub.haproxy

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.extensions.PluginAware
import com.intellij.openapi.extensions.PluginDescriptor
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
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
        when (val node = HaproxyNode.resolve(HaproxySettings.getInstance().nodePath)) {
            is NodeLookup.Found -> {
                val serverScript = HaproxyPlugin.serverScript(plugin.pluginPath)
                serverStarter.ensureServerStarted(HaproxyLspServerDescriptor(project, node.executable, serverScript))
            }
            is NodeLookup.ConfiguredPathInvalid -> notifyNodeUnavailable(
                project,
                "The configured Node.js executable <code>${node.path}</code> does not exist or is not executable.",
            )
            NodeLookup.NotOnPath -> notifyNodeUnavailable(
                project,
                "Node.js was not found on PATH. Install Node.js (LTS) or set its path in the HAProxy settings.",
            )
        }
    }

    private fun notifyNodeUnavailable(project: Project, reason: String) {
        if (project.getUserData(NODE_WARNING_SHOWN) == true) return
        project.putUserData(NODE_WARNING_SHOWN, true)
        NotificationGroupManager.getInstance()
            .getNotificationGroup("HAProxy Config")
            .createNotification(
                "HAProxy language server unavailable",
                "$reason Validation, completion, hover, and navigation need it. Syntax highlighting still works.",
                NotificationType.WARNING,
            )
            .addAction(NotificationAction.createSimpleExpiring("Configure Node.js…") {
                ShowSettingsUtil.getInstance().showSettingsDialog(project, HaproxyConfigurable::class.java)
            })
            .notify(project)
    }

    companion object {
        private val NODE_WARNING_SHOWN = Key.create<Boolean>("haproxy.node.warning.shown")

        /** Lets a settings change report a still-missing Node.js again. */
        fun resetNodeWarnings() {
            ProjectManager.getInstance().openProjects.forEach { it.putUserData(NODE_WARNING_SHOWN, null) }
        }
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
        if (item.section == "haproxy") HaproxySettings.getInstance().asLspConfiguration() else null
}
