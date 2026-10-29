package com.example.easycook.ui.screens.home

import androidx.lifecycle.SavedStateHandle
import com.example.easycook.FakeRecipeDao
import com.example.easycook.MainDispatcherRule
import com.example.easycook.collectAll
import com.example.easycook.data.ImportError
import com.example.easycook.data.OWN_PROFILE_ID
import com.example.easycook.data.ProfileEntity
import com.example.easycook.data.RecipeEntity
import com.example.easycook.data.TransferLink
import com.example.easycook.testRepository
import com.example.easycook.transfer.DownloadError
import com.example.easycook.transfer.DownloadResult
import com.example.easycook.transfer.TextDocuments
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    /** Fichiers choisis par l'utilisateur, en mémoire. */
    private class FakeDocuments(val files: MutableMap<String, String> = mutableMapOf()) : TextDocuments {
        var canWrite = true

        override fun read(uri: String): String? = files[uri]

        override fun write(uri: String, text: String): Boolean {
            if (canWrite) files[uri] = text
            return canWrite
        }
    }

    private val validExport =
        """{"format":"easycook-recipes","version":1,"exportedAt":0,"recipes":[{"title":"Salade"},{"title":"Soupe"}]}"""

    private val qrCode = TransferLink(host = "192.168.1.20", port = 5000, token = "code", key = "ab").encode()

    private fun dao() = FakeRecipeDao(
        initialRecipes = listOf(
            RecipeEntity(id = 1, title = "A moi", image = "", cookingTime = 10),
            RecipeEntity(id = 2, title = "Favori", image = "", cookingTime = 10, isFavorite = true),
            RecipeEntity(id = 3, title = "Importee", image = "", cookingTime = 10, profileId = 5)
        ),
        initialProfiles = listOf(ProfileEntity(id = 5, number = 1, importedAt = 0))
    )

    private fun TestScope.viewModel(
        dao: FakeRecipeDao = dao(),
        documents: TextDocuments = FakeDocuments(),
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        download: (TransferLink) -> DownloadResult = { DownloadResult.Failure(DownloadError.UNREACHABLE) }
    ) = HomeViewModel(
        repository = testRepository(dao),
        savedStateHandle = savedStateHandle,
        documents = documents,
        download = download,
        ioDispatcher = UnconfinedTestDispatcher(testScheduler)
    )

    // --- Affichage ---

    @Test
    fun `l accueil attend la base avant d afficher la liste`() = runTest {
        val viewModel = viewModel()

        assertTrue(viewModel.uiState.value.isLoading)

        val states = collectAll(viewModel.uiState)
        assertFalse(states.last().isLoading)
    }

    @Test
    fun `seules les recettes de mon profil sont affichees, favoris en premier`() = runTest {
        val viewModel = viewModel()
        val states = collectAll(viewModel.uiState)

        assertEquals(OWN_PROFILE_ID, states.last().selectedProfileId)
        assertNull(states.last().selectedProfile)
        assertEquals(listOf("Favori", "A moi"), states.last().recipes.map { it.title })
    }

    @Test
    fun `choisir un onglet affiche les recettes de ce profil`() = runTest {
        val viewModel = viewModel()
        val states = collectAll(viewModel.uiState)

        viewModel.selectProfile(5)

        assertEquals(5, states.last().selectedProfile?.id)
        assertEquals(listOf("Importee"), states.last().recipes.map { it.title })
    }

    @Test
    fun `l onglet choisi est conserve si l ecran est recree`() = runTest {
        val handle = SavedStateHandle()
        viewModel(savedStateHandle = handle).selectProfile(5)

        val recreated = viewModel(savedStateHandle = handle)

        assertEquals(5, collectAll(recreated.uiState).last().selectedProfileId)
    }

    @Test
    fun `un profil choisi qui n existe plus ramene sur mon profil`() = runTest {
        val viewModel = viewModel()
        val states = collectAll(viewModel.uiState)

        viewModel.selectProfile(42)

        assertEquals(OWN_PROFILE_ID, states.last().selectedProfileId)
    }

    @Test
    fun `le coeur inverse le statut de favori`() = runTest {
        val viewModel = viewModel()
        val states = collectAll(viewModel.uiState)

        viewModel.toggleFavorite(states.last().recipes.first { it.title == "A moi" })

        assertTrue(states.last().recipes.all { it.isFavorite })
    }

    @Test
    fun `supprimer une recette la retire de la liste`() = runTest {
        val viewModel = viewModel()
        val states = collectAll(viewModel.uiState)

        viewModel.deleteRecipe(1)

        assertEquals(listOf("Favori"), states.last().recipes.map { it.title })
    }

    @Test
    fun `supprimer un profil importe ramene sur mon profil`() = runTest {
        val viewModel = viewModel()
        val states = collectAll(viewModel.uiState)
        viewModel.selectProfile(5)

        viewModel.deleteProfile(5)

        assertEquals(OWN_PROFILE_ID, states.last().selectedProfileId)
        assertTrue(states.last().profiles.isEmpty())
    }

    // --- Fichiers ---

    @Test
    fun `importer un fichier cree un profil et l affiche`() = runTest {
        val viewModel = viewModel(documents = FakeDocuments(mutableMapOf("content://export" to validExport)))
        val states = collectAll(viewModel.uiState)
        val events = collectAll(viewModel.events)

        viewModel.importFromFile("content://export")

        assertEquals(listOf(HomeEvent.Imported(recipeCount = 2)), events)
        assertEquals(2, states.last().selectedProfile?.number)
        assertEquals(listOf("Salade", "Soupe"), states.last().recipes.map { it.title })
    }

    @Test
    fun `un fichier illisible ou invalide est signale`() = runTest {
        val viewModel = viewModel(documents = FakeDocuments(mutableMapOf("content://texte" to "bonjour")))
        val events = collectAll(viewModel.events)

        viewModel.importFromFile("content://absent")
        viewModel.importFromFile("content://texte")

        assertEquals(listOf(HomeEvent.FileUnreadable, HomeEvent.ImportFailed(ImportError.INVALID_FILE)), events)
    }

    @Test
    fun `exporter ecrit les recettes de mon profil dans le fichier choisi`() = runTest {
        val documents = FakeDocuments()
        val viewModel = viewModel(documents = documents)
        val events = collectAll(viewModel.events)

        viewModel.exportToFile("content://sortie")

        assertEquals(listOf(HomeEvent.Exported(success = true)), events)
        val json = documents.files.getValue("content://sortie")
        assertTrue(json.contains("A moi"))
        assertFalse(json.contains("Importee"))
    }

    @Test
    fun `un echec d ecriture est signale`() = runTest {
        val viewModel = viewModel(documents = FakeDocuments().apply { canWrite = false })
        val events = collectAll(viewModel.events)

        viewModel.exportToFile("content://sortie")

        assertEquals(listOf(HomeEvent.Exported(success = false)), events)
    }

    // --- Réception par QR code ---

    @Test
    fun `un QR code d une autre application est refuse sans telechargement`() = runTest {
        var downloaded = false
        val viewModel = viewModel(download = { downloaded = true; DownloadResult.Success(validExport) })
        val events = collectAll(viewModel.events)

        viewModel.receive("https://example.com")

        assertEquals(listOf(HomeEvent.NotAnEasyCookQrCode), events)
        assertFalse(downloaded)
    }

    @Test
    fun `les recettes recues par QR code sont importees dans un nouveau profil`() = runTest {
        var requested: TransferLink? = null
        val viewModel = viewModel(download = { requested = it; DownloadResult.Success(validExport) })
        val states = collectAll(viewModel.uiState)
        val events = collectAll(viewModel.events)

        viewModel.receive(qrCode)

        assertEquals("192.168.1.20", requested?.host)
        assertEquals(listOf(HomeEvent.Imported(recipeCount = 2)), events)
        assertEquals(listOf("Salade", "Soupe"), states.last().recipes.map { it.title })
        assertFalse(states.last().isReceiving)
    }

    @Test
    fun `la fenetre de reception reste affichee pendant le telechargement`() = runTest {
        val states = mutableListOf<Boolean>()
        lateinit var viewModel: HomeViewModel
        viewModel = viewModel(download = {
            states += viewModel.uiState.value.isReceiving
            DownloadResult.Failure(DownloadError.REFUSED)
        })
        collectAll(viewModel.uiState)

        viewModel.receive(qrCode)

        assertEquals(listOf(true), states)
        assertFalse(viewModel.uiState.value.isReceiving)
    }

    @Test
    fun `un echec de telechargement est signale`() = runTest {
        val viewModel = viewModel(download = { DownloadResult.Failure(DownloadError.REFUSED) })
        val events = collectAll(viewModel.events)

        viewModel.receive(qrCode)

        assertEquals(listOf(HomeEvent.ReceiveFailed(DownloadError.REFUSED)), events)
    }
}
