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

    @Query("SELECT COUNT(*) FROM recipes")
    suspend fun countRecipes(): Int

    @Query("DELETE FROM recipes WHERE id = :id")
    suspend fun deleteRecipeById(id: Int)

    @Query("DELETE FROM ingredients WHERE recipeId = :recipeId")
    suspend fun deleteIngredientsByRecipeId(recipeId: Int)

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
        ingredients: List<Ingredient>
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
    }

    @Insert
    suspend fun insertRecipe(recipe: RecipeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredients(ingredients: List<IngredientEntity>)

    @Transaction
    suspend fun insertRecipeWithIngredients(
        recipe: RecipeEntity,
        ingredients: List<Ingredient>
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
    }
}

@Database(
    entities = [RecipeEntity::class, IngredientEntity::class],
    version = 2,
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

        @Volatile
        private var instance: RecipeDatabase? = null

        fun getInstance(context: Context): RecipeDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    RecipeDatabase::class.java,
                    "easycook.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build().also { instance = it }
            }
        }
    }
}
