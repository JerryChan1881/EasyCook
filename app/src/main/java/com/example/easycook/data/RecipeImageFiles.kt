package com.example.easycook.data

/**
 * Accès aux fichiers des photos de recettes.
 *
 * Une photo est désignée par son seul nom de fichier (ex. « 3f2a….jpg »), jamais par un chemin :
 * le dossier qui les contient peut changer, par exemple après une restauration sur un autre appareil.
 *
 * Interface séparée du stockage Android pour que le repository reste testable sur la JVM.
 */
interface RecipeImageFiles {

    /** Supprime une photo ; sans effet si le nom ne désigne pas une photo de l'application. */
    fun delete(name: String)

    /** Supprime les photos qu'aucune recette n'utilise (formulaires abandonnés, photos remplacées…). */
    fun deleteUnused(usedNames: Set<String>)

    /** Contenu d'une photo, pour l'envoyer à un autre téléphone ; null si illisible. */
    fun read(name: String): ByteArray?

    /** Enregistre une photo reçue d'un autre téléphone et renvoie son nom ; null si ce n'est pas une image. */
    fun save(bytes: ByteArray): String?
}
