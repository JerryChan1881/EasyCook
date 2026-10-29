package com.example.easycook.ui.screens.steps

import androidx.lifecycle.SavedStateHandle
import com.example.easycook.FakeRecipeDao
import com.example.easycook.MainDispatcherRule
import com.example.easycook.collectAll
import com.example.easycook.data.RecipeEntity
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

class StepByStepViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private fun dao() = FakeRecipeDao(
        initialRecipes = listOf(RecipeEntity(id = 1, title = "Tarte", image = "", cookingTime = 30)),
        initialSteps = listOf(
            StepEntity(id = 1, recipeId = 1, position = 0, instruction = "Étaler la pâte"),
            StepEntity(id = 2, recipeId = 1, position = 1, instruction = "Cuire", isDone = true)
        )
    )

    private fun TestScope.viewModel(dao: FakeRecipeDao = dao()) =
        StepByStepViewModel(recipeId = 1, repository = testRepository(dao), savedStateHandle = SavedStateHandle())

    private fun List<StepByStepUiState>.instructions() = last().steps.map { it.instruction }

    @Test
    fun `l ecran affiche le titre les etapes et la progression`() = runTest {
        val state = collectAll(viewModel().uiState).last()

        assertEquals("Tarte", state.recipeTitle)
        assertEquals(listOf("Étaler la pâte", "Cuire"), state.steps.map { it.instruction })
        assertEquals(1, state.doneCount)
    }

    @Test
    fun `cocher puis tout decocher`() = runTest {
        val viewModel = viewModel()
        val states = collectAll(viewModel.uiState)

        viewModel.setDone(stepId = 1, isDone = true)
        assertEquals(2, states.last().doneCount)

        viewModel.uncheckAll()
        assertEquals(0, states.last().doneCount)
    }

    @Test
    fun `ajouter une etape ouvre sa saisie puis enregistre l instruction`() = runTest {
        val viewModel = viewModel()
        val states = collectAll(viewModel.uiState)

        viewModel.startAddingStep()
        val newStepId = states.last().editingStepId
        assertEquals(newStepId, states.last().steps.last().id)

        viewModel.confirmInstruction("  Servir tiède ")

        assertNull(states.last().editingStepId)
        assertEquals(listOf("Étaler la pâte", "Cuire", "Servir tiède"), states.instructions())
    }

    @Test
    fun `une instruction vide est refusee`() = runTest {
        val viewModel = viewModel()
        val states = collectAll(viewModel.uiState)
        viewModel.startEditing(1)

        viewModel.confirmInstruction("   ")

        assertTrue(states.last().showEmptyInstructionError)
        assertEquals(1L, states.last().editingStepId)
        assertEquals("Étaler la pâte", states.last().steps.first().instruction)
    }

    @Test
    fun `annuler l ajout d une etape la retire`() = runTest {
        val viewModel = viewModel()
        val states = collectAll(viewModel.uiState)
        viewModel.startAddingStep()

        viewModel.cancelEditing()

        assertNull(states.last().editingStepId)
        assertEquals(listOf("Étaler la pâte", "Cuire"), states.instructions())
    }

    @Test
    fun `annuler la modification d une etape existante la conserve`() = runTest {
        val viewModel = viewModel()
        val states = collectAll(viewModel.uiState)
        viewModel.startEditing(1)

        viewModel.cancelEditing()

        assertNull(states.last().editingStepId)
        assertFalse(states.last().showEmptyInstructionError)
        assertEquals(listOf("Étaler la pâte", "Cuire"), states.instructions())
    }

    @Test
    fun `supprimer l etape en cours de saisie ferme la saisie`() = runTest {
        val viewModel = viewModel()
        val states = collectAll(viewModel.uiState)
        viewModel.startEditing(2)

        viewModel.deleteStep(2)

        assertNull(states.last().editingStepId)
        assertEquals(listOf("Étaler la pâte"), states.instructions())
    }
}
