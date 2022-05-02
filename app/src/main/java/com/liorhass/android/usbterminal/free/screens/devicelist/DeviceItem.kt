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
package com.liorhass.android.usbterminal.free.screens.devicelist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.liorhass.android.usbterminal.free.R
import com.liorhass.android.usbterminal.free.ui.theme.UsbTerminalTheme
import com.liorhass.android.usbterminal.free.usbserial.UsbSerialDevice
import com.liorhass.android.usbterminal.free.usbserial.UsbSerialPort

@Composable
fun DeviceItem(
    usbSerialPort: UsbSerialPort,
//dbg    usbSerialPort: DummyUsbSerialPort,
    itemId: Int,
    isConnected: Boolean,
    onConnectToPortClick: (itemId: Int) -> Unit,
    onSetDeviceTypeAndConnectToPortClick: (itemId: Int) -> Unit,
    onDisconnectFromPortClick: (itemId: Int) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(4.dp),
        elevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp)
//            .clickable { onItemClick(itemId) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            Row {
                Text(
                    text = stringResource(id = R.string.vid, usbSerialPort.usbSerialDevice.vendorId, usbSerialPort.usbSerialDevice.vendorId), // VID: 777 (0x12AA)
                    modifier = Modifier
                        .width(120.dp)
                        .padding(top = 4.dp),
                )
                Text(
                    text = stringResource(id = R.string.pid, usbSerialPort.usbSerialDevice.productId, usbSerialPort.usbSerialDevice.productId), // PID: 777 (0x12AA)
                    modifier = Modifier
                        .padding(top = 4.dp)
                )
            }
            Text(
                text = stringResource(id = R.string.portNumber, usbSerialPort.portNumber), // Port#: 0
                modifier = Modifier
                    .padding(top = 4.dp)
            )
            Text(
                text = if (isConnected) stringResource(R.string.statusConnected) else stringResource(R.string.statusDisconnected),
                color = if (isConnected) UsbTerminalTheme.extendedColors.textColorWhenConnected else UsbTerminalTheme.extendedColors.textColorWhenDisconnected,
                fontWeight = if (isConnected) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier
                    .padding(top = 4.dp)
            )
            Text(
                text=stringResource(R.string.deviceType, usbSerialPort.usbSerialDevice.deviceTypeStr),
                modifier = Modifier
                    .padding(top = 4.dp)
            )
            if (isConnected) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = { onDisconnectFromPortClick(itemId) },
                    ) {
                        Text(text = stringResource(R.string.disconnect))
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (usbSerialPort.usbSerialDevice.deviceType != UsbSerialDevice.DeviceType.UNRECOGNIZED && !isConnected) {
                        OutlinedButton(
                            onClick = { onConnectToPortClick(itemId) },
                        ) {
                            Text(text = stringResource(R.string.connect))
                        }
                        Spacer(Modifier.size(16.dp))
                    }
                    OutlinedButton(
                        onClick = { onSetDeviceTypeAndConnectToPortClick(itemId) },
                    ) {
                        Text(text = stringResource(R.string.set_type_and_connect))
                    }
                }
            }
        }
    }
}
