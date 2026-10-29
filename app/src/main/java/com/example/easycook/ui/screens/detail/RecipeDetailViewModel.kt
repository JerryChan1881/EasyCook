package com.example.easycook.ui.screens.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.easycook.data.DraftCheck
import com.example.easycook.data.IngredientParser
import com.example.easycook.data.Recipe
import com.example.easycook.data.RecipeDraft
import com.example.easycook.data.RecipeError
import com.example.easycook.data.RecipeRepository
import com.example.easycook.data.RecipeStep
import com.example.easycook.data.RecipeValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RecipeDetailUiState(
    /** Vrai tant que la recette et ses étapes n'ont pas été lues. */
    val isLoading: Boolean = true,
    /** Null si la recette n'existe pas (ou plus). */
    val recipe: Recipe? = null,
    val steps: List<RecipeStep> = emptyList(),
    val isEditing: Boolean = false,
    val error: RecipeError? = null
) {
    /** Contenu de départ du formulaire de modification. */
    fun draft(): RecipeDraft? = recipe?.let {
        RecipeDraft(
            title = it.title,
            image = it.image,
            cookingTime = RecipeValidator.formatCookingTime(it.cookingTime),
            description = it.description,
            ingredients = it.ingredientList.map(IngredientParser::format),
            steps = steps.map { step -> step.instruction }
        )
    }
}

/** Détail d'une recette et sa modification. */
class RecipeDetailViewModel(
    private val recipeId: Int,
    private val repository: RecipeRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val error = MutableStateFlow<RecipeError?>(null)

    // Les flux sont créés une seule fois ici, et non à chaque redessin de l'écran
    val uiState: StateFlow<RecipeDetailUiState> = combine(
        repository.observeRecipe(recipeId),
        repository.observeSteps(recipeId),
        savedStateHandle.getStateFlow(KEY_EDITING, false),
        error
    ) { recipe, steps, isEditing, error ->
        RecipeDetailUiState(
            isLoading = false,
            recipe = recipe,
            steps = steps,
            isEditing = isEditing,
            error = error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecipeDetailUiState())

    fun startEditing() {
        error.value = null
        savedStateHandle[KEY_EDITING] = true
    }

    fun cancelEditing() {
        error.value = null
        savedStateHandle[KEY_EDITING] = false
    }

    fun save(draft: RecipeDraft) {
        when (val check = RecipeValidator.check(draft)) {
            is DraftCheck.Invalid -> error.value = check.error
            is DraftCheck.Valid -> viewModelScope.launch {
                repository.updateRecipe(recipeId, check.content)
                cancelEditing()
            }
        }
    }

    private companion object {
        const val KEY_EDITING = "isEditing"
    }
}
