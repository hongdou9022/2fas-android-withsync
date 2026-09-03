package com.twofasapp.data.session.domain

import com.twofasapp.common.domain.SelectedTheme

data class AppSettings(
    val showNextCode: Boolean = false,
    val autoFocusSearch: Boolean = false,
    val showBackupNotice: Boolean = false,
    val sendCrashLogs: Boolean = false,
    val allowScreenshots: Boolean = false,
    val selectedTheme: SelectedTheme = SelectedTheme.Auto,
    val homeUiMode: HomeUiMode = HomeUiMode.Classic,
    val servicesStyle: ServicesStyle = ServicesStyle.Default,
    val servicesSort: ServicesSort = ServicesSort.Manual,
    val hideCodes: Boolean = false,
    val skipBrowserRequestAuth: Boolean = false,
    val dynamicColors: Boolean = false,
    val customColors: Boolean = false,
    val customColor: Long = DefaultCustomColor,
)

const val DefaultCustomColor = 0xFFED1C24L
