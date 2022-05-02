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
package com.liorhass.android.usbterminal.free.usbserial

import android.content.Context
import android.hardware.usb.UsbManager
import com.hoho.android.usbserial.driver.*
import timber.log.Timber
import java.io.IOException
import com.hoho.android.usbserial.driver.UsbSerialPort as DriverUsbSerialPort


class UsbSerialPort(
    val usbSerialDevice: UsbSerialDevice,
    val portNumber: Int = 0,
    private val baudRate: Int = 9600,
    private val dataBits: Int = 8,
    private val stopBits: Int = STOPBITS_1,
    private val parity: Int = PARITY_NONE,
    private val onDataReceived: ((receivedData: ByteArray, inputPaused: Boolean) -> Unit)? = null,
    private val onError: ((exception: Exception) -> Unit)? = null,
) {

    constructor(
        usbSerialDevice: UsbSerialDevice,
        portNumber: Int,
        serialCommunicationParams: SerialCommunicationParams,
        onDataReceived: (receivedData: ByteArray, inputPaused: Boolean) -> Unit,
        onError: (exception: Exception) -> Unit,
    ) : this(
        usbSerialDevice = usbSerialDevice,
        portNumber = portNumber,
        baudRate = serialCommunicationParams.baudRate,
        dataBits = serialCommunicationParams.dataBits,
        stopBits = serialCommunicationParams.stopBits,
        parity = serialCommunicationParams.parity,
        onDataReceived = onDataReceived,
        onError = onError,
    )

    @Suppress("unused")
    companion object {
        const val PARITY_NONE = DriverUsbSerialPort.PARITY_NONE
        const val PARITY_ODD = DriverUsbSerialPort.PARITY_ODD
        const val PARITY_EVEN = DriverUsbSerialPort.PARITY_EVEN
        const val PARITY_MARK = DriverUsbSerialPort.PARITY_MARK
        const val PARITY_SPACE = DriverUsbSerialPort.PARITY_SPACE
        const val STOPBITS_1 = DriverUsbSerialPort.STOPBITS_1
        const val STOPBITS_1_5 = DriverUsbSerialPort.STOPBITS_1_5
        const val STOPBITS_2 = DriverUsbSerialPort.STOPBITS_2
    }

    enum class ErrorCode {
        OK,
        NOT_CONNECTED,
    }

    data class SerialCommunicationParams(
        val baudRate: Int,
        val dataBits: Int,
        val stopBits: Int,
        val parity: Int,
    )

    enum class ConnectStatusCode {
        IDLE,
        CONNECTED,
        ERR_NO_PERMISSION,
        ERR_ALREADY_CONNECTED,
        // ERR_DEVICE_WITH_SPECIFIED_ID_NOT_FOUND,
        ERR_UNRECOGNIZED_DEVICE_TYPE,
        ERR_NO_SUCH_PORT_NUMBER,
        ERR_OPEN_DEVICE_FAILED,
        ERR_OPEN_PORT_FAILED,
        ERR_SET_PORT_PARAMETERS_FAILED,
        ERR_MISSING_HANDLERS,
    }
    class ConnectResult(val statusCode: ConnectStatusCode, val msg: String)

    /** Indicates whether this port is currently connected or not */
    var isConnected = false

    private var driverUsbSerialPort: DriverUsbSerialPort? = null

    private var usbPortBufferedIoManager: UsbPortBufferedIoManager? = null


    fun isEqual(that: UsbSerialPort?): Boolean {
        if (that == null) return false
        return this.portNumber == that.portNumber  &&
                this.usbSerialDevice.usbDevice.deviceId == that.usbSerialDevice.usbDevice.deviceId
    }

    /**
     * Connect to the USB port
     *
     * This is a lengthy operation. It should be called on a worker thread
     */
    fun connect(context: Context): ConnectResult {
        // Timber.d("connect()")
        if (isConnected) {
            return ConnectResult(ConnectStatusCode.ERR_ALREADY_CONNECTED, "Already connected")
        }
        if (onDataReceived == null  ||  onError == null) {
            return ConnectResult(ConnectStatusCode.ERR_MISSING_HANDLERS, "Unspecified onDataReceived() or onError()")
        }

        val usbDevice = usbSerialDevice.usbDevice

        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

        // Verify that we have been granted permission to access the USB device
        if (! usbManager.hasPermission(usbDevice)) {
            return ConnectResult(
                ConnectStatusCode.ERR_NO_PERMISSION,
                "No permission granted to access USB device: $usbDevice"
            )
        }

        // Get an instance of the driver corresponding to the USB device type
        val usbSerialDriver = usbSerialDevice.driver
            ?: return ConnectResult(
                ConnectStatusCode.ERR_UNRECOGNIZED_DEVICE_TYPE,
                "Unrecognized device type. Device=$usbDevice"
            )

        // Verify that the specified port number is in range of device's available ports
        if (usbSerialDriver.ports.size < portNumber) {
            return ConnectResult(
                ConnectStatusCode.ERR_NO_SUCH_PORT_NUMBER,
                "Invalid port number: $portNumber. Device has only ${usbSerialDriver.ports.size} ports."
            )
        }

        // Open the device
        val usbConnection = usbManager.openDevice(usbDevice)
            ?: return ConnectResult(
                ConnectStatusCode.ERR_OPEN_DEVICE_FAILED,
                "UsbManager#openDevice() failed for device: $portNumber."
            )

        // Open the port and set it's parameters (baudRate, dataBits, stopBits, parity)
        driverUsbSerialPort = usbSerialDriver.ports[portNumber]
        // Timber.d("connect(): portNumber=$portNumber driverUsbSerialPort=$driverUsbSerialPort")
        try {
            driverUsbSerialPort?.open(usbConnection)
        } catch (e: Exception) {
            Timber.e(e, "driverUsbSerialPort#open() failed: ${e.message}")
            disconnect()
            return ConnectResult(
                ConnectStatusCode.ERR_OPEN_PORT_FAILED,
                "Open port failed. Port number: $portNumber."
            )
        }
        try {
            driverUsbSerialPort?.setParameters(baudRate, dataBits, stopBits, parity)
        } catch (e: Exception) {
            Timber.e(e, "driverUsbSerialPort#setParameters() failed: ${e.message}")
            disconnect()
            return ConnectResult(
                ConnectStatusCode.ERR_SET_PORT_PARAMETERS_FAILED,
                "Set port parameters failed. Port number: $portNumber."
            )
        }
        usbPortBufferedIoManager = UsbPortBufferedIoManager(onDataReceived, onError)
        driverUsbSerialPort?.let {
            usbPortBufferedIoManager?.connect(it)
        }

        isConnected = true

        return ConnectResult(ConnectStatusCode.CONNECTED, "OK")
    }

    fun disconnect() {
        // Timber.d("$$$ disconnect()")
        try {
            driverUsbSerialPort?.close()
        } catch (ignored: IOException) {}
        driverUsbSerialPort = null

        usbPortBufferedIoManager?.disconnect()

        isConnected = false
    }

    suspend fun write(data: ByteArray): ErrorCode {
        if (! isConnected) {
            Timber.e("write(): Illegal state: Not-Connected")
            return ErrorCode.NOT_CONNECTED
        }
        usbPortBufferedIoManager?.write(data)
        return ErrorCode.OK
    }

    /** Set the DTR (Data Terminal Ready) line of the UART to the specified value */
    fun setDTR(value: Boolean) {
        driverUsbSerialPort?.dtr = value
    }
    /** Set the RTS (Request To Send) line of the UART to the specified value */
    fun setRTS(value: Boolean) {
        driverUsbSerialPort?.rts = value
    }
    /** Get the DTR (Data Terminal Ready) line of the UART */
    fun getDTR(): Boolean = driverUsbSerialPort?.dtr ?: false
    /** Get the RTS (Request To Send) line of the UART */
    fun getRTS(): Boolean = driverUsbSerialPort?.rts ?: false
    /** Get the CD (Carrier Detect) line of the UART */
    @Suppress("unused") fun getCD(): Boolean = driverUsbSerialPort?.cd ?: false
    /** Get the CTS (Clear To Send) line of the UART */
    @Suppress("unused") fun getCTS(): Boolean = driverUsbSerialPort?.cts ?: false
    /** Get the DSR (Data Set Ready) line of the UART */
    @Suppress("unused") fun getDSR(): Boolean = driverUsbSerialPort?.dsr ?: false
    /** Get the RI (Ring Indicator) line of the UART */
    @Suppress("unused") fun getRI(): Boolean = driverUsbSerialPort?.ri ?: false
}


// For debugging only
//class DummyUsbSerialDevice(
//    val vendorId: Int,
//    val productId: Int,
//    val deviceType: UsbSerialDevice.DeviceType,
//    val deviceTypeStr: String, // "CDC/ACM", "FTDI", "CH34x", "CP21xx", "Prolific", "Unrecognized"
//    val driver: UsbSerialDriver? = null,
//)
//class DummyUsbSerialPort(
//    vendorId: Int,
//    productId: Int,
//    val deviceType: UsbSerialDevice.DeviceType,
//    deviceTypeStr: String, // "CDC/ACM", "FTDI", "CH34x", "CP21xx", "Prolific", "Unrecognized"
//    val portNumber: Int,
//    val isConnected: Boolean,
//) {
//    val usbSerialDevice = DummyUsbSerialDevice(vendorId, productId, deviceType, deviceTypeStr)
//}