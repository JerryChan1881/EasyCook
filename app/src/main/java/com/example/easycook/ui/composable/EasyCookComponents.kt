package com.example.easycook.ui.composable

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RamenDining
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.easycook.R

/** Pastille verte affichant le temps de cuisson d'une recette. */
@Composable
fun CookingTimeChip(
    minutes: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Schedule,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = stringResource(R.string.recipe_card_cooking_time, minutes),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

/**
 * Bandeau illustrant une recette, en attendant l'affichage des vraies images.
 * La couleur alterne entre orange et vert d'une recette à l'autre.
 */
@Composable
fun RecipeBanner(
    recipeId: Int,
    modifier: Modifier = Modifier,
    height: Dp = 96.dp
) {
    val isOrange = recipeId % 2 == 0
    val background = if (isOrange) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val tint = if (isOrange) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.secondary
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isOrange) Icons.Default.Restaurant else Icons.Default.RamenDining,
            contentDescription = null,
            modifier = Modifier.size(height / 3),
            tint = tint
        )
    }
}

/** Bouton flottant orange foncé commun à tous les écrans. */
@Composable
fun EasyCookFab(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription
        )
    }
}

/** Carte blanche bordée d'orange clair, utilisée pour regrouper une section d'écran. */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}
