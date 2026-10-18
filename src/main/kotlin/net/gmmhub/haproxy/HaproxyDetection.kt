package net.gmmhub.haproxy

/** File name and content rules shared with the VS Code extension (0.6.x). */
internal object HaproxyDetection {
    /** Lines inspected when deciding whether a generic .cfg/.conf file is HAProxy. */
    const val LINE_LIMIT = 200

    private val EXACT_NAMES = setOf("haproxy.cfg", "haproxy.conf")
    private val PREFIXED_CFG = Regex("""haproxy.*\.cfg""")
    private val CANDIDATE_EXTENSION = Regex(""".*\.(cfg|conf)""", RegexOption.IGNORE_CASE)

    // HAProxy requires section headers at column 0. A keyword followed by `=` or `:` is a
    // `key = value` setting (php-fpm, dovecot, ini), not a header.
    private val SECTION_HEADER = Regex(
        """^(global|defaults|frontend|backend|listen|userlist|peers|resolvers|mailers|ring|log-forward|program|http-errors|cache|acme)(?=\s|$)(?!\s*[=:])""",
    )

    /** `haproxy.cfg`, `haproxy.conf`, `haproxy*.cfg`, or any `.cfg`/`.conf` inside a `haproxy/` directory. */
    fun matchesByName(name: String, parentName: String?): Boolean =
        name in EXACT_NAMES ||
            PREFIXED_CFG.matches(name) ||
            (parentName == "haproxy" && (name.endsWith(".cfg") || name.endsWith(".conf")))

    /** Only `.cfg` and `.conf` files are inspected for section headers. */
    fun isContentCandidate(name: String): Boolean = CANDIDATE_EXTENSION.matches(name)

    fun looksLikeHaproxyConfig(text: CharSequence): Boolean =
        text.lineSequence().take(LINE_LIMIT).any { SECTION_HEADER.containsMatchIn(it) }
}
