package com.example.easycook.data

/**
 * Suppression des fichiers images des recettes.
 *
 * Interface séparée du stockage Android pour que le ViewModel reste testable sur la JVM.
 */
interface RecipeImageCleaner {

    /** Supprime l'image d'une recette ; sans effet si le chemin n'est pas une image de l'application. */
    fun delete(path: String)

    /** Supprime les images qu'aucune recette n'utilise (formulaires abandonnés, images remplacées…). */
    fun deleteUnused(usedPaths: Set<String>)
}
