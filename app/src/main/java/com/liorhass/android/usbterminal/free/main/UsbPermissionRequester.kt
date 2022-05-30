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

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.liorhass.android.usbterminal.free.usbserial.UsbSerialDevice
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber


@SuppressLint("StaticFieldLeak")
object UsbPermissionRequester {
    private const val INTENT_ACTION_USB_PERMISSION = "com.liorhass.android.usbterminal.free.usbPermission"
    private var activity: Activity? = null
    private var requestAlreadyDenied: Boolean = false
    private var onPermissionDecision: ((
        permissionGranted: Boolean,
        UsbDevice?,
        portNumber: Int,
        deviceType: UsbSerialDevice.DeviceType) -> Unit)? = null
    private var portNumber = 0
    private var deviceType: UsbSerialDevice.DeviceType = UsbSerialDevice.DeviceType.UNRECOGNIZED
    private var job: Job? = null

    data class PermissionRequestParams(
        val shouldRequestPermission: Boolean, // If false do nothing
        val shouldRequestEvenIfAlreadyDenied: Boolean = false,
        val usbDevice: UsbDevice? = null,
        val portNumber: Int = 0,
        val usbDeviceType: UsbSerialDevice.DeviceType = UsbSerialDevice.DeviceType.UNRECOGNIZED,
        val onPermissionDecision: ((
            permissionGranted: Boolean,
            usbDevice: UsbDevice?,
            portNumber: Int,
            deviceType: UsbSerialDevice.DeviceType) -> Unit)? = null
    )

    /**
     * Bind an Activity to this object
     *
     * Must be called once by the Activity before calling requestPermission().
     *
     * A call to [unbindActivity] must be made at activity's onDestroy()
     * to prevent memory leak
     */
    fun bindActivity(
        activity: ComponentActivity,
        shouldAskForPermissionFlow: StateFlow<PermissionRequestParams>,
        onPermissionRequested: () -> Unit // A callback that resets the state (set shouldRequestPermission to false) to enable one-time-event
    ) {
        if (job != null) {
            // Timber.d("bindActivity(): Canceling old job")
            job?.cancel()
        }
        this.activity = activity

        val filter = IntentFilter(INTENT_ACTION_USB_PERMISSION)
        activity.registerReceiver(usbPermissionBroadcastReceiver, filter)

        // As long as the activity is STARTED, collect a flow that indicates when
        // we should ask the user for permission.
        // This coroutine's scope is bound to Dispatchers.Main.immediate
        job = activity.lifecycleScope.launch {
            activity.repeatOnLifecycle(Lifecycle.State.STARTED) {
                shouldAskForPermissionFlow.collect { permissionRequestParams ->
                    if (permissionRequestParams.shouldRequestPermission) {
                        Timber.d("Got should-request-permission event. permissionRequestParams: device-name: ${permissionRequestParams.usbDevice?.deviceName}")
                        requestPermission(permissionRequestParams)
                        onPermissionRequested()
                    }
                }
            }
        }
    }

    fun unbindActivity() {
        if (this.activity == null) {
            throw IllegalStateException("Activity not bound")
        }
        activity?.unregisterReceiver(usbPermissionBroadcastReceiver)
        activity = null
    }

    /**
     * Ask the user for permission to access the specified USB device. User response will be
     * passed to the callback specified by `requestParams.onPermissionDecision`.
     * @param requestParams
     */
    private fun requestPermission(requestParams: PermissionRequestParams) {

        if (activity == null) {
            throw IllegalStateException("bindActivity() must be called before this function")
        }
        if (requestAlreadyDenied && !requestParams.shouldRequestEvenIfAlreadyDenied) {
            Timber.w("UsbPermissionRequester#requestPermission() already denied")
            return
        }

        // Save the callback to be called when the user decides, and the device's port number
        onPermissionDecision = requestParams.onPermissionDecision
        portNumber = requestParams.portNumber
        deviceType = requestParams.usbDeviceType

        @SuppressLint("InlinedApi")
        val usbPermissionIntent = PendingIntent.getBroadcast(
            activity,0, Intent(INTENT_ACTION_USB_PERMISSION), PendingIntent.FLAG_MUTABLE)
        // val usbPermissionIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        //     PendingIntent.getBroadcast(
        //         activity,0, Intent(INTENT_ACTION_USB_PERMISSION), PendingIntent.FLAG_MUTABLE)
        // } else {
        //     PendingIntent.getBroadcast(
        //         activity,0, Intent(INTENT_ACTION_USB_PERMISSION), 0)
        // }
        requestParams.usbDevice?.let { usbDevice ->
            Timber.d("Calling UsbManager.requestPermission() for device: ${usbDevice.deviceName}")
            (activity?.getSystemService(Context.USB_SERVICE) as UsbManager)
                .requestPermission(usbDevice, usbPermissionIntent)
        }
    }


    // This BroadcastReceiver is going to be called with the user's decision intent
    // https://developer.android.com/guide/topics/connectivity/usb/host#permission-d
    private val usbPermissionBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Timber.d("usbPermissionBroadcastReceiver.onReceive() intent.action=${intent.action}")
            if (intent.action == INTENT_ACTION_USB_PERMISSION) {
                synchronized(this) {
                    val permissionGranted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    if (! permissionGranted)
                        requestAlreadyDenied = true
                    onPermissionDecision?.invoke(permissionGranted, device, portNumber, deviceType)
                }
            }
        }
    }
}