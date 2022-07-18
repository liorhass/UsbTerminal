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
package com.liorhass.android.usbterminal.free.settings.model

import android.annotation.SuppressLint
import android.content.Context
import androidx.datastore.migrations.SharedPreferencesMigration
import androidx.datastore.migrations.SharedPreferencesView
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.hoho.android.usbserial.driver.UsbSerialPort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.roundToInt

val Context.settingsDataStore by preferencesDataStore(
    name = SettingsRepository.SETTINGS_DATASTORE_NAME,
    produceMigrations = { context ->
        // The old PreferenceManager.getDefaultSharedPreferencesName() method returns
        // context.packageName + "_preferences"
        listOf(SharedPreferencesMigration(
            context = context,
            sharedPreferencesName = context.packageName + "_preferences",
            migrate = ::doMigration
        ))
    }
)



class SettingsRepository private constructor(private val context: Context) {

    data class SettingsData(
        // General
        val themeType: Int = DefaultValues.themeType,
        val logSessionDataToFile: Boolean = DefaultValues.logSessionDataToFile,
        val alsoLogOutgoingData: Boolean = DefaultValues.alsoLogOutgoingData,
        val markLoggedOutgoingData: Boolean = DefaultValues.markLoggedOutgoingData,
        val zipLogFilesWhenSharing: Boolean = DefaultValues.zipLogFilesWhenSharing,
        val connectToDeviceOnStart: Boolean = DefaultValues.connectToDeviceOnStart,
        val emailAddressForSharing: String = DefaultValues.emailAddressForSharing, // Comma-separated list of email addresses
        val workAlsoInBackground: Boolean = DefaultValues.workAlsoInBackground,
        val maxBytesToRetainForBackScroll: Int = DefaultValues.maxBytesToRetainForBackScroll,

        // Terminal
        val inputMode: Int = DefaultValues.inputMode, // CHAR_BY_CHAR or WHOLE_LINE
        val sendInputLineOnEnterKey: Boolean = DefaultValues.sendInputLineOnEnterKey,
        val bytesSentByEnterKey: Int = DefaultValues.bytesSentByEnterKey,
        val loopBack: Boolean = DefaultValues.loopBack,
        val fontSize: Int = DefaultValues.fontSize, // in sp
        val defaultTextColor: Int = DefaultValues.defaultTextColor,
        val defaultTextColorFreeInput: Int = DefaultValues.defaultTextColorFreeInput,
        val soundOn: Boolean = DefaultValues.soundOn,
        val silentlyDropUnrecognizedCtrlChars: Boolean = DefaultValues.silentlyDropUnrecognizedCtrlChars,

        // Serial comm
        val baudRate: Int = DefaultValues.baudRate,
        val baudRateFreeInput: Int = DefaultValues.baudRateFreeInput,
        val dataBits: Int = DefaultValues.dataBits,
        val stopBits: Int = DefaultValues.stopBits,
        val parity: Int = DefaultValues.parity,
        val setDTRTrueOnConnect: Boolean = DefaultValues.setDTRTrueOnConnect,
        val setRTSTrueOnConnect: Boolean = DefaultValues.setRTSTrueOnConnect,

        // Internal (not from settings screens)
        val isDefaultValues: Boolean = DefaultValues.isDefaultValues,
        val showedEulaV1: Boolean = DefaultValues.showedEulaV1,
        val showedV2WelcomeMsg: Boolean = DefaultValues.showedV2WelcomeMsg,
        val displayType: DisplayType = DefaultValues.displayType, // Text or Hex or Undefined
        val showCtrlButtonsRow: Boolean = DefaultValues.showCtrlButtonsRow,
        val logFilesListSortingOrder: Int = DefaultValues.logFilesListSortingOrder,
        val lastVisitedHelpUrl: String? = null
    )
    object DefaultValues {
        // General
        const val themeType = ThemeType.AS_SYSTEM
        const val logSessionDataToFile = false
        const val alsoLogOutgoingData = false
        const val markLoggedOutgoingData = true
        const val zipLogFilesWhenSharing = true
        const val connectToDeviceOnStart = true
        const val emailAddressForSharing= "" // Comma-separated list of email addresses
        const val workAlsoInBackground = true
        const val maxBytesToRetainForBackScroll = 100_000

