package com.swipecleaner

import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Test

class StringsKeyParityTest {

    private val localeFolders = listOf(
        "values-es",
        "values-fr",
        "values-de",
        "values-it",
        "values-pt-rBR",
        "values-ru",
        "values-tr",
        "values-id",
        "values-vi",
        "values-hi",
        "values-ar",
        "values-he",
        "values-fa",
        "values-ja",
        "values-ko",
    )

    @Test
    fun batchOneLocalesMatchEnglishKeys() {
        val englishFile = File("src/main/res/values/strings.xml")
        val englishKeys = extractKeys(englishFile)

        localeFolders.forEach { folder ->
            val localeFile = File("src/main/res/$folder/strings.xml")
            val localeKeys = extractKeys(localeFile)

            val missing = englishKeys - localeKeys
            val extra = localeKeys - englishKeys

            assertWithMessage("$folder missing keys: $missing")
                .that(missing)
                .isEmpty()
            assertWithMessage("$folder extra keys: $extra")
                .that(extra)
                .isEmpty()
        }
    }

    private fun extractKeys(file: File): Set<String> {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        }
        val document = factory.newDocumentBuilder().parse(file)
        val nodes = document.getElementsByTagName("string")

        return buildSet {
            for (index in 0 until nodes.length) {
                val name = nodes.item(index).attributes?.getNamedItem("name")?.nodeValue
                if (!name.isNullOrBlank()) {
                    add(name)
                }
            }
        }
    }
}
