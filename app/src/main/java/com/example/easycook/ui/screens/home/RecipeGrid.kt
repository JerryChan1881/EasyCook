package com.example.easycook.ui.screens.home

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.easycook.R
import com.example.easycook.data.Recipe
import com.example.easycook.ui.composable.RecipeCard

/** Grille des recettes : 2 colonnes en portrait, 3 en paysage. */
@Composable
fun RecipeGrid(
    recipes: List<Recipe>,
    onOpen: (Recipe) -> Unit,
    onToggleFavorite: (Recipe) -> Unit,
    onDelete: (Recipe) -> Unit,
    modifier: Modifier = Modifier
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    LazyVerticalGrid(
        columns = GridCells.Fixed(if (isLandscape) 3 else 2),
        modifier = modifier.fillMaxSize(),
        // Marge du bas : la dernière ligne n'est pas cachée par le bouton +
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(recipes, key = { it.id }) { recipe ->
            RecipeCard(
                recipe = recipe,
                onClick = { onOpen(recipe) },
                onToggleFavorite = { onToggleFavorite(recipe) },
                onDelete = { onDelete(recipe) }
            )
        }
    }
}

@Composable
fun EmptyRecipes(modifier: Modifier = Modifier) {
    CenteredMessage(
        icon = Icons.Default.RestaurantMenu,
        title = stringResource(R.string.home_empty_title),
        hint = stringResource(R.string.home_empty_hint),
        modifier = modifier
    )
}

@Composable
fun NoSearchResult(query: String, modifier: Modifier = Modifier) {
    CenteredMessage(
        icon = Icons.Default.SearchOff,
        title = stringResource(R.string.home_search_no_result, query.trim()),
        modifier = modifier
    )
}

@Composable
private fun CenteredMessage(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    hint: String? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )

        hint?.let {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
