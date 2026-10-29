package com.example.easycook.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "steps",
    foreignKeys = [
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["recipeId"])]
)
data class StepEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipeId: Int,
    val position: Int,
    val instruction: String,
    val isDone: Boolean = false
)

@Entity(
    tableName = "recipes",
    indices = [Index(value = ["profileId"])]
)
data class RecipeEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    // Nom du fichier de la photo (dans le dossier des photos de l'application), ou chaîne vide
    val image: String,
    val cookingTime: Int,
    val description: String = "",
    val isFavorite: Boolean = false,
    // 'Mon profil' ou profil importé d'où vient la recette
    val profileId: Int = OWN_PROFILE_ID
)

@Entity(
    tableName = "ingredients",
    foreignKeys = [
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["recipeId"])]
)
data class IngredientEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val recipeId: Int,
    val quantity: String,
    val name: String
)

data class RecipeWithIngredients(
    @androidx.room.Embedded val recipe: RecipeEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "recipeId"
    )
    val ingredients: List<IngredientEntity>
)

@Dao
interface RecipeDao {
    @Transaction
    // Les favoris d'abord, puis l'ordre de création
    @Query("SELECT * FROM recipes ORDER BY isFavorite DESC, id")
    fun observeRecipes(): Flow<List<RecipeWithIngredients>>

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :id")
    fun observeRecipe(id: Int): Flow<RecipeWithIngredients?>

