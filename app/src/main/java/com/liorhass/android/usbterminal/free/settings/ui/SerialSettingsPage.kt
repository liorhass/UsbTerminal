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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.liorhass.android.usbterminal.free.R
import com.liorhass.android.usbterminal.free.main.MainViewModel
import com.liorhass.android.usbterminal.free.settings.model.SettingsRepository
import com.liorhass.android.usbterminal.free.settings.ui.lib.SettingsSingleChoice
import com.liorhass.android.usbterminal.free.settings.ui.lib.SettingsSwitch

@Composable
fun SerialSettingsPage(
    mainViewModel: MainViewModel,
    settingsData: SettingsRepository.SettingsData,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colors.surface) // todo: replace with surface https://developer.android.com/jetpack/compose/themes#content-color
    ) {

        // Baud rate
        SettingsSingleChoice(
            title = {Text(text= stringResource(R.string.baud_rate))},
            choices = SettingsRepository.BaudRateValues.preDefined.map { it.toString() },
            hasFreeInputField = true,
            freeInputFieldValue = settingsData.baudRateFreeInput.let{if (it == -1) "" else it.toString()},
            preSelectedIndex = mainViewModel.settingsRepository.indexOfBaudRate(settingsData.baudRate),
        ) { choiceIndex, choiceValue ->
            mainViewModel.settingsRepository.setBaudRate(choiceValue)
        }

        // Data bits
        SettingsSingleChoice(
            title = {Text(text=stringResource(R.string.data_bits))},
            choices = stringArrayResource(R.array.data_bits).toList(),
            preSelectedIndex = SettingsRepository.indexOfDataBits(settingsData.dataBits),
        ) { choiceIndex, choiceValue ->
            mainViewModel.settingsRepository.setDataBits(choiceValue)
        }

        // Stop bits
        SettingsSingleChoice(
            title = {Text(text=stringResource(R.string.stop_bits))},
            choices = stringArrayResource(R.array.stop_bits).toList(),
            preSelectedIndex = SettingsRepository.indexOfStopBits(settingsData.stopBits),
        ) { choiceIndex, choiceValue ->
            mainViewModel.settingsRepository.setStopBits(choiceIndex)
        }

        // Parity
        SettingsSingleChoice(
            title = {Text(text=stringResource(R.string.parity))},
            choices = stringArrayResource(R.array.parity).toList(),
            preSelectedIndex = SettingsRepository.indexOfParity(settingsData.parity),
        ) { choiceIndex, choiceValue ->
            mainViewModel.settingsRepository.setParity(choiceIndex)
        }

        // Set DTR on connect
        SettingsSwitch(
            title = {Text(text=stringResource(R.string.set_dtr_on_connect))},
            checked = settingsData.setDTRTrueOnConnect
        ) { checked ->
            mainViewModel.settingsRepository.setSetDTROnConnect(checked)
        }

        // Set RTS on connect
        SettingsSwitch(
            title = {Text(text=stringResource(R.string.set_rts_on_connect))},
            checked = settingsData.setRTSTrueOnConnect
        ) { checked ->
            mainViewModel.settingsRepository.setSetRTSOnConnect(checked)
        }
    }
}