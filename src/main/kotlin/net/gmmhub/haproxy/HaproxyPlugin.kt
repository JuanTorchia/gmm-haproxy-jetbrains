package net.gmmhub.haproxy

import java.nio.file.Path

/** Layout of the files that prepareSandbox copies next to the plugin jar. */
internal object HaproxyPlugin {
    fun serverScript(pluginHome: Path): Path = pluginHome.resolve("language-server/server.js")

    fun textMateBundle(pluginHome: Path): Path = pluginHome.resolve("textmate/haproxy")
}
