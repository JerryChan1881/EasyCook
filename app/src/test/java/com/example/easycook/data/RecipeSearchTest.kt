package com.example.easycook.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeSearchTest {

    private fun recipe(
        id: Int,
        title: String,
        ingredients: List<String> = emptyList(),
        description: String = ""
    ) = Recipe(
        id = id,
        title = title,
        image = "",
        cookingTime = 10,
        ingredientList = ingredients.map { Ingredient("1", it) },
        description = description
    )

    private val carbonara = recipe(1, "Spaghetti Carbonara", listOf("Spaghetti", "Lardons", "Œufs"))
    private val curry = recipe(2, "Curry de poulet", listOf("Poulet", "Lait de coco"))
    private val pizza = recipe(3, "Pizza Margherita", listOf("Farine", "Mozzarella"), "Une pizza napolitaine")
    private val crepes = recipe(4, "Crêpes sucrées", listOf("Farine", "Lait", "Œufs"))
    private val all = listOf(carbonara, curry, pizza, crepes)

    private fun titles(query: String) = RecipeSearch.search(all, query).map { it.title }

    @Test
    fun `une recherche vide renvoie toutes les recettes dans l ordre d origine`() {
        assertEquals(all, RecipeSearch.search(all, "   "))
    }

    @Test
    fun `la recherche ignore la casse et les accents`() {
        assertEquals(listOf("Crêpes sucrées"), titles("CREPES"))
    }

    @Test
    fun `un titre qui commence par la recherche passe avant un titre qui la contient`() {
        val recipes = listOf(recipe(1, "Soupe de pizza"), recipe(2, "Pizza Margherita"))

        assertEquals(
            listOf("Pizza Margherita", "Soupe de pizza"),
            RecipeSearch.search(recipes, "piz").map { it.title }
        )
    }

    @Test
    fun `un titre exact passe en premier`() {
        val recipes = listOf(recipe(1, "Pizza Margherita"), recipe(2, "Pizza"))

        assertEquals("Pizza", RecipeSearch.search(recipes, "pizza").first().title)
    }

    @Test
    fun `un mot du titre qui commence par la recherche passe avant une correspondance au milieu d un mot`() {
        val recipes = listOf(recipe(1, "Tarte aux pommes"), recipe(2, "Gratin de pommes de terre"))

        // "terre" commence un mot dans le gratin, alors que "tarte" ne contient pas "terre"
        assertEquals(listOf("Gratin de pommes de terre"), RecipeSearch.search(recipes, "terre").map { it.title })
        assertEquals(
            listOf("Gratin de pommes de terre", "Tarte aux pommes"),
            RecipeSearch.search(recipes, "pom").map { it.title }
        )
    }

    @Test
    fun `le titre passe avant les ingredients`() {
        // "poulet" est dans le titre du curry, mais seulement en ingredient ailleurs
        val recipes = listOf(recipe(1, "Riz cantonais", listOf("Poulet")), curry)

        assertEquals(
            listOf("Curry de poulet", "Riz cantonais"),
            RecipeSearch.search(recipes, "poulet").map { it.title }
        )
    }

    @Test
    fun `la recherche trouve une recette par ses ingredients`() {
        assertEquals(listOf("Crêpes sucrées", "Pizza Margherita"), titles("farine"))
    }

    @Test
    fun `les ingredients passent avant la description`() {
        val recipes = listOf(
            recipe(1, "Salade", description = "Avec de la mozzarella"),
            recipe(2, "Lasagnes", listOf("Mozzarella"))
        )

        assertEquals(
            listOf("Lasagnes", "Salade"),
            RecipeSearch.search(recipes, "mozza").map { it.title }
        )
    }

    @Test
    fun `la recherche trouve une recette par sa description`() {
        assertEquals(listOf("Pizza Margherita"), titles("napolitaine"))
    }

    @Test
    fun `les lettres dans le bon ordre suffisent en dernier recours`() {
        assertEquals(listOf("Spaghetti Carbonara"), titles("crbnr"))
    }

    @Test
    fun `une recherche sans correspondance ne renvoie rien`() {
        assertTrue(titles("chocolat").isEmpty())
    }

    @Test
    fun `a score egal les recettes sont triees par ordre alphabetique`() {
        val recipes = listOf(recipe(1, "Tarte salée"), recipe(2, "Tarte aux fraises"))

        assertEquals(
            listOf("Tarte aux fraises", "Tarte salée"),
            RecipeSearch.search(recipes, "tar").map { it.title }
        )
    }

    @Test
    fun `a score egal un favori passe avant l ordre alphabetique`() {
        val recipes = listOf(recipe(1, "Tarte aux fraises"), recipe(2, "Tarte salée").copy(isFavorite = true))

        assertEquals(
            listOf("Tarte salée", "Tarte aux fraises"),
            RecipeSearch.search(recipes, "tar").map { it.title }
        )
    }

    @Test
    fun `un favori ne passe pas devant une recette plus pertinente`() {
        val recipes = listOf(recipe(1, "Soupe de pizza").copy(isFavorite = true), recipe(2, "Pizza"))

        assertEquals(
            listOf("Pizza", "Soupe de pizza"),
            RecipeSearch.search(recipes, "pizza").map { it.title }
        )
    }

    @Test
    fun `une seule lettre ne declenche pas la correspondance par lettres dans l ordre`() {
        // "z" n'est dans aucun titre, ingredient ou description du curry
        assertTrue(RecipeSearch.search(listOf(curry), "z").isEmpty())
    }
}
