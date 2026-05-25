package com.zahri.lighttodo.data

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Test

class TodoDaoSourceTest {

    @Test
    fun todoDaoDoesNotExposeUnusedReplaceAllHelper() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/data/Daos.kt")
            .readText()

        assertFalse(source.contains("suspend fun replaceAll("))
    }

    private fun sourceFile(relativePath: String): File {
        val userDir = requireNotNull(System.getProperty("user.dir"))
        var dir = File(userDir).absoluteFile
        while (true) {
            val candidate = File(dir, relativePath)
            if (candidate.exists()) return candidate
            dir = dir.parentFile ?: break
        }
        error("Could not find $relativePath from $userDir")
    }
}
