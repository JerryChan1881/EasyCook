package com.example.easycook.ui.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.easycook.data.RecipeImageFiles
import java.io.File
import java.util.UUID

/**
 * Fichiers images des recettes, rangés dans le stockage privé de l'application.
 *
 * Chaque image enregistrée reçoit un nom unique et n'est jamais réécrite :
 * changer l'image d'une recette crée un nouveau fichier et supprime l'ancien.
 */
class RecipeImageStore(context: Context) : RecipeImageFiles {

    private val appContext = context.applicationContext

    private val imagesDir: File
        get() = File(appContext.filesDir, IMAGES_DIR).apply { mkdirs() }

    private val cameraDir: File
        get() = File(appContext.cacheDir, CAMERA_DIR).apply { mkdirs() }

    /** Adresse où l'application appareil photo doit enregistrer la prochaine photo. */
    fun createCameraUri(): Uri {
        val file = File(cameraDir, "photo_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(appContext, "${appContext.packageName}.fileprovider", file)
    }

    /** Enregistre une image recadrée et renvoie son chemin, à stocker dans la recette. */
    fun save(bitmap: Bitmap): String {
        val file = File(imagesDir, "${UUID.randomUUID()}.jpg")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        return file.absolutePath
    }

    override fun read(path: String): ByteArray? {
        val file = File(path)
        return if (file.parentFile == imagesDir && file.isFile) {
            runCatching { file.readBytes() }.getOrNull()
        } else {
            null
        }
    }

    override fun save(bytes: ByteArray): String? {
        // Une image reçue d'un autre téléphone doit être une vraie image, de taille raisonnable
        if (bytes.isEmpty() || bytes.size > MAX_RECEIVED_IMAGE_BYTES) {
            return null
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return null
        }

        val file = File(imagesDir, "${UUID.randomUUID()}.jpg")
        return runCatching {
            file.writeBytes(bytes)
            file.absolutePath
        }.getOrNull()
    }

    override fun delete(path: String) {
        val file = File(path)
        if (file.parentFile == imagesDir) {
            file.delete()
        }
    }

    override fun deleteUnused(usedPaths: Set<String>) {
        imagesDir.listFiles()
            ?.filter { it.absolutePath !in usedPaths }
            ?.forEach { it.delete() }
        // Les photos brutes de l'appareil photo ne servent que le temps du recadrage
        cameraDir.listFiles()?.forEach { it.delete() }
    }

    private companion object {
        const val IMAGES_DIR = "recipe_images"
        const val CAMERA_DIR = "camera"
        const val JPEG_QUALITY = 85
        const val MAX_RECEIVED_IMAGE_BYTES = 5 * 1024 * 1024
    }
}
