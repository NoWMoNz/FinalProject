package com.example.myapplication.model

import java.util.UUID

enum class ExpirationStatus {
    URGENT,        // 0-1 days (Red)
    EXPIRING_SOON, // 2-3 days (Yellow / Orange)
    FRESH          // 4+ days (Green)
}

data class PantryItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val daysUntilExpiration: Int,
    val category: String = "Pantry",
    val quantity: String = "1 item"
) {
    val status: ExpirationStatus
        get() = when {
            daysUntilExpiration <= 1 -> ExpirationStatus.URGENT
            daysUntilExpiration <= 3 -> ExpirationStatus.EXPIRING_SOON
            else -> ExpirationStatus.FRESH
        }
}

data class Recipe(
    val title: String,
    val prepTime: String,
    val difficulty: String,
    val usedIngredients: List<String>,
    val missingIngredients: List<String>,
    val instructions: List<String>
)
