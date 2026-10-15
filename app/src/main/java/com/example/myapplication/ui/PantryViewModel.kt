package com.example.myapplication.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.PantryDatabase
import com.example.myapplication.data.PantryRepository
import com.example.myapplication.model.ExpirationStatus
import com.example.myapplication.model.PantryItem
import com.example.myapplication.model.Recipe
import com.example.myapplication.service.RecipeAiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface RecipeGenerationState {
    object Idle : RecipeGenerationState
    object Loading : RecipeGenerationState
    data class Success(val recipes: List<Recipe>) : RecipeGenerationState
    data class Error(val message: String) : RecipeGenerationState
}

class PantryViewModel(application: Application) : AndroidViewModel(application) {

    constructor() : this(Application())

    private val db = try {
        if (application.baseContext == null) {
            null
        } else {
            PantryDatabase.getDatabase(application)
        }
    } catch (_: Throwable) {
        null
    }

    private val repository = db?.let { PantryRepository(it.pantryDao(), it.savedRecipeDao()) }
    private val recipeAiService = RecipeAiService()

    private val defaultPantryItems = listOf(
        PantryItem(name = "Chicken Breast", daysUntilExpiration = 1, category = "Meat"),
        PantryItem(name = "Tomatoes", daysUntilExpiration = 2, category = "Produce"),
        PantryItem(name = "Spinach", daysUntilExpiration = 2, category = "Produce"),
        PantryItem(name = "Garlic", daysUntilExpiration = 5, category = "Pantry"),
        PantryItem(name = "Brown Rice", daysUntilExpiration = 12, category = "Pantry"),
        PantryItem(name = "Avocado", daysUntilExpiration = 3, category = "Produce")
    )

    private val _fallbackPantryItems = MutableStateFlow(defaultPantryItems)
    private val _fallbackSavedRecipes = MutableStateFlow<List<Recipe>>(emptyList())

    val pantryItems: StateFlow<List<PantryItem>> = repository?.pantryItems
        ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), defaultPantryItems)
        ?: _fallbackPantryItems.asStateFlow()

    val savedRecipes: StateFlow<List<Recipe>> = repository?.savedRecipes
        ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        ?: _fallbackSavedRecipes.asStateFlow()

    private val _recipeState = MutableStateFlow<RecipeGenerationState>(RecipeGenerationState.Idle)
    val recipeState: StateFlow<RecipeGenerationState> = _recipeState.asStateFlow()

    private val _selectedRecipe = MutableStateFlow<Recipe?>(null)
    val selectedRecipe: StateFlow<Recipe?> = _selectedRecipe.asStateFlow()

    private val _aiAdvice = MutableStateFlow<String?>(null)
    val aiAdvice: StateFlow<String?> = _aiAdvice.asStateFlow()

    private val _isAiAdviceLoading = MutableStateFlow(false)
    val isAiAdviceLoading: StateFlow<Boolean> = _isAiAdviceLoading.asStateFlow()

    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    fun updateApiKey(key: String) {
        _apiKey.value = key.trim()
    }

    fun addPantryItem(name: String, daysUntilExpiration: Int, category: String = "Pantry") {
        if (name.isBlank()) return
        viewModelScope.launch {
            if (repository != null) {
                repository.addPantryItem(name.trim(), daysUntilExpiration, category)
            } else {
                _fallbackPantryItems.value = _fallbackPantryItems.value + PantryItem(
                    name = name.trim(),
                    daysUntilExpiration = daysUntilExpiration,
                    category = category
                )
            }
        }
    }

    fun removePantryItem(id: String) {
        viewModelScope.launch {
            if (repository != null) {
                repository.removePantryItem(id)
            } else {
                _fallbackPantryItems.value = _fallbackPantryItems.value.filter { it.id != id }
            }
        }
    }

    fun selectRecipe(recipe: Recipe?) {
        _selectedRecipe.value = recipe
    }

    fun saveRecipe(recipe: Recipe) {
        viewModelScope.launch {
            if (repository != null) {
                repository.saveRecipe(recipe)
            } else {
                _fallbackSavedRecipes.value = _fallbackSavedRecipes.value + recipe
            }
        }
    }

    fun generateRecipes() {
        val currentItems = pantryItems.value
        val expiringIngredients = currentItems
            .filter { it.status == ExpirationStatus.URGENT || it.status == ExpirationStatus.EXPIRING_SOON }
            .map { it.name.lowercase() }

        val ingredientsToQuery = if (expiringIngredients.isNotEmpty()) {
            expiringIngredients
        } else {
            currentItems.map { it.name.lowercase() }
        }

        if (ingredientsToQuery.isEmpty()) {
            _recipeState.value = RecipeGenerationState.Error("No ingredients available in pantry.")
            return
        }

        _recipeState.value = RecipeGenerationState.Loading
        val key = _apiKey.value.ifBlank { null }

        viewModelScope.launch {
            val result = recipeAiService.generateRecipes(ingredientsToQuery, key)
            result.onSuccess { recipes ->
                _recipeState.value = RecipeGenerationState.Success(recipes)
            }.onFailure { error ->
                _recipeState.value = RecipeGenerationState.Error(
                    error.localizedMessage ?: "Failed to generate recipes. Please try again."
                )
            }
        }
    }

    fun askAiAdvice(userQuery: String) {
        if (userQuery.isBlank()) return
        _isAiAdviceLoading.value = true
        val key = _apiKey.value.ifBlank { null }

        viewModelScope.launch {
            val result = recipeAiService.getAiAdvice(userQuery, key)
            result.onSuccess { advice ->
                _aiAdvice.value = advice
            }.onFailure {
                _aiAdvice.value = "Unable to get AI advice right now. Please check your network or try again."
            }
            _isAiAdviceLoading.value = false
        }
    }

        fun resetRecipeState() {
        _recipeState.value = RecipeGenerationState.Idle
    }
}
