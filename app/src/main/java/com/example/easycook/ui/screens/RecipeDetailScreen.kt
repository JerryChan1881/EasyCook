package com.example.easycook.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.easycook.R
import com.example.easycook.Routes
import com.example.easycook.StateViewModel
import com.example.easycook.data.IngredientParser
import com.example.easycook.data.Recipe
import com.example.easycook.data.RecipeError
import com.example.easycook.data.RecipeValidator
import com.example.easycook.ui.composable.CookingTimeChip
import com.example.easycook.ui.composable.EasyCookFab
import com.example.easycook.ui.composable.SectionCard
import com.example.easycook.ui.image.CropMath
import com.example.easycook.ui.image.RecipeImage
import com.example.easycook.ui.image.RecipeImageField
import com.example.easycook.ui.messageRes
import com.example.easycook.ui.rememberSaveableStringList

@Composable
fun RecipeDetailScreen(
    recipe: Recipe,
    navController: NavHostController,
    stateViewModel: StateViewModel
) {
    var isEditing by rememberSaveable { mutableStateOf(false) }
    var title by rememberSaveable { mutableStateOf(recipe.title) }
    var image by rememberSaveable { mutableStateOf(recipe.image) }
    var cookingTime by rememberSaveable { mutableStateOf(RecipeValidator.formatCookingTime(recipe.cookingTime)) }
    var description by rememberSaveable { mutableStateOf(recipe.description) }
    var errorMessage by rememberSaveable { mutableStateOf<RecipeError?>(null) }
    val ingredients = rememberSaveableStringList {
        recipe.ingredientList.map(IngredientParser::format)
    }
    val persistedStepsState = stateViewModel.observeSteps(recipe.id).collectAsState(initial = null)
    val persistedSteps = persistedStepsState.value.orEmpty()
    val steps = rememberSaveableStringList { emptyList() }
    var stepsLoaded by rememberSaveable { mutableStateOf(false) }

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
        cookingTime = RecipeValidator.formatCookingTime(recipe.cookingTime)
        description = recipe.description
        ingredients.clear()
        ingredients.addAll(recipe.ingredientList.map(IngredientParser::format))
        steps.clear()
        steps.addAll(persistedSteps.map { it.instruction })
        errorMessage = null
    }

    val confirmChanges: () -> Unit = {
        val rawIngredients = ingredients.map(IngredientParser::parse)

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
                image = image,
                cookingTime = RecipeValidator.cookingTimeOrNone(cookingTime),
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

                RecipeImageField(
                    recipeId = recipe.id,
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
                    placeholder = {
                        Text(stringResource(R.string.recipe_form_description_placeholder))
                    }
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
                    text = stringResource(R.string.recipe_form_steps),
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
                // Une photo garde les proportions du recadrage ; l'illustration reste plus compacte
                RecipeImage(
                    recipeId = recipe.id,
                    imagePath = recipe.image,
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
                    Spacer(modifier = Modifier.height(16.dp))

                    SectionCard {
                        Text(
                            text = stringResource(R.string.recipe_detail_description),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = recipe.description,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                SectionCard {
                    Text(
                        text = stringResource(R.string.recipe_form_ingredients),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    recipe.ingredientList.forEach { ingredient ->
                        Text(
                            text = IngredientParser.format(ingredient),
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                if (persistedSteps.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))

                    SectionCard {
                        Text(
                            text = stringResource(R.string.recipe_detail_preparation),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        persistedSteps.forEachIndexed { index, step ->
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

        EasyCookFab(
            onClick = {
                if (isEditing) {
                    confirmChanges()
                } else {
                    errorMessage = null
                    isEditing = true
                }
            },
            icon = if (isEditing) Icons.Default.Check else Icons.Default.Edit,
            contentDescription = if (isEditing) {
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
