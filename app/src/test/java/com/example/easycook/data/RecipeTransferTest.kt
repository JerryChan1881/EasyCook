package com.example.easycook.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeTransferTest {

    private val carbonara = ExportedRecipe(
        title = "Spaghetti Carbonara",
        cookingTime = 30,
        description = "Recette romaine",
        ingredients = listOf(ExportedIngredient("200 g", "Spaghetti"), ExportedIngredient("2", "Œufs")),
        steps = listOf("Faire bouillir l'eau", "Cuire les pâtes")
    )

    private fun decodeSuccess(json: String): RecipeExport {
        val result = RecipeTransfer.decode(json)
        assertTrue("Decodage attendu en succes, obtenu $result", result is DecodeResult.Success)
        return (result as DecodeResult.Success).export
    }

    private fun decodeError(json: String): ImportError {
        val result = RecipeTransfer.decode(json)
        assertTrue("Decodage attendu en echec, obtenu $result", result is DecodeResult.Failure)
        return (result as DecodeResult.Failure).error
    }

    // --- Aller-retour ---

    @Test
    fun `un export relu redonne exactement les memes recettes`() {
        val export = RecipeExport(exportedAt = 1_700_000_000_000, recipes = listOf(carbonara))

        assertEquals(export, decodeSuccess(RecipeTransfer.encode(export)))
    }

    @Test
    fun `l export contient la signature et la version du format`() {
        val json = RecipeTransfer.encode(RecipeExport(exportedAt = 0, recipes = listOf(carbonara)))

        assertTrue(json.contains("\"format\":\"easycook-recipes\""))
        assertTrue(json.contains("\"version\":1"))
    }

    @Test
    fun `les accents et caracteres speciaux sont conserves`() {
        val recipe = ExportedRecipe(title = "Crème brûlée « maison » & co", steps = listOf("Caraméliser"))
        val export = RecipeExport(exportedAt = 0, recipes = listOf(recipe))

        assertEquals(recipe, decodeSuccess(RecipeTransfer.encode(export)).recipes.single())
    }

    // --- Tolerance ---

    @Test
    fun `les champs facultatifs absents prennent leur valeur par defaut`() {
        val json = """{"format":"easycook-recipes","version":1,"exportedAt":0,"recipes":[{"title":"Salade"}]}"""

        val recipe = decodeSuccess(json).recipes.single()

        assertEquals(ExportedRecipe(title = "Salade"), recipe)
        assertEquals(Recipe.NO_COOKING_TIME, recipe.cookingTime)
    }

    @Test
    fun `les champs inconnus d une version future compatible sont ignores`() {
        val json = """{"format":"easycook-recipes","version":1,"exportedAt":0,"author":"Jerry",
            |"recipes":[{"title":"Salade","rating":5}]}""".trimMargin()

        assertEquals("Salade", decodeSuccess(json).recipes.single().title)
    }

    @Test
    fun `les recettes sans titre sont ecartees`() {
        val json = """{"format":"easycook-recipes","version":1,"exportedAt":0,
            |"recipes":[{"title":"  "},{"title":"Salade"}]}""".trimMargin()

        assertEquals(listOf("Salade"), decodeSuccess(json).recipes.map { it.title })
    }

    // --- Refus ---

    @Test
    fun `un texte qui n est pas du JSON est refuse`() {
        assertEquals(ImportError.INVALID_FILE, decodeError("ceci n'est pas un export"))
    }

    @Test
    fun `un JSON d une autre application est refuse`() {
        assertEquals(ImportError.INVALID_FILE, decodeError("""{"name":"autre chose"}"""))
        assertEquals(
            ImportError.INVALID_FILE,
            decodeError("""{"format":"autre-format","version":1,"exportedAt":0,"recipes":[{"title":"A"}]}""")
        )
    }

    @Test
    fun `un export d une version plus recente est refuse`() {
        val json = """{"format":"easycook-recipes","version":2,"exportedAt":0,"recipes":[{"title":"A"}]}"""

        assertEquals(ImportError.UNSUPPORTED_VERSION, decodeError(json))
    }

    @Test
    fun `un export sans recette est refuse`() {
        val json = """{"format":"easycook-recipes","version":1,"exportedAt":0,"recipes":[]}"""

        assertEquals(ImportError.NO_RECIPES, decodeError(json))
    }

    // --- Numerotation des profils ---

    @Test
    fun `le premier profil importe est le profil 1`() {
        assertEquals(1, ProfileNumbering.next(emptyList()))
    }

    @Test
    fun `les profils suivants prennent le numero suivant`() {
        assertEquals(3, ProfileNumbering.next(listOf(1, 2)))
    }

    @Test
    fun `un numero libere par une suppression est reutilise`() {
        assertEquals(1, ProfileNumbering.next(listOf(2, 3)))
        assertEquals(2, ProfileNumbering.next(listOf(1, 3)))
    }
}
