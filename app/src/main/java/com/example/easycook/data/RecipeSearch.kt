package com.example.easycook.data

import java.text.Normalizer

/**
 * Recherche de recettes classée par pertinence.
 *
 * La comparaison ignore la casse et les accents ("creme" trouve "Crème").
 * Chaque recette reçoit un score selon la meilleure correspondance trouvée ;
 * les recettes sans correspondance sont écartées. Comme [RecipeValidator],
 * cet objet n'a aucune dépendance Android et se teste directement sur la JVM.
 */
object RecipeSearch {

    private const val SCORE_EXACT_TITLE = 100
    private const val SCORE_TITLE_PREFIX = 80
    private const val SCORE_WORD_PREFIX = 60
    private const val SCORE_TITLE_CONTAINS = 40
    private const val SCORE_INGREDIENT = 20
    private const val SCORE_DESCRIPTION = 10
    private const val SCORE_LETTERS_IN_ORDER = 5

    private val wordSeparator = Regex("[^a-z0-9]+")

    /**
     * Retourne les recettes correspondant à [query], de la plus pertinente à la moins
     * pertinente. À score égal, les favoris passent en premier, puis l'ordre alphabétique.
     * Une recherche vide renvoie toutes les recettes dans leur ordre d'origine.
     */
    fun search(recipes: List<Recipe>, query: String): List<Recipe> {
        val normalizedQuery = normalize(query)
        if (normalizedQuery.isEmpty()) {
            return recipes
        }

        return recipes
            .map { recipe -> recipe to score(recipe, normalizedQuery) }
            .filter { (_, score) -> score > 0 }
            .sortedWith(
                compareByDescending<Pair<Recipe, Int>> { (_, score) -> score }
                    .thenByDescending { (recipe, _) -> recipe.isFavorite }
                    .thenBy { (recipe, _) -> normalize(recipe.title) }
            )
            .map { (recipe, _) -> recipe }
    }

    /** Score de pertinence d'une recette pour une recherche déjà normalisée (0 = aucune correspondance). */
    fun score(recipe: Recipe, normalizedQuery: String): Int {
        val title = normalize(recipe.title)

        return when {
            title == normalizedQuery -> SCORE_EXACT_TITLE
            title.startsWith(normalizedQuery) -> SCORE_TITLE_PREFIX
            title.split(wordSeparator).any { it.startsWith(normalizedQuery) } -> SCORE_WORD_PREFIX
            normalizedQuery in title -> SCORE_TITLE_CONTAINS
            recipe.ingredientList.any { normalizedQuery in normalize(it.name) } -> SCORE_INGREDIENT
            normalizedQuery in normalize(recipe.description) -> SCORE_DESCRIPTION
            hasLettersInOrder(title, normalizedQuery) -> SCORE_LETTERS_IN_ORDER
            else -> 0
        }
    }

    /** Met en minuscules, retire les accents et les espaces superflus. */
    fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .lowercase()
            .trim()

    /**
     * Indique si toutes les lettres de [query] apparaissent dans [text] dans le même ordre,
     * pas forcément côte à côte : "crbnr" correspond à "carbonara".
     */
    private fun hasLettersInOrder(text: String, query: String): Boolean {
        val letters = query.filterNot { it.isWhitespace() }
        if (letters.length < 2) {
            return false
        }

        var index = 0
        for (char in text) {
            if (char == letters[index]) {
                index++
                if (index == letters.length) {
                    return true
                }
            }
        }
        return false
    }
}
