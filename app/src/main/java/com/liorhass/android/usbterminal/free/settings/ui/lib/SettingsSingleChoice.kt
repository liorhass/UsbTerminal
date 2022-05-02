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
package com.liorhass.android.usbterminal.free.settings.ui.lib

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.liorhass.android.usbterminal.free.R
import com.liorhass.android.usbterminal.free.settings.ui.lib.internal.SettingsTileIcon
import com.liorhass.android.usbterminal.free.settings.ui.lib.internal.SettingsTileTexts
import com.liorhass.android.usbterminal.free.settings.ui.lib.internal.SingleChoiceDialog
import com.liorhass.android.usbterminal.free.settings.ui.lib.internal.SingleChoiceWithFreeInputFieldDialog
import timber.log.Timber

@Composable
fun SettingsSingleChoice(
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    title: @Composable () -> Unit,
    choices: List<String>,
    hasFreeInputField: Boolean = false,
    freeInputFieldValue: String = "",
    preSelectedIndex: Int = -1,
    onSelection: (choiceIndex: Int, choiceValue: String) -> Unit,
) {
//    Timber.d("SettingsSingleChoice() preSelectedIndex=$preSelectedIndex hasFreeInput=$hasFreeInputField")
    var showDialog by remember { mutableStateOf(false) }

    Surface {
        Row(
            modifier = modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
                    .clickable(onClick = { showDialog = true }),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (icon != null) {
                    SettingsTileIcon(icon = icon)
                } else {
                    Spacer(modifier = Modifier.width(20.dp))
                }
                SettingsTileTexts(
                    title = title,
                    subtitle = {
                        Text(text = when {
                            preSelectedIndex == choices.size -> freeInputFieldValue
                            preSelectedIndex >= 0 -> choices[preSelectedIndex]
                            else -> stringResource(R.string.not_set)
                        })
                    }
                )
            }
        }
    }

    if (showDialog) {
        if (hasFreeInputField) {

            SingleChoiceWithFreeInputFieldDialog(
                title = title,
                choices = choices,
                initiallySelectedIndex = preSelectedIndex,
                freeInputFieldValue = freeInputFieldValue,
                onCancel = { showDialog = false },
                onSelection = {selectionIndex, selectionValue ->
                    Timber.d("SettingsSingleChoice(): selectionIndex=$selectionIndex selectionValue=$selectionValue")
                    showDialog = false
                    onSelection(selectionIndex, selectionValue)
                }
            )
        } else {
            SingleChoiceDialog(
                title = title,
                choices = choices,
                preSelectedIndex = preSelectedIndex,
                onCancel = { showDialog = false },
                onSelection = { selectedIndex ->
                    Timber.d("SettingsSingleChoice(): selectedIndex=$selectedIndex")
                    showDialog = false
                    onSelection(selectedIndex, choices[selectedIndex])
                }
            )
        }
    }
}

@Preview
@Composable
internal fun SettingsSingleChoicePreview() {
    MaterialTheme {
        SettingsSingleChoice(
            icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings") },
            title = { Text(text = "Single choice item") },
            preSelectedIndex = 1,
            hasFreeInputField = true,
            choices = listOf("option 0","option 1","option 2","option 3"),
            onSelection = {_, _-> },
        )
    }
}
