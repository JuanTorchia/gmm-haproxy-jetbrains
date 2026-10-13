package net.gmmhub.haproxy

import com.intellij.openapi.extensions.PluginAware
import com.intellij.openapi.extensions.PluginDescriptor
import org.jetbrains.plugins.textmate.api.TextMateBundleProvider
import org.jetbrains.plugins.textmate.api.TextMateBundleProvider.PluginBundle

class HaproxyTextMateBundleProvider : TextMateBundleProvider, PluginAware {
    private lateinit var plugin: PluginDescriptor

    override fun setPluginDescriptor(pluginDescriptor: PluginDescriptor) {
        plugin = pluginDescriptor
    }

    override fun getBundles(): List<PluginBundle> =
        listOf(PluginBundle("HAProxy", HaproxyPlugin.textMateBundle(plugin.pluginPath)))
}
