package com.example.easycook

import android.app.Application
import com.example.easycook.data.RecipeDatabase
import com.example.easycook.data.RecipeRepository
import com.example.easycook.ui.image.RecipeImageStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Crée une seule fois, pour toute l'application, le [RecipeRepository] partagé par les ViewModels.
 */
class EasyCookApplication : Application() {

    /** Tâches qui ne dépendent d'aucun écran (nettoyage des photos au démarrage). */
    private val applicationScope = CoroutineScope(SupervisorJob())

    val repository: RecipeRepository by lazy {
        RecipeRepository(
            dao = RecipeDatabase.getInstance(this).recipeDao(),
            imageFiles = RecipeImageStore(this)
        )
    }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch { repository.deleteUnusedImages() }
    }
}
