package com.example.easycook.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeValidatorTest {

    private val validIngredients = listOf(
        Ingredient("200 g", "Spaghetti"),
        Ingredient("50 g", "Parmesan")
    )

    // --- parseCookingTime ---

    @Test
    fun `parseCookingTime retourne la valeur pour un nombre valide`() {
        assertEquals(30, RecipeValidator.parseCookingTime("30"))
    }

    @Test
    fun `parseCookingTime ignore les espaces`() {
        assertEquals(45, RecipeValidator.parseCookingTime("  45  "))
    }

    @Test
    fun `parseCookingTime retourne null pour une valeur non numerique`() {
        assertNull(RecipeValidator.parseCookingTime("abc"))
    }

    @Test
    fun `parseCookingTime retourne null pour zero`() {
        assertNull(RecipeValidator.parseCookingTime("0"))
    }

    @Test
    fun `parseCookingTime retourne null pour une valeur negative`() {
        assertNull(RecipeValidator.parseCookingTime("-10"))
    }

    @Test
    fun `parseCookingTime retourne null pour une chaine vide`() {
        assertNull(RecipeValidator.parseCookingTime(""))
    }

    // --- Temps de cuisson facultatif ---

    @Test
    fun `cookingTimeOrNone renvoie la valeur saisie`() {
        assertEquals(30, RecipeValidator.cookingTimeOrNone("30"))
    }

    @Test
    fun `cookingTimeOrNone renvoie NO_COOKING_TIME pour un champ vide`() {
        assertEquals(Recipe.NO_COOKING_TIME, RecipeValidator.cookingTimeOrNone("  "))
    }

    @Test
    fun `formatCookingTime affiche la valeur ou un champ vide`() {
        assertEquals("45", RecipeValidator.formatCookingTime(45))
        assertEquals("", RecipeValidator.formatCookingTime(Recipe.NO_COOKING_TIME))
    }

    @Test
    fun `hasCookingTime indique si le temps est renseigne`() {
        val recipe = Recipe(id = 1, title = "Salade", image = "", cookingTime = 0, ingredientList = emptyList())

        assertTrue(!recipe.hasCookingTime)
        assertTrue(recipe.copy(cookingTime = 15).hasCookingTime)
    }

    @Test
    fun `validate accepte un temps de cuisson vide`() {
        assertNull(RecipeValidator.validate(title = "Salade", rawCookingTime = "", ingredients = validIngredients))
        assertNull(RecipeValidator.validate(title = "Salade", rawCookingTime = "   ", ingredients = validIngredients))
    }

    @Test
    fun `validate refuse un temps de cuisson nul ou negatif`() {
        assertEquals(
            RecipeError.COOKING_TIME_INVALID,
            RecipeValidator.validate(title = "Salade", rawCookingTime = "0", ingredients = validIngredients)
        )
        assertEquals(
            RecipeError.COOKING_TIME_INVALID,
            RecipeValidator.validate(title = "Salade", rawCookingTime = "-5", ingredients = validIngredients)
        )
    }

    // --- cleanIngredients ---

    @Test
    fun `cleanIngredients supprime les lignes entierement vides`() {
        val ingredients = listOf(
            Ingredient("200 g", "Spaghetti"),
            Ingredient("", ""),
            Ingredient("50 g", "Parmesan")
        )

        val cleaned = RecipeValidator.cleanIngredients(ingredients)

        assertEquals(2, cleaned.size)
        assertEquals(Ingredient("200 g", "Spaghetti"), cleaned[0])
        assertEquals(Ingredient("50 g", "Parmesan"), cleaned[1])
    }

    @Test
    fun `cleanIngredients garde une ligne avec seulement une quantite`() {
        val cleaned = RecipeValidator.cleanIngredients(listOf(Ingredient("200 g", "")))

        assertEquals(listOf(Ingredient("200 g", "")), cleaned)
    }

    @Test
    fun `cleanIngredients garde une ligne avec seulement un nom`() {
        val cleaned = RecipeValidator.cleanIngredients(listOf(Ingredient("", "Sel et poivre")))

        assertEquals(listOf(Ingredient("", "Sel et poivre")), cleaned)
    }

    @Test
    fun `cleanIngredients nettoie les espaces`() {
        val cleaned = RecipeValidator.cleanIngredients(listOf(Ingredient(" 200 g ", " Spaghetti ")))

        assertEquals(Ingredient("200 g", "Spaghetti"), cleaned.single())
    }

    @Test
    fun `cleanIngredients conserve l ordre original`() {
        val cleaned = RecipeValidator.cleanIngredients(
            listOf(
                Ingredient("1", "Sel"),
                Ingredient("2", "Poivre"),
                Ingredient("3", "Sucre")
            )
        )

        assertEquals(listOf("Sel", "Poivre", "Sucre"), cleaned.map { it.name })
    }

    // --- cleanSteps ---

    @Test
    fun `cleanSteps supprime les etapes vides et nettoie`() {
        val cleaned = RecipeValidator.cleanSteps(
            listOf("  Bouillir l'eau  ", "", "   ", "Melanger les oeufs")
        )

        assertEquals(listOf("Bouillir l'eau", "Melanger les oeufs"), cleaned)
    }

    @Test
    fun `cleanSteps sur une liste vide retourne une liste vide`() {
        assertTrue(RecipeValidator.cleanSteps(emptyList()).isEmpty())
    }

    @Test
    fun `cleanSteps conserve l ordre des etapes`() {
        val cleaned = RecipeValidator.cleanSteps(listOf("Etape 1", "Etape 2", "Etape 3"))

        assertEquals(listOf("Etape 1", "Etape 2", "Etape 3"), cleaned)
    }

    // --- isBlankIngredient ---

    @Test
    fun `isBlankIngredient detecte une ligne vide`() {
        assertTrue(RecipeValidator.isBlankIngredient(Ingredient("", "")))
    }

    @Test
    fun `isBlankIngredient detecte une ligne a moitié remplie`() {
        assertTrue(!RecipeValidator.isBlankIngredient(Ingredient("200 g", "")))
    }

    // --- validate ---

    @Test
    fun `validate accepte une recette complete`() {
        val error = RecipeValidator.validate(
            title = "Spaghetti Carbonara",
            rawCookingTime = "30",
            ingredients = validIngredients
        )

        assertNull(error)
    }

    @Test
    fun `validate refuse un titre vide`() {
        val error = RecipeValidator.validate(
            title = "   ",
            rawCookingTime = "30",
            ingredients = validIngredients
        )

        assertEquals(RecipeError.TITLE_REQUIRED, error)
    }

    @Test
    fun `validate refuse un temps de cuisson invalide`() {
        val error = RecipeValidator.validate(
            title = "Pizza",
            rawCookingTime = "beaucoup",
            ingredients = validIngredients
        )

        assertEquals(RecipeError.COOKING_TIME_INVALID, error)
    }

    @Test
    fun `validate refuse une recette sans aucun ingredient`() {
        val error = RecipeValidator.validate(
            title = "Pizza",
            rawCookingTime = "30",
            ingredients = listOf(Ingredient("", ""))
        )

        assertEquals(RecipeError.NO_INGREDIENT, error)
    }

    @Test
    fun `validate accepte un ingredient en texte libre`() {
        val error = RecipeValidator.validate(
            title = "Pizza",
            rawCookingTime = "30",
            ingredients = listOf(Ingredient("250 g", ""), Ingredient("", "Sel, poivre & herbes !"))
        )

        assertNull(error)
    }

    @Test
    fun `validate ignore les lignes d ingredient vides`() {
        val error = RecipeValidator.validate(
            title = "Pizza",
            rawCookingTime = "30",
            ingredients = listOf(Ingredient("", ""), Ingredient("125 g", "Mozzarella"), Ingredient(" ", " "))
        )

        assertNull(error)
    }

    @Test
    fun `validate signale le titre manquant avant le temps de cuisson`() {
        val error = RecipeValidator.validate(
            title = "",
            rawCookingTime = "abc",
            ingredients = validIngredients
        )

        assertEquals(RecipeError.TITLE_REQUIRED, error)
    }

    @Test
    fun `validate accepte une recette dont le titre est rempli avec des espaces`() {
        val error = RecipeValidator.validate(
            title = "  ",
            rawCookingTime = "30",
            ingredients = validIngredients
        )

        assertEquals(RecipeError.TITLE_REQUIRED, error)
    }

    @Test
    fun `validate accepte plusieurs recettes differentes`() {
        val recipes = listOf(
            Triple("Carbonara", "30", validIngredients),
            Triple("Curry", "45", listOf(Ingredient("300 g", "Chicken"))),
            Triple("Pizza", "40", listOf(Ingredient("250 g", "Flour")))
        )

        recipes.forEach { (title, time, ingredients) ->
            assertNull(RecipeValidator.validate(title, time, ingredients))
        }
    }
}
