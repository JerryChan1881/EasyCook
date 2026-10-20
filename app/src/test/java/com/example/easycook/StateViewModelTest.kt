package com.example.easycook

import com.example.easycook.data.Ingredient
import com.example.easycook.data.IngredientEntity
import com.example.easycook.data.RecipeEntity
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
        steps: List<StepEntity> = emptyList()
    ) = FakeRecipeDao(
        initialRecipes = listOf(recipe),
        initialIngredients = ingredients,
        initialSteps = steps
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
