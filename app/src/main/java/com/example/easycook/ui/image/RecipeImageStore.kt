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
 * Fichiers des photos de recettes, rangés dans le stockage privé de l'application.
 *
 * Chaque photo enregistrée reçoit un nom unique et n'est jamais réécrite :
 * changer la photo d'une recette crée un nouveau fichier et supprime l'ancien.
 * La base ne stocke que ce nom ; le dossier est retrouvé ici à chaque accès.
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

    /**
     * Fichier d'une photo de recette, ou null si le nom est vide ou ne désigne pas
     * un fichier du dossier des photos (un nom contenant « ../ » est ainsi refusé).
     */
    fun fileOf(name: String): File? {
        if (name.isBlank()) return null
        val file = File(imagesDir, name)
        return file.takeIf { it.parentFile == imagesDir }
    }

    /** Enregistre une photo recadrée et renvoie son nom, à stocker dans la recette. */
    fun save(bitmap: Bitmap): String {
        val name = newName()
        File(imagesDir, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        return name
    }

    override fun read(name: String): ByteArray? {
        val file = fileOf(name)?.takeIf { it.isFile } ?: return null
        return runCatching { file.readBytes() }.getOrNull()
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

        val name = newName()
        return runCatching {
            File(imagesDir, name).writeBytes(bytes)
            name
        }.getOrNull()
    }

    override fun delete(name: String) {
        fileOf(name)?.delete()
    }

    override fun deleteUnused(usedNames: Set<String>) {
        imagesDir.listFiles()
            ?.filter { it.name !in usedNames }
            ?.forEach { it.delete() }
        // Les photos brutes de l'appareil photo ne servent que le temps du recadrage
        cameraDir.listFiles()?.forEach { it.delete() }
    }

    private fun newName() = "${UUID.randomUUID()}.jpg"

    private companion object {
        const val IMAGES_DIR = "recipe_images"
        const val CAMERA_DIR = "camera"
        const val JPEG_QUALITY = 85
        const val MAX_RECEIVED_IMAGE_BYTES = 5 * 1024 * 1024
    }
}
