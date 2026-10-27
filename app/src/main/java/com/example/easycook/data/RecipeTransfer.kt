package com.example.easycook.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Format JSON d'échange de recettes entre deux téléphones (fichier aujourd'hui, QR code demain).
 *
 * Les photos voyagent dans le JSON, encodées en base64 (champ facultatif : un fichier sans
 * photo reste valide). Les favoris ne sont pas transférés : ils sont propres à chaque utilisateur.
 */
@Serializable
data class RecipeExport(
    /** Signature du format, pour refuser un fichier JSON qui n'a rien à voir. */
    val format: String = FORMAT,
    /** Version du format, à incrémenter si sa structure change. */
    val version: Int = CURRENT_VERSION,
    val exportedAt: Long,
    val recipes: List<ExportedRecipe>
) {
    companion object {
        const val FORMAT = "easycook-recipes"
        const val CURRENT_VERSION = 1
    }
}

@Serializable
data class ExportedRecipe(
    val title: String,
    /** En minutes ; [Recipe.NO_COOKING_TIME] si non renseigné. */
    val cookingTime: Int = Recipe.NO_COOKING_TIME,
    val description: String = "",
    val ingredients: List<ExportedIngredient> = emptyList(),
    val steps: List<String> = emptyList(),
    /** Photo de la recette en JPEG encodé en base64 ; absente si la recette n'a pas de photo. */
    val image: String? = null
)

@Serializable
data class ExportedIngredient(
    val quantity: String = "",
    val name: String = ""
)

/** Raisons pour lesquelles un fichier ne peut pas être importé. */
enum class ImportError {
    /** Ce n'est pas du JSON lisible, ou pas un export EasyCook. */
    INVALID_FILE,
    /** Export d'une version plus récente de l'application. */
    UNSUPPORTED_VERSION,
    /** Export valide, mais sans aucune recette. */
    NO_RECIPES
}

/** Résultat de la lecture d'un export. */
sealed interface DecodeResult {
    data class Success(val export: RecipeExport) : DecodeResult
    data class Failure(val error: ImportError) : DecodeResult
}

object RecipeTransfer {

    private val json = Json {
        // Un export d'une version future peut contenir des champs inconnus : on les ignore
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(export: RecipeExport): String = json.encodeToString(RecipeExport.serializer(), export)

    fun decode(text: String): DecodeResult {
        val export = try {
            json.decodeFromString(RecipeExport.serializer(), text)
        } catch (_: SerializationException) {
            return DecodeResult.Failure(ImportError.INVALID_FILE)
        } catch (_: IllegalArgumentException) {
            return DecodeResult.Failure(ImportError.INVALID_FILE)
        }

        return when {
            export.format != RecipeExport.FORMAT -> DecodeResult.Failure(ImportError.INVALID_FILE)
            export.version > RecipeExport.CURRENT_VERSION -> DecodeResult.Failure(ImportError.UNSUPPORTED_VERSION)
            export.recipes.none { it.title.isNotBlank() } -> DecodeResult.Failure(ImportError.NO_RECIPES)
            else -> DecodeResult.Success(export.copy(recipes = export.recipes.filter { it.title.isNotBlank() }))
        }
    }
}

/** Résultat d'un import de recettes. */
sealed interface ImportResult {
    data class Success(val profileId: Int, val recipeCount: Int) : ImportResult
    data class Failure(val error: ImportError) : ImportResult
}
