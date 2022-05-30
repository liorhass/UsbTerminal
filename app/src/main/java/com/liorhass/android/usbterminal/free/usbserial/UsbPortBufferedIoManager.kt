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
package com.liorhass.android.usbterminal.free.usbserial

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import timber.log.Timber

/**
 * Interface with the low-level USB driver code com.hoho.android.usbserial.driver.UsbSerialPort
 *
 * Implement a buffer mechanism so read and write operations return quickly.
 * There are two queues: input and output. When this class is initialized
 * by a call to its connect() method, it creates three constantly-running
 * coroutines:
 *
 * 1: Read received data from the USB driver and put it in the input queue
 *
 * 2: Retrieve data from the input queue and pass it the the consumer by
 *    calling the onDataReceived() callback
 *
 * 3: Retrieve data from the output queue and pass it to the USB driver
 *    for transmission. Data is inserted to this queue by calling this
 *    class' write() method
 */
class UsbPortBufferedIoManager(
    private val onDataReceived: (receivedData: ByteArray, inputPaused: Boolean) -> Unit,
    private val onError: (exception: Exception) -> Unit,
) {
    private val inputChannel: Channel<ByteArray> = Channel(100)
    private val outputChannel: Channel<ByteArray> = Channel(100)
    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var inputDispatcherJob: Job? = null
    private var inputJob: Job?  = null
    private var outputJob: Job? = null

    @OptIn(ExperimentalCoroutinesApi::class) // For inputChannel.isEmpty (at 20220331)
    fun connect(driverUsbSerialPort: com.hoho.android.usbserial.driver.UsbSerialPort) {
        Timber.d("UsbPortBufferedIoManager#connect")
        // A coroutine that constantly awaits newly received data from the USB device.
        // When new data is received it only put it in our inputChannel. This is done
        // in order to be able to handle received data very fast so we won't drop
        // any received bytes.
        // I know, this coroutine blocks its thread (very unorthodoxy), so Dispatchers.IO
        // pool has one less thread than what was intended. Get over it!
        inputJob = ioScope.launch {
            val buf = ByteArray(driverUsbSerialPort.readEndpoint?.maxPacketSize ?: 1024)
            while (isActive) {
                try {
                    @Suppress("BlockingMethodInNonBlockingContext")
                    val len = driverUsbSerialPort.read(buf, 0)
//                    Timber.d("inputJob#Got new USB data. len=$len")
                    if (len > 0) {
                        val data = buf.copyOfRange(0, len)
                        inputChannel.send(data)
                    }
                } catch (e: java.lang.Exception) {
                    // After a call to disconnect() there's one send attempt on a close channel.
                    // This is OK and causes an NPE that can be ignored
                    if (isActive) {
                        Timber.e("Error reading from UsbSerialPort: $e")
                        onError.invoke(e)
                    }
                }
            }
        }

        // A coroutine that extracts newly received data that was put in inputChannel
        // and sed it upstream by calling receivedDataHandler()
        inputDispatcherJob = ioScope.launch {
            while (isActive) {
                val data = inputChannel.receive()
                onDataReceived.invoke(data, inputChannel.isEmpty)
            }
        }

        // Our write() method only put the data in our outputChannel. Here we launch a
        // coroutine that constantly waits for that data, and when there's data in
        // outputChannel it extracts it from the channel and sens it to the USB device.
        outputJob = ioScope.launch {
            while (isActive) {
                val outputData = outputChannel.receive()
                try {
                    @Suppress("BlockingMethodInNonBlockingContext")
                    driverUsbSerialPort.write(outputData, 0)
//                    Timber.d("outputJob#Wrote USB data. len=${outputData.size}")
                } catch (e: java.lang.Exception) {
                    if (isActive) {
                        Timber.e("Error writing to UsbSerialPort: ${e.message}")
                        onError.invoke(e)
                    }
                }
            }
        }
    }

    suspend fun write(data: ByteArray): UsbSerialPort.ErrorCode {
//        Timber.d("UsbPortBufferedIoManager#write() len=${data.size}")
        outputChannel.send(data)
        return UsbSerialPort.ErrorCode.OK
    }

    fun disconnect() {
        Timber.d("UsbPortBufferedIoManager#Disconnect()")
        outputJob?.cancel(); outputJob = null
        inputJob?.cancel(); inputJob = null
        inputDispatcherJob?.cancel(); inputDispatcherJob = null
        inputChannel.close()
        outputChannel.close()
    }
}
