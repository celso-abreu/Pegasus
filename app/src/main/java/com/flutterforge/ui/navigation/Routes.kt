package com.flutterforge.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object SplashRoute

@Serializable
data class LoginRoute(val sessionExpired: Boolean = false)

@Serializable
data object HomeRoute

@Serializable
data class RepoDetailRoute(
    val owner: String,
    val repo: String,
    val defaultBranch: String,
    val description: String? = null,
    val isPrivate: Boolean = false
)

@Serializable
data object ProjectsRoute

@Serializable
data object SettingsRoute