        // Terminal
        const val inputMode = InputMode.CHAR_BY_CHAR // CHAR_BY_CHAR or WHOLE_LINE
        const val sendInputLineOnEnterKey: Boolean = true
        const val bytesSentByEnterKey = BytesSentByEnterKey.CR
        const val loopBack = false
        const val fontSize = 16
        const val defaultTextColor = 0xeeeeee
        const val defaultTextColorFreeInput = -1
        const val soundOn = true
        const val silentlyDropUnrecognizedCtrlChars = true

        // Serial comm
        const val baudRate = 115200
        const val baudRateFreeInput = -1
        const val dataBits = 8
        const val stopBits = UsbSerialPort.STOPBITS_1
        const val parity = UsbSerialPort.PARITY_NONE
        const val setDTRTrueOnConnect = true
        const val setRTSTrueOnConnect = true

        // Internal (not from settings screens)
        const val isDefaultValues = true // A hack to be able to know when values are only defaults, or actually from disk. This is always set to false in the mapping from Preferences to SettingsData
        const val showedEulaV1 = false // If this is true it means that we have upgraded from UsbTerminal-v1.x
        const val showedV2WelcomeMsg = false
        val displayType = DisplayType.TEXT // Text or Hex
        const val showCtrlButtonsRow = false
        const val logFilesListSortingOrder = LogFilesListSortingOrder.ASCENDING
    }
    object SettingsKeys {
        val THEME_TYPE_KEY = intPreferencesKey("themeType")
        val INPUT_MODE_KEY = intPreferencesKey("inputMode")
        val SEND_INPUT_LINE_ON_ENTER_KEY_KEY = booleanPreferencesKey("siloek")
        val BYTES_SENT_BY_ENTER_KEY_KEY = intPreferencesKey("bsbek")
        val LOOPBACK_KEY = booleanPreferencesKey("loopback")
        val SOUND_ON_KEY = booleanPreferencesKey("sound")
        val DROP_UNRECOGNIZED_CTRL_CHARS_KEY = booleanPreferencesKey("ducc")
        val FONT_SIZE_KEY = intPreferencesKey("fontSize")
        val DEFAULT_TEXT_COLOR_KEY = intPreferencesKey("defaultTextColorDialogParams")
        val DEFAULT_TEXT_COLOR_FREE_INPUT_KEY = intPreferencesKey("defaultTextColorFreeInput")
        val BAUD_RATE_KEY = intPreferencesKey("baudRate")
        val BAUD_RATE_FREE_INPUT_KEY = intPreferencesKey("baudRateFreeInput")
        val DATA_BITS_KEY = intPreferencesKey("dataBits")
        val STOP_BITS_KEY = intPreferencesKey("atopBits")
        val PARITY_KEY = intPreferencesKey("parity")
        val SET_DTR_KEY = booleanPreferencesKey("setDtr")
        val SET_RTS_KEY = booleanPreferencesKey("setRts")
        val LOG_SESSION_DATA_TO_FILE_KEY = booleanPreferencesKey("logToFile")
        val ALSO_LOG_OUTGOING_DATA_KEY = booleanPreferencesKey("logOutgoing")
        val MARK_LOGGED_OUTGOING_DATA_KEY = booleanPreferencesKey("markOutgoing")
        val ZIP_LOG_FILES_WHEN_SHARING_KEY = booleanPreferencesKey("zipShrdLogs")
        val CONNECT_TO_DEVICE_ON_START_KEY = booleanPreferencesKey("cos")
        val EMAIL_ADDR_FOR_SHARING_KEY = stringPreferencesKey("eafs")
        val WORK_ALSO_IN_BACKGROUND_KEY = booleanPreferencesKey("bg")
        val LOG_FILES_SORT_KEY = intPreferencesKey("logFilesSort")
        val SHOWED_EULA_V1_KEY = booleanPreferencesKey("seulav1")
        val SHOWED_V2_WELCOME_MSG_KEY = booleanPreferencesKey("sv2wm")
        val DISPLAY_TYPE_KEY = intPreferencesKey("dt")
        val SHOW_CTRL_BUTTON_KEY = booleanPreferencesKey("scb")
        val MAX_BYTES_TO_RETAIN_FOR_BACK_SCROLL_KEY = intPreferencesKey("bbs")
        val LAST_VISITED_HELP_URL_KEY = stringPreferencesKey("lvhu")
    }
    private fun mapPreferencesToModel(preferences: Preferences): SettingsData =
        SettingsData(
            // General
            themeType = preferences[SettingsKeys.THEME_TYPE_KEY] ?: DefaultValues.themeType,
            logSessionDataToFile = preferences[SettingsKeys.LOG_SESSION_DATA_TO_FILE_KEY] ?: DefaultValues.logSessionDataToFile,
            alsoLogOutgoingData = preferences[SettingsKeys.ALSO_LOG_OUTGOING_DATA_KEY] ?: DefaultValues.alsoLogOutgoingData,
            markLoggedOutgoingData = preferences[SettingsKeys.MARK_LOGGED_OUTGOING_DATA_KEY] ?: DefaultValues.markLoggedOutgoingData,
            zipLogFilesWhenSharing = preferences[SettingsKeys.ZIP_LOG_FILES_WHEN_SHARING_KEY] ?: DefaultValues.zipLogFilesWhenSharing,
            connectToDeviceOnStart = preferences[SettingsKeys.CONNECT_TO_DEVICE_ON_START_KEY] ?: DefaultValues.connectToDeviceOnStart,
            emailAddressForSharing = preferences[SettingsKeys.EMAIL_ADDR_FOR_SHARING_KEY] ?: DefaultValues.emailAddressForSharing,
            workAlsoInBackground = preferences[SettingsKeys.WORK_ALSO_IN_BACKGROUND_KEY] ?: DefaultValues.workAlsoInBackground,
            maxBytesToRetainForBackScroll = preferences[SettingsKeys.MAX_BYTES_TO_RETAIN_FOR_BACK_SCROLL_KEY] ?: DefaultValues.maxBytesToRetainForBackScroll,

            // Terminal
            inputMode = preferences[SettingsKeys.INPUT_MODE_KEY] ?: DefaultValues.inputMode,
            sendInputLineOnEnterKey = preferences[SettingsKeys.SEND_INPUT_LINE_ON_ENTER_KEY_KEY] ?: DefaultValues.sendInputLineOnEnterKey,
            bytesSentByEnterKey = preferences[SettingsKeys.BYTES_SENT_BY_ENTER_KEY_KEY] ?: DefaultValues.bytesSentByEnterKey,
            loopBack = preferences[SettingsKeys.LOOPBACK_KEY] ?: DefaultValues.loopBack,
            fontSize = preferences[SettingsKeys.FONT_SIZE_KEY] ?: DefaultValues.fontSize,
            defaultTextColor = preferences[SettingsKeys.DEFAULT_TEXT_COLOR_KEY] ?: DefaultValues.defaultTextColor,
            defaultTextColorFreeInput = preferences[SettingsKeys.DEFAULT_TEXT_COLOR_FREE_INPUT_KEY] ?: DefaultValues.defaultTextColorFreeInput,
            soundOn = preferences[SettingsKeys.SOUND_ON_KEY] ?: DefaultValues.soundOn,
            silentlyDropUnrecognizedCtrlChars = preferences[SettingsKeys.DROP_UNRECOGNIZED_CTRL_CHARS_KEY] ?: DefaultValues.silentlyDropUnrecognizedCtrlChars,

            // Serial communication
            baudRate  = preferences[SettingsKeys.BAUD_RATE_KEY] ?: DefaultValues.baudRate,
            baudRateFreeInput = preferences[SettingsKeys.BAUD_RATE_FREE_INPUT_KEY] ?: DefaultValues.baudRateFreeInput,
            dataBits  = preferences[SettingsKeys.DATA_BITS_KEY] ?: DefaultValues.dataBits,
            stopBits  = preferences[SettingsKeys.STOP_BITS_KEY] ?: DefaultValues.stopBits,
            parity    = preferences[SettingsKeys.PARITY_KEY] ?: DefaultValues.parity,
            setDTRTrueOnConnect = preferences[SettingsKeys.SET_DTR_KEY] ?: DefaultValues.setDTRTrueOnConnect,
            setRTSTrueOnConnect = preferences[SettingsKeys.SET_RTS_KEY] ?: DefaultValues.setRTSTrueOnConnect,

            // Internal
            isDefaultValues = false, // A hack. See above for explanation
            showedEulaV1 = preferences[SettingsKeys.SHOWED_EULA_V1_KEY] ?: DefaultValues.showedEulaV1,
            showedV2WelcomeMsg = preferences[SettingsKeys.SHOWED_V2_WELCOME_MSG_KEY] ?: DefaultValues.showedV2WelcomeMsg,
            displayType = DisplayType.fromInt(preferences[SettingsKeys.DISPLAY_TYPE_KEY]) ?: DefaultValues.displayType,
            showCtrlButtonsRow = preferences[SettingsKeys.SHOW_CTRL_BUTTON_KEY] ?: DefaultValues.showCtrlButtonsRow,
            logFilesListSortingOrder = preferences[SettingsKeys.LOG_FILES_SORT_KEY] ?: DefaultValues.logFilesListSortingOrder,
            lastVisitedHelpUrl = preferences[SettingsKeys.LAST_VISITED_HELP_URL_KEY],
        )

