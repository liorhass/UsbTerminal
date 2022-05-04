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

import android.annotation.SuppressLint
import android.app.Application
import android.content.*
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.IBinder
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.liorhass.android.usbterminal.free.R
import com.liorhass.android.usbterminal.free.settings.model.SettingsRepository
import com.liorhass.android.usbterminal.free.usbcommservice.IOPacketsList
import com.liorhass.android.usbterminal.free.usbcommservice.UsbCommService
import com.liorhass.android.usbterminal.free.usbserial.UsbSerialDevice
import com.liorhass.android.usbterminal.free.usbserial.UsbSerialPort
import com.liorhass.android.usbterminal.free.usbserial.getSerialPortList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.*

//TODO: split this large monolithic viewModel to few smaller ones
class MainViewModel(
    application: Application,
    initialIntent: Intent
) : AndroidViewModel(application) {

    /** This exposes the settings repository. The repository exposes settingsStateFlow attribute
     * which is a StateFlow representing the current settings state.
     *
     * In composables this can be accessed like:
     * `val settingsData by vm.settingsRepository.settingsStateFlow.collectAsStateLifecycleAware()`
     *
     * The current settings can be accessed like:
     * `vm.settingsRepository.settingsStateFlow.value` */
    val settingsRepository = SettingsRepository.getInstance(application)
    private var settingsData = SettingsRepository.SettingsData()

    data class UsbConnectionState(
        val statusCode: UsbSerialPort.ConnectStatusCode,
        val connectedUsbPort: UsbSerialPort? = null, // Available for statusCode==CONNECTED
        val msg: String = ""  // Available for all error statusCodes
    )
    private val _usbConnectionState = mutableStateOf(UsbConnectionState(UsbSerialPort.ConnectStatusCode.IDLE))
    val usbConnectionState: State<UsbConnectionState> = _usbConnectionState

    private val _portListState: MutableState<List<UsbSerialPort>> = mutableStateOf(emptyList())
    val portListState: State<List<UsbSerialPort>> = _portListState
    //dbg    private val _portListState: MutableState<List<DummyUsbSerialPort>> = mutableStateOf(emptyList())
    //dbg    val portListState: State<List<DummyUsbSerialPort>> = _portListState

    private val _shouldRequestUsbPermissionFlow = MutableStateFlow(UsbPermissionRequester.PermissionRequestParams(false))
    val shouldRequestUsbPermissionFlow: StateFlow<UsbPermissionRequester.PermissionRequestParams> = _shouldRequestUsbPermissionFlow.asStateFlow()
    fun usbPermissionWasRequested() {_shouldRequestUsbPermissionFlow.value = _shouldRequestUsbPermissionFlow.value.copy(shouldRequestPermission = false)}

    private val screenTextModel = ScreenTextModel(viewModelScope,80, ::sendBuf, settingsData.maxBytesToRetainForBackScroll)
    val screenLines = screenTextModel.screenLinesState
    val screenTextShouldScrollToBottom = screenTextModel.shouldScrollToBottom
    fun onScrolledToBottom() = screenTextModel.onScrolledToBottom()
    val cursorPosition = screenTextModel.displayedCursorPosition

    private val screenHexModel = ScreenHexModel(viewModelScope, 10_000)
    val screenHexTextBlocksState = screenHexModel.screenHexTextBlocksState
    val screenHexShouldScrollToBottom = screenHexModel.shouldScrollToBottom
    fun onScreenHexScrolledToBottom() = screenHexModel.onScrolledToBottom()

    data class ScreenDimensions(val width: Int, val height: Int)
    private val _screenDimensions = mutableStateOf(ScreenDimensions(0,0))
    val screenDimensions: State<ScreenDimensions> = _screenDimensions

    private val _ctrlButtonsRowVisible = mutableStateOf(false)
    val ctrlButtonsRowVisible: State<Boolean> = _ctrlButtonsRowVisible

    // Why is the flag shouldMeasureScreenDimensions an Int and not a Boolean?
    // Well, if it was a Boolean the following scenario might happen:
    //  1. A trigger is made to measure the screen, which sets the flag to true
    //  2. As a result recomposition happens and it measures the screen
    //  3. After the screen is measured a call is made to onScreenDimensionsMeasured()
    //     on a LaunchedEffect which reset the flag to false
    //  4. Before a recomposition happens (due to the flag reset) some other code
    //     triggers another measurement of the screen, which sets the flag to true again
    //  5. Finally the UI is ready to recompose, buf from Compose's perspective no
    //     recomposition is needed since the flag never changed (was true and is
    //     still true).
    // The solution is to use an Int instead of a Boolean. 0 marks that no measurement
    // is needed, and when a measurement is needed the flag is set to some unique
    // Int (different every time). The composition makes (totally dummy) use of this Int
    // which triggers a recomposition even when the flag's reset is missed.
    private var uid: Int = 1
        get() { field++; return field }
    private val _shouldMeasureScreenDimensions = mutableStateOf(uid)
    val shouldMeasureScreenDimensions: State<Int> = _shouldMeasureScreenDimensions
    fun onScreenDimensionsMeasured(screenDimensions: ScreenDimensions) {
        _shouldMeasureScreenDimensions.value = 0
        if (screenDimensions != _screenDimensions.value) {
            _screenDimensions.value = screenDimensions
            screenTextModel.setScreenDimensions(screenDimensions.width, screenDimensions.height)
            redrawScreen()
        }
    }
    fun remeasureScreenDimensions() {
        _shouldMeasureScreenDimensions.value = uid
    }

    // A flow that signals MainActivity to finish when it becomes true
    val shouldTerminateApp = MutableStateFlow(false)

    private val _shouldShowWelcomeMsg = mutableStateOf(false)
    val shouldShowWelcomeMsg: State<Boolean> = _shouldShowWelcomeMsg
    private val _shouldShowUpgradeFromV1Msg = mutableStateOf(false)
    val shouldShowUpgradeFromV1Msg: State<Boolean> = _shouldShowUpgradeFromV1Msg
    fun onUserAcceptedWelcomeOrUpgradeMsg() {
        _shouldShowWelcomeMsg.value = false
        _shouldShowUpgradeFromV1Msg.value = false
        // Timber.d("setShowedV2WelcomeMsg TRUE")
        settingsRepository.setShowedV2WelcomeMsg(true)
    }
    fun onUserDeclinedWelcomeOrUpgradeMsg() {
        // terminate app
        terminateApp()
    }

    // Text displayed in the "TextToXmitInputSection" text field. This is what we xmit when the "send" button is clicked
    private val _textToXmit = MutableStateFlow("")
    val textToXmit = _textToXmit.asStateFlow()

    private val _isTopBarInContextualMode = MutableStateFlow(false)
    val isTopBarInContextualMode = _isTopBarInContextualMode.asStateFlow()
    fun setIsTopBarInContextualMode(v: Boolean) { _isTopBarInContextualMode.value = v }
    private val _topBarClearButtonClicked = MutableStateFlow(false)
    val topBarClearButtonClicked = _topBarClearButtonClicked.asStateFlow()
    fun onTopBarClearButtonClicked() { _topBarClearButtonClicked.value = true }
    fun topBarClearButtonHandled() { _topBarClearButtonClicked.value = false }
    class TopBarTitleParams(val fmtResId: Int, val params: Array<out Any>)
    private val _topBarTitle = MutableStateFlow(TopBarTitleParams(R.string.app_name, emptyArray()))
    val topBarTitle = _topBarTitle.asStateFlow()
    fun setTopBarTitle(fmtResId: Int, vararg args: Any) {
        _topBarTitle.value = TopBarTitleParams(fmtResId, args)
    }

    // A bound communication service. Holding this reference is ok since anyway the usbCommService outlives this ViewModel
    @SuppressLint("StaticFieldLeak")
    private var usbCommService: UsbCommService? = null

    // Observer where IOPacketsList notifies us about events (e.g. new data received)
    private val ioPacketsListObserver = IOPacketsListObserver()
    private var nextByteToProcessInIOPacketsList = IOPacketsList.DataPointer(0, 0)
    private val nextByteToProcessInIOPacketsListMutex = Mutex()

    private var usbDeviceFromIntent: UsbDevice? = null

    val userInputHandler = UserInputHandler(settingsRepository, viewModelScope)

    /** Defines callbacks for service binding, passed to bindService()  */
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
            // Timber.d("serviceConnection.onServiceConnected() $componentName")
            usbCommService = (iBinder as UsbCommService.CommunicationServiceBinder).getService()
            userInputHandler.usbCommService = usbCommService

            usbCommService?.addObserver(communicationServiceObserver) // If already observed by this observer, this is NOP.
            usbCommService?.ioPacketsList?.addObserver(ioPacketsListObserver) // If already observed by this observer, this is NOP.

            usbDeviceFromIntent?.let {
                connectToUsbPort(it, portNumber = 0, deviceType = UsbSerialDevice.DeviceType.AUTO_DETECT)
            }
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            Timber.d("serviceConnection.onServiceDisconnected() $arg0") //todo:2brm
            userInputHandler.usbCommService = null
            usbCommService = null
            usbDeviceFromIntent = null
        }
    }

    /** Observer where the communication service notifies us about events (e.g. usb-connection-established) */
    private val communicationServiceObserver = Observer { o, arg ->
        val event = arg as UsbCommService.Event
        Timber.d("communicationServiceObserver: type=${event.eventType.name}  current thrd: ${Thread.currentThread()}") //todo:2brm
        when (event.eventType) {
            UsbCommService.Event.Type.CONNECTED -> onUsbConnected(event.obj as UsbCommService.Event.UsbConnectionParams)
            UsbCommService.Event.Type.DISCONNECTED -> onUsbDisconnected(event.obj as UsbCommService.Event.UsbDisconnectionParams?)
            UsbCommService.Event.Type.CONNECTION_ERROR -> onUsbConnectionError(event.obj as UsbSerialPort.ConnectResult)
            UsbCommService.Event.Type.NO_USB_PERMISSION -> requestUsbPermission(event.obj as UsbCommService.Event.UsbPermissionRequestParams)
            UsbCommService.Event.Type.SHOULD_UNBIND_SERVICE -> unbindCommunicationService()
            UsbCommService.Event.Type.UNRECOGNIZED_DEVICE_TYPE -> Timber.e("CommunicationServiceObserver : Observer - Got UsbCommService.Event.Type.UNRECOGNIZED_DEVICE_TYPE")
        }
    }

    /** A BroadcastReceiver to handle USB device attach/detach broadcast */
    private val usbAttachedOrDetachedBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            Timber.d("UsbAttachedOrDetachedBroadcastReceiver#onReceive(): action=$action") //todo:2brm

            // Reload the USB device list
            _portListState.value = getSerialPortList(getApplication())

            if (action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
                val attachedDevice = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                if (attachedDevice != null) {
                    Timber.d("UsbAttachedOrDetachedBroadcastReceiver#onReceive(): attached-device-Id=${attachedDevice.deviceId}")
                    if (_usbConnectionState.value.statusCode != UsbSerialPort.ConnectStatusCode.CONNECTED) {
                        connectToUsbPort(attachedDevice, portNumber = 0, deviceType = UsbSerialDevice.DeviceType.AUTO_DETECT)
                    }
                } else {
                    Timber.e("UsbAttachedOrDetachedBroadcastReceiver#onReceive(): no USB device to attach")
                }
            } else if (action == UsbManager.ACTION_USB_DEVICE_DETACHED) {
                val detachedDevice = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                if (detachedDevice != null) {
                    Timber.d("UsbAttachedOrDetachedBroadcastReceiver#onReceive(): detached-device-Id=${detachedDevice.deviceId}")
                } else {
                    Timber.e("UsbAttachedOrDetachedBroadcastReceiver#onReceive(): no USB device to detach")
                }
                if (_usbConnectionState.value.connectedUsbPort?.usbSerialDevice?.usbDevice?.deviceId == detachedDevice?.deviceId) {
                    disconnectFromUsbPort()
                }
            }
        }
    }

    init {
        // Timber.d("init(): intent.action=${initialIntent.action}")

        // Initialize the USB device list with whatever ports are currently attached to our device
        _portListState.value = getSerialPortList(application)
        // _portListState.value = getDummySerialPortList()

        // Register a broadcast receiver to be notified of USB device attach/detach events
        val intentFilter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        application.registerReceiver(usbAttachedOrDetachedBroadcastReceiver, intentFilter)

        // If the activity was launched due to a USB device being connected to the device,
        // we'll get that USB device in the intent. In that case we'll try to connect to it
        // after the service starts and we bind to it
        if (initialIntent.action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
            usbDeviceFromIntent = initialIntent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
        }


        Intent(getApplication(), UsbCommService::class.java).also { intent ->
            // Start our communication service (we explicitly start before bind so it will stay alive when we unbind)
            application.startService(intent)

            // Bind to our communication service. As the service starts and stops,
            // calls are made to callbacks defined by serviceConnection
            val rc = application.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            // Timber.d("bindService() rc=$rc")
        }

        when(settingsData.displayType) {
            // At this point settingRepository might only be initialized with default values
            // so basically we always start at TEXT mode. This may change immediately to HEX
            // after the first read of settingsRepository
            SettingsRepository.DisplayType.HEX -> screenHexModel.onStart()
            else -> screenTextModel.onStart()
        }

        // We constantly monitor app's settings in order to react to changes
        viewModelScope.launch {
            settingsRepository.settingsStateFlow.collect { newSettingsData ->
                onSettingsUpdated(newSettingsData)
            }
        }
    }

    fun onMainActivityCreate() {
        // We get called here in the main activity's onCreate()
        _shouldMeasureScreenDimensions.value = uid
    }

    override fun onCleared() {
        super.onCleared()
        getApplication<Application>().unregisterReceiver(usbAttachedOrDetachedBroadcastReceiver)

        usbCommService?.ioPacketsList?.deleteObserver(ioPacketsListObserver)
        usbCommService?.deleteObserver(communicationServiceObserver)
        getApplication<Application>().unbindService(serviceConnection)
    }

    fun onActivityStart() {
        usbCommService?.becomeBackgroundService()
    }
    fun onActivityStop() {
        if (settingsData.workAlsoInBackground) {
            usbCommService?.becomeForegroundService()
        }
    }

    /**
     * Connect to a USB port. Connection attempt results are dispatched
     * by the service to the observer [communicationServiceObserver]
     */
    fun connectToUsbPort(usbDevice: UsbDevice, portNumber: Int, deviceType: UsbSerialDevice.DeviceType) {
        val serialParams = UsbSerialPort.SerialCommunicationParams(
            baudRate = settingsData.baudRate,
            dataBits = settingsData.dataBits,
            stopBits = settingsData.stopBits,
            parity   = settingsData.parity,
        )
        usbCommService?.connectToUsbPort(usbDevice, portNumber, deviceType,
            serialParams, settingsData.setDTRTrueOnConnect, settingsData.setRTSTrueOnConnect)
    }

    fun disconnectFromUsbPort() {
        usbCommService?.disconnectFromUsbPort(exception = null)
    }

    // Handle events triggered by IOPacketsList (new data received)
    inner class IOPacketsListObserver : Observer {
        override fun update(o: Observable?, arg: Any?) {
            Timber.d("IOPacketsListObserver.update() totalSize=${usbCommService?.ioPacketsList?.getTotalSize()} nextByteToProcessInIOPacketsList=(${nextByteToProcessInIOPacketsList.packetSerialNumber}, ${nextByteToProcessInIOPacketsList.offsetInPacket})")
            viewModelScope.launch(Dispatchers.Default) {
                nextByteToProcessInIOPacketsListMutex.withLock {
                    nextByteToProcessInIOPacketsList = usbCommService?.ioPacketsList?.processData(
                        startAt = nextByteToProcessInIOPacketsList,
                        processor = ::processIOBytesIntoDisplayData,
                    ) ?: nextByteToProcessInIOPacketsList
                }
            }
        }
    }

    private fun redrawScreen() {
        // Timber.d("redrawScreen()")
        viewModelScope.launch(Dispatchers.Default) {
            clearScreen(alsoEraseBufferedData = false)
            nextByteToProcessInIOPacketsListMutex.withLock {
                nextByteToProcessInIOPacketsList = usbCommService?.ioPacketsList?.processData(
                    startAt = IOPacketsList.DataPointer(0, 0),
                    processor = ::processIOBytesIntoDisplayData,
                ) ?: nextByteToProcessInIOPacketsList
            }
        }
    }

    /**
     * We need to know if the data we're about to process was already processed in
     * the past, so we can avoid re-sending status reports in response to DSR.
     * This may happen when we redraw the screen (e.g. when switching from HEX to TEXT or
     * on orientation change). Note that we don't have to ever reset this variable
     * (not even on clearScreen) because packetSerialNumbers generated by IOPacketsList
     * is ever-growing (it never resets back to 0)
     */
    private val highWatterMark = IOPacketsList.MutableDataPointer(0, 0)

    // Handle IOPacketsList's newly received bytes
    private fun processIOBytesIntoDisplayData(
        data: ByteArray,
        packetSerialNumber: Int,
        offset: Int,
        dataDirection: IOPacketsList.DataDirection,
        timeStamp: Long,
    ) {
        // Timber.d("processIOBytesIntoDisplayData(): offset=$offset  data.size=${data.size}  direction=${dataDirection.name}")
        if (settingsData.displayType == SettingsRepository.DisplayType.HEX) {
            screenHexModel.onNewData(data, offset, dataDirection, timeStamp)
        } else {
            if (highWatterMark.packetSerialNumber == packetSerialNumber) {
                if (offset >= highWatterMark.offsetInPacket) {
                    // Received data is beyond the highWatterMark
                    screenTextModel.onNewData(data, offset, dataDirection, false)
                    highWatterMark.offsetInPacket = data.size
                } else {
                    // Received data may be partially below and partially above the highWatterMark
                    val buf1 = data.copyOfRange(offset, highWatterMark.offsetInPacket)
                    screenTextModel.onNewData(buf1, 0, dataDirection, true)
                    if (data.size > highWatterMark.offsetInPacket) {
                        val buf2 = data.copyOfRange(highWatterMark.offsetInPacket, data.size)
                        screenTextModel.onNewData(buf2, 0, dataDirection, false)
                        highWatterMark.offsetInPacket = data.size
                    }
                }
            } else if (packetSerialNumber > highWatterMark.packetSerialNumber) {
                // Received data is beyond the highWatterMark
                screenTextModel.onNewData(data, offset, dataDirection, false)
                highWatterMark.packetSerialNumber = packetSerialNumber
                highWatterMark.offsetInPacket = data.size
            } else {
                // Received data is below the highWatterMark
                screenTextModel.onNewData(data, offset, dataDirection, true)
            }
        }
    }


    private fun onUsbConnected(connectionParams: UsbCommService.Event.UsbConnectionParams?) {
        if (connectionParams != null) {
            Timber.d("onUsbConnected(): vId=${connectionParams.usbSerialPort.usbSerialDevice.usbDevice.vendorId} pId=${connectionParams.usbSerialPort.usbSerialDevice.usbDevice.productId}")
            _usbConnectionState.value = UsbConnectionState(
                statusCode = UsbSerialPort.ConnectStatusCode.CONNECTED,
                connectedUsbPort = connectionParams.usbSerialPort,
            )

            // Reload the USB device list
            _portListState.value = getSerialPortList(getApplication())
        } else {
            Timber.e("onUsbConnected(): null arg!")
        }
    }

    private fun onUsbDisconnected(disconnectionParam: UsbCommService.Event.UsbDisconnectionParams?) {
        _usbConnectionState.value = UsbConnectionState(UsbSerialPort.ConnectStatusCode.IDLE)
        disconnectionParam?.exception?.let {
            Timber.d("onUsbDisconnected(): USB disconnect due to: ${it.message ?: it.toString()}")
            // todo: maybe show something to the user
        } ?: run {
            Timber.d("onUsbDisconnected(): Nominal disconnection")
        }

        // Reload the USB device list
        _portListState.value = getSerialPortList(getApplication())
    }

    private fun onUsbConnectionError(connectResult: UsbSerialPort.ConnectResult?) {
        if (connectResult != null) {
            Timber.d("onUsbConnectionError(): code=${connectResult.statusCode.name} msg=${connectResult.msg}")
            _usbConnectionState.value = UsbConnectionState(
                statusCode = connectResult.statusCode,
                msg = connectResult.msg
            )
        } else {
            Timber.e("onUsbConnectionError(): null arg!")
        }
    }

    private fun sendBuf(buf: ByteArray) {
        usbCommService?.sendUsbData(buf)
    }

    private fun requestUsbPermission(usbPermissionRequestParams: UsbCommService.Event.UsbPermissionRequestParams) {
        _shouldRequestUsbPermissionFlow.value = UsbPermissionRequester.PermissionRequestParams(
            shouldRequestPermission = true,
            shouldRequestEvenIfAlreadyDenied = false,
            usbDevice = usbPermissionRequestParams.usbDevice,
            portNumber = usbPermissionRequestParams.portNumber,
            usbDeviceType = usbPermissionRequestParams.deviceType,
            onPermissionDecision = ::onUsbPermissionUserDecision
        )
    }

    private fun onUsbPermissionUserDecision(
        permissionGranted: Boolean,
        usbDevice: UsbDevice?,
        portNumber: Int,
        deviceType: UsbSerialDevice.DeviceType
    ) {
        if (permissionGranted) {
            usbDevice?.let {
                // Now that we have permission, retry connecting to the USB device
                // Connection attempt results are dispatched to the callback
                // CommunicationServiceObserver.update()
                connectToUsbPort(it, portNumber, deviceType)
            } ?: Timber.e("onUsbPermissionUserDecision(): no usbDevice")
        } else {
            Timber.i("User refused to grant us USB access permission")
            _usbConnectionState.value = UsbConnectionState(UsbSerialPort.ConnectStatusCode.ERR_NO_PERMISSION)
            // todo: show rational, and give the user a way to change their mind
        }
    }

    private fun unbindCommunicationService() {
        // Unbind from our communication service
        getApplication<Application>().unbindService(serviceConnection)
    }

    // For when we're working at InputMode.WHOLE_LINE mode
    fun setTextToXmit(text: String) {
        _textToXmit.value = text
    }

    // Used when working in whole-line mode
    fun onXmitButtonClick() {
        // Timber.d("onXmitButtonClick(): text='${textToXmit.value}'")
        usbCommService?.sendUsbData(textToXmit.value.toByteArray(charset = Charsets.UTF_8)) //todo: replace with char-set-dependant map lookup
    }

    fun onToggleHexTxtButtonClick() {
        // Timber.d("onToggleHexTxt()")
        when (settingsData.displayType) {
            // Changing settings values triggers a call to our onSettingsUpdated() method,
            // which is in charge of handling them
            SettingsRepository.DisplayType.TEXT -> {
                settingsRepository.setDisplayType(SettingsRepository.DisplayType.HEX.value)
            }
            else -> {
                settingsRepository.setDisplayType(SettingsRepository.DisplayType.TEXT.value)
            }
        }
    }

    fun onToggleShowCtrlButtonsRowButtonClick() {
        // Timber.d("onShowCtrlButtonsRowButtonClick()")
        // Changing settings values triggers a call to our onSettingsUpdated() method,
        // which is in charge of handling them
        settingsRepository.setShowCtrlButtonsRow(! settingsData.showCtrlButtonsRow)
    }

    suspend fun clearScreen(alsoEraseBufferedData: Boolean = true) {
        if (alsoEraseBufferedData) usbCommService?.eraseBufferedData()
        screenTextModel.clear()
        screenHexModel.clear()
        nextByteToProcessInIOPacketsListMutex.withLock {
            nextByteToProcessInIOPacketsList = IOPacketsList.DataPointer(0, 0)
        } // synchronized
    }

    fun setDTR(value: Boolean) {
        usbCommService?.setDTR(value)
    }
    fun setRTS(value: Boolean) {
        usbCommService?.setRTS(value)
    }
    fun esp32BootloaderReset() {
        // Timber.d("esp32BootloaderReset()")
        viewModelScope.launch(Dispatchers.IO) {
            usbCommService?.apply {
                val oldDTR = getDTR()
                val oldRTS = getRTS()
                // DTR and RTS are active low
                setDTR(false)
                setRTS(false) // DTR=1 RTS=1  -->  EN=1  IO0=1
                delay(200)
                setRTS(true)  // DTR=1 RTS=0  -->  EN=0  IO0=1
                delay(400)
                setRTS(false) // DTR=1 RTS=1  -->  EN=1  IO0=1
                delay(200)
                setDTR(oldDTR)
                setRTS(oldRTS)
            }
        }
    }
    fun arduinoReset() {
        // Timber.d("arduinoReset()")
        viewModelScope.launch(Dispatchers.IO) {
            // Some Arduinos connect the DTR line to the reset pin (via a capacitor and
            // a pull-up resistor), and some use RTS. Here we just pulse both of them
            usbCommService?.apply {
                val oldDTR = getDTR()
                val oldRTS = getRTS()
                // DTR and RTS are active low
                setDTR(false)
                setRTS(false) // DTR=1 RTS=1
                delay(10)
                setRTS(true)
                setDTR(true)  // DTR=0 RTS=0
                delay(10)
                setDTR(oldDTR)
                setRTS(oldRTS)
            }
        }
    }

    private fun onSettingsUpdated(newSettingsData: SettingsRepository.SettingsData) {
        if (newSettingsData.isDefaultValues) {
            // First time we're called after app startup is with default values (before
            // settingsData was read from disk). We shouldn't handle this because it
            // will cause a screen flicker
            return
        }
        // Timber.d("onSettingsUpdated()")

        val oldSettingsData = settingsData
        settingsData = newSettingsData
        // Timber.d("onSettingsUpdated() showedV2WelcomeMsg=${settingsData.showedV2WelcomeMsg}")

        if (! settingsData.showedV2WelcomeMsg) {
            if (settingsData.showedEulaV1) {
                // It's first run after an upgrade from v1
                _shouldShowUpgradeFromV1Msg.value = true
            } else {
                // It's first run after new install
                _shouldShowWelcomeMsg.value = true
            }
        }

        if (newSettingsData.displayType != oldSettingsData.displayType) {
            // Timber.d("new displayType=${newSettingsData.displayType.name}")
            onDisplayTypeChanged()
        }

        if (newSettingsData.showCtrlButtonsRow != oldSettingsData.showCtrlButtonsRow) {
            _ctrlButtonsRowVisible.value = newSettingsData.showCtrlButtonsRow
        }

        if (newSettingsData.maxBytesToRetainForBackScroll != oldSettingsData.maxBytesToRetainForBackScroll) {
            screenTextModel.setMaxTotalSize(newSettingsData.maxBytesToRetainForBackScroll)
            screenHexModel.setMaxTotalSize(newSettingsData.maxBytesToRetainForBackScroll)
        }

        if (newSettingsData.fontSize != oldSettingsData.fontSize) {
            viewModelScope.launch {
                clearScreen(alsoEraseBufferedData = false)
                _shouldMeasureScreenDimensions.value = uid
            }
        }

        if (newSettingsData.soundOn != oldSettingsData.soundOn) {
            screenTextModel.soundOn = newSettingsData.soundOn
        }
    }

    private fun onDisplayTypeChanged() {
        // When displayType changes (HEX to TEXT or vice versa), redraw the whole
        // screen from scratch (from data stored at IOPacketsList)
        when (settingsData.displayType) {
            SettingsRepository.DisplayType.HEX -> {
                screenHexModel.onStart()
                screenTextModel.onStop()
            }
            else -> {
                screenTextModel.onStart()
                screenHexModel.onStop()
            }
        }
        redrawScreen()
    }

    fun setIsDarkTheme(isDarkTheme: Boolean) {
        // When the theme changes (Light to Dark or vice versa), we need to redraw the whole
        // screen from scratch (from data stored at IOPacketsList) because text colors have changed
        screenTextModel.setIsDarkTheme(isDarkTheme)
        screenHexModel.setIsDarkTheme(isDarkTheme)
        redrawScreen()
    }

    private fun terminateApp() {
        usbCommService?.stop()
        shouldTerminateApp.value = true  // Signal our MainActivity to finish
    }

    fun debug1() {
        usbCommService?.debug('1') ?: run { Timber.d("No usbCommService")}
    }
    fun debug2() {
        usbCommService?.debug('2') ?: run { Timber.d("No usbCommService")}
    }
    fun debugUpgradeMsg() { _shouldShowUpgradeFromV1Msg.value = true }
    fun debugWelcomeMsg() { _shouldShowWelcomeMsg.value = true }


    class Factory(
        private val application: Application,
        private val initialIntent: Intent
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return MainViewModel(application, initialIntent) as T
        }
    }
}
