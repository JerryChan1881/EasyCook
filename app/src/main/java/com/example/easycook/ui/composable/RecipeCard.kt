package com.example.easycook.ui.composable

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.easycook.R
import com.example.easycook.data.Recipe

/**
 * Carte carrée d'une recette, pensée pour une grille de 2 ou 3 colonnes :
 * illustration en haut (avec les actions favori et suppression), titre et temps en bas.
 * Le cœur est plein quand la recette est en favori.
 */
@Composable
fun RecipeCard(
    recipe: Recipe,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            RecipeBanner(
                recipeId = recipe.id,
                modifier = Modifier.fillMaxSize()
            )

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                CardActionButton(
                    icon = if (recipe.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (recipe.isFavorite) {
                        stringResource(R.string.recipe_card_remove_favorite)
                    } else {
                        stringResource(R.string.recipe_card_add_favorite)
                    },
                    tint = MaterialTheme.colorScheme.primary,
                    onClick = onToggleFavorite
                )
                CardActionButton(
                    icon = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.home_delete_recipe),
                    tint = MaterialTheme.colorScheme.error,
                    onClick = onDelete
                )
            }
        }

        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = recipe.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (recipe.hasCookingTime) {
                Spacer(modifier = Modifier.height(4.dp))

                CookingTimeChip(minutes = recipe.cookingTime)
            }
        }
    }
}

/** Petit bouton rond posé sur l'illustration, lisible quelle que soit la couleur du bandeau. */
@Composable
private fun CardActionButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerLowest
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier
                    .padding(6.dp)
                    .size(18.dp),
                tint = tint
            )
        }
    }
}
