package com.twofasapp.feature.appsettings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import com.twofasapp.common.domain.SelectedTheme
import com.twofasapp.core.design.MdtIcons
import com.twofasapp.core.design.MdtTheme
import com.twofasapp.core.design.feature.settings.SettingsLink
import com.twofasapp.core.design.feature.settings.SettingsSwitch
import com.twofasapp.core.design.foundation.dialog.BaseDialog
import com.twofasapp.core.design.foundation.dialog.ConfirmDialog
import com.twofasapp.core.design.foundation.dialog.ListRadioDialog
import com.twofasapp.core.design.foundation.topbar.TopAppBar
import com.twofasapp.core.design.ktx.currentActivity
import com.twofasapp.data.session.domain.HomeUiMode
import com.twofasapp.data.session.domain.ServicesStyle
import com.twofasapp.locale.R
import com.twofasapp.locale.TwLocale
import org.koin.androidx.compose.koinViewModel
import java.util.Locale

@Composable
internal fun AppSettingsScreen(
    viewModel: AppSettingsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ScreenContent(
        uiState = uiState,
        onConsumeEvent = { viewModel.consumeEvent(it) },
        onSelectedThemeChange = { viewModel.setSelectedTheme(it) },
        onHomeUiModeChange = { viewModel.setHomeUiMode(it) },
        onServicesStyleChange = { viewModel.setServiceStyle(it) },
        onShowNextTokenToggle = { viewModel.toggleShowNextToken() },
        onShowBackupNoticeToggle = { viewModel.toggleShowBackupNotice() },
        onAutoFocusSearchToggle = { viewModel.toggleAutoFocusSearch() },
        onHideCodesToggle = { viewModel.toggleHideTokens() },
        onDynamicColorsToggle = { viewModel.toggleDynamicColors() },
        onCustomColorsDisable = { viewModel.disableCustomColors() },
        onCustomColorChange = { viewModel.setCustomColor(it) },
    )
}

@Composable
private fun ScreenContent(
    uiState: AppSettingsUiState,
    onConsumeEvent: (AppSettingsUiEvent) -> Unit,
    onSelectedThemeChange: (SelectedTheme) -> Unit,
    onHomeUiModeChange: (HomeUiMode) -> Unit,
    onServicesStyleChange: (ServicesStyle) -> Unit,
    onShowNextTokenToggle: () -> Unit,
    onShowBackupNoticeToggle: () -> Unit,
    onAutoFocusSearchToggle: () -> Unit,
    onHideCodesToggle: () -> Unit,
    onDynamicColorsToggle: () -> Unit,
    onCustomColorsDisable: () -> Unit,
    onCustomColorChange: (Long) -> Unit,
) {
    val activity = LocalContext.currentActivity
    var showThemeDialog by remember { mutableStateOf(false) }
    var showHomeUiModeDialog by remember { mutableStateOf(false) }
    var showServicesStyleDialog by remember { mutableStateOf(false) }
    var showConfirmDisableBackupNotice by remember { mutableStateOf(false) }
    var showCustomColorDialog by remember { mutableStateOf(false) }

    uiState.events.firstOrNull()?.let {
        onConsumeEvent(it)

        when (it) {
            AppSettingsUiEvent.Recreate -> activity.recreate()
        }
    }

    Scaffold(
        topBar = { TopAppBar(titleText = TwLocale.strings.settingsAppearance) },
    ) { padding ->

        LazyColumn(Modifier.padding(padding)) {
            item {
                SettingsLink(
                    title = TwLocale.strings.settingsHomeUiMode,
                    subtitle = uiState.appSettings.homeUiMode.toStringResource(),
                    icon = MdtIcons.Home,
                    onClick = { showHomeUiModeDialog = true },
                )
            }

            item {
                SettingsLink(
                    title = TwLocale.strings.settingsTheme,
                    subtitle = uiState.appSettings.selectedTheme.toStringResource(),
                    icon = MdtIcons.Theme,
                    onClick = { showThemeDialog = true },
                )
            }

            item {
                SettingsSwitch(
                    title = TwLocale.strings.settingsDynamicColors,
                    subtitle = TwLocale.strings.settingsDynamicColorsBody,
                    icon = MdtIcons.Theme,
                    checked = uiState.appSettings.dynamicColors,
                    onCheckedChange = { onDynamicColorsToggle() },
                )
            }

            item {
                SettingsSwitch(
                    title = TwLocale.strings.settingsCustomColors,
                    subtitle = TwLocale.strings.settingsCustomColorsBody,
                    icon = MdtIcons.Theme,
                    checked = uiState.appSettings.customColors,
                    onCheckedChange = { checked ->
                        if (checked) {
                            showCustomColorDialog = true
                        } else {
                            onCustomColorsDisable()
                        }
                    },
                )
            }

            item {
                SettingsLink(
                    title = TwLocale.strings.settingsCustomColor,
                    subtitle = "${TwLocale.strings.settingsCustomColorBody} ${uiState.appSettings.customColor.toHexColor()}",
                    icon = MdtIcons.Theme,
                    endContent = {
                        ColorSwatch(
                            color = Color(uiState.appSettings.customColor.toInt()),
                            modifier = Modifier.size(28.dp),
                        )
                    },
                    onClick = { showCustomColorDialog = true },
                )
            }

            item {
                SettingsLink(
                    title = TwLocale.strings.settingsServicesStyle,
                    subtitle = uiState.appSettings.servicesStyle.toStringResource(),
                    icon = MdtIcons.ListStyle,
                    onClick = { showServicesStyleDialog = true },
                )
            }

            item {
                SettingsSwitch(
                    title = TwLocale.strings.settingsShowNextCode,
                    subtitle = TwLocale.strings.settingsShowNextCodeBody,
                    icon = MdtIcons.NextToken,
                    checked = uiState.appSettings.showNextCode,
                    onCheckedChange = { onShowNextTokenToggle() },
                )
            }

            item {
                SettingsSwitch(
                    title = TwLocale.strings.settingsAutoFocusSearch,
                    subtitle = TwLocale.strings.settingsAutoFocusSearchBody,
                    icon = MdtIcons.Search,
                    checked = uiState.appSettings.autoFocusSearch,
                    onCheckedChange = { onAutoFocusSearchToggle() },
                )
            }

            item {
                SettingsSwitch(
                    title = TwLocale.strings.settingsShowBackupNotice,
                    icon = MdtIcons.CloudOff,
                    checked = uiState.appSettings.showBackupNotice,
                    onCheckedChange = { checked ->
                        if (checked.not()) {
                            showConfirmDisableBackupNotice = true
                        } else {
                            onShowBackupNoticeToggle()
                        }
                    },
                )
            }

            item {
                SettingsSwitch(
                    title = TwLocale.strings.settingsHideCodes,
                    subtitle = TwLocale.strings.settingsHideCodesBody,
                    icon = MdtIcons.Eye,
                    checked = uiState.appSettings.hideCodes,
                    onCheckedChange = { onHideCodesToggle() },
                )
            }
        }

        if (showThemeDialog) {
            ListRadioDialog(
                onDismissRequest = { showThemeDialog = false },
                title = TwLocale.strings.settingsTheme,
                options = SelectedTheme.values().map { it.toStringResource() },
                selectedOption = uiState.appSettings.selectedTheme.toStringResource(),
                onOptionSelected = { index, _ -> onSelectedThemeChange(SelectedTheme.values()[index]) },
            )
        }

        if (showServicesStyleDialog) {
            ListRadioDialog(
                onDismissRequest = { showServicesStyleDialog = false },
                title = TwLocale.strings.settingsServicesStyle,
                options = ServicesStyle.values().map { it.toStringResource() },
                selectedOption = uiState.appSettings.servicesStyle.toStringResource(),
                onOptionSelected = { index, _ -> onServicesStyleChange(ServicesStyle.values()[index]) },
            )
        }

        if (showHomeUiModeDialog) {
            ListRadioDialog(
                onDismissRequest = { showHomeUiModeDialog = false },
                title = TwLocale.strings.settingsHomeUiMode,
                options = HomeUiMode.values().map { it.toStringResource() },
                selectedOption = uiState.appSettings.homeUiMode.toStringResource(),
                onOptionSelected = { index, _ -> onHomeUiModeChange(HomeUiMode.values()[index]) },
            )
        }

        if (showConfirmDisableBackupNotice) {
            ConfirmDialog(
                onDismissRequest = { showConfirmDisableBackupNotice = false },
                title = TwLocale.strings.settingsShowBackupNotice,
                body = TwLocale.strings.settingsShowBackupNoticeConfirmBody,
                onPositive = { onShowBackupNoticeToggle() },
            )
        }

        if (showCustomColorDialog) {
            CustomColorPickerDialog(
                initialColor = Color(uiState.appSettings.customColor.toInt()),
                onDismissRequest = { showCustomColorDialog = false },
                onColorSelected = onCustomColorChange,
            )
        }
    }
}

