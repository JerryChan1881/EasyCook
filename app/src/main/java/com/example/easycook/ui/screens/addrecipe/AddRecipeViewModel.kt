package com.example.easycook.ui.screens.addrecipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.easycook.data.DraftCheck
import com.example.easycook.data.RecipeDraft
import com.example.easycook.data.RecipeError
import com.example.easycook.data.RecipeRepository
import com.example.easycook.data.RecipeValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddRecipeUiState(
    val error: RecipeError? = null,
    val isSaving: Boolean = false,
    /** La recette est enregistrée : l'écran peut se fermer. */
    val isSaved: Boolean = false
)

/** Création d'une recette dans un profil (« Mon profil » ou un profil importé). */
class AddRecipeViewModel(
    private val profileId: Int,
    private val repository: RecipeRepository
) : ViewModel() {

    private val state = MutableStateFlow(AddRecipeUiState())
    val uiState: StateFlow<AddRecipeUiState> = state.asStateFlow()

    fun save(draft: RecipeDraft) {
        // Un double appui sur « Enregistrer » ne crée pas la recette deux fois
        if (state.value.isSaving || state.value.isSaved) return

        when (val check = RecipeValidator.check(draft)) {
            is DraftCheck.Invalid -> state.update { it.copy(error = check.error) }
            is DraftCheck.Valid -> {
                state.update { it.copy(error = null, isSaving = true) }
                viewModelScope.launch {
                    repository.addRecipe(check.content, profileId)
                    state.update { it.copy(isSaving = false, isSaved = true) }
                }
            }
        }
    }
}
