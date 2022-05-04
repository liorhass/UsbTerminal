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
package com.liorhass.android.usbterminal.free.usbcommservice

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.random.Random

fun usbCommServiceDebug(
    param: Char,
    coroutineScope: CoroutineScope,
    ioPacketsList: IOPacketsList,
) {
    Timber.d("debug param=$param")
    coroutineScope.launch {
        // Emulate receiving a message
        // val data01 = "1234567890ABCDEFGHIJ\n".toByteArray()
//            var i = 1000000
//            repeat(100) {
//                repeat(10) {
//                    //ioPacketsList.appendData(data01, IOPacketsList.DataDirection.IN)
//                    ioPacketsList.appendData("$i 1234567890ABCDEFGHIJ\n".toByteArray(), IOPacketsList.DataDirection.IN)
//                    // Timber.d("debug(): Calling ioPacketsList.notifyObservers() 1")
//                    ioPacketsList.notifyObservers()
//                    i++
//                }
//                delay(20)
//            }
//

        @Suppress("CascadeIf")
        if (param == '1') {
//                val ba = "0123456789abcdefghij".toByteArray()
//                val b = byteArrayOf(ba[dbgIndex++ % ba.size])
//                ioPacketsList.appendData(b, IOPacketsList.DataDirection.IN)

            ioPacketsList.appendData("S1234567890123456789".toByteArray(), IOPacketsList.DataDirection.IN)
            ioPacketsList.appendData("\u001B[15D".toByteArray(), IOPacketsList.DataDirection.IN)
            ioPacketsList.appendData("x".toByteArray(), IOPacketsList.DataDirection.IN)
            ioPacketsList.appendData("\u001B[0K\n".toByteArray(), IOPacketsList.DataDirection.IN)

            ioPacketsList.appendData("01234567890123456789".toByteArray(), IOPacketsList.DataDirection.IN)
            ioPacketsList.appendData("\u001B[15D".toByteArray(), IOPacketsList.DataDirection.IN)
            ioPacketsList.appendData("x".toByteArray(), IOPacketsList.DataDirection.IN)
            ioPacketsList.appendData("\u001B[0K".toByteArray(), IOPacketsList.DataDirection.IN)
            ioPacketsList.appendData("X\n".toByteArray(), IOPacketsList.DataDirection.IN)

            ioPacketsList.appendData("01234567890123456789".toByteArray(), IOPacketsList.DataDirection.IN)
            ioPacketsList.appendData("\u001B[15D".toByteArray(), IOPacketsList.DataDirection.IN)
            ioPacketsList.appendData("x".toByteArray(), IOPacketsList.DataDirection.IN)
            ioPacketsList.appendData("\u001B[1K".toByteArray(), IOPacketsList.DataDirection.IN)
            ioPacketsList.appendData("X\n".toByteArray(), IOPacketsList.DataDirection.IN)

            ioPacketsList.appendData("vvvvv\n".toByteArray(), IOPacketsList.DataDirection.IN)
            ioPacketsList.appendData("01234567890123456789".toByteArray(), IOPacketsList.DataDirection.IN)
            ioPacketsList.appendData("\u001B[15D".toByteArray(), IOPacketsList.DataDirection.IN)
            ioPacketsList.appendData("x".toByteArray(), IOPacketsList.DataDirection.IN)
            ioPacketsList.appendData("\u001B[2K".toByteArray(), IOPacketsList.DataDirection.IN)
            ioPacketsList.appendData("X\n".toByteArray(), IOPacketsList.DataDirection.IN)
            ioPacketsList.appendData("^^^^^\n".toByteArray(), IOPacketsList.DataDirection.IN)
/////////////////////////////////////////////////
            ioPacketsList.appendData( "1         2         3         4\n".toByteArray(), IOPacketsList.DataDirection.IN)
            ioPacketsList.appendData( "1234567890123456789012345678901\n".toByteArray(), IOPacketsList.DataDirection.IN)
            ioPacketsList.appendData( "1234567890\rlior\n".toByteArray(), IOPacketsList.DataDirection.IN)
            ioPacketsList.appendData( "1234567890\b\b\b\blior".toByteArray(), IOPacketsList.DataDirection.IN)
            ioPacketsList.appendData( "\n".toByteArray(), IOPacketsList.DataDirection.IN)
            ioPacketsList.appendData( "1234567890\r".toByteArray(), IOPacketsList.DataDirection.IN)
            ioPacketsList.appendData( "lior\n".toByteArray(), IOPacketsList.DataDirection.IN)
            ioPacketsList.appendData( "\u001B[A".toByteArray(), IOPacketsList.DataDirection.IN)
            ioPacketsList.appendData( "\u001B[C".toByteArray(), IOPacketsList.DataDirection.IN)
            ioPacketsList.appendData( "\u001B[C".toByteArray(), IOPacketsList.DataDirection.IN)
            ioPacketsList.appendData( "\u001B[C".toByteArray(), IOPacketsList.DataDirection.IN)
            ioPacketsList.appendData( "\u001B[C".toByteArray(), IOPacketsList.DataDirection.IN)
            ioPacketsList.appendData( "hass\n\n".toByteArray(), IOPacketsList.DataDirection.IN)

            ioPacketsList.appendData( "1234567890\r".toByteArray(), IOPacketsList.DataDirection.IN)
            ioPacketsList.appendData( "lior\n".toByteArray(), IOPacketsList.DataDirection.IN)
            ioPacketsList.appendData( "\u001B[A".toByteArray(), IOPacketsList.DataDirection.IN)
            ioPacketsList.appendData( "\u001B[4C".toByteArray(), IOPacketsList.DataDirection.IN)
            ioPacketsList.appendData( "hass\n".toByteArray(), IOPacketsList.DataDirection.IN)

            ioPacketsList.appendData( "1234567890\n".toByteArray(), IOPacketsList.DataDirection.IN)
            ioPacketsList.appendData( "1234567890\n".toByteArray(), IOPacketsList.DataDirection.IN)
            ioPacketsList.appendData( "1234567890\n".toByteArray(), IOPacketsList.DataDirection.IN)
            ioPacketsList.appendData( "lior\n".toByteArray(), IOPacketsList.DataDirection.IN)
            ioPacketsList.appendData( "\u001B[4A".toByteArray(), IOPacketsList.DataDirection.IN)
            ioPacketsList.appendData( "X".toByteArray(), IOPacketsList.DataDirection.IN)
            ioPacketsList.appendData( "\u001B[B".toByteArray(), IOPacketsList.DataDirection.IN)
            ioPacketsList.appendData( "X".toByteArray(), IOPacketsList.DataDirection.IN)
            ioPacketsList.appendData( "\u001B[B".toByteArray(), IOPacketsList.DataDirection.IN)
            ioPacketsList.appendData( "X".toByteArray(), IOPacketsList.DataDirection.IN)
            ioPacketsList.appendData( "\u001B[B".toByteArray(), IOPacketsList.DataDirection.IN)
            ioPacketsList.appendData( "X".toByteArray(), IOPacketsList.DataDirection.IN)

            ioPacketsList.appendData( "\n".toByteArray(), IOPacketsList.DataDirection.IN)

            ioPacketsList.appendData("\u0007".toByteArray(), IOPacketsList.DataDirection.IN)
            ioPacketsList.appendData("1234567890123456789012345678901234567890\n".toByteArray(), IOPacketsList.DataDirection.IN)
            ioPacketsList.appendData("\tx\txx\txxx\t4".toByteArray(), IOPacketsList.DataDirection.IN)
            ioPacketsList.inputPaused()

            ioPacketsList.appendData( "\n\n".toByteArray(), IOPacketsList.DataDirection.IN)
            ioPacketsList.appendData( "\u001B[0K".toByteArray(), IOPacketsList.DataDirection.IN)
            ioPacketsList.appendData( "\u001B[0;32mGREEN\u001B[0mDEFAULT\u001B[0;32mGREEN\u001B[0mDEFAULT\n".toByteArray(), IOPacketsList.DataDirection.IN)
            ioPacketsList.inputPaused()

            ioPacketsList.appendData( "12345678901234567890\u001B[15Dx\u001B[0K\n".toByteArray(), IOPacketsList.DataDirection.IN)
            ioPacketsList.appendData( "12345678901234567890\u001B[15Dx\u001B[Kz\n".toByteArray(), IOPacketsList.DataDirection.IN)
            ioPacketsList.appendData( "12345678901234567890\u001B[15Dx\u001B[1Kz\n".toByteArray(), IOPacketsList.DataDirection.IN)
            ioPacketsList.appendData( "12345678901234567890\u001B[15Dx\u001B[2Kz\n".toByteArray(), IOPacketsList.DataDirection.IN)
            ioPacketsList.appendData( "\u001B[H".toByteArray(), IOPacketsList.DataDirection.IN) // Go to home
            ioPacketsList.appendData( "\n\n".toByteArray(), IOPacketsList.DataDirection.IN) // Down 2 lines. Now at 3:1
            ioPacketsList.appendData( "**".toByteArray(), IOPacketsList.DataDirection.IN)
            ioPacketsList.appendData( "\u001B[5;8H".toByteArray(), IOPacketsList.DataDirection.IN) // Cursor to 5:8
            ioPacketsList.appendData( "**".toByteArray(), IOPacketsList.DataDirection.IN)
            ioPacketsList.appendData( "\u001B[6;4H".toByteArray(), IOPacketsList.DataDirection.IN) // Cursor to 6:4

            val nLines = 33 // For a screen with 33 lines
            for (i in 1..nLines) {
                ioPacketsList.appendData( "\n".toByteArray(), IOPacketsList.DataDirection.IN)
            }
            ioPacketsList.appendData( "##### This line should remain\n".toByteArray(), IOPacketsList.DataDirection.IN)
            for (i in 1 until nLines) {
                ioPacketsList.appendData( "$i\n".toByteArray(), IOPacketsList.DataDirection.IN)
            }
            ioPacketsList.appendData( "$nLines".toByteArray(), IOPacketsList.DataDirection.IN)
            ioPacketsList.appendData( "\u001B[H".toByteArray(), IOPacketsList.DataDirection.IN) // home
            ioPacketsList.appendData( "\u001B[0J".toByteArray(), IOPacketsList.DataDirection.IN) // Clear from cursor to end-of-screen (including under cursor)


            ioPacketsList.inputPaused()
        } else if (param == '2') {
            val beginning = System.currentTimeMillis()
            repeat(100) { i ->
                val ba = "<<$i>> 0123456789abcdefghij\n".toByteArray()
                repeat(10) { j ->
                    repeat(ba.size) { k ->
                        val b = byteArrayOf(ba[k])
                        ioPacketsList.appendData(b, IOPacketsList.DataDirection.IN)
                    }
                    //                delay(2)
                }
                repeat(2) { j ->
                    repeat(ba.size) { k ->
                        val b = byteArrayOf(ba[k])
                        ioPacketsList.appendData(b, IOPacketsList.DataDirection.OUT)
                    }
                    //                delay(2)
                }
                ioPacketsList.inputPaused()
            }
            val end = System.currentTimeMillis()
            val elapsed = end - beginning
            Timber.d("Wrote 20,000 chars in ${elapsed}mSec  ${20_000_000.0 / elapsed.toDouble()} chars/Sec")
        } else if (param == 'h') {
            val ba = "\u001B[1D".toByteArray() // cursor left
            ioPacketsList.appendData(ba, IOPacketsList.DataDirection.IN)
            ioPacketsList.inputPaused()
        } else if (param == 'j') {
            val ba = "\u001B[1B".toByteArray() // cursor down
            ioPacketsList.appendData(ba, IOPacketsList.DataDirection.IN)
            ioPacketsList.inputPaused()
        } else if (param == 'k') {
            val ba = "\u001B[0A".toByteArray() // cursor right
            ioPacketsList.appendData(ba, IOPacketsList.DataDirection.IN)
            ioPacketsList.inputPaused()
        } else if (param == 'l') {
            val ba = "\u001B[C".toByteArray() // cursor up
            ioPacketsList.appendData(ba, IOPacketsList.DataDirection.IN)
            ioPacketsList.inputPaused()
        }
    }
}

class Dbg2Brm {
    var sum: Int = 0
    fun add(v: Int) {
        sum += v
    }
}
@Suppress("Unused")
private fun usbCommServiceDebug1() {
    data class Holder(val v1: Int, val v2: Int)
    val list = MutableList(10000) {i -> Holder(i, Random.nextInt(0, 100))}
    val dbg2Brm = Dbg2Brm()
    val startTime = System.currentTimeMillis()
    repeat(100000) { i ->
//            list.add(Holder(Random.nextInt(), i))
        val r = (Holder(Random.nextInt(0, 100000), i))
        if (r.v1 < 2) {
            list.add(Holder(Random.nextInt(), i))
        }
        val al = ArrayList<Holder>(list)
        dbg2Brm.add(al.size)
    }
    val stopTime = System.currentTimeMillis()
    Timber.d("Elapsed time = ${stopTime-startTime}mSec  sum=${dbg2Brm.sum}")
}
