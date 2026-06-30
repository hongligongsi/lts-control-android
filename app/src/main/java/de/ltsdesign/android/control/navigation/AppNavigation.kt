package de.ltsdesign.android.control.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import de.ltsdesign.android.control.ui.ControlViewModel
import de.ltsdesign.android.control.ui.screens.ConnectionScreen
import de.ltsdesign.android.control.ui.screens.ControlScreen
import de.ltsdesign.android.control.ui.screens.MoreScreen
import de.ltsdesign.android.control.ui.screens.SettingsScreen

@Composable
fun LTSApp(navController: NavHostController = rememberNavController()) {
    val vm: ControlViewModel = viewModel()

    Scaffold(
        bottomBar = { LTSTabBar(navController) },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = TabRoutes.Control,
            modifier = Modifier.padding(padding),
        ) {
            composable(TabRoutes.Control) { ControlScreen(vm) }
            composable(TabRoutes.Settings) { SettingsScreen() }
            composable(TabRoutes.Connection) { ConnectionScreen() }
            composable(TabRoutes.More) { MoreScreen() }
        }
    }
}
