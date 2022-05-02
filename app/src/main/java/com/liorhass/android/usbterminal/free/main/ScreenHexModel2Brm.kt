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
package com.liorhass.android.usbterminal.free.main

/*
import android.icu.text.SimpleDateFormat
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import com.liorhass.android.usbterminal.free.ui.theme.*
import com.liorhass.android.usbterminal.free.usbcommservice.IOPacketsList
import com.liorhass.android.usbterminal.free.util.HexDump
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.lang.Integer.min
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

const val MIN_TIME_BETWEEN_TIME_STAMPS = 2000L

class ScreenHexModel(
    val coroutineScope: CoroutineScope,
    maxTotalSize: Int,
) {

    private val _screenHexAnnotatedStringState = mutableStateOf(AnnotatedString(""))
    val screenHexAnnotatedStringState: State<AnnotatedString> = _screenHexAnnotatedStringState

    private val _shouldScrollToBottom = mutableStateOf(false)
    val shouldScrollToBottom: State<Boolean> = _shouldScrollToBottom
    fun onScrolledToBottom() {
        _shouldScrollToBottom.value = false
    }

    private var deadHexTextAS = AnnotatedString("")
    private val latestDataBAOS = ByteArrayOutputStream(1000)
    private var latestDataDirection = IOPacketsList.DataDirection.UNDEFINED
    private var timeOfLastTimestamp = 0L
    private val timeFormatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private val uiUpdateTriggered = AtomicBoolean(false)
    private var totalSizeUpperLimit: Int = 1
    init {setMaxTotalSize(maxTotalSize)}

    fun onStart() {}
    fun onStop() {
        clear()
    }

    private var spanStyleForInputDataText = SpanStyle(color = Color.Transparent)
    private var spanStyleForOutputDataText = SpanStyle(color = Color.Transparent)
    fun setIsDarkTheme(isDarkTheme: Boolean) {
        if (isDarkTheme) {
            spanStyleForInputDataText = SpanStyle(color = ColorOfInputTextForDarkTheme)
            spanStyleForOutputDataText = SpanStyle(color = ColorOfOutputTextForDarkTheme)
        } else {
            spanStyleForInputDataText = SpanStyle(color = ColorOfInputTextForLightTheme)
            spanStyleForOutputDataText = SpanStyle(color = ColorOfOutputTextForLightTheme)
        }
    }

    fun onNewData(data: ByteArray, offset: Int, dataDirection: IOPacketsList.DataDirection, timeStamp: Long) {
        val now = System.currentTimeMillis()
        synchronized(latestDataBAOS) {
            if (now - timeOfLastTimestamp > MIN_TIME_BETWEEN_TIME_STAMPS || dataDirection != latestDataDirection) {
                if (latestDataBAOS.size() > 0) {
                    val hexDumpStr = HexDump.dumpHexString(latestDataBAOS.toByteArray(),0, latestDataBAOS.size())
                    deadHexTextAS += with(AnnotatedString.Builder(hexDumpStr.length)) {
                        pushStyle(dataDirectionToSpanStyle(latestDataDirection))
                        append(hexDumpStr)
                        pop()
                        toAnnotatedString()
                    }
                    latestDataBAOS.reset()
                }
                deadHexTextAS += with(AnnotatedString.Builder(20)) {
                    pushStyle(SpanStyle(color = Color.DarkGray))
                    append("\n${timeFormatter.format(Date(timeStamp))}\n")
                    pop()
                    toAnnotatedString()
                }
                timeOfLastTimestamp = now
                latestDataDirection = dataDirection
            } else if (latestDataBAOS.size() >= 4096) {
                val hexDumpStr = HexDump.dumpHexString(latestDataBAOS.toByteArray(),0, latestDataBAOS.size())
                deadHexTextAS += with(AnnotatedString.Builder(hexDumpStr.length)) {
                    pushStyle(dataDirectionToSpanStyle(latestDataDirection))
                    append(hexDumpStr)
                    pop()
                    toAnnotatedString()
                }
                latestDataBAOS.reset()
            }
            trimIfNeeded()
            latestDataBAOS.write(data, offset, data.size - offset)
        }
        updateUi()
    }

    fun setMaxTotalSize(newMaxTotalSize: Int) {
        synchronized(latestDataBAOS) {
            // A line can display values of up to 8 bytes. If it has only one byte
            // its length is 25 chars, if it has 8 bytes its length is 32 chars.
            // So best-case-scenario we need 4 chars per byte.
            // The worst-case-scenario is if we have one byte at a time with
            // alternating directions. In this case we print a line + timestamp
            // for every byte. That's 25+12 = 37 chars per byte!
            // In order to keep things reasonable, we assume 10 chars per byte,
            // but we don't allow more than 1M chars total
            totalSizeUpperLimit = min(newMaxTotalSize * 10, 1_000_000)
            trimIfNeeded()
        }
    }

    private fun trimIfNeeded() {
        if (deadHexTextAS.length > totalSizeUpperLimit) {
            val nCharsToTrim = totalSizeUpperLimit / 4  // Trimming is done by removing a 1/4 of current size (arbitrary number)
            // Timber.d("trimIfNeeded(): Trimming $nCharsToTrim chars")
            deadHexTextAS = if (deadHexTextAS[deadHexTextAS.lastIndex] == '\n') {
                deadHexTextAS.subSequence(nCharsToTrim, deadHexTextAS.length)
            } else {
                deadHexTextAS.subSequence(nCharsToTrim, deadHexTextAS.length) + AnnotatedString("\n")
            }
        }
    }

    private fun updateUi() {
        // Optimization: We only trigger a recomposition of the screen once every
        // 40mSec. Data may be coming very fast which causes very fast refresh of
        // the screen content (consuming lots of resources). There is no point in
        // updating the screen more than 25 times a second anyway.
        if (uiUpdateTriggered.compareAndSet(false, true)) {
            coroutineScope.launch {
                delay(40)
                uiUpdateTriggered.set(false)
                _screenHexAnnotatedStringState.value = getAnnotatedString()
                _shouldScrollToBottom.value = true
//                Timber.d("updateUi(): screenLinesState.value.size: ${screenLinesState.value.size}  shouldScrollToPosition=${shouldScrollToPosition.value}")
            }
        }
    }

    private fun getAnnotatedString(): AnnotatedString {
        synchronized(latestDataBAOS) {
            return if (latestDataBAOS.size() > 0) {
                deadHexTextAS + with(AnnotatedString.Builder(200)) {
                    pushStyle(dataDirectionToSpanStyle(latestDataDirection))
                    append(HexDump.dumpHexString(latestDataBAOS.toByteArray(), 0, latestDataBAOS.size()))
                    pop()
                    toAnnotatedString()
                }
            } else {
                deadHexTextAS
            }
        }
    }

    fun clear() {
        _screenHexAnnotatedStringState.value = AnnotatedString("")
        deadHexTextAS = AnnotatedString("")
        synchronized(latestDataBAOS) {
            latestDataBAOS.reset()
        }
        latestDataDirection = IOPacketsList.DataDirection.UNDEFINED
        timeOfLastTimestamp = 0L
    }

    private fun dataDirectionToSpanStyle(dd: IOPacketsList.DataDirection): SpanStyle {
        return when(dd) {
            IOPacketsList.DataDirection.OUT -> spanStyleForOutputDataText
            else -> spanStyleForInputDataText
        }
    }
}

*/
