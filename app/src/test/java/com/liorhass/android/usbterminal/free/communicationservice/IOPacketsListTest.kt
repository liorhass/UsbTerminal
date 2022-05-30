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

// https://developer.android.com/studio/test
// https://developer.android.com/training/testing
// https://truth.dev/
// https://truth.dev/api/latest/index.html?overview-summary.html

package com.liorhass.android.usbterminal.free.communicationservice

import com.google.common.truth.Truth.assertThat
import com.liorhass.android.usbterminal.free.usbcommservice.IOPacketsList
import org.junit.Before

import org.junit.Test

class IOPacketsListTest {

    @Before
    fun setUp() {

    }

    @Test
    fun testAppendDataAndProcessData() {
        val ioPacketsList = IOPacketsList(10, maxDurationOfSinglePacket = 300)
        val ba10 = ByteArray(10)
        for (byte in 0..9)  ba10[byte] = byte.toByte()
        val ba7 = ByteArray(7)
        for (byte in 0..6)  ba7[byte] = byte.toByte()
        val ba25 = ByteArray(25)
        for (byte in 0..24)  ba25[byte] = byte.toByte()

        ioPacketsList.appendData(ba10, IOPacketsList.DataDirection.IN) // packet 1
        ioPacketsList.appendData(ba7, IOPacketsList.DataDirection.IN)  // packet 2
        ioPacketsList.appendData(ba7, IOPacketsList.DataDirection.OUT) // packet 3
        ioPacketsList.appendData(ba7, IOPacketsList.DataDirection.IN)  // packet 4
        ioPacketsList.appendData(ba7, IOPacketsList.DataDirection.IN)  // packet 4, 5
        Thread.sleep(500)
        ioPacketsList.appendData(ba7, IOPacketsList.DataDirection.IN)  // packet 6

        var pointer = IOPacketsList.DataPointer() // Point to 0;0
        var packetNum = 0
        pointer = ioPacketsList.processData(pointer) {data, packetSerialNumber, offset, direction, timeStamp ->
            when (packetNum) {
                0 -> {
                    assertThat(data.size).isEqualTo(10)
                    assertThat(offset).isEqualTo(0)
                    assertThat(direction).isEqualTo(IOPacketsList.DataDirection.IN)
                    for (byte in 0..9)  assertThat(data[byte]).isEqualTo(byte.toByte())
                }
                1 -> {
                    assertThat(data.size).isEqualTo(7)
                    assertThat(offset).isEqualTo(0)
                    assertThat(direction).isEqualTo(IOPacketsList.DataDirection.IN)
                    for (byte in 0..6)  assertThat(data[byte]).isEqualTo(byte.toByte())
                }
                2 -> {
                    assertThat(data.size).isEqualTo(7)
                    assertThat(offset).isEqualTo(0)
                    assertThat(direction).isEqualTo(IOPacketsList.DataDirection.OUT)
                    for (byte in 0..6)  assertThat(data[byte]).isEqualTo(byte.toByte())
                }
                3 -> {
                    assertThat(data.size).isEqualTo(10)
                    assertThat(offset).isEqualTo(0)
                    assertThat(direction).isEqualTo(IOPacketsList.DataDirection.IN)
                    for (byte in 0..6)  assertThat(data[byte]).isEqualTo(byte.toByte())
                    for (byte in 7..9)  assertThat(data[byte]).isEqualTo(byte.toByte()-7)
                }
                4 -> {
                    assertThat(data.size).isEqualTo(4)
                    assertThat(offset).isEqualTo(0)
                    assertThat(direction).isEqualTo(IOPacketsList.DataDirection.IN)
                    for (byte in 0..3)  assertThat(data[byte]).isEqualTo(byte.toByte()+3)
                }
                5 -> {
                    assertThat(data.size).isEqualTo(7)
                    assertThat(offset).isEqualTo(0)
                    assertThat(direction).isEqualTo(IOPacketsList.DataDirection.IN)
                    for (byte in 0..6)  assertThat(data[byte]).isEqualTo(byte.toByte())
                }
            }
            packetNum++
        }
        assertThat(packetNum).isEqualTo(6)
        assertThat(pointer.offsetInPacket).isEqualTo(7)
        assertThat(pointer.packetSerialNumber).isEqualTo(6)

        ioPacketsList.appendData(ba25, IOPacketsList.DataDirection.IN) // packet 6,7,8,9
        packetNum = 0
        pointer = ioPacketsList.processData(pointer) {data, packetSerialNumber, offset, direction, timeStamp ->
            when (packetNum) {
                0 -> {
                    assertThat(data.size).isEqualTo(10)
                    assertThat(offset).isEqualTo(7)
                    assertThat(direction).isEqualTo(IOPacketsList.DataDirection.IN)
                    for (byte in 7..9)  assertThat(data[byte]).isEqualTo(byte.toByte()-7)
                }
                1 -> {
                    assertThat(data.size).isEqualTo(10)
                    assertThat(offset).isEqualTo(0)
                    assertThat(direction).isEqualTo(IOPacketsList.DataDirection.IN)
                    for (byte in 0..9)  assertThat(data[byte]).isEqualTo(byte.toByte()+3)
                }
                2 -> {
                    assertThat(data.size).isEqualTo(10)
                    assertThat(offset).isEqualTo(0)
                    assertThat(direction).isEqualTo(IOPacketsList.DataDirection.IN)
                    for (byte in 0..9)  assertThat(data[byte]).isEqualTo(byte.toByte()+13)
                }
                3 -> {
                    assertThat(data.size).isEqualTo(2)
                    assertThat(offset).isEqualTo(0)
                    assertThat(direction).isEqualTo(IOPacketsList.DataDirection.IN)
                    for (byte in 0..1)  assertThat(data[byte]).isEqualTo(byte.toByte()+23)
                }
            }
            packetNum++
        }
        assertThat(packetNum).isEqualTo(4)
        assertThat(pointer.offsetInPacket).isEqualTo(2)
        assertThat(pointer.packetSerialNumber).isEqualTo(9)
    }

