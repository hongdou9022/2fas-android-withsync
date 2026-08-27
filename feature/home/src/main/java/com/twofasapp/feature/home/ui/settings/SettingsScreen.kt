package com.twofasapp.feature.home.ui.settings

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twofasapp.core.design.MdtIcons
import com.twofasapp.core.design.MdtTheme
import com.twofasapp.core.design.feature.settings.SettingsDivider
import com.twofasapp.core.design.feature.settings.SettingsLink
import com.twofasapp.core.design.foundation.topbar.TopAppBar
import com.twofasapp.data.session.domain.HomeUiMode
import com.twofasapp.feature.home.navigation.HomeNavigationListener
import com.twofasapp.feature.home.ui.bottombar.BottomBar
import com.twofasapp.feature.home.ui.bottombar.BottomBarListener
import com.twofasapp.locale.TwLocale
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun SettingsRoute(
    listener: HomeNavigationListener,
    bottomBarListener: BottomBarListener,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val homeUiMode = viewModel.homeUiMode.collectAsStateWithLifecycle().value

    SettingsScreen(
        listener = listener,
        bottomBarListener = bottomBarListener,
        refreshed = homeUiMode == HomeUiMode.Refreshed,
    )
}

@Composable
private fun SettingsScreen(
    listener: HomeNavigationListener,
    bottomBarListener: BottomBarListener,
    refreshed: Boolean,
) {
    val activity = LocalContext.current as Activity

    BackHandler(enabled = refreshed) { bottomBarListener.openHome() }

    Scaffold(
        bottomBar = {
            if (refreshed.not()) BottomBar(1, bottomBarListener)
        },
        topBar = {
            TopAppBar(
                titleText = TwLocale.strings.settingsSettings,
                showBackButton = refreshed,
                onBackClick = bottomBarListener::openHome,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MdtTheme.color.background)
                .padding(padding),
        ) {
            item {
                SettingsLink(title = TwLocale.strings.settingsBackup, icon = MdtIcons.CloudUpload) {
                    listener.openBackup(false)
                }
            }

            item {
                SettingsLink(title = TwLocale.strings.settingsSecurity, icon = MdtIcons.Security) {
                    listener.openSecurity(activity)
                }
            }

            item {
                SettingsLink(title = TwLocale.strings.settingsAppearance, icon = MdtIcons.Eye) {
                    listener.openAppSettings()
                }
            }

            item {
                SettingsLink(title = TwLocale.strings.settingsExternalImport, icon = MdtIcons.Download) {
                    listener.openExternalImport()
                }
            }

            item {
                SettingsLink(title = TwLocale.strings.settingsBrowserExt, icon = MdtIcons.Extension) {
                    listener.openBrowserExt()
                }
            }

            item { SettingsDivider() }

            item {
                SettingsLink(title = TwLocale.strings.settingsTrash, icon = MdtIcons.Delete) {
                    listener.openTrash()
                }
            }

            item {
                SettingsLink(title = TwLocale.strings.settingsAbout, icon = MdtIcons.Info) {
                    listener.openAbout()
                }
            }
        }
    }
}
