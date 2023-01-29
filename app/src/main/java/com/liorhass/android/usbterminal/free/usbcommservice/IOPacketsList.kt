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
package com.liorhass.android.usbterminal.free.usbcommservice

import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.min

/**
 * Holds the communication's history as a list of packets. Each packet represents consecutive bytes
 * in the same direction (either in or out).
 * Each packet is identified by a serial number. Serial numbers start at 1 and are incremented for
 * every new packet that is appended.
 * Observing objects are notified about newly appended data.
 */
class IOPacketsList(private val maxPacketSize: Int = MAX_PACKET_SIZE,
                    maxTotalSize: Int = MAX_TOTAL_SIZE,
                    private val maxDurationOfSinglePacket: Int = MAX_DURATION_OF_SINGLE_PACKET) {
    companion object {
        const val MAX_TOTAL_SIZE = 100_000 // Total bytes to hold when trimming
        const val MAX_PACKET_SIZE = 1024
        const val MAX_DURATION_OF_SINGLE_PACKET = 2000 // In mSec
    }

    enum class DataDirection {
        IN, OUT, UNDEFINED
    }

    private val packetsMap = HashMap<Int, IOPacket>(503)
    private var firstPacketSerialNumber = 0
    private var lastPacketSerialNumber = 0 // Ever increasing. Never reset back to 0
    private var totalBytesIn  = 0
    private var totalBytesOut = 0
    private var lastPacketStartTime = 0L
    private var lastDirection = DataDirection.UNDEFINED
    private var nConsecutivePackets = 0
    private var totalSizeUpperBound = maxTotalSize + MAX_PACKET_SIZE // Add one packet size in order to not go below the specified size in trim()

    private val observable = IOPacketListObservable()
    private class IOPacketListObservable : Observable() {
        // This class is only needed to convert a protected method to public, because we want to
        // hold an Observable object not inherit from it (favour composition over inheritance)
        @Deprecated("Deprecated in Java")
        public override fun setChanged() {
            super.setChanged()
        }
    }
    fun addObserver(observer: Observer) {observable.addObserver(observer)}
    fun deleteObserver(observer: Observer) {observable.deleteObserver(observer)}

    class DataPointer(val packetSerialNumber: Int = 0, val offsetInPacket: Int = 0) {
        constructor(mdp: MutableDataPointer): this(mdp.packetSerialNumber, mdp.offsetInPacket)
        operator fun compareTo(other: DataPointer): Int {
            return when {
                this.packetSerialNumber > other.packetSerialNumber -> 1
                this.packetSerialNumber < other.packetSerialNumber -> -1
                this.offsetInPacket > other.offsetInPacket -> 1
                this.offsetInPacket < other.offsetInPacket -> -1
                else -> 0
            }
        }
    }
    class MutableDataPointer(var packetSerialNumber: Int = 0, var offsetInPacket: Int = 0) {
        fun set(packetSerialNumber: Int, offsetInPacket: Int) {
            this.packetSerialNumber = packetSerialNumber
            this.offsetInPacket     = offsetInPacket
        }
        fun set(dp: DataPointer) {
            this.packetSerialNumber = dp.packetSerialNumber
            this.offsetInPacket     = dp.offsetInPacket
        }
    }
    private val resultDataPointer = MutableDataPointer()

    fun appendData(data: ByteArray, direction: DataDirection) {
        synchronized(this) {
            // If it's a long time since the previous update, start a new packet so we can display
            // this new data as separate from the previous data (with its own timestamp)
            val now = System.currentTimeMillis()
            val itsALongTimeFromLastUpdate = now > (lastPacketStartTime + maxDurationOfSinglePacket)

            if (itsALongTimeFromLastUpdate || lastDirection != direction || packetsMap.isEmpty()) {
                // Should start a new packet
                if (packetsMap.isEmpty()) {
                    firstPacketSerialNumber = addPacket(++lastPacketSerialNumber, currentTime = now, direction = direction)
                } else {
                    addPacket(++lastPacketSerialNumber, currentTime = now, direction = direction)
                }
                lastDirection = direction
            }

            // Append data to the packet
            var nBytesToCopy = data.size
            var offset = 0
            while(nBytesToCopy > 0) {
                val currentPacket = requireNotNull(packetsMap[lastPacketSerialNumber])
                when {
                    currentPacket.data.size + nBytesToCopy <= maxPacketSize -> {
                        // There's enough room in current packet to simply append to it
                        currentPacket.append(data, offset, nBytesToCopy)
                        nBytesToCopy = 0
                    }
                    currentPacket.data.size == maxPacketSize -> {
                        // Current packet is full. Start a new one
                        addPacket(++lastPacketSerialNumber, currentTime = now, direction = direction)
                    }
                    else -> {
                        // Some data goes to the end of the existing packet
                        val spaceLeft = maxPacketSize - currentPacket.data.size
                        val len = min(nBytesToCopy, spaceLeft)
                        currentPacket.append(data, offset, len)
                        offset += len
                        nBytesToCopy -= len
                    }
                }
            }
            when (direction) {
                DataDirection.IN -> totalBytesIn  += data.size
                else             -> totalBytesOut += data.size
            }
            trim(totalSizeUpperBound)
            observable.setChanged() // From Observable. Mark this observable object as changed. After this a call to our notifyObservers() will call the observers

            // If we got a substantial amount of new data, notify the UI to update data on screen
            if (nConsecutivePackets++ >= 4) {
                nConsecutivePackets = 0
                observable.notifyObservers()
            }
        }
    }

    /**
     *  Should be called whenever there is no more received data after some data was received.
     *  This allows us to notify the UI that it should update the data on screen.
     */
    fun inputPaused() {
        nConsecutivePackets = 0
        observable.notifyObservers()
    }

    fun setMaxSize(newMaxSize: Int) {
        val oldTotalSizeUpperBound = totalSizeUpperBound
        totalSizeUpperBound = newMaxSize + maxPacketSize
        if (totalSizeUpperBound < oldTotalSizeUpperBound) {
            trim(newMaxSize + maxPacketSize)
        }
    }

    // Should be private. It's public only to enable direct call from unit testing
    fun trim(targetTotalSizeUpperBound: Int) {
        synchronized(this) {
            if (packetsMap.isEmpty()) return // No data
            var totalSize = totalBytesIn + totalBytesOut
            while (totalSize >= targetTotalSizeUpperBound) {
                val firstPacket = packetsMap[firstPacketSerialNumber]
                if (firstPacket == null) {
                    Timber.wtf("trim(): firstPacket == null")
                    break
                }
                val firstPacketSize = firstPacket.data.size
                totalSize -= firstPacketSize
                when(firstPacket.direction) {
                    DataDirection.IN -> totalBytesIn  -= firstPacketSize
                    else             -> totalBytesOut -= firstPacketSize
                }
                packetsMap.remove(firstPacketSerialNumber)
                firstPacketSerialNumber++
            }
        }
    }

    /**
     * Process the stored data by calling the [processor] function starting at [startAt].
     * @param startAt Where to start processing (packet serialNumber and offset in that packet)
     * @param processor The processor function
     * @return A [DataPointer] to the end of the processed data. Typically used in the next call
     * to [processData] after additional data is appended
     */
    fun processData(
        startAt: DataPointer,
        processor: (data: ByteArray, packetSerialNumber: Int, offset: Int, direction: DataDirection, timeStamp: Long) -> Unit
    ): DataPointer {
        // Timber.d("processData() startAt=(${startAt.packetSerialNumber}, ${startAt.offsetInPacket}) firstPacketSerialNumber=$firstPacketSerialNumber packet.size=${packetsMap[firstPacketSerialNumber]?.data?.size}")
        synchronized(this) {
            if (lastPacketSerialNumber == 0) { // Empty so nothing to do
                return startAt
            }
            if (startAt.packetSerialNumber > lastPacketSerialNumber) {
                Timber.wtf("processData() bad startAt startAt.packetSerialNumber=${startAt.packetSerialNumber}  lastPacketSerialNumber=$lastPacketSerialNumber")
                Thread.dumpStack()
                return startAt
            }

            var packetSerialNumber = startAt.packetSerialNumber
            var offsetInPacket = startAt.offsetInPacket
            if (packetSerialNumber < firstPacketSerialNumber) { // Can happen if we trimmed old packets
                packetSerialNumber = firstPacketSerialNumber
                offsetInPacket = 0
            }
            var packet = packetsMap[packetSerialNumber]

            resultDataPointer.set(startAt) // This is a class variable (vs. local variable) only as an optimization to prevent excessive allocations. It's safe inside the synchronized block
            while (packet != null) {
                // It's possible to have packet.data.size == offsetInPacket. It's perfectly OK,
                // and in this case we should simply move to the next packet. This happens when
                // we process a packet (which sets the "next-byte-to-process" to packet.size,
                // and then a new packet is started (without adding any data to the current
                // packet) e.g. when a long time passes before we receive the next data.
                if (packet.data.size > offsetInPacket) {
                    // Timber.d("processData() calling processor(). packetSerialNumber=$packetSerialNumber offsetInPacket=$offsetInPacket")
                    processor(packet.data, packetSerialNumber, offsetInPacket, packet.direction, packet.timeStamp)
                }
                resultDataPointer.set(packetSerialNumber, packet.data.size)
                packetSerialNumber++
                packet = packetsMap[packetSerialNumber]
                offsetInPacket = 0
            }
            return DataPointer(resultDataPointer)
        }
    }

    fun getCurrentLocation() = DataPointer(
        lastPacketSerialNumber,
        packetsMap[lastPacketSerialNumber]?.data?.size ?: 0
    )

    /**
     * @return Total input and output bytes stored as Pair(totalInBytes, totalOutBytes)
     */
    fun getTotalSize(): Pair<Int, Int> {
        synchronized(this) {
            return Pair(totalBytesIn, totalBytesOut)
        }
    }

    /** Delete all stored packets and free all memory they occupy */
    fun clear() {
        synchronized(this) {
            packetsMap.clear()
            firstPacketSerialNumber = lastPacketSerialNumber
            totalBytesIn = 0
            totalBytesOut = 0
            lastPacketStartTime = 0L
            lastDirection = DataDirection.UNDEFINED
        }
    }

    /** @return the added packet's serial number */
    private fun addPacket(packetSerialNumber: Int, currentTime: Long, direction: DataDirection): Int {
        lastPacketStartTime = currentTime
        packetsMap[packetSerialNumber] = IOPacket(maxPacketSize, direction, packetSerialNumber, lastPacketStartTime)
        return packetSerialNumber
    }

    private class IOPacket(
        len: Int,
        val direction: DataDirection,
        @Suppress("unused") val serialNumber: Int,
        val timeStamp: Long,
    ) {
        val data: ByteArray
            get() = byteArrayOutputStream.toByteArray()

        private val byteArrayOutputStream = ByteArrayOutputStream(len)

        /** Append data to this packet. This does not change the timeStamp of the packet. */
        fun append(data: ByteArray, offset: Int, len: Int) {
            byteArrayOutputStream.write(data, offset, len)
        }
    }
}