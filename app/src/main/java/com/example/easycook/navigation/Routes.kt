package com.example.easycook.navigation

import kotlinx.serialization.Serializable

/**
 * Destinations de l'application, typées : chaque écran déclare ses arguments comme propriétés,
 * vérifiées à la compilation (plus de chaînes "recipeDetailScreen/{recipeId}" à assembler et relire).
 */
sealed interface Route {

    @Serializable
    data object Home : Route

    /** Création d'une recette dans ce profil (« Mon profil » ou un profil importé). */
    @Serializable
    data class AddRecipe(val profileId: Int) : Route

    @Serializable
    data class RecipeDetail(val recipeId: Int) : Route

    @Serializable
    data class StepByStep(val recipeId: Int) : Route

    /** Partage par QR code de recettes de ce profil. */
    @Serializable
    data class ShareRecipes(val profileId: Int) : Route
}
