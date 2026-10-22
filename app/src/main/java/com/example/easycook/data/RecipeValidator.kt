package com.example.easycook.data

/**
 * Validation et normalisation des données d'une recette.
 *
 * Cette classe ne contient aucune dépendance Android : elle peut donc être
 * testée directement sur la JVM, sans émulateur ni double de test.
 * Les erreurs sont donc renvoyées sous forme de [RecipeError] ; c'est l'UI
 * qui les traduit en texte via les ressources.
 */
enum class RecipeError {
    TITLE_REQUIRED,
    COOKING_TIME_INVALID,
    NO_INGREDIENT
}

object RecipeValidator {

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
     * Le texte d'un ingrédient est libre : seule une ligne entièrement vide est retirée.
     * L'ordre des ingrédients est conservé.
     */
    fun cleanIngredients(ingredients: List<Ingredient>): List<Ingredient> =
        ingredients
            .filterNot { isBlankIngredient(it) }
            .map { Ingredient(quantity = it.quantity.trim(), name = it.name.trim()) }

    /**
     * Retire les étapes laissées vides et nettoie les espaces.
     * L'ordre des étapes est conservé.
     */
    fun cleanSteps(steps: List<String>): List<String> =
        steps.map { it.trim() }.filter { it.isNotBlank() }

    /**
     * Indique si une ligne d'ingrédient est complètement vide.
     */
    fun isBlankIngredient(ingredient: Ingredient): Boolean =
        ingredient.quantity.isBlank() && ingredient.name.isBlank()

    /**
     * Valide les champs d'une recette en création ou en modification.
     *
     * Retourne l'erreur rencontrée, ou null si tout est valide.
     * Les règles sont évaluées dans l'ordre d'importance, la première
     * violation rencontrée l'emporte.
     */
    fun validate(
        title: String,
        rawCookingTime: String,
        ingredients: List<Ingredient>
    ): RecipeError? {
        if (title.isBlank()) {
            return RecipeError.TITLE_REQUIRED
        }

        if (parseCookingTime(rawCookingTime) == null) {
            return RecipeError.COOKING_TIME_INVALID
        }

        // Les lignes vides sont ignorées, comme pour les étapes
        if (cleanIngredients(ingredients).isEmpty()) {
            return RecipeError.NO_INGREDIENT
        }

        return null
    }
}
