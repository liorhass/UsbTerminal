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

/*  Linenoise lib escape sequences:
* See https://vt100.net/docs/vt100-ug/chapter3.html
* List of escape sequences used by this program, we do everything just
* with three sequences. In order to be so cheap we may have some
* flickering effect with some slow terminal, but the lesser sequences
* the more compatible.
*
* EL (Erase Line)
*    Sequence: ESC [ n K
*    Effect: if n is 0 or missing, clear from cursor to end of line
*    Effect: if n is 1, clear from beginning of line to cursor
*    Effect: if n is 2, clear entire line
*
* CUF (Cursor Forward)
*    Sequence: ESC [ n C
*    Effect: moves cursor forward n chars
*
* CUB (Cursor Backward)
*    Sequence: ESC [ n D
*    Effect: moves cursor backward n chars
*
* The following is used to get the terminal width if getting
* the width with the TIOCGWINSZ ioctl fails
*
* DSR (Device Status Report)
*    Sequence: ESC [ 6 n
*    Effect: reports the current cursor position as ESC [ n ; m R
*            where n is the row and m is the column
*
* When multi line mode is enabled, we also use an additional escape
* sequence. However multi line editing is disabled by default.
*
* CUU (Cursor Up)
*    Sequence: ESC [ n A
*    Effect: moves cursor up of n chars.
*
* CUD (Cursor Down)
*    Sequence: ESC [ n B
*    Effect: moves cursor down of n chars.
*
* When linenoiseClearScreen() is called, two additional escape sequences
* are used in order to clear the screen and position the cursor at home
* position.
*
* Cursor to home
*    Sequence: ESC H
*    Effect: moves the cursor to upper left corner (of visible screen - scroll-back buffer remains as is)
*
* CUP (Cursor position)
*    Sequence: ESC [ H
*    Effect: moves the cursor to upper left corner (of visible screen - scroll-back buffer remains as is)
*
*    Sequence: ESC [ l ; c H
*    Effect: moves the cursor to the specified line and column  (of visible screen
*            - scroll-back buffer remains as is)
*
* ED (Erase display)
*    Sequence: ESC [ p J
*    If p is 0 - Clear from cursor to end of screen (including under the cursor). Cursor stays where it is
*    If p is 2 - Clear the whole screen. Cursor stays where it is
*/

package com.liorhass.android.usbterminal.free.main

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import com.liorhass.android.usbterminal.free.settings.model.SettingsRepository
import com.liorhass.android.usbterminal.free.ui.theme.CursorColorForDarkTheme
import com.liorhass.android.usbterminal.free.ui.theme.CursorColorForLightTheme
import com.liorhass.android.usbterminal.free.usbcommservice.IOPacketsList
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.lang.Integer.max
import java.lang.Integer.min
import java.lang.StringBuilder
import java.util.concurrent.atomic.AtomicBoolean

