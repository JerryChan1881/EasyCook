package com.example.easycook

/** Routes de navigation de l'application, centralisées pour éviter les fautes de frappe. */
object Routes {
    const val ARG_RECIPE_ID = "recipeId"

    const val HOME = "homeScreen"
    const val ADD_RECIPE = "addRecipeScreen"
    const val RECIPE_DETAIL = "recipeDetailScreen/{$ARG_RECIPE_ID}"
    const val STEP_BY_STEP = "stepByStepScreen/{$ARG_RECIPE_ID}"

    fun recipeDetail(recipeId: Int) = "recipeDetailScreen/$recipeId"
    fun stepByStep(recipeId: Int) = "stepByStepScreen/$recipeId"
}
