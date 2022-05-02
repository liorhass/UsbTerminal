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

/**
 * Get a list of all USB ports that are currently connected to our Android device.
 * Typically a USB device has one port, so usually this is a list of all connected devices. If
 * a device has multiple ports, every port is listed separately in the list.
 */
fun getSerialPortList(context: Context): List<UsbSerialPort> {
    val usbDefaultProber = UsbSerialProber.getDefaultProber()
    val portList = mutableListOf<UsbSerialPort>()

    for (device in (context.getSystemService(Context.USB_SERVICE) as UsbManager).deviceList.values) {

        // todo: this isn't correct when we're given the device type by the user
        val usbSerialDevice = UsbSerialDevice(device, UsbSerialDevice.DeviceType.AUTO_DETECT, usbDefaultProber)

        // See if we recognize this device (by vendor and product IDs). If we do, return the appropriate driver.
        val driver = usbDefaultProber.probeDevice(device)

        if (driver != null) {
            for (portNumber in 0 until usbSerialDevice.nPorts) {
                portList.add(UsbSerialPort(usbSerialDevice, portNumber))
            }
        } else {
            portList.add(UsbSerialPort(usbSerialDevice))
        }
    }
    return portList
}

fun driverToDeviceType(driver: UsbSerialDriver?): UsbSerialDevice.DeviceType {
    return driver?.let {
        when (it) {
            is CdcAcmSerialDriver -> UsbSerialDevice.DeviceType.CDC_ACM
            is FtdiSerialDriver -> UsbSerialDevice.DeviceType.FTDI
            is Ch34xSerialDriver -> UsbSerialDevice.DeviceType.CH34X
            is Cp21xxSerialDriver -> UsbSerialDevice.DeviceType.CP21XX
            is ProlificSerialDriver -> UsbSerialDevice.DeviceType.PROLIFIC
            else -> UsbSerialDevice.DeviceType.UNRECOGNIZED
        }
    } ?: UsbSerialDevice.DeviceType.UNRECOGNIZED
}

//fun getDummySerialPortList(): List<DummyUsbSerialPort> {
//    val result = mutableListOf<DummyUsbSerialPort>()
//    result.add(DummyUsbSerialPort(
//        vendorId = 7777,
//        productId = 33,
//        deviceType = UsbSerialDevice.DeviceType.CDC_ACM,
//        deviceTypeStr = "CDC/ACM", // "CDC/ACM", "FTDI", "CH34x", "CP21xx", "Prolific", "Unrecognized"
//        portNumber = 0,
//        isConnected = true,
//    ))
//    result.add(DummyUsbSerialPort(
//        vendorId = 666,
//        productId = 1,
//        deviceType = UsbSerialDevice.DeviceType.UNRECOGNIZED,
//        deviceTypeStr = "Unrecognized", // "CDC/ACM", "FTDI", "CH34x", "CP21xx", "Prolific", "Unrecognized"
//        portNumber = 0,
//        isConnected = false,
//    ))
//    result.add(DummyUsbSerialPort(
//        vendorId = 2200,
//        productId = 10,
//        deviceType = UsbSerialDevice.DeviceType.PROLIFIC,
//        deviceTypeStr = "Prolific", // "CDC/ACM", "FTDI", "CH34x", "CP21xx", "Prolific", "Unrecognized"
//        portNumber = 0,
//        isConnected = false,
//    ))
//    result.add(DummyUsbSerialPort(
//        vendorId = 667,
//        productId = 11,
//        deviceType = UsbSerialDevice.DeviceType.AUTO_DETECT,
//        deviceTypeStr = "Unrecognized", // "CDC/ACM", "FTDI", "CH34x", "CP21xx", "Prolific", "Unrecognized"
//        portNumber = 0,
//        isConnected = false,
//    ))
//
//    return result
//}
