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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.liorhass.android.usbterminal.free.R
import com.liorhass.android.usbterminal.free.main.MainViewModel
import com.liorhass.android.usbterminal.free.usbserial.UsbSerialPort
import kotlinx.coroutines.launch

@Composable
fun TerminalScreenTopAppBarActions(mainViewModel: MainViewModel) {

    val usbConnectionState by mainViewModel.usbConnectionState
    val showOverflowMenu = remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Look at https://android--code.blogspot.com/2021/03/jetpack-compose-how-to-use-topappbar.html

    IconButton(
        onClick = { coroutineScope.launch { mainViewModel.clearScreen() } },
    ) {
        Icon(
            modifier = Modifier.padding(start = 0.dp, end = 0.dp),
            painter = painterResource(id = R.drawable.ic_baseline_delete_24),
            contentDescription = stringResource(R.string.clear_screen)
        )
    }
    IconButton(
        onClick = { mainViewModel.onToggleHexTxtButtonClick() },
    ) {
        Icon(
            modifier = Modifier.padding(start = 0.dp, end = 0.dp),
            painter = painterResource(id = R.drawable.ic_baseline_txt_hex_24),
            contentDescription = stringResource(R.string.toggle_hex_text)
        )
    }
    IconButton(
        onClick = { mainViewModel.onToggleShowCtrlButtonsRowButtonClick() },
    ) {
        Icon(
            modifier = Modifier.padding(start = 0.dp, end = 0.dp),
            painter = painterResource(id = R.drawable.ic_baseline_open_with_24),
            contentDescription = stringResource(R.string.arrows_keypad)
        )
    }

    // Dropdown menu (overflow menu)
    Box {
        IconButton(
            onClick = { showOverflowMenu.value = !showOverflowMenu.value }
        ) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.more)
            )
        }
        DropdownMenu(
            expanded = showOverflowMenu.value,
            onDismissRequest = { showOverflowMenu.value = false },
            offset = DpOffset((0).dp, (-60).dp),
        ) {

            // DTR
            DropdownMenuItem(
                onClick = { mainViewModel.setDTR(true); showOverflowMenu.value = false },
                enabled = usbConnectionState.connectedUsbPort?.getDTR() == false,
            ) {
                Text(
                    text = stringResource(R.string.set_dtr),
                    modifier = Modifier
                        .clickable { mainViewModel.setDTR(true); showOverflowMenu.value = false }
                )
            }
            DropdownMenuItem(
                onClick = { mainViewModel.setDTR(false); showOverflowMenu.value = false },
                enabled = usbConnectionState.connectedUsbPort?.getDTR() == true,
            ) {
                Text(
                    text = stringResource(R.string.clear_dtr),
                    modifier = Modifier
                        .clickable { mainViewModel.setDTR(false); showOverflowMenu.value = false }
                )
            }

            // RTS
            DropdownMenuItem(
                onClick = { mainViewModel.setRTS(true); showOverflowMenu.value = false },
                enabled = usbConnectionState.connectedUsbPort?.getRTS() == false,
            ) {
                Text(
                    text = stringResource(R.string.set_rts),
                    modifier = Modifier
                        .clickable { mainViewModel.setRTS(true); showOverflowMenu.value = false }
                )
            }
            DropdownMenuItem(
                onClick = { mainViewModel.setRTS(false); showOverflowMenu.value = false },
                enabled = usbConnectionState.connectedUsbPort?.getRTS() == true,
            ) {
                Text(
                    text = stringResource(R.string.clear_rts),
                    modifier = Modifier
                        .clickable { mainViewModel.setRTS(false); showOverflowMenu.value = false }
                )
            }

            // ESP32 boot-reset
            DropdownMenuItem(
                onClick = { mainViewModel.esp32BootloaderReset(); showOverflowMenu.value = false },
                enabled = usbConnectionState.statusCode == UsbSerialPort.ConnectStatusCode.CONNECTED
            ) {
                Text(
                    text = stringResource(R.string.esp32_boot_reset),
                    modifier = Modifier
                        .clickable {
                            mainViewModel.esp32BootloaderReset(); showOverflowMenu.value = false
                        }
                )
            }

            // Arduino reset
            DropdownMenuItem(
                onClick = { mainViewModel.arduinoReset(); showOverflowMenu.value = false },
                enabled = usbConnectionState.statusCode == UsbSerialPort.ConnectStatusCode.CONNECTED
            ) {
                Text(
                    text = stringResource(R.string.arduino_reset),
                    modifier = Modifier
                        .clickable {
                            mainViewModel.arduinoReset(); showOverflowMenu.value = false
                        }
                )
            }

            // Debug section
            DropdownMenuDebugSection(showOverflowMenu, mainViewModel)
        }
    }
}