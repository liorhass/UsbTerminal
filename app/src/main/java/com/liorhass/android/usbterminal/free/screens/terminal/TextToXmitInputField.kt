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

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liorhass.android.usbterminal.free.settings.model.SettingsRepository
import com.liorhass.android.usbterminal.free.ui.theme.UsbTerminalTheme
import timber.log.Timber


@Composable
fun TextToXmitInputField(
    inputMode: Int, // One of the values in SettingsRepository.InputMode: CHAR_BY_CHAR or WHOLE_LINE
    textToXmit: State<String>,
    textToXmitCharByChar: State<TextFieldValue>,
    onXmitCharByCharKBInput: (text: TextFieldValue) -> Unit,
    onTextChanged: (text: String) -> Unit,
    onXmitText: () -> Unit,
    mainFocusRequester: FocusRequester,
    auxFocusRequester: FocusRequester,
) {
    if (inputMode == SettingsRepository.InputMode.WHOLE_LINE) {
        WholeLineTextToXmitInputField(textToXmit, onTextChanged, onXmitText, mainFocusRequester)
    } else {
        CharByCharTextToXmitInputField(textToXmitCharByChar, onXmitCharByCharKBInput, mainFocusRequester, auxFocusRequester)
    }
}

@Composable
fun CharByCharTextToXmitInputField(
    textToXmit: State<TextFieldValue>,
    onTextChanged: (textFieldValue: TextFieldValue) -> Unit,
    mainFocusRequester: FocusRequester,
    auxFocusRequester: FocusRequester,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(0.dp)
    ) {
        // Main TextField
        TextField(
            value = textToXmit.value,
            onValueChange = { tfv -> onTextChanged(tfv) },
            modifier = Modifier
                .padding(0.dp)
                .height(1.dp)
                .width(44.dp)
//                .weight(1f)
                .focusRequester(mainFocusRequester),
            maxLines = 1,
            textStyle = TextStyle.Default.copy(
                fontSize = 1.sp,
//                color = Color.Transparent,
                background = Color.Transparent,
            ),
//            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text, imeAction = ImeAction.Default),
            colors = TextFieldDefaults.textFieldColors(
                textColor = Color(0x0a000000),
                disabledTextColor = Color.Transparent,
                backgroundColor = Color.Transparent,
                cursorColor = Color.Transparent,
                errorCursorColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent,
                leadingIconColor = Color.Transparent,
                disabledLeadingIconColor = Color.Transparent,
                errorLeadingIconColor = Color.Transparent,
                trailingIconColor = Color.Transparent,
                disabledTrailingIconColor = Color.Transparent,
                errorTrailingIconColor = Color.Transparent,
                focusedLabelColor = Color.Transparent,
                unfocusedLabelColor = Color.Transparent,
                disabledLabelColor = Color.Transparent,
                errorLabelColor = Color.Transparent,
                placeholderColor = Color.Transparent,
                disabledPlaceholderColor = Color.Transparent,
            ),
        )
        // Aux TextField
        TextField(
            value = "",
            onValueChange = {},
            modifier = Modifier
                .padding(0.dp)
                .height(1.dp)
//                .weight(1f)
                .focusRequester(auxFocusRequester),
            maxLines = 1,
            textStyle = TextStyle.Default.copy(
                fontSize = 1.sp,
                color = Color.Transparent,
                background = Color.Transparent,
            ),
//            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text, imeAction = ImeAction.Default),
            colors = TextFieldDefaults.textFieldColors(
                textColor = Color.Red,
                disabledTextColor = Color.Transparent,
                backgroundColor = Color.Transparent,
                cursorColor = Color.Transparent,
                errorCursorColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent,
                leadingIconColor = Color.Transparent,
                disabledLeadingIconColor = Color.Transparent,
                errorLeadingIconColor = Color.Transparent,
                trailingIconColor = Color.Transparent,
                disabledTrailingIconColor = Color.Transparent,
                errorTrailingIconColor = Color.Transparent,
                focusedLabelColor = Color.Transparent,
                unfocusedLabelColor = Color.Transparent,
                disabledLabelColor = Color.Transparent,
                errorLabelColor = Color.Transparent,
                placeholderColor = Color.Transparent,
                disabledPlaceholderColor = Color.Transparent,
            ),
        )
    }
}

@Composable
fun WholeLineTextToXmitInputField(
    textToXmit: State<String>,
    onTextChanged: (text: String) -> Unit,
    onXmitText: () -> Unit,
    focusRequester: FocusRequester,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 2.dp)
    ) {
        Timber.d("WholeLineTextToXmitInputField(): textToXmit='${textToXmit.value}'")
        BasicTextField(
            value = textToXmit.value,
            onValueChange = onTextChanged,
            textStyle = TextStyle.Default.copy(
                fontSize = 16.sp,
                color = Color.Blue,
                background = Color.LightGray,
            ),
            modifier = Modifier
                .padding(start = 2.dp, top = 2.dp)
                .horizontalScroll(rememberScrollState())
                .weight(1f)
                .focusRequester(focusRequester),
            maxLines = 2,
        ) { innerTextField ->
            Box(modifier = Modifier
                .background(
                    color = UsbTerminalTheme.extendedColors.textToXmitInputFieldBackgroundColor, //Color(0xFFEEEEEE),
                    shape = RoundedCornerShape(2.dp)
                )
                .border(
                    BorderStroke(1.dp, UsbTerminalTheme.extendedColors.textToXmitInputFieldBorderColor), //Color(0xFF444444)
                    shape = RoundedCornerShape(2.dp)
                )
                .padding(4.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                innerTextField()
            }
        }

        IconButton(
            onClick = {
                onXmitText()
            },
        ) {
            Icon(
                Icons.Filled.Send,
                contentDescription = "Send",
                tint = MaterialTheme.colors.primary,
            )
        }
    }
}


