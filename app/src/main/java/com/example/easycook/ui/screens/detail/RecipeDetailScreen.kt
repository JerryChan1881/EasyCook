package com.example.easycook.ui.screens.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.easycook.R
import com.example.easycook.ui.composable.EasyCookFab
import com.example.easycook.ui.composable.FormError
import com.example.easycook.ui.composable.RecipeFormFields
import com.example.easycook.ui.composable.rememberRecipeFormState

@Composable
fun RecipeDetailScreen(
    viewModel: RecipeDetailViewModel,
    onBack: () -> Unit,
    onFollowSteps: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Rien à afficher tant que la recette n'est pas chargée, ou si elle n'existe plus
    val recipe = state.recipe ?: return

    // Le formulaire repart de la recette enregistrée à chaque entrée en modification
    val form = rememberRecipeFormState(key = state.isEditing) { state.draft()!! }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back)
                )
            }

            if (state.isEditing) {
                RecipeFormFields(
                    state = form,
                    recipeId = recipe.id,
                    stepsTitle = stringResource(R.string.recipe_form_steps)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = viewModel::cancelEditing,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.recipe_form_cancel_changes))
                }

                FormError(state.error)
            } else {
                RecipeDetailView(
                    recipe = recipe,
                    steps = state.steps,
                    onFollowSteps = onFollowSteps
                )
            }

            // Le bouton flottant ne doit pas cacher la fin de la page
            Spacer(modifier = Modifier.height(88.dp))
        }

        EasyCookFab(
            onClick = {
                if (state.isEditing) viewModel.save(form.toDraft()) else viewModel.startEditing()
            },
            icon = if (state.isEditing) Icons.Default.Check else Icons.Default.Edit,
            contentDescription = if (state.isEditing) {
                stringResource(R.string.recipe_detail_confirm_changes)
            } else {
                stringResource(R.string.recipe_detail_edit)
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }
}
