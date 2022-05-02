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

import android.hardware.usb.UsbDevice
import com.hoho.android.usbserial.driver.*

class UsbSerialDevice(
    val usbDevice: UsbDevice,
    var deviceType: DeviceType /*= DeviceType.AUTO_DETECT*/,
    private var usbSerialProber: UsbSerialProber? = null // When null we use the defaultProber
) {
    val id: Int         get() = usbDevice.deviceId
    val name: String    get() = usbDevice.deviceName
    val vendorId: Int   get() = usbDevice.vendorId
    val productId: Int  get() = usbDevice.productId
    var nPorts = 0
    val deviceTypeStr: String   get() = getDeviceTypeStr(deviceType)
    var driver: UsbSerialDriver? = null

    enum class DeviceType {
        AUTO_DETECT, CDC_ACM, FTDI, CH34X, CP21XX, PROLIFIC, UNRECOGNIZED
    }

    init {
        if (deviceType == DeviceType.AUTO_DETECT) {
            if (usbSerialProber == null) {
                usbSerialProber = UsbSerialProber.getDefaultProber() // todo: add support for CustomProber. See com.hoho.android.usbserial.examples.TerminalFragment line#182
            }
            driver = usbSerialProber?.probeDevice(usbDevice)
            deviceType = driverToDeviceType(driver)
        } else {
            // We were given a specific device type
            driver = getDriverForDeviceType(deviceType, usbDevice)
        }
        nPorts = driver?.ports?.size ?: 0
    }

    companion object {
        fun getDriverForDeviceType(deviceType: DeviceType, usbDevice: UsbDevice): UsbSerialDriver? {
            // Timber.d("getDriverForDeviceType() deviceType=${deviceType.name}")
            return when (deviceType) {
                DeviceType.CDC_ACM -> CdcAcmSerialDriver(usbDevice)
                DeviceType.FTDI -> FtdiSerialDriver(usbDevice)
                DeviceType.CH34X -> Ch34xSerialDriver(usbDevice)
                DeviceType.CP21XX -> Cp21xxSerialDriver(usbDevice)
                DeviceType.PROLIFIC -> ProlificSerialDriver(usbDevice)
                else -> null
            }
        }
    }

    private fun getDeviceTypeStr(deviceType: DeviceType): String {
        return when (deviceType) {
            DeviceType.CDC_ACM -> "CDC/ACM"
            DeviceType.FTDI -> "FTDI"
            DeviceType.CH34X -> "CH34x"
            DeviceType.CP21XX -> "CP21xx"
            DeviceType.PROLIFIC -> "Prolific"
            else -> "Unrecognized"
        }
    }
}