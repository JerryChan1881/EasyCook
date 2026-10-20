package com.example.easycook

import com.example.easycook.data.IngredientEntity
import com.example.easycook.data.RecipeDao
import com.example.easycook.data.RecipeEntity
import com.example.easycook.data.RecipeWithIngredients
import com.example.easycook.data.StepEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Faux DAO реалиant [RecipeDao] entirely en memoire.
 *
 * Objectif : tester [StateViewModel] sans Room, sans Android et sans emulateur.
 * On reproduit le comportement qui compte pour le ViewModel :
 * - [observeRecipes] emet la liste a jour a chaque ecriture
 * - [nextStepPosition] calcule la position suivante comme le fait SQL
 * - les identifiants sont auto-increments comme un AUTOINCREMENT Room
 */
class FakeRecipeDao(
    initialRecipes: List<RecipeEntity> = emptyList(),
    initialIngredients: List<IngredientEntity> = emptyList(),
    initialSteps: List<StepEntity> = emptyList()
) : RecipeDao {

    private val recipes = MutableStateFlow(initialRecipes)
    private val ingredients = MutableStateFlow(initialIngredients)
    private val steps = MutableStateFlow(initialSteps)

    private var nextRecipeId = (initialRecipes.maxOfOrNull { it.id } ?: 0) + 1
    private var nextIngredientId = (initialIngredients.maxOfOrNull { it.id } ?: 0) + 1
    private var nextStepEntityId = (initialSteps.maxOfOrNull { it.id } ?: 0L) + 1

    override fun observeRecipes(): Flow<List<RecipeWithIngredients>> =
        recipes.map { recipeList ->
            recipeList.map { recipe ->
                RecipeWithIngredients(
                    recipe = recipe,
                    ingredients = ingredients.value.filter { it.recipeId == recipe.id }
                )
            }
        }

    override suspend fun insertRecipe(recipe: RecipeEntity): Long {
        val assignedId = if (recipe.id == 0) nextRecipeId else recipe.id
        if (recipe.id == 0) {
            nextRecipeId++
        }
        recipes.value = recipes.value.filterNot { it.id == assignedId } + recipe.copy(id = assignedId)
        return assignedId.toLong()
    }

    override suspend fun insertIngredients(ingredients: List<IngredientEntity>) {
        val withIds = ingredients.map { ingredient ->
            val assignedId = if (ingredient.id == 0) nextIngredientId else ingredient.id
            if (ingredient.id == 0) {
                nextIngredientId++
            }
            ingredient.copy(id = assignedId)
        }
        this.ingredients.value = this.ingredients.value + withIds
    }

    override suspend fun deleteRecipeById(id: Int) {
        recipes.value = recipes.value.filterNot { it.id == id }
        ingredients.value = ingredients.value.filterNot { it.recipeId == id }
        steps.value = steps.value.filterNot { it.recipeId == id }
    }

    override suspend fun deleteIngredientsByRecipeId(recipeId: Int) {
        ingredients.value = ingredients.value.filterNot { it.recipeId == recipeId }
    }

    override fun observeSteps(recipeId: Int): Flow<List<StepEntity>> =
        steps.map { stepList ->
            stepList.filter { it.recipeId == recipeId }.sortedWith(
                compareBy({ it.position }, { it.id })
            )
        }

    override suspend fun nextStepPosition(recipeId: Int): Int =
        steps.value
            .filter { it.recipeId == recipeId }
            .maxOfOrNull { it.position }
            ?.plus(1)
            ?: 0

    override suspend fun insertStep(step: StepEntity): Long {
        val assignedId = nextStepEntityId++
        steps.value = steps.value + step.copy(id = assignedId)
        return assignedId
    }

    override suspend fun deleteStepById(stepId: Long) {
        steps.value = steps.value.filterNot { it.id == stepId }
    }

    override suspend fun deleteStepsByRecipeId(recipeId: Int) {
        steps.value = steps.value.filterNot { it.recipeId == recipeId }
    }

    override suspend fun updateStepInstruction(stepId: Long, instruction: String) {
        steps.value = steps.value.map { step ->
            if (step.id == stepId) step.copy(instruction = instruction) else step
        }
    }

    override suspend fun setStepDone(stepId: Long, isDone: Boolean) {
        steps.value = steps.value.map { step ->
            if (step.id == stepId) step.copy(isDone = isDone) else step
        }
    }

    override suspend fun resetSteps(recipeId: Int) {
        steps.value = steps.value.map { step ->
            if (step.recipeId == recipeId) step.copy(isDone = false) else step
        }
    }

    override suspend fun updateRecipe(
        id: Int,
        title: String,
        image: String,
        cookingTime: Int,
        description: String
    ) {
        recipes.value = recipes.value.map { recipe ->
            if (recipe.id == id) {
                recipe.copy(
                    title = title,
                    image = image,
                    cookingTime = cookingTime,
                    description = description
                )
            } else {
                recipe
            }
        }
    }
}
