package com.example.easycook.data

import org.junit.Assert.assertEquals
import org.junit.Test

class IngredientParserTest {

    private fun parse(text: String) = IngredientParser.parse(text)

    @Test
    fun `parse separe la quantite avec unite du nom`() {
        assertEquals(Ingredient("200 g", "Spaghetti"), parse("200 g Spaghetti"))
    }

    @Test
    fun `parse accepte une unite collee au nombre`() {
        assertEquals(Ingredient("200g", "Spaghetti"), parse("200g Spaghetti"))
    }

    @Test
    fun `parse reconnait kg avant g`() {
        assertEquals(Ingredient("1 kg", "Pommes de terre"), parse("1 kg Pommes de terre"))
    }

    @Test
    fun `parse accepte un nombre sans unite`() {
        assertEquals(Ingredient("2", "Œufs"), parse("2 Œufs"))
    }

    @Test
    fun `parse accepte les decimales fractions et intervalles`() {
        assertEquals(Ingredient("1,5 l", "Lait"), parse("1,5 l Lait"))
        assertEquals(Ingredient("1/2", "Citron"), parse("1/2 Citron"))
        assertEquals(Ingredient("2-3", "Tomates"), parse("2-3 Tomates"))
        assertEquals(Ingredient("2-3 gousses", "d'ail"), parse("2-3 gousses d'ail"))
    }

    @Test
    fun `parse reconnait les unites de cuisine`() {
        assertEquals(Ingredient("2 c. à s.", "Huile d'olive"), parse("2 c. à s. Huile d'olive"))
        assertEquals(Ingredient("1 pincée", "Sel"), parse("1 pincée Sel"))
        assertEquals(Ingredient("2 tbsp", "Curry"), parse("2 tbsp Curry"))
    }

    @Test
    fun `parse ne prend pas le debut d un mot pour une unite`() {
        // "g" ne doit pas etre lu comme unite dans "gruyère"
        assertEquals(Ingredient("100", "gruyère râpé"), parse("100 gruyère râpé"))
    }

    @Test
    fun `parse ignore la casse des unites`() {
        assertEquals(Ingredient("200 G", "Farine"), parse("200 G Farine"))
    }

    @Test
    fun `parse sans nombre en tete garde tout comme nom`() {
        assertEquals(Ingredient("", "Sel et poivre"), parse("Sel et poivre"))
    }

    @Test
    fun `parse d une quantite seule laisse le nom vide`() {
        assertEquals(Ingredient("200 g", ""), parse("200 g"))
    }

    @Test
    fun `parse nettoie les espaces`() {
        assertEquals(Ingredient("50 g", "Parmesan"), parse("   50 g   Parmesan  "))
    }

    @Test
    fun `parse d une saisie vide donne un ingredient vide`() {
        assertEquals(Ingredient("", ""), parse("   "))
    }

    @Test
    fun `format recompose la saisie`() {
        assertEquals("200 g Spaghetti", IngredientParser.format(Ingredient("200 g", "Spaghetti")))
        assertEquals("Sel", IngredientParser.format(Ingredient("", "Sel")))
    }

    @Test
    fun `format puis parse redonne l ingredient d origine`() {
        val ingredients = listOf(
            Ingredient("200 g", "Spaghetti"),
            Ingredient("2", "Œufs"),
            Ingredient("200 ml", "Coconut milk"),
            Ingredient("2 tbsp", "Curry")
        )

        ingredients.forEach { ingredient ->
            assertEquals(ingredient, parse(IngredientParser.format(ingredient)))
        }
    }
}
