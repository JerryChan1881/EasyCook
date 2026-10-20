package com.example.easycook.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.easycook.R
import com.example.easycook.Routes
import com.example.easycook.StateViewModel
import com.example.easycook.data.Recipe
import com.example.easycook.ui.composable.EasyCookFab
import com.example.easycook.ui.composable.RecipeCard

@Composable
fun HomeScreen(
    navController: NavHostController,
    stateViewModel: StateViewModel,
    modifier: Modifier = Modifier
) {
    val recipes by stateViewModel.recipes.collectAsState()
    var recipeToDelete by remember { mutableStateOf<Recipe?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.home_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.home_search)
                    )
                }
            }

            Text(
                text = stringResource(R.string.home_subtitle),
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (recipes.isEmpty()) {
                EmptyRecipes(modifier = Modifier.fillMaxSize())
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(recipes) { recipe ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RecipeCard(
                                recipe = recipe,
                                onClick = {
                                    navController.navigate(Routes.recipeDetail(recipe.id))
                                },
                                modifier = Modifier.weight(1f)
                            )

                            IconButton(onClick = { recipeToDelete = recipe }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.home_delete_recipe),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }

        EasyCookFab(
            onClick = { navController.navigate(Routes.ADD_RECIPE) },
            icon = Icons.Default.Add,
            contentDescription = stringResource(R.string.home_add_recipe),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )

        recipeToDelete?.let { recipe ->
            AlertDialog(
                onDismissRequest = { recipeToDelete = null },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                title = { Text(stringResource(R.string.home_delete_recipe)) },
                text = {
                    Text(stringResource(R.string.home_delete_recipe_confirm, recipe.title))
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            stateViewModel.deleteRecipe(recipe.id)
                            recipeToDelete = null
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.action_delete),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { recipeToDelete = null }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }
    }
}

@Composable
private fun EmptyRecipes(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.RestaurantMenu,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.home_empty_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.home_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
