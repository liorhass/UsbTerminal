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

import com.liorhass.android.usbterminal.free.settings.model.SettingsRepository
import timber.log.Timber
import java.lang.Integer.max
import java.lang.StringBuilder

// Supported sequences:
// \n - new line
// \r - carriage return
// \b - backspace
// '\u0007' - Bell (make sound)
// Esc [ Pn A - Cursor Up. Pn is considered 1 if it's missing, 0 or 1.
// Esc [ Pn B - Cursor Down. Pn is considered 1 if it's missing, 0 or 1.
// Esc [ Pn C - Cursor Forward. Pn is considered 1 if it's missing, 0 or 1.
// Esc [ Pn D - Cursor Backward. Pn is considered 1 if it's missing, 0 or 1.
// Esc [ Ps K - Ps 0 or missing - Erase from cursor to EOL
//              Ps 1 - Erase from beginning of line to cursor
//              Ps 2 - Erase line containing cursor
// Esc [ Ps J - Ps 0 or missing - Erase from cursor to end of screen
//              Ps 2 - Erase entire screen
// Esc H      - Cursor to Home Position. (Same as Esc[H)
// Esc [ Pn ; Pn H - Cursor Position. First parameter is line number and the second is column
// Esc [ 6 n  - DSR (Device Status Report) - reports the current cursor position as
//              ESC [ n ; m R   where n is the row and m is the column
//
enum class StateOfStateMachine {
    IDLE,
    RECEIVED_ESC,
    RECEIVED_ESC_AND_BRACKET,
}
class ScreenTextModelStateMachine(
    private val screenTextModel: ScreenTextModel,
    private val cursorPosition: ScreenTextModel.CursorPosition,
) {
    private var state: StateOfStateMachine = StateOfStateMachine.IDLE
    private var param1 = StringBuilder(4)
    private var params = mutableListOf<Int>() // For use with multiple params that are separated by ';'
    private var keepParams = false
    var silentlyDropUnrecognizedCtrlChars = SettingsRepository.DefaultValues.silentlyDropUnrecognizedCtrlChars

    /**
     * Process a byte received from the serial device
     *
     * Note: Must be called from within a withLock block (vfy)
     *
     * @param byte The input byte to process
     * @param result A CharArray containing the processing result that should be written on the screen
     *
     * @return The number of chars in result. Can be 0 if nothing should be written on the screen
     */
    fun onNewByte(byte: Byte, dataWasAlreadyProcessed: Boolean, result: CharArray): Int {
        val c = iso_8859_1[byte.toUByte().toInt()]  // Byte to char with specific encoding

        return when (state) {
            StateOfStateMachine.IDLE -> {
                if (c.code >= 32) {
                    // The vast majority of received bytes fall here ("regular" chars). As an
                    // optimization, we do without the call to onNewCharWhenIdle() by
                    // handling it here.
                    result[0] = c
                    1
                } else {
                    onNewControlCharWhenIdle(c, result)
                }
            }
            StateOfStateMachine.RECEIVED_ESC -> onNewCharWhenReceivedEsc(c, result)
            StateOfStateMachine.RECEIVED_ESC_AND_BRACKET -> onNewCharWhenReceivedEscAndBracket(c, dataWasAlreadyProcessed, result)
        }
    }

    // Note: Must be called from within a withLock block (vfy)
    private fun onNewCharWhenIdle(c: Char, result: CharArray): Int {
        return if (c.code < 32) { // Control characters
            onNewControlCharWhenIdle(c, result)
        } else {
            result[0] = c
            1
        }
    }

    // Note: Must be called from within a withLock block (vfy)
    private fun onNewControlCharWhenIdle(c: Char, result: CharArray): Int {
        return when (c) {
            '\u001B' -> { // Esc
                state = StateOfStateMachine.RECEIVED_ESC
                0
            }
            '\n' -> {
                screenTextModel.onNewLine()
                0
            }
            '\b' -> {
                cursorPosition.onBackspace()
                0
            }
            '\r' -> {
                cursorPosition.onCarriageReturn()
                0
            }
            '\t' -> {
                cursorPosition.moveRightToNextTabStop()
                screenTextModel.extendCurrentLineUpToCursor()
                0
            }
            '\u0007' -> { // Bell
                screenTextModel.beep()
                0
            }
            else -> { // All unsupported control chars are displayed as ⸮ (Unicode character U+2E2E) unless configured to be ignored
                if (silentlyDropUnrecognizedCtrlChars) {
                    0
                } else {
                    result[0] = '\u2e2e'
                    1
                }
            }
        }
    }

    // Already received Esc
    // Note: Must be called from within a withLock block (vfy)
    private fun onNewCharWhenReceivedEsc(c: Char, result: CharArray): Int {
        return if (c == '[') {
            state = StateOfStateMachine.RECEIVED_ESC_AND_BRACKET
            0
        } else if (c == 'H') {
            state = StateOfStateMachine.IDLE
            screenTextModel.positionCursorInDisplayedWindow(listOf(0, 0)) // Cursor to home
            0
        } else {
            state = StateOfStateMachine.IDLE
            result[0] = '^'
            result[1] = '[' // We print Esc as "^["
            val tmp = CharArray(1)
            val l = onNewCharWhenIdle(c, tmp)
            if (l > 0) {
                result[2] = tmp[0]
                3
            } else {
                2
            }
        }
    }

    /**
     * Already received Esc[
     *
     * @return The number of chars in result that should be written on screen.
     * Can be 0 if nothing to write to screen
     */
    private fun onNewCharWhenReceivedEscAndBracket(
        c: Char, dataWasAlreadyProcessed: Boolean, result: CharArray): Int {
        return if ((c in '0'..'9' || (c == '-' && param1.isEmpty())) && param1.length <= 3) {
            param1.append(c)
            0
        } else {
            val pn1 = param1.getNumericValue()
            keepParams = false
            state = StateOfStateMachine.IDLE
            var rc = 0
            when (c) {
                'A' -> { // CUU Cursor Up
                    cursorPosition.moveUp(max(1, pn1))
                    screenTextModel.extendCurrentLineUpToCursor()
                }
                'B' -> { // CUD Cursor Down
                    cursorPosition.moveDown(max(1, pn1))
                    screenTextModel.extendCurrentLineUpToCursor()
                }
                'C' -> { // CUF Cursor Forward
                    cursorPosition.moveRight(max(1, pn1))
                    screenTextModel.extendCurrentLineUpToCursor()
                }
                'D' -> { // CUB Cursor Back
                    cursorPosition.moveLeft(max(1, pn1))
                }
                'H' -> { // CUP Cursor Position
                    params.add(pn1)
                    screenTextModel.positionCursorInDisplayedWindow(params)
                }
                'J' -> { // ED Erase in Display
                    screenTextModel.eraseDisplay(pn1)
                }
                'K' -> { // EL Erase in Line. 0: from cursor, 1: to cursor, 2: entire line
                    screenTextModel.eraseLine(pn1)
                }
                ';' -> {
                    params.add(pn1)
                    if (params.size > 10) {
                        Timber.w("onNewCharWhenReceivedEscAndBracket() Too many parameters")
                        params.clear()
                    } else {
                        keepParams = true
                        state = StateOfStateMachine.RECEIVED_ESC_AND_BRACKET
                    }
                }
                'm' -> { // SGR Select Graphic Rendition
                    params.add(pn1)
                    screenTextModel.selectGraphicRendition(params, alsoRememberRendition = true)
                }
                'n' -> { // DSR Device Status Report
                    // If dataWasAlreadyProcessed we already handled this DSR. Should ignore
                    // it this time. This happens (for example) when we're redrawing the screen
                    if (! dataWasAlreadyProcessed) {
                        screenTextModel.doDeviceStatusReport(pn1)
                    }
                }
                else -> {
                    // TODO: currently this ignores multiple parameters separated by ';'
                    result[0] = '^'; result[1] = '[' // We print Esc as "^["
                    result[2] = '['
                    var i = 3
                    param1.forEach { c2 ->
                        result[i++] = c2
                    }
                    result[i++] = c
                    rc = i
                }
            }
            param1.clear()
            if (! keepParams) params.clear()
            rc
        }
    }
}

