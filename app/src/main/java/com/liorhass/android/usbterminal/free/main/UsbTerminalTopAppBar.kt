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
package com.liorhass.android.usbterminal.free.main

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liorhass.android.usbterminal.free.R
import com.liorhass.android.usbterminal.free.ui.theme.UsbTerminalTheme

enum class UTTopAppBarNavigationIcon {
    Menu, Back, Clear
}

@Composable
fun UsbTerminalTopAppBar(
    navigationIcon: UTTopAppBarNavigationIcon,
    onNavigationIconClick: () -> Unit,
    title: String,
    isInContextualMode: Boolean,
    actions: @Composable RowScope.() -> Unit
) {
//    Surface(color = MaterialTheme.colors.primary) {
    val onSurfaceColor = if (isInContextualMode) {
        UsbTerminalTheme.extendedColors.contextualAppBarOnBackground
    } else {
        MaterialTheme.colors.onPrimary
    }
    TopAppBar(
        title = {
            Text(
                text = title,
                fontSize = 18.sp,
                color = onSurfaceColor,
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigationIconClick) {
                when (navigationIcon) {
                    UTTopAppBarNavigationIcon.Menu  -> Icon(Icons.Filled.Menu, stringResource(id = R.string.menu), tint = onSurfaceColor)
                    UTTopAppBarNavigationIcon.Back  -> Icon(Icons.Filled.ArrowBack, stringResource(id = R.string.back), tint = onSurfaceColor)
                    UTTopAppBarNavigationIcon.Clear -> Icon(Icons.Filled.Clear, stringResource(id = R.string.clear), tint = onSurfaceColor)
                }
            }
        },
        actions = actions,
        elevation = 4.dp,
        backgroundColor = when (isInContextualMode) {
            true -> UsbTerminalTheme.extendedColors.contextualAppBarBackground
            false -> MaterialTheme.colors.primary
        },
    )
//    }
}

