package com.example.easycook.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.easycook.R
import com.example.easycook.StateViewModel
import com.example.easycook.data.IngredientParser
import com.example.easycook.data.RecipeError
import com.example.easycook.data.RecipeValidator
import com.example.easycook.ui.image.RecipeImageField
import com.example.easycook.ui.messageRes
import com.example.easycook.ui.rememberSaveableStringList

@Composable
fun AddRecipeScreen(
    navController: NavHostController,
    stateViewModel: StateViewModel
) {
    var title by rememberSaveable { mutableStateOf("") }
    var image by rememberSaveable { mutableStateOf("") }
    var cookingTime by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var errorMessage by rememberSaveable { mutableStateOf<RecipeError?>(null) }
    val ingredients = rememberSaveableStringList { listOf("") }
    val steps = rememberSaveableStringList { listOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back)
                )
            }
            Text(
                text = stringResource(R.string.add_recipe_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.recipe_form_name)) },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        RecipeImageField(
            recipeId = 0,
            imagePath = image,
            onImageChange = { image = it }
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = cookingTime,
            onValueChange = { cookingTime = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.recipe_form_cooking_time)) },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.recipe_form_description)) },
            minLines = 4,
            placeholder = { Text(stringResource(R.string.recipe_form_description_placeholder)) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.recipe_form_ingredients),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        ingredients.forEachIndexed { index, ingredient ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = ingredient,
                    onValueChange = { value -> ingredients[index] = value },
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(R.string.recipe_form_ingredient)) },
                    placeholder = { Text(stringResource(R.string.recipe_form_ingredient_placeholder)) },
                    singleLine = true
                )
                IconButton(
                    onClick = {
                        if (ingredients.size > 1) {
                            ingredients.removeAt(index)
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.recipe_form_delete_ingredient)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        OutlinedButton(
            onClick = { ingredients.add("") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.recipe_form_add_ingredient))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.recipe_form_steps_optional),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        steps.forEachIndexed { index, step ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.step_number, index + 1),
                    style = MaterialTheme.typography.titleMedium
                )
                OutlinedTextField(
                    value = step,
                    onValueChange = { value -> steps[index] = value },
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(R.string.step_instruction)) },
                    placeholder = { Text(stringResource(R.string.step_instruction_placeholder)) },
                    maxLines = 3
                )
                IconButton(
                    onClick = { steps.removeAt(index) }
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.step_delete)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        OutlinedButton(
            onClick = { steps.add("") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.recipe_form_add_step))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary
            ),
            onClick = {
                val rawIngredients = ingredients.map(IngredientParser::parse)

                val validationError = RecipeValidator.validate(
                    title = title,
                    rawCookingTime = cookingTime,
                    ingredients = rawIngredients
                )

                if (validationError != null) {
                    errorMessage = validationError
                } else {
                    stateViewModel.addRecipe(
                        title = title.trim(),
                        image = image,
                        cookingTime = RecipeValidator.cookingTimeOrNone(cookingTime),
                        description = description.trim(),
                        ingredients = RecipeValidator.cleanIngredients(rawIngredients),
                        steps = RecipeValidator.cleanSteps(steps),
                        onComplete = { navController.popBackStack() }
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.recipe_form_save))
        }

        errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(message.messageRes()),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
