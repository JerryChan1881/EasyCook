package com.example.easycook.ui.screens.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.easycook.data.ImportError
import com.example.easycook.data.ImportResult
import com.example.easycook.data.OWN_PROFILE_ID
import com.example.easycook.data.Profile
import com.example.easycook.data.Recipe
import com.example.easycook.data.RecipeRepository
import com.example.easycook.data.TransferLink
import com.example.easycook.transfer.DownloadError
import com.example.easycook.transfer.DownloadResult
import com.example.easycook.transfer.TextDocuments
import com.example.easycook.transfer.TransferClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HomeUiState(
    /** Vrai tant que la base n'a pas répondu : on n'affiche pas encore « Aucune recette ». */
    val isLoading: Boolean = true,
    /** Profils importés (« Mon profil » n'en fait pas partie). */
    val profiles: List<Profile> = emptyList(),
    val selectedProfileId: Int = OWN_PROFILE_ID,
    /** Recettes du profil affiché, favoris en premier. */
    val recipes: List<Recipe> = emptyList(),
    /** Téléchargement en cours de recettes partagées par QR code. */
    val isReceiving: Boolean = false
) {
    /** Profil importé affiché, ou null pour « Mon profil ». */
    val selectedProfile: Profile?
        get() = profiles.find { it.id == selectedProfileId }
}

/** Messages ponctuels à montrer à l'utilisateur, une seule fois chacun. */
sealed interface HomeEvent {
    data class Imported(val recipeCount: Int) : HomeEvent
    data class ImportFailed(val error: ImportError) : HomeEvent
    data object FileUnreadable : HomeEvent
    data class Exported(val success: Boolean) : HomeEvent
    data object NotAnEasyCookQrCode : HomeEvent
    data class ReceiveFailed(val error: DownloadError) : HomeEvent
}

/**
 * Accueil : liste des recettes par profil, favoris, suppression, et toutes les arrivées et
 * sorties de recettes (fichier JSON, réception par QR code).
 */
class HomeViewModel(
    private val repository: RecipeRepository,
    private val savedStateHandle: SavedStateHandle,
    private val documents: TextDocuments,
    private val download: (TransferLink) -> DownloadResult = TransferClient::download,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val selectedProfileId = savedStateHandle.getStateFlow(KEY_SELECTED_PROFILE, OWN_PROFILE_ID)
    private val isReceiving = MutableStateFlow(false)

    val uiState: StateFlow<HomeUiState> = combine(
        repository.recipes,
        repository.profiles,
        selectedProfileId,
        isReceiving
    ) { recipes, profiles, selectedId, receiving ->
        // Si le profil choisi a été supprimé, on revient sur « Mon profil »
        val profileId = selectedId.takeIf { id -> id == OWN_PROFILE_ID || profiles.any { it.id == id } }
            ?: OWN_PROFILE_ID
        HomeUiState(
            isLoading = false,
            profiles = profiles,
            selectedProfileId = profileId,
            recipes = recipes.filter { it.profileId == profileId },
            isReceiving = receiving
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private val eventChannel = Channel<HomeEvent>(Channel.BUFFERED)
    val events: Flow<HomeEvent> = eventChannel.receiveAsFlow()

    fun selectProfile(profileId: Int) {
        savedStateHandle[KEY_SELECTED_PROFILE] = profileId
    }

    fun toggleFavorite(recipe: Recipe) {
        viewModelScope.launch { repository.setFavorite(recipe.id, !recipe.isFavorite) }
    }

    fun deleteRecipe(recipeId: Int) {
        viewModelScope.launch { repository.deleteRecipe(recipeId) }
    }

    /** Supprime un profil importé avec ses recettes, puis revient sur « Mon profil ». */
    fun deleteProfile(profileId: Int) {
        selectProfile(OWN_PROFILE_ID)
        viewModelScope.launch { repository.deleteProfile(profileId) }
    }

    /** Enregistre les recettes de « Mon profil » dans le fichier choisi par l'utilisateur. */
    fun exportToFile(uri: String) {
        viewModelScope.launch {
            val json = repository.exportRecipes()
            val written = withContext(ioDispatcher) { documents.write(uri, json) }
            eventChannel.send(HomeEvent.Exported(written))
        }
    }

    /** Importe le fichier choisi dans un nouveau profil. */
    fun importFromFile(uri: String) {
        viewModelScope.launch {
            val json = withContext(ioDispatcher) { documents.read(uri) }
            if (json == null) {
                eventChannel.send(HomeEvent.FileUnreadable)
            } else {
                import(json)
            }
        }
    }

    /** Reçoit les recettes du QR code scanné : téléchargement depuis l'autre téléphone, puis import. */
    fun receive(scannedText: String) {
        val link = TransferLink.parse(scannedText)
        if (link == null) {
            eventChannel.trySend(HomeEvent.NotAnEasyCookQrCode)
            return
        }

        isReceiving.value = true
        viewModelScope.launch {
            try {
                when (val result = withContext(ioDispatcher) { download(link) }) {
                    is DownloadResult.Success -> import(result.json)
                    is DownloadResult.Failure -> eventChannel.send(HomeEvent.ReceiveFailed(result.error))
                }
            } finally {
                isReceiving.value = false
            }
        }
    }

    // Commun au fichier et au QR code : crée un profil et l'affiche aussitôt
    private suspend fun import(json: String) {
        when (val result = repository.importRecipes(json)) {
            is ImportResult.Success -> {
                selectProfile(result.profileId)
                eventChannel.send(HomeEvent.Imported(result.recipeCount))
            }
            is ImportResult.Failure -> eventChannel.send(HomeEvent.ImportFailed(result.error))
        }
    }

    private companion object {
        const val KEY_SELECTED_PROFILE = "selectedProfileId"
    }
}
