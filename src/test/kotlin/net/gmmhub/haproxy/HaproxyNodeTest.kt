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

    @Test
    fun `finds the first executable in the PATH directories`() {
        val empty = temp.newFolder("empty")
        val notExecutable = temp.newFolder("plain").also { File(it, "node").apply { createNewFile(); setExecutable(false) } }
        val bin = temp.newFolder("usr-bin")
        val node = File(bin, "node").apply { createNewFile(); setExecutable(true) }
        val path = listOf(empty, notExecutable, bin).joinToString(File.pathSeparator)
        assertEquals(node, HaproxyNode.findOnPath("node", path, listOf("")))
        assertEquals(null, HaproxyNode.findOnPath("node", empty.path, listOf("")))
    }

    @Test
    fun `tries each executable extension`() {
        val bin = temp.newFolder("win-bin")
        val node = File(bin, "node.exe").apply { createNewFile(); setExecutable(true) }
        assertEquals(node, HaproxyNode.findOnPath("node", bin.path, listOf(".com", ".exe")))
    }
}
