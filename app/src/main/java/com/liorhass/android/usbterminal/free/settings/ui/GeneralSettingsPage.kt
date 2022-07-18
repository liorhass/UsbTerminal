// Copyright 2022 Lior Hass
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.liorhass.android.usbterminal.free.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.liorhass.android.usbterminal.free.R
import com.liorhass.android.usbterminal.free.main.MainViewModel
import com.liorhass.android.usbterminal.free.settings.model.SettingsRepository
import com.liorhass.android.usbterminal.free.settings.ui.lib.SettingsFreeText
import com.liorhass.android.usbterminal.free.settings.ui.lib.SettingsSingleChoice
import com.liorhass.android.usbterminal.free.settings.ui.lib.SettingsSwitch
import com.liorhass.android.usbterminal.free.ui.util.GeneralDialog

@Composable
fun GeneralSettingsPage(
    mainViewModel: MainViewModel,
    settingsData: SettingsRepository.SettingsData,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colors.surface) // todo: replace with surface https://developer.android.com/jetpack/compose/themes#content-color
    ) {

        // Theme (dark/light)
        SettingsSingleChoice(
            title = { Text(text = stringResource(R.string.theme)) },
            icon = {
                Icon(
                    painterResource(R.drawable.ic_baseline_brightness_medium_24),
                    contentDescription = stringResource(R.string.theme),
                )
            },
            choices = listOf(
                stringResource(R.string.light),
                stringResource(R.string.dark),
                stringResource(R.string.same_as_system),
            ),
            hasFreeInputField = false,
            preSelectedIndex = settingsData.themeType
        ) { choiceIndex, choiceValue ->
            mainViewModel.settingsRepository.setThemeType(choiceIndex)
        }

        // Log session data to file
        SettingsSwitch(
            title = {Text(text= stringResource(R.string.log_session_data_to_file))},
            icon = {Icon(
                painterResource(R.drawable.ic_baseline_history_edu_24),
                contentDescription = stringResource(R.string.log_session_data_to_file),
            )},
            checked = settingsData.logSessionDataToFile
        ) { checked ->
            mainViewModel.settingsRepository.setLogSessionDataToFile(checked)
        }

        // Also log outgoing data
        SettingsSwitch(
            title = {Text(text=stringResource(R.string.also_log_outgoing_data))},
            checked = settingsData.alsoLogOutgoingData,
            enabled = settingsData.logSessionDataToFile,
            replaceIconWithSameSpace = true,
        ) { checked ->
            mainViewModel.settingsRepository.setAlsoLogOutgoingData(checked)
        }

        // Mark logged outgoing data
        SettingsSwitch(
            title = {Text(text=stringResource(R.string.mark_logged_outgoing_data))},
            checked = settingsData.markLoggedOutgoingData,
            enabled = (settingsData.logSessionDataToFile && settingsData.alsoLogOutgoingData),
            replaceIconWithSameSpace = true,
        ) { checked ->
            mainViewModel.settingsRepository.setMarkLoggedOutgoingData(checked)
        }

        // Zip log files when sharing
        SettingsSwitch(
            title = {Text(text=stringResource(R.string.zip_log_files_when_sharing))},
            checked = settingsData.zipLogFilesWhenSharing,
            enabled = (settingsData.logSessionDataToFile),
            icon = {Icon(
                painterResource(R.drawable.folder_zip_outline),
                contentDescription = stringResource(R.string.zip_log_files_when_sharing),
            )},
        ) { checked ->
            mainViewModel.settingsRepository.setZipLogFilesWhenSharing(checked)
        }

        // Work Also In Background
        var shouldDisplayWorkInBGDialog by remember { mutableStateOf(false) }
        SettingsSwitch(
            title = {Text(text=stringResource(R.string.work_also_in_the_background))},
            icon = {Icon(
                painterResource(R.drawable.ic_baseline_content_copy_24),
                contentDescription = stringResource(R.string.work_also_in_the_background),
            )},
            checked = settingsData.workAlsoInBackground
        ) { checked ->
            if (checked) {
                shouldDisplayWorkInBGDialog = true
            } else {
                mainViewModel.settingsRepository.setWorkAlsoInBackground(false)
            }
        }
        if (shouldDisplayWorkInBGDialog) {
            GeneralDialog(
                titleText = stringResource(R.string.work_also_in_the_background),
                onPositiveText = stringResource(R.string.ok),
                onPositiveClick = {
                    shouldDisplayWorkInBGDialog = false
                    mainViewModel.settingsRepository.setWorkAlsoInBackground(true)
                },
                onDismissText = stringResource(R.string.cancel_all_caps),
                onDismissClick = { shouldDisplayWorkInBGDialog = false }
            ) {
                Text(
                    modifier = Modifier.padding(20.dp),
                    text = stringResource(R.string.work_also_in_the_background_explanation),
                )
            }
        }

        // Connect to USB device when started
        SettingsSwitch(
            title = {Text(text= stringResource(R.string.connect_to_usb_device_on_start))},
            icon = {Icon(
                painterResource(R.drawable.ic_baseline_compare_arrows_24),
                contentDescription = stringResource(R.string.connect_to_usb_device_on_start),
            )},
            checked = settingsData.connectToDeviceOnStart
        ) { checked ->
            mainViewModel.settingsRepository.setConnectToDeviceOnStart(checked)
        }

        // Email address(s) for sharing
        SettingsFreeText(
            title = {Text(text=stringResource(R.string.default_sharing_email_addresses))},
            icon = {Icon(
                painterResource(R.drawable.ic_baseline_mail_outline_24),
                contentDescription = stringResource(R.string.default_sharing_email_addresses),
            )},
            label = {Text(text=stringResource(R.string.email_address))},
            previousText = settingsData.emailAddressForSharing,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
        ) { emailAddress ->
            mainViewModel.settingsRepository.setEmailAddressForSharing(emailAddress)
        }
    }
}