package com.example.easycook.ui.screens.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.easycook.R
import com.example.easycook.data.IngredientParser
import com.example.easycook.data.Recipe
import com.example.easycook.data.RecipeStep
import com.example.easycook.ui.composable.CookingTimeChip
import com.example.easycook.ui.composable.SectionCard
import com.example.easycook.ui.image.CropMath
import com.example.easycook.ui.image.RecipeImage

/** Consultation d'une recette : photo, titre, temps, description, ingrédients et étapes. */
@Composable
fun RecipeDetailView(
    recipe: Recipe,
    steps: List<RecipeStep>,
    onFollowSteps: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Une photo garde les proportions du recadrage ; l'illustration reste plus compacte
        RecipeImage(
            recipeId = recipe.id,
            imageName = recipe.image,
            modifier = Modifier
                .clip(MaterialTheme.shapes.large)
                .then(
                    if (recipe.image.isNotBlank()) {
                        Modifier.aspectRatio(CropMath.ASPECT_RATIO)
                    } else {
                        Modifier.height(160.dp)
                    }
                ),
            iconSize = 53.dp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = recipe.title,
            style = MaterialTheme.typography.headlineMedium
        )

        if (recipe.hasCookingTime) {
            Spacer(modifier = Modifier.height(8.dp))
            CookingTimeChip(minutes = recipe.cookingTime)
        }

        if (recipe.description.isNotBlank()) {
            DetailSection(title = stringResource(R.string.recipe_detail_description)) {
                Text(
                    text = recipe.description,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        DetailSection(title = stringResource(R.string.recipe_form_ingredients)) {
            recipe.ingredientList.forEach { ingredient ->
                Text(
                    text = IngredientParser.format(ingredient),
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        if (steps.isNotEmpty()) {
            DetailSection(title = stringResource(R.string.recipe_detail_preparation)) {
                steps.forEachIndexed { index, step ->
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.step_number, index + 1),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = step.instruction,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary
            ),
            onClick = onFollowSteps,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(imageVector = Icons.Default.Checklist, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.recipe_detail_follow_steps))
        }
    }
}

/** Carte titrée (description, ingrédients, préparation). */
@Composable
private fun DetailSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Spacer(modifier = Modifier.height(16.dp))

    SectionCard {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        content()
    }
}
