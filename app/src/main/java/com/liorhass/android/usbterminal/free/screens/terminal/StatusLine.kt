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
package com.liorhass.android.usbterminal.free.screens.terminal

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liorhass.android.usbterminal.free.R
import com.liorhass.android.usbterminal.free.main.MainViewModel
import com.liorhass.android.usbterminal.free.main.ScreenTextModel
import com.liorhass.android.usbterminal.free.settings.model.SettingsRepository
import com.liorhass.android.usbterminal.free.ui.theme.LedColorWhenConnected
import com.liorhass.android.usbterminal.free.ui.theme.LedColorWhenDisconnected
import com.liorhass.android.usbterminal.free.ui.theme.LedColorWhenError
import com.liorhass.android.usbterminal.free.ui.theme.UsbTerminalTheme
import com.liorhass.android.usbterminal.free.usbserial.UsbSerialPort

@Composable
fun StatusLine(
    usbConnectionState: MainViewModel.UsbConnectionState,
    screenDimensions: MainViewModel.ScreenDimensions,
    cursorPosition: ScreenTextModel.DisplayedCursorPosition,
    displayType: SettingsRepository.DisplayType,
) {
    val scroll = rememberScrollState()

    val connectionStatusLEDColor = when (usbConnectionState.statusCode) {
        UsbSerialPort.ConnectStatusCode.CONNECTED -> LedColorWhenConnected
        UsbSerialPort.ConnectStatusCode.IDLE -> LedColorWhenDisconnected
        else -> LedColorWhenError
    }
    val connectionStatusMsg = usbConnectionState.msg.ifBlank {
        when (usbConnectionState.statusCode) {
            UsbSerialPort.ConnectStatusCode.CONNECTED -> stringResource(R.string.connected)
            UsbSerialPort.ConnectStatusCode.IDLE -> stringResource(R.string.disconnected)
            else -> usbConnectionState.statusCode.name
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = UsbTerminalTheme.extendedColors.statusLineBackgroundColor)
            .padding(1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_baseline_wb_sunny_24),
            contentDescription = "",
            colorFilter = ColorFilter.tint(connectionStatusLEDColor),
            modifier = Modifier
                .padding(start = 2.dp),
        )
        Text(
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp, end = 10.dp)
                .horizontalScroll(scroll),
            text = connectionStatusMsg,
            color = UsbTerminalTheme.extendedColors.statusLineTextColor,
        )
        if (displayType == SettingsRepository.DisplayType.TEXT) {
            // Show cursor position (only on text screen)
            Text(
                modifier = Modifier
                    .padding(end = 6.dp),
                text = "${cursorPosition.line}:${cursorPosition.column}",
                fontSize = 14.sp,
                color = UsbTerminalTheme.extendedColors.statusLineTextColor,
            )
            // Show screen dimensions WxH (only on text screen)
            Text(
                modifier = Modifier
                    .padding(end = 6.dp),
                text = "${screenDimensions.height}x${screenDimensions.width}",
                fontSize = 14.sp,
                color = UsbTerminalTheme.extendedColors.statusLineTextColor,
            )
        }
        // TXT or HEX indicator
        Text(
            modifier = Modifier
                .padding(end = 4.dp),
            text = when(displayType) {
                SettingsRepository.DisplayType.TEXT -> stringResource(R.string.display_type_indicator_txt)
                SettingsRepository.DisplayType.HEX -> stringResource(R.string.display_type_indicator_hex)
            },
            fontSize = 14.sp,
            color = UsbTerminalTheme.extendedColors.statusLineTextColor,
        )
    }
}

