package com.twofasapp.feature.about.ui.about

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ShareCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twofasapp.core.design.MdtIcons
import com.twofasapp.core.design.MdtTheme
import com.twofasapp.core.design.feature.settings.SettingsDivider
import com.twofasapp.core.design.feature.settings.SettingsHeader
import com.twofasapp.core.design.feature.settings.SettingsLink
import com.twofasapp.core.design.foundation.topbar.TopAppBar
import com.twofasapp.core.design.ktx.openSafely
import com.twofasapp.locale.R
import com.twofasapp.locale.TwLocale
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun AboutScreen(
    viewModel: AboutViewModel = koinViewModel(),
    openLicenses: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ScreenContent(
        uiState = uiState,
        onLicensesClick = openLicenses,
        onReviewClick = { viewModel.reviewDone() },
    )
}

@Composable
private fun ScreenContent(
    uiState: AboutUiState,
    onLicensesClick: () -> Unit,
    onReviewClick: () -> Unit,
) {
    val activity = LocalContext.current as Activity
    val uriHandler = LocalUriHandler.current
    val shareText = TwLocale.strings.aboutTellFriendShareText

    Scaffold(
        topBar = { TopAppBar(titleText = TwLocale.strings.aboutTitle) },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            LazyColumn(modifier = Modifier.weight(1f)) {
                item { SettingsHeader(title = TwLocale.strings.aboutGeneral) }

                item {
                    SettingsLink(title = TwLocale.strings.aboutWriteReview, icon = MdtIcons.Write, external = true) {
                        onReviewClick()
                        uriHandler.openSafely(TwLocale.links.playStore, activity)
                    }
                }

                item {
                    SettingsLink(title = TwLocale.strings.aboutPrivacyPolicy, icon = MdtIcons.LockOpen, external = true) {
                        uriHandler.openSafely(TwLocale.links.privacyPolicy, activity)
                    }
                }

                item {
                    SettingsLink(title = TwLocale.strings.aboutTerms, icon = MdtIcons.Terms, external = true) {
                        uriHandler.openSafely(TwLocale.links.terms, activity)
                    }
                }

                item {
                    SettingsLink(title = TwLocale.strings.aboutLicenses, icon = MdtIcons.Licenses) {
                        onLicensesClick()
                    }
                }

                item { SettingsDivider() }

                item { SettingsHeader(title = TwLocale.strings.aboutRepositories) }

                item {
                    SettingsLink(
                        title = TwLocale.strings.aboutProjectRepository,
                        image = painterResource(id = com.twofasapp.core.design.R.drawable.ic_github),
                        external = true,
                    ) {
                        uriHandler.openSafely(TwLocale.links.repository, activity)
                    }
                }

                item {
                    SettingsLink(
                        title = TwLocale.strings.aboutOfficialRepository,
                        image = painterResource(id = com.twofasapp.core.design.R.drawable.ic_github),
                        external = true,
                    ) {
                        uriHandler.openSafely(TwLocale.links.officialRepository, activity)
                    }
                }

                item { SettingsDivider() }

                item { SettingsHeader(title = TwLocale.strings.aboutSupportAndShare) }

                item {
                    SettingsLink(
                        title = TwLocale.strings.settingsSupport,
                        icon = MdtIcons.Support,
                        external = true,
                    ) {
                        uriHandler.openSafely(TwLocale.links.support, activity)
                    }
                }

                item {
                    SettingsLink(
                        title = TwLocale.strings.settingsDonate,
                        icon = MdtIcons.Favorite,
                        external = true,
                    ) {
                        uriHandler.openSafely(TwLocale.links.donate, activity)
                    }
                }

                item {
                    SettingsLink(title = TwLocale.strings.aboutTellFriend, icon = MdtIcons.Share) {
                        ShareCompat.IntentBuilder(activity)
                            .setType("text/plain")
                            .setChooserTitle("Share 2FAS")
                            .setText(shareText)
                            .startChooser()
                    }
                }

                item { SettingsDivider() }

                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                            .padding(start = 24.dp, end = 16.dp),
                    ) {
                        Text(
                            text = stringResource(id = R.string.settings__version, uiState.versionName),
                            color = MdtTheme.color.onSurfaceSecondary,
                            style = MdtTheme.typo.body3,
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Image(
                            painter = painterResource(id = com.twofasapp.core.design.R.drawable.logo_2fas),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }
    }
}
