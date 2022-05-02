// Copyright 2022 Lior Hass
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.liorhass.android.usbterminal.free.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.liorhass.android.usbterminal.free.R
import com.liorhass.android.usbterminal.free.main.MainViewModel
import com.liorhass.android.usbterminal.free.settings.model.SettingsRepository
import com.liorhass.android.usbterminal.free.settings.ui.lib.SettingsFreeText
import com.liorhass.android.usbterminal.free.settings.ui.lib.SettingsSingleChoice
import com.liorhass.android.usbterminal.free.settings.ui.lib.SettingsSwitch

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
            checked = settingsData.logSessionDataToFile
        ) { checked ->
            mainViewModel.settingsRepository.setLogSessionDataToFile(checked)
        }

        // Also log outgoing data
        SettingsSwitch(
            title = {Text(text=stringResource(R.string.also_log_outgoing_data))},
            checked = settingsData.alsoLogOutgoingData,
            enabled = settingsData.logSessionDataToFile,
        ) { checked ->
            mainViewModel.settingsRepository.setAlsoLogOutgoingData(checked)
        }

        // Mark logged outgoing data
        SettingsSwitch(
            title = {Text(text=stringResource(R.string.mark_logged_outgoing_data))},
            checked = settingsData.markLoggedOutgoingData,
            enabled = (settingsData.logSessionDataToFile && settingsData.alsoLogOutgoingData),
        ) { checked ->
            mainViewModel.settingsRepository.setMarkLoggedOutgoingData(checked)
        }

        // Max number of bytes to retain in back-scroll buffer
        SettingsFreeText(
            title = {Text(text=stringResource(R.string.max_bytes_to_retain_for_back_scroll))},
            label = {Text(text=stringResource(R.string.max_bytes_to_retain))},
            previousText = settingsData.maxBytesToRetainForBackScroll.toString(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
        ) { maxBytesToRetainStr ->
            val maxBytesToRetain = maxBytesToRetainStr.toIntOrNull() ?: 100_000
            mainViewModel.settingsRepository.setMaxBytesToRetainForBackScroll(maxBytesToRetain)
        }

        // Email address(s) for sharing
        SettingsFreeText(
            title = {Text(text=stringResource(R.string.default_sharing_email_addresses))},
            label = {Text(text=stringResource(R.string.email_address))},
            previousText = settingsData.emailAddressForSharing,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
        ) { emailAddress ->
            mainViewModel.settingsRepository.setEmailAddressForSharing(emailAddress)
        }
    }
}