    object ThemeType {
        @Suppress("unused")
        const val LIGHT     = 0
        const val DARK      = 1
        const val AS_SYSTEM = 2
    }
    object InputMode {
        const val CHAR_BY_CHAR = 0
        const val WHOLE_LINE = 1
    }
    object BytesSentByEnterKey {
        const val CR    = 0
        const val LF    = 1
        const val CR_LF = 2
    }
    object BaudRateValues {
        val preDefined = arrayOf(300,600,1200,2400,4800,9600,19200,28800,38400,57600,115200)
        fun isPreDefined(baud: Int): Boolean = preDefined.contains(baud)
    }
    object DataBits {
        val values = arrayOf(8, 7, 6)
    }
    object StopBits {
        val values = arrayOf(
            UsbSerialPort.STOPBITS_1,
            UsbSerialPort.STOPBITS_1_5,
            UsbSerialPort.STOPBITS_2
        )
    }
    object Parity {
        val values = arrayOf(
            UsbSerialPort.PARITY_NONE,
            UsbSerialPort.PARITY_ODD,
            UsbSerialPort.PARITY_EVEN,
            UsbSerialPort.PARITY_MARK,
            UsbSerialPort.PARITY_SPACE
        )
    }
    object LogFilesListSortingOrder {
        const val ASCENDING = 0
        const val DESCENDING = 1
    }
    enum class DisplayType(val value: Int) {
        TEXT(0),
        HEX(1);
        companion object {
            fun fromInt(value: Int?) = values().firstOrNull { it.value == value }
        }
    }
    object FontSize {
        val values = arrayOf(8, 9, 10, 11, 12, 14, 16, 18, 20, 22, 24, 28)
    }
    object DefaultTextColorValues { // In coordination with strings.xml->text_colors
        val preDefined = arrayOf(
            0xeeeeee, // White
            0x66ff66, // Green
            0xffc600, // Amber
        )
        fun isPreDefined(color: Int): Boolean = preDefined.contains(color)
    }

