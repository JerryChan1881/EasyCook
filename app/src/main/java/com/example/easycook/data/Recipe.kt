package com.example.easycook.data

data class Recipe(
    val id: Int,
    val title: String,
    val image: String,
    val cookingTime: Int,
    val ingredientList: List<Ingredient>,
    val description: String = "",
    val isFavorite: Boolean = false,
    /** « Mon profil » ([OWN_PROFILE_ID]) ou profil importé auquel appartient la recette. */
    val profileId: Int = OWN_PROFILE_ID
) {
    /** Le temps de cuisson est facultatif : [NO_COOKING_TIME] signifie qu'il n'a pas été renseigné. */
    val hasCookingTime: Boolean
        get() = cookingTime > NO_COOKING_TIME

    companion object {
        const val NO_COOKING_TIME = 0
    }
}
