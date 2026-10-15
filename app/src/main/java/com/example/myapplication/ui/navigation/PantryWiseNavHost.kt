package com.example.myapplication.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.ui.GreenLight
import com.example.myapplication.ui.GreenPrimary
import com.example.myapplication.ui.PantryViewModel
import com.example.myapplication.ui.PantryWiseScreen
import com.example.myapplication.ui.screens.AiAssistantScreen
import com.example.myapplication.ui.screens.RecipeDetailScreen
import com.example.myapplication.ui.screens.SavedRecipesScreen

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Pantry : Screen("pantry", "Pantry", Icons.Default.Kitchen)
    object Saved : Screen("saved", "Saved", Icons.Default.Bookmark)
    object AiAssistant : Screen("ai_assistant", "AI Advisor", Icons.Default.AutoAwesome)
    object RecipeDetail : Screen("recipe_detail", "Recipe Detail", Icons.Default.Kitchen)
}

@Composable
fun PantryWiseNavHost(
    viewModel: PantryViewModel = viewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        Screen.Pantry,
        Screen.Saved,
        Screen.AiAssistant
    )

    Scaffold(
        bottomBar = {
            if (currentRoute in bottomNavItems.map { it.route }) {
                NavigationBar(
                    containerColor = Color.White
                ) {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(imageVector = screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentRoute == screen.route,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = GreenPrimary,
                                selectedTextColor = GreenPrimary,
                                indicatorColor = GreenLight
                            ),
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Pantry.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Pantry.route) {
                PantryWiseScreen(
                    viewModel = viewModel,
                    onSelectRecipe = { recipe ->
                        viewModel.selectRecipe(recipe)
                        navController.navigate(Screen.RecipeDetail.route)
                    }
                )
            }

            composable(Screen.Saved.route) {
                SavedRecipesScreen(
                    viewModel = viewModel,
                    onSelectRecipe = { recipe ->
                        viewModel.selectRecipe(recipe)
                        navController.navigate(Screen.RecipeDetail.route)
                    }
                )
            }

            composable(Screen.AiAssistant.route) {
                AiAssistantScreen(viewModel = viewModel)
            }

            composable(Screen.RecipeDetail.route) {
                val selectedRecipe by viewModel.selectedRecipe.collectAsState()
                RecipeDetailScreen(
                    recipe = selectedRecipe,
                    onBack = { navController.popBackStack() },
                    onSaveRecipe = { recipe -> viewModel.saveRecipe(recipe) }
                )
            }
        }
    }
}
