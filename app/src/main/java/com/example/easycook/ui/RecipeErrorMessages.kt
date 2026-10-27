package com.example.easycook.ui

import androidx.annotation.StringRes
import com.example.easycook.R
import com.example.easycook.data.ImportError
import com.example.easycook.data.RecipeError
import com.example.easycook.transfer.DownloadError

/** Associe chaque erreur de validation au message à afficher. */
@StringRes
fun RecipeError.messageRes(): Int = when (this) {
    RecipeError.TITLE_REQUIRED -> R.string.error_title_required
    RecipeError.COOKING_TIME_INVALID -> R.string.error_cooking_time_invalid
    RecipeError.NO_INGREDIENT -> R.string.error_no_ingredient
}

/** Associe chaque erreur d'import au message à afficher. */
@StringRes
fun ImportError.messageRes(): Int = when (this) {
    ImportError.INVALID_FILE -> R.string.import_error_invalid
    ImportError.UNSUPPORTED_VERSION -> R.string.import_error_version
    ImportError.NO_RECIPES -> R.string.import_error_empty
}

/** Associe chaque échec de réception par QR code au message à afficher. */
@StringRes
fun DownloadError.messageRes(): Int = when (this) {
    DownloadError.UNREACHABLE -> R.string.receive_error_unreachable
    DownloadError.REFUSED -> R.string.receive_error_refused
    DownloadError.TOO_LARGE -> R.string.receive_error_too_large
    DownloadError.INVALID_DATA -> R.string.receive_error_invalid_data
}
