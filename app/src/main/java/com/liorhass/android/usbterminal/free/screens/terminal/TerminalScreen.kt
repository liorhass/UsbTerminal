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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.liorhass.android.usbterminal.free.R
import com.liorhass.android.usbterminal.free.main.MainViewModel
import com.liorhass.android.usbterminal.free.main.ScreenTextModel
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
    val cursorPosition = if (displayType == SettingsRepository.DisplayType.TEXT) {
        mainViewModel.textScreenState.value.displayedCursorPosition
    } else {
        ScreenTextModel.DisplayedCursorPosition(0,0) // This isn't displayed anyway
    }
    val fontSize = settingsData.fontSize
    val shouldShowWelcomeMsg by mainViewModel.shouldShowWelcomeMsg
//    val shouldShowWelcomeMsg by remember { mutableStateOf(true) } // For debugging
    val shouldShowUpgradeFromV1Msg by mainViewModel.shouldShowUpgradeFromV1Msg
//    val shouldShowUpgradeFromV1Msg by remember { mutableStateOf(true) } // For debugging
    val shouldMeasureScreenDimensions by mainViewModel.shouldMeasureScreenDimensions
    val shouldReportIfAtBottom by mainViewModel.shouldReportIfAtBottom

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) //todo: should be same as screen's LazyColumn background (i.e. from config in combination with text color)
    ) {
        // This focusRequesters is how a click on the text area sets focus to the
        // TextToXmitInputField, or opens the soft keyboard
        val mainFocusRequester = remember { FocusRequester() }
        val auxFocusRequester = remember { FocusRequester() }

        if (displayType == SettingsRepository.DisplayType.HEX) {
            TerminalScreenHexSection(
                textBlocks = mainViewModel.screenHexTextBlocksState,
                shouldScrollToBottom = mainViewModel.screenHexShouldScrollToBottom,
                shouldReportIfAtBottom = shouldReportIfAtBottom,
                onReportIfAtBottom = mainViewModel::onReportIfAtBottom,
                onScrolledToBottom = mainViewModel::onScreenHexScrolledToBottom,
                fontSize = fontSize,
                mainFocusRequester = mainFocusRequester,
                auxFocusRequester = auxFocusRequester,
                onKeyboardStateChange = { mainViewModel.remeasureScreenDimensions() }
            )
        } else {
            TerminalScreenTextSection(
                screenState = mainViewModel.textScreenState,
                shouldMeasureScreenDimensions = shouldMeasureScreenDimensions,
                onScreenDimensionsMeasured = mainViewModel::onScreenDimensionsMeasured,
                shouldReportIfAtBottom = shouldReportIfAtBottom,
                onReportIfAtBottom = mainViewModel::onReportIfAtBottom,
                onScrolledToBottom = mainViewModel::onScreenTxtScrolledToBottom,
                fontSize = fontSize,
                mainFocusRequester = mainFocusRequester,
                auxFocusRequester = auxFocusRequester,
                onKeyboardStateChange = { mainViewModel.remeasureScreenDimensions() }
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
        // AnimatedVisibility(
        //     visible = mainViewModel.ctrlButtonsRowVisible.value,
        //     enter = slideInVertically(
        //         animationSpec = tween(1000),
        //         initialOffsetY = { fullHeight ->
        //             Timber.d("fullHeight=$fullHeight")
        //             fullHeight }
        //     ),
        //     exit = fadeOut(animationSpec = tween(1000))
        // ) {
        //     CtrlButtonsRow(mainViewModel, modifier = Modifier.clipToBounds())
        // }
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
}

fun openSoftKeyboard(
    coroutineScope: CoroutineScope,
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
        auxFocusRequester.requestFocus()
        delay(50)
        mainFocusRequester.requestFocus()
    }
}
