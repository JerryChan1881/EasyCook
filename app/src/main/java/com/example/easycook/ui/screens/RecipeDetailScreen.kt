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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.easycook.StateViewModel
import com.example.easycook.data.Ingredient
import com.example.easycook.data.Recipe

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
    var errorMessage by remember { mutableStateOf<String?>(null) }
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
        val parsedCookingTime = cookingTime.toIntOrNull()
        val validIngredients = ingredients.filter {
            it.quantity.isNotBlank() && it.name.isNotBlank()
        }

        when {
            title.isBlank() -> errorMessage = "Le nom de la recette est obligatoire."
            parsedCookingTime == null || parsedCookingTime <= 0 ->
                errorMessage = "Le temps de cuisson doit être un nombre positif."
            validIngredients.isEmpty() ->
                errorMessage = "Ajoutez au moins un ingrédient complet."
            validIngredients.size != ingredients.size ->
                errorMessage = "Complétez ou supprimez les ingrédients vides."
            else -> {
                stateViewModel.updateRecipe(
                    id = recipe.id,
                    title = title.trim(),
                    image = image.trim(),
                    cookingTime = parsedCookingTime,
                    description = description.trim(),
                    ingredients = validIngredients.map {
                        Ingredient(
                            quantity = it.quantity.trim(),
                            name = it.name.trim()
                        )
                    },
                    steps = steps.map { it.trim() }.filter { it.isNotBlank() },
                    onComplete = {
                        isEditing = false
                        errorMessage = null
                    }
                )
            }
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
                    contentDescription = "Retour"
                )
            }

            if (isEditing) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nom de la recette") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = image,
                    onValueChange = { image = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Image (URL)") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = cookingTime,
                    onValueChange = { cookingTime = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Temps de cuisson (minutes)") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Description (facultatif)") },
                    minLines = 4,
                    placeholder = {
                        Text("Décrivez la recette en détail : étapes, astuces, accompagnements...")
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Ingrédients",
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
                            label = { Text("Quantité") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = ingredient.name,
                            onValueChange = { value ->
                                ingredients[index] = ingredient.copy(name = value)
                            },
                            modifier = Modifier.weight(1.2f),
                            label = { Text("Ingrédient") },
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
                                contentDescription = "Supprimer l'ingrédient"
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
                    Text("Ajouter un ingrédient")
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Étapes de préparation",
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
                            text = "${index + 1}.",
                            style = MaterialTheme.typography.titleMedium
                        )
                        OutlinedTextField(
                            value = step,
                            onValueChange = { value -> steps[index] = value },
                            modifier = Modifier.weight(1f),
                            label = { Text("Instruction") },
                            placeholder = { Text("Ex. Bouillir l'eau à 45 degrés") },
                            maxLines = 3
                        )
                        IconButton(
                            onClick = { steps.removeAt(index) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Supprimer l'étape"
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
                    Text("Ajouter une étape de préparation")
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        resetForm()
                        isEditing = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Annuler les modifications")
                }
            } else {
                Text(
                    text = recipe.title,
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "${recipe.cookingTime} minutes",
                    style = MaterialTheme.typography.bodyLarge
                )

                if (recipe.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Description",
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
                    text = "Ingrédients",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(8.dp))

                recipe.ingredientList.forEach { ingredient ->
                    Text(
                        text = "${ingredient.quantity} - ${ingredient.name}",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (persistedSteps.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Préparation",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    persistedSteps.forEachIndexed { index, step ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Text(
                                text = "${index + 1}.",
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
                    onClick = { navController.navigate("stepByStepScreen/${recipe.id}") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Checklist,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Suivre la recette étape par étape")
                }
            }

            errorMessage?.let { message ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
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
                    "Confirmer les modifications"
                } else {
                    "Modifier la recette"
                }
            )
        }
    }
}
