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

import android.app.*
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_MIN
import com.liorhass.android.usbterminal.free.R
import com.liorhass.android.usbterminal.free.main.MainActivity
import com.liorhass.android.usbterminal.free.settings.model.SettingsRepository
import com.liorhass.android.usbterminal.free.usbserial.UsbSerialDevice
import com.liorhass.android.usbterminal.free.usbserial.UsbSerialPort
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.*


class UsbCommService : Service() {

    companion object {
        const val NOTIFICATION_CHANNEL_ID_FOR_FOREGROUND_SERVICE = "UTFSChannelId"
        const val FOREGROUND_SERVICE_NOTIFICATION_ID = 7734
        const val STOP_SELF = "UTStopSelf"
    }

    enum class ErrorCode {
        OK,
        NOT_CONNECTED,
    }

    private val job = SupervisorJob()
    private val defaultScope = CoroutineScope(Dispatchers.Default + job)
    lateinit var settingsRepository: SettingsRepository
    private var settingsData = SettingsRepository.SettingsData()
    private var isStopping = false
    private var isForeground = false

    private lateinit var usbManager: UsbManager

    private var usbSerialPort: UsbSerialPort? = null

    /** Holds a list of all packets received. This is an observable class. Consumers of newly received
     * data are expected to register as observers. They are then notified when new data arrives. */
    val ioPacketsList = IOPacketsList(settingsData.maxBytesToRetainForBackScroll)

    private val observable = UsbCommServiceObservable()
    private class UsbCommServiceObservable : Observable() {
        // This class is only needed to convert a protected method to public, because we want to
        // hold an Observable object not inherit from it (favour composition over inheritance)
        public override fun setChanged() {
            super.setChanged()
        }
    }
    class Event(val eventType: Type, val obj: Any? = null) {
        enum class Type {
            CONNECTED,            // obj is UsbConnectionParams
            DISCONNECTED,         // obj is UsbDisconnectionParams
            CONNECTION_ERROR,     // obj is UsbSerialPort.ConnectResult
            NO_USB_PERMISSION,    // obj is UsbPermissionRequestParams
            SHOULD_UNBIND_SERVICE,
            UNRECOGNIZED_DEVICE_TYPE,
        }
        class UsbConnectionParams(val usbSerialPort: UsbSerialPort)
        class UsbDisconnectionParams(val exception: Exception?)
        class UsbPermissionRequestParams(val usbDevice: UsbDevice, val portNumber: Int, val deviceType: UsbSerialDevice.DeviceType)
    }
    fun addObserver(observer: Observer) {observable.addObserver(observer)}
    fun deleteObserver(observer: Observer) {observable.deleteObserver(observer)}

    private val binder = CommunicationServiceBinder() // Binder given to clients
    private var logFile: LogFile? = null

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository.getInstance(application)
        defaultScope.launch {
            settingsRepository.settingsStateFlow.collect {
                if (it.maxBytesToRetainForBackScroll != settingsData.maxBytesToRetainForBackScroll) {
                    ioPacketsList.setMaxSize(it.maxBytesToRetainForBackScroll)
                }
                settingsData = it
                if (logFile == null && settingsData.logSessionDataToFile) {
                    val logFileResult = LogFile.Builder.getLogFileAsync(
                        context = application,
                        coroutineScope = defaultScope,
                        ioPacketsList = ioPacketsList
                    ).await()
                    logFile = logFileResult.getOrNull()
                    if (logFile == null) {
                        Timber.e("Can't create log-file. ${logFileResult.exceptionOrNull()?.message}") //todo: show error msg to user
                    }
                    // Timber.d("Log file created. Name: ${logFile?.fileName}")
                } else if (!settingsData.logSessionDataToFile) {
                    logFile?.close()
                    logFile = null
                }
                logFile?.updateSettings(settingsData)
            }
        }
        // Timber.d("onCreate(): log-file:'${logFile?.fileName}'")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        // Timber.d("onStartCommand() intent.stop=${intent?.getBooleanExtra(STOP_SELF, false)}")

