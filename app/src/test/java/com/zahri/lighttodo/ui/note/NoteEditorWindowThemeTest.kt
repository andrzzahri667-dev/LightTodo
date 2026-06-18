package com.zahri.lighttodo.feature.noteeditor

import com.zahri.lighttodo.test.sourceFile

import org.junit.Assert.assertEquals
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class NoteEditorWindowThemeTest {
    @Test
    fun manifestUsesTransparentNoPreviewThemeForNoteEditorStartingWindow() {
        val manifest = xmlDocument(sourceFile("app/src/main/AndroidManifest.xml"))
        val noteActivity = manifest.getElementsByTagName("activity")
            .asElements()
            .first { it.getAttribute("android:name") == ".feature.noteeditor.NoteEditActivity" }

        assertEquals("@style/Theme.LightTodo.NoteTransform", noteActivity.getAttribute("android:theme"))
    }

    @Test
    fun noteTransformThemeDisablesSystemPreviewAndUsesTransparentBackground() {
        listOf(
            "app/src/main/res/values/themes.xml",
            "app/src/main/res/values-night/themes.xml"
        ).forEach { path ->
            val themes = xmlDocument(sourceFile(path))
            val noteTransform = themes.getElementsByTagName("style")
                .asElements()
                .first { it.getAttribute("name") == "Theme.LightTodo.NoteTransform" }

            assertEquals("Theme.LightTodo", noteTransform.getAttribute("parent"))
            assertEquals("true", noteTransform.itemValue("android:windowDisablePreview"))
            assertEquals("@android:color/transparent", noteTransform.itemValue("android:windowBackground"))
            assertEquals("false", noteTransform.itemValue("android:backgroundDimEnabled"))
        }
    }

    private fun xmlDocument(file: File) =
        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)

    private fun org.w3c.dom.NodeList.asElements(): List<Element> =
        List(length) { item(it) }.filterIsInstance<Element>()

    private fun Element.itemValue(name: String): String =
        getElementsByTagName("item")
            .asElements()
            .first { it.getAttribute("name") == name }
            .textContent
            .trim()
}
