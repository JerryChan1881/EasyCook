package com.example.easycook.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.io.encoding.Base64

/**
 * Point d'accès unique aux recettes pour tous les ViewModels.
 *
 * Il cache la base Room et les fichiers des photos : les écrans ne manipulent que des
 * modèles d'affichage ([Recipe], [RecipeStep], [Profile]). Sans dépendance Android,
 * il se teste sur la JVM avec un faux DAO.
 */
class RecipeRepository(
    private val dao: RecipeDao,
    private val imageFiles: RecipeImageFiles? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val clock: () -> Long = System::currentTimeMillis
) {

    /** Toutes les recettes, favoris en premier. */
    val recipes: Flow<List<Recipe>> = dao.observeRecipes()
        .map { recipes -> recipes.map { it.toRecipe() } }

    /** Profils importés, dans l'ordre de leurs numéros (« Mon profil » n'en fait pas partie). */
    val profiles: Flow<List<Profile>> = dao.observeProfiles()
        .map { profiles -> profiles.map { Profile(id = it.id, number = it.number) } }

    /** Une recette, ou null si elle n'existe pas (ou plus). */
    fun observeRecipe(id: Int): Flow<Recipe?> =
        dao.observeRecipe(id).map { it?.toRecipe() }

    fun observeSteps(recipeId: Int): Flow<List<RecipeStep>> =
        dao.observeSteps(recipeId).map { steps ->
            steps.map { RecipeStep(id = it.id, instruction = it.instruction, isDone = it.isDone) }
        }

    // --- Recettes ---

    suspend fun addRecipe(content: RecipeContent, profileId: Int = OWN_PROFILE_ID) = write {
        dao.insertRecipeWithIngredients(
            recipe = RecipeEntity(
                title = content.title,
                image = content.image,
                cookingTime = content.cookingTime,
                description = content.description,
                profileId = profileId
            ),
            ingredients = content.ingredients,
            steps = content.steps
        )
    }

    /** Remplace le contenu d'une recette ; l'ancienne photo est supprimée si elle a changé. */
    suspend fun updateRecipe(id: Int, content: RecipeContent) = write {
        val previousImage = dao.getImage(id)
        dao.updateRecipeWithIngredients(
            id = id,
            title = content.title,
            image = content.image,
            cookingTime = content.cookingTime,
            description = content.description,
            ingredients = content.ingredients,
            steps = content.steps
        )
        if (previousImage != content.image) {
            deleteImage(previousImage)
        }
    }

    suspend fun setFavorite(id: Int, isFavorite: Boolean) = write {
        dao.setFavorite(id, isFavorite)
    }

    suspend fun deleteRecipe(id: Int) = write {
        val image = dao.getImage(id)
        dao.deleteRecipeById(id)
        deleteImage(image)
    }

    // --- Étapes ---

    /** Ajoute une étape en fin de recette et renvoie son identifiant. */
    suspend fun addStep(recipeId: Int, instruction: String): Long =
        write {
            dao.insertStep(
                StepEntity(
                    recipeId = recipeId,
                    position = dao.nextStepPosition(recipeId),
                    instruction = instruction
                )
            )
        }

    suspend fun updateStepInstruction(stepId: Long, instruction: String) = write {
        dao.updateStepInstruction(stepId, instruction)
    }

    suspend fun setStepDone(stepId: Long, isDone: Boolean) = write {
        dao.setStepDone(stepId, isDone)
    }

    suspend fun deleteStep(stepId: Long) = write {
        dao.deleteStepById(stepId)
    }

    suspend fun resetSteps(recipeId: Int) = write {
        dao.resetSteps(recipeId)
    }

    // --- Transfert entre téléphones ---

    /**
     * JSON des recettes d'un profil (par défaut « Mon profil »), photos comprises.
     * [recipeIds] limite l'export aux recettes choisies ; null exporte tout le profil.
     */
    suspend fun exportRecipes(profileId: Int = OWN_PROFILE_ID, recipeIds: Set<Int>? = null): String =
        withContext(ioDispatcher) {
            val stepsByRecipe = dao.getAllSteps().groupBy { it.recipeId }
            val recipes = dao.getRecipesOfProfile(profileId)
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

    /** Importe un JSON reçu dans un nouveau profil (« Profil 1 », « Profil 2 »…), photos comprises. */
    suspend fun importRecipes(json: String): ImportResult {
        val export = when (val decoded = withContext(ioDispatcher) { RecipeTransfer.decode(json) }) {
            is DecodeResult.Failure -> return ImportResult.Failure(decoded.error)
            is DecodeResult.Success -> decoded.export
        }

        // Une photo illisible est simplement ignorée : la recette arrive sans photo
        val imageNames = withContext(ioDispatcher) {
            export.recipes.map { recipe ->
                recipe.image
                    ?.let { runCatching { Base64.decode(it) }.getOrNull() }
                    ?.let { imageFiles?.save(it) }
                    .orEmpty()
            }
        }
        return ImportResult.Success(
            profileId = write { dao.importProfile(export, importedAt = clock(), imageNames = imageNames) },
            recipeCount = export.recipes.size
        )
    }

    /** Supprime un profil importé, ses recettes et leurs photos. « Mon profil » ne peut pas l'être. */
    suspend fun deleteProfile(profileId: Int) {
        if (profileId == OWN_PROFILE_ID) return

        write {
            val images = dao.getImagesOfProfile(profileId)
            dao.deleteProfileWithRecipes(profileId)
            images.forEach { deleteImage(it) }
        }
    }

    // --- Photos ---

    /** Supprime les photos laissées par des formulaires abandonnés ou des photos remplacées. */
    suspend fun deleteUnusedImages() {
        val cleaner = imageFiles ?: return
        val usedImages = dao.getAllImages().filter { it.isNotBlank() }.toSet()
        withContext(ioDispatcher) { cleaner.deleteUnused(usedImages) }
    }

    /**
     * Les écritures sont lancées depuis le ViewModel d'un écran : si l'utilisateur quitte l'écran
     * juste après avoir validé, elles doivent aller jusqu'au bout au lieu d'être annulées avec lui.
     */
    private suspend fun <T> write(block: suspend () -> T): T = withContext(NonCancellable) { block() }

    private suspend fun deleteImage(name: String?) {
        val cleaner = imageFiles ?: return
        if (!name.isNullOrBlank()) {
            withContext(ioDispatcher) { cleaner.delete(name) }
        }
    }

    private fun RecipeWithIngredients.toRecipe() = Recipe(
        id = recipe.id,
        title = recipe.title,
        image = recipe.image,
        cookingTime = recipe.cookingTime,
        description = recipe.description,
        isFavorite = recipe.isFavorite,
        profileId = recipe.profileId,
        ingredientList = ingredients.map { Ingredient(quantity = it.quantity, name = it.name) }
    )
}
