package com.ma.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ma.app.data.repository.NodeRepository
import com.ma.app.ui.screens.NodeDetailScreen
import com.ma.app.ui.screens.OutlineScreen
import com.ma.app.ui.screens.SearchScreen
import com.ma.app.ui.screens.SettingsScreen

/**
 * Rutas de navegación de la app.
 */
sealed class Screen(val route: String) {
    data object Outline : Screen("outline")
    data object Search : Screen("search")
    data object NodeDetail : Screen("node/{nodeId}") {
        fun createRoute(nodeId: Long) = "node/$nodeId"
    }
    data object Settings : Screen("settings")
}

/**
 * Grafo de navegación principal.
 */
@Composable
fun MaNavigation(
    navController: NavHostController,
    repository: NodeRepository
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Outline.route
    ) {
        // Pantalla principal de outline
        composable(Screen.Outline.route) {
            OutlineScreen(
                repository = repository,
                onNavigateToSearch = {
                    navController.navigate(Screen.Search.route)
                },
                onNavigateToNodeDetail = { nodeId ->
                    navController.navigate(Screen.NodeDetail.createRoute(nodeId))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        // Pantalla de búsqueda
        composable(Screen.Search.route) {
            SearchScreen(
                repository = repository,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToNode = { nodeId, parentId ->
                    // Navegar al outline con focus en el padre
                    navController.popBackStack()
                    // TODO: Pasar el parentId al outline para hacer focus
                }
            )
        }

        // Pantalla de detalle de nodo
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
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Pantalla de configuración
        composable(Screen.Settings.route) {
            SettingsScreen(
                repository = repository,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
