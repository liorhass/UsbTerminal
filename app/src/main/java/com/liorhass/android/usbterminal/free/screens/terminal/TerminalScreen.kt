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

import android.graphics.Rect
import android.view.ViewTreeObserver
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.liorhass.android.usbterminal.free.R
import com.liorhass.android.usbterminal.free.main.MainViewModel
import com.liorhass.android.usbterminal.free.main.UsbTerminalScreenAttributes
import com.liorhass.android.usbterminal.free.settings.model.SettingsRepository
import com.liorhass.android.usbterminal.free.ui.theme.UsbTerminalTheme
import com.liorhass.android.usbterminal.free.util.collectAsStateLifecycleAware
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object TerminalScreenAttributes : UsbTerminalScreenAttributes(
    isTopInBackStack = true,
    route = "Terminal",
) {
    override fun getTopAppBarActions(
        mainViewModel: MainViewModel,
        isTopBarInContextualMode: Boolean
    ): @Composable RowScope.() -> Unit = {
        TerminalScreenTopAppBarActions(mainViewModel)
    }
}

@Composable
fun TerminalScreen(
    mainViewModel: MainViewModel
) {
    LaunchedEffect(true) { mainViewModel.setTopBarTitle(R.string.terminal_screen_title) }
    val textToXmit = mainViewModel.textToXmit.collectAsStateLifecycleAware() //todo: can probably be converted to state (no need for flow)
    val textToXmitCharByChar = mainViewModel.userInputHandler.textToXmitCharByChar
    val usbConnectionState by mainViewModel.usbConnectionState
    val settingsData by mainViewModel.settingsRepository.settingsStateFlow.collectAsStateLifecycleAware() //todo: can probably be converted to state (no need for flow)
    val displayType = settingsData.displayType
    val screenDimensions by mainViewModel.screenDimensions
    val cursorPosition by mainViewModel.cursorPosition
    val fontSize = settingsData.fontSize
    val shouldShowWelcomeMsg by mainViewModel.shouldShowWelcomeMsg
//    val shouldShowWelcomeMsg by remember { mutableStateOf(true) } // For debugging
    val shouldShowUpgradeFromV1Msg by mainViewModel.shouldShowUpgradeFromV1Msg
//    val shouldShowUpgradeFromV1Msg by remember { mutableStateOf(true) } // For debugging
    val shouldMeasureScreenDimensions by mainViewModel.shouldMeasureScreenDimensions

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // This focusRequesters is how a click on the text area sets focus to the
        // TextToXmitInputField, or opens the soft keyboard
        val mainFocusRequester = remember { FocusRequester() }
        val auxFocusRequester = remember { FocusRequester() }

        if (displayType == SettingsRepository.DisplayType.HEX) {
            TerminalScreenHexSection(
                mainViewModel.screenHexTextBlocksState,
                mainViewModel.screenHexShouldScrollToBottom,
                mainViewModel::onScreenHexScrolledToBottom,
                fontSize,
                mainFocusRequester,
                auxFocusRequester,
            )
        } else {
            TerminalScreenTextSection(
                mainViewModel.screenLines,
                shouldMeasureScreenDimensions,
                mainViewModel::onScreenDimensionsMeasured,
                mainViewModel.screenTextShouldScrollToBottom,
                mainViewModel::onScrolledToBottom,
                fontSize,
                mainFocusRequester,
                auxFocusRequester,
            )
        }
        TextToXmitInputField(
            settingsData.inputMode,
            textToXmit,
            textToXmitCharByChar,
            mainViewModel.userInputHandler::onXmitCharByCharKBInput,
            mainViewModel::setTextToXmit,
            mainViewModel::onXmitButtonClick,
            mainFocusRequester,
            auxFocusRequester,
        )
        if (mainViewModel.ctrlButtonsRowVisible.value) {
            CtrlButtonsRow(mainViewModel)
        }
        Divider(color = UsbTerminalTheme.extendedColors.statusLineDividerColor, thickness = 1.dp)
        StatusLine(usbConnectionState, screenDimensions, cursorPosition, displayType)
        if (shouldShowWelcomeMsg) {
            WelcomeMsgDialog(
                mainViewModel::onUserAcceptedWelcomeOrUpgradeMsg,
                mainViewModel::onUserDeclinedWelcomeOrUpgradeMsg,
            )
        }
        if (shouldShowUpgradeFromV1Msg) {
            UpgradeFromV1MsgDialog(
                mainViewModel::onUserAcceptedWelcomeOrUpgradeMsg,
                mainViewModel::onUserDeclinedWelcomeOrUpgradeMsg,
            )
        }
    }
    LaunchedEffect(keyboardAsState.value) {
        // Launch every time KB changes state (from close to open and vice versa)
        // Timber.d("KB State changed")
        mainViewModel.remeasureScreenDimensions()
    }
}

fun openSoftKeyboard(
    coroutineScope: CoroutineScope,
    listState: LazyListState,
    listSize: Int,
    mainFocusRequester: FocusRequester,
    auxFocusRequester: FocusRequester,
    ) {
    coroutineScope.launch {
        // This is a hack. All we want it to request focus to our hidden
        // TextField (which should display the soft-keyboard). The problem
        // with that is that if the hidden TextField already has the focus,
        // this is nop. This is a problem if the user closes the KB with
        // the system's back button, and then wants to re-open it. The
        // focus does nothing, and the KB remains close. To bypass this
        // we move the focus to a second hidden TextField and back. There's
        // a small delay to let the system catch these focus changes.
        val atBottom = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index == listState.layoutInfo.totalItemsCount - 1
        auxFocusRequester.requestFocus()
        delay(50)
        mainFocusRequester.requestFocus()

        // Since we just opened the KB we should scroll to bottom (but only if we were at the bottom beforehand)
        if (atBottom) {
            delay(500) // Yuck. let the kb enough time to open. A better solution here: https://stackoverflow.com/a/69533584/1071117
            listState.scrollToItem(listSize - 1)
        }
    }
}

/**
 * Tell if the soft keyboard is open or closed. From: https://stackoverflow.com/a/69533584/1071117
 */
val keyboardAsState: State<Boolean>
    @Composable
    get() {
        val keyboardState = remember { mutableStateOf(false) }
        val view = LocalView.current
        DisposableEffect(view) {
            val onGlobalListener = ViewTreeObserver.OnGlobalLayoutListener {
                val rect = Rect()
                view.getWindowVisibleDisplayFrame(rect)
                val screenHeight = view.rootView.height
                val keypadHeight = screenHeight - rect.bottom
                keyboardState.value = keypadHeight > screenHeight * 0.15
            }
            view.viewTreeObserver.addOnGlobalLayoutListener(onGlobalListener)

            onDispose {
                view.viewTreeObserver.removeOnGlobalLayoutListener(onGlobalListener)
            }
        }
        return keyboardState
    }