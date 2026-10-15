package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "pantry_items")
data class PantryItemEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val daysUntilExpiration: Int,
    val category: String = "Pantry"
)

@Entity(tableName = "saved_recipes")
data class SavedRecipeEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val prepTime: String,
    val difficulty: String,
    val usedIngredientsJson: String,
    val missingIngredientsJson: String,
    val instructionsJson: String,
    val savedTimestamp: Long = System.currentTimeMillis()
)
