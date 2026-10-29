package com.example.easycook.data

import com.example.easycook.FakeImageFiles
import com.example.easycook.FakeRecipeDao
import com.example.easycook.collectAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests du [RecipeRepository], avec un faux DAO en memoire.
 *
 * Les fonctions du repository sont suspendues : chaque appel est termine quand il rend la main,
 * on peut donc verifier le resultat juste apres. Les flux du faux DAO renvoient l'etat courant,
 * une simple lecture `first()` suffit.
 */
class RecipeRepositoryTest {

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
    ) = StepEntity(id = id, recipeId = recipeId, position = position, instruction = instruction, isDone = isDone)

    private fun content(
        title: String = "Recette",
        image: String = "",
        cookingTime: Int = 10,
        description: String = "",
        ingredients: List<Ingredient> = listOf(Ingredient("1", "Pain")),
        steps: List<String> = emptyList()
    ) = RecipeContent(title, image, cookingTime, description, ingredients, steps)

    /** Repository dont les traitements en arriere-plan suivent le temps virtuel du test. */
    private fun TestScope.repository(
        dao: FakeRecipeDao = daoWithRecipe(),
        images: FakeImageFiles? = null,
        clock: () -> Long = { 0L }
    ) = RecipeRepository(dao, images, StandardTestDispatcher(testScheduler), clock)

    private suspend fun RecipeRepository.recipe(title: String): Recipe =
        recipes.first().first { it.title == title }

    private suspend fun RecipeRepository.instructions(recipeId: Int): List<String> =
        observeSteps(recipeId).first().map { it.instruction }

    // --- Lecture des recettes ---

    @Test
    fun `recipes expose les recettes de la base avec leurs ingredients et leur description`() = runTest {
        val dao = daoWithRecipe(
            recipe = RecipeEntity(id = 1, title = "Curry", image = "", cookingTime = 45, description = "Un curry doux"),
            ingredients = listOf(IngredientEntity(id = 1, recipeId = 1, quantity = "250 g", name = "Riz"))
        )

        val recipe = repository(dao).recipes.first().single()

        assertEquals("Curry", recipe.title)
        assertEquals("Un curry doux", recipe.description)
        assertEquals(listOf(Ingredient("250 g", "Riz")), recipe.ingredientList)
    }

    @Test
    fun `aucune recette n est inseree automatiquement quand la base est vide`() = runTest {
        assertTrue(repository(FakeRecipeDao()).recipes.first().isEmpty())
    }

    @Test
    fun `observeRecipe suit une seule recette et renvoie null quand elle disparait`() = runTest {
        val repository = repository()
        val received = collectAll(repository.observeRecipe(1).map { it?.title })

        repository.updateRecipe(1, content(title = "Renommee"))
        repository.deleteRecipe(1)

        assertEquals("Recette existante", received.first())
        assertTrue(received.contains("Renommee"))
        assertNull(received.last())
    }

    // --- Ajout ---

    @Test
    fun `addRecipe enregistre la recette ses ingredients et ses etapes dans l ordre`() = runTest {
        val repository = repository()

        repository.addRecipe(
            content(
                title = "Ratatouille",
                image = "photo.jpg",
                cookingTime = 50,
                description = "Une recette du sud",
                ingredients = listOf(Ingredient("2", "Aubergines"), Ingredient("3", "Courgettes")),
                steps = listOf("Couper les légumes", "Faire mijoter 30 minutes")
            )
        )

        val added = repository.recipe("Ratatouille")
        assertEquals(2, added.ingredientList.size)
        assertEquals("Une recette du sud", added.description)
        assertEquals(50, added.cookingTime)
        assertEquals("photo.jpg", added.image)
        assertEquals(listOf("Couper les légumes", "Faire mijoter 30 minutes"), repository.instructions(added.id))
    }

    @Test
    fun `addRecipe accepte une recette sans etape`() = runTest {
        val repository = repository()

        repository.addRecipe(content(title = "Sans etape"))

        assertTrue(repository.instructions(repository.recipe("Sans etape").id).isEmpty())
    }

    @Test
    fun `une nouvelle recette appartient par defaut a mon profil, ou au profil demande`() = runTest {
        val repository = repository(daoWithRecipe(initialProfiles = listOf(ProfileEntity(id = 7, number = 1, importedAt = 0))))

        repository.addRecipe(content(title = "A moi"))
        repository.addRecipe(content(title = "Au profil 7"), profileId = 7)

        assertEquals(OWN_PROFILE_ID, repository.recipe("A moi").profileId)
        assertEquals(7, repository.recipe("Au profil 7").profileId)
    }

    // --- Modification ---

    @Test
    fun `updateRecipe remplace les champs les ingredients et les etapes`() = runTest {
        val dao = daoWithRecipe(
            ingredients = listOf(IngredientEntity(id = 1, recipeId = 1, quantity = "1", name = "Ancien")),
            steps = listOf(step(id = 1, instruction = "Ancienne étape"))
        )
        val repository = repository(dao)

        repository.updateRecipe(
            1,
            content(
                title = "Nouveau titre",
                image = "img.jpg",
                cookingTime = 25,
                description = "Nouvelle description",
                ingredients = listOf(Ingredient("2", "Nouveau")),
                steps = listOf("Nouvelle étape")
            )
        )

        val updated = repository.recipes.first().single()
        assertEquals("Nouveau titre", updated.title)
        assertEquals("img.jpg", updated.image)
        assertEquals(25, updated.cookingTime)
        assertEquals("Nouvelle description", updated.description)
        assertEquals(listOf(Ingredient("2", "Nouveau")), updated.ingredientList)
        assertEquals(listOf("Nouvelle étape"), repository.instructions(1))
    }

    @Test
    fun `updateRecipe peut vider la liste des etapes`() = runTest {
        val repository = repository(daoWithRecipe(steps = listOf(step(id = 1, instruction = "Étape existante"))))

        repository.updateRecipe(1, content(steps = emptyList()))

        assertTrue(repository.instructions(1).isEmpty())
    }

    // --- Favoris ---

    @Test
    fun `setFavorite met une recette en favori puis la retire`() = runTest {
        val repository = repository()
        assertFalse(repository.recipes.first().single().isFavorite)

        repository.setFavorite(id = 1, isFavorite = true)
        assertTrue(repository.recipes.first().single().isFavorite)

        repository.setFavorite(id = 1, isFavorite = false)
        assertFalse(repository.recipes.first().single().isFavorite)
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
        val repository = repository(dao)

        repository.setFavorite(id = 3, isFavorite = true)

        assertEquals(listOf("C", "A", "B"), repository.recipes.first().map { it.title })
    }

    @Test
    fun `modifier une recette conserve son statut de favori`() = runTest {
        val repository = repository(
            daoWithRecipe(recipe = RecipeEntity(id = 1, title = "Recette", image = "", cookingTime = 10, isFavorite = true))
        )

        repository.updateRecipe(1, content(title = "Titre modifie"))

        assertTrue(repository.recipe("Titre modifie").isFavorite)
    }

    // --- Suppression ---

    @Test
    fun `deleteRecipe supprime la recette et ses etapes`() = runTest {
        val dao = FakeRecipeDao(
            initialRecipes = listOf(
                RecipeEntity(id = 1, title = "A supprimer", image = "", cookingTime = 10),
                RecipeEntity(id = 2, title = "A garder", image = "", cookingTime = 20)
            ),
            initialSteps = listOf(step(id = 1, instruction = "Une étape"))
        )
        val repository = repository(dao)

        repository.deleteRecipe(1)

        assertEquals(listOf("A garder"), repository.recipes.first().map { it.title })
        assertTrue(repository.instructions(1).isEmpty())
    }

    // --- Photos ---

    @Test
    fun `deleteUnusedImages ne garde que les photos reellement utilisees`() = runTest {
        val dao = FakeRecipeDao(
            initialRecipes = listOf(
                RecipeEntity(id = 1, title = "Avec photo", image = "a.jpg", cookingTime = 10),
                RecipeEntity(id = 2, title = "Sans photo", image = "", cookingTime = 10)
            )
        )
        val images = FakeImageFiles()

        repository(dao, images).deleteUnusedImages()

        // Un champ vide n'est pas une photo
        assertEquals(setOf("a.jpg"), images.usedNamesAtCleanup)
    }

    @Test
    fun `supprimer une recette supprime aussi sa photo`() = runTest {
        val images = FakeImageFiles()
        val repository = repository(
            daoWithRecipe(recipe = RecipeEntity(id = 1, title = "Recette", image = "a.jpg", cookingTime = 10)),
            images
        )

        repository.deleteRecipe(1)

        assertEquals(listOf("a.jpg"), images.deleted)
    }

    @Test
    fun `supprimer une recette sans photo ne supprime aucun fichier`() = runTest {
        val images = FakeImageFiles()

        repository(images = images).deleteRecipe(1)

        assertTrue(images.deleted.isEmpty())
    }

    @Test
    fun `remplacer la photo d une recette supprime l ancienne`() = runTest {
        val dao = daoWithRecipe(recipe = RecipeEntity(id = 1, title = "Recette", image = "ancienne.jpg", cookingTime = 10))
        val images = FakeImageFiles()

        repository(dao, images).updateRecipe(1, content(image = "nouvelle.jpg"))

        assertEquals(listOf("ancienne.jpg"), images.deleted)
        assertEquals("nouvelle.jpg", dao.getImage(1))
    }

    @Test
    fun `modifier une recette sans changer sa photo la conserve`() = runTest {
        val dao = daoWithRecipe(recipe = RecipeEntity(id = 1, title = "Recette", image = "a.jpg", cookingTime = 10))
        val images = FakeImageFiles()

        repository(dao, images).updateRecipe(1, content(title = "Nouveau titre", image = "a.jpg"))

        assertTrue(images.deleted.isEmpty())
    }

    // --- Étapes ---

    @Test
    fun `addStep ajoute une etape en fin de liste et renvoie des identifiants croissants`() = runTest {
        val repository = repository(daoWithRecipe(steps = listOf(step(id = 1, position = 0, instruction = "Première"))))

        val first = repository.addStep(recipeId = 1, instruction = "Deuxième")
        val second = repository.addStep(recipeId = 1, instruction = "Troisième")

        assertEquals(listOf("Première", "Deuxième", "Troisième"), repository.instructions(1))
        assertTrue(second > first)
    }

    @Test
    fun `les etapes sont triees par position et propres a chaque recette`() = runTest {
        val dao = daoWithRecipe(
            steps = listOf(
                step(id = 1, position = 2, instruction = "Troisième"),
                step(id = 2, position = 0, instruction = "Première"),
                step(id = 3, position = 1, instruction = "Deuxième"),
                step(id = 4, recipeId = 2, instruction = "Autre recette")
            )
        )
        val repository = repository(dao)

        assertEquals(listOf("Première", "Deuxième", "Troisième"), repository.instructions(1))
        assertEquals(listOf("Autre recette"), repository.instructions(2))
    }

    @Test
    fun `updateStepInstruction ne modifie que l etape visee`() = runTest {
        val repository = repository(
            daoWithRecipe(steps = listOf(step(id = 1, position = 0, instruction = "A"), step(id = 2, position = 1, instruction = "B")))
        )

        repository.updateStepInstruction(stepId = 2, instruction = "B modifié")

        assertEquals(listOf("A", "B modifié"), repository.instructions(1))
    }

    @Test
    fun `setStepDone coche puis decoche une etape`() = runTest {
        val repository = repository(daoWithRecipe(steps = listOf(step(id = 1, instruction = "Étape"))))

        repository.setStepDone(stepId = 1, isDone = true)
        assertTrue(repository.observeSteps(1).first().single().isDone)

        repository.setStepDone(stepId = 1, isDone = false)
        assertFalse(repository.observeSteps(1).first().single().isDone)
    }

    @Test
    fun `resetSteps decoche toutes les etapes de la recette seulement`() = runTest {
        val dao = daoWithRecipe(
            steps = listOf(
                step(id = 1, recipeId = 1, position = 0, instruction = "A", isDone = true),
                step(id = 2, recipeId = 1, position = 1, instruction = "B", isDone = true),
                step(id = 3, recipeId = 2, position = 0, instruction = "C", isDone = true)
            )
        )
        val repository = repository(dao)

        repository.resetSteps(recipeId = 1)

        assertTrue(repository.observeSteps(1).first().none { it.isDone })
        assertTrue(repository.observeSteps(2).first().single().isDone)
    }

    @Test
    fun `deleteStep supprime une seule etape`() = runTest {
        val repository = repository(
            daoWithRecipe(steps = listOf(step(id = 1, position = 0, instruction = "A"), step(id = 2, position = 1, instruction = "B")))
        )

        repository.deleteStep(stepId = 1)

        assertEquals(listOf("B"), repository.instructions(1))
    }

    // --- Profils importés et transfert ---

    private val validExport =
        """{"format":"easycook-recipes","version":1,"exportedAt":0,"recipes":[
            |{"title":"Salade","cookingTime":0,"ingredients":[{"quantity":"1","name":"Laitue"},{"quantity":"","name":""}],
            |"steps":["Laver","  ","Servir"]},
            |{"title":"Soupe","cookingTime":20}]}""".trimMargin()

    @Test
    fun `importer des recettes cree le profil 1 avec ces recettes`() = runTest {
        val repository = repository(clock = { 123L })

        val result = repository.importRecipes(validExport) as ImportResult.Success

        assertEquals(2, result.recipeCount)
        val profile = repository.profiles.first().single()
        assertEquals(result.profileId, profile.id)
        assertEquals(1, profile.number)

        val imported = repository.recipes.first().filter { it.profileId == result.profileId }
        assertEquals(listOf("Salade", "Soupe"), imported.map { it.title })
        // Les lignes vides recues sont ecartees comme lors d'une saisie manuelle
        assertEquals(listOf(Ingredient("1", "Laitue")), imported.first().ingredientList)
        assertEquals(listOf("Laver", "Servir"), repository.instructions(imported.first().id))
    }

    @Test
    fun `les recettes importees ne touchent pas a mon profil`() = runTest {
        val repository = repository()

        repository.importRecipes(validExport)

        val own = repository.recipes.first().filter { it.profileId == OWN_PROFILE_ID }
        assertEquals(listOf("Recette existante"), own.map { it.title })
    }

    @Test
    fun `deux imports creent deux profils distincts`() = runTest {
        val repository = repository()

        repository.importRecipes(validExport)
        repository.importRecipes(validExport)

        assertEquals(listOf(1, 2), repository.profiles.first().map { it.number })
    }

    @Test
    fun `un fichier invalide est refuse sans creer de profil`() = runTest {
        val dao = daoWithRecipe()

        val result = repository(dao).importRecipes("pas du json")

        assertEquals(ImportResult.Failure(ImportError.INVALID_FILE), result)
        assertTrue(dao.getProfileNumbers().isEmpty())
    }

    @Test
    fun `l export de mon profil contient recettes ingredients et etapes`() = runTest {
        val dao = daoWithRecipe(
            recipe = RecipeEntity(id = 1, title = "Pizza", image = "pizza.jpg", cookingTime = 40, description = "Napolitaine"),
            ingredients = listOf(IngredientEntity(id = 1, recipeId = 1, quantity = "250 g", name = "Farine")),
            steps = listOf(step(id = 1, position = 0, instruction = "Petrir"), step(id = 2, position = 1, instruction = "Cuire"))
        )

        val json = repository(dao, clock = { 42L }).exportRecipes()

        val export = (RecipeTransfer.decode(json) as DecodeResult.Success).export
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
        // Le nom du fichier local de la photo n'a aucun sens sur un autre telephone
        assertFalse(json.contains("pizza.jpg"))
    }

    @Test
    fun `l export de mon profil n inclut pas les recettes importees`() = runTest {
        val repository = repository()
        repository.importRecipes(validExport)

        val export = (RecipeTransfer.decode(repository.exportRecipes()) as DecodeResult.Success).export

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

        val json = repository(dao).exportRecipes(recipeIds = setOf(1, 3))

        val export = (RecipeTransfer.decode(json) as DecodeResult.Success).export
        assertEquals(listOf("A", "C"), export.recipes.map { it.title })
    }

    @Test
    fun `la photo d une recette voyage avec elle d un telephone a l autre`() = runTest {
        val photo = byteArrayOf(1, 2, 3, 4, 5)
        val sender = repository(
            daoWithRecipe(recipe = RecipeEntity(id = 1, title = "Pizza", image = "pizza.jpg", cookingTime = 40)),
            FakeImageFiles(mutableMapOf("pizza.jpg" to photo))
        )
        val receiverImages = FakeImageFiles()
        val receiver = repository(FakeRecipeDao(), receiverImages)

        val result = receiver.importRecipes(sender.exportRecipes()) as ImportResult.Success

        val received = receiver.recipes.first().single { it.profileId == result.profileId }
        assertTrue(received.image.isNotBlank())
        assertTrue(photo.contentEquals(receiverImages.files[received.image]))
    }

    @Test
    fun `une photo illisible n empeche pas l import de la recette`() = runTest {
        val json = """{"format":"easycook-recipes","version":1,"exportedAt":0,"recipes":[
            |{"title":"Base64 casse","image":"%%% pas du base64 %%%"},
            |{"title":"Image vide","image":""}]}""".trimMargin()
        val images = FakeImageFiles()
        val repository = repository(FakeRecipeDao(), images)

        val result = repository.importRecipes(json) as ImportResult.Success

        assertEquals(2, result.recipeCount)
        assertTrue(repository.recipes.first().all { it.image.isBlank() })
        assertTrue(images.files.isEmpty())
    }

    @Test
    fun `supprimer un profil importe supprime ses recettes et leurs photos`() = runTest {
        val dao = FakeRecipeDao(
            initialRecipes = listOf(
                RecipeEntity(id = 1, title = "A moi", image = "moi.jpg", cookingTime = 10),
                RecipeEntity(id = 2, title = "Importee", image = "importee.jpg", cookingTime = 10, profileId = 5)
            ),
            initialSteps = listOf(step(id = 1, recipeId = 2, instruction = "Etape importee")),
            initialProfiles = listOf(ProfileEntity(id = 5, number = 1, importedAt = 0))
        )
        val images = FakeImageFiles()
        val repository = repository(dao, images)

        repository.deleteProfile(5)

        assertEquals(listOf("A moi"), repository.recipes.first().map { it.title })
        assertTrue(repository.profiles.first().isEmpty())
        assertTrue(repository.instructions(2).isEmpty())
        assertEquals(listOf("importee.jpg"), images.deleted)
    }

    @Test
    fun `mon profil ne peut pas etre supprime`() = runTest {
        val repository = repository()

        repository.deleteProfile(OWN_PROFILE_ID)

        assertEquals(1, repository.recipes.first().size)
        assertNull(repository.profiles.first().firstOrNull())
    }
}
