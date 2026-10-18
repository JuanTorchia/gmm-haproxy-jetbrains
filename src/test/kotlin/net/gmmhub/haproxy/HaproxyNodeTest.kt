package net.gmmhub.haproxy

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class HaproxyNodeTest {
    @get:Rule
    val temp = TemporaryFolder()

    @Test
    fun `uses PATH when no executable is configured`() {
        val onPath = File("/usr/bin/node")
        assertEquals(NodeLookup.Found(onPath), HaproxyNode.resolve("") { onPath })
        assertEquals(NodeLookup.NotOnPath, HaproxyNode.resolve("  ") { null })
    }

    @Test
    fun `prefers a configured executable over PATH`() {
        val node = temp.newFile("node").apply { setExecutable(true) }
        assertEquals(NodeLookup.Found(node), HaproxyNode.resolve(node.path) { File("/usr/bin/node") })
    }

    @Test
    fun `reports a configured path that is missing or not executable`() {
        val missing = File(temp.root, "missing-node").path
        assertEquals(NodeLookup.ConfiguredPathInvalid(missing), HaproxyNode.resolve(missing) { File("/usr/bin/node") })
        val directory = temp.newFolder("bin").path
        assertEquals(NodeLookup.ConfiguredPathInvalid(directory), HaproxyNode.resolve(directory) { null })
    }
}
