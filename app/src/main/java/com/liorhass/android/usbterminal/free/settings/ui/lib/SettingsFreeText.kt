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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.liorhass.android.usbterminal.free.R
import com.liorhass.android.usbterminal.free.settings.ui.lib.internal.*
import com.liorhass.android.usbterminal.free.settings.ui.lib.internal.SettingsTileIcon
import com.liorhass.android.usbterminal.free.settings.ui.lib.internal.SettingsTileTexts
import timber.log.Timber

// Note: There's a bug that KB is not opened automatically after focus request to a TextField
// inside a dialog. See https://issuetracker.google.com/issues/204502668
@Composable
fun SettingsFreeText(
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    title: @Composable () -> Unit,
    label: @Composable () -> Unit,
    previousText: String?,
    keyboardOptions: KeyboardOptions,
    onTextInput: (text: String) -> Unit,
) {
    // Timber.d("SettingsFreeText()")
    var newText by remember { mutableStateOf(previousText ?: "") }
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

                val subtitle = if (previousText.isNullOrBlank()) {
                    stringResource(R.string.not_set)
                } else {
                    previousText
                }

                SettingsTileTexts(
                    title = title,
                    subtitle = { Text(text = subtitle) }
                )
            }
        }
    }

    if (showDialog) {
        FreeTextDialog(
            title = title,
            label = label,
            previousText = previousText ?: "",
            maxLines = 3,
            onTextChange = { newText = it },
            onCancel = { showDialog = false },
            onOk = {
                Timber.d("SettingsFreeText(): newText=$newText")
                onTextInput(newText)
                showDialog = false
            },
            keyboardActions = KeyboardActions(onDone = {
                Timber.d("SettingsFreeText(): newText=$newText")
                onTextInput(newText)
                showDialog = false
            }),
            keyboardOptions = keyboardOptions,
//            keyboardOptions: KeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        )
    }
}

@Preview
@Composable
fun SettingsFreeTextPreview() {
    MaterialTheme {
        Column {
            SettingsFreeText(
                icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings") },
                title = { Text(text = "Free text") },
                label = { Text(text = "FreeText label") },
                previousText = "Not set",
                onTextInput = {},
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            )
            Spacer(modifier = Modifier.height(30.dp))
            SettingsFreeText(
                title = { Text(text = "Free text") },
                label = { Text(text = "FreeText label") },
                previousText = "lior.apps@gmail.com",
                onTextInput = {},
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            )
        }
    }
}
