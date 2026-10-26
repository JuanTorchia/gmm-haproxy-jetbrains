package net.gmmhub.haproxy

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.DialogPanel
import com.intellij.platform.lsp.api.LspClientManager
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.listCellRenderer.textListCellRenderer
import com.intellij.ui.dsl.listCellRenderer.textListCellRenderer

/** Settings | Languages & Frameworks | HAProxy. */
class HaproxyConfigurable : BoundConfigurable("HAProxy") {
    private val settings = HaproxySettings.getInstance()

    override fun createPanel(): DialogPanel = panel {
        row("HAProxy version:") {
            comboBox(
                HaproxySettings.SUPPORTED_VERSIONS.keys,
                textListCellRenderer { HaproxySettings.SUPPORTED_VERSIONS[it] ?: it },
            ).bindItem(
                { settings.version },
                { settings.version = it ?: HaproxySettings.DEFAULT_VERSION },
            ).comment("Directives that do not exist, or are deprecated, in this version are reported.")
        }
        row("Node.js executable:") {
            textFieldWithBrowseButton(
                FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor().withTitle("Select Node.js Executable"),
            ).bindText(settings::nodePath)
                .align(AlignX.FILL)
                .comment("Leave empty to use <code>node</code> from PATH. Node.js runs the language server.")
        }
    }

    override fun apply() {
        super.apply()
        HaproxyLspIntegrationProvider.resetNodeWarnings()
        ProjectManager.getInstance().openProjects.forEach {
            LspClientManager.getInstance(it).stopAndRestartClientsIfNeeded(HaproxyLspIntegrationProvider::class.java)
        }
    }
}
