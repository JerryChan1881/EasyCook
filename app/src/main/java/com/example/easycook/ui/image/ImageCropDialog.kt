package com.example.easycook.ui.image

import android.graphics.Bitmap
import androidx.core.net.toUri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.easycook.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/** Écran de recadrage : on y est seul, sur fond noir, comme dans une application photo. */
private val CropBackground = Color.Black
private val CropContent = Color.White
private val CropDimming = Color.Black.copy(alpha = 0.6f)

private sealed interface LoadState {
    data object Loading : LoadState
    data class Loaded(val bitmap: Bitmap) : LoadState
    data object Failed : LoadState
}

/**
 * Plein écran de recadrage d'une image choisie ou photographiée.
 *
 * L'utilisateur déplace et zoome l'image (deux doigts) dans un cadre aux proportions
 * de [CropMath.ASPECT_RATIO], puis valide, annule, ou relance la prise de vue.
 */
@Composable
fun ImageCropDialog(
    sourceUri: String,
    retakeLabel: String,
    onRetake: () -> Unit,
    onCancel: () -> Unit,
    onConfirm: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    val loadState by produceState<LoadState>(initialValue = LoadState.Loading, sourceUri) {
        value = LoadState.Loading
        val bitmap = withContext(Dispatchers.IO) {
            RecipeImageDecoder.decode(context, sourceUri.toUri())
        }
        value = if (bitmap != null) LoadState.Loaded(bitmap) else LoadState.Failed
    }

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = CropBackground,
            contentColor = CropContent
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.crop_title),
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = stringResource(R.string.crop_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = CropContent.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    when (val state = loadState) {
                        is LoadState.Loaded -> CropArea(
                            bitmap = state.bitmap,
                            onConfirm = onConfirm,
                            onRetake = onRetake,
                            onCancel = onCancel,
                            retakeLabel = retakeLabel
                        )
                        LoadState.Failed -> Text(
                            text = stringResource(R.string.image_error_load),
                            textAlign = TextAlign.Center
                        )
                        LoadState.Loading -> CircularProgressIndicator(color = CropContent)
                    }
                }

                // Pendant le chargement ou après un échec, on peut quand même recommencer ou annuler
                if (loadState !is LoadState.Loaded) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = onRetake) { Text(retakeLabel, color = CropContent) }
                        TextButton(onClick = onCancel) {
                            Text(stringResource(R.string.action_cancel), color = CropContent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CropArea(
    bitmap: Bitmap,
    retakeLabel: String,
    onRetake: () -> Unit,
    onCancel: () -> Unit,
    onConfirm: (Bitmap) -> Unit
) {
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
    // zoom = 1 : l'image recouvre tout juste le cadre
    var zoom by remember(bitmap) { mutableFloatStateOf(1f) }
    var offsetX by remember(bitmap) { mutableFloatStateOf(0f) }
    var offsetY by remember(bitmap) { mutableFloatStateOf(0f) }
    var areaSize by remember { mutableStateOf(IntSize.Zero) }

    // Plus grand cadre aux bonnes proportions qui tient dans la zone de dessin
    val frameWidth = minOf(areaSize.width.toFloat(), areaSize.height * CropMath.ASPECT_RATIO)
    val frameHeight = frameWidth / CropMath.ASPECT_RATIO
    val isMeasured = frameWidth > 0f
    val minScale = if (isMeasured) {
        CropMath.minScale(bitmap.width, bitmap.height, frameWidth, frameHeight)
    } else {
        1f
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clipToBounds()
                .onSizeChanged { areaSize = it }
                .pointerInput(bitmap, frameWidth, frameHeight) {
                    detectTransformGestures { _, pan, gestureZoom, _ ->
                        zoom = (zoom * gestureZoom).coerceIn(1f, CropMath.MAX_ZOOM)
                        val scale = minScale * zoom
                        offsetX = CropMath.clampOffset(offsetX + pan.x, bitmap.width, scale, frameWidth)
                        offsetY = CropMath.clampOffset(offsetY + pan.y, bitmap.height, scale, frameHeight)
                    }
                }
        ) {
            if (!isMeasured) return@Canvas

            val scale = minScale * zoom
            val imageWidth = bitmap.width * scale
            val imageHeight = bitmap.height * scale
            drawImage(
                image = imageBitmap,
                dstOffset = IntOffset(
                    (center.x + offsetX - imageWidth / 2f).roundToInt(),
                    (center.y + offsetY - imageHeight / 2f).roundToInt()
                ),
                dstSize = IntSize(imageWidth.roundToInt(), imageHeight.roundToInt())
            )

            // Assombrit tout ce qui sera coupé, autour du cadre
            val frameLeft = center.x - frameWidth / 2f
            val frameTop = center.y - frameHeight / 2f
            val frameRight = frameLeft + frameWidth
            val frameBottom = frameTop + frameHeight
            drawRect(CropDimming, Offset.Zero, Size(size.width, frameTop))
            drawRect(CropDimming, Offset(0f, frameBottom), Size(size.width, size.height - frameBottom))
            drawRect(CropDimming, Offset(0f, frameTop), Size(frameLeft, frameHeight))
            drawRect(CropDimming, Offset(frameRight, frameTop), Size(size.width - frameRight, frameHeight))

            // Cadre et grille des tiers pour aider à composer
            drawRect(
                color = CropContent,
                topLeft = Offset(frameLeft, frameTop),
                size = Size(frameWidth, frameHeight),
                style = Stroke(width = 2.dp.toPx())
            )
            for (i in 1..2) {
                val x = frameLeft + frameWidth * i / 3f
                val y = frameTop + frameHeight * i / 3f
                drawLine(CropContent.copy(alpha = 0.4f), Offset(x, frameTop), Offset(x, frameBottom))
                drawLine(CropContent.copy(alpha = 0.4f), Offset(frameLeft, y), Offset(frameRight, y))
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onRetake) { Text(retakeLabel, color = CropContent) }
            Row {
                TextButton(onClick = onCancel) {
                    Text(stringResource(R.string.action_cancel), color = CropContent)
                }
                Button(
                    onClick = {
                        val rect = CropMath.cropRect(
                            imageWidth = bitmap.width,
                            imageHeight = bitmap.height,
                            frameWidth = frameWidth,
                            frameHeight = frameHeight,
                            scale = minScale * zoom,
                            offsetX = offsetX,
                            offsetY = offsetY
                        )
                        onConfirm(Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width, rect.height))
                    },
                    enabled = isMeasured
                ) {
                    Text(stringResource(R.string.crop_confirm))
                }
            }
        }
    }
}
