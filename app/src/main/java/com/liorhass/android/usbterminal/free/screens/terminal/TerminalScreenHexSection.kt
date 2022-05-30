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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.liorhass.android.usbterminal.free.main.ScreenHexModel
import com.liorhass.android.usbterminal.free.ui.util.isKeyboardOpenAsState
import kotlinx.coroutines.launch

@Composable
fun ColumnScope.TerminalScreenHexSection(
    textBlocks: State<Array<ScreenHexModel.HexTextBlock>>,
    shouldScrollToBottom: State<Boolean>,
    onScrolledToBottom: () -> Unit,
    fontSize: Int,
    mainFocusRequester: FocusRequester,
    auxFocusRequester: FocusRequester,
    onKeyboardStateChange: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    val isKeyboardOpen by isKeyboardOpenAsState()
    var atBottomBeforeKBWasOpened by remember { mutableStateOf(false) }

    // Used to prevent ripple effect when clicked. https://stackoverflow.com/a/66703893/1071117
    val interactionSource = remember { MutableInteractionSource() }

    LazyColumn(
        state = lazyListState,
        modifier = Modifier
            .fillMaxSize()
            .weight(1f, true)
            .background(Color.Black) //todo: from config in combination with text color
            .clickable(interactionSource, indication = null) {
                atBottomBeforeKBWasOpened =
                    lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index == lazyListState.layoutInfo.totalItemsCount - 1
                openSoftKeyboard(
                    coroutineScope = coroutineScope,
                    mainFocusRequester = mainFocusRequester,
                    auxFocusRequester = auxFocusRequester,
                )
            },
    ) {
        items(
            items = textBlocks.value,
            key = { textBlock -> textBlock.uid },
        ) { textBlock ->
            Text(
                text = textBlock.annotatedString ?: AnnotatedString(""),
                fontFamily = FontFamily.Monospace,
                fontSize = fontSize.sp,
                overflow = TextOverflow.Clip,
            )
        }
    }
    LaunchedEffect(key1 = isKeyboardOpen) {
        if (isKeyboardOpen && atBottomBeforeKBWasOpened) {
            lazyListState.scrollToItem(textBlocks.value.lastIndex)
        }
        onKeyboardStateChange()
    }
    if (shouldScrollToBottom.value) {
        LaunchedEffect(textBlocks.value) {
            lazyListState.scrollToItem(textBlocks.value.lastIndex)
            onScrolledToBottom()
        }
    }
}
