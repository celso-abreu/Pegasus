package com.flutterforge.ui.navigation

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Source
import androidx.compose.material.icons.rounded.FolderSpecial
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Source
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.flutterforge.ui.home.HomeScreen
import com.flutterforge.ui.login.LoginScreen
import com.flutterforge.ui.login.SplashScreen
import com.flutterforge.ui.projects.ProjectsScreen
import com.flutterforge.ui.repodetail.RepoDetailScreen
import com.flutterforge.ui.settings.SettingsScreen
import kotlinx.coroutines.flow.SharedFlow

private data class TopLevelTab(
    val route: Any,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
fun FlutterForgeNavGraph(
    unauthorizedEvents: SharedFlow<Unit>,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val topLevelTabs = listOf(
        TopLevelTab(
            route = HomeRoute,
            labelRes = R.string.nav_repos,
            selectedIcon = Icons.Rounded.Source,
            unselectedIcon = Icons.Outlined.Source,
            testTag = "nav_tab_repos"
        ),
        TopLevelTab(
            route = ProjectsRoute,
            labelRes = R.string.nav_projects,
            selectedIcon = Icons.Rounded.FolderSpecial,
            unselectedIcon = Icons.Outlined.FolderSpecial,
            testTag = "nav_tab_projects"
        ),
        TopLevelTab(
            route = SettingsRoute,
            labelRes = R.string.nav_settings,
            selectedIcon = Icons.Rounded.Settings,
            unselectedIcon = Icons.Outlined.Settings,
            testTag = "nav_tab_settings"
        )
    )

    val showTopLevelNav = currentDestination?.let { dest ->
        dest.hasRoute<HomeRoute>() ||
            dest.hasRoute<ProjectsRoute>() ||
            dest.hasRoute<SettingsRoute>()
    } ?: false

    // Automatic logout on HTTP 401 token revocation
    LaunchedEffect(unauthorizedEvents) {
        unauthorizedEvents.collect {
            navController.navigate(LoginRoute(sessionExpired = true)) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (showTopLevelNav && !useNavRail) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        topLevelTabs.forEach { tab ->
                            val selected = currentDestination?.hierarchy?.any {
                                it.hasRoute(tab.route::class)
                            } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(HomeRoute) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = { Text(text = stringResource(tab.labelRes)) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showTopLevelNav && useNavRail) {
                    NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                        topLevelTabs.forEach { tab ->
                            val selected = currentDestination?.hierarchy?.any {
                                it.hasRoute(tab.route::class)
                            } == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(HomeRoute) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = { Text(text = stringResource(tab.labelRes)) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = SplashRoute,
                    modifier = Modifier.weight(1f)
                ) {
                    composable<SplashRoute> {
                        SplashScreen(
                            onNavigateHome = {
                                navController.navigate(HomeRoute) {
                                    popUpTo<SplashRoute> { inclusive = true }
                                }
                            },
                            onNavigateLogin = {
                                navController.navigate(LoginRoute()) {
                                    popUpTo<SplashRoute> { inclusive = true }
                                }
                            }
                        )
                    }

                    composable<LoginRoute> {
                        LoginScreen(
                            onConnectWithGitHub = {},
                            onLoginSuccess = {
                                navController.navigate(HomeRoute) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        inclusive = true
                                    }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    composable<HomeRoute> {
                        HomeScreen(
                            onRepositoryClick = { repo ->
                                navController.navigate(
                                    RepoDetailRoute(
                                        owner = repo.ownerLogin,
                                        repo = repo.name,
                                        defaultBranch = repo.defaultBranch,
                                        description = repo.description,
                                        isPrivate = repo.isPrivate
                                    )
                                )
                            }
                        )
                    }

                    composable<RepoDetailRoute> {
                        RepoDetailScreen(
                            onBack = { navController.popBackStack() },
                            onOpenProjects = {
                                navController.navigate(ProjectsRoute) {
                                    popUpTo(HomeRoute) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }

                    composable<ProjectsRoute> {
                        ProjectsScreen()
                    }

                    composable<SettingsRoute> {
                        SettingsScreen(
                            onLoggedOut = {
                                navController.navigate(LoginRoute()) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
