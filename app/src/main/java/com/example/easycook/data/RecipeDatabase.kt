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

@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val image: String,
    val cookingTime: Int,
    val description: String = ""
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
    @Query("SELECT * FROM recipes ORDER BY id")
    fun observeRecipes(): Flow<List<RecipeWithIngredients>>

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
    entities = [RecipeEntity::class, IngredientEntity::class, StepEntity::class],
    version = 3,
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

        @Volatile
        private var instance: RecipeDatabase? = null

        fun getInstance(context: Context): RecipeDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    RecipeDatabase::class.java,
                    "easycook.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_1_3)
                    .build().also { instance = it }
            }
        }
    }
}
