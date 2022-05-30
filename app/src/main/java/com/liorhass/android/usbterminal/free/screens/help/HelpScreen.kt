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
package com.liorhass.android.usbterminal.free.screens.help

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liorhass.android.usbterminal.free.R
import com.liorhass.android.usbterminal.free.main.*
import com.liorhass.android.usbterminal.free.ui.util.ComposeWebView


object HelpScreenAttributes : UsbTerminalScreenAttributes(
    isTopInBackStack = false,
    route = "Help",
)

// todo: support dark theme: https://medium.com/backyard-programmers/using-webview-in-jetpack-compose-and-enforce-dark-mode-313bca06b5e4
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HelpScreen(
    mainViewModel: MainViewModel = viewModel()
) {
    LaunchedEffect(true) { mainViewModel.setTopBarTitle(R.string.help_screen_title) }

    val mainPageUrl = "https://appassets.androidplatform.net/assets/help/help.html"

//    val state = rememberWebViewState(mainPageUrl)
//    ComposeWebView2(
//        state
//    )
    Dialog(
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false,
            dismissOnBackPress = true,
        ),
        onDismissRequest = { UsbTerminalNavigator.navigateBack() },
    ) {
//        Card(
//            elevation = 8.dp,
//            shape = RoundedCornerShape(8.dp),
//            modifier = Modifier
//                .width(LocalConfiguration.current.screenWidthDp.dp)
//                .height(LocalConfiguration.current.screenHeightDp.dp)
//                .background(Color.Transparent)
//                .padding(4.dp),
//        ) {
            Column(
                modifier = Modifier
                    .background(Color.Transparent)
                    .padding(0.dp)
//                    .verticalScroll(rememberScrollState())
                    .width(LocalConfiguration.current.screenWidthDp.dp)
//                    .height(LocalConfiguration.current.screenHeightDp.dp)
            ) {
//                Box(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .height(40.dp)
//                        .background(Color.Red)
//                ) {
//
//                }
                UsbTerminalTopAppBar(
                    navigationIcon = UTTopAppBarNavigationIcon.Back,
                    onNavigationIconClick = { UsbTerminalNavigator.navigateBack() },
                    title = "Help",
                    isInContextualMode = false
                ) {

                }
                ComposeWebView(
                    url = run {
                        val lastUrl =
                            mainViewModel.settingsRepository.settingsStateFlow.value.lastVisitedHelpUrl
                        lastUrl ?: mainPageUrl
                    },
                    onPageLoaded = { url ->
                        if (url != null) {
                            mainViewModel.settingsRepository.setLastVisitedHelpUrl(url)
                        }
                    }
                )
            }
//        }
    }
}