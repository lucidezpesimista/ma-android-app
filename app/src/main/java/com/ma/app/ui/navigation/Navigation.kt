package com.ma.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ma.app.data.repository.NodeRepository
import com.ma.app.ui.screens.*

/**
 * Rutas de navegación.
 */
sealed class Screen(val route: String) {
    data object Outline : Screen("outline")
    data object Tasks : Screen("tasks")
    data object Calendar : Screen("calendar")
    data object Claude : Screen("claude")
    data object Search : Screen("search")
    data object NodeDetail : Screen("node/{nodeId}") {
        fun createRoute(nodeId: Long) = "node/$nodeId"
    }
    data object Settings : Screen("settings")
}

/**
 * Ítems del bottom navigation.
 */
data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Outline, "Notas", Icons.Filled.Notes, Icons.Outlined.Notes),
    BottomNavItem(Screen.Tasks, "Tareas", Icons.Filled.CheckBox, Icons.Outlined.CheckBoxOutlineBlank),
    BottomNavItem(Screen.Calendar, "Calendario", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    BottomNavItem(Screen.Claude, "Claude", Icons.Filled.SmartToy, Icons.Outlined.SmartToy)
)

// Rutas que muestran bottom navigation
val bottomNavRoutes = setOf(
    Screen.Outline.route,
    Screen.Tasks.route,
    Screen.Calendar.route,
    Screen.Claude.route
)

/**
 * Componente principal con bottom navigation.
 */
@Composable
fun MaNavigation(
    navController: NavHostController,
    repository: NodeRepository
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute in bottomNavRoutes) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    navController.navigate(item.screen.route) {
                                        // Pop hasta el inicio del grafo para evitar acumulación
                                        popUpTo(Screen.Outline.route) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Outline.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            // Outline (Workflowy)
            composable(Screen.Outline.route) {
                OutlineScreen(
                    repository = repository,
                    onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                    onNavigateToNodeDetail = { nodeId ->
                        navController.navigate(Screen.NodeDetail.createRoute(nodeId))
                    },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }

            // Tareas (Todoist)
            composable(Screen.Tasks.route) {
                TasksScreen(
                    repository = repository,
                    onNavigateToNode = { nodeId ->
                        navController.navigate(Screen.NodeDetail.createRoute(nodeId))
                    }
                )
            }

            // Calendario
            composable(Screen.Calendar.route) {
                CalendarScreen(
                    repository = repository,
                    onNavigateToNode = { nodeId ->
                        navController.navigate(Screen.NodeDetail.createRoute(nodeId))
                    }
                )
            }

            // Claude AI
            composable(Screen.Claude.route) {
                ClaudeScreen()
            }

            // Búsqueda
            composable(Screen.Search.route) {
                SearchScreen(
                    repository = repository,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToNode = { nodeId, _ ->
                        navController.popBackStack()
                    }
                )
            }

            // Detalle de nodo
            composable(
                route = Screen.NodeDetail.route,
                arguments = listOf(
                    navArgument("nodeId") { type = NavType.LongType }
                )
            ) { backStackEntry ->
                val nodeId = backStackEntry.arguments?.getLong("nodeId") ?: return@composable
                NodeDetailScreen(
                    nodeId = nodeId,
                    repository = repository,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Configuración
            composable(Screen.Settings.route) {
                SettingsScreen(
                    repository = repository,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
