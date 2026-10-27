package com.example.easycook

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.easycook.data.DecodeResult
import com.example.easycook.data.ExportedIngredient
import com.example.easycook.data.ExportedRecipe
import com.example.easycook.data.Ingredient
import com.example.easycook.data.ImportResult
import com.example.easycook.data.OWN_PROFILE_ID
import com.example.easycook.data.Profile
import com.example.easycook.data.Recipe
import com.example.easycook.data.RecipeDao
import com.example.easycook.data.RecipeDatabase
import com.example.easycook.data.RecipeExport
import com.example.easycook.data.RecipeEntity
import com.example.easycook.data.RecipeImageFiles
import com.example.easycook.data.RecipeStep
import com.example.easycook.data.RecipeTransfer
import com.example.easycook.data.StepEntity
import com.example.easycook.ui.image.RecipeImageStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.io.encoding.Base64

class StateViewModel(
    private val recipeDao: RecipeDao,
    coroutineScope: CoroutineScope? = null,
    private val imageFiles: RecipeImageFiles? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val clock: () -> Long = System::currentTimeMillis
) : ViewModel() {

    /**
     * Scope utilisé pour toutes les écritures asynchrones.
     * Injecté uniquement par les tests (via `backgroundScope`) ; en production
     * il s'agit du `viewModelScope` lié au cycle de vie de l'écran.
     */
    private val scope: CoroutineScope = coroutineScope ?: viewModelScope

    val recipes: StateFlow<List<Recipe>> = recipeDao.observeRecipes()
        .map { recipesWithIngredients ->
            recipesWithIngredients.map { recipeWithIngredients ->
                Recipe(
                    id = recipeWithIngredients.recipe.id,
                    title = recipeWithIngredients.recipe.title,
                    image = recipeWithIngredients.recipe.image,
                    cookingTime = recipeWithIngredients.recipe.cookingTime,
                    description = recipeWithIngredients.recipe.description,
                    isFavorite = recipeWithIngredients.recipe.isFavorite,
                    profileId = recipeWithIngredients.recipe.profileId,
                    ingredientList = recipeWithIngredients.ingredients.map { ingredient ->
                        Ingredient(
                            quantity = ingredient.quantity,
                            name = ingredient.name
                        )
                    }
                )
            }
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    /** Profils importés, dans l'ordre de leurs numéros (« Mon profil » n'en fait pas partie). */
    val profiles: StateFlow<List<Profile>> = recipeDao.observeProfiles()
        .map { profiles -> profiles.map { Profile(id = it.id, number = it.number) } }
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    init {
        // Supprime les images laissées par des formulaires abandonnés ou des images remplacées
        imageFiles?.let { cleaner ->
            scope.launch {
                val usedImages = recipeDao.getAllImages().filter { it.isNotBlank() }.toSet()
                withContext(ioDispatcher) { cleaner.deleteUnused(usedImages) }
            }
        }
    }

    fun addRecipe(
        title: String,
        image: String,
        cookingTime: Int,
        description: String,
        ingredients: List<Ingredient>,
        steps: List<String>,
        profileId: Int = OWN_PROFILE_ID,
        onComplete: () -> Unit
    ) {
        scope.launch {
            recipeDao.insertRecipeWithIngredients(
                recipe = RecipeEntity(
                    title = title,
                    image = image,
                    cookingTime = cookingTime,
                    description = description,
                    profileId = profileId
                ),
                ingredients = ingredients,
                steps = steps
            )
            onComplete()
        }
    }

    fun updateRecipe(
        id: Int,
        title: String,
        image: String,
        cookingTime: Int,
        description: String,
        ingredients: List<Ingredient>,
        steps: List<String>,
        onComplete: () -> Unit
    ) {
        scope.launch {
            val previousImage = recipeDao.getImage(id)
            recipeDao.updateRecipeWithIngredients(
                id = id,
                title = title,
                image = image,
                cookingTime = cookingTime,
                description = description,
                ingredients = ingredients,
                steps = steps
            )
            if (previousImage != image) {
                deleteImage(previousImage)
            }
            onComplete()
        }
    }

    fun setFavorite(id: Int, isFavorite: Boolean) {
        scope.launch {
            recipeDao.setFavorite(id, isFavorite)
        }
    }

    fun deleteRecipe(id: Int) {
        scope.launch {
            val image = recipeDao.getImage(id)
            recipeDao.deleteRecipeById(id)
            deleteImage(image)
        }
    }

    // --- Transfert entre téléphones ---

    /**
     * Construit le JSON des recettes d'un profil (par défaut « Mon profil »), photos comprises.
     * [recipeIds] limite l'export aux recettes choisies ; null exporte tout le profil.
     */
    fun exportRecipes(
        profileId: Int = OWN_PROFILE_ID,
        recipeIds: Set<Int>? = null,
        onReady: (String) -> Unit
    ) {
        scope.launch {
            val json = withContext(ioDispatcher) {
                val stepsByRecipe = recipeDao.getAllSteps().groupBy { it.recipeId }
                val recipes = recipeDao.getRecipesOfProfile(profileId)
                    .filter { recipeIds == null || it.recipe.id in recipeIds }
                    .map { recipeWithIngredients ->
                        val recipe = recipeWithIngredients.recipe
                        ExportedRecipe(
                            title = recipe.title,
                            cookingTime = recipe.cookingTime,
                            description = recipe.description,
                            ingredients = recipeWithIngredients.ingredients.map {
                                ExportedIngredient(quantity = it.quantity, name = it.name)
                            },
                            steps = stepsByRecipe[recipe.id].orEmpty().map { it.instruction },
                            image = recipe.image
                                .takeIf { it.isNotBlank() }
                                ?.let { imageFiles?.read(it) }
                                ?.let { Base64.encode(it) }
                        )
                    }
                RecipeTransfer.encode(RecipeExport(exportedAt = clock(), recipes = recipes))
            }
            onReady(json)
        }
    }

    /** Importe un JSON reçu dans un nouveau profil (« Profil 1 », « Profil 2 »…), photos comprises. */
    fun importRecipes(json: String, onResult: (ImportResult) -> Unit) {
        scope.launch {
            val result = when (val decoded = withContext(ioDispatcher) { RecipeTransfer.decode(json) }) {
                is DecodeResult.Failure -> ImportResult.Failure(decoded.error)
                is DecodeResult.Success -> {
                    val export = decoded.export
                    // Une photo illisible est simplement ignorée : la recette arrive sans photo
                    val imagePaths = withContext(ioDispatcher) {
                        export.recipes.map { recipe ->
                            recipe.image
                                ?.let { runCatching { Base64.decode(it) }.getOrNull() }
                                ?.let { imageFiles?.save(it) }
                                .orEmpty()
                        }
                    }
                    ImportResult.Success(
                        profileId = recipeDao.importProfile(export, importedAt = clock(), imagePaths = imagePaths),
                        recipeCount = export.recipes.size
                    )
                }
            }
            onResult(result)
        }
    }

    /** Supprime un profil importé, ses recettes et leurs images. « Mon profil » ne peut pas l'être. */
    fun deleteProfile(profileId: Int) {
        if (profileId == OWN_PROFILE_ID) return

        scope.launch {
            val images = recipeDao.getImagesOfProfile(profileId)
            recipeDao.deleteProfileWithRecipes(profileId)
            images.forEach { deleteImage(it) }
        }
    }

    private suspend fun deleteImage(path: String?) {
        val cleaner = imageFiles ?: return
        if (!path.isNullOrBlank()) {
            withContext(ioDispatcher) { cleaner.delete(path) }
        }
    }

    fun observeSteps(recipeId: Int): Flow<List<RecipeStep>> =
        recipeDao.observeSteps(recipeId)
            .map { steps ->
                steps.map { step ->
                    RecipeStep(
                        id = step.id,
                        instruction = step.instruction,
                        isDone = step.isDone
                    )
                }
            }

    fun addStep(recipeId: Int, instruction: String, onInserted: (Long) -> Unit) {
        scope.launch {
            val newStepId = recipeDao.insertStep(
                StepEntity(
                    recipeId = recipeId,
                    position = recipeDao.nextStepPosition(recipeId),
                    instruction = instruction
                )
            )
            onInserted(newStepId)
        }
    }

    fun updateStepInstruction(stepId: Long, instruction: String) {
        scope.launch {
            recipeDao.updateStepInstruction(stepId, instruction)
        }
    }

    fun toggleStep(stepId: Long, isDone: Boolean) {
        scope.launch {
            recipeDao.setStepDone(stepId, isDone)
        }
    }

    fun deleteStep(stepId: Long) {
        scope.launch {
            recipeDao.deleteStepById(stepId)
        }
    }

    fun resetSteps(recipeId: Int) {
        scope.launch {
            recipeDao.resetSteps(recipeId)
        }
    }

    companion object {
        fun from(application: Application): StateViewModel = StateViewModel(
            recipeDao = RecipeDatabase.getInstance(application).recipeDao(),
            imageFiles = RecipeImageStore(application)
        )
    }
}