    @Test
    fun testTrim() {
        val ioPacketsList = IOPacketsList(1024, maxTotalSize = 200000)
        val ba1000 = ByteArray(1000)
        for (byte in 0..999) ba1000[byte] = byte.toByte()
        for (i in 0..49) {
            ioPacketsList.appendData(ba1000, IOPacketsList.DataDirection.IN)
            ioPacketsList.appendData(ba1000, IOPacketsList.DataDirection.OUT)
        }
        assertThat(ioPacketsList.getTotalSize()).isEqualTo(Pair(50000, 50000))
        ioPacketsList.trim(180000)
        assertThat(ioPacketsList.getTotalSize()).isEqualTo(Pair(50000,50000))
        ioPacketsList.trim(100333)
        assertThat(ioPacketsList.getTotalSize()).isEqualTo(Pair(50000,50000))
        ioPacketsList.trim(77666)
        assertThat(ioPacketsList.getTotalSize()).isEqualTo(Pair(38000,39000))

        var pointer = IOPacketsList.DataPointer() // Point to 0;0
        var packetNum = 0
        pointer = ioPacketsList.processData(pointer) { data, packetSerialNumber, offset, direction, timeStamp ->
            when (packetNum) {
                0 -> assertThat(direction).isEqualTo(IOPacketsList.DataDirection.OUT)
                1 -> assertThat(direction).isEqualTo(IOPacketsList.DataDirection.IN)
            }
            packetNum++
        }
        assertThat(packetNum).isEqualTo(77)

        ioPacketsList.clear()
        for (i in 0..110) { // Append more than 2000 bytes so trim() will take effect
            ioPacketsList.appendData(ba1000, IOPacketsList.DataDirection.IN)
            ioPacketsList.appendData(ba1000, IOPacketsList.DataDirection.OUT)
        }
        assertThat(ioPacketsList.getTotalSize()).isEqualTo(Pair(100000,101000))
    }
}