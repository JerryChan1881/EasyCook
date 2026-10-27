package com.example.easycook

import com.example.easycook.data.DecodeResult
import com.example.easycook.data.ExportedIngredient
import com.example.easycook.data.ExportedRecipe
import com.example.easycook.data.ImportError
import com.example.easycook.data.ImportResult
import com.example.easycook.data.Ingredient
import com.example.easycook.data.IngredientEntity
import com.example.easycook.data.OWN_PROFILE_ID
import com.example.easycook.data.ProfileEntity
import com.example.easycook.data.RecipeEntity
import com.example.easycook.data.RecipeImageFiles
import com.example.easycook.data.RecipeTransfer
import com.example.easycook.data.StepEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests du [StateViewModel].
 *
 * Deux precautions rendent ces tests deterministes :
 *
 * 1. Le ViewModel recoit un scope de test independant partageant le scheduler
 *    du test (et non `viewModelScope` : ce dernier utilise `Dispatchers.Main`
 *    et ses coroutines ne seraient pas drainees par `advanceUntilIdle`).
 *    Ce scope etant annule en fin de test, aucune coroutine ne subsiste.
 *
 * 2. `advanceUntilIdle()` est appele apres chaque action : les methodes du
 *    ViewModel lancent des coroutines, l'ecriture n'a donc pas eu lieu au
 *    moment de l'assertion tant que le temps virtuel n'a pas avance.
 *
 * Note : `recipes` est un `StateFlow` partage avec une valeur initiale vide.
 * Il se lit donc avec `first { ... }` : sans predicat, on recupererait la
 * valeur initiale plutot que la premiere emission reelle. `observeSteps`
 * renvoie un flux froid, une simple lecture `first()` suffit.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StateViewModelTest {

    private val testScopes = mutableListOf<CoroutineScope>()

    @After
    fun tearDown() {
        testScopes.forEach { it.cancel() }
        testScopes.clear()
    }

    private fun TestScope.newTestScope(): CoroutineScope =
        CoroutineScope(StandardTestDispatcher(testScheduler) + Job())
            .also { testScopes += it }

    /** DAO contenant une recette de base (id = 1) sur laquelle portent les tests. */
    private fun daoWithRecipe(
        recipe: RecipeEntity = RecipeEntity(id = 1, title = "Recette existante", image = "", cookingTime = 10),
        ingredients: List<IngredientEntity> = emptyList(),
        steps: List<StepEntity> = emptyList(),
        initialProfiles: List<ProfileEntity> = emptyList()
    ) = FakeRecipeDao(
        initialRecipes = listOf(recipe),
        initialIngredients = ingredients,
        initialSteps = steps,
        initialProfiles = initialProfiles
    )

    private fun step(
        id: Long,
        recipeId: Int = 1,
        position: Int = 0,
        instruction: String,
        isDone: Boolean = false
    ) = StepEntity(
        id = id,
        recipeId = recipeId,
        position = position,
        instruction = instruction,
        isDone = isDone
    )

    private fun recipe(title: String) = RecipeEntity(
        id = 1,
        title = title,
        image = "",
        cookingTime = 10
    )

    // --- Mapping des recettes ---

    @Test
    fun `recipes demarre avec une liste vide`() = runTest {
        val viewModel = StateViewModel(daoWithRecipe(), newTestScope())

        assertTrue(viewModel.recipes.value.isEmpty())
    }

    @Test
    fun `recipes expose les recettes de la base`() = runTest {
        val viewModel = StateViewModel(daoWithRecipe(), newTestScope())

        val recipes = viewModel.recipes.first { it.isNotEmpty() }

        assertEquals(1, recipes.size)
        assertEquals("Recette existante", recipes.single().title)
    }

    @Test
    fun `recipes associe les ingredients a leur recette`() = runTest {
        val dao = daoWithRecipe(
            ingredients = listOf(IngredientEntity(id = 1, recipeId = 1, quantity = "250 g", name = "Flour"))
        )
        val viewModel = StateViewModel(dao, newTestScope())

        val recipe = viewModel.recipes.first { it.isNotEmpty() }.single()

        assertEquals(1, recipe.ingredientList.size)
        assertEquals("Flour", recipe.ingredientList.single().name)
    }

    @Test
    fun `recipes transmet la description`() = runTest {
        val dao = daoWithRecipe(
            recipe = RecipeEntity(
                id = 1,
                title = "Curry",
                image = "",
                cookingTime = 45,
                description = "Un curry doux"
            )
        )
        val viewModel = StateViewModel(dao, newTestScope())

        val recipe = viewModel.recipes.first { it.isNotEmpty() }.single()

        assertEquals("Un curry doux", recipe.description)
    }

    @Test
    fun `aucune recette n est inseree automatiquement quand la base est vide`() = runTest {
        val dao = FakeRecipeDao()
        val viewModel = StateViewModel(dao, newTestScope())
        val receivedSizes = mutableListOf<Int>()
        val job = newTestScope().launch {
            viewModel.recipes.collect { receivedSizes += it.size }
        }

        advanceUntilIdle()
        job.cancel()

        assertTrue(receivedSizes.all { it == 0 })
    }

    // --- addRecipe ---

    @Test
    fun `addRecipe enregistre la recette et ses ingredients`() = runTest {
        val viewModel = StateViewModel(daoWithRecipe(), newTestScope())
        var completed = false

        viewModel.addRecipe(
            title = "Ratatouille",
            image = "photo.png",
            cookingTime = 50,
            description = "Une recette du sud",
            ingredients = listOf(Ingredient("2", "Aubergines"), Ingredient("3", "Courgettes")),
            steps = listOf("Couper les légumes", "Faire mijoter 30 minutes"),
            onComplete = { completed = true }
        )
        advanceUntilIdle()

        val recipes = viewModel.recipes.first { list -> list.any { it.title == "Ratatouille" } }
        val added = recipes.first { it.title == "Ratatouille" }

        assertEquals(2, added.ingredientList.size)
        assertEquals("Une recette du sud", added.description)
        assertEquals(50, added.cookingTime)
        assertEquals("photo.png", added.image)
        assertTrue(completed)
    }

    @Test
    fun `addRecipe enregistre les etapes dans l ordre de saisie`() = runTest {
        val viewModel = StateViewModel(daoWithRecipe(), newTestScope())

        viewModel.addRecipe(
            title = "Tartine",
            image = "",
            cookingTime = 10,
            description = "",
            ingredients = listOf(Ingredient("1", "Pain")),
            steps = listOf("Griller le pain", "Beurrer", "Servir"),
            onComplete = {}
        )
        advanceUntilIdle()

        val recipes = viewModel.recipes.first { list -> list.any { it.title == "Tartine" } }
        val added = recipes.first { it.title == "Tartine" }

        assertEquals(
            listOf("Griller le pain", "Beurrer", "Servir"),
            viewModel.observeSteps(recipeId = added.id).first().map { it.instruction }
        )
    }

    @Test
    fun `addRecipe accepte une recette sans etape`() = runTest {
        val viewModel = StateViewModel(daoWithRecipe(), newTestScope())

        viewModel.addRecipe(
            title = "Sans etape",
            image = "",
            cookingTime = 10,
            description = "",
            ingredients = listOf(Ingredient("1", "Pain")),
            steps = emptyList(),
            onComplete = {}
        )
        advanceUntilIdle()

        val recipes = viewModel.recipes.first { list -> list.any { it.title == "Sans etape" } }
        val added = recipes.first { it.title == "Sans etape" }

        assertTrue(viewModel.observeSteps(recipeId = added.id).first().isEmpty())
    }

    // --- updateRecipe ---

    @Test
    fun `updateRecipe modifie les champs de la recette`() = runTest {
        val viewModel = StateViewModel(daoWithRecipe(), newTestScope())
        var completed = false

        viewModel.updateRecipe(
            id = 1,
            title = "Nouveau titre",
            image = "img.png",
            cookingTime = 25,
            description = "Nouvelle description",
            ingredients = listOf(Ingredient("1", "Nouvel ingrédient")),
            steps = listOf("Première étape"),
            onComplete = { completed = true }
        )
        advanceUntilIdle()

        val recipes = viewModel.recipes.first { list -> list.any { it.title == "Nouveau titre" } }
        val updated = recipes.first { it.title == "Nouveau titre" }

        assertEquals("img.png", updated.image)
        assertEquals(25, updated.cookingTime)
        assertEquals("Nouvelle description", updated.description)
        assertEquals("Nouvel ingrédient", updated.ingredientList.single().name)
        assertTrue(completed)
    }

    @Test
    fun `updateRecipe remplace les ingredients et les etapes`() = runTest {
        val dao = daoWithRecipe(
            ingredients = listOf(IngredientEntity(id = 1, recipeId = 1, quantity = "1", name = "Ancien")),
            steps = listOf(step(id = 1, instruction = "Ancienne étape"))
        )
        val viewModel = StateViewModel(dao, newTestScope())

        viewModel.updateRecipe(
            id = 1,
            title = "Recette existante",
            image = "",
            cookingTime = 10,
            description = "",
            ingredients = listOf(Ingredient("2", "Nouveau")),
            steps = listOf("Nouvelle étape"),
            onComplete = {}
        )
        advanceUntilIdle()

        val recipes = viewModel.recipes.first { list ->
            list.singleOrNull()?.ingredientList?.singleOrNull()?.name == "Nouveau"
        }

        assertEquals(1, recipes.single().ingredientList.size)
        assertEquals(
            listOf("Nouvelle étape"),
            viewModel.observeSteps(recipeId = 1).first().map { it.instruction }
        )
    }

    @Test
    fun `updateRecipe peut vider la liste des etapes`() = runTest {
        val dao = daoWithRecipe(steps = listOf(step(id = 1, instruction = "Étape existante")))
        val viewModel = StateViewModel(dao, newTestScope())

        viewModel.updateRecipe(
            id = 1,
            title = "Recette existante",
            image = "",
            cookingTime = 10,
            description = "",
            ingredients = listOf(Ingredient("1", "Pain")),
            steps = emptyList(),
            onComplete = {}
        )
        advanceUntilIdle()

        assertTrue(viewModel.observeSteps(recipeId = 1).first().isEmpty())
    }

    // --- Favoris ---

    @Test
    fun `une nouvelle recette n est pas en favori`() = runTest {
        val viewModel = StateViewModel(daoWithRecipe(), newTestScope())

        val recipe = viewModel.recipes.first { it.isNotEmpty() }.single()

        assertFalse(recipe.isFavorite)
    }

    @Test
    fun `setFavorite met une recette en favori puis la retire`() = runTest {
        val viewModel = StateViewModel(daoWithRecipe(), newTestScope())

        viewModel.setFavorite(id = 1, isFavorite = true)
        advanceUntilIdle()
        assertTrue(viewModel.recipes.first { list -> list.any { it.isFavorite } }.single().isFavorite)

        viewModel.setFavorite(id = 1, isFavorite = false)
        advanceUntilIdle()
        assertFalse(viewModel.recipes.first { list -> list.none { it.isFavorite } }.single().isFavorite)
    }

    @Test
    fun `les favoris apparaissent en premier`() = runTest {
        val dao = FakeRecipeDao(
            initialRecipes = listOf(
                RecipeEntity(id = 1, title = "A", image = "", cookingTime = 10),
                RecipeEntity(id = 2, title = "B", image = "", cookingTime = 10),
                RecipeEntity(id = 3, title = "C", image = "", cookingTime = 10)
            )
        )
        val viewModel = StateViewModel(dao, newTestScope())

        viewModel.setFavorite(id = 3, isFavorite = true)
        advanceUntilIdle()

        val recipes = viewModel.recipes.first { list -> list.firstOrNull()?.isFavorite == true }

        assertEquals(listOf("C", "A", "B"), recipes.map { it.title })
    }

    @Test
    fun `modifier une recette conserve son statut de favori`() = runTest {
        val dao = daoWithRecipe(
            recipe = RecipeEntity(id = 1, title = "Recette existante", image = "", cookingTime = 10, isFavorite = true)
        )
        val viewModel = StateViewModel(dao, newTestScope())

        viewModel.updateRecipe(
            id = 1,
            title = "Titre modifie",
            image = "",
            cookingTime = 10,
            description = "",
            ingredients = listOf(Ingredient("1", "Pain")),
            steps = emptyList(),
            onComplete = {}
        )
        advanceUntilIdle()

        val recipe = viewModel.recipes.first { list -> list.any { it.title == "Titre modifie" } }.single()

        assertTrue(recipe.isFavorite)
    }

    // --- Images ---

    /** Images en memoire au lieu du disque ; enregistre aussi les suppressions demandees. */
    private class FakeImageFiles(
        val files: MutableMap<String, ByteArray> = mutableMapOf()
    ) : RecipeImageFiles {
        val deleted = mutableListOf<String>()
        var usedPathsAtCleanup: Set<String>? = null
        private var savedCount = 0

        override fun read(path: String): ByteArray? = files[path]

        override fun save(bytes: ByteArray): String? {
            // Comme le vrai stockage, refuse ce qui n'est pas une image (ici : un contenu vide)
            if (bytes.isEmpty()) return null
            val path = "/images/recue_${++savedCount}.jpg"
            files[path] = bytes
            return path
        }

        override fun delete(path: String) {
            deleted += path
        }

        override fun deleteUnused(usedPaths: Set<String>) {
            usedPathsAtCleanup = usedPaths
        }
    }

    private fun TestScope.viewModelWithImages(dao: FakeRecipeDao, cleaner: FakeImageFiles) =
        StateViewModel(
            recipeDao = dao,
            coroutineScope = newTestScope(),
            imageFiles = cleaner,
            ioDispatcher = StandardTestDispatcher(testScheduler)
        )

    @Test
    fun `au demarrage les images non utilisees sont nettoyees`() = runTest {
        val dao = FakeRecipeDao(
            initialRecipes = listOf(
                RecipeEntity(id = 1, title = "Avec image", image = "/images/a.jpg", cookingTime = 10),
                RecipeEntity(id = 2, title = "Sans image", image = "", cookingTime = 10)
            )
        )
        val cleaner = FakeImageFiles()

        viewModelWithImages(dao, cleaner)
        advanceUntilIdle()

        // Seules les images reellement utilisees sont conservees ; un champ vide n'en est pas une
        assertEquals(setOf("/images/a.jpg"), cleaner.usedPathsAtCleanup)
    }

    @Test
    fun `supprimer une recette supprime aussi son image`() = runTest {
        val dao = daoWithRecipe(
            recipe = RecipeEntity(id = 1, title = "Recette", image = "/images/a.jpg", cookingTime = 10)
        )
        val cleaner = FakeImageFiles()
        val viewModel = viewModelWithImages(dao, cleaner)

        viewModel.deleteRecipe(1)
        advanceUntilIdle()

        assertEquals(listOf("/images/a.jpg"), cleaner.deleted)
    }

    @Test
    fun `supprimer une recette sans image ne supprime aucun fichier`() = runTest {
        val cleaner = FakeImageFiles()
        val viewModel = viewModelWithImages(daoWithRecipe(), cleaner)

        viewModel.deleteRecipe(1)
        advanceUntilIdle()

        assertTrue(cleaner.deleted.isEmpty())
    }

    @Test
    fun `remplacer l image d une recette supprime l ancienne`() = runTest {
        val dao = daoWithRecipe(
            recipe = RecipeEntity(id = 1, title = "Recette", image = "/images/ancienne.jpg", cookingTime = 10)
        )
        val cleaner = FakeImageFiles()
        val viewModel = viewModelWithImages(dao, cleaner)

        viewModel.updateRecipe(
            id = 1,
            title = "Recette",
            image = "/images/nouvelle.jpg",
            cookingTime = 10,
            description = "",
            ingredients = listOf(Ingredient("1", "Pain")),
            steps = emptyList(),
            onComplete = {}
        )
        advanceUntilIdle()

        assertEquals(listOf("/images/ancienne.jpg"), cleaner.deleted)
        assertEquals("/images/nouvelle.jpg", dao.getImage(1))
    }

    @Test
    fun `modifier une recette sans changer son image la conserve`() = runTest {
        val dao = daoWithRecipe(
            recipe = RecipeEntity(id = 1, title = "Recette", image = "/images/a.jpg", cookingTime = 10)
        )
        val cleaner = FakeImageFiles()
        val viewModel = viewModelWithImages(dao, cleaner)

        viewModel.updateRecipe(
            id = 1,
            title = "Nouveau titre",
            image = "/images/a.jpg",
            cookingTime = 10,
            description = "",
            ingredients = listOf(Ingredient("1", "Pain")),
            steps = emptyList(),
            onComplete = {}
        )
        advanceUntilIdle()

        assertTrue(cleaner.deleted.isEmpty())
    }

    // --- Profils importes et transfert ---

    /** ViewModel dont les traitements en arriere-plan (export, import) suivent le temps virtuel du test. */
    private fun TestScope.transferViewModel(dao: FakeRecipeDao, clock: () -> Long = { 0L }) =
        StateViewModel(
            recipeDao = dao,
            coroutineScope = newTestScope(),
            ioDispatcher = StandardTestDispatcher(testScheduler),
            clock = clock
        )

    private val validExport =
        """{"format":"easycook-recipes","version":1,"exportedAt":0,"recipes":[
            |{"title":"Salade","cookingTime":0,"ingredients":[{"quantity":"1","name":"Laitue"},{"quantity":"","name":""}],
            |"steps":["Laver","  ","Servir"]},
            |{"title":"Soupe","cookingTime":20}]}""".trimMargin()

    @Test
    fun `une nouvelle recette appartient par defaut a mon profil`() = runTest {
        val viewModel = transferViewModel(daoWithRecipe())

        viewModel.addRecipe(
            title = "Tartine",
            image = "",
            cookingTime = 5,
            description = "",
            ingredients = listOf(Ingredient("1", "Pain")),
            steps = emptyList(),
            onComplete = {}
        )
        advanceUntilIdle()

        val added = viewModel.recipes.first { list -> list.any { it.title == "Tartine" } }.first { it.title == "Tartine" }
        assertEquals(OWN_PROFILE_ID, added.profileId)
    }

    @Test
    fun `une recette peut etre ajoutee dans un profil importe`() = runTest {
        val dao = daoWithRecipe(initialProfiles = listOf(ProfileEntity(id = 7, number = 1, importedAt = 0)))
        val viewModel = transferViewModel(dao)

        viewModel.addRecipe(
            title = "Tartine",
            image = "",
            cookingTime = 5,
            description = "",
            ingredients = listOf(Ingredient("1", "Pain")),
            steps = emptyList(),
            profileId = 7,
            onComplete = {}
        )
        advanceUntilIdle()

        val added = viewModel.recipes.first { list -> list.any { it.title == "Tartine" } }.first { it.title == "Tartine" }
        assertEquals(7, added.profileId)
    }

    @Test
    fun `importer des recettes cree le profil 1 avec ces recettes`() = runTest {
        val viewModel = transferViewModel(daoWithRecipe(), clock = { 123L })
        var result: ImportResult? = null

        viewModel.importRecipes(validExport) { result = it }
        advanceUntilIdle()

        val success = result as ImportResult.Success
        assertEquals(2, success.recipeCount)

        val profile = viewModel.profiles.first { it.isNotEmpty() }.single()
        assertEquals(success.profileId, profile.id)
        assertEquals(1, profile.number)

        val imported = viewModel.recipes.first { it.size == 3 }.filter { it.profileId == success.profileId }
        assertEquals(listOf("Salade", "Soupe"), imported.map { it.title })
        // Les lignes vides recues sont ecartees comme lors d'une saisie manuelle
        assertEquals(listOf(Ingredient("1", "Laitue")), imported.first().ingredientList)
        assertEquals(
            listOf("Laver", "Servir"),
            viewModel.observeSteps(imported.first().id).first().map { it.instruction }
        )
    }

    @Test
    fun `les recettes importees ne touchent pas a mon profil`() = runTest {
        val viewModel = transferViewModel(daoWithRecipe())

        viewModel.importRecipes(validExport) { }
        advanceUntilIdle()

        val own = viewModel.recipes.first { it.size == 3 }.filter { it.profileId == OWN_PROFILE_ID }
        assertEquals(listOf("Recette existante"), own.map { it.title })
    }

    @Test
    fun `deux imports creent deux profils distincts`() = runTest {
        val viewModel = transferViewModel(daoWithRecipe())

        viewModel.importRecipes(validExport) { }
        advanceUntilIdle()
        viewModel.importRecipes(validExport) { }
        advanceUntilIdle()

        assertEquals(listOf(1, 2), viewModel.profiles.first { it.size == 2 }.map { it.number })
    }

    @Test
    fun `un fichier invalide est refuse sans creer de profil`() = runTest {
        val dao = daoWithRecipe()
        val viewModel = transferViewModel(dao)
        var result: ImportResult? = null

        viewModel.importRecipes("pas du json") { result = it }
        advanceUntilIdle()

        assertEquals(ImportResult.Failure(ImportError.INVALID_FILE), result)
        assertTrue(dao.getProfileNumbers().isEmpty())
    }

    @Test
    fun `l export de mon profil contient recettes ingredients et etapes`() = runTest {
        val dao = daoWithRecipe(
            recipe = RecipeEntity(id = 1, title = "Pizza", image = "/images/pizza.jpg", cookingTime = 40, description = "Napolitaine"),
            ingredients = listOf(IngredientEntity(id = 1, recipeId = 1, quantity = "250 g", name = "Farine")),
            steps = listOf(step(id = 1, position = 0, instruction = "Petrir"), step(id = 2, position = 1, instruction = "Cuire"))
        )
        val viewModel = transferViewModel(dao, clock = { 42L })
        var json: String? = null

        viewModel.exportRecipes { json = it }
        advanceUntilIdle()

        val export = (RecipeTransfer.decode(json!!) as DecodeResult.Success).export
        assertEquals(42L, export.exportedAt)
        assertEquals(
            ExportedRecipe(
                title = "Pizza",
                cookingTime = 40,
                description = "Napolitaine",
                ingredients = listOf(ExportedIngredient("250 g", "Farine")),
                steps = listOf("Petrir", "Cuire")
            ),
            export.recipes.single()
        )
        // Le chemin local de l'image n'a aucun sens sur un autre telephone
        assertTrue(!json!!.contains("pizza.jpg"))
    }

    @Test
    fun `l export de mon profil n inclut pas les recettes importees`() = runTest {
        val viewModel = transferViewModel(daoWithRecipe())
        viewModel.importRecipes(validExport) { }
        advanceUntilIdle()
        var json: String? = null

        viewModel.exportRecipes { json = it }
        advanceUntilIdle()

        val export = (RecipeTransfer.decode(json!!) as DecodeResult.Success).export
        assertEquals(listOf("Recette existante"), export.recipes.map { it.title })
    }

    @Test
    fun `l export peut se limiter aux recettes choisies`() = runTest {
        val dao = FakeRecipeDao(
            initialRecipes = listOf(
                RecipeEntity(id = 1, title = "A", image = "", cookingTime = 10),
                RecipeEntity(id = 2, title = "B", image = "", cookingTime = 10),
                RecipeEntity(id = 3, title = "C", image = "", cookingTime = 10)
            )
        )
        val viewModel = transferViewModel(dao)
        var json: String? = null

        viewModel.exportRecipes(recipeIds = setOf(1, 3)) { json = it }
        advanceUntilIdle()

        val export = (RecipeTransfer.decode(json!!) as DecodeResult.Success).export
        assertEquals(listOf("A", "C"), export.recipes.map { it.title })
    }

    @Test
    fun `la photo d une recette voyage avec elle d un telephone a l autre`() = runTest {
        val photo = byteArrayOf(1, 2, 3, 4, 5)
        val senderImages = FakeImageFiles(mutableMapOf("/images/pizza.jpg" to photo))
        val sender = StateViewModel(
            recipeDao = daoWithRecipe(
                recipe = RecipeEntity(id = 1, title = "Pizza", image = "/images/pizza.jpg", cookingTime = 40)
            ),
            coroutineScope = newTestScope(),
            imageFiles = senderImages,
            ioDispatcher = StandardTestDispatcher(testScheduler)
        )
        var json: String? = null
        sender.exportRecipes { json = it }
        advanceUntilIdle()

        val receiverDao = FakeRecipeDao()
        val receiverImages = FakeImageFiles()
        val receiver = viewModelWithImages(receiverDao, receiverImages)
        var result: ImportResult? = null
        receiver.importRecipes(json!!) { result = it }
        advanceUntilIdle()

        val profileId = (result as ImportResult.Success).profileId
        val received = receiver.recipes.first { it.isNotEmpty() }.single { it.profileId == profileId }
        assertTrue(received.image.isNotBlank())
        assertTrue(photo.contentEquals(receiverImages.files[received.image]))
    }

    @Test
    fun `une photo illisible n empeche pas l import de la recette`() = runTest {
        val json = """{"format":"easycook-recipes","version":1,"exportedAt":0,"recipes":[
            |{"title":"Base64 casse","image":"%%% pas du base64 %%%"},
            |{"title":"Image vide","image":""}]}""".trimMargin()
        val images = FakeImageFiles()
        val viewModel = viewModelWithImages(FakeRecipeDao(), images)
        var result: ImportResult? = null

        viewModel.importRecipes(json) { result = it }
        advanceUntilIdle()

        assertEquals(2, (result as ImportResult.Success).recipeCount)
        val imported = viewModel.recipes.first { it.size == 2 }
        assertTrue(imported.all { it.image.isBlank() })
        assertTrue(images.files.isEmpty())
    }

    @Test
    fun `supprimer un profil importe supprime ses recettes et leurs images`() = runTest {
        val dao = FakeRecipeDao(
            initialRecipes = listOf(
                RecipeEntity(id = 1, title = "A moi", image = "/images/moi.jpg", cookingTime = 10),
                RecipeEntity(id = 2, title = "Importee", image = "/images/importee.jpg", cookingTime = 10, profileId = 5)
            ),
            initialSteps = listOf(step(id = 1, recipeId = 2, instruction = "Etape importee")),
            initialProfiles = listOf(ProfileEntity(id = 5, number = 1, importedAt = 0))
        )
        val cleaner = FakeImageFiles()
        val viewModel = viewModelWithImages(dao, cleaner)

        viewModel.deleteProfile(5)
        advanceUntilIdle()

        assertEquals(listOf("A moi"), viewModel.recipes.first { it.size == 1 }.map { it.title })
        assertTrue(viewModel.profiles.first().isEmpty())
        assertTrue(viewModel.observeSteps(2).first().isEmpty())
        assertEquals(listOf("/images/importee.jpg"), cleaner.deleted)
    }

    @Test
    fun `mon profil ne peut pas etre supprime`() = runTest {
        val viewModel = transferViewModel(daoWithRecipe())

        viewModel.deleteProfile(OWN_PROFILE_ID)
        advanceUntilIdle()

        assertEquals(1, viewModel.recipes.first { it.isNotEmpty() }.size)
    }

    // --- deleteRecipe ---

    @Test
    fun `deleteRecipe supprime la recette`() = runTest {
        val dao = FakeRecipeDao(
            initialRecipes = listOf(
                RecipeEntity(id = 1, title = "A supprimer", image = "", cookingTime = 10),
                RecipeEntity(id = 2, title = "A garder", image = "", cookingTime = 20)
            )
        )
        val viewModel = StateViewModel(dao, newTestScope())

        viewModel.deleteRecipe(1)
        advanceUntilIdle()

        val recipes = viewModel.recipes.first { it.size == 1 }

        assertEquals("A garder", recipes.single().title)
    }

    @Test
    fun `deleteRecipe supprime aussi les etapes`() = runTest {
        val dao = daoWithRecipe(steps = listOf(step(id = 1, instruction = "Une étape")))
        val viewModel = StateViewModel(dao, newTestScope())

        viewModel.deleteRecipe(1)
        advanceUntilIdle()

        assertTrue(viewModel.observeSteps(recipeId = 1).first().isEmpty())
    }

    // --- Gestion des etapes ---

    @Test
    fun `addStep ajoute une etape en fin de liste`() = runTest {
        val dao = daoWithRecipe(steps = listOf(step(id = 1, instruction = "Première")))
        val viewModel = StateViewModel(dao, newTestScope())
        var insertedId = 0L

        viewModel.addStep(recipeId = 1, instruction = "Deuxième") { insertedId = it }
        advanceUntilIdle()

        val steps = viewModel.observeSteps(recipeId = 1).first()

        assertEquals(listOf("Première", "Deuxième"), steps.map { it.instruction })
        assertTrue(insertedId > 0)
    }

    @Test
    fun `addStep renvoie un identifiant different a chaque appel`() = runTest {
        val viewModel = StateViewModel(daoWithRecipe(), newTestScope())
        val ids = mutableListOf<Long>()

        viewModel.addStep(recipeId = 1, instruction = "A") { ids += it }
        advanceUntilIdle()
        viewModel.addStep(recipeId = 1, instruction = "B") { ids += it }
        advanceUntilIdle()

        assertEquals(2, ids.size)
        assertTrue("Les identifiants doivent etre croissants", ids[1] > ids[0])
    }

    @Test
    fun `les etapes sont triees par position`() = runTest {
        val dao = daoWithRecipe(
            steps = listOf(
                step(id = 1, position = 2, instruction = "Troisième"),
                step(id = 2, position = 0, instruction = "Première"),
                step(id = 3, position = 1, instruction = "Deuxième")
            )
        )
        val viewModel = StateViewModel(dao, newTestScope())

        val steps = viewModel.observeSteps(recipeId = 1).first()

        assertEquals(listOf("Première", "Deuxième", "Troisième"), steps.map { it.instruction })
    }

    @Test
    fun `les etapes sont propres a chaque recette`() = runTest {
        val dao = daoWithRecipe(
            steps = listOf(
                step(id = 1, recipeId = 1, instruction = "Étape de la recette 1"),
                step(id = 2, recipeId = 2, instruction = "Étape de la recette 2")
            )
        )
        val viewModel = StateViewModel(dao, newTestScope())

        assertEquals("Étape de la recette 1", viewModel.observeSteps(recipeId = 1).first().single().instruction)
        assertEquals("Étape de la recette 2", viewModel.observeSteps(recipeId = 2).first().single().instruction)
    }

    @Test
    fun `addStep positionne la nouvelle etape apres la derniere`() = runTest {
        val dao = daoWithRecipe(
            steps = listOf(
                step(id = 1, position = 0, instruction = "A"),
                step(id = 2, position = 1, instruction = "B")
            )
        )
        val viewModel = StateViewModel(dao, newTestScope())

        viewModel.addStep(recipeId = 1, instruction = "C") { }
        advanceUntilIdle()

        assertEquals(
            listOf("A", "B", "C"),
            viewModel.observeSteps(recipeId = 1).first().map { it.instruction }
        )
    }

    @Test
    fun `updateStepInstruction modifie le texte de l etape`() = runTest {
        val dao = daoWithRecipe(steps = listOf(step(id = 1, instruction = "Ancienne")))
        val viewModel = StateViewModel(dao, newTestScope())

        viewModel.updateStepInstruction(stepId = 1, instruction = "Nouvelle instruction")
        advanceUntilIdle()

        val steps = viewModel.observeSteps(recipeId = 1).first()

        assertEquals("Nouvelle instruction", steps.single().instruction)
    }

    @Test
    fun `updateStepInstruction ne touche pas aux autres etapes`() = runTest {
        val dao = daoWithRecipe(
            steps = listOf(
                step(id = 1, position = 0, instruction = "A"),
                step(id = 2, position = 1, instruction = "B")
            )
        )
        val viewModel = StateViewModel(dao, newTestScope())

        viewModel.updateStepInstruction(stepId = 2, instruction = "B modifié")
        advanceUntilIdle()

        assertEquals(
            listOf("A", "B modifié"),
            viewModel.observeSteps(recipeId = 1).first().map { it.instruction }
        )
    }

    @Test
    fun `toggleStep coche une etape`() = runTest {
        val dao = daoWithRecipe(steps = listOf(step(id = 1, instruction = "Étape")))
        val viewModel = StateViewModel(dao, newTestScope())

        viewModel.toggleStep(stepId = 1, isDone = true)
        advanceUntilIdle()

        assertTrue(viewModel.observeSteps(recipeId = 1).first().single().isDone)
    }

    @Test
    fun `toggleStep decoche une etape`() = runTest {
        val dao = daoWithRecipe(steps = listOf(step(id = 1, instruction = "Étape", isDone = true)))
        val viewModel = StateViewModel(dao, newTestScope())

        viewModel.toggleStep(stepId = 1, isDone = false)
        advanceUntilIdle()

        assertFalse(viewModel.observeSteps(recipeId = 1).first().single().isDone)
    }

    @Test
    fun `resetSteps decoche toutes les etapes de la recette`() = runTest {
        val dao = daoWithRecipe(
            steps = listOf(
                step(id = 1, recipeId = 1, position = 0, instruction = "A", isDone = true),
                step(id = 2, recipeId = 1, position = 1, instruction = "B", isDone = true),
                step(id = 3, recipeId = 2, position = 0, instruction = "C", isDone = true)
            )
        )
        val viewModel = StateViewModel(dao, newTestScope())

        viewModel.resetSteps(recipeId = 1)
        advanceUntilIdle()

        assertTrue(viewModel.observeSteps(recipeId = 1).first().none { it.isDone })
        // La recette 2 ne doit pas etre affectee par le reset de la recette 1.
        assertTrue(viewModel.observeSteps(recipeId = 2).first().single().isDone)
    }

    @Test
    fun `deleteStep supprime une seule etape`() = runTest {
        val dao = daoWithRecipe(
            steps = listOf(
                step(id = 1, position = 0, instruction = "A"),
                step(id = 2, position = 1, instruction = "B")
            )
        )
        val viewModel = StateViewModel(dao, newTestScope())

        viewModel.deleteStep(stepId = 1)
        advanceUntilIdle()

        val steps = viewModel.observeSteps(recipeId = 1).first()

        assertEquals(1, steps.size)
        assertEquals("B", steps.single().instruction)
    }

    // --- Emissions des flux ---

    @Test
    fun `le flux des etapes emet a chaque ajout`() = runTest {
        val viewModel = StateViewModel(daoWithRecipe(), newTestScope())
        val receivedSizes = mutableListOf<Int>()
        val job = newTestScope().launch {
            viewModel.observeSteps(recipeId = 1).collect { receivedSizes += it.size }
        }
        advanceUntilIdle()

        viewModel.addStep(recipeId = 1, instruction = "A") { }
        advanceUntilIdle()
        viewModel.addStep(recipeId = 1, instruction = "B") { }
        advanceUntilIdle()
        job.cancel()

        assertEquals(listOf(0, 1, 2), receivedSizes)
    }

    @Test
    fun `le flux des recettes emet apres une suppression`() = runTest {
        val dao = FakeRecipeDao(
            initialRecipes = listOf(
                RecipeEntity(id = 1, title = "A", image = "", cookingTime = 10),
                RecipeEntity(id = 2, title = "B", image = "", cookingTime = 10)
            )
        )
        val viewModel = StateViewModel(dao, newTestScope())
        val receivedTitles = mutableListOf<List<String>>()
        val job = newTestScope().launch {
            viewModel.recipes.collect { list -> receivedTitles += list.map { it.title } }
        }
        advanceUntilIdle()

        viewModel.deleteRecipe(1)
        advanceUntilIdle()
        job.cancel()

        assertEquals(listOf("B"), receivedTitles.last())
        assertTrue(
            "Le flux doit avoir emis la liste complete avant la suppression",
            receivedTitles.contains(listOf("A", "B"))
        )
    }

    @Test
    fun `les etapes restent disponibles apres plusieurs collectes`() = runTest {
        val viewModel = StateViewModel(daoWithRecipe(), newTestScope())

        assertTrue(viewModel.observeSteps(recipeId = 1).first().isEmpty())

        viewModel.addStep(recipeId = 1, instruction = "Ajoutée") { }
        advanceUntilIdle()

        assertEquals(
            listOf("Ajoutée"),
            viewModel.observeSteps(recipeId = 1).first().map { it.instruction }
        )
    }

    @Test
    fun `cinq etapes consecutives conservent leur ordre`() = runTest {
        val viewModel = StateViewModel(daoWithRecipe(), newTestScope())

        repeat(5) { index ->
            viewModel.addStep(recipeId = 1, instruction = "Étape ${index + 1}") { }
        }
        advanceUntilIdle()

        assertEquals(
            (1..5).map { "Étape $it" },
            viewModel.observeSteps(recipeId = 1).first().map { it.instruction }
        )
    }
}
