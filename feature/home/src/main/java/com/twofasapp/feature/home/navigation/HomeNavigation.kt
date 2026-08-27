package com.twofasapp.feature.home.navigation

import android.app.Activity
import android.graphics.Path
import android.view.animation.PathInterpolator
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.twofasapp.android.navigation.NavArg
import com.twofasapp.android.navigation.Screen
import com.twofasapp.data.browserext.domain.TokenRequest
import com.twofasapp.feature.home.ui.bottombar.BottomBarListener
import com.twofasapp.feature.home.ui.editservice.EditServiceScreenRoute
import com.twofasapp.feature.home.ui.groups.GroupEntriesRoute
import com.twofasapp.feature.home.ui.groups.GroupsRoute
import com.twofasapp.feature.home.ui.notifications.NotificationsScreen
import com.twofasapp.feature.home.ui.services.ServicesRoute
import com.twofasapp.feature.home.ui.settings.SettingsRoute

fun NavGraphBuilder.homeNavigation(
    navController: NavController,
    listener: HomeNavigationListener,
    openEditServiceAuth: (successCallback: () -> Unit) -> Unit,
    homeSlideDistancePx: Int,
) {
    val bottomBarListener = object : BottomBarListener {
        override fun openHome() {
            navController.popBackStack(
                route = Screen.Services.route,
                inclusive = false,
                saveState = true,
            )
        }

        override fun openSettings() {
            navController.navigate(Screen.Settings.route) {
                popUpTo(Screen.Services.route) { inclusive = false }
            }
        }

        override fun openGroups() {
            navController.navigate(Screen.Groups.route) {
                launchSingleTop = true
            }
        }
    }

    composable(
        route = Screen.Services.route,
        enterTransition = homeSlideEnter(homeSlideDistancePx),
        exitTransition = homeSlideExit(),
        popEnterTransition = homeSlidePopEnter(),
        popExitTransition = homeSlidePopExit(homeSlideDistancePx),
    ) {
        ServicesRoute(
            listener = listener,
            bottomBarListener = bottomBarListener,
            openAuth = openEditServiceAuth,
        )
    }

    composable(
        route = Screen.Settings.route,
        enterTransition = homeSlideEnter(homeSlideDistancePx),
        exitTransition = homeSlideExit(),
        popEnterTransition = homeSlidePopEnter(),
        popExitTransition = homeSlidePopExit(homeSlideDistancePx),
    ) {
        SettingsRoute(listener, bottomBarListener)
    }

    composable(
        route = Screen.Groups.route,
        enterTransition = homeSlideEnter(homeSlideDistancePx),
        exitTransition = homeSlideExit(),
        popEnterTransition = homeSlidePopEnter(),
        popExitTransition = homeSlidePopExit(homeSlideDistancePx),
    ) {
        GroupsRoute(
            onBack = { navController.popBackStack() },
            onAssignEntries = { groupId ->
                navController.navigate(
                    Screen.GroupEntries.routeWithArgs(NavArg.GroupId to groupId),
                )
            },
        )
    }

    composable(
        route = Screen.GroupEntries.route,
        arguments = listOf(NavArg.GroupId),
        enterTransition = homeSlideEnter(homeSlideDistancePx),
        exitTransition = homeSlideExit(),
        popEnterTransition = homeSlidePopEnter(),
        popExitTransition = homeSlidePopExit(homeSlideDistancePx),
    ) { backStackEntry ->
        GroupEntriesRoute(
            groupId = backStackEntry.arguments?.getString(NavArg.GroupId.name).orEmpty(),
            onBack = { navController.popBackStack() },
        )
    }

    composable(Screen.Notifications.route) {
        NotificationsScreen(
            openInternalRoute = { route ->
                when (route) {
                    Screen.Backup.route -> {
                        navController.navigate(Screen.Backup.routeWithArgs(NavArg.TurnOnBackup to true))
                    }

                    else -> {
                        navController.navigate(route)
                    }
                }
            },
        )
    }

    composable(Screen.EditService.route, listOf(NavArg.ServiceId, NavArg.EditServiceAction)) { backStackEntry ->
        EditServiceScreenRoute(
            navController = navController,
            openSecurity = { navController.navigate(Screen.Security.route) },
            openAuth = openEditServiceAuth,
            initialAction = backStackEntry.arguments
                ?.getString(NavArg.EditServiceAction.name)
                ?.let { runCatching { EditServiceInitialAction.valueOf(it) }.getOrNull() }
                ?: EditServiceInitialAction.Details,
        )
    }
}

private const val HomeSlideDuration = 450
private const val HomeEnterFadeDuration = 83
private const val HomeEnterFadeDelay = 50
private const val HomeExitFadeDuration = 83
private const val HomeExitFadeDelay = 35

private val HomeSlideRoutes = setOf(
    Screen.Services.route,
    Screen.Settings.route,
    Screen.Groups.route,
    Screen.GroupEntries.route,
)

private fun AnimatedContentTransitionScope<NavBackStackEntry>.isHomeSlideTransition(): Boolean =
    initialState.destination.route in HomeSlideRoutes && targetState.destination.route in HomeSlideRoutes

private fun homeSlideEnter(
    slideDistancePx: Int,
): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition? = {
    if (isHomeSlideTransition()) {
        slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Left,
            animationSpec = tween(HomeSlideDuration, easing = HomeSlideEasing),
            initialOffset = { distance -> minOf(distance, slideDistancePx) },
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = HomeEnterFadeDuration,
                delayMillis = HomeEnterFadeDelay,
                easing = LinearEasing,
            ),
        )
    } else {
        null
    }
}

private fun homeSlideExit(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition? = {
    if (isHomeSlideTransition()) {
        ExitTransition.None
    } else {
        null
    }
}

private fun homeSlidePopEnter(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition? = {
    if (isHomeSlideTransition()) {
        EnterTransition.None
    } else {
        null
    }
}

private fun homeSlidePopExit(
    slideDistancePx: Int,
): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition? = {
    if (isHomeSlideTransition()) {
        slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Right,
            animationSpec = tween(HomeSlideDuration, easing = HomeSlideEasing),
            targetOffset = { distance -> minOf(distance, slideDistancePx) },
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = HomeExitFadeDuration,
                delayMillis = HomeExitFadeDelay,
                easing = LinearEasing,
            ),
        )
    } else {
        null
    }
}

private val HomeSlideInterpolator = PathInterpolator(
    Path().apply {
        moveTo(0f, 0f)
        cubicTo(0.05f, 0f, 0.133333f, 0.06f, 0.166666f, 0.4f)
        cubicTo(0.208333f, 0.82f, 0.25f, 1f, 1f, 1f)
    },
)

private val HomeSlideEasing = Easing { progress ->
    HomeSlideInterpolator.getInterpolation(progress)
}

enum class EditServiceInitialAction {
    Details,
    Icon,
    Qr,
}

interface HomeNavigationListener {
    fun openService(
        activity: Activity,
        serviceId: Long,
        initialAction: EditServiceInitialAction = EditServiceInitialAction.Details,
    )
    fun openExternalImport()
    fun openBrowserExt()
    fun openBrowserExtRequest(request: TokenRequest)
    fun openSecurity(activity: Activity)
    fun openBackup(turnOnBackup: Boolean)
    fun openAppSettings()
    fun openTrash()
    fun openNotifications()
    fun openAbout()
    fun openAddServiceModal()
    fun openFocusServiceModal(id: Long)
    fun openBackupImport(filePath: String?)
}
