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
package com.liorhass.android.usbterminal.free.settings.ui.lib.internal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.liorhass.android.usbterminal.free.R

@Composable
fun SingleChoiceDialog(
    title: @Composable () -> Unit,
    choices: List<String>,
    preSelectedIndex: Int, // -1 if nothing is selected
    onSelection: (index: Int) -> Unit,
    onCancel: () -> Unit,
) {
    Dialog(onDismissRequest = onCancel) {
        Card(
            elevation = 8.dp,
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Box(modifier = Modifier.padding(start = 10.dp, top = 8.dp)) {
                    ProvideTextStyle(value = MaterialTheme.typography.h6) {
                        title()
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                RadioButtonGroup(
                    labels = choices,
                    selectedIndex = preSelectedIndex,
                    onClick = onSelection
                )

                Spacer(modifier = Modifier.height(8.dp))
                // Cancel button
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onCancel) {
                        Text(text = stringResource(R.string.cancel_all_caps))
                    }
//                    Spacer(modifier = Modifier.width(4.dp))
//                    TextButton(onClick = {
//                        onPositiveClick(color)
//                    }) {
//                        Text(text = "OK")
//                    }
                }
            }
        }
    }
}

@Composable
fun RadioButtonGroup(labels: List<String>, selectedIndex: Int, onClick: (index: Int) -> Unit) {
//    Timber.d("RadioButtonGroup() selectedIndex=$selectedIndex")
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp)
    ) {
        labels.forEachIndexed { index, label ->
            Row(modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick(index) }
                .padding(top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = index == selectedIndex,
                    onClick = { onClick(index) },
                    modifier = Modifier
                        .align(Alignment.CenterVertically),
                )
                Text(
                    text = label,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .align(Alignment.CenterVertically),
                )
            }
        }
    }
}

@Preview
@Composable
fun RadioButtonGroupPreview() {
    val labels = listOf("Label 1","Label 2", "Label 3")
    RadioButtonGroup(labels, selectedIndex = 1, onClick = { })
}

