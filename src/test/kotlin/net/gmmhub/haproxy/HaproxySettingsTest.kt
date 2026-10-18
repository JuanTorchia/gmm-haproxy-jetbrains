package net.gmmhub.haproxy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HaproxySettingsTest {
    @Test
    fun `defaults to HAProxy 3_2 and node from PATH`() {
        val settings = HaproxySettings()
        assertEquals("3.2", settings.version)
        assertEquals("", settings.nodePath)
    }

    @Test
    fun `offers the same versions as the VS Code extension`() {
        assertEquals(
            listOf("2.4", "2.6", "2.8", "3.0", "3.1", "3.2", "3.3", "3.4"),
            HaproxySettings.SUPPORTED_VERSIONS.keys.toList(),
        )
    }

    @Test
    fun `falls back to the default for unsupported stored versions`() {
        val settings = HaproxySettings()
        settings.loadState(HaproxySettings.SettingsState().apply { version = "1.8" })
        assertEquals(HaproxySettings.DEFAULT_VERSION, settings.version)
    }

    @Test
    fun `keeps a supported version through a state round trip`() {
        val settings = HaproxySettings()
        settings.version = "3.4"
        settings.nodePath = "  /opt/node/bin/node  "

        val restored = HaproxySettings()
        restored.loadState(settings.state)
        assertEquals("3.4", restored.version)
        assertEquals("/opt/node/bin/node", restored.nodePath)
    }

    @Test
    fun `blank node path is not stored`() {
        val settings = HaproxySettings()
        settings.nodePath = "   "
        assertNull(settings.state.nodePath)
    }

    @Test
    fun `exposes the haproxy section to the language server`() {
        val settings = HaproxySettings()
        settings.version = "2.8"
        assertEquals(
            mapOf(
                "version" to "2.8",
                "validate" to mapOf("enable" to true),
                "completion" to mapOf("enable" to true),
            ),
            settings.asLspConfiguration(),
        )
    }
}