    @Query("UPDATE recipes SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Int, isFavorite: Boolean)

    @Query("SELECT image FROM recipes WHERE id = :id")
    suspend fun getImage(id: Int): String?

    @Query("SELECT image FROM recipes")
    suspend fun getAllImages(): List<String>

    // --- Profils importés ---

    @Query("SELECT * FROM profiles ORDER BY number")
    fun observeProfiles(): Flow<List<ProfileEntity>>

    @Query("SELECT number FROM profiles")
    suspend fun getProfileNumbers(): List<Int>

    @Insert
    suspend fun insertProfile(profile: ProfileEntity): Long

    @Query("SELECT image FROM recipes WHERE profileId = :profileId")
    suspend fun getImagesOfProfile(profileId: Int): List<String>

    @Query("DELETE FROM recipes WHERE profileId = :profileId")
    suspend fun deleteRecipesOfProfile(profileId: Int)

    @Query("DELETE FROM profiles WHERE id = :profileId")
    suspend fun deleteProfileById(profileId: Int)

    /** Supprime un profil importé et toutes ses recettes (ingrédients et étapes suivent en cascade). */
    @Transaction
    suspend fun deleteProfileWithRecipes(profileId: Int) {
        deleteRecipesOfProfile(profileId)
        deleteProfileById(profileId)
    }

    // --- Export et import ---

    @Transaction
    @Query("SELECT * FROM recipes WHERE profileId = :profileId ORDER BY id")
    suspend fun getRecipesOfProfile(profileId: Int): List<RecipeWithIngredients>

    @Query("SELECT * FROM steps ORDER BY position, id")
    suspend fun getAllSteps(): List<StepEntity>

    /**
     * Crée un nouveau profil importé contenant les recettes de [export] et renvoie son identifiant.
     * [imageNames] donne, dans le même ordre que les recettes, le nom de leur photo déjà enregistrée.
     * Les saisies vides sont écartées comme lors d'une création manuelle.
     */
    @Transaction
    suspend fun importProfile(
        export: RecipeExport,
        importedAt: Long,
        imageNames: List<String> = emptyList()
    ): Int {
        val profileId = insertProfile(
            ProfileEntity(number = ProfileNumbering.next(getProfileNumbers()), importedAt = importedAt)
        ).toInt()

        export.recipes.forEachIndexed { index, recipe ->
            insertRecipeWithIngredients(
                recipe = RecipeEntity(
                    title = recipe.title.trim(),
                    image = imageNames.getOrElse(index) { "" },
                    cookingTime = recipe.cookingTime.coerceAtLeast(Recipe.NO_COOKING_TIME),
                    description = recipe.description.trim(),
                    profileId = profileId
                ),
                ingredients = RecipeValidator.cleanIngredients(
                    recipe.ingredients.map { Ingredient(quantity = it.quantity, name = it.name) }
                ),
                steps = RecipeValidator.cleanSteps(recipe.steps)
            )
        }
        return profileId
    }

    @Query("DELETE FROM recipes WHERE id = :id")
    suspend fun deleteRecipeById(id: Int)

    @Query("DELETE FROM ingredients WHERE recipeId = :recipeId")
    suspend fun deleteIngredientsByRecipeId(recipeId: Int)

    @Transaction
    @Query("SELECT * FROM steps WHERE recipeId = :recipeId ORDER BY position, id")
    fun observeSteps(recipeId: Int): Flow<List<StepEntity>>

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM steps WHERE recipeId = :recipeId")
    suspend fun nextStepPosition(recipeId: Int): Int

    @Insert
    suspend fun insertStep(step: StepEntity): Long

    @Query("DELETE FROM steps WHERE id = :stepId")
    suspend fun deleteStepById(stepId: Long)

    @Query("DELETE FROM steps WHERE recipeId = :recipeId")
    suspend fun deleteStepsByRecipeId(recipeId: Int)

    @Query("UPDATE steps SET instruction = :instruction WHERE id = :stepId")
    suspend fun updateStepInstruction(stepId: Long, instruction: String)

    @Query("UPDATE steps SET isDone = :isDone WHERE id = :stepId")
    suspend fun setStepDone(stepId: Long, isDone: Boolean)

    @Query("UPDATE steps SET isDone = 0 WHERE recipeId = :recipeId")
    suspend fun resetSteps(recipeId: Int)

    @Transaction
    suspend fun insertSteps(recipeId: Int, steps: List<String>) {
        var position = nextStepPosition(recipeId)
        steps.forEach { instruction ->
            insertStep(
                StepEntity(
                    recipeId = recipeId,
                    position = position++,
                    instruction = instruction
                )
            )
        }
    }

    @Query("UPDATE recipes SET title = :title, image = :image, cookingTime = :cookingTime, description = :description WHERE id = :id")
    suspend fun updateRecipe(
        id: Int,
        title: String,
        image: String,
        cookingTime: Int,
        description: String
    )

    @Transaction
    suspend fun updateRecipeWithIngredients(
        id: Int,
        title: String,
        image: String,
        cookingTime: Int,
        description: String,
        ingredients: List<Ingredient>,
        steps: List<String>
    ) {
        updateRecipe(
            id = id,
            title = title,
            image = image,
            cookingTime = cookingTime,
            description = description
        )
        deleteIngredientsByRecipeId(id)
        insertIngredients(
            ingredients.map { ingredient ->
                IngredientEntity(
                    recipeId = id,
                    quantity = ingredient.quantity,
                    name = ingredient.name
                )
            }
        )
        deleteStepsByRecipeId(id)
        insertSteps(recipeId = id, steps = steps)
    }

    @Insert
    suspend fun insertRecipe(recipe: RecipeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredients(ingredients: List<IngredientEntity>)

    @Transaction
    suspend fun insertRecipeWithIngredients(
        recipe: RecipeEntity,
        ingredients: List<Ingredient>,
        steps: List<String> = emptyList()
    ) {
        val recipeId = insertRecipe(recipe).toInt()
        insertIngredients(
            ingredients.map { ingredient ->
                IngredientEntity(
                    recipeId = recipeId,
                    quantity = ingredient.quantity,
                    name = ingredient.name
                )
            }
        )
        if (steps.isNotEmpty()) {
            insertSteps(recipeId = recipeId, steps = steps)
        }
    }
}

@Database(
    entities = [RecipeEntity::class, IngredientEntity::class, StepEntity::class, ProfileEntity::class],
    version = 6,
    exportSchema = false
)
abstract class RecipeDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE recipes ADD COLUMN description TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `steps` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `recipeId` INTEGER NOT NULL,
                        `position` INTEGER NOT NULL,
                        `instruction` TEXT NOT NULL,
                        `isDone` INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(`recipeId`) REFERENCES `recipes`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_steps_recipeId` ON `steps` (`recipeId`)"
                )
            }
        }

        private val MIGRATION_1_3 = object : Migration(1, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_1_2.migrate(db)
                MIGRATION_2_3.migrate(db)
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE recipes ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `profiles` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `number` INTEGER NOT NULL,
                        `importedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                // Les recettes existantes appartiennent à 'Mon profil'
                db.execSQL("ALTER TABLE recipes ADD COLUMN profileId INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recipes_profileId` ON `recipes` (`profileId`)")
            }
        }

        /**
         * Les photos étaient référencées par leur chemin absolu, qui change lors d'une restauration
         * sur un autre appareil : on ne garde que le nom du fichier (ce qui suit le dernier « / »).
         */
        internal val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(IMAGE_PATH_TO_NAME_SQL)
            }
        }

        // rtrim(image, <tous les caractères sauf « / »>) ne garde que le dossier ; la suite est le nom du fichier
        internal const val IMAGE_PATH_TO_NAME_SQL =
            "UPDATE recipes SET image = substr(image, length(rtrim(image, replace(image, '/', ''))) + 1) " +
                "WHERE image LIKE '%/%'"

        @Volatile
        private var instance: RecipeDatabase? = null

        fun getInstance(context: Context): RecipeDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    RecipeDatabase::class.java,
                    "easycook.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_1_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .build().also { instance = it }
            }
        }
    }
}
