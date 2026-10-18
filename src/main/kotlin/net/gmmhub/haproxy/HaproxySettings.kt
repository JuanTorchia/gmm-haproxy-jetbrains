package net.gmmhub.haproxy

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service

/** Application-wide settings, sent to the language server as the `haproxy` configuration section. */
@Service(Service.Level.APP)
@State(name = "HaproxySettings", storages = [Storage("haproxy.xml")])
class HaproxySettings : SimplePersistentStateComponent<HaproxySettings.SettingsState>(SettingsState()) {

    class SettingsState : BaseState() {
        var version by string(DEFAULT_VERSION)
        var nodePath by string()
    }

    /** HAProxy version to validate against; unknown stored values fall back to the default. */
    var version: String
        get() = state.version?.takeIf { it in SUPPORTED_VERSIONS } ?: DEFAULT_VERSION
        set(value) {
            state.version = value
        }

    /** Node.js executable; empty means "find node on PATH". */
    var nodePath: String
        get() = state.nodePath.orEmpty()
        set(value) {
            state.nodePath = value.trim().ifEmpty { null }
        }

    fun asLspConfiguration(): Map<String, Any> = mapOf(
        "version" to version,
        "validate" to mapOf("enable" to true),
        "completion" to mapOf("enable" to true),
    )

    companion object {
        const val DEFAULT_VERSION = "3.2"

        /** Same versions and labels as the VS Code `haproxy.version` setting. */
        val SUPPORTED_VERSIONS: Map<String, String> = linkedMapOf(
            "2.4" to "HAProxy 2.4 (EOL)",
            "2.6" to "HAProxy 2.6 LTS (critical fixes only)",
            "2.8" to "HAProxy 2.8 LTS (critical fixes only)",
            "3.0" to "HAProxy 3.0 LTS",
            "3.1" to "HAProxy 3.1 (EOL)",
            "3.2" to "HAProxy 3.2 LTS (recommended)",
            "3.3" to "HAProxy 3.3",
            "3.4" to "HAProxy 3.4 LTS (latest LTS)",
        )

        fun getInstance(): HaproxySettings = service()
    }
}
