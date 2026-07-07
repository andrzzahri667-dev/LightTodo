package com.zahri.lighttodo.ui.edit

import com.zahri.lighttodo.test.sourceFile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditSaveLifecycleSourceTest {

    @Test
    fun editScreenWaitsForSaveAndDeleteBeforeNavigatingBack() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/feature/todoedit/EditScreen.kt")
            .readText()

        assertTrue(source.contains("import androidx.compose.runtime.rememberCoroutineScope"))
        assertTrue(source.contains("import kotlinx.coroutines.launch"))
        assertTrue(source.contains("val scope = rememberCoroutineScope()"))

        val deleteCall = source.indexOf("vm.delete()")
        val saveCall = source.indexOf("vm.save()")
        val deleteScope = source.lastIndexOf("scope.launch {", deleteCall)
        val saveScope = source.lastIndexOf("scope.launch {", saveCall)
        val deleteBack = source.indexOf("onBack()", deleteCall)
        val permissionCall = source.indexOf("onRequestExactAlarmPermission(state.hasReminder)", saveCall)
        val saveBack = source.indexOf("onBack()", permissionCall)

        assertTrue("delete must run inside screen coroutine scope", deleteScope >= 0 && deleteScope < deleteCall)
        assertTrue("delete must finish before navigating back", deleteCall < deleteBack)
        assertTrue("save must use a separate screen coroutine scope", saveScope > deleteCall && saveScope < saveCall)
        assertTrue("save must finish before requesting settings and navigating back", saveCall < permissionCall && permissionCall < saveBack)
    }

    @Test
    fun editViewModelSaveAndDeleteAreAwaitableAndNonCancellable() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/feature/todoedit/EditViewModel.kt")
            .readText()

        assertTrue(source.contains("import kotlinx.coroutines.NonCancellable"))
        assertTrue(source.contains("import kotlinx.coroutines.Dispatchers"))
        assertTrue(source.contains("import kotlinx.coroutines.withContext"))
        assertTrue(source.contains("suspend fun save()"))
        assertTrue(source.contains("suspend fun delete()"))

        val saveBody = source.substring(
            source.indexOf("suspend fun save()"),
            source.indexOf("suspend fun delete()")
        )
        val deleteBody = source.substring(source.indexOf("suspend fun delete()"))

        assertTrue(saveBody.contains("withContext(NonCancellable + Dispatchers.IO)"))
        assertTrue(deleteBody.contains("withContext(NonCancellable + Dispatchers.IO)"))
        assertFalse(saveBody.contains("viewModelScope.launch"))
        assertFalse(deleteBody.contains("viewModelScope.launch"))
    }
}
