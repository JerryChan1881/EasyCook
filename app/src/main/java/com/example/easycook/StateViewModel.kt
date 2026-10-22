package com.example.easycook

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.easycook.data.Ingredient
import com.example.easycook.data.Recipe
import com.example.easycook.data.RecipeDao
import com.example.easycook.data.RecipeDatabase
import com.example.easycook.data.RecipeEntity
import com.example.easycook.data.RecipeStep
import com.example.easycook.data.StepEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StateViewModel(
    private val recipeDao: RecipeDao,
    coroutineScope: CoroutineScope? = null
) : ViewModel() {

    /**
     * Scope utilisé pour toutes les écritures asynchrones.
     * Injecté uniquement par les tests (via `backgroundScope`) ; en production
     * il s'agit du `viewModelScope` lié au cycle de vie de l'écran.
     */
    private val scope: CoroutineScope = coroutineScope ?: viewModelScope

    val recipes: StateFlow<List<Recipe>> = recipeDao.observeRecipes()
        .map { recipesWithIngredients ->
            recipesWithIngredients.map { recipeWithIngredients ->
                Recipe(
                    id = recipeWithIngredients.recipe.id,
                    title = recipeWithIngredients.recipe.title,
                    image = recipeWithIngredients.recipe.image,
                    cookingTime = recipeWithIngredients.recipe.cookingTime,
                    description = recipeWithIngredients.recipe.description,
                    isFavorite = recipeWithIngredients.recipe.isFavorite,
                    ingredientList = recipeWithIngredients.ingredients.map { ingredient ->
                        Ingredient(
                            quantity = ingredient.quantity,
                            name = ingredient.name
                        )
                    }
                )
            }
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun addRecipe(
        title: String,
        image: String,
        cookingTime: Int,
        description: String,
        ingredients: List<Ingredient>,
        steps: List<String>,
        onComplete: () -> Unit
    ) {
        scope.launch {
            recipeDao.insertRecipeWithIngredients(
                recipe = RecipeEntity(
                    title = title,
                    image = image,
                    cookingTime = cookingTime,
                    description = description
                ),
                ingredients = ingredients,
                steps = steps
            )
            onComplete()
        }
    }

    fun updateRecipe(
        id: Int,
        title: String,
        image: String,
        cookingTime: Int,
        description: String,
        ingredients: List<Ingredient>,
        steps: List<String>,
        onComplete: () -> Unit
    ) {
        scope.launch {
            recipeDao.updateRecipeWithIngredients(
                id = id,
                title = title,
                image = image,
                cookingTime = cookingTime,
                description = description,
                ingredients = ingredients,
                steps = steps
            )
            onComplete()
        }
    }

    fun setFavorite(id: Int, isFavorite: Boolean) {
        scope.launch {
            recipeDao.setFavorite(id, isFavorite)
        }
    }

    fun deleteRecipe(id: Int) {
        scope.launch {
            recipeDao.deleteRecipeById(id)
        }
    }

    fun observeSteps(recipeId: Int): Flow<List<RecipeStep>> =
        recipeDao.observeSteps(recipeId)
            .map { steps ->
                steps.map { step ->
                    RecipeStep(
                        id = step.id,
                        instruction = step.instruction,
                        isDone = step.isDone
                    )
                }
            }

    fun addStep(recipeId: Int, instruction: String, onInserted: (Long) -> Unit) {
        scope.launch {
            val newStepId = recipeDao.insertStep(
                StepEntity(
                    recipeId = recipeId,
                    position = recipeDao.nextStepPosition(recipeId),
                    instruction = instruction
                )
            )
            onInserted(newStepId)
        }
    }

    fun updateStepInstruction(stepId: Long, instruction: String) {
        scope.launch {
            recipeDao.updateStepInstruction(stepId, instruction)
        }
    }

    fun toggleStep(stepId: Long, isDone: Boolean) {
        scope.launch {
            recipeDao.setStepDone(stepId, isDone)
        }
    }

    fun deleteStep(stepId: Long) {
        scope.launch {
            recipeDao.deleteStepById(stepId)
        }
    }

    fun resetSteps(recipeId: Int) {
        scope.launch {
            recipeDao.resetSteps(recipeId)
        }
    }

    companion object {
        fun from(application: Application): StateViewModel = StateViewModel(
            RecipeDatabase.getInstance(application).recipeDao()
        )
    }
}
