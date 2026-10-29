package com.example.easycook.ui.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File

/**
 * Chargement des images en mémoire, en évitant de décoder une photo de 12 Mpx
 * en pleine résolution. Les appels sont bloquants : à faire hors du thread principal.
 */
object RecipeImageDecoder {

    /** Taille maximale utilisée pour recadrer une image choisie ou photographiée. */
    const val CROP_SOURCE_MAX_SIZE = 1600

    /** Taille maximale utilisée pour afficher une image de recette. */
    private const val DISPLAY_MAX_SIZE = 800

    // Les fichiers de recette ne sont jamais réécrits (nom unique), le cache par chemin est donc sûr
    private val displayCache = object : LruCache<String, ImageBitmap>(
        (Runtime.getRuntime().maxMemory() / 8 / 1024).toInt()
    ) {
        override fun sizeOf(key: String, value: ImageBitmap): Int =
            value.width * value.height * 4 / 1024
    }

    /** Décode une image choisie ou photographiée, remise à l'endroit selon ses données EXIF. */
    fun decode(context: Context, uri: Uri, maxSize: Int = CROP_SOURCE_MAX_SIZE): Bitmap? = runCatching {
        val resolver = context.contentResolver

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return null
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = CropMath.sampleSize(bounds.outWidth, bounds.outHeight, maxSize)
        }
        val bitmap = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return null

        val rotation = resolver.openInputStream(uri)?.use { exifRotation(ExifInterface(it)) } ?: 0
        rotate(bitmap, rotation)
    }.getOrNull()

    /** Photo d'une recette prête à afficher, ou null si le fichier est absent ou illisible. */
    fun loadForDisplay(context: Context, file: File): ImageBitmap? {
        val key = file.path
        displayCache.get(key)?.let { return it }

        if (!file.isFile) {
            return null
        }

        return decode(context, Uri.fromFile(file), DISPLAY_MAX_SIZE)
            ?.asImageBitmap()
            ?.also { displayCache.put(key, it) }
    }

    private fun exifRotation(exif: ExifInterface): Int =
        when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }

    private fun rotate(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) {
            return bitmap
        }
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