class ScreenTextModel(
    private val coroutineScope: CoroutineScope,
    private var maxLineLen: Int,
    private val sendBuf: (buf: ByteArray) -> Unit,
    maxTotalSize: Int,
) {
    private companion object {
        const val TOTAL_SIZE_TRIMMING_HYSTERESIS = 1000
    }

    /** Cursor position. `line` and `column` start at 1. For displaying at the status-line */
    data class DisplayedCursorPosition(val line: Int, val column: Int)

    private var uid: Int = 1
        get() { field++; return field }

    // Why is the flags shouldScrollToBottom an Int and not a Boolean?
    // Well, it's a hack we have to use as a punishment for using a system
    // designed to represent a state (Jetpack Compose) to pass a one-of commands.
    // if it was a Boolean the following scenario might happen:
    //  1. A trigger is made to scroll to bottom, which sets the flag to true
    //  2. As a result recomposition happens and it scrolls to bottom
    //  3. After that a call is made to onScrolledToBottom() on a LaunchedEffect
    //     which reset the flag to false
    //  4. Before a recomposition happens (due to the flag reset) some other code
    //     adds content and triggers another scroll to bottom, which sets the flag
    //     to true again
    //  5. Finally the UI is ready to recompose, but from Compose's perspective
    //     recomposition isn't needed since the flag never changed (was true and is
    //     still true).
    // The solution is to use an Int instead of a Boolean. 0 marks that no scroll
    // is needed, and when a scroll is needed the flag is set to some unique
    // Int (different every time). The composition makes (totally dummy) use of this Int
    // which triggers a scroll even when the flag's reset is missed.
    data class ScreenState(
        val lines: Array<ScreenLine>,
        val displayedCursorPosition: DisplayedCursorPosition,
        val shouldScrollToBottom: Int, // For an explanation why this is Int and not Boolean see above
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ScreenState

            if (!(lines === other.lines)) return false
            if (displayedCursorPosition != other.displayedCursorPosition) return false
            if (shouldScrollToBottom != other.shouldScrollToBottom) return false

            return true
        }

        override fun hashCode(): Int {
            var result = lines.contentHashCode()
            result = 31 * result + displayedCursorPosition.hashCode()
            result = 31 * result + shouldScrollToBottom.hashCode()
            return result
        }
    }

    private val _screenState = mutableStateOf(ScreenState(
        lines = emptyArray(),
        displayedCursorPosition = DisplayedCursorPosition(0, 0),
        shouldScrollToBottom = 0,
    ))
    val screenState: State<ScreenState> = _screenState
    fun shouldScrollToBottom() {
        _screenState.value = _screenState.value.copy(shouldScrollToBottom = uid)
    }
    fun onScrolledToBottom(uid: Int) {
        if (uid == _screenState.value.shouldScrollToBottom) {
            _screenState.value = _screenState.value.copy(shouldScrollToBottom = 0)
        }
    }

    /**
     * The main content of the terminal screen. Internally we hold the screen content
     * on a `mutableListOf<ScreenLine>`. For the UI composables, we expose a
     * `State<Array<ScreenLine>>`. Every time we want to update the UI we do
     * a shallow copy of the list to a new array.
     * I've tried using a `mutableStateListOf<ScreenLine>` instead (that's a
     * list that triggers its observers on every change). This was easier but
     * performance were awful.
     */
    private val screenLines = mutableListOf<ScreenLine>()
    // private val _screenLinesState = mutableStateOf<Array<ScreenLine>>(emptyArray())
    // val screenLinesState: State<Array<ScreenLine>> = _screenLinesState
    private var totalCharCount = 0

    // These are set by MainViewModel whenever settings are changed
    var soundOn = SettingsRepository.DefaultValues.soundOn
    var silentlyDropUnrecognizedCtrlChars: Boolean = SettingsRepository.DefaultValues.silentlyDropUnrecognizedCtrlChars
        set(value) {
            field = value
            stateMachine.silentlyDropUnrecognizedCtrlChars = value
        }

    private val uiUpdateTriggered = AtomicBoolean(false)
    private val missedScrollToBottom = AtomicBoolean(false)
    private var screenHeight = 0 // Number of lines that fit in the screen (visible lines)
    private val currentGraphicRendition = mutableListOf<Int>()
    private val cursor = Cursor()
    private val stateMachine = ScreenTextModelStateMachine(this, cursor.position)
    private var totalSizeUpperLimit: Int = maxTotalSize + maxLineLen

    private val mutex = Mutex()

    init {
        clear()
    }

    fun onStart() {
        cursor.onStart()
    }
    fun onStop() {
        cursor.onStop()
        clear()
    }

    fun setMaxTotalSize(newMaxTotalSize: Int) {
        coroutineScope.launch {
            mutex.withLock {
                totalSizeUpperLimit = newMaxTotalSize + maxLineLen
                trimIfNeeded()
            }
        }
    }

    fun clear() {
        coroutineScope.launch {
            mutex.withLock {
                cursor.isBlinking = false // Stop cursor and hide it so it won't access a non-existent line after we delete all lines
                cursor.hide()
                screenLines.clear()
                totalCharCount = 0
                appendNewLine() // We start with one empty line
                putCharAtCursorLocation(' ') // One space character to enable cursor drawing
                _screenState.value = ScreenState(
                    lines = screenLines.toTypedArray(),
                    shouldScrollToBottom = 0,
                    displayedCursorPosition = DisplayedCursorPosition(
                        cursor.position.lineIndex + 1,
                        cursor.position.offsetInLine + 1)
                )
                cursor.isBlinking = true
            }
        }
    }

    /** The line where the cursor is */
    private val currentLine: ScreenLine
        get() = screenLines[cursor.position.lineIndex]

    /** Set screen dimensions in characters */
    fun setScreenDimensions(width: Int, height: Int) {
        screenHeight = height
        if (width != maxLineLen) {
            coroutineScope.launch {
                mutex.withLock {
                    maxLineLen = width
                    screenLines.forEach { screenLine ->
                        screenLine.setMaxLineLength(maxLineLen)
                    }
                }
            }
        }
    }

    fun setIsDarkTheme(isDarkTheme: Boolean) {
        val cursorColor = if (isDarkTheme) {
            CursorColorForDarkTheme
        } else {
            CursorColorForLightTheme
        }
        cursor.setColor(cursorColor)
    }

    fun onNewData(data: ByteArray,
                  offset: Int,
                  dataDirection: IOPacketsList.DataDirection,
                  dataWasAlreadyProcessed: Boolean) {
        // Timber.d("ScreenTextModel::onNewData(): adding ${data.size-offset} bytes  direction:${dataDirection.name}")
        if (dataDirection == IOPacketsList.DataDirection.OUT) {
            return // We only display incoming data so ignore any outgoing data
        }
        var remainingBytes = data.size - offset
        if (remainingBytes <= 0) {
            Timber.w("onNewData(): remainingBytes=$remainingBytes")
            return
        }
        var inputByteOffset = offset
        coroutineScope.launch {
            mutex.withLock {
                cursor.hide()
                while (remainingBytes > 0) {
                    processReceivedByte(data[inputByteOffset], dataWasAlreadyProcessed)

                    inputByteOffset++
                    remainingBytes--
                }
            }
            updateUi(alsoScrollToBottomOfScreen = true)
        }
    }

    private val resultOfByteProcessing = CharArray(64)
    // Note: Must be called from within a withLock block (vfy)
    private fun processReceivedByte(byte: Byte, dataWasAlreadyProcessed: Boolean) {
        val nCharsToDisplay = stateMachine.onNewByte(
            byte, dataWasAlreadyProcessed, resultOfByteProcessing)

        for (i in 0 until nCharsToDisplay) {
            putCharAtCursorLocation(resultOfByteProcessing[i], advanceCursorPosition = true)
        }
    }

    /**
     * Insert char at cursor position. Append new line if needed.
     * Optionally advance cursor to next position
     *
     * Note: Must be called from within a withLock block (vfy)
     */
    private fun putCharAtCursorLocation(c: Char, advanceCursorPosition: Boolean = false) {
        val lineSizeDelta = currentLine.putCharAt(c, cursor.position.offsetInLine, advanceCursorPosition)
        if (lineSizeDelta == -1) {
            // Cursor position is beyond line's end
            onNewLine()
            currentLine.putCharAt(c, cursor.position.offsetInLine, advanceCursorPosition)
        } else {
            totalCharCount += lineSizeDelta
        }
        if (advanceCursorPosition) {
            cursor.position.offsetInLine++
            if (cursor.position.offsetInLine >= currentLine.textLength) {
                // Cursor position is beyond line's text end. In order to draw cursor,
                // we add a space (so it can be underscored)
                putCharAtCursorLocation(' ') // This appends a new line when current line's end reached
            }
        }
    }

    /** Append spaces to end of current line up to cursor location (inclusive) */
    fun extendCurrentLineUpToCursor() {
        currentLine.appendSpacesUpToLocation(cursor.position.offsetInLine)
    }

    /**
     * Move cursor position to beginning of next line. If that line
     * doesn't exist, append a new line
     *
     * Note: Must be called from within a withLock block (vfy)
     */
    fun onNewLine() {
        cursor.position.onNewLine()
        if (cursor.position.lineIndex >= screenLines.size) {
            appendNewLine()
        }
    }

    /**
     * Append a new line to screenLines, and update the cursor position to point
     * to its beginning
     *
     * Note: Must be called from within a withLock block (vfy)
     */
    private fun appendNewLine() {
       // Timber.d("appendNewLine()")
        screenLines.add(ScreenLine(" ", maxLineLen))
        totalCharCount++
        trimIfNeeded()
        cursor.position.setPosition(lineIndex = screenLines.size-1, offsetInLine = 0)
        selectGraphicRendition(currentGraphicRendition, false)
    }

    // Must be called from within a withLock block (vfy)
    private fun trimIfNeeded() {
        if (totalCharCount > totalSizeUpperLimit + TOTAL_SIZE_TRIMMING_HYSTERESIS) {
            Timber.d("Trimming totalCharCount=$totalCharCount  totalSizeUpperLimit=$totalSizeUpperLimit  nLines=${screenLines.size}")
            while (totalCharCount - screenLines.first().textLength > totalSizeUpperLimit) {
                Timber.d("Deleting 1st line. txtLength=${screenLines.first().textLength}")
                totalCharCount -= screenLines.removeFirst().textLength
            }
        }
    }

    private val colors30to37 = arrayOf(0xFF000000, 0xFFBB0000, 0xFF00BB00, 0xFFBBBB00, 0xFF0000BB, 0xFFBB00BB, 0xFF00BBBB, 0xFFBBBBBB)
    private val colors90to97 = arrayOf(0xFF555555, 0xFFFF5555, 0xFF55FF55, 0xFFFFFF55, 0xFF5555FF, 0xFFFF55FF, 0xFF55FFFF, 0xFFFFFFFF)
    fun selectGraphicRendition(params: List<Int>, alsoRememberRendition: Boolean) {
        params.forEach {
            when (it) {
                0 -> {
                    currentLine.endAllSpanStyles(cursor.position.offsetInLine)
                    if (alsoRememberRendition) currentGraphicRendition.clear()
                }
                in 30..37 -> {
                    currentLine.endAllSpanStylesByTag(cursor.position.offsetInLine, "clr") // Start of color terminates any previous color
                    currentLine.startSpanStyle(
                        style  = SpanStyle(color = Color(colors30to37[it-30])),
                        index = cursor.position.offsetInLine,
                        tag = "clr",
                    )
                    if (alsoRememberRendition) currentGraphicRendition.add(it)
                }
                in 90..97 -> {
                    currentLine.endAllSpanStylesByTag(cursor.position.offsetInLine, "clr") // Start of color terminates any previous color
                    currentLine.startSpanStyle(
                        style  = SpanStyle(color = Color(colors90to97[it-90])),
                        index = cursor.position.offsetInLine,
                        tag = "clr",
                    )
                    if (alsoRememberRendition) currentGraphicRendition.add(it)
                }
                49 -> {
                    // This code is "Default background color". The linenoise library sometimes
                    // sends this code. Since we don't support changing background color, we
                    // can simply ignore this code.
                }
                else -> Timber.w("selectGraphicRendition: Unsupported parameter: $it")
            }
        }
    }

    private suspend fun updateUi(alsoScrollToBottomOfScreen: Boolean) {
        // Optimization: We only trigger a recomposition of the screen once every
        // 40mSec. Data may be coming very fast which causes very fast refresh of
        // the screen content (consuming lots of resources). There is no point in
        // updating the screen more than 25 times a second anyway.
        if (uiUpdateTriggered.compareAndSet(false, true)) {
            coroutineScope.launch {
                delay(40)
                uiUpdateTriggered.set(false)
                mutex.withLock {
                    // Timber.d("updateUi(): screenLines.size: ${screenLines.size}  shouldScrollToBottom=${alsoScrollToBottomOfScreen}")
                    val shouldScrollToBottom = if (alsoScrollToBottomOfScreen || missedScrollToBottom.getAndSet(false)) uid else _screenState.value.shouldScrollToBottom
                    _screenState.value = ScreenState(
                        lines = screenLines.toTypedArray(),
                        shouldScrollToBottom = shouldScrollToBottom,
                        displayedCursorPosition = DisplayedCursorPosition(
                            cursor.position.lineIndex + 1,
                            cursor.position.offsetInLine + 1
                        )
                    )
                }
            }
        } else {
            if (alsoScrollToBottomOfScreen) {
                missedScrollToBottom.set(true)
            }
        }
    }

    /**
     * Hold current cursor position and make it blink
     */
    private inner class Cursor {
        val position = CursorPosition(lineIndex = 0, offsetInLine = 0)
        private val lastDisplayedPosition = CursorPosition(lineIndex = 0, offsetInLine = 0)
        private var cursorCurrentlyShown = false
        private var spanStyleUnderscore = SpanStyle(textDecoration = TextDecoration.Underline, color = Color.Transparent)
        var isBlinking = true

        fun setColor(color: Color) {
            spanStyleUnderscore = SpanStyle(textDecoration = TextDecoration.Underline, color = color)
        }

        private fun show() {
            if (position.lineIndex >= screenLines.size)
                return // Can happen at startup or after screen clear
            lastDisplayedPosition.setPosition(position)
            screenLines[position.lineIndex].addSpanStyle(
                AnnotatedString.Range(
                    start = position.offsetInLine,       // Inclusive
                    end   = position.offsetInLine + 1,   // Exclusive
                    item  = spanStyleUnderscore,
                    tag   = "crsr"
                ),
                true
            )
            cursorCurrentlyShown = true
//            Timber.d("Cursor::show(): position: ${position.lineIndex},${position.offsetInLine}")
        }

        fun hide() {
            if (cursorCurrentlyShown) {
                screenLines[lastDisplayedPosition.lineIndex].removeSpanStyleByTag("crsr", true)
//                Timber.d("Cursor::hide(): position: ${position.lineIndex},${position.offsetInLine}")
                cursorCurrentlyShown = false
            }
        }

        var job: Job? = null
        fun onStart() {
            // Constantly blink the cursor
            job = coroutineScope.launch {
                while (isActive) {
                    if (isBlinking) {
                        if (cursorCurrentlyShown)
                            mutex.withLock { hide() }
                        else
                            mutex.withLock { show() }
                        updateUi(alsoScrollToBottomOfScreen = false)
                    }
                    delay(500)
                }
            }
        }
        fun onStop() {job?.cancel()}
    }
    // This would have been more elegant nested in class Cursor, but it's impossible due
    // to some kotlin limitation. See https://stackoverflow.com/q/51758115/1071117
    inner class CursorPosition(
        var lineIndex: Int,
        /** Cursor position in the line (zero-based. I.e. 1st location's index is 0) */
        var offsetInLine: Int,
    ) {
        fun setPosition(lineIndex: Int, offsetInLine: Int) {this.lineIndex=lineIndex; this.offsetInLine=offsetInLine}
        fun setPosition(other: CursorPosition) {this.lineIndex=other.lineIndex; this.offsetInLine=other.offsetInLine}
        /** Move cursor one line downwards (may point to a non existent line) and to the line's beginning */
        fun onNewLine() { lineIndex++; offsetInLine=0 }
        /** Move cursor to the line's beginning */
        fun onCarriageReturn() { offsetInLine=0 }
        /** Move cursor one place to the left. If already at line's beginning do nothing */
        fun onBackspace() { if (offsetInLine > 0) offsetInLine-- }

        /** Move cursor n places to the left. If already at line's beginning do nothing */
        fun moveLeft(n: Int) {
            if (n < 0) {
                moveRight(-n)
            } else {
                offsetInLine = max(0, offsetInLine - n)
            }
        }
        /** Move cursor n places to the right but no more than maxLineLength */
        fun moveRight(n: Int) {
            if (n < 0) {
                moveLeft(-n)
            } else {
                offsetInLine = min(maxLineLen - 1, offsetInLine + n)
            }
        }
        /** Move cursor to next tab-stop (n*8) but no more than maxLineLength */
        fun moveRightToNextTabStop() {
            offsetInLine = min(maxLineLen - 1, (offsetInLine + 8) and 0xFFF8)
        }
        /** Move cursor n places downwards. */
        fun moveDown(n: Int) { lineIndex = min(screenLines.size - 1, lineIndex + n) }
        /** Move cursor n places upwards. */
        fun moveUp(n: Int) { lineIndex = max(0, lineIndex - n) }

        fun copy(): CursorPosition {return CursorPosition(lineIndex, offsetInLine)}
    }


    internal fun eraseLine(n: Int) {
        when (n) {
            0 -> { // Clear from cursor to end of line (including under the cursor). Cursor stays where it is
                totalCharCount -= screenLines[cursor.position.lineIndex].clearFromInclusive(cursor.position.offsetInLine)
            }
            1 -> { // Clear from beginning of line to cursor (including under the cursor). Cursor stays where it is
                screenLines[cursor.position.lineIndex].clearToInclusive(cursor.position.offsetInLine)
            }
            2 -> { // Clear entire line (including under the cursor). Cursor stays where it is (spaces to its left)
                totalCharCount -= screenLines[cursor.position.lineIndex].clearAndTruncateTo(cursor.position.offsetInLine)
            }
        }
    }

    // ESC [ Ps J
    internal fun eraseDisplay(ps: Int) {
        when (ps) {
            0 -> { // Clear from cursor to end of screen (including under the cursor). Cursor stays where it is
                eraseLine(0) // Erase line where cursor is located from cursor to EOL
                for (i in (cursor.position.lineIndex + 1)..screenLines.lastIndex) {
                    totalCharCount -= screenLines[i].clear()
                }
            }
            2 -> { // Clear the whole (visible) screen. Cursor stays where it is
                val currentCursorPosition = cursor.position.copy()
                positionCursorInDisplayedWindow(1, 1)
                eraseDisplay(0)
                // Fill cursor's line with spaces up to the cursor, so it can be drawn
                totalCharCount -= screenLines[currentCursorPosition.lineIndex].clearAndTruncateTo(currentCursorPosition.offsetInLine)
                cursor.position.setPosition(currentCursorPosition)
            }
        }
    }

    internal fun beep(durationMs: Int = 200) {
        if (soundOn) {
            try {
                ToneGenerator(AudioManager.STREAM_NOTIFICATION, ToneGenerator.MAX_VOLUME).startTone(
                    ToneGenerator.TONE_PROP_PROMPT,
                    durationMs
                )
            } catch (e: Exception) {
                // Sometimes we get an exception in ToneGenerator's constructor. We can afford
                // to ignore this
                Timber.e("beep(): ${e.message}")
            }
        }
    }

    /**
     * Position cursor in the displayed window (back-scroll lines are ignored)
     * @param nRow Row number (starts at 1 like VT100)
     * @param nCol Column number (starts at 1 like VT100)
     */
    private fun positionCursorInDisplayedWindow(nRow: Int, nCol: Int) {
        var row = nRow
        if (row < 1) row = 1 else if (row > screenLines.size) row = screenLines.size
        var col = nCol
        if (col < 1) col = 1 else if (col > maxLineLen) col = maxLineLen
        val firstDisplayedLine = max(0, screenLines.size - screenHeight) // Assuming we've scrolled to bottom
        val targetLine = firstDisplayedLine + row - 1 // -1 because in vt100 index of 1st line is 1 not 0
        cursor.position.setPosition(targetLine, col - 1)
    }

    /**
     * Position cursor in the displayed window (back-scroll lines are ignored)
     * @param params A list<Int> of length 0,1 or 2. If it's empty cursor is positioned
     * at the screen top left. If it's length is 1 it is taken as a Row number (starts
     * at 1 like VT100), and the cursor is positioned at the beginning of that line.
     * If it's length is 2 the 2nd parameter is taken as a Column number (starts
     * at 1 like VT100), and the cursor is positioned at the specified line and column
     * numbers.
     */
    internal fun positionCursorInDisplayedWindow(params: List<Int>) {
        when {
            params.isEmpty() -> positionCursorInDisplayedWindow(1, 1)
            params.size == 1 -> positionCursorInDisplayedWindow(params[0], 1)
            params.size == 2 -> positionCursorInDisplayedWindow(params[0], params[1])
            else -> Timber.w("positionCursorInDisplayedWindow() params.size=${params.size} (should be 0,1 or 2)")
        }
    }

    internal fun doDeviceStatusReport(ps1: Int) {
        when (ps1) {
            6 -> {
                // We should send a Cursor Position Report (CPR)
                val cpr = "\u001B[${cursor.position.lineIndex + 1};${cursor.position.offsetInLine + 1}R".toByteArray() // +1 is because vt100's origin is (1,1)
                sendBuf(cpr)
            }
            5 -> {
                // We should send a Status Report (DSR)
                val dsr = "\u001B[0n".toByteArray() // DSR of "Ready, No malfunctions detected"
                sendBuf(dsr)
            }
            else -> {
                Timber.w("Received Device Status Report (DSR) sequence with unsupported Ps1: $ps1")
            }
        }
    }

}

/**
 * Get string's numerical value. An empty string's value is 0
 */
fun StringBuilder.getNumericValue(): Int {
    return when {
        isEmpty() -> 0
        else -> {
            var nv = 0
            run breakSimulationLabel@ { // kotlin doesn't have a break in forEach. We simulate this with a run block and a local return
                this.forEach {
                    if (it in '0'..'9') {
                        nv = 10 * nv + it.code - '0'.code
                    } else {
                        Timber.w("StringBuilder.getNumericValue(): Bad string: '$this'")
                        return@breakSimulationLabel
                    }
                }
            }
            nv
        }
    }
}

