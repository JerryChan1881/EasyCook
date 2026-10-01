package com.example.easycook

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.easycook.ui.screens.AddRecipeScreen
import com.example.easycook.ui.screens.HomeScreen
import com.example.easycook.ui.screens.RecipeDetailScreen

@Composable
fun NavHostModel(
    navController: NavHostController,
    stateViewModel: StateViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = "homeScreen",
        modifier = modifier
    ) {
        composable("homeScreen") {
            println("HOME SCREEN")
            HomeScreen(
                navController = navController,
                stateViewModel = stateViewModel
            )
        }

        composable("recipeDetailScreen/{recipeId}") { backStackEntry ->
            println("RECIPE DETAIL SCREEN")
            val recipeId = backStackEntry.arguments
                ?.getString("recipeId")
                ?.toIntOrNull()

            val recipe = recipeId?.let {
                stateViewModel.getRecipeById(it)
            }

            if (recipe != null) {
                RecipeDetailScreen(
                    recipe = recipe,
                    navController = navController,
                    stateViewModel = stateViewModel
                )
            }
        }

        composable("addRecipeScreen") {
            AddRecipeScreen(
                navController = navController,
                stateViewModel = stateViewModel
            )
        }
    }
}