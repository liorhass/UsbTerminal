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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.liorhass.android.usbterminal.free.R
import com.liorhass.android.usbterminal.free.main.MainViewModel
import com.liorhass.android.usbterminal.free.settings.model.SettingsRepository
import com.liorhass.android.usbterminal.free.settings.ui.lib.SettingsCheckbox
import com.liorhass.android.usbterminal.free.settings.ui.lib.SettingsFreeText
import com.liorhass.android.usbterminal.free.settings.ui.lib.SettingsSingleChoice
import com.liorhass.android.usbterminal.free.settings.ui.lib.SettingsSwitch

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

        // Text color
        val defaultTextColorDialogParams by mainViewModel.defaultTextColorDialogParams
        SettingsSingleChoice(
            title = { Text(text=stringResource(R.string.text_color)) },
            icon = {
                Icon(
                    painterResource(R.drawable.ic_baseline_color_lens_24),
                    contentDescription = stringResource(R.string.text_color),
                )
            },
            choices = stringArrayResource(R.array.text_colors).toList(),
            hasFreeInputField = true,
            freeInputFieldLabel = "RGB hex value e.g. ee6c00", //todo: string
            freeInputFieldValue = defaultTextColorDialogParams.freeTextInputField,//todo:2brm settingsData.defaultTextColorFreeInput.let{if (it == -1) "" else it.toString(16)}, //todo: string
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done),
            freeInputFieldIsValid = defaultTextColorDialogParams.isOk,
            preSelectedIndex = defaultTextColorDialogParams.preSelectedIndex,
            bottomBlockContent = {
                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(color = Color.Black),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = stringResource(id = defaultTextColorDialogParams.exampleText),
                        color = Color(defaultTextColorDialogParams.color).copy(alpha = 1f),
                        modifier = Modifier
                            .padding(start = 8.dp),
                    )
                }
            },
            onFreeInputFieldChange = {
                mainViewModel.onDefaultTextColorFreeTextChanged(it)
            }
        ) { choiceIndex, choiceValue ->
            mainViewModel.onDefaultTextColorSelected(choiceIndex, choiceValue)
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
            preSelectedIndex = settingsData.bytesSentByEnterKey,
        ) { choiceIndex, choiceValue ->
            mainViewModel.settingsRepository.setBytesSentByEnterKey(choiceIndex)
        }

        // Max number of bytes to retain in back-scroll buffer
        SettingsFreeText(
            title = {Text(text=stringResource(R.string.max_bytes_to_retain_for_back_scroll))},
            icon = {Icon(
                painterResource(R.drawable.ic_baseline_format_align_justify_24),
                contentDescription = stringResource(R.string.max_bytes_to_retain_for_back_scroll),
            )},
            label = {Text(text=stringResource(R.string.max_bytes_to_retain))},
            previousText = settingsData.maxBytesToRetainForBackScroll.toString(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
        ) { maxBytesToRetainStr ->
            val maxBytesToRetain = maxBytesToRetainStr.toIntOrNull() ?: 100_000
            mainViewModel.settingsRepository.setMaxBytesToRetainForBackScroll(maxBytesToRetain)
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

        // Silently drop unrecognized control chars
        SettingsCheckbox(
            title = {Text(text=stringResource(R.string.silently_drop_unrecognized_ctrl_chars))},
            replaceIconWithSameSpace = true,
            checked = settingsData.silentlyDropUnrecognizedCtrlChars
        ) { checked ->
            mainViewModel.settingsRepository.setSilentlyDropUnrecognizedCtrlChars(checked)
        }
    }
}