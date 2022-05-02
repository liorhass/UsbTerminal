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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.liorhass.android.usbterminal.free.main.MainViewModel
import com.liorhass.android.usbterminal.free.main.ScreenLine
import com.liorhass.android.usbterminal.free.ui.theme.DefaultTextColorInTextMode
import kotlinx.coroutines.delay

@Composable
fun ColumnScope.TerminalScreenTextSection(
    lines: State<Array<ScreenLine>>,
    shouldMeasureScreenDimensions: Int,
    onScreenDimensionsMeasured: (MainViewModel.ScreenDimensions) -> Unit,
    shouldScrollToBottom: State<Boolean>,
    onScrolledToBottom: () -> Unit,
    fontSize: Int,
    mainFocusRequester: FocusRequester,
    auxFocusRequester: FocusRequester,
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    if (shouldMeasureScreenDimensions != 0) {
        MeasureScreenDimensions(onScreenDimensionsMeasured, fontSize, shouldMeasureScreenDimensions)
    } else {
        // Used to prevent ripple effect when clicked. https://stackoverflow.com/a/66703893/1071117
        val interactionSource = remember { MutableInteractionSource() }
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .weight(1f, true)
                .background(Color.Black) //todo: from config in combination with text color
                .clickable(interactionSource, indication = null) {
                    openSoftKeyboard(
                        coroutineScope = coroutineScope,
                        listState = listState,
                        listSize = lines.value.size,
                        mainFocusRequester = mainFocusRequester,
                        auxFocusRequester = auxFocusRequester,
                    )
                },
        ) {
            items(
                items = lines.value,
                key = { line -> line.uid },
            ) { line ->
                TerminalScreenLine(line, fontSize)
            }
        }
        if (shouldScrollToBottom.value) {
            LaunchedEffect(lines.value) {
                listState.scrollToItem(lines.value.size)
                delay(100)
                listState.scrollToItem(lines.value.size)
                onScrolledToBottom()
            }
        }
    }
}

@Composable
fun TerminalScreenLine(
    line : ScreenLine,
    fontSize: Int,
    modifier: Modifier = Modifier,
    onTextLayout: (TextLayoutResult) -> Unit = {},
) {
//    Timber.d("TerminalScreenLine(): line.textLength=${line.textLength}  line.annotation.size=${line.getAnnotatedString().spanStyles.size}")
    Text(
        text = line.getAnnotatedString(),
        color = DefaultTextColorInTextMode, // todo: from config
        fontFamily = FontFamily.Monospace,
        fontSize = fontSize.sp,
        overflow = TextOverflow.Clip,
        maxLines = 1,
        onTextLayout = onTextLayout,
        modifier = modifier,
    )
}

