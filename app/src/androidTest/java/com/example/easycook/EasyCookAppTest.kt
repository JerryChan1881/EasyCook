package com.example.easycook

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.easycook.data.Ingredient
import com.example.easycook.data.RecipeContent
import com.example.easycook.data.RecipeDatabase
import com.example.easycook.data.RecipeRepository
import com.example.easycook.transfer.TextDocuments
import com.example.easycook.ui.theme.EasyCookTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Parcours complets dans l'application, de l'accueil jusqu'au mode étape par étape.
 *
 * L'application tourne sur une base Room en mémoire, propre à chaque test : les recettes
 * de l'appareil ne sont ni lues ni modifiées.
 */
@RunWith(AndroidJUnit4::class)
class EasyCookAppTest {

    @get:Rule
    val compose = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val database = Room.inMemoryDatabaseBuilder(context, RecipeDatabase::class.java).build()
    private val repository = RecipeRepository(database.recipeDao())

    private object NoDocuments : TextDocuments {
        override fun read(uri: String): String? = null
        override fun write(uri: String, text: String) = false
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    private fun text(@StringRes id: Int, vararg args: Any): String = context.getString(id, *args)

    private fun plural(@PluralsRes id: Int, count: Int, vararg args: Any): String =
        context.resources.getQuantityString(id, count, *args)

    private fun launchApp(vararg recipes: RecipeContent) {
        runBlocking { recipes.forEach { repository.addRecipe(it) } }
        compose.setContent {
            EasyCookTheme {
                EasyCookApp(repository = repository, documents = NoDocuments)
            }
        }
    }

    /** La base répond sur ses propres threads : on attend que le texte apparaisse (ou disparaisse). */
    private fun waitForText(text: String, present: Boolean = true) {
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() == present
        }
    }

    private fun recipe(title: String, steps: List<String> = emptyList()) = RecipeContent(
        title = title,
        image = "",
        cookingTime = 20,
        description = "",
        ingredients = listOf(Ingredient("250 g", "farine")),
        steps = steps
    )

    /** Champ de saisie repéré par son libellé. */
    private fun field(label: String) = compose.onNode(hasSetTextAction() and hasText(label))

    // --- Accueil ---

    @Test
    fun accueilSansRecetteInviteAEnAjouterUne() {
        launchApp()

        waitForText(text(R.string.home_empty_title))
        compose.onNodeWithText(text(R.string.home_empty_hint)).assertIsDisplayed()
    }

    @Test
    fun ajouterUneRecetteLAfficheSurLAccueil() {
        launchApp()

        compose.onNodeWithContentDescription(text(R.string.home_add_recipe)).performClick()
        field(text(R.string.recipe_form_name)).performTextInput("Crêpes")
        field(text(R.string.recipe_form_ingredient)).performTextInput("250 g farine")
        compose.onNodeWithText(text(R.string.recipe_form_save)).performScrollTo().performClick()

        // Retour à l'accueil une fois la recette enregistrée (le formulaire contient aussi « Crêpes »)
        waitForText(text(R.string.add_recipe_title), present = false)
        waitForText("Crêpes")
        compose.onNodeWithText(text(R.string.home_subtitle)).assertIsDisplayed()
    }

    @Test
    fun enregistrerSansNomAfficheUneErreur() {
        launchApp()

        compose.onNodeWithContentDescription(text(R.string.home_add_recipe)).performClick()
        compose.onNodeWithText(text(R.string.recipe_form_save)).performScrollTo().performClick()

        compose.onNodeWithText(text(R.string.error_title_required)).assertIsDisplayed()
    }

    @Test
    fun laRechercheNeGardeQueLesRecettesCorrespondantes() {
        launchApp(recipe("Crêpes"), recipe("Pizza"))
        waitForText("Pizza")

        compose.onNodeWithContentDescription(text(R.string.home_search)).performClick()
        compose.onNode(hasSetTextAction()).performTextInput("piz")

        waitForText("Crêpes", present = false)
        compose.onNodeWithText("Pizza").assertIsDisplayed()
    }

    @Test
    fun leCoeurMetLaRecetteEnFavori() {
        launchApp(recipe("Crêpes"))
        waitForText("Crêpes")

        compose.onNodeWithContentDescription(text(R.string.recipe_card_add_favorite)).performClick()

        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithContentDescription(text(R.string.recipe_card_remove_favorite))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun supprimerUneRecetteDemandeConfirmation() {
        launchApp(recipe("Crêpes"))
        waitForText("Crêpes")

        compose.onNodeWithContentDescription(text(R.string.home_delete_recipe)).performClick()
        compose.onNodeWithText(text(R.string.home_delete_recipe_confirm, "Crêpes")).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.action_delete)).performClick()

