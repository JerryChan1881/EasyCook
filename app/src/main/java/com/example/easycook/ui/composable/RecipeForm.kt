package com.example.easycook.ui.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.easycook.R
import com.example.easycook.data.RecipeDraft
import com.example.easycook.data.RecipeError
import com.example.easycook.ui.image.RecipeImageField
import com.example.easycook.ui.messageRes

/**
 * Saisie en cours d'un formulaire de recette (création ou modification).
 * Conservée lors d'une rotation de l'écran grâce à [rememberRecipeFormState].
 */
@Stable
class RecipeFormState(initial: RecipeDraft) {
    var title by mutableStateOf(initial.title)
    var image by mutableStateOf(initial.image)
    var cookingTime by mutableStateOf(initial.cookingTime)
    var description by mutableStateOf(initial.description)
    val ingredients = initial.ingredients.toMutableStateList()
    val steps = initial.steps.toMutableStateList()

    fun toDraft() = RecipeDraft(
        title = title,
        image = image,
        cookingTime = cookingTime,
        description = description,
        ingredients = ingredients.toList(),
        steps = steps.toList()
    )

    companion object {
        val Saver: Saver<RecipeFormState, Any> = listSaver(
            save = { state ->
                val draft = state.toDraft()
                listOf(
                    draft.title,
                    draft.image,
                    draft.cookingTime,
                    draft.description,
                    ArrayList(draft.ingredients),
                    ArrayList(draft.steps)
                )
            },
            restore = { saved ->
                @Suppress("UNCHECKED_CAST")
                RecipeFormState(
                    RecipeDraft(
                        title = saved[0] as String,
                        image = saved[1] as String,
                        cookingTime = saved[2] as String,
                        description = saved[3] as String,
                        ingredients = saved[4] as List<String>,
                        steps = saved[5] as List<String>
                    )
                )
            }
        )
    }
}

/** Formulaire conservé lors d'une rotation ; il repart de [initial] chaque fois que [key] change. */
@Composable
fun rememberRecipeFormState(key: Any? = null, initial: () -> RecipeDraft = { RecipeDraft() }): RecipeFormState =
    rememberSaveable(key, saver = RecipeFormState.Saver) { RecipeFormState(initial()) }

/**
 * Champs du formulaire de recette : nom, photo, temps de cuisson, description,
 * ingrédients (un champ libre par ligne) et étapes de préparation.
 */
@Composable
fun RecipeFormFields(
    state: RecipeFormState,
    /** Recette modifiée (pour l'illustration de remplacement), 0 pour une nouvelle recette. */
    recipeId: Int,
    stepsTitle: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = state.title,
            onValueChange = { state.title = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.recipe_form_name)) },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        RecipeImageField(
            recipeId = recipeId,
            imageName = state.image,
            onImageChange = { state.image = it }
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = state.cookingTime,
            onValueChange = { state.cookingTime = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.recipe_form_cooking_time)) },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = state.description,
            onValueChange = { state.description = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.recipe_form_description)) },
            minLines = 4,
            placeholder = { Text(stringResource(R.string.recipe_form_description_placeholder)) }
        )

        FormSectionTitle(stringResource(R.string.recipe_form_ingredients))

        state.ingredients.forEachIndexed { index, ingredient ->
            EditableLine(
                value = ingredient,
                onValueChange = { state.ingredients[index] = it },
                label = stringResource(R.string.recipe_form_ingredient),
                placeholder = stringResource(R.string.recipe_form_ingredient_placeholder),
                deleteDescription = stringResource(R.string.recipe_form_delete_ingredient),
                // On garde toujours au moins une ligne d'ingrédient
                onDelete = { if (state.ingredients.size > 1) state.ingredients.removeAt(index) },
                singleLine = true
            )
        }

        AddLineButton(
            label = stringResource(R.string.recipe_form_add_ingredient),
            onClick = { state.ingredients.add("") }
        )

        FormSectionTitle(stepsTitle)

        state.steps.forEachIndexed { index, step ->
            EditableLine(
                value = step,
                onValueChange = { state.steps[index] = it },
                label = stringResource(R.string.step_instruction),
                placeholder = stringResource(R.string.step_instruction_placeholder),
                deleteDescription = stringResource(R.string.step_delete),
                onDelete = { state.steps.removeAt(index) },
                number = index + 1
            )
        }

        AddLineButton(
            label = stringResource(R.string.recipe_form_add_step),
            onClick = { state.steps.add("") }
        )
    }
}

@Composable
private fun FormSectionTitle(text: String) {
    Spacer(modifier = Modifier.height(24.dp))
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(8.dp))
}

/** Ligne d'ingrédient ou d'étape : champ de saisie et bouton de suppression (numéro pour une étape). */
@Composable
private fun EditableLine(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    deleteDescription: String,
    onDelete: () -> Unit,
    number: Int? = null,
    singleLine: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        number?.let {
            Text(
                text = stringResource(R.string.step_number, it),
                style = MaterialTheme.typography.titleMedium
            )
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            singleLine = singleLine,
            maxLines = if (singleLine) 1 else 3
        )
        IconButton(onClick = onDelete) {
            Icon(imageVector = Icons.Default.Delete, contentDescription = deleteDescription)
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun AddLineButton(label: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Icon(imageVector = Icons.Default.Add, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(label)
    }
}

/** Message d'erreur de validation affiché sous le formulaire ; rien si [error] est null. */
@Composable
fun FormError(error: RecipeError?) {
    error ?: return
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = stringResource(error.messageRes()),
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium
    )
}
