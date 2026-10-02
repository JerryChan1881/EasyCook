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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.easycook.StateViewModel
import com.example.easycook.data.Ingredient

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
    var errorMessage by remember { mutableStateOf<String?>(null) }
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
                    contentDescription = "Retour"
                )
            }
            Text(
                text = "Nouvelle recette",
                style = MaterialTheme.typography.headlineMedium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

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
            placeholder = { Text("Décrivez la recette en détail : étapes, astuces, accompagnements...") }
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

        Button(
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
        Button(
            onClick = { ingredients.add(IngredientForm()) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Ajouter une étape de préparation")
        }


        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
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
                        stateViewModel.addRecipe(
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
                            onComplete = { navController.popBackStack() }
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Enregistrer la recette")
        }

        errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
