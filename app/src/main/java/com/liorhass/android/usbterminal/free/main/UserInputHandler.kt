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
package com.liorhass.android.usbterminal.free.main

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.liorhass.android.usbterminal.free.settings.model.SettingsRepository
import com.liorhass.android.usbterminal.free.usbcommservice.UsbCommService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber


class UserInputHandler(
    settingsRepository: SettingsRepository,
    viewModelScope: CoroutineScope,
) {
    var usbCommService: UsbCommService? = null

    @Suppress("PrivatePropertyName")
    private val BASELINE_TXT_LEN = 5
    private val baselineTextToXmitCharByChar = arrayOf("1    ", "2    ") // String's length must match BASELINE_TXT_LEN
    private val baselineMarks = arrayOf('1', '2')
    private var lastBaselineUsed = 0
    // Text displayed in the "CharByCharTextToXmitInputField" text field.
    private val _textToXmitCharByChar = mutableStateOf(TextFieldValue(
        text = baselineTextToXmitCharByChar[lastBaselineUsed],
        selection = TextRange(baselineTextToXmitCharByChar[lastBaselineUsed].length) // Position cursor
    ))
    val textToXmitCharByChar: State<TextFieldValue> = _textToXmitCharByChar

    private val _ctrlButtonIsSelected = mutableStateOf(false)
    val ctrlButtonIsSelected: State<Boolean> = _ctrlButtonIsSelected

    private var bytesSentByEnterKey = SettingsRepository.DefaultValues.bytesSentByEnterKey
    init {
        val settingsStateFlow = settingsRepository.settingsStateFlow
        viewModelScope.launch {
            settingsStateFlow.collect {
                bytesSentByEnterKey = it.bytesSentByEnterKey
            }
        }
    }

    // For when we're working at InputMode.CHAR_BY_CHAR mode.
    // Operation principle: Initially we set here _textToXmitCharByChar to a known
    // five characters string. This is then set by TerminalScreen to be the value
    // of the TextField that receives the user keyboard input. Whenever the content
    // of this TextField is changed (due to keyboard input), we get notified by a
    // call to this function.
    // Here we compare the current content of the TextField to the content we
    // previously set there, and from that we can deduce what was the user's input.
    // Then we reset the TextField's content, and we're ready for the next user's
    // input.
    // In practice, things are a bit more complicated than that because sometimes
    // when there's fast user input, we may get called here a second time before
    // the UI had enough time for recomposition. For example, lets assume that the
    // initial content of the TextField is "xxxxx". If the user types ABCDEFGHIJ
    // very fast, we may get a call with xxxxxABCDE and then again with
    // xxxxxABCDEFGHIJ instead of the xxxxxFGHIJ we expect.
    // In order to overcome this issue, we use two different initialization strings
    // e.g. "1xxxx" and "2xxxx" and we alternate between them every time we
    // initialize the content of the TextField. This way we can distinguish
    // between a nominal situation and a "double call" situation.
    private var presetTextLen = baselineTextToXmitCharByChar[0].length
    fun onXmitCharByCharKBInput(tfv: TextFieldValue) {
        if (tfv.text.first() == baselineMarks[lastBaselineUsed]) {
            // This is a nominal situation
            presetTextLen = BASELINE_TXT_LEN
        }
        val bytesToXmit: ByteArray = run {
            val nNewChars = tfv.text.length - presetTextLen
//            Timber.d("onXmitCharByCharKBInput(): nNewChars=$nNewChars text='${tfv.text}'")
            if (nNewChars < 0) {
                // We have less characters than were pre-set, it indicates a backspace
//                Timber.d("Got ${-nNewChars} BACKSPACE(s)")
                ByteArray(-nNewChars) { '\b'.code.toByte() }
            } else if (nNewChars > 0) {
                ByteArray(nNewChars) { i ->
                    val b = tfv.text[presetTextLen + i].code.toByte()
                    if (_ctrlButtonIsSelected.value) {
                        when (b) {
                            in 64..95 -> { // Capital letters and some misc
                                _ctrlButtonIsSelected.value = false // Turn off the Ctrl button on any char input
                                (b - 64).toByte()
                            }
                            in 96..127 -> { // Lower-case letters and some misc
                                _ctrlButtonIsSelected.value = false // Turn off the Ctrl button on any char input
                                (b - 96).toByte()
                            }
                            else -> { // These don't map to controls
                                b
                            }
                        }
                    } else {
                        b
                    }
                }
            } else {
                ByteArray(0)
            }.also {
                presetTextLen = tfv.text.length
            }
        }
        if (bytesToXmit.isNotEmpty()) {
            usbCommService?.sendUsbData(processSendBuf(bytesToXmit))
        }
        lastBaselineUsed = 1-lastBaselineUsed
        _textToXmitCharByChar.value = tfv.copy(text = baselineTextToXmitCharByChar[lastBaselineUsed], selection = TextRange(BASELINE_TXT_LEN))
    }

    fun onCtrlKeyButtonClick() {
        Timber.d("onCtrlKeyButtonClick()")
        _ctrlButtonIsSelected.value = ! _ctrlButtonIsSelected.value
    }

    fun onUpButtonClick() {
        Timber.d("onUpButtonClick")
        val bytesToXmit = byteArrayOf(0x1B, 0x5B, 0x41) // Esc [ A
        usbCommService?.sendUsbData(bytesToXmit)
    }
    fun onDownButtonClick() {
        Timber.d("onDownButtonClick")
        val bytesToXmit = byteArrayOf(0x1B, 0x5B, 0x42) // Esc [ B
        usbCommService?.sendUsbData(bytesToXmit)
    }
    fun onRightButtonClick() {
        Timber.d("onRightButtonClick")
        val bytesToXmit = byteArrayOf(0x1B, 0x5B, 0x43) // Esc [ C
        usbCommService?.sendUsbData(bytesToXmit)
    }
    fun onLeftButtonClick() {
        Timber.d("onLeftButtonClick")
        val bytesToXmit = byteArrayOf(0x1B, 0x5B, 0x44) // Esc [ D
        usbCommService?.sendUsbData(bytesToXmit)
    }

    private fun processSendBuf(buf: ByteArray): ByteArray {
        @Suppress("CascadeIf")
        if (bytesSentByEnterKey == SettingsRepository.BytesSentByEnterKey.CR_LF) {
//            Timber.d("processSendBuf() INPUT vvvvvvvvvvvvv")
//            Timber.d(HexDump.dumpHexString(buf))
//            Timber.d("^^^^^^^^^^^^^")
            val nExtraBytes = buf.count { it == 0x0A.toByte() }
            if (nExtraBytes == 0) {
                return buf
            }
            val result = ByteArray(buf.size + nExtraBytes)
            var i = 0
            buf.forEach {
                if (it == 0x0A.toByte()) { // LF
                    result[i++] = 0x0D.toByte() // CR
                    result[i++] = 0x0A.toByte() // LF
                } else {
                    result[i++] = it
                }
            }
//            Timber.d("processSendBuf() OUTPUT vvvvvvvvvvvvv")
//            Timber.d(HexDump.dumpHexString(result))
//            Timber.d("^^^^^^^^^^^^^")
            return result
        } else if (bytesSentByEnterKey == SettingsRepository.BytesSentByEnterKey.CR) {
//            Timber.d("processSendBuf() INPUT vvvvvvvvvvvvv")
//            Timber.d(HexDump.dumpHexString(buf))
//            Timber.d("^^^^^^^^^^^^^")
            val nNewLineChars = buf.count { it == 0x0A.toByte() }
            if (nNewLineChars == 0) {
                return buf
            }
            val result = ByteArray(buf.size)
            var i = 0
            buf.forEach {
                if (it == 0x0A.toByte()) { // LF
                    result[i++] = 0x0D.toByte() // CR
                } else {
                    result[i++] = it
                }
            }
//            Timber.d("processSendBuf() OUTPUT vvvvvvvvvvvvv")
//            Timber.d(HexDump.dumpHexString(result))
//            Timber.d("^^^^^^^^^^^^^")
            return result
        } else {
            // On send LF as LF we don't have to do anything
            return buf
        }
    }
}