    private val settingsFlow: Flow<Preferences> = context.settingsDataStore.data
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    /** Expose the settings data as a StateFlow. In composables this should be accessed like: val settingsData by settingsRepository.settingsFlow.collectAsStateLifecycleAware() */
    val settingsStateFlow: StateFlow<SettingsData> = settingsFlow
        .map { mapPreferencesToModel(it) }
        // .onEach { Timber.d("settingsFlow: showedV2WelcomeMsg=${it.showedV2WelcomeMsg} isDefaultValues=${it.isDefaultValues}") }
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.Eagerly, // Eagerly because in some places in the code it's convenient to access directly settingsStateFlow.value without collecting
            initialValue = SettingsData(),
        )

    fun setBaudRate(newBaudRateValue: String) {
        val newBaud = newBaudRateValue.toIntOrNull() ?: 0
        if (newBaud == 0) {
            Timber.w("setBaudRate() Illegal baud rate: '$newBaudRateValue'")
        } else {
            updateSettingsDataStore(SettingsKeys.BAUD_RATE_KEY, newBaud)
            if (! BaudRateValues.isPreDefined(newBaud)) {
                updateSettingsDataStore(SettingsKeys.BAUD_RATE_FREE_INPUT_KEY, newBaud)
            }
        }
    }

    fun setDataBits(newDataBitsValue: String) {
        val newDataBits = newDataBitsValue.toIntOrNull() ?: 0
        if (newDataBits == 0) {
            Timber.w("setDataBits() Illegal value: '$newDataBitsValue'")
        } else {
            updateSettingsDataStore(SettingsKeys.DATA_BITS_KEY, newDataBits)
        }
    }

    fun setStopBits(newStopBitsIndex: Int) {
        if (newStopBitsIndex < 0 || newStopBitsIndex >= StopBits.values.size) {
            Timber.w("setStopBits() Illegal index value: '$newStopBitsIndex'")
        } else {
            val newStopBitsValue = StopBits.values[newStopBitsIndex]
            updateSettingsDataStore(SettingsKeys.STOP_BITS_KEY, newStopBitsValue)
        }
    }

    fun setParity(newValue: Int) {
        if (! Parity.values.contains(newValue)) {
            Timber.w("setParity() Illegal value: '$newValue'")
        } else {
            updateSettingsDataStore(SettingsKeys.PARITY_KEY, newValue)
        }
    }

    fun setFontSize(newFontSizeValue: String) {
        val newFontSize = newFontSizeValue.toIntOrNull() ?: 0
        if (newFontSize == 0) {
            Timber.w("setFontSize() Illegal value: '$newFontSize'")
        } else {
            updateSettingsDataStore(SettingsKeys.FONT_SIZE_KEY, newFontSize)
        }
    }

    fun setDefaultTextColor(newTextColor: Int) {
        updateSettingsDataStore(SettingsKeys.DEFAULT_TEXT_COLOR_KEY, newTextColor)
        if (! DefaultTextColorValues.isPreDefined(newTextColor)) {
            updateSettingsDataStore(SettingsKeys.DEFAULT_TEXT_COLOR_FREE_INPUT_KEY, newTextColor)
        }
    }

    fun setThemeType(themeType: Int) { updateSettingsDataStore(SettingsKeys.THEME_TYPE_KEY, themeType) }
    fun setInputMode(inputMode: Int) { updateSettingsDataStore(SettingsKeys.INPUT_MODE_KEY, inputMode) }
    fun setBytesSentByEnterKey(bytesSentByEnterKey: Int) { updateSettingsDataStore(SettingsKeys.BYTES_SENT_BY_ENTER_KEY_KEY, bytesSentByEnterKey) }
    fun setLoopBack(newValue: Boolean) { updateSettingsDataStore(SettingsKeys.LOOPBACK_KEY, newValue) }
    fun setSoundOn(newValue: Boolean) { updateSettingsDataStore(SettingsKeys.SOUND_ON_KEY, newValue) }
    fun setSilentlyDropUnrecognizedCtrlChars(newValue: Boolean) { updateSettingsDataStore(SettingsKeys.DROP_UNRECOGNIZED_CTRL_CHARS_KEY, newValue) }
    fun setSetDTROnConnect(newValue: Boolean) { updateSettingsDataStore(SettingsKeys.SET_DTR_KEY, newValue) }
    fun setSetRTSOnConnect(newValue: Boolean) { updateSettingsDataStore(SettingsKeys.SET_RTS_KEY, newValue) }
    fun setLogSessionDataToFile(newValue: Boolean) { updateSettingsDataStore(SettingsKeys.LOG_SESSION_DATA_TO_FILE_KEY, newValue) }
    fun setAlsoLogOutgoingData(newValue: Boolean) { updateSettingsDataStore(SettingsKeys.ALSO_LOG_OUTGOING_DATA_KEY, newValue) }
    fun setMarkLoggedOutgoingData(newValue: Boolean) { updateSettingsDataStore(SettingsKeys.MARK_LOGGED_OUTGOING_DATA_KEY, newValue) }
    fun setZipLogFilesWhenSharing(newValue: Boolean) { updateSettingsDataStore(SettingsKeys.ZIP_LOG_FILES_WHEN_SHARING_KEY, newValue) }
    fun setConnectToDeviceOnStart(newValue: Boolean) { updateSettingsDataStore(SettingsKeys.CONNECT_TO_DEVICE_ON_START_KEY, newValue) }
    fun setEmailAddressForSharing(emailAddr: String) { updateSettingsDataStore(SettingsKeys.EMAIL_ADDR_FOR_SHARING_KEY, emailAddr) }
    fun setWorkAlsoInBackground(newValue: Boolean) { updateSettingsDataStore(SettingsKeys.WORK_ALSO_IN_BACKGROUND_KEY, newValue) }
    fun setLogFilesSortingOrder(sortingOrder: Int) { updateSettingsDataStore(SettingsKeys.LOG_FILES_SORT_KEY, sortingOrder) }
    fun setDisplayType(displayType: Int) { updateSettingsDataStore(SettingsKeys.DISPLAY_TYPE_KEY, displayType) }
    fun setShowCtrlButtonsRow(show: Boolean) { updateSettingsDataStore(SettingsKeys.SHOW_CTRL_BUTTON_KEY, show) }
    fun setMaxBytesToRetainForBackScroll(nMaxBytes: Int) { updateSettingsDataStore(SettingsKeys.MAX_BYTES_TO_RETAIN_FOR_BACK_SCROLL_KEY, nMaxBytes) }
    fun setShowedV2WelcomeMsg(showedMsg: Boolean) { updateSettingsDataStore(SettingsKeys.SHOWED_V2_WELCOME_MSG_KEY, showedMsg) }
    fun setSendInputLineOnEnterKey(send: Boolean) { updateSettingsDataStore(SettingsKeys.SEND_INPUT_LINE_ON_ENTER_KEY_KEY, send) }
    fun setLastVisitedHelpUrl(url: String) { updateSettingsDataStore(SettingsKeys.LAST_VISITED_HELP_URL_KEY, url) }


    private fun<T> updateSettingsDataStore(key: Preferences.Key<T>, value: T) {
        coroutineScope.launch {
            context.settingsDataStore.edit { settings ->
                // Timber.d("updateSettingsDataStore(): key=${key.name}")
                settings[key] = value
            }
        }
    }

    /**
     * Find the index of a specified text-color in the predefined-text-colors-list
     *
     * @return: If text-color is one of the predefined values, return its index. Otherwise
     * return the size of the predefine text colors list (as if the index is one place
     * beyond the end of that list).
     */
    fun indexOfTextColor(color: Int): Int {
        var index = DefaultTextColorValues.preDefined.indexOfFirst { it == color }
        if (index == -1) {
            index = DefaultTextColorValues.preDefined.size
        }
        return index
    }

    /**
     * Find the index of a specified baud-rate in the predefined-baud-rates-list
     *
     * @return: If baud is one of the predefined values, return its index. Otherwise
     * if baud is equal to the free-input-baud, return the size of the predefine
     * baud rates list (as if the index is one place beyond the end of that list).
     * Otherwise return -1.
     */
    fun indexOfBaudRate(baud: Int): Int {
        var index = BaudRateValues.preDefined.indexOfFirst { it == baud }
        if (index == -1) {
            val freeInputBaudRate = settingsStateFlow.value.baudRateFreeInput
            if (freeInputBaudRate == baud) {
                index = BaudRateValues.preDefined.size
            }
        }
        return index
    }


    // Singleton factory
    companion object {
        @SuppressLint("StaticFieldLeak") // Lint (rightfully so) complains that we leak a context. This is OK since what we hold is the applicationContext
        @Volatile
        private var INSTANCE: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE?.let {
                    return it
                }

                val instance = SettingsRepository(context)
                INSTANCE = instance
                instance
            }
        }

        @Suppress("unused")
        fun getInstance(): SettingsRepository {
            return INSTANCE ?: throw (IllegalStateException("The first call to SettingsRepository.getInstance() must pass a context object"))
        }

        const val SETTINGS_DATASTORE_NAME = "settings"

        /**
         * Find the index of a specified font-size in the predefined-font-size-list
         *
         * @return: Index of specified font-size, or -1 if the specified font-size
         * number is not in the list of predefined values
         */
        fun indexOfFontSize(fontSize: Int): Int = FontSize.values.indexOfFirst { it == fontSize }

        /**
         * Find the index of a specified parity in the predefined-parity-list
         *
         * @return: Index of specified parity, or -1 if the specified parity
         * is not in the list of predefined values
         */
        fun indexOfParity(parity: Int): Int = Parity.values.indexOfFirst { it == parity }

        /**
         * Find the index of a specified stop-bits in the predefined-stop-bits-list
         *
         * @return: Index of specified stop-bits, or -1 if the specified stop-bits
         * number is not in the list of predefined values
         */
        fun indexOfStopBits(stopBits: Int): Int = StopBits.values.indexOfFirst { it == stopBits }

        /**
         * Find the index of a specified data-bits in the predefined-data-bits-list
         *
         * @return: Index of specified data-bits, or -1 if the specified data-bits
         * number is not in the list of predefined values
         */
        fun indexOfDataBits(dataBits: Int): Int = DataBits.values.indexOfFirst { it == dataBits }
    }
}

