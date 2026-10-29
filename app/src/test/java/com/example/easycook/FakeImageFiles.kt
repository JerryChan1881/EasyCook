package com.example.easycook

import com.example.easycook.data.RecipeImageFiles

/** Photos en memoire au lieu du disque ; enregistre aussi les suppressions demandees. */
class FakeImageFiles(
    val files: MutableMap<String, ByteArray> = mutableMapOf()
) : RecipeImageFiles {
    val deleted = mutableListOf<String>()
    var usedNamesAtCleanup: Set<String>? = null
    private var savedCount = 0

    override fun read(name: String): ByteArray? = files[name]

    override fun save(bytes: ByteArray): String? {
        // Comme le vrai stockage, refuse ce qui n'est pas une image (ici : un contenu vide)
        if (bytes.isEmpty()) return null
        val name = "recue_${++savedCount}.jpg"
        files[name] = bytes
        return name
    }

    override fun delete(name: String) {
        deleted += name
    }

    override fun deleteUnused(usedNames: Set<String>) {
        usedNamesAtCleanup = usedNames
    }
}
