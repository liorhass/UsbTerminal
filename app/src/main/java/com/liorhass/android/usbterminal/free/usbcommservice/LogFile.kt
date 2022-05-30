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

import android.content.Context
import com.liorhass.android.usbterminal.free.R
import com.liorhass.android.usbterminal.free.settings.model.SettingsRepository
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success


class LogFile private constructor(
    val context: Context,
    val coroutineScope: CoroutineScope,
    val ioPacketsList: IOPacketsList,
) {
    private val outgoingMarkHead = "OUT{".toByteArray()
    private val outgoingMarkTail = "}".toByteArray()

    /**
     * Creating a LogFile takes a long time (creating a new file on disk, etc), so
     * we want this to be done in a coroutine on `Dispatchers.IO`.
     * However, Kotlin constructors can't be suspend functions - See
     * https://discuss.kotlinlang.org/t/suspend-constructors/13267,
     * so instead, we provide a Builder with a `getLogFileAsync()` method that
     * returns a `Deferred<LogFile>` and does the work in a coroutine it creates
     * by itself.
     *
     * The caller should call `await()` on the returned value to get the `LogFile`
     */
    object Builder {
        fun getLogFileAsync(
            context: Context,
            coroutineScope: CoroutineScope,
            ioPacketsList: IOPacketsList,
        ) : Deferred<Result<LogFile>> {
            return coroutineScope.async(Dispatchers.IO) {
                try {
                    val logFile = LogFile(context, coroutineScope, ioPacketsList)
                    success(logFile)
                } catch (e: Exception) {
                    failure(e)
                }
            }
        }
    }

    private var fileName: String = generateFileName() // Each log file has a name like this: UsbTerminal_20220223_103842.log
    private val logFilesDir = getLogFilesDir(context)
    init { if (logFilesDir == null) throw Exception("Cannot create log-files directory") }
    private var file: File = File(logFilesDir, fileName)
    private var bos: BufferedOutputStream = BufferedOutputStream(FileOutputStream(file, true))
    private var settings: SettingsRepository.SettingsData? = null
    fun updateSettings(s: SettingsRepository.SettingsData?) {settings = s}

    private var nextByteToProcessInIOPacketsList = ioPacketsList.getCurrentLocation()
    private val nextByteToProcessInIOPacketsListLock = Object()

    // Observer where IOPacketsList notifies us when new data is received
    private val ioPacketsListObserver = IOPacketsListObserver()

    init {
        try {
            val logStartMsg = context.getString(R.string.log_start_msg, currentDateStr)
            bos.write(logStartMsg.toByteArray(charset = Charsets.UTF_8))
            bos.flush()
            ioPacketsList.addObserver(ioPacketsListObserver) // If already observed by this observer, this is NOP.
        } catch (e: Exception) {
            bos.close()
            throw e
        }
    }

    fun close() {
        ioPacketsList.deleteObserver(ioPacketsListObserver)
        bos.close()
    }

    // Handle events triggered by IOPacketsList (new data received)
    inner class IOPacketsListObserver : Observer {
        override fun update(o: Observable?, arg: Any?) {
            // Ask the IOPacketsList to go over all new data starting at nextByteToProcess and process
            // it by calling us back at handleNewIOBytes() for every packet
            synchronized(nextByteToProcessInIOPacketsListLock) {
                coroutineScope.launch(context = Dispatchers.IO) {
//                    Timber.d("LogFile#IOPacketsListObserver currentLocation=(${currentPositionInIOPacketsList.packetSerialNumber}, ${currentPositionInIOPacketsList.offsetInPacket})")
                    nextByteToProcessInIOPacketsList = ioPacketsList.processData(
                            startAt = nextByteToProcessInIOPacketsList,
                            processor = ::handleNewIOBytes
                        )
                }
            }
        }
    }

    private var flushJob: Job? = null
    private fun handleNewIOBytes(
        data: ByteArray,
        packetSerialNumber: Int,
        offset: Int,
        dataDirection: IOPacketsList.DataDirection,
        timeStamp: Long  //todo: support time-stamps in log files (in settings)
    ) {
//        Timber.d("LogFile::handleNewIOBytes() byteArray.size=${data.size}  offset=$offset  direction=${dataDirection.name}")
        if (dataDirection == IOPacketsList.DataDirection.IN) {
            bos.write(data, offset, data.size - offset)
        } else if (settings?.alsoLogOutgoingData == true) {
            if (settings?.markLoggedOutgoingData == true) {
                bos.write(outgoingMarkHead)
                bos.write(data, offset, data.size - offset)
                bos.write(outgoingMarkTail)
            } else {
                bos.write(data, offset, data.size - offset)
            }
        }
        if (flushJob?.isActive != true) {
            flushJob = coroutineScope.launch(Dispatchers.IO) {
                delay(3000)
                bos.flush()
            }
        }
    }

    companion object {
        private const val DIRECTORY_NAME = "logs"
        private const val DATE_FORMAT = "yyyyMMdd_HHmmss"
        private val dateFormat = SimpleDateFormat(DATE_FORMAT, Locale.US)
        private val currentDateStr: String
            get() = dateFormat.format(Date(System.currentTimeMillis()))

        fun getLogFilesDir(context: Context): File? {
            val dir = File(context.filesDir, DIRECTORY_NAME)
//            val dir = File(context.getExternalFilesDir(null), DIRECTORY_NAME) // todo: let the user select in settings whether to store in internal or external storage
            if (! dir.isDirectory) {
                if (! dir.mkdirs()) {
                    if (! dir.mkdirs()) { // Saw one time the first mkdirs fail without any reason. Doesn't reproduce so can't debug this. Sigh
                        Timber.e("Error in mkdirs() for '${dir.path}'")
                        return null
                    }
                }
            }
            return dir
        }

        fun generateFileName(): String {
            return "UsbTerminal_$currentDateStr.log" // Each log file has a name like this: UsbTerminal_20220223_103842.log
        }
    }

}