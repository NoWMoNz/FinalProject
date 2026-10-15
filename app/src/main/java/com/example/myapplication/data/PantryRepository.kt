package com.example.myapplication.data

import com.example.myapplication.model.PantryItem
import com.example.myapplication.model.Recipe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray

class PantryRepository(
    private val pantryDao: PantryDao,
    private val savedRecipeDao: SavedRecipeDao
) {

    val pantryItems: Flow<List<PantryItem>> = pantryDao.getAllPantryItems().map { entities ->
        entities.map { entity ->
            PantryItem(
                id = entity.id,
                name = entity.name,
                daysUntilExpiration = entity.daysUntilExpiration,
                category = entity.category
            )
        }
    }

    val savedRecipes: Flow<List<Recipe>> = savedRecipeDao.getAllSavedRecipes().map { entities ->
        entities.map { entity ->
            Recipe(
                title = entity.title,
                prepTime = entity.prepTime,
                difficulty = entity.difficulty,
                usedIngredients = jsonToList(entity.usedIngredientsJson),
                missingIngredients = jsonToList(entity.missingIngredientsJson),
                instructions = jsonToList(entity.instructionsJson)
            )
        }
    }

    suspend fun addPantryItem(name: String, daysUntilExpiration: Int, category: String = "Pantry") {
        pantryDao.insertPantryItem(
            PantryItemEntity(
                name = name,
                daysUntilExpiration = daysUntilExpiration,
                category = category
            )
        )
    }

    suspend fun removePantryItem(id: String) {
        pantryDao.deletePantryItemById(id)
    }

    suspend fun saveRecipe(recipe: Recipe) {
        savedRecipeDao.insertSavedRecipe(
            SavedRecipeEntity(
                title = recipe.title,
                prepTime = recipe.prepTime,
                difficulty = recipe.difficulty,
                usedIngredientsJson = listToJson(recipe.usedIngredients),
                missingIngredientsJson = listToJson(recipe.missingIngredients),
                instructionsJson = listToJson(recipe.instructions)
            )
        )
    }

    suspend fun removeSavedRecipe(title: String) {
        val recipes = savedRecipeDao.getAllSavedRecipes()
        // Simple title matching for deletion
        savedRecipeDao.deleteSavedRecipeById(title)
    }

    private fun listToJson(list: List<String>): String {
        val jsonArray = JSONArray()
        list.forEach { jsonArray.put(it) }
        return jsonArray.toString()
    }

    private fun jsonToList(json: String): List<String> {
        val list = mutableListOf<String>()
        try {
            val jsonArray = JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getString(i))
            }
        } catch (_: Exception) {}
        return list
    }
}
