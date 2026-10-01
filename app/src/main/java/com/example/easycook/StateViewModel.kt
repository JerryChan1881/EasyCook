package com.example.easycook

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.easycook.data.Ingredient
import com.example.easycook.data.Recipe
import com.example.easycook.data.RecipeDatabase
import com.example.easycook.data.RecipeEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StateViewModel(application: Application) : AndroidViewModel(application) {

    private val recipeDao = RecipeDatabase
        .getInstance(application)
        .recipeDao()

    val recipes: StateFlow<List<Recipe>> = recipeDao.observeRecipes()
        .map { recipesWithIngredients ->
            recipesWithIngredients.map { recipeWithIngredients ->
                Recipe(
                    id = recipeWithIngredients.recipe.id,
                    title = recipeWithIngredients.recipe.title,
                    image = recipeWithIngredients.recipe.image,
                    cookingTime = recipeWithIngredients.recipe.cookingTime,
                    description = recipeWithIngredients.recipe.description,
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
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            if (recipeDao.countRecipes() == 0) {
                insertInitialRecipes()
            }
        }
    }

    fun getRecipeById(id: Int): Recipe? {
        return recipes.value.find { it.id == id }
    }

    fun addRecipe(
        title: String,
        image: String,
        cookingTime: Int,
        description: String,
        ingredients: List<Ingredient>,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            recipeDao.insertRecipeWithIngredients(
                recipe = RecipeEntity(
                    title = title,
                    image = image,
                    cookingTime = cookingTime,
                    description = description
                ),
                ingredients = ingredients
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
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            recipeDao.updateRecipeWithIngredients(
                id = id,
                title = title,
                image = image,
                cookingTime = cookingTime,
                description = description,
                ingredients = ingredients
            )
            onComplete()
        }
    }

    fun deleteRecipe(id: Int) {
        viewModelScope.launch {
            recipeDao.deleteRecipeById(id)
        }
    }

    private suspend fun insertInitialRecipes() {
        recipeDao.insertRecipeWithIngredients(
            RecipeEntity(
                title = "Spaghetti Carbonara",
                image = "",
                cookingTime = 30,
                description = "Une recette romaine simple et crémeuse, sans crème : les œufs et le parmesan fondent avec les pâtes grâce à l'eau de cuisson."
            ),
            listOf(
                Ingredient("200 g", "Spaghetti"),
                Ingredient("100 g", "Lardons"),
                Ingredient("2", "Œufs"),
                Ingredient("50 g", "Parmesan")
            )
        )
        recipeDao.insertRecipeWithIngredients(
            RecipeEntity(
                title = "Chicken Curry",
                image = "",
                cookingTime = 45,
                description = "Un curry de poulet doux et parfumé, parfait avec du riz basmati. mijotez les épices avec le lait de coco pour libérer les saveurs."
            ),
            listOf(
                Ingredient("300 g", "Chicken"),
                Ingredient("200 ml", "Coconut milk"),
                Ingredient("2 tbsp", "Curry")
            )
        )
        recipeDao.insertRecipeWithIngredients(
            RecipeEntity(
                title = "Pizza Margherita",
                image = "",
                cookingTime = 40,
                description = "La pizza classique napolitaine : pâte fine, tomate, mozzarella et basilic frais. Cuisson à feu vif pour une pâte croustillante."
            ),
            listOf(
                Ingredient("250 g", "Flour"),
                Ingredient("150 g", "Tomato sauce"),
                Ingredient("125 g", "Mozzarella")
            )
        )
    }
}
