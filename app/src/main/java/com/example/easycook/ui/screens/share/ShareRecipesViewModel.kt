package com.example.easycook.ui.screens.share

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.easycook.data.Recipe
import com.example.easycook.data.RecipeRepository
import com.example.easycook.data.TransferLink
import com.example.easycook.transfer.TransferClient
import com.example.easycook.transfer.TransferCrypto
import com.example.easycook.transfer.TransferServer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Étapes du partage, une fois les recettes choisies. */
sealed interface ShareState {
    /** Choix des recettes, avant le QR code. */
    data object Selecting : ShareState
    data object Preparing : ShareState
    /** Pas de Wi-Fi (ni partage de connexion) : impossible de partager. */
    data object NoNetwork : ShareState
    data class Ready(val link: TransferLink, val recipeCount: Int, val downloads: Int = 0) : ShareState
}

data class ShareUiState(
    val isLoading: Boolean = true,
    /** Recettes du profil partagé. */
    val recipes: List<Recipe> = emptyList(),
    val selectedIds: Set<Int> = emptySet(),
    val share: ShareState = ShareState.Selecting
) {
    /** Nombre de recettes cochées, sans compter celles supprimées entre-temps. */
    val selectedCount: Int
        get() = recipes.count { it.id in selectedIds }

    val allSelected: Boolean
        get() = recipes.isNotEmpty() && selectedCount == recipes.size
}

/**
 * Partage de recettes par QR code : on choisit les recettes, puis un petit serveur sur le Wi-Fi
 * les envoie (chiffrées) au téléphone qui scanne le QR code.
 *
 * Le serveur appartient au ViewModel : il survit à une rotation de l'écran (le QR code reste le même)
 * et s'arrête dès qu'on revient au choix des recettes ou qu'on quitte l'écran.
 */
class ShareRecipesViewModel(
    private val profileId: Int,
    private val repository: RecipeRepository,
    private val savedStateHandle: SavedStateHandle,
    private val localAddress: () -> String? = TransferClient::localAddress,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    /** Threads du serveur (connexions bloquantes), toujours hors du thread principal. */
    private val serverDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val selectedIds = savedStateHandle.getStateFlow(KEY_SELECTED, IntArray(0))
    private val share = MutableStateFlow<ShareState>(ShareState.Selecting)
    private var shareJob: Job? = null

    val uiState: StateFlow<ShareUiState> = combine(
        repository.recipes.map { recipes -> recipes.filter { it.profileId == profileId } },
        selectedIds,
        share
    ) { recipes, selected, share ->
        ShareUiState(isLoading = false, recipes = recipes, selectedIds = selected.toSet(), share = share)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShareUiState())

    fun toggle(recipeId: Int) {
        val selected = selectedIds.value.toMutableSet()
        if (!selected.remove(recipeId)) selected.add(recipeId)
        savedStateHandle[KEY_SELECTED] = selected.toIntArray()
    }

    /** « Tout sélectionner », ou tout décocher si tout l'est déjà. */
    fun toggleAll() {
        val state = uiState.value
        savedStateHandle[KEY_SELECTED] = if (state.allSelected) IntArray(0) else state.recipes.map { it.id }.toIntArray()
    }

    /** Prépare les recettes cochées et ouvre le serveur ; le QR code s'affiche quand il est prêt. */
    fun startSharing() {
        val state = uiState.value
        val recipeIds = state.recipes.map { it.id }.filter { it in state.selectedIds }.toSet()
        if (recipeIds.isEmpty()) return

        shareJob?.cancel()
        share.value = ShareState.Preparing
        shareJob = viewModelScope.launch {
            val address = withContext(ioDispatcher) { localAddress() }
            if (address == null) {
                share.value = ShareState.NoNetwork
                return@launch
            }

            val json = repository.exportRecipes(profileId = profileId, recipeIds = recipeIds)
            val token = TransferServer.newToken()
            val key = TransferCrypto.newKey()
            val server = withContext(ioDispatcher) {
                // Chiffré avec une clé qui ne voyage que dans le QR code ; serveur limité à l'adresse Wi-Fi
                val payload = TransferCrypto.encrypt(json.toByteArray(Charsets.UTF_8), key)
                TransferServer(payload, token, bindAddress = address) {
                    share.update { if (it is ShareState.Ready) it.copy(downloads = it.downloads + 1) else it }
                }
            }
            try {
                server.start(this, serverDispatcher)
                share.value = ShareState.Ready(
                    link = TransferLink(host = address, port = server.port, token = token, key = key),
                    recipeCount = recipeIds.size
                )
                awaitCancellation()
            } finally {
                server.stop()
            }
        }
    }

    /** Ferme le serveur et revient au choix des recettes. */
    fun stopSharing() {
        shareJob?.cancel()
        shareJob = null
        share.value = ShareState.Selecting
    }

    private companion object {
        const val KEY_SELECTED = "selectedRecipeIds"
    }
}
