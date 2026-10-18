package net.gmmhub.haproxy

import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.io.File

class HaproxyFilesTest : BasePlatformTestCase() {
    private lateinit var root: File

    override fun setUp() {
        super.setUp()
        root = FileUtil.createTempDirectory("haproxy-files", null)
    }

    override fun tearDown() {
        try {
            FileUtil.delete(root)
        } finally {
            super.tearDown()
        }
    }

    private fun localFile(path: String, text: String): VirtualFile {
        val file = File(root, path)
        file.parentFile.mkdirs()
        file.writeText(text)
        return LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)!!
    }

    fun `test recognizes files by name`() {
        assertTrue(HaproxyFiles.isHaproxyConfig(localFile("haproxy.cfg", "# empty\n")))
        assertTrue(HaproxyFiles.isHaproxyConfig(localFile("haproxy/edge.conf", "# empty\n")))
    }

    fun `test recognizes generic cfg files by section headers`() {
        assertTrue(HaproxyFiles.isHaproxyConfig(localFile("lb/edge.cfg", "global\n    daemon\n")))
    }

    fun `test leaves other cfg and conf files alone`() {
        assertFalse(HaproxyFiles.isHaproxyConfig(localFile("setup.cfg", "[metadata]\nname = demo\n")))
        assertFalse(HaproxyFiles.isHaproxyConfig(localFile("www.conf", "[www]\nlisten = 127.0.0.1:9000\n")))
        assertFalse(HaproxyFiles.isHaproxyConfig(localFile("notes.txt", "global\n")))
    }

    fun `test redetects after the file changes`() {
        val file = localFile("edge.cfg", "[metadata]\n")
        assertFalse(HaproxyFiles.isHaproxyConfig(file))
        File(file.path).writeText("frontend web\n    bind :80\n")
        file.refresh(false, false)
        assertTrue(HaproxyFiles.isHaproxyConfig(file))
    }

    fun `test ignores files outside the local file system`() {
        assertFalse(HaproxyFiles.isHaproxyConfig(LightVirtualFile("edge.cfg", "global\n")))
    }

    fun `test overrider keeps files that are already TextMate`() {
        val file = localFile("lb/edge.cfg", "global\n")
        val byName = FileTypeManager.getInstance().getFileTypeByFileName(file.nameSequence)
        val overridden = HaproxyFileTypeOverrider().getOverriddenFileType(file)
        if (byName.name == TEXTMATE_FILE_TYPE) {
            assertNull(overridden)
        } else {
            assertEquals(TEXTMATE_FILE_TYPE, overridden?.name)
        }
    }
}
