package com.example.easycook.data

/**
 * Conversion entre un ingrédient et sa saisie en un seul champ texte,
 * par exemple "200 g Spaghetti" <-> Ingredient(quantity = "200 g", name = "Spaghetti").
 *
 * La quantité est le nombre en tête de saisie, éventuellement suivi d'une unité connue.
 * Sans dépendance Android, comme [RecipeValidator].
 */
object IngredientParser {

    private val units = listOf(
        "kg", "g", "mg", "gramme", "grammes",
        "l", "dl", "cl", "ml", "litre", "litres",
        "c. à s.", "c. à c.", "c.à.s", "c.à.c", "càs", "càc", "cas", "cac",
        "cuillère", "cuillères", "tbsp", "tsp", "cup", "cups",
        "pincée", "pincées", "tranche", "tranches", "gousse", "gousses",
        "feuille", "feuilles", "sachet", "sachets", "boîte", "boîtes",
        "verre", "verres", "bol", "bols", "botte", "bottes"
    )

    // Unités les plus longues d'abord, pour que "kg" soit reconnu avant "g"
    private val unitPattern = units
        .sortedByDescending { it.length }
        .joinToString("|") { Regex.escape(it) }

    // Nombre (200, 1.5, 1,5, 1/2, 2-3), unité facultative, puis le nom
    private val ingredientRegex = Regex(
        "^(\\d+(?:[.,/]\\d+)?(?:\\s*-\\s*\\d+(?:[.,/]\\d+)?)?(?:\\s*(?:$unitPattern)(?=\\s|$))?)\\s*(.*)$",
        RegexOption.IGNORE_CASE
    )

    /**
     * Découpe une saisie en quantité et nom.
     * Une saisie sans nombre en tête est entièrement considérée comme le nom.
     */
    fun parse(text: String): Ingredient {
        val trimmed = text.trim()
        val match = ingredientRegex.matchEntire(trimmed)
            ?: return Ingredient(quantity = "", name = trimmed)

        return Ingredient(
            quantity = match.groupValues[1].trim(),
            name = match.groupValues[2].trim()
        )
    }

    /** Recompose la saisie à afficher dans le champ à partir d'un ingrédient. */
    fun format(ingredient: Ingredient): String =
        listOf(ingredient.quantity, ingredient.name)
            .filter { it.isNotBlank() }
            .joinToString(" ")
}
