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
package com.liorhass.android.usbterminal.free.screens.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liorhass.android.usbterminal.free.R
import com.liorhass.android.usbterminal.free.main.MainViewModel
import com.liorhass.android.usbterminal.free.ui.theme.UsbTerminalTheme

@Composable
fun CtrlButtonsRow(
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(color = UsbTerminalTheme.extendedColors.ctrlButtonsLineBackgroundColor)
            .padding(vertical = 1.dp, horizontal = 1.dp)
    ) {
        ToggleButton(
            isSelected = mainViewModel.userInputHandler.ctrlButtonIsSelected.value,
            modifier = Modifier
//                .defaultMinSize(minHeight = 1.dp)
                .height(40.dp)
                .padding(horizontal = 1.dp),
            onClick = { mainViewModel.userInputHandler.onCtrlKeyButtonClick() }
        ) {
            Text(
                text = stringResource(R.string.ctrl),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.onBackground,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        val buttonModifier = Modifier
            .width(48.dp)
            .height(40.dp)
//            .defaultMinSize(minHeight = 1.dp)

        OutlinedButton(
            modifier = buttonModifier.padding(horizontal = 1.dp),
            onClick = { mainViewModel.userInputHandler.onLeftButtonClick() }
        ) {
            Icon(
                // Icons.Filled.KeyboardArrowLeft,
                painter = painterResource(id = R.drawable.ic_baseline_arrow_back_24),
                tint = MaterialTheme.colors.onBackground,
                contentDescription = stringResource(R.string.left)
            )
        }
        OutlinedButton(
            modifier = buttonModifier.padding(horizontal = 1.dp),
            onClick = { mainViewModel.userInputHandler.onDownButtonClick() }
        ) {
            Icon(
                // Icons.Filled.KeyboardArrowDown,
                painter = painterResource(id = R.drawable.ic_baseline_arrow_downward_24),
                tint = MaterialTheme.colors.onBackground,
                contentDescription = stringResource(R.string.down)
            )
        }
        OutlinedButton(
            modifier = buttonModifier.padding(horizontal = 1.dp),
            onClick = { mainViewModel.userInputHandler.onUpButtonClick() }
        ) {
            Icon(
                // Icons.Filled.KeyboardArrowUp,
                painter = painterResource(id = R.drawable.ic_baseline_arrow_upward_24),
                tint = MaterialTheme.colors.onBackground,
                contentDescription = stringResource(R.string.up)
            )
        }
        OutlinedButton(
            modifier = buttonModifier.padding(horizontal = 1.dp),
            onClick = { mainViewModel.userInputHandler.onRightButtonClick() }
        ) {
            Icon(
                //Icons.Filled.KeyboardArrowRight,
                painter = painterResource(id = R.drawable.ic_baseline_arrow_forward_24),
                tint = MaterialTheme.colors.onBackground,
                contentDescription = stringResource(R.string.right)
            )
        }
    }
}