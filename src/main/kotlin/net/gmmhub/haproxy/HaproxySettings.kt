package net.gmmhub.haproxy

/** Spike placeholder: a real Settings page replaces this. Override with -Dhaproxy.version=3.1. */
internal object HaproxySettings {
    private const val DEFAULT_VERSION = "3.2"

    val version: String
        get() = System.getProperty("haproxy.version", DEFAULT_VERSION)

    fun asLspConfiguration(): Map<String, Any> = mapOf(
        "version" to version,
        "validate" to mapOf("enable" to true),
        "completion" to mapOf("enable" to true),
    )
}
