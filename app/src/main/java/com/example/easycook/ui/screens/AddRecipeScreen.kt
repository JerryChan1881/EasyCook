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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.easycook.R
import com.example.easycook.StateViewModel
import com.example.easycook.data.Ingredient
import com.example.easycook.data.RecipeError
import com.example.easycook.data.RecipeValidator
import com.example.easycook.ui.messageRes

data class IngredientForm(
    val quantity: String = "",
    val name: String = ""
)

data class StepForm(
    val description: String = ""
)

@Composable
fun AddRecipeScreen(
    navController: NavHostController,
    stateViewModel: StateViewModel
) {
    var title by remember { mutableStateOf("") }
    var image by remember { mutableStateOf("") }
    var cookingTime by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<RecipeError?>(null) }
    val ingredients = remember { mutableStateListOf(IngredientForm()) }
    val steps = remember { mutableStateListOf(StepForm()) }

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

        OutlinedTextField(
            value = image,
            onValueChange = { image = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.recipe_form_image)) },
            singleLine = true
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
                    value = ingredient.quantity,
                    onValueChange = { value ->
                        ingredients[index] = ingredient.copy(quantity = value)
                    },
                    modifier = Modifier.weight(0.8f),
                    label = { Text(stringResource(R.string.recipe_form_quantity)) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = ingredient.name,
                    onValueChange = { value ->
                        ingredients[index] = ingredient.copy(name = value)
                    },
                    modifier = Modifier.weight(1.2f),
                    label = { Text(stringResource(R.string.recipe_form_ingredient)) },
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
            onClick = { ingredients.add(IngredientForm()) },
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
                    value = step.description,
                    onValueChange = { value ->
                        steps[index] = step.copy(description = value)
                    },
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
            onClick = { steps.add(StepForm()) },
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
                val rawIngredients = ingredients.map {
                    Ingredient(quantity = it.quantity, name = it.name)
                }

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
                        image = image.trim(),
                        cookingTime = RecipeValidator.parseCookingTime(cookingTime) ?: 0,
                        description = description.trim(),
                        ingredients = RecipeValidator.cleanIngredients(rawIngredients),
                        steps = RecipeValidator.cleanSteps(steps.map { it.description }),
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
