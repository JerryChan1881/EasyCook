package com.example.easycook.ui.screens.addrecipe

import com.example.easycook.FakeRecipeDao
import com.example.easycook.MainDispatcherRule
import com.example.easycook.data.Ingredient
import com.example.easycook.data.RecipeDraft
import com.example.easycook.data.RecipeError
import com.example.easycook.testRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AddRecipeViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val validDraft = RecipeDraft(
        title = "  Crêpes  ",
        cookingTime = "20",
        ingredients = listOf("250 g farine", "", "3 oeufs"),
        steps = listOf("Mélanger", "  ")
    )

    @Test
    fun `une saisie valide est enregistree nettoyee dans le profil demande`() = runTest {
        val repository = testRepository(FakeRecipeDao())
        val viewModel = AddRecipeViewModel(profileId = 7, repository = repository)

        viewModel.save(validDraft)

        val saved = repository.recipes.first().single()
        assertEquals("Crêpes", saved.title)
        assertEquals(20, saved.cookingTime)
        assertEquals(7, saved.profileId)
        assertEquals(listOf(Ingredient("250 g", "farine"), Ingredient("3", "oeufs")), saved.ingredientList)
        assertEquals(listOf("Mélanger"), repository.observeSteps(saved.id).first().map { it.instruction })
        assertTrue(viewModel.uiState.value.isSaved)
    }

    @Test
    fun `une saisie invalide affiche l erreur sans rien enregistrer`() = runTest {
        val repository = testRepository(FakeRecipeDao())
        val viewModel = AddRecipeViewModel(profileId = 0, repository = repository)

        viewModel.save(validDraft.copy(title = " "))

        assertEquals(RecipeError.TITLE_REQUIRED, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isSaved)
        assertTrue(repository.recipes.first().isEmpty())
    }

    @Test
    fun `l erreur disparait quand la saisie est corrigee`() = runTest {
        val viewModel = AddRecipeViewModel(profileId = 0, repository = testRepository(FakeRecipeDao()))

        viewModel.save(validDraft.copy(ingredients = listOf("")))
        assertEquals(RecipeError.NO_INGREDIENT, viewModel.uiState.value.error)

        viewModel.save(validDraft)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `un double appui sur enregistrer ne cree qu une recette`() = runTest {
        val repository = testRepository(FakeRecipeDao())
        val viewModel = AddRecipeViewModel(profileId = 0, repository = repository)

        viewModel.save(validDraft)
        viewModel.save(validDraft)

        assertEquals(1, repository.recipes.first().size)
    }
}
