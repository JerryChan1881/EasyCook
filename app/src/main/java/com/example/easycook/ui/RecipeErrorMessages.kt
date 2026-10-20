package com.example.easycook.ui

import androidx.annotation.StringRes
import com.example.easycook.R
import com.example.easycook.data.RecipeError

/** Associe chaque erreur de validation au message à afficher. */
@StringRes
fun RecipeError.messageRes(): Int = when (this) {
    RecipeError.TITLE_REQUIRED -> R.string.error_title_required
    RecipeError.COOKING_TIME_INVALID -> R.string.error_cooking_time_invalid
    RecipeError.NO_COMPLETE_INGREDIENT -> R.string.error_no_complete_ingredient
    RecipeError.INCOMPLETE_INGREDIENTS -> R.string.error_incomplete_ingredients
}