        // User asked us to stop from the notification shade
        if (intent?.getBooleanExtra(STOP_SELF, false) == true) {
            Timber.d("onStartCommand(): Stopping self")
            defaultScope.launch {
                notifyObservers(Event(Event.Type.SHOULD_UNBIND_SERVICE)) // Ask observers (i.e. MainActivityViewModel) to unbind so we can stop ourselves.
                stopSelf()
            }
            return START_NOT_STICKY
        }

        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager

        return START_STICKY
    }

     // Class used for the client Binder.
    inner class CommunicationServiceBinder : Binder() {
        // Return this instance of Service so clients can call public methods
        fun getService(): UsbCommService = this@UsbCommService
    }

    override fun onBind(intent: Intent?): IBinder {
        // Timber.d("onBind()")
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        // Timber.d("onDestroy()")
        logFile?.close()
        logFile = null
        job.cancel()
    }

    // From https://stackoverflow.com/a/47549638/1071117
    fun becomeForegroundService() {
        Timber.d("becomeForegroundService(): isStopping=$isStopping")
        if (isStopping) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }

        val openActivityIntent = Intent(this, MainActivity::class.java)
        val openActivityPendingIntent= PendingIntent.getActivity(this, 0, openActivityIntent, PendingIntent.FLAG_IMMUTABLE)

        val stopServiceIntent = Intent(this, UsbCommService::class.java).apply {
            putExtra(STOP_SELF, true)
        }
        val stopServicePendingIntent = PendingIntent.getService(this, 0, stopServiceIntent, PendingIntent.FLAG_IMMUTABLE)

        val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID_FOR_FOREGROUND_SERVICE)
        val notification = notificationBuilder
            .setContentTitle(getString(R.string.fg_service_notification_title))
