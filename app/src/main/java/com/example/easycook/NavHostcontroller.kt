package com.example.easycook

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.easycook.ui.screens.AddRecipeScreen
import com.example.easycook.ui.screens.HomeScreen
import com.example.easycook.ui.screens.RecipeDetailScreen
import com.example.easycook.ui.screens.StepByStepScreen

@Composable
fun NavHostModel(
    navController: NavHostController,
    stateViewModel: StateViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                navController = navController,
                stateViewModel = stateViewModel
            )
        }

        composable(Routes.RECIPE_DETAIL) { backStackEntry ->
            val recipes by stateViewModel.recipes.collectAsState()
            val recipeId = backStackEntry.arguments
                ?.getString(Routes.ARG_RECIPE_ID)
                ?.toIntOrNull()

            val recipe = recipeId?.let { id ->
                recipes.find { it.id == id }
            }

            if (recipe != null) {
                RecipeDetailScreen(
                    recipe = recipe,
                    navController = navController,
                    stateViewModel = stateViewModel
                )
            }
        }

        composable(Routes.ADD_RECIPE) {
            AddRecipeScreen(
                navController = navController,
                stateViewModel = stateViewModel
            )
        }

        composable(Routes.STEP_BY_STEP) { backStackEntry ->
            val recipes by stateViewModel.recipes.collectAsState()
            val recipeId = backStackEntry.arguments
                ?.getString(Routes.ARG_RECIPE_ID)
                ?.toIntOrNull()

            val recipe = recipeId?.let { id ->
                recipes.find { it.id == id }
            }

            if (recipe != null) {
                StepByStepScreen(
                    recipe = recipe,
                    navController = navController,
                    stateViewModel = stateViewModel
                )
            }
        }
    }
}