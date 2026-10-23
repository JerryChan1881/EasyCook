package com.example.easycook.ui.image

import kotlin.math.max
import kotlin.math.roundToInt

/** Zone d'une image, en pixels, à conserver lors du recadrage. */
data class CropRect(val left: Int, val top: Int, val width: Int, val height: Int)

/**
 * Calculs du recadrage, sans dépendance Android pour être testés sur la JVM.
 *
 * Convention : l'image est dessinée avec une échelle `scale` et son centre est décalé
 * de (`offsetX`, `offsetY`) par rapport au centre du cadre de recadrage.
 */
object CropMath {

    /** Proportions du cadre de recadrage (largeur / hauteur). */
    const val ASPECT_RATIO = 4f / 3f

    /** Zoom maximal autorisé, relatif à l'échelle minimale. */
    const val MAX_ZOOM = 5f

    /** Échelle minimale pour que l'image recouvre entièrement le cadre. */
    fun minScale(imageWidth: Int, imageHeight: Int, frameWidth: Float, frameHeight: Float): Float =
        max(frameWidth / imageWidth, frameHeight / imageHeight)

    /**
     * Limite le décalage sur un axe pour que l'image recouvre toujours le cadre
     * (aucune bande vide ne doit apparaître dans la zone recadrée).
     */
    fun clampOffset(offset: Float, imageSize: Int, scale: Float, frameSize: Float): Float {
        val maxOffset = max(0f, (imageSize * scale - frameSize) / 2f)
        return offset.coerceIn(-maxOffset, maxOffset)
    }

    /** Partie de l'image (en pixels de l'image) visible dans le cadre. */
    fun cropRect(
        imageWidth: Int,
        imageHeight: Int,
        frameWidth: Float,
        frameHeight: Float,
        scale: Float,
        offsetX: Float,
        offsetY: Float
    ): CropRect {
        val width = (frameWidth / scale).roundToInt().coerceIn(1, imageWidth)
        val height = (frameHeight / scale).roundToInt().coerceIn(1, imageHeight)
        val left = ((imageWidth * scale / 2f - frameWidth / 2f - offsetX) / scale).roundToInt()
        val top = ((imageHeight * scale / 2f - frameHeight / 2f - offsetY) / scale).roundToInt()

        return CropRect(
            left = left.coerceIn(0, imageWidth - width),
            top = top.coerceIn(0, imageHeight - height),
            width = width,
            height = height
        )
    }

    /**
     * Facteur de sous-échantillonnage (puissance de 2) pour décoder une image en
     * mémoire sans dépasser inutilement [maxSize] pixels sur son plus grand côté.
     */
    fun sampleSize(width: Int, height: Int, maxSize: Int): Int {
        val longestSide = max(width, height)
        var sampleSize = 1
        while (longestSide / (sampleSize * 2) >= maxSize) {
            sampleSize *= 2
        }
        return sampleSize
    }
}
