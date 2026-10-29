package com.example.easycook.data

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Passage de la base de la version 5 (chemins absolus des photos) à la version 6 (noms de fichiers).
 *
 * Une base version 5 est créée à la main dans un fichier de test dédié, puis ouverte par Room :
 * Room applique la migration et vérifie que le schéma obtenu correspond bien aux entités.
 */
@RunWith(AndroidJUnit4::class)
class RecipeDatabaseMigrationTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val dbFile = context.getDatabasePath(TEST_DB)

    @Before
    @After
    fun deleteTestDatabase() {
        context.deleteDatabase(TEST_DB)
    }

    private fun createVersion5Database() {
        dbFile.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(dbFile, null).use { db ->
            db.execSQL(
                "CREATE TABLE recipes (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, " +
                    "image TEXT NOT NULL, cookingTime INTEGER NOT NULL, description TEXT NOT NULL DEFAULT '', " +
                    "isFavorite INTEGER NOT NULL DEFAULT 0, profileId INTEGER NOT NULL DEFAULT 0)"
            )
            db.execSQL("CREATE INDEX index_recipes_profileId ON recipes (profileId)")
            db.execSQL(
                "CREATE TABLE ingredients (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, recipeId INTEGER NOT NULL, " +
                    "quantity TEXT NOT NULL, name TEXT NOT NULL, FOREIGN KEY(recipeId) REFERENCES recipes(id) " +
                    "ON UPDATE NO ACTION ON DELETE CASCADE)"
            )
            db.execSQL("CREATE INDEX index_ingredients_recipeId ON ingredients (recipeId)")
            db.execSQL(
                "CREATE TABLE steps (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, recipeId INTEGER NOT NULL, " +
                    "position INTEGER NOT NULL, instruction TEXT NOT NULL, isDone INTEGER NOT NULL DEFAULT 0, " +
                    "FOREIGN KEY(recipeId) REFERENCES recipes(id) ON UPDATE NO ACTION ON DELETE CASCADE)"
            )
            db.execSQL("CREATE INDEX index_steps_recipeId ON steps (recipeId)")
            db.execSQL(
                "CREATE TABLE profiles (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, number INTEGER NOT NULL, " +
                    "importedAt INTEGER NOT NULL)"
            )

            db.execSQL(
                "INSERT INTO recipes (id, title, image, cookingTime) VALUES " +
                    "(1, 'Avec photo', '/data/user/0/com.example.easycook/files/recipe_images/3f2a-11.jpg', 10), " +
                    "(2, 'Sans photo', '', 0)"
            )
            db.execSQL("INSERT INTO ingredients (recipeId, quantity, name) VALUES (1, '250 g', 'farine')")
            db.version = 5
        }
    }

    @Test
    fun laMigrationNeGardeQueLeNomDuFichierDesPhotos() = runBlocking {
        createVersion5Database()

        val database = Room.databaseBuilder(context, RecipeDatabase::class.java, TEST_DB)
            .addMigrations(RecipeDatabase.MIGRATION_5_6)
            .build()
        try {
            val recipes = database.recipeDao().observeRecipes().first()

            assertEquals(listOf("3f2a-11.jpg", ""), recipes.map { it.recipe.image })
            // Le reste de la recette est intact
            assertEquals("farine", recipes.first().ingredients.single().name)
        } finally {
            database.close()
        }
    }

    private companion object {
        const val TEST_DB = "migration-test.db"
    }
}
