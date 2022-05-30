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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import com.liorhass.android.usbterminal.free.main.MainViewModel

@Suppress("unused")
@Composable
fun ColumnScope.DropdownMenuDebugSection(
    showOverflowMenu: MutableState<Boolean>,
    mainViewModel: MainViewModel,
) {
    DropdownMenuItem(onClick = {
        showOverflowMenu.value = false
    }
    ) {
        Text(
            text = "Debug 1",
            modifier = Modifier
                .clickable { mainViewModel.debug1(); showOverflowMenu.value = false }
        )
    }
    DropdownMenuItem(onClick = {
        showOverflowMenu.value = false
    }
    ) {
        Text(
            text = "Debug 2",
            modifier = Modifier
                .clickable { mainViewModel.debug2(); showOverflowMenu.value = false }
        )
    }
    DropdownMenuItem(onClick = {
        showOverflowMenu.value = false
    }
    ) {
        Text(
            text = "Debug welcome msg",
            modifier = Modifier
                .clickable { mainViewModel.debugWelcomeMsg(); showOverflowMenu.value = false }
        )
    }
    DropdownMenuItem(onClick = {
        showOverflowMenu.value = false
    }
    ) {
        Text(
            text = "Debug upgrade msg",
            modifier = Modifier
                .clickable { mainViewModel.debugUpgradeMsg(); showOverflowMenu.value = false }
        )
    }
}