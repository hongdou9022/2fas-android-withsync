package com.twofasapp.cloudbackup.webdav.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twofasapp.cloudbackup.api.CloudBackupError
import com.twofasapp.core.design.feature.settings.SettingsSwitch
import com.twofasapp.core.design.foundation.button.Button
import com.twofasapp.core.design.foundation.button.OutlinedButton
import com.twofasapp.core.design.foundation.textfield.OutlinedTextField
import com.twofasapp.core.design.foundation.textfield.OutlinedTextFieldPassword
import com.twofasapp.core.design.foundation.topbar.TopAppBar
import com.twofasapp.core.design.ktx.strings
import com.twofasapp.core.design.ktx.toastShort
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun WebDavSettingsScreen(
    viewModel: WebDavSettingsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val strings = LocalContext.strings

    uiState.events.firstOrNull()?.let { event ->
        LaunchedEffect(event) {
            context.toastShort(
                when (event) {
                    WebDavSettingsUiEvent.TestSuccess -> strings.webDavTestSuccess
                    WebDavSettingsUiEvent.Saved -> strings.webDavSaved
                    WebDavSettingsUiEvent.RequiredFields -> strings.webDavRequiredFields
                    WebDavSettingsUiEvent.InvalidUrl -> strings.webDavInvalidUrl
                    is WebDavSettingsUiEvent.Failure -> when (event.error) {
                        CloudBackupError.Unauthorized -> strings.webDavUnauthorized
                        CloudBackupError.NetworkUnavailable -> strings.webDavNetworkError
                        else -> strings.webDavUnknownError
                    }
                },
            )
            viewModel.consumeEvent(event)
        }
    }

    Scaffold(
        topBar = { TopAppBar(titleText = strings.webDavTitle) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                OutlinedTextField(
                    value = uiState.config.baseUrl,
                    onValueChange = viewModel::updateBaseUrl,
                    modifier = FormFieldModifier,
                    labelText = strings.webDavBaseUrl,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    singleLine = true,
                )
            }
            item {
                OutlinedTextField(
                    value = uiState.config.username,
                    onValueChange = viewModel::updateUsername,
                    modifier = FormFieldModifier,
                    labelText = strings.webDavUsername,
                    singleLine = true,
                )
            }
            item {
                OutlinedTextFieldPassword(
                    value = uiState.config.password,
                    onValueChange = viewModel::updatePassword,
                    modifier = FormFieldModifier,
                    labelText = strings.webDavPassword,
                    singleLine = true,
                )
            }
            item {
                OutlinedTextField(
                    value = uiState.config.remoteDirectory,
                    onValueChange = viewModel::updateDirectory,
                    modifier = FormFieldModifier,
                    labelText = strings.webDavDirectory,
                    singleLine = true,
                )
            }
            item {
                SettingsSwitch(
                    title = strings.webDavEnabled,
                    subtitle = strings.webDavEnabledDescription,
                    checked = uiState.config.enabled,
                    enabled = uiState.testing.not(),
                    showEmptySpaceWhenIconMissing = false,
                    onCheckedChange = viewModel::updateEnabled,
                )
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    OutlinedButton(
                        text = strings.webDavTest,
                        onClick = viewModel::testConnection,
                        enabled = uiState.testing.not(),
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        text = strings.commonSave,
                        onClick = viewModel::save,
                        enabled = uiState.testing.not(),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

private val FormFieldModifier = Modifier
    .fillMaxWidth()
    .padding(horizontal = 24.dp)
