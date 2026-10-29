package com.example.easycook.ui.composable

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.easycook.data.RecipeDraft
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** La saisie du formulaire de recette survit à une recréation de l'écran (rotation…). */
@RunWith(AndroidJUnit4::class)
class RecipeFormStateTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun laSaisieEstConserveeApresUneRotation() {
        val restoration = StateRestorationTester(compose)
        lateinit var form: RecipeFormState
        restoration.setContent { form = rememberRecipeFormState() }

        compose.runOnIdle {
            form.title = "Crêpes"
            form.image = "photo.jpg"
            form.cookingTime = "20"
            form.description = "Pour 4"
            form.ingredients[0] = "250 g farine"
            form.ingredients.add("3 oeufs")
            form.steps[0] = "Mélanger"
        }

        restoration.emulateSavedInstanceStateRestore()

        compose.runOnIdle {
            assertEquals(
                RecipeDraft(
                    title = "Crêpes",
                    image = "photo.jpg",
                    cookingTime = "20",
                    description = "Pour 4",
                    ingredients = listOf("250 g farine", "3 oeufs"),
                    steps = listOf("Mélanger")
                ),
                form.toDraft()
            )
        }
    }

    @Test
    fun leFormulaireRepartDeLaRecetteQuandLaCleChange() {
        var key by mutableIntStateOf(0)
        lateinit var form: RecipeFormState
        compose.setContent { form = rememberRecipeFormState(key = key) { RecipeDraft(title = "Version $key") } }

        compose.runOnIdle {
            form.title = "Modifiée"
            key = 1
        }

        compose.runOnIdle { assertEquals("Version 1", form.title) }
    }
}
