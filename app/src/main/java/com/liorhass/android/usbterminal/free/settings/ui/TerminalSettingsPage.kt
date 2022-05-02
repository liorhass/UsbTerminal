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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.liorhass.android.usbterminal.free.R
import com.liorhass.android.usbterminal.free.main.MainViewModel
import com.liorhass.android.usbterminal.free.settings.model.SettingsRepository
import com.liorhass.android.usbterminal.free.settings.ui.lib.SettingsSingleChoice
import com.liorhass.android.usbterminal.free.settings.ui.lib.SettingsSwitch
import com.liorhass.android.usbterminal.free.ui.util.GeneralDialog

@Composable
fun TerminalSettingsPage(
    mainViewModel: MainViewModel,
    settingsData: SettingsRepository.SettingsData,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colors.surface) // todo: replace with surface https://developer.android.com/jetpack/compose/themes#content-color
    ) {

        // Input-mode (Auto/TextField)
        SettingsSingleChoice(
            title = { Text(text = stringResource(R.string.kb_input_mode)) },
            icon = {
                Icon(
                    painterResource(R.drawable.ic_baseline_keyboard_24),
                    contentDescription = stringResource(R.string.kb_input_mode),
                )
            },
            choices = listOf(
                stringResource(R.string.auto),
                stringResource(R.string.complete_line)
            ),
            hasFreeInputField = false,
            preSelectedIndex = settingsData.inputMode,
        ) { choiceIndex, choiceValue ->
            mainViewModel.settingsRepository.setInputMode(choiceIndex)
        }

        // Send input line on enter key
        SettingsSwitch(
            title = {Text(text=stringResource(R.string.enter_key_sends_line))},
            icon = {
                Icon(
                    painterResource(R.drawable.ic_baseline_send_24),
                    contentDescription = stringResource(R.string.enter_key_sends_line),
                )
            },
            checked = settingsData.sendInputLineOnEnterKey,
            enabled = settingsData.inputMode == SettingsRepository.InputMode.WHOLE_LINE,
        ) { checked ->
            mainViewModel.settingsRepository.setSendInputLineOnEnterKey(checked)
        }

        // Font size
        SettingsSingleChoice(
            title = { Text(text=stringResource(R.string.font_size)) },
            icon = {
                Icon(
                    painterResource(R.drawable.ic_baseline_format_size_24),
                    contentDescription = stringResource(R.string.font_size),
                )
            },
            choices = stringArrayResource(R.array.font_size).toList(),
            preSelectedIndex = SettingsRepository.indexOfFontSize(settingsData.fontSize),
        ) { choiceIndex, choiceValue ->
            mainViewModel.settingsRepository.setFontSize(choiceValue)
        }


        // Bytes-sent-by-enter-key (CR_LF/LF/CR)
        SettingsSingleChoice(
            title = { Text(text = stringResource(R.string.enter_key_sends)) },
            icon = {
                Icon(
                    painterResource(R.drawable.ic_baseline_keyboard_return_24),
                    contentDescription = stringResource(R.string.enter_key_sends),
                )
            },
            choices = listOf(
                stringResource(R.string.cr),
                stringResource(R.string.lf),
                stringResource(R.string.cr_lf),
            ),
            hasFreeInputField = false,
            preSelectedIndex = settingsData.bytesSentByEnterKey,
        ) { choiceIndex, choiceValue ->
            mainViewModel.settingsRepository.setBytesSentByEnterKey(choiceIndex)
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

        // Loopback
        SettingsSwitch(
            title = {Text(text=stringResource(R.string.local_echo))},
            icon = {
                Icon(
                    painterResource(R.drawable.ic_baseline_replay_24),
                    contentDescription = stringResource(R.string.local_echo),
                )
            },
            checked = settingsData.loopBack
        ) { checked ->
            mainViewModel.settingsRepository.setLoopBack(checked)
        }

        // Sound
        SettingsSwitch(
            title = {Text(text=stringResource(R.string.sound_on))},
            icon = {
                Icon(
                    painterResource(R.drawable.ic_baseline_volume_up_24),
                    contentDescription = stringResource(R.string.sound_on),
                )
            },
            checked = settingsData.soundOn
        ) { checked ->
            mainViewModel.settingsRepository.setSoundOn(checked)
        }
    }
}