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

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.liorhass.android.usbterminal.free.R

@Composable
fun FreeTextDialog(
    title: @Composable () -> Unit,
    label: @Composable () -> Unit,
    previousText: String,
    maxLines: Int = 1,
    onTextChange: (String) -> Unit,
    onOk: () -> Unit,
    onCancel: () -> Unit,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
) {
    var value by remember {
        // Use of TextRange is so on first focus the cursor is placed at the end of the number. See comment at https://stackoverflow.com/a/66391682/1071117
        mutableStateOf(TextFieldValue(previousText, TextRange(previousText.length)))
    }
    val focusRequester = remember { FocusRequester() }

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

                OutlinedTextField(
                    value = value,
                    onValueChange = {value = it; onTextChange(it.text)},
                    label = label,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, top = 2.dp)
                        .horizontalScroll(rememberScrollState())
                        .focusRequester(focusRequester),
                    maxLines = maxLines,
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                )

                Spacer(modifier = Modifier.height(8.dp))
                // OK & Cancel buttons
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onCancel) {
                        Text(text = stringResource(R.string.cancel_all_caps))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    TextButton(onClick = { onOk() }) {
                        Text(text = stringResource(R.string.ok))
                    }
                }
            }
        }
    }
    LaunchedEffect(true) {
        focusRequester.requestFocus()
    }
}

@Preview
@Composable
fun FreeTextDialogPreview() {
    var value by remember { mutableStateOf(TextFieldValue("previous text")) }

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
                    Text("Title")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = value,
                onValueChange = {value = it},
                label =  { Text("Email address") },//label,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, top = 2.dp)
//                        .align(Alignment.CenterVertically)
//                        .widthIn(min=200.dp,max=300.dp)
                    .horizontalScroll(rememberScrollState()),
                maxLines = 3,
//                keyboardOptions = keyboardOptions,
//                keyboardActions = keyboardActions,
            )

            Spacer(modifier = Modifier.height(8.dp))
            // OK & Cancel buttons
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(onClick = {}) {
                    Text(text = "CANCEL")
                }
                Spacer(modifier = Modifier.width(4.dp))
                TextButton(onClick = { }) {
                    Text(text = "OK")
                }
            }
        }
    }
}


// Unfortunately as of this writing (20210922) we can't preview a dialog.
// We get the following error in the preview window:
//   The graphics preview in the layout editor may not be accurate:
//   - The preview does not support multiple windows.
//
//@Preview
//@Composable
//fun SingleChoiceDialogPreview() {
//    val title = "This is a title"
//    val labels = arrayOf<String>("Label 1","Label 2", "Label 3")
//    SingleChoiceDialog(
//        title,
//        labels,
//        selectedIndex = 1,
//        onSelection = { _ ->  },
//        onCancelClick = {}
//    )
//}
