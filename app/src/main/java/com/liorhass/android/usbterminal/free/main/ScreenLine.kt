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

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import java.lang.Integer.max
import java.lang.Integer.min
import java.util.concurrent.atomic.AtomicLong

class ScreenLine(
    private var maxLineLen: Int,
) {
    constructor(text: String): this(text, text.length)
    constructor(text: String, maxLineLen: Int): this(maxLineLen) {
        text.forEachIndexed { i, c -> textArray[i] = c }
        textLength = min(text.length, maxLineLen)
    }

    private var textArray: CharArray = CharArray(maxLineLen)
    var textLength: Int = 0 // Length of the text currently held
        private set
    private val spanStyles: MutableList<AnnotatedString.Range<SpanStyle>> = mutableListOf()
    private val activeSpanStyles: MutableList<MutableSpanStyleRange> = mutableListOf()
    private var annotatedString: AnnotatedString? = null


    private val _uid = ScreenLineUid()    // Unique ID
    val uid: Long
        get() = _uid.toLong()

    fun setMaxLineLength(maxLen: Int) {
        maxLineLen = maxLen
        if (textLength > maxLineLen) textLength = maxLineLen
        textArray = textArray.copyOf(maxLineLen)
    }

    /**
     * @return Number of bytes added to the line (delta size). This may be 0 if the
     * line size wasn't increased. Return -1 on failure due to index >= maxLineLen
     */
    fun putCharAt(c: Char, index: Int, alsoExtendActiveSpanStyles: Boolean): Int {
        if (index >= maxLineLen) return -1 // Indicate we're beyond line's max length
        var deltaSize = 0
        textArray[index] = c
        if (index >= textLength) { // We're beyond current line's end
            for (i in textLength until index) { // Fill empty places before the added char with spaces
                textArray[i] = ' '
            }
            deltaSize = index + 1 - textLength
            textLength = min(index + 1, maxLineLen)
        }

        if (alsoExtendActiveSpanStyles) {
            // Advance end of all active SpanStyles (e.g. text color) to include added char
            activeSpanStyles.forEach {
                if (it.end < index + 1) {
                    it.end = index + 1 // end of spanStyle's range is exclusive, so we mark the end at one place beyond the added character
                }
            }
        }

        markThatWeHaveBeenChanged()
        return deltaSize
    }

    /**
     *  Append spaces to end of line up to specified location (inclusive).
     *
     * Active SpanStyles are extended up to, but not including, location.
     * It doesn't include location because the last space char is always
     * a "synthetic" space just for drawing the cursor
     */
    fun appendSpacesUpToLocation(location: Int) {
        val lastIndex = min(location, maxLineLen - 1)
        for ( i in textLength .. lastIndex) {
            textArray[i] = ' '
        }

        // Advance end of all active SpanStyles (e.g. text color) to include appended chars
        activeSpanStyles.forEach {
            if (it.end < lastIndex) {
                it.end = lastIndex
            }
        }
    }


    /**
     *  Clears line's content by setting it only one space (textLength = 1)
     *  @return Number of characters deleted
     */
    fun clear(): Int {
        val oldTextLength = textLength
        textArray[0] = ' '
        textLength = 1
        spanStyles.clear()
        activeSpanStyles.clear()
        markThatWeHaveBeenChanged()
        return oldTextLength - 1
    }

    /**
     *  Clears line's content: Fill with spaces until the specified location (inclusive)
     *  @return Number of characters removed (strangely can be negative if chars were added)
     */
    fun clearAndTruncateTo(to: Int): Int {
        if (to >= maxLineLen) return 0
        for (i in 0..to) {
            textArray[i] = ' '
        }
        val oldTextLength = textLength
        textLength = to + 1
        markThatWeHaveBeenChanged()
        return oldTextLength - textLength
    }

    /**
     *  Clears line's content from the specified index (inclusive) to the line's end
     *  @return Number of characters removed
     */
    fun clearFromInclusive(from: Int): Int {
        if (from >= maxLineLen) return 0
        textArray[from] = ' '
        val oldTextLength = textLength
        textLength = from + 1
        markThatWeHaveBeenChanged()
        return oldTextLength - textLength
    }

    /**
     *  Clears line's content from the line's start to the specified index (inclusive)
     *  by replacing it with spaces
     */
    fun clearToInclusive(to: Int) {
        if (to >= maxLineLen) return
        for (i in 0..to) {
            textArray[i] = ' '
        }
        markThatWeHaveBeenChanged()
    }

    fun addSpanStyle(style: AnnotatedString.Range<SpanStyle>, alsoToggleUid: Boolean) {
        spanStyles.add(style)
        if (alsoToggleUid) { _uid.toggle() }
        markThatWeHaveBeenChanged()
    }

    fun removeSpanStyleByTag(tag: String, alsoToggleUid: Boolean) {
        spanStyles.removeAll { it.tag == tag }
        if (alsoToggleUid) { _uid.toggle() }
        markThatWeHaveBeenChanged()
    }

    fun startSpanStyle(style: SpanStyle, index: Int, tag: String) {
        // This spanStyleRange is of length 0 (since start == end)
        val spanStyleRange = MutableSpanStyleRange(
            start = index, // Inclusive
            end = index,   // Exclusive
            item = style,
            tag = tag
        )
        if (activeSpanStyles.size < 20) { // for safety
            activeSpanStyles.add(spanStyleRange)
        }
    }

    /** @param index index where all spanStyles are to be terminated (Exclusive index) */
    fun endAllSpanStyles(index: Int) {
        activeSpanStyles.forEach {
            it.end = max(index, it.end)
            if (it.end > it.start) {
                spanStyles.add(it.toSpanStyleRange())
            }
        }
        activeSpanStyles.clear()
        markThatWeHaveBeenChanged()
    }

    /**
     *  @param index index where all spanStyles are to be terminated (Exclusive index)
     *  @param tag Only spanStyles with this tag are to be terminated
     */
    fun endAllSpanStylesByTag(index: Int, tag: String) {
        var weHaveBeenChanged = false
        val itr = activeSpanStyles.iterator()
        while (itr.hasNext()) { // We use iterator instead of forEach so we can remove items while iterating
            val spanStyleRange = itr.next()
            if (spanStyleRange.tag == tag) {
                spanStyleRange.end = index // max(index, spanStyleRange.end)
                if (spanStyleRange.end > spanStyleRange.start) {
                    spanStyles.add(spanStyleRange.toSpanStyleRange())
                }
                itr.remove()
                weHaveBeenChanged = true
            }
        }
        if (weHaveBeenChanged) markThatWeHaveBeenChanged()
    }

    /** Return an AnnotatedString (build it if not cached and cache it for future calls) */
    fun getAnnotatedString(): AnnotatedString {
        return annotatedString ?: run {
//            val newText = String(textArray.copyOfRange(0, textLength))
//            Timber.d("======= textLength: ${textLength} newText: '$newText' len:${newText.length}  spanStyles.size: ${spanStyles.size}")
            val currentStyleSpans: MutableList<AnnotatedString.Range<SpanStyle>> =
                if (activeSpanStyles.size == 0) {
                    spanStyles
                } else {
                    val currentStyleSpans = mutableListOf<AnnotatedString.Range<SpanStyle>>()
                    spanStyles.forEach {
                        currentStyleSpans.add(it)
                    }
                    activeSpanStyles.forEach {
                        currentStyleSpans.add(it.toSpanStyleRange())
                    }
                    currentStyleSpans
                }
            annotatedString = AnnotatedString(
                text = String(textArray.copyOfRange(0, textLength)),
                spanStyles = currentStyleSpans
            )
            annotatedString!!
        }
    }

    private fun markThatWeHaveBeenChanged() {
        _uid.generateNewUid()
        annotatedString = null
    }

    private class ScreenLineUid {
        private val uid = AtomicLong(ScreenLineUidGenerator.getNextUid())
        private var dirty: Boolean = false
        fun toLong(): Long {
            if (dirty) {
                uid.set(ScreenLineUidGenerator.getNextUid())
                dirty = false
            }
            return uid.toLong()
        }
        fun generateNewUid() {dirty = true} // Must be cheap because it's called on every added char
        fun toggle(): Long {
            if (dirty) {
                uid.set(ScreenLineUidGenerator.getNextUid())
                dirty = false
            }
            return uid.accumulateAndGet(0x4000_0000_0000_0000L) { a, b -> a xor b }
        }
    }

    /** Generate UIDs for screenLines */
    private object ScreenLineUidGenerator {
        private var uid = AtomicLong(0L)

        /** Return a UID. */
        fun getNextUid(): Long = uid.incrementAndGet()
    }

    private data class MutableSpanStyleRange(
        var start: Int, // Inclusive
        var end: Int,   // Exclusive
        val item: SpanStyle,
        val tag: String,
    ) {
        fun toSpanStyleRange(): AnnotatedString.Range<SpanStyle> {
            return AnnotatedString.Range(
                start = start, // Inclusive
                end = end,     // Exclusive
                item = item,
                tag = tag,
            )
        }
    }
}