@Composable
private fun CustomColorPickerDialog(
    initialColor: Color,
    onDismissRequest: () -> Unit,
    onColorSelected: (Long) -> Unit,
) {
    val controller = rememberColorPickerController()
    var selectedColor by remember(initialColor) { mutableStateOf(initialColor) }

    BaseDialog(
        onDismissRequest = onDismissRequest,
        title = TwLocale.strings.settingsCustomColor,
        positive = TwLocale.strings.commonSave,
        negative = TwLocale.strings.commonCancel,
        onPositiveClick = { onColorSelected(selectedColor.toStoredArgb()) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            HsvColorPicker(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                controller = controller,
                initialColor = initialColor,
                onColorChanged = { selectedColor = it.color.copy(alpha = 1f) },
            )

            BrightnessSlider(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp),
                controller = controller,
                borderColor = MdtTheme.color.divider,
                initialColor = initialColor,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ColorSwatch(
                    color = selectedColor,
                    modifier = Modifier.size(32.dp),
                )
                Text(
                    text = selectedColor.toStoredArgb().toHexColor(),
                    style = MdtTheme.typo.body1,
                    color = MdtTheme.color.onSurfacePrimary,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun ColorSwatch(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(color)
            .border(1.dp, MdtTheme.color.divider, CircleShape),
    )
}

private fun Color.toStoredArgb(): Long = toArgb().toLong() and 0xFFFFFFFFL

private fun Long.toHexColor(): String = String.format(Locale.US, "#%06X", this and 0xFFFFFFL)

@Composable
private fun SelectedTheme.toStringResource(): String {
    return when (this) {
        SelectedTheme.Auto -> stringResource(id = R.string.settings__theme_option_auto)
        SelectedTheme.Light -> stringResource(id = R.string.settings__theme_option_light)
        SelectedTheme.Dark -> stringResource(id = R.string.settings__theme_option_dark)
    }
}

@Composable
private fun ServicesStyle.toStringResource(): String {
    return when (this) {
        ServicesStyle.Default -> stringResource(id = R.string.settings__list_style_option_default)
        ServicesStyle.Compact -> stringResource(id = R.string.settings__list_style_option_compact)
    }
}

@Composable
private fun HomeUiMode.toStringResource(): String {
    return when (this) {
        HomeUiMode.Classic -> stringResource(id = R.string.settings__home_ui_mode_classic)
        HomeUiMode.Refreshed -> stringResource(id = R.string.settings__home_ui_mode_refreshed)
    }
}
