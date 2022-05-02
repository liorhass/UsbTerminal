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

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.liorhass.android.usbterminal.free.R
import timber.log.Timber

/**
 * Display a list of radio-buttons and one free-text field.
 *
 * @param initiallySelectedIndex -1 if nothing is selected, choices.size if the freeInputField is selected
 * @param onSelection If the freeInputField is selected index is set to choices.size
 */
@OptIn(ExperimentalComposeUiApi::class) // 20220220 for LocalSoftwareKeyboardController
@Composable
fun SingleChoiceWithFreeInputFieldDialog(
    title: @Composable () -> Unit,
    choices: List<String>,
    freeInputFieldValue: String,
    initiallySelectedIndex: Int, // -1 if nothing is selected, choices.size if freeInputField is selected
    onCancel: () -> Unit,
    onSelection: (index: Int, freeInputFieldValue: String) -> Unit,
    keyboardOptions: KeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
) {
    var freeInputFieldText by remember { mutableStateOf(freeInputFieldValue) }
    var okButtonEnabled by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableStateOf(initiallySelectedIndex) }
    val keyboardController = LocalSoftwareKeyboardController.current

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

                RadioButtonGroupWithFreeInputField(
                    labels = choices,
                    freeInputFieldValue = freeInputFieldValue,
                    selectedIndex = selectedIndex,
                    onFreeFieldChange = {newText -> freeInputFieldText = newText},
                    onSelection = { index ->
                        Timber.d("onSelection index=$index")
                        if (index == choices.size) {
                            selectedIndex = index
                            okButtonEnabled = true
                        } else {
                            keyboardController?.hide()
                            onSelection(index, choices[index])
                        }
                    },
                    keyboardOptions = keyboardOptions,
                    keyboardActions = KeyboardActions(onDone = {
                        keyboardController?.hide()
                        onSelection(choices.size, freeInputFieldText)
                    }),
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
                    if (okButtonEnabled) {
                        Spacer(modifier = Modifier.width(4.dp))
                        TextButton(
                            onClick = {
                                keyboardController?.hide()
                                onSelection(choices.size, freeInputFieldText)
                            }
                        ) {
                            Text(text = stringResource(R.string.ok))
                        }
                    }
                }
            }
        }
    }
}

@ExperimentalComposeUiApi
@Composable
fun RadioButtonGroupWithFreeInputField(
    labels: List<String>,
    freeInputFieldValue: String,
    selectedIndex: Int,
    onFreeFieldChange: (newText: String) -> Unit,
    onSelection: (index: Int) -> Unit,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
) {
    Column(modifier = Modifier
        .padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp)
    ) {
        labels.forEachIndexed { index, label ->
            RadioButtonWithLabel(
                label = label,
                isSelected = index == selectedIndex,
                onClick = { onSelection(index) },
            )
        }
        RadioButtonWithFreeInputField(
            freeInputFieldValue = freeInputFieldValue,
            isSelected = selectedIndex == labels.size,
            onTextChange = onFreeFieldChange,
            onSelect = { onSelection(labels.size) },
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
        )
    }
}

@ExperimentalComposeUiApi
@Preview
@Composable
fun RadioButtonGroupWithFreeInputFieldPreview() {
    val labels = listOf("Label 1","Label 2", "Label 3")
    RadioButtonGroupWithFreeInputField(
        labels,
        freeInputFieldValue = "FFRREEEE",
        selectedIndex = 1,
        onFreeFieldChange = {},
        onSelection = {},
        keyboardOptions = KeyboardOptions(),
        keyboardActions = KeyboardActions(),
    )
}

@Composable
fun RadioButtonWithLabel(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .selectable(true, onClick = { onClick() })
        .padding(top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            modifier = Modifier
                .align(Alignment.CenterVertically),
        )
        Text(
            text = label,
            //                    style = MaterialTheme.typography.body2,
            modifier = Modifier
                .padding(start = 8.dp)
                .align(Alignment.CenterVertically),
        )
    }
}

@Composable
fun RadioButtonWithFreeInputField(
    freeInputFieldValue: String,
    isSelected: Boolean,
    onTextChange: (newText: String) -> Unit,
    onSelect: () -> Unit,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
) {
    var value by remember { mutableStateOf(TextFieldValue(freeInputFieldValue)) }

    // initialize focus reference to be able to request focus programmatically. https://stackoverflow.com/a/66626506/1071117
    val focusRequester = FocusRequester()


    Row(modifier = Modifier
        .fillMaxWidth()
        .selectable(true, onClick = onSelect)
        .padding(top = 6.dp, bottom = 6.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = isSelected,
            onClick = {
                onSelect()
                focusRequester.requestFocus() // Request focus to the text field
            },
            modifier = Modifier
                .align(Alignment.CenterVertically),
        )

        OutlinedTextField(
            value = value,
            onValueChange = {value = it; onTextChange(it.text)},
            label = { Text("Any Baud Rate") },
            modifier = Modifier
                .padding(start = 8.dp, top = 2.dp)
                .align(Alignment.CenterVertically)
                .weight(1f)
                .horizontalScroll(rememberScrollState())
                .focusRequester(focusRequester)
                .onFocusChanged {
//                    Timber.d("onFocusChanged hasFocus=${it.hasFocus}  isFocused=${it.isFocused}")
                    if (it.isFocused) onSelect()
                }, // The text field may get focused by a user clicking on it. We need to set the radio buttons accordingly
            maxLines = 1,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
        )
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
