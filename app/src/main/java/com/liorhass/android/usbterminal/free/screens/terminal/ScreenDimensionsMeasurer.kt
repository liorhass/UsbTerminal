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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import com.liorhass.android.usbterminal.free.main.MainViewModel
import com.liorhass.android.usbterminal.free.main.ScreenLine

@Composable
fun MeasureScreenDimensions(
    onMeasuredScreenDimensions: (MainViewModel.ScreenDimensions) -> Unit,
    fontSize: Int,
    requestUID: Int,
) {
    // A hack to measure line width in characters:
    // We draw a very long line and use textLayoutResult.getLineEnd() to get it.
    var screenDimensions =  MainViewModel.ScreenDimensions(0,0)
    val longLine = ScreenLine(text = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx$requestUID")
    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.Transparent) // todo: Should be text-screen background color (if it's not always black)
    ) {
        TerminalScreenLine(
            line = longLine,
            fontSize = fontSize,
            onTextLayout = { textLayoutResult ->
                val lineEndIndex = textLayoutResult.getLineEnd(lineIndex = 0, visibleEnd = true)
                screenDimensions = screenDimensions.copy(width = lineEndIndex)
            },
            modifier = Modifier
                .fillMaxWidth()
                .alpha(0f)
        )

        // A hack to measure screen height in lines:
        // We draw many lines and use listState.layoutInfo.visibleItemsInfo.size to get the
        // number of visible lines
        val line = ScreenLine(text = if (requestUID == 999999999) " " else "") // Dummy use of requestUID. Only to force recomposition when it changes
        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                //todo:2brm  .background(Color.Black), // todo: Should be text-screen background color (if it's not always black)
        ) {
            items(100) {
                TerminalScreenLine(
                    line = line,
                    fontSize = fontSize,
                    modifier = Modifier.alpha(0f),
                )
            }
        }
        LaunchedEffect(key1 = requestUID) {
            // The -1 here is because visibleItemsInfo includes all lines visible even if
            // they're only partially visible. We consider a partially-visible line as
            // off-screen rather than as on-screen
            screenDimensions = screenDimensions.copy(height = listState.layoutInfo.visibleItemsInfo.size - 1)
            onMeasuredScreenDimensions(screenDimensions)
        }
    }
}

