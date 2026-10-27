package com.example.easycook.data

/**
 * Accès aux fichiers images des recettes.
 *
 * Interface séparée du stockage Android pour que le ViewModel reste testable sur la JVM.
 */
interface RecipeImageFiles {

    /** Supprime l'image d'une recette ; sans effet si le chemin n'est pas une image de l'application. */
    fun delete(path: String)

    /** Supprime les images qu'aucune recette n'utilise (formulaires abandonnés, images remplacées…). */
    fun deleteUnused(usedPaths: Set<String>)

    /** Contenu d'une image de recette, pour l'envoyer à un autre téléphone ; null si illisible. */
    fun read(path: String): ByteArray?

    /** Enregistre une image reçue d'un autre téléphone et renvoie son chemin ; null si ce n'est pas une image. */
    fun save(bytes: ByteArray): String?
}
