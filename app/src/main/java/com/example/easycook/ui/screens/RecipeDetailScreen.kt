package com.example.easycook.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.easycook.R
import com.example.easycook.Routes
import com.example.easycook.StateViewModel
import com.example.easycook.data.Ingredient
import com.example.easycook.data.Recipe
import com.example.easycook.data.RecipeError
import com.example.easycook.data.RecipeValidator
import com.example.easycook.ui.messageRes

@Composable
fun RecipeDetailScreen(
    recipe: Recipe,
    navController: NavHostController,
    stateViewModel: StateViewModel
) {
    var isEditing by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf(recipe.title) }
    var image by remember { mutableStateOf(recipe.image) }
    var cookingTime by remember { mutableStateOf(recipe.cookingTime.toString()) }
    var description by remember { mutableStateOf(recipe.description) }
    var errorMessage by remember { mutableStateOf<RecipeError?>(null) }
    val ingredients = remember {
        mutableStateListOf<IngredientForm>().apply {
            addAll(recipe.ingredientList.map { IngredientForm(it.quantity, it.name) })
        }
    }
    val persistedStepsState = stateViewModel.observeSteps(recipe.id).collectAsState(initial = null)
    val persistedSteps = persistedStepsState.value.orEmpty()
    val steps = remember { mutableStateListOf<String>() }
    var stepsLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(persistedStepsState.value) {
        val loadedSteps = persistedStepsState.value
        if (!stepsLoaded && loadedSteps != null) {
            steps.clear()
            steps.addAll(loadedSteps.map { it.instruction })
            stepsLoaded = true
        }
    }

    val resetForm: () -> Unit = {
        title = recipe.title
        image = recipe.image
        cookingTime = recipe.cookingTime.toString()
        description = recipe.description
        ingredients.clear()
        ingredients.addAll(recipe.ingredientList.map { IngredientForm(it.quantity, it.name) })
        steps.clear()
        steps.addAll(persistedSteps.map { it.instruction })
        errorMessage = null
    }

    val confirmChanges: () -> Unit = {
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
            stateViewModel.updateRecipe(
                id = recipe.id,
                title = title.trim(),
                image = image.trim(),
                cookingTime = RecipeValidator.parseCookingTime(cookingTime) ?: 0,
                description = description.trim(),
                ingredients = RecipeValidator.cleanIngredients(rawIngredients),
                steps = RecipeValidator.cleanSteps(steps),
                onComplete = {
                    isEditing = false
                    errorMessage = null
                }
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            IconButton(
                onClick = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back)
                )
            }

            if (isEditing) {
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
                    placeholder = {
                        Text(stringResource(R.string.recipe_form_description_placeholder))
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.recipe_form_ingredients),
                    style = MaterialTheme.typography.titleLarge
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
                    text = stringResource(R.string.recipe_form_steps),
                    style = MaterialTheme.typography.titleLarge
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

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        resetForm()
                        isEditing = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.recipe_form_cancel_changes))
                }
            } else {
                Text(
                    text = recipe.title,
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = pluralStringResource(R.plurals.recipe_detail_cooking_time, recipe.cookingTime, recipe.cookingTime),
                    style = MaterialTheme.typography.bodyLarge
                )

                if (recipe.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = stringResource(R.string.recipe_detail_description),
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = recipe.description,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.recipe_form_ingredients),
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(8.dp))

                recipe.ingredientList.forEach { ingredient ->
                    Text(
                        text = stringResource(R.string.recipe_detail_ingredient_line, ingredient.quantity, ingredient.name),
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (persistedSteps.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = stringResource(R.string.recipe_detail_preparation),
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    persistedSteps.forEachIndexed { index, step ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.step_number, index + 1),
                                style = MaterialTheme.typography.bodyLarge
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

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedButton(
                    onClick = { navController.navigate(Routes.stepByStep(recipe.id)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Checklist,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.recipe_detail_follow_steps))
                }
            }

            errorMessage?.let { message ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(message.messageRes()),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(88.dp))
        }

        FloatingActionButton(
            onClick = {
                if (isEditing) {
                    confirmChanges()
                } else {
                    errorMessage = null
                    isEditing = true
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                contentDescription = if (isEditing) {
                    stringResource(R.string.recipe_detail_confirm_changes)
                } else {
                    stringResource(R.string.recipe_detail_edit)
                }
            )
        }
    }
}
