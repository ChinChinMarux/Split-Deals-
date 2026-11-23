package com.example.splitthedeals.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.splitthedeals.ui.bill.BillScreen
import com.example.splitthedeals.ui.detail.BillDetailScreen
import com.example.splitthedeals.ui.history.RiwayatScreen
import com.example.splitthedeals.ui.home.HomeScreen
import com.example.splitthedeals.ui.profile.EditProfileScreen
import com.example.splitthedeals.ui.profile.ProfileScreen

private data class Tab(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val tabs = listOf(
    Tab("home", "Home", Icons.Outlined.Home),
    Tab("split", "Split", Icons.Outlined.ContentCut),
    Tab("riwayat", "Riwayat", Icons.Outlined.Receipt),
    Tab("profile", "Profil", Icons.Outlined.Person),
)

private val tabRoutes = tabs.map { it.route }.toSet()


private fun showBottomBar(route: String?): Boolean =
    route != null && route in tabRoutes

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        bottomBar = {
            if (showBottomBar(currentRoute)) {
                NavigationBar {
                    tabs.forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding),
        ) {
            composable("home") { HomeScreen(navController) }
            composable(
                route = Screen.Split,
                arguments = listOf(navArgument("billId") {
                    type = NavType.StringType
                    defaultValue = ""
                }),
            ) { entry ->
                val billId = entry.arguments?.getString("billId").orEmpty()
                BillScreen(navController, billId.ifBlank { null })
            }
            composable("riwayat") { RiwayatScreen(navController) }
            composable("profile") { ProfileScreen(navController) }
            composable(
                route = Screen.BillDetail,
                arguments = listOf(navArgument("billId") { type = NavType.StringType }),
            ) { entry ->
                BillDetailScreen(navController, entry.arguments?.getString("billId").orEmpty())
            }
            composable(Screen.EditProfile) { EditProfileScreen(navController) }
        }
    }
}
