package com.example.easycook.ui.screens.steps

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.easycook.data.RecipeRepository
import com.example.easycook.data.RecipeStep
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class StepByStepUiState(
    val isLoading: Boolean = true,
    val recipeTitle: String = "",
    val steps: List<RecipeStep> = emptyList(),
    /** Étape dont l'instruction est en cours de saisie, ou null. */
    val editingStepId: Long? = null,
    /** L'utilisateur a tenté de valider une instruction vide. */
    val showEmptyInstructionError: Boolean = false
) {
    val doneCount: Int
        get() = steps.count { it.isDone }
}

/**
 * Suivi d'une recette étape par étape : on coche les étapes réalisées, et on peut en ajouter,
 * en modifier ou en supprimer.
 */
class StepByStepViewModel(
    private val recipeId: Int,
    private val repository: RecipeRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val editingStepId = savedStateHandle.getStateFlow<Long?>(KEY_EDITING_STEP, null)

    val uiState: StateFlow<StepByStepUiState> = combine(
        repository.observeRecipe(recipeId),
        repository.observeSteps(recipeId),
        editingStepId,
        savedStateHandle.getStateFlow(KEY_EMPTY_ERROR, false)
    ) { recipe, steps, editingId, emptyError ->
        StepByStepUiState(
            isLoading = false,
            recipeTitle = recipe?.title.orEmpty(),
            steps = steps,
            editingStepId = editingId,
            showEmptyInstructionError = emptyError
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StepByStepUiState())

    /** Ajoute une étape vide en fin de liste et ouvre sa saisie. */
    fun startAddingStep() {
        viewModelScope.launch {
            val stepId = repository.addStep(recipeId, "")
            startEditing(stepId, isNew = true)
        }
    }

    fun startEditing(stepId: Long) = startEditing(stepId, isNew = false)

    private fun startEditing(stepId: Long, isNew: Boolean) {
        savedStateHandle[KEY_EDITING_STEP] = stepId
        savedStateHandle[KEY_NEW_STEP] = isNew
        savedStateHandle[KEY_EMPTY_ERROR] = false
    }

    /** Enregistre l'instruction saisie ; une instruction vide est refusée. */
    fun confirmInstruction(instruction: String) {
        val stepId = editingStepId.value ?: return
        val trimmed = instruction.trim()
        if (trimmed.isEmpty()) {
            savedStateHandle[KEY_EMPTY_ERROR] = true
            return
        }

        stopEditing()
        viewModelScope.launch { repository.updateStepInstruction(stepId, trimmed) }
    }

    /** Abandonne la saisie : une étape tout juste ajoutée est retirée, une étape existante est conservée. */
    fun cancelEditing() {
        val stepId = editingStepId.value ?: return
        val isNew = savedStateHandle.get<Boolean>(KEY_NEW_STEP) == true

        stopEditing()
        if (isNew) {
            viewModelScope.launch { repository.deleteStep(stepId) }
        }
    }

    fun setDone(stepId: Long, isDone: Boolean) {
        viewModelScope.launch { repository.setStepDone(stepId, isDone) }
    }

    fun deleteStep(stepId: Long) {
        if (editingStepId.value == stepId) {
            stopEditing()
        }
        viewModelScope.launch { repository.deleteStep(stepId) }
    }

    fun uncheckAll() {
        viewModelScope.launch { repository.resetSteps(recipeId) }
    }

    private fun stopEditing() {
        savedStateHandle[KEY_EDITING_STEP] = null
        savedStateHandle[KEY_NEW_STEP] = false
        savedStateHandle[KEY_EMPTY_ERROR] = false
    }

    private companion object {
        const val KEY_EDITING_STEP = "editingStepId"
        const val KEY_NEW_STEP = "isNewStep"
        const val KEY_EMPTY_ERROR = "showEmptyInstructionError"
    }
}
