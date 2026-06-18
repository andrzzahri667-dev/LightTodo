package com.zahri.lighttodo.data

import com.zahri.lighttodo.test.sourceFile

import org.junit.Assert.assertFalse
import org.junit.Test

class TodoDaoSourceTest {

    @Test
    fun todoDaoDoesNotExposeUnusedReplaceAllHelper() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/data/local/Daos.kt")
            .readText()

        assertFalse(source.contains("suspend fun replaceAll("))
    }
}
