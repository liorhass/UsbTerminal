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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.liorhass.android.usbterminal.free.R
import timber.log.Timber

/**
 * Display a list of radio-buttons and one free-text field.
 */
@OptIn(ExperimentalComposeUiApi::class) // 20220220 for LocalSoftwareKeyboardController
@Composable
fun SingleChoiceWithFreeInputFieldDialog(
    title: @Composable () -> Unit,
    choices: List<String>,
    freeInputFieldValue: String,
    freeInputFieldLabel: String,
    freeInputFieldIsValid: Boolean,
    initiallySelectedIndex: Int, // -1 if nothing is selected, choices.size if freeInputField is selected
    bottomBlockContent: (@Composable ColumnScope.() -> Unit)?,
    onFreeInputFieldChange: ((String) -> Unit)?,
    onCancel: () -> Unit,
    keyboardOptions: KeyboardOptions,
    onSelection: (index: Int, freeInputFieldValue: String) -> Unit,
) {
    // If onFreeInputFieldChange() callback is null we maintain the value of the
    // free-input-field locally in this var. Otherwise we call onFreeInputFieldChange()
    // on every change, and let our controller (e.g. viewModel) handle these changes
    // (by setting our freeInputFieldValue on each change)
    var freeInputFieldValueLocal by remember { mutableStateOf(freeInputFieldValue) }
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
                    freeInputFieldValue = if (onFreeInputFieldChange != null) freeInputFieldValue else freeInputFieldValueLocal,
                    freeInputFieldLabel = freeInputFieldLabel,
                    selectedIndex = selectedIndex,
                    bottomBlockContent = bottomBlockContent,
                    onFreeInputFieldChange = { newText ->
                        if (onFreeInputFieldChange != null) {
                            onFreeInputFieldChange(newText)
                        } else {
                            freeInputFieldValueLocal = newText
                        }
                    },
                    onSelection = { index ->
                        if (index == choices.size) {
                            selectedIndex = index
                        } else {
                            keyboardController?.hide()
                            onSelection(index, choices[index])
                        }
                    },
                    keyboardOptions = keyboardOptions,
                    keyboardActions = KeyboardActions(onDone = {
                        keyboardController?.hide()
                        onSelection(choices.size, if (onFreeInputFieldChange == null) freeInputFieldValueLocal else freeInputFieldValue)
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
                    Spacer(modifier = Modifier.width(4.dp))
                    TextButton(
                        enabled = freeInputFieldIsValid,
                        onClick = {
                            keyboardController?.hide()
                            onSelection(choices.size, if (onFreeInputFieldChange == null) freeInputFieldValueLocal else freeInputFieldValue)
                        }
                    ) {
                        Text(text = stringResource(R.string.ok))
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
    freeInputFieldLabel: String,
    freeInputFieldValue: String,
    selectedIndex: Int,
    bottomBlockContent: (@Composable ColumnScope.() -> Unit)?,
    onFreeInputFieldChange: (newText: String) -> Unit,
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
            label = freeInputFieldLabel,
            freeInputFieldValue = freeInputFieldValue,
            isSelected = selectedIndex == labels.size,
            onTextChange = onFreeInputFieldChange,
            onSelect = { onSelection(labels.size) },
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
        )
        if (bottomBlockContent != null) {
            Spacer(modifier = Modifier.height(16.dp))
            bottomBlockContent()
        }
    }
}

@ExperimentalComposeUiApi
@Preview
@Composable
fun RadioButtonGroupWithFreeInputFieldPreview() {
    val labels = listOf("Label 1","Label 2", "Label 3")
    RadioButtonGroupWithFreeInputField(
        labels,
        freeInputFieldValue = "FREE",
        freeInputFieldLabel = "Any baud rate",
        selectedIndex = 1,
        bottomBlockContent = {
            Box(
                modifier = Modifier
                    .height(40.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(color = Color.Black),
                    contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = "Text color - Green",
                    color = Color(0xffff6c00),
                    modifier = Modifier
                        .padding(start = 8.dp),
                )
            }
        },
        onFreeInputFieldChange = {},
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
    label: String,
    freeInputFieldValue: String,
    isSelected: Boolean,
    onTextChange: (newText: String) -> Unit,
    onSelect: () -> Unit,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
) {
//todo:2brm     var value by remember { mutableStateOf(TextFieldValue(freeInputFieldValue)) }

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
            value = freeInputFieldValue,
            onValueChange = { onTextChange(it) },
            label = { Text(label) },
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