// Migrate from our old SharedPreferences:
//     SETTINGS_NAME from getDefaultSharedPreferences(): "com.liorhass.android.usbterminal.free.preferences"
//   - boolean showed_eula_1
//   - string  pref_baudrate (used with Integer.parseInt())
//   - string  pref_data_bits (used with Integer.parseInt() 5,6,7,8)
//   - string  pref_parity ("none" "even" "odd" "mark" "space")
//   - string  pref_stop_bits ("1" "1.5" "2")
//   - string  pref_enter_key_sends ("CR" "LF" "CR_LF")
//   - string  pref_received_lines_end_with ("CR" "LF")
//   - string  pref_text_size (used with Float.parseFloat() default "16")
//   - boolean pref_use_dedicated_field_for_kb_input (default false)
//   - boolean pref_send_when_enter_key_pressed (default true)
//   - boolean pref_wrap_complete_words (default false)
//   - boolean pref_log_session (default true)
//   - boolean pref_local_echo (default false)
//   - string  pref_scrollback_buffer_size (used with Integer.parseInt() default 10)
/** Migrate from app's v1's SharedPreferences to v2's DataStore */
private fun doMigration(sharedPrefs: SharedPreferencesView, currentData: Preferences): Preferences {
    // prefs.getAll is already filtered to our key set, but we don't want to overwrite
    // elements that already exist in currentData
    val currentKeys = currentData.asMap().keys.map { it.name }

    // All elements in sharedPrefs that don't already exist in currentData
    val filteredSharedPreferences =
        sharedPrefs.getAll().filter { (key, _) -> key !in currentKeys }

    val mutablePreferences = currentData.toMutablePreferences()
    for ((key, value) in filteredSharedPreferences) {
        if (key == "showed_eula_1") {
            mutablePreferences[SettingsRepository.SettingsKeys.SHOWED_EULA_V1_KEY] = value as Boolean
        } else if (key == "pref_baudrate") {
            val baudRate = (value as String).toIntOrNull() ?: 0
            if (baudRate != 0) {
                mutablePreferences[SettingsRepository.SettingsKeys.BAUD_RATE_KEY] = baudRate
                if (!SettingsRepository.BaudRateValues.isPreDefined(baudRate)) {
                    mutablePreferences[SettingsRepository.SettingsKeys.BAUD_RATE_FREE_INPUT_KEY] = baudRate
                }
            }
        } else if (key == "pref_data_bits") {
            val dataBits = (value as String).toIntOrNull() ?: 0
            if (dataBits != 0) {
                mutablePreferences[SettingsRepository.SettingsKeys.DATA_BITS_KEY] = dataBits
            }
        } else if (key == "pref_parity") {
            when (value as String) {
                "none" -> mutablePreferences[SettingsRepository.SettingsKeys.PARITY_KEY] = UsbSerialPort.PARITY_NONE
                "even" -> mutablePreferences[SettingsRepository.SettingsKeys.PARITY_KEY] = UsbSerialPort.PARITY_EVEN
                "odd"  -> mutablePreferences[SettingsRepository.SettingsKeys.PARITY_KEY] = UsbSerialPort.PARITY_ODD
                "mark" -> mutablePreferences[SettingsRepository.SettingsKeys.PARITY_KEY] = UsbSerialPort.PARITY_MARK
                "space"-> mutablePreferences[SettingsRepository.SettingsKeys.PARITY_KEY] = UsbSerialPort.PARITY_SPACE
            }
        } else if (key == "pref_stop_bits") {
            when (value as String) {
                "1" -> mutablePreferences[SettingsRepository.SettingsKeys.STOP_BITS_KEY] = UsbSerialPort.STOPBITS_1
                "1.5" -> mutablePreferences[SettingsRepository.SettingsKeys.STOP_BITS_KEY] = UsbSerialPort.STOPBITS_1_5
                "2" -> mutablePreferences[SettingsRepository.SettingsKeys.STOP_BITS_KEY] = UsbSerialPort.STOPBITS_2
            }
        } else if (key == "pref_enter_key_sends") {
            when (value as String) {
                "CR" -> mutablePreferences[SettingsRepository.SettingsKeys.BYTES_SENT_BY_ENTER_KEY_KEY] = SettingsRepository.BytesSentByEnterKey.CR
                "LF" -> mutablePreferences[SettingsRepository.SettingsKeys.BYTES_SENT_BY_ENTER_KEY_KEY] = SettingsRepository.BytesSentByEnterKey.LF
                "CR_CR" -> mutablePreferences[SettingsRepository.SettingsKeys.BYTES_SENT_BY_ENTER_KEY_KEY] = SettingsRepository.BytesSentByEnterKey.CR_LF
            }
        } else if (key == "pref_text_size") {
            val fontSizeFloat = (value as String).toFloatOrNull() ?: 0f
            val fontSizeInt = fontSizeFloat.roundToInt()
            if (fontSizeFloat > 0.1f  &&  SettingsRepository.indexOfFontSize(fontSizeInt) > 0) {
                mutablePreferences[SettingsRepository.SettingsKeys.FONT_SIZE_KEY] = fontSizeInt
            }
        } else if (key == "pref_use_dedicated_field_for_kb_input") {
            if (value as Boolean) {
                mutablePreferences[SettingsRepository.SettingsKeys.INPUT_MODE_KEY] = SettingsRepository.InputMode.WHOLE_LINE
            } else {
                mutablePreferences[SettingsRepository.SettingsKeys.INPUT_MODE_KEY] = SettingsRepository.InputMode.CHAR_BY_CHAR
            }
        } else if (key == "pref_send_when_enter_key_pressed") {
            mutablePreferences[SettingsRepository.SettingsKeys.SEND_INPUT_LINE_ON_ENTER_KEY_KEY] = value as Boolean
        } else if (key == "pref_log_session") {
            mutablePreferences[SettingsRepository.SettingsKeys.LOG_SESSION_DATA_TO_FILE_KEY] = value as Boolean
        } else if (key == "pref_local_echo") {
            mutablePreferences[SettingsRepository.SettingsKeys.LOOPBACK_KEY] = value as Boolean
        } else if (key == "pref_scrollback_buffer_size") {
            val scrollBackBufferSize = (value as String).toIntOrNull() ?: 0
            if (scrollBackBufferSize != 0) {
                mutablePreferences[SettingsRepository.SettingsKeys.MAX_BYTES_TO_RETAIN_FOR_BACK_SCROLL_KEY] = 1000 * scrollBackBufferSize
            }
        }
    }

    return mutablePreferences.toPreferences()
}

