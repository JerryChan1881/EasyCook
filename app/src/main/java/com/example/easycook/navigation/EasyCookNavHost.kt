package com.example.easycook.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.easycook.data.RecipeRepository
import com.example.easycook.transfer.TextDocuments
import com.example.easycook.ui.screens.addrecipe.AddRecipeScreen
import com.example.easycook.ui.screens.addrecipe.AddRecipeViewModel
import com.example.easycook.ui.screens.detail.RecipeDetailScreen
import com.example.easycook.ui.screens.detail.RecipeDetailViewModel
import com.example.easycook.ui.screens.home.HomeScreen
import com.example.easycook.ui.screens.home.HomeViewModel
import com.example.easycook.ui.screens.share.ShareRecipesScreen
import com.example.easycook.ui.screens.share.ShareRecipesViewModel
import com.example.easycook.ui.screens.steps.StepByStepScreen
import com.example.easycook.ui.screens.steps.StepByStepViewModel

/**
 * Graphe de navigation. Chaque écran reçoit son propre ViewModel, rattaché à sa destination :
 * il est créé à l'ouverture de l'écran et détruit quand on le quitte.
 */
@Composable
fun EasyCookNavHost(
    repository: RecipeRepository,
    documents: TextDocuments,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    // navigateUp ne quitte jamais l'écran d'accueil, même en cas de double appui sur « Retour »
    val goBack: () -> Unit = { navController.navigateUp() }

    NavHost(
        navController = navController,
        startDestination = Route.Home,
        modifier = modifier
    ) {
        composable<Route.Home> {
            HomeScreen(
                viewModel = viewModel { HomeViewModel(repository, createSavedStateHandle(), documents) },
                onOpenRecipe = { navController.navigate(Route.RecipeDetail(it)) },
                onAddRecipe = { navController.navigate(Route.AddRecipe(it)) },
                onShare = { navController.navigate(Route.ShareRecipes(it)) }
            )
        }

        composable<Route.AddRecipe> { entry ->
            val route = entry.toRoute<Route.AddRecipe>()
            AddRecipeScreen(
                viewModel = viewModel { AddRecipeViewModel(route.profileId, repository) },
                onBack = goBack
            )
        }

        composable<Route.RecipeDetail> { entry ->
            val route = entry.toRoute<Route.RecipeDetail>()
            RecipeDetailScreen(
                viewModel = viewModel { RecipeDetailViewModel(route.recipeId, repository, createSavedStateHandle()) },
                onBack = goBack,
                onFollowSteps = { navController.navigate(Route.StepByStep(route.recipeId)) }
            )
        }

        composable<Route.StepByStep> { entry ->
            val route = entry.toRoute<Route.StepByStep>()
            StepByStepScreen(
                viewModel = viewModel { StepByStepViewModel(route.recipeId, repository, createSavedStateHandle()) },
                onBack = goBack
            )
        }

        composable<Route.ShareRecipes> { entry ->
            val route = entry.toRoute<Route.ShareRecipes>()
            ShareRecipesScreen(
                viewModel = viewModel { ShareRecipesViewModel(route.profileId, repository, createSavedStateHandle()) },
                onFinish = goBack
            )
        }
    }
}
