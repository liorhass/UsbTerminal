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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class ScreenHexModel(
    private val coroutineScope: CoroutineScope,
    private var maxTotalSize: Int,
) {
    private companion object {
        const val TOTAL_SIZE_TRIMMING_HYSTERESIS = 1000

        var nextUid = 1L
            get() = field++
    }

    class HexTextBlock(
        var uid: Long = nextUid,
        var timeStamp: Long,
        var dataDirection: IOPacketsList.DataDirection,
        _annotatedString: AnnotatedString? = null,
    ) {
        var annotatedString: AnnotatedString? = _annotatedString
            set(value) {
                field = value
                uid = nextUid // If we update this HexTextBlock we must change its uid so it will be re-drawn
            }
    }

    private val hexTextBlocks = mutableListOf<HexTextBlock>()
    private val _screenHexTextBlocksState = mutableStateOf<Array<HexTextBlock>>(emptyArray())
    val screenHexTextBlocksState: State<Array<HexTextBlock>> = _screenHexTextBlocksState
    private var totalCharCount = 0

    private val _shouldScrollToBottom = mutableStateOf(false)
    val shouldScrollToBottom: State<Boolean> = _shouldScrollToBottom
    fun setShouldScrollToBottom() {
        _shouldScrollToBottom.value = true
    }
    fun onScrolledToBottom() {
        _shouldScrollToBottom.value = false
    }
    init {
        hexTextBlocks.add(
            HexTextBlock(
                timeStamp = 0L,
                dataDirection = IOPacketsList.DataDirection.UNDEFINED
            )
        )
    }

    private val mutex = Mutex()

    private val latestDataBAOS = ByteArrayOutputStream(1000)
    private val timeFormatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private val uiUpdateTriggered = AtomicBoolean(false)


    fun onStart() {}
    fun onStop() {
        clear()
    }

    private var spanStyleForInputDataText = SpanStyle(color = Color.Transparent)
    private var spanStyleForOutputDataText = SpanStyle(color = Color.Transparent)
    private var spanStyleForTimestamp = SpanStyle(color = Color.Transparent)
    fun setIsDarkTheme(isDarkTheme: Boolean) {
        if (isDarkTheme) {
            spanStyleForInputDataText = SpanStyle(color = ColorOfInputHexTextForDarkTheme)
            spanStyleForOutputDataText = SpanStyle(color = ColorOfOutputHexTextForDarkTheme)
            spanStyleForTimestamp = SpanStyle(color = TimestampColorForDarkTheme)
        } else {
            spanStyleForInputDataText = SpanStyle(color = ColorOfInputHexTextForLightTheme)
            spanStyleForOutputDataText = SpanStyle(color = ColorOfOutputHexTextForLightTheme)
            spanStyleForTimestamp = SpanStyle(color = TimestampColorForLightTheme)
        }
    }

    fun onNewData(data: ByteArray, offset: Int, dataDirection: IOPacketsList.DataDirection, timeStamp: Long) {
        // Timber.d("onNewData() len=${data.size} offset=$offset timeStamp=$timeStamp latestDataBAOS.size=${latestDataBAOS.size()}")
        coroutineScope.launch {
            mutex.withLock {
                var lastHexTextBlock = hexTextBlocks.last()
                if (timeStamp != lastHexTextBlock.timeStamp ||
                    dataDirection != lastHexTextBlock.dataDirection ||
                    latestDataBAOS.size() >= 1024
                ) {
                    // Should start a new textBlock

                    // If this is the first block of data received, replace the synthetic first block
                    if (hexTextBlocks.size == 1 && lastHexTextBlock.dataDirection == IOPacketsList.DataDirection.UNDEFINED) {
                        lastHexTextBlock.timeStamp = timeStamp
                        lastHexTextBlock.dataDirection = dataDirection
                    } else {
                        // Before writing new data to the latestDataBAOS, dump anything in it
                        // into the last element of the list
                        if (latestDataBAOS.size() > 0) {
                            dumpLatestDataIntoTextBlock(lastHexTextBlock)
                            // Timber.d("onNewData() latestDataBAOS.size=${latestDataBAOS.size()} lastHexTextBlock.size=${lastHexTextBlock.annotatedString?.length}")
                            latestDataBAOS.reset()
                        }

                        // Append a new empty HexTextBlock holding the new timeStamp and dataDirection to the list
                        lastHexTextBlock =
                            HexTextBlock(timeStamp = timeStamp, dataDirection = dataDirection)
                        hexTextBlocks.add(
                            lastHexTextBlock
                        )
                    }
                }
                @Suppress("BlockingMethodInNonBlockingContext") // No worries. We know that all this write() does is done in-memory by System.arraycopy()
                latestDataBAOS.write(data, offset, data.size - offset)
                dumpLatestDataIntoTextBlock(lastHexTextBlock)
                trimIfNeeded()
            } // withLock
            updateUi()
        }
    }

    private fun dumpLatestDataIntoTextBlock(hexTextBlock: HexTextBlock) {
        // Timber.d(">>> dumping ${latestDataBAOS.size()} bytes. Block already contain ${hexTextBlock.annotatedString?.length} chars")
        val timeStampStr = "\n${timeFormatter.format(Date(hexTextBlock.timeStamp))}\n"
        val hexDumpStr = HexDump.dumpHexString(latestDataBAOS.toByteArray(),0, latestDataBAOS.size())
        totalCharCount -= (hexTextBlock.annotatedString?.length ?: 0)
        hexTextBlock.annotatedString =
            with (AnnotatedString.Builder(timeStampStr.length + hexDumpStr.length)) {
                pushStyle(spanStyleForTimestamp)
                append(timeStampStr)
                pop()
                pushStyle(dataDirectionToSpanStyle(hexTextBlock.dataDirection))
                append(hexDumpStr)
                pop()
                toAnnotatedString()
            }
        totalCharCount += (hexTextBlock.annotatedString?.length ?: 0)
    }

    fun setMaxTotalSize(newMaxTotalSize: Int) {
        coroutineScope.launch {
            mutex.withLock {
                maxTotalSize = newMaxTotalSize
                trimIfNeeded()
            } // withLock
        }
    }

    // Must be called from within a mutex.withLock{} block (vfy)
    private fun trimIfNeeded() {
        if (totalCharCount > maxTotalSize + TOTAL_SIZE_TRIMMING_HYSTERESIS) {
            // Timber.d("Trimming totalCharCount=$totalCharCount  maxTotalSize=$maxTotalSize  nHexTextBlocks=${hexTextBlocks.size}")
            while (totalCharCount - (hexTextBlocks.firstOrNull()?.annotatedString?.length ?: 0) > maxTotalSize) {
                // Timber.d("Deleting 1st block. txtLength=${(hexTextBlocks.firstOrNull()?.annotatedString?.length ?: 0)}")
                totalCharCount -= (hexTextBlocks.removeFirst().annotatedString?.length ?: 0)
            }
        }
    }

    private val emptyBlock = HexTextBlock(
        timeStamp = 0L,
        dataDirection = IOPacketsList.DataDirection.UNDEFINED,
        _annotatedString = AnnotatedString("")
    )
    private suspend fun updateUi() {
        // Optimization: We only trigger a recomposition of the screen once every
        // 40mSec. Data may be coming very fast which causes very fast refresh of
        // the screen content (consuming lots of resources). There is no point in
        // updating the screen more than 25 times a second anyway.
        if (uiUpdateTriggered.compareAndSet(false, true)) {
                delay(40)
                uiUpdateTriggered.set(false)
                mutex.withLock {
                    // A hack: The only way to scroll to the bottom of a LazyColumn is to
                    // scroll to the last item. However these items (textBlocks) may be
                    // quite tall, so scrolling to the last item (which for a tall item
                    // scrolls so the top of the last item is at the top of the screen)
                    // actually doesn't scroll to the bottom. The hack is that we add
                    // an empty block at the end of the list, so when the LazyColumn
                    // scrolls to the bottom, we really are taken to the bottom of the
                    // data.
                    val screenHexTextBlocks = hexTextBlocks.toTypedArray(hexTextBlocks.size + 1)
                    screenHexTextBlocks[hexTextBlocks.size] = emptyBlock
                    _screenHexTextBlocksState.value = screenHexTextBlocks
                    _shouldScrollToBottom.value = true
                    // Timber.d("updateUi(): screenLinesState.value.size: ${screenLinesState.value.size}  shouldScrollToPosition=${shouldScrollToPosition.value}")
                } // withLock
        }
    }

    fun clear() {
        coroutineScope.launch {
            mutex.withLock {
                hexTextBlocks.clear()
                hexTextBlocks.add( // We start with one empty block
                    HexTextBlock(
                        timeStamp = 0L,
                        dataDirection = IOPacketsList.DataDirection.UNDEFINED
                    )
                )
                latestDataBAOS.reset()
                totalCharCount = 0
                _screenHexTextBlocksState.value = hexTextBlocks.toTypedArray()
            } // withLock
        }
    }

    private fun dataDirectionToSpanStyle(dd: IOPacketsList.DataDirection): SpanStyle {
        return when(dd) {
            IOPacketsList.DataDirection.OUT -> spanStyleForOutputDataText
            else -> spanStyleForInputDataText
        }
    }
}

/**
 * Like `Collection<T>.toTypedArray()` but with ability to specify size
 */
@Suppress("UNCHECKED_CAST")
inline fun <reified T> Collection<T>.toTypedArray(size: Int): Array<T> {
    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    val thisCollection = this as java.util.Collection<T>
    return thisCollection.toArray(arrayOfNulls<T>(size)) as Array<T>
}
