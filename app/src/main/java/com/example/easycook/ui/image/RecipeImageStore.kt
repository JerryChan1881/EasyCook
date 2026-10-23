package com.example.easycook.ui.image

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.easycook.data.RecipeImageCleaner
import java.io.File
import java.util.UUID

/**
 * Fichiers images des recettes, rangés dans le stockage privé de l'application.
 *
 * Chaque image enregistrée reçoit un nom unique et n'est jamais réécrite :
 * changer l'image d'une recette crée un nouveau fichier et supprime l'ancien.
 */
class RecipeImageStore(context: Context) : RecipeImageCleaner {

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
    }
}
