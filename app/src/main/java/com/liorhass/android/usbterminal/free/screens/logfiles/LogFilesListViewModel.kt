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
package com.liorhass.android.usbterminal.free.screens.logfiles

import android.app.Application
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.liorhass.android.usbterminal.free.BuildConfig
import com.liorhass.android.usbterminal.free.R
import com.liorhass.android.usbterminal.free.UsbTerminalApplication
import com.liorhass.android.usbterminal.free.main.MainViewModel
import com.liorhass.android.usbterminal.free.settings.model.SettingsRepository
import com.liorhass.android.usbterminal.free.usbcommservice.LogFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


class LogFilesListViewModel(
    application: Application,
    private val mainViewModel: MainViewModel,
) : AndroidViewModel(application) {

    data class LogFileListItemModel(
        val fileName: String,
        val fileSize: Long,
        val isSelected: Boolean = false
    )
    private val _filesList = MutableStateFlow<List<LogFileListItemModel>>(emptyList())
    val filesList = _filesList.asStateFlow()
    val nSelected = _filesList.transform { list ->
        emit(list.count{ it.isSelected })
    }.onEach { count ->
        if (count > 0) {
            mainViewModel.setIsTopBarInContextualMode(true)
            mainViewModel.setTopBarTitle(R.string.log_files_screen_top_appbar_cab_title, count)
        } else {
            mainViewModel.setIsTopBarInContextualMode(false)
            mainViewModel.setTopBarTitle(R.string.log_files_screen_top_appbar_normal_title)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000), initialValue = 0)

    var shouldDisplayDeleteConfirmationDialog by mutableStateOf(false)
        private set
    fun onDeleteConfirmationDialogDismissed() { shouldDisplayDeleteConfirmationDialog = false }
    fun onDeleteConfirmed() {
        deleteSelectedFiles()
        shouldDisplayDeleteConfirmationDialog = false
    }

    private val _listIsRefreshing = mutableStateOf(false)
    val listIsRefreshing: State<Boolean> = _listIsRefreshing

    private val _shouldViewFile = MutableStateFlow(Uri.EMPTY)
    val shouldViewFile = _shouldViewFile.asStateFlow()
    fun fileViewed() {
        _shouldViewFile.value = Uri.EMPTY
    }

    data class FileSharingInfo(
        val file: File? = null,
        val uri: Uri? = null,
        val mimeType: String? = null,
        @StringRes val subjectLine:  Int = -1,
        @StringRes val chooserTitle: Int = -1,
    )
    private val _shouldShareFile = MutableStateFlow(FileSharingInfo())
    val shouldShareFile = _shouldShareFile.asStateFlow()
    fun fileShareHandled() {
        _shouldShareFile.value = FileSharingInfo()
    }

//    private val _shouldDisplayDeleteConfirmationDialog = MutableStateFlow(false)
//    val shouldDisplayDeleteConfirmationDialog = _shouldDisplayDeleteConfirmationDialog.asStateFlow()
//    fun onDeleteConfirmationDialogDismissed() { _shouldDisplayDeleteConfirmationDialog.value = false }
//    fun onDeleteConfirmed() {
//        deleteSelectedFiles()
//        _shouldDisplayDeleteConfirmationDialog.value = false
//    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _filesList.value = getFilesListFromDisk()
        }

        // Listen on a StateFlow exposed by mainViewModel indicating a click on the clear
        // button (X) or a contextual-mode topBar. When it's clicked, clear all selections
        mainViewModel.topBarClearButtonClicked.onEach {
            if (it) {
                clearAllSelections()
                mainViewModel.topBarClearButtonHandled()
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = false)
    }

    inner class LogFileNameComparator(private val sortOrder: Int?) : Comparator<LogFileListItemModel> {
        override fun compare(o1: LogFileListItemModel, o2: LogFileListItemModel): Int {
            return if (sortOrder == SettingsRepository.LogFilesListSortingOrder.DESCENDING)
                o1.fileName.compareTo(o2.fileName)
            else
                o2.fileName.compareTo(o1.fileName)
        }
    }
    private fun getFilesListFromDisk(): List<LogFileListItemModel> {
        val dir = LogFile.getLogFilesDir(getApplication())
        return dir?.listFiles()
            ?.map { file -> LogFileListItemModel(file.name, file.length()) }
            ?.sortedWith(LogFileNameComparator(mainViewModel.settingsRepository.settingsStateFlow.value.logFilesListSortingOrder))
            ?: emptyList()
    }

    fun onToggleSortOrderClick() {
        Timber.d("onToggleSortOrderClick()")
        val currentSortOrder = mainViewModel.settingsRepository.settingsStateFlow.value.logFilesListSortingOrder
        val newSortOrder = if (currentSortOrder == SettingsRepository.LogFilesListSortingOrder.ASCENDING)
            SettingsRepository.LogFilesListSortingOrder.DESCENDING
        else
            SettingsRepository.LogFilesListSortingOrder.ASCENDING
        mainViewModel.settingsRepository.setLogFilesSortingOrder(newSortOrder)
        _filesList.value = _filesList.value.sortedWith(LogFileNameComparator(newSortOrder))
    }

    fun onPreviewFileButtonClick() {
        val selectedFile = _filesList.value.firstOrNull { it.isSelected }
        if (selectedFile == null) {
            Timber.e("onPreviewFileButtonClick(): selectedFile=null")
            return
        }
        // Timber.d("onPreviewFileButtonClick(): selected file name = ${selectedFile.fileName}")

        val logFilesDir = LogFile.getLogFilesDir(getApplication())
        if (logFilesDir == null) {
            Timber.e("onPreviewFileButtonClick(): logFilesDir=null")
            return
        }

        val file = File(logFilesDir, selectedFile.fileName)
        // Timber.d("absolutePath=${file.absolutePath}")
        val uri = FileProvider.getUriForFile(
            getApplication(),
            BuildConfig.APPLICATION_ID + ".fileprovider",
            file)

        // Timber.d("onPreviewFileButtonClick(): uri=$uri")
        _shouldViewFile.value = uri
    }

    fun onShareButtonClick() {
        viewModelScope.launch(Dispatchers.IO) {
            Timber.d("onShareButtonClick()")
            val zipFile = zipSelectedFiles() ?: run {
                Timber.e("Error when trying to zip log files")
                return@launch // TODO: show some error message to the user
            }

            // Timber.d("absolutePath=${zipFile.absolutePath}")
            val uri: Uri = FileProvider.getUriForFile(
                getApplication<UsbTerminalApplication>(),
                BuildConfig.APPLICATION_ID + ".fileprovider",
                zipFile)

            // Sharing is done in the Activity and not here because sharing needs to be done
            // from within an Activity context
            _shouldShareFile.value = FileSharingInfo(
                file = zipFile,
                uri = uri,
                mimeType = "application/zip",
                subjectLine = R.string.log_files_sharing_subject_line,
                chooserTitle = R.string.log_files_sharing_chooser_title
            )
        }
    }

    fun onRefreshRequested() {
        Timber.d("onRefreshButtonClick()")
        _listIsRefreshing.value = true
        _filesList.value = getFilesListFromDisk()
        _listIsRefreshing.value = false
    }

    fun onDeleteButtonClick() {
        Timber.d("onDeleteButtonClick()")
        shouldDisplayDeleteConfirmationDialog = true
    }

    fun onLogFilesListItemClick(itemIndex: Int) {
        // Timber.d("onLogFilesListItemClick(): itemIndex=$itemIndex")
        _filesList.value = _filesList.value
            .mapIndexed { index, item ->
                LogFileListItemModel(
                    fileName = item.fileName,
                    fileSize = item.fileSize,
                    isSelected = if (index == itemIndex) ! item.isSelected else item.isSelected
                )
            }
    }

    fun clearAllSelections() {
        _filesList.value = _filesList.value
            .map { item -> item.copy(isSelected = false) }
    }

    private fun deleteSelectedFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            val dir = LogFile.getLogFilesDir(getApplication()) ?: return@launch
            _filesList.value.filter { it.isSelected }.forEach { logFileListItemModel ->
                Timber.d("Deleting ${logFileListItemModel.fileName}")
                val file = File(dir, logFileListItemModel.fileName)
                if (! file.delete()) {
                    Timber.e("Can't delete file: '${file.name}'")
                }
            }
            _filesList.value = getFilesListFromDisk()
        }
    }

    /**
     * Zip all selected files in our file-list into an output file located in the app's
     * cache directory. Output-file's name is unique.
     * @return The zipped file's File instance
     */
    private fun zipSelectedFiles(): File? {
        val cacheDir = getApplication<UsbTerminalApplication>().cacheDir
        val outputFileName = LogFile.generateFileName() + ".zip"
        val outputFile = File(cacheDir, outputFileName)
        val zos = ZipOutputStream(BufferedOutputStream(FileOutputStream(outputFile)))
        val logFilesDir = LogFile.getLogFilesDir(getApplication()) ?: return null

        _filesList.value.filter { it.isSelected }.forEach { logFileListItemModel ->
            val fileName = logFileListItemModel.fileName
            // Timber.d("Zipping $fileName")
            zos.putNextEntry(ZipEntry(fileName))
            val bis = BufferedInputStream(FileInputStream(File(logFilesDir, fileName)))
            bis.copyTo(zos)
            bis.close()
        }
        zos.close()
        return outputFile
    }

    fun onScreenVisible() {
        // Timber.d("onScreenVisible()")
        _filesList.value = getFilesListFromDisk()
        // todo: start monitoring file system so logfile's size gets updated if new data arrives in the background
    }
    fun onScreenHidden() {
        // Timber.d("onScreenHidden()")
        // todo: stop monitoring file system
    }

    class Factory(
        private val application: Application,
        private val mainViewModel: MainViewModel,
        ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LogFilesListViewModel(application, mainViewModel) as T
        }
    }
}