package com.example.easycook

/** Routes de navigation de l'application, centralisées pour éviter les fautes de frappe. */
object Routes {
    const val ARG_RECIPE_ID = "recipeId"
    const val ARG_PROFILE_ID = "profileId"

    const val HOME = "homeScreen"
    const val ADD_RECIPE = "addRecipeScreen/{$ARG_PROFILE_ID}"
    const val RECIPE_DETAIL = "recipeDetailScreen/{$ARG_RECIPE_ID}"
    const val STEP_BY_STEP = "stepByStepScreen/{$ARG_RECIPE_ID}"
    const val SHARE = "shareScreen/{$ARG_PROFILE_ID}"

    /** La recette sera créée dans ce profil (« Mon profil » ou un profil importé). */
    fun addRecipe(profileId: Int) = "addRecipeScreen/$profileId"
    fun recipeDetail(recipeId: Int) = "recipeDetailScreen/$recipeId"
    fun stepByStep(recipeId: Int) = "stepByStepScreen/$recipeId"

    /** Partage par QR code de recettes de ce profil. */
    fun share(profileId: Int) = "shareScreen/$profileId"
}
