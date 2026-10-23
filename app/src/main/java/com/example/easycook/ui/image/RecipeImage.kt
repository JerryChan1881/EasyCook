package com.example.easycook.ui.image

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.easycook.R
import com.example.easycook.ui.composable.RecipeBanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Image d'une recette, recadrée pour remplir [modifier].
 * Sans image (ou si le fichier est illisible), affiche l'illustration de remplacement.
 */
@Composable
fun RecipeImage(
    recipeId: Int,
    imagePath: String,
    modifier: Modifier = Modifier,
    iconSize: Dp = 32.dp
) {
    val context = LocalContext.current
    val image by produceState<ImageBitmap?>(initialValue = null, imagePath) {
        value = if (imagePath.isBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) { RecipeImageDecoder.loadForDisplay(context, imagePath) }
        }
    }

    val loaded = image
    if (loaded != null) {
        Image(
            bitmap = loaded,
            contentDescription = stringResource(R.string.image_description),
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        RecipeBanner(recipeId = recipeId, modifier = modifier, iconSize = iconSize)
    }
}
