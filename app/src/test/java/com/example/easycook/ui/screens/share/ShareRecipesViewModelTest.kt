package com.example.easycook.ui.screens.share

import androidx.lifecycle.SavedStateHandle
import com.example.easycook.FakeRecipeDao
import com.example.easycook.MainDispatcherRule
import com.example.easycook.collectAll
import com.example.easycook.data.DecodeResult
import com.example.easycook.data.RecipeEntity
import com.example.easycook.data.RecipeTransfer
import com.example.easycook.testRepository
import com.example.easycook.transfer.DownloadResult
import com.example.easycook.transfer.TransferClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShareRecipesViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private fun dao() = FakeRecipeDao(
        initialRecipes = listOf(
            RecipeEntity(id = 1, title = "A", image = "", cookingTime = 10),
            RecipeEntity(id = 2, title = "B", image = "", cookingTime = 10),
            RecipeEntity(id = 3, title = "Autre profil", image = "", cookingTime = 10, profileId = 5)
        )
    )

    /** Le serveur écoute en local sur la machine de test (127.0.0.1), comme sur le Wi-Fi d'un téléphone. */
    private fun TestScope.viewModel(
        handle: SavedStateHandle = SavedStateHandle(),
        localAddress: () -> String? = { "127.0.0.1" }
    ) = ShareRecipesViewModel(
        profileId = 0,
        repository = testRepository(dao()),
        savedStateHandle = handle,
        localAddress = localAddress,
        ioDispatcher = UnconfinedTestDispatcher(testScheduler)
    )

    @Test
    fun `seules les recettes du profil partage sont proposees`() = runTest {
        val state = collectAll(viewModel().uiState).last()

        assertEquals(listOf("A", "B"), state.recipes.map { it.title })
        assertEquals(0, state.selectedCount)
    }

    @Test
    fun `cocher decocher et tout selectionner`() = runTest {
        val viewModel = viewModel()
        val states = collectAll(viewModel.uiState)

        viewModel.toggle(1)
        assertEquals(setOf(1), states.last().selectedIds)
        assertFalse(states.last().allSelected)

        viewModel.toggleAll()
        assertTrue(states.last().allSelected)

        viewModel.toggleAll()
        assertEquals(0, states.last().selectedCount)

        viewModel.toggle(2)
        viewModel.toggle(2)
        assertEquals(0, states.last().selectedCount)
    }

    @Test
    fun `la selection survit a la recreation de l ecran`() = runTest {
        val handle = SavedStateHandle()
        val first = viewModel(handle)
        collectAll(first.uiState)
        first.toggle(2)

        assertEquals(setOf(2), collectAll(viewModel(handle).uiState).last().selectedIds)
    }

    @Test
    fun `sans wifi le partage est impossible`() = runTest {
        val viewModel = viewModel(localAddress = { null })
        val states = collectAll(viewModel.uiState)
        viewModel.toggle(1)

        viewModel.startSharing()

        assertEquals(ShareState.NoNetwork, states.last().share)
    }

    @Test
    fun `le QR code permet de telecharger exactement les recettes choisies`() = runTest {
        val viewModel = viewModel()
        val states = collectAll(viewModel.uiState)
        viewModel.toggle(2)

        viewModel.startSharing()
        val ready = states.last().share as ShareState.Ready

        try {
            assertEquals(1, ready.recipeCount)
            val download = TransferClient.download(ready.link) as DownloadResult.Success
            val export = (RecipeTransfer.decode(download.json) as DecodeResult.Success).export
            assertEquals(listOf("B"), export.recipes.map { it.title })
        } finally {
            viewModel.stopSharing()
        }
    }

    @Test
    fun `revenir au choix des recettes ferme le serveur`() = runTest {
        val viewModel = viewModel()
        val states = collectAll(viewModel.uiState)
        viewModel.toggle(1)
        viewModel.startSharing()
        val link = (states.last().share as ShareState.Ready).link

        viewModel.stopSharing()

        assertEquals(ShareState.Selecting, states.last().share)
        assertTrue(TransferClient.download(link) is DownloadResult.Failure)
    }

    @Test
    fun `sans recette cochee rien n est partage`() = runTest {
        val viewModel = viewModel()
        val states = collectAll(viewModel.uiState)

        viewModel.startSharing()

        assertEquals(ShareState.Selecting, states.last().share)
    }
}
