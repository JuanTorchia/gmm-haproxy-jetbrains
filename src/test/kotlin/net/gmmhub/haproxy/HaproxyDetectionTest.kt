package net.gmmhub.haproxy

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HaproxyDetectionTest {
    @Test
    fun `matches HAProxy file names`() {
        assertTrue(HaproxyDetection.matchesByName("haproxy.cfg", "etc"))
        assertTrue(HaproxyDetection.matchesByName("haproxy.conf", null))
        assertTrue(HaproxyDetection.matchesByName("haproxy-edge.cfg", "conf"))
        assertTrue(HaproxyDetection.matchesByName("edge.cfg", "haproxy"))
        assertTrue(HaproxyDetection.matchesByName("edge.conf", "haproxy"))
    }

    @Test
    fun `does not match other configuration names`() {
        assertFalse(HaproxyDetection.matchesByName("setup.cfg", "project"))
        assertFalse(HaproxyDetection.matchesByName("haproxy-edge.conf", "conf"))
        assertFalse(HaproxyDetection.matchesByName("haproxy.ini", "haproxy"))
        assertFalse(HaproxyDetection.matchesByName("nginx.conf", "nginx"))
    }

    @Test
    fun `only cfg and conf files are content candidates`() {
        assertTrue(HaproxyDetection.isContentCandidate("edge.cfg"))
        assertTrue(HaproxyDetection.isContentCandidate("EDGE.CONF"))
        assertFalse(HaproxyDetection.isContentCandidate("edge.ini"))
        assertFalse(HaproxyDetection.isContentCandidate("edge.cfg.bak"))
    }

    @Test
    fun `detects every HAProxy section header`() {
        listOf(
            "global", "defaults", "frontend web", "backend app", "listen stats", "userlist admins",
            "peers mypeers", "resolvers dns", "mailers alerts", "ring logs", "log-forward syslog",
            "program agent", "http-errors errors", "cache static", "acme le",
        ).forEach { header ->
            assertTrue(header, HaproxyDetection.looksLikeHaproxyConfig("# comment\n$header\n    option httplog\n"))
        }
    }

    @Test
    fun `detects headers with Windows line endings and trailing comments`() {
        assertTrue(HaproxyDetection.looksLikeHaproxyConfig("global\r\n    daemon\r\n"))
        assertTrue(HaproxyDetection.looksLikeHaproxyConfig("defaults # shared timeouts\n"))
    }

    @Test
    fun `ignores key value settings and indented keywords`() {
        assertFalse(HaproxyDetection.looksLikeHaproxyConfig("[global]\nerror_log = /var/log/php-fpm.log\n"))
        assertFalse(HaproxyDetection.looksLikeHaproxyConfig("global = yes\nlisten = 127.0.0.1:9000\n"))
        assertFalse(HaproxyDetection.looksLikeHaproxyConfig("cache: true\n"))
        assertFalse(HaproxyDetection.looksLikeHaproxyConfig("    backend app\n"))
        assertFalse(HaproxyDetection.looksLikeHaproxyConfig("globalsettings\nbackends 3\n"))
        assertFalse(HaproxyDetection.looksLikeHaproxyConfig("Listen 80\nServerName example.org\n"))
    }

    @Test
    fun `inspects only the first lines`() {
        val padding = "# filler\n".repeat(HaproxyDetection.LINE_LIMIT)
        assertFalse(HaproxyDetection.looksLikeHaproxyConfig(padding + "global\n"))
        val almost = "# filler\n".repeat(HaproxyDetection.LINE_LIMIT - 1)
        assertTrue(HaproxyDetection.looksLikeHaproxyConfig(almost + "global\n"))
    }
}
