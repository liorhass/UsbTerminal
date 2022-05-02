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

import android.app.Application
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liorhass.android.usbterminal.free.R
import com.liorhass.android.usbterminal.free.main.MainViewModel
import com.liorhass.android.usbterminal.free.main.UsbTerminalScreenAttributes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.liorhass.android.usbterminal.free.usbserial.UsbSerialPort


object DeviceListScreenAttributes : UsbTerminalScreenAttributes(
    isTopInBackStack = false,
    route = "DeviceList",
) {
    override fun getTopAppBarActions(
        mainViewModel: MainViewModel,
        isTopBarInContextualMode: Boolean): @Composable RowScope.() -> Unit =
        {DeviceListTopAppBarActions(mainViewModel)} // Would have been nicer if we could simply write ::AlarmListTopAppBarActions, but reference to composable functions is currently not implemented
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun DeviceListTopAppBarActions(mainViewModel: MainViewModel) {
}

@Composable
fun DeviceListScreen(
    mainViewModel: MainViewModel
) {
    LaunchedEffect(true) { mainViewModel.setTopBarTitle(R.string.device_list_screen_title) }

    val viewModel: DeviceListViewModel = viewModel(
        factory = DeviceListViewModel.Factory(
            application = LocalContext.current.applicationContext as Application,
            mainViewModel = mainViewModel)
    )
    val items by mainViewModel.portListState
    val usbConnectionState by mainViewModel.usbConnectionState
    val connectedUsbPort = usbConnectionState.connectedUsbPort

    if (items.isEmpty()) {
        EmptyDeviceListMessage()
    } else {
        DeviceList(
            items,
            connectedUsbPort,
            viewModel::onConnectToPortClick,
            viewModel::onSetDeviceTypeAndConnectToPortClick,
            viewModel::onDisconnectFromPortClick,
        )
    }
    if (viewModel.shouldShowSelectDeviceTypeAndConnectDialog.value) {
        SelectDeviceTypeAndConnectDialog(
            choices = viewModel.deviceTypeStrings,
            selectedIndex = viewModel.deviceTypeToIndex(viewModel.selectedDeviceType.value),
            onSelection = viewModel::onSelectDeviceType,
            onConnect = viewModel::onConnectToPortWithSelectedDeviceTypeClick,
            onCancel = viewModel::onCancelSetDeviceTypeAndConnectToPortClick
        )
    }
}

@Composable
private fun EmptyDeviceListMessage() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.no_usb_devices),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.h6,
        )
    }
}

@Composable
private fun DeviceList(
    usbSerialPorts: List<UsbSerialPort>,
//dbg    devices: List<DummyUsbSerialPort>,
    connectedUsbPort: UsbSerialPort?,
    onConnectToPortClick: (itemId: Int) -> Unit,
    onSetDeviceTypeAndConnectToPortClick: (itemId: Int) -> Unit,
    onDisconnectFromPortClick: (itemId: Int) -> Unit,
) {
    // Remember the scroll position across compositions, and also used for scrolling the lazyColumn
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
    ) {
        items(count = usbSerialPorts.size) { itemIndex ->
            DeviceItem(
                usbSerialPort = usbSerialPorts[itemIndex],
                itemId = itemIndex,
                isConnected = usbSerialPorts[itemIndex].isEqual(connectedUsbPort),
                onConnectToPortClick = onConnectToPortClick,
                onSetDeviceTypeAndConnectToPortClick = onSetDeviceTypeAndConnectToPortClick,
                onDisconnectFromPortClick = onDisconnectFromPortClick,
            )
        }
    }
}