        waitForText(text(R.string.home_empty_title))
    }

    // --- Détail et modification ---

    @Test
    fun leDetailAfficheIngredientsEtEtapes() {
        launchApp(recipe("Crêpes", steps = listOf("Mélanger", "Cuire")))
        waitForText("Crêpes")

        compose.onNodeWithText("Crêpes").performClick()

        waitForText("250 g farine")
        compose.onNodeWithText("Mélanger").assertIsDisplayed()
        compose.onNodeWithText("Cuire").assertIsDisplayed()
    }

    @Test
    fun modifierUneRecetteMetAJourLeDetailEtLAccueil() {
        launchApp(recipe("Crêpes"))
        waitForText("Crêpes")
        compose.onNodeWithText("Crêpes").performClick()
        waitForText("250 g farine")

        compose.onNodeWithContentDescription(text(R.string.recipe_detail_edit)).performClick()
        field(text(R.string.recipe_form_name)).performTextReplacement("Crêpes au sucre")
        compose.onNodeWithContentDescription(text(R.string.recipe_detail_confirm_changes)).performClick()

        // Fin de la modification : le bouton « Modifier » revient et le détail affiche le nouveau titre
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithContentDescription(text(R.string.recipe_detail_edit)).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Crêpes au sucre").assertIsDisplayed()
        compose.onNodeWithContentDescription(text(R.string.action_back)).performClick()
        waitForText(text(R.string.home_subtitle))
        compose.onNodeWithText("Crêpes au sucre").assertIsDisplayed()
    }

    @Test
    fun annulerLaModificationRemetLaRecetteIntacte() {
        launchApp(recipe("Crêpes"))
        waitForText("Crêpes")
        compose.onNodeWithText("Crêpes").performClick()
        waitForText("250 g farine")

        compose.onNodeWithContentDescription(text(R.string.recipe_detail_edit)).performClick()
        field(text(R.string.recipe_form_name)).performTextReplacement("Autre chose")
        compose.onNodeWithText(text(R.string.recipe_form_cancel_changes)).performScrollTo().performClick()

        // Retour à la consultation, avec le titre enregistré (la page peut rester défilée vers le bas)
        compose.onNodeWithContentDescription(text(R.string.recipe_detail_edit)).assertExists()
        compose.onNodeWithText("Crêpes").assertExists()
        compose.onNodeWithText("Autre chose").assertDoesNotExist()
    }

    // --- Étape par étape ---

    @Test
    fun cocherUneEtapeFaitAvancerLaProgression() {
        launchApp(recipe("Crêpes", steps = listOf("Mélanger", "Cuire")))
        waitForText("Crêpes")
        compose.onNodeWithText("Crêpes").performClick()
        waitForText("Mélanger")

        compose.onNodeWithText(text(R.string.recipe_detail_follow_steps)).performScrollTo().performClick()
        waitForText(plural(R.plurals.step_by_step_progress, 2, 0, 2))
        // Première case à cocher : l'étape « Mélanger »
        compose.onAllNodes(isToggleable())[0].performClick()

        waitForText(plural(R.plurals.step_by_step_progress, 2, 1, 2))
        compose.onNodeWithText(text(R.string.step_by_step_uncheck_all)).assertIsDisplayed()
    }

    @Test
    fun ajouterUneEtapeDepuisLeModeEtapeParEtape() {
        launchApp(recipe("Crêpes"))
        waitForText("Crêpes")
        compose.onNodeWithText("Crêpes").performClick()
        waitForText(text(R.string.recipe_detail_follow_steps))
        compose.onNodeWithText(text(R.string.recipe_detail_follow_steps)).performScrollTo().performClick()
        waitForText(text(R.string.step_by_step_empty_title))

        compose.onNodeWithContentDescription(text(R.string.step_add)).performClick()
        compose.waitUntil(5_000) { runCatching { field(text(R.string.step_instruction)).assertExists() }.isSuccess }
        field(text(R.string.step_instruction)).performTextInput("Servir chaud")
        compose.onNodeWithContentDescription(text(R.string.step_confirm_instruction)).performClick()

        waitForText(plural(R.plurals.step_by_step_progress, 1, 0, 1))
        compose.onNodeWithText("Servir chaud").assertIsDisplayed()
    }

    // --- Partage ---

    @Test
    fun toutSelectionnerActiveLaCreationDuQrCode() {
        launchApp(recipe("Crêpes"), recipe("Pizza"))
        waitForText("Pizza")

        compose.onNodeWithContentDescription(text(R.string.home_more_options)).performClick()
        compose.onNodeWithText(text(R.string.menu_share_qr)).performClick()
        waitForText(text(R.string.share_select_all))
        compose.onNodeWithText(text(R.string.share_select_all)).performClick()

        compose.onNodeWithText(text(R.string.share_selected_count, 2, 2)).assertIsDisplayed()
        compose.onNodeWithText(plural(R.plurals.share_generate_qr, 2, 2)).assertIsEnabled()
    }
}
