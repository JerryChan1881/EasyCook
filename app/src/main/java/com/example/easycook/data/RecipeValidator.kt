package com.example.easycook.data

/**
 * Validation et normalisation des données d'une recette.
 *
 * Cette classe ne contient aucune dépendance Android : elle peut donc être
 * testée directement sur la JVM, sans émulateur ni double de test.
 */
object RecipeValidator {

    const val TITLE_REQUIRED = "Le nom de la recette est obligatoire."
    const val COOKING_TIME_INVALID = "Le temps de cuisson doit être un nombre positif."
    const val NO_COMPLETE_INGREDIENT = "Ajoutez au moins un ingrédient complet."
    const val INCOMPLETE_INGREDIENTS = "Complétez ou supprimez les ingrédients vides."

    /**
     * Convertit la saisie du temps de cuisson en entier.
     * Retourne null si la saisie n'est pas un nombre strictement positif.
     */
    fun parseCookingTime(rawValue: String): Int? {
        val parsed = rawValue.trim().toIntOrNull()
        return parsed?.takeIf { it > 0 }
    }

    /**
     * Retire les lignes d'ingrédients laissées vides et nettoie les espaces.
     * L'ordre des ingrédients est conservé.
     */
    fun cleanIngredients(ingredients: List<Ingredient>): List<Ingredient> =
        ingredients
            .filter { it.quantity.isNotBlank() && it.name.isNotBlank() }
            .map { Ingredient(quantity = it.quantity.trim(), name = it.name.trim()) }

    /**
     * Retire les étapes laissées vides et nettoie les espaces.
     * L'ordre des étapes est conservé.
     */
    fun cleanSteps(steps: List<String>): List<String> =
        steps.map { it.trim() }.filter { it.isNotBlank() }

    /**
     * Indique si une ligne d'ingrédient est complètement vide.
     * Une ligne semi-remplie est considérée comme incomplète, pas vide.
     */
    fun isBlankIngredient(ingredient: Ingredient): Boolean =
        ingredient.quantity.isBlank() && ingredient.name.isBlank()

    /**
     * Valide les champs d'une recette en création ou en modification.
     *
     * Retourne le message d'erreur à afficher, ou null si tout est valide.
     * Les règles sont évaluées dans l'ordre d'importance, la première
     * violation rencontrée l'emporte.
     */
    fun validate(
        title: String,
        rawCookingTime: String,
        ingredients: List<Ingredient>
    ): String? {
        if (title.isBlank()) {
            return TITLE_REQUIRED
        }

        if (parseCookingTime(rawCookingTime) == null) {
            return COOKING_TIME_INVALID
        }

        val completeIngredients = cleanIngredients(ingredients)

        if (completeIngredients.isEmpty()) {
            return NO_COMPLETE_INGREDIENT
        }

        if (completeIngredients.size != ingredients.size) {
            return INCOMPLETE_INGREDIENTS
        }

        return null
    }
}