//            .setContentText("content text") // todo: this line should indicate the state of usbSerialPort.isConnected
            .setSmallIcon(R.drawable.ic_baseline_monitor_24)
            .setColor(resources.getColor(R.color.teal_900, theme))
            .setPriority(PRIORITY_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setOngoing(true)
            .setSilent(true)
            .addAction(R.drawable.ic_baseline_monitor_24, getString(R.string.open_app_all_caps), openActivityPendingIntent)
            .addAction(R.drawable.ic_baseline_clear_24, getString(R.string.stop_all_caps), stopServicePendingIntent)
            .build()
        startForeground(FOREGROUND_SERVICE_NOTIFICATION_ID, notification)
        isForeground = true
    }

    fun becomeBackgroundService() {
        Timber.d("becomeBackgroundService(): isForeground=$isForeground")
        if (isForeground) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            isForeground = false
        }
    }

    /**
     * Connect to a USB device
     *
     * USB connection is a lengthy operation, so this function launches a
     * coroutine to execute it on a background thread and returns immediately.
     * The result of the connection attempt is returned by a notification to
     * our observers. Possible results are:
     * CONNECTED, ERR_NO_PERMISSION, CONNECTION_ERROR
     */
    fun connectToUsbPort(
        usbDevice: UsbDevice,
        portNumber: Int,
        deviceType: UsbSerialDevice.DeviceType,
        serialCommunicationParams: UsbSerialPort.SerialCommunicationParams,
        setDTROnConnect: Boolean,
        setRTSOnConnect: Boolean,
    ) {
        defaultScope.launch {
            // Verify that we have permission to access this device
            if (!usbManager.hasPermission(usbDevice)) {
                notifyObservers(
                    Event(
                        eventType = Event.Type.NO_USB_PERMISSION,
                        obj = Event.UsbPermissionRequestParams(usbDevice, portNumber, deviceType)
                    )
                )
                return@launch
            }

            // If we were asked to connect to the same port already connected, do nothing
            usbSerialPort?.let { currentlyConnectedPort ->
                if (currentlyConnectedPort.usbSerialDevice.usbDevice.deviceId == usbDevice.deviceId &&
                    currentlyConnectedPort.portNumber == portNumber) {
                    Timber.d("connectToUsbPort(): Already connected to this port. Doing nothing.")
                    return@launch
                }
                // Disconnect currently connected port before connecting to a new one
                currentlyConnectedPort.disconnect()
            }

            val usbSerialDevice = UsbSerialDevice(usbDevice, deviceType) // This is a lengthy operation (probing)
            Timber.d("connectToUsbPort(): DeviceType=${usbSerialDevice.deviceType.name} port#=$portNumber baud=${serialCommunicationParams.baudRate} nBits=${serialCommunicationParams.dataBits} nStopBits=${serialCommunicationParams.stopBits} parity=${serialCommunicationParams.parity}")
            if (usbSerialDevice.deviceType != UsbSerialDevice.DeviceType.UNRECOGNIZED) {
                usbSerialPort = UsbSerialPort(
                    usbSerialDevice = usbSerialDevice,
                    portNumber = portNumber,
                    serialCommunicationParams = serialCommunicationParams,
                    onDataReceived = ::onUsbDataReceived, // { receivedData -> onUsbDataReceived(receivedData) },
                    onError = ::onUsbError //{ exception -> onUsbError(exception) }
                )
                usbSerialPort?.let { usbSerialPort ->
                    val connectResult = usbSerialPort.connect(application) // This is a lengthy operation
                    when (connectResult.statusCode) {
                        UsbSerialPort.ConnectStatusCode.CONNECTED -> {
                            Timber.d("connectToUsbPort(): Connected successfully")
                            setDTR(setDTROnConnect) // On devices like 32u4, usually DTR must be set for in-bound transfers to work. https://stackoverflow.com/a/59224470/1071117
                            setRTS(setRTSOnConnect)
                            notifyObservers(
                                Event(
                                    Event.Type.CONNECTED,
                                    Event.UsbConnectionParams(usbSerialPort = usbSerialPort)
                                )
                            )
                        }
                        else -> {
                            // None of these should happen
                            // UsbSerialPort.ConnectStatusCode.ERR_NO_PERMISSION
                            // UsbSerialPort.ConnectStatusCode.ERR_ALREADY_CONNECTED,
                            // UsbSerialPort.ConnectStatusCode.ERR_DEVICE_WITH_SPECIFIED_ID_NOT_FOUND,
                            // UsbSerialPort.ConnectStatusCode.ERR_UNRECOGNIZED_DEVICE_TYPE,
                            // UsbSerialPort.ConnectStatusCode.ERR_NO_SUCH_PORT_NUMBER,
                            // UsbSerialPort.ConnectStatusCode.ERR_OPEN_DEVICE_FAILED,
                            // UsbSerialPort.ConnectStatusCode.ERR_OPEN_PORT_FAILED,
                            // UsbSerialPort.ConnectStatusCode.ERR_SET_PORT_PARAMETERS_FAILED
                            Timber.w("connectToUsbPort(): Error in usbSerialPort.connect(). errCode=${connectResult.statusCode} usbDevice=$usbDevice")
                            notifyObservers(Event(Event.Type.CONNECTION_ERROR, connectResult))
                        }
                    }
                }
            } else {
                notifyObservers(Event(Event.Type.UNRECOGNIZED_DEVICE_TYPE))
            }
        }
    }

    /**
     * Disconnect from a USB port
     *
     * USB disconnection is a lengthy operation, so this function launches a
     * coroutine to execute it on a background thread and returns immediately.
     * After the device is disconnected a notification is sent to our
     * observers with event type of DISCONNECTED. If the disconnection was
     * triggered due to an error, the accompanied object holds the Exception.
     * This Exception is null on normal disconnection.
     */
    fun disconnectFromUsbPort(exception: Exception?) {
        usbSerialPort?.let {
            usbSerialPort = null
            defaultScope.launch {
                Timber.d("disconnectFromUsbDevice()")
                it.disconnect()
                notifyObservers(Event(Event.Type.DISCONNECTED, Event.UsbDisconnectionParams(exception)))
            }
        } ?: run {
            Timber.w("disconnectFromUsbPort(): usbSerialPort is null")
        }
    }

    /** Called when a new data is received by UsbSerialPort. */
    private fun onUsbDataReceived(receivedData: ByteArray, inputPaused: Boolean) {
        // Timber.d("onUsbDataReceived() len=${receivedData.size} inputPaused=$inputPaused \n${HexDump.dumpHexString(receivedData)}")

        ioPacketsList.appendData(receivedData, IOPacketsList.DataDirection.IN)
        if (inputPaused) {
            // The queue is empty, so we notify the IoPacketList so it will ask the UI
            // thread to display the newly received data
            // Timber.d("ReceivedDataProcessingRunnable: notifying observers. nObservers=${ioPacketList.countObservers()}")
            ioPacketsList.inputPaused() // notifyObservers()
        }
    }

    private fun onUsbError(exception: Exception) {
        Timber.e("onUsbError(): exception=${exception}")
        // Disconnect from the USB device and notify observers
        disconnectFromUsbPort(exception)
    }

    /**
     * Write data to both USB-Port and IOPacketsList
     *
     * @return ErrorCode.OK or ErrorCode.NOT_CONNECTED. When loopBack mode is on,
     * not being connected is not an error, and ErrorCode.OK is always returned
     */
    fun sendUsbData(data: ByteArray): ErrorCode {
        // Timber.d("sendUsbData() len=${data.size} \n${HexDump.dumpHexString(data)}")
        val isLoopBackOn = settingsData.loopBack
        var rc = ErrorCode.OK
        usbSerialPort?.let { port ->
            if (port.isConnected || isLoopBackOn) {
                // These operations shouldn't take long by themselves, but they're done in a synchronise{}
                // block which might cause a long block of the thread. For this reason we move this
                // operation to a thread in Dispatchers.Default pool.
                defaultScope.launch {
                    if (port.isConnected) {
                        port.write(data)
                        ioPacketsList.appendData(data, IOPacketsList.DataDirection.OUT)
                    }

                    if (isLoopBackOn) {
                        ioPacketsList.appendData(data, IOPacketsList.DataDirection.IN)
                    }

                    ioPacketsList.inputPaused() // Tells ioPacketsList that it should notify its observers that there's new data
                }
            } else {
                Timber.w("sendUsbData(): usbSerialPort is not connected")
                rc = ErrorCode.NOT_CONNECTED
            }
        } ?: run {
            if (isLoopBackOn) {
                defaultScope.launch {
                    ioPacketsList.appendData(data, IOPacketsList.DataDirection.IN)
                    ioPacketsList.inputPaused() // Tells ioPacketsList that it should notify its observers that there's new data
                }
            } else {
                Timber.w("sendUsbData(): usbSerialPort is null")
                rc = ErrorCode.NOT_CONNECTED
            }
        }
        return rc
    }

    /** Set the DTR line of the UART to the specified value */
    fun setDTR(value: Boolean) {
        usbSerialPort?.setDTR(value)
    }
    /** Set the RTS line of the UART to the specified value */
    fun setRTS(value: Boolean) {
        usbSerialPort?.setRTS(value)
    }
    fun getDTR(): Boolean = usbSerialPort?.getDTR() ?: false
    fun getRTS(): Boolean = usbSerialPort?.getRTS() ?: false

    /** Erase all data stored in ioPacketsList */
    fun eraseBufferedData() {
        ioPacketsList.clear()
    }

    fun stop() {
        isStopping = true
        stopSelf()
    }

    /**
     * Notify our observers. This is done on the Main thread to enable
     * UI operations in the observers' callbacks
     */
    private suspend fun notifyObservers(event: Event) {
        withContext(Dispatchers.Main) {
            Timber.d("notifyObservers() eventType=${event.eventType.name}")
            observable.setChanged()
            observable.notifyObservers(event)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (notificationManager.getNotificationChannel(
                NOTIFICATION_CHANNEL_ID_FOR_FOREGROUND_SERVICE
            ) == null) {
            val srvNotificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID_FOR_FOREGROUND_SERVICE,
                getString(R.string.usb_comm_service_notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setSound(null, null)
                importance = NotificationManager.IMPORTANCE_DEFAULT
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(srvNotificationChannel)
        }
    }

    fun debug(param: Char) {
        usbCommServiceDebug(param, defaultScope, ioPacketsList)
    }
}

