package com.example.easycook.ui.screens.detail

import androidx.lifecycle.SavedStateHandle
import com.example.easycook.FakeRecipeDao
import com.example.easycook.MainDispatcherRule
import com.example.easycook.collectAll
import com.example.easycook.data.IngredientEntity
import com.example.easycook.data.RecipeDraft
import com.example.easycook.data.RecipeEntity
import com.example.easycook.data.RecipeError
import com.example.easycook.data.StepEntity
import com.example.easycook.testRepository
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RecipeDetailViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private fun dao() = FakeRecipeDao(
        initialRecipes = listOf(RecipeEntity(id = 1, title = "Pizza", image = "pizza.jpg", cookingTime = 0)),
        initialIngredients = listOf(IngredientEntity(id = 1, recipeId = 1, quantity = "250 g", name = "farine")),
        initialSteps = listOf(StepEntity(id = 1, recipeId = 1, position = 0, instruction = "Pétrir"))
    )

    private fun TestScope.viewModel(dao: FakeRecipeDao = dao(), handle: SavedStateHandle = SavedStateHandle()) =
        RecipeDetailViewModel(recipeId = 1, repository = testRepository(dao), savedStateHandle = handle)

    @Test
    fun `l ecran recoit la recette et ses etapes`() = runTest {
        val state = collectAll(viewModel().uiState).last()

        assertFalse(state.isLoading)
        assertEquals("Pizza", state.recipe?.title)
        assertEquals(listOf("Pétrir"), state.steps.map { it.instruction })
    }

    @Test
    fun `le formulaire de modification part de la recette enregistree`() = runTest {
        val state = collectAll(viewModel().uiState).last()

        assertEquals(
            RecipeDraft(
                title = "Pizza",
                image = "pizza.jpg",
                // Sans temps de cuisson, le champ reste vide
                cookingTime = "",
                description = "",
                ingredients = listOf("250 g farine"),
                steps = listOf("Pétrir")
            ),
            state.draft()
        )
    }

    @Test
    fun `enregistrer une modification valide met a jour la recette et quitte la modification`() = runTest {
        val viewModel = viewModel()
        val states = collectAll(viewModel.uiState)
        viewModel.startEditing()
        assertTrue(states.last().isEditing)

        viewModel.save(states.last().draft()!!.copy(title = "Pizza royale", steps = listOf("Pétrir", "Cuire")))

        assertFalse(states.last().isEditing)
        assertEquals("Pizza royale", states.last().recipe?.title)
        assertEquals(listOf("Pétrir", "Cuire"), states.last().steps.map { it.instruction })
    }

    @Test
    fun `une modification invalide reste en cours avec son erreur`() = runTest {
        val viewModel = viewModel()
        val states = collectAll(viewModel.uiState)
        viewModel.startEditing()

        viewModel.save(states.last().draft()!!.copy(cookingTime = "-5"))

        assertTrue(states.last().isEditing)
        assertEquals(RecipeError.COOKING_TIME_INVALID, states.last().error)
        assertEquals("Pizza", states.last().recipe?.title)
    }

    @Test
    fun `annuler efface l erreur et quitte la modification`() = runTest {
        val viewModel = viewModel()
        val states = collectAll(viewModel.uiState)
        viewModel.startEditing()
        viewModel.save(RecipeDraft())

        viewModel.cancelEditing()

        assertFalse(states.last().isEditing)
        assertNull(states.last().error)
    }

    @Test
    fun `le mode modification survit a la recreation de l ecran`() = runTest {
        val handle = SavedStateHandle()
        viewModel(handle = handle).startEditing()

        assertTrue(collectAll(viewModel(handle = handle).uiState).last().isEditing)
    }

    @Test
    fun `une recette supprimee n est plus affichee`() = runTest {
        val dao = dao()
        val states = collectAll(viewModel(dao).uiState)

        dao.deleteRecipeById(1)

        assertNull(states.last().recipe)
    }
}
