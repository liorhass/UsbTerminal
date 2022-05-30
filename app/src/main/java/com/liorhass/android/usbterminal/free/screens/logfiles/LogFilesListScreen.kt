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
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liorhass.android.usbterminal.free.R
import com.liorhass.android.usbterminal.free.main.MainViewModel
import com.liorhass.android.usbterminal.free.main.UsbTerminalScreenAttributes
import com.liorhass.android.usbterminal.free.ui.theme.UsbTerminalTheme
import com.liorhass.android.usbterminal.free.util.collectAsStateLifecycleAware
import com.liorhass.android.usbterminal.free.util.getActivity
import timber.log.Timber

object LogFilesListScreenAttributes : UsbTerminalScreenAttributes(
    isTopInBackStack = false,
    route = "LogFiles",
) {
    override fun getTopAppBarActions(
        mainViewModel: MainViewModel,
        isTopBarInContextualMode: Boolean): @Composable RowScope.() -> Unit =
        { LogFilesListTopAppBarActions(mainViewModel, isTopBarInContextualMode) } // Would have been nicer if we could simply write ::AlarmListTopAppBarActions, but reference to composable functions is currently not implemented
}

@Composable
fun LogFilesListTopAppBarActions(mainViewModel: MainViewModel, isTopBarInContextualMode: Boolean) {
    val logFilesListViewModel: LogFilesListViewModel = viewModel(
        factory = LogFilesListViewModel.Factory(
            application = LocalContext.current.applicationContext as Application,
            mainViewModel = mainViewModel,
        )
    )

    if (isTopBarInContextualMode) {
        // Look at https://android--code.blogspot.com/2021/03/jetpack-compose-how-to-use-topappbar.html
        IconButton(
            onClick = {
                logFilesListViewModel.onDeleteButtonClick()
            }
        ) {
            Icon(
                modifier = Modifier.padding(start = 0.dp, end = 0.dp),
                painter = painterResource(id = R.drawable.ic_baseline_delete_24),
                contentDescription = stringResource(R.string.delete),
                tint = UsbTerminalTheme.extendedColors.contextualAppBarOnBackground,
            )
        }
        IconButton(
            onClick = {
                logFilesListViewModel.onShareButtonClick()
            }
        ) {
            Icon(
                modifier = Modifier.padding(start = 0.dp, end = 0.dp),
                painter = painterResource(id = R.drawable.ic_baseline_share_24),
                contentDescription = stringResource(R.string.share),
                tint = UsbTerminalTheme.extendedColors.contextualAppBarOnBackground,
            )
        }
    } else {
        IconButton(
            onClick = {
                logFilesListViewModel.onRefreshButtonClick()
            }
        ) {
            Icon(
                modifier = Modifier.padding(start = 0.dp, end = 0.dp),
                painter = painterResource(id = R.drawable.ic_baseline_refresh_24),
                contentDescription = stringResource(R.string.refresh),
            )
        }
        IconButton(
            onClick = {
                logFilesListViewModel.onToggleSortOrderClick()
            }
        ) {
            Icon(
                modifier = Modifier.padding(start = 0.dp, end = 0.dp),
                painter = painterResource(id = R.drawable.ic_baseline_height_24),
                contentDescription = stringResource(R.string.toggle_sort_order),
            )
        }
    }
}


@Composable
fun LogFilesListScreen(
    mainViewModel: MainViewModel
) {
    val viewModel: LogFilesListViewModel = viewModel(
        factory = LogFilesListViewModel.Factory(
            application = LocalContext.current.applicationContext as Application,
            mainViewModel = mainViewModel,
        )
    )

    val fileNamesList by viewModel.filesList.collectAsStateLifecycleAware()
    val nSelected by viewModel.nSelected.collectAsStateLifecycleAware()
    val shouldDisplayDeleteConfirmationDialog = viewModel.shouldDisplayDeleteConfirmationDialog

    // A hack needed because Compose doesn't destroy our viewModel even when the app navigates back.
    // Since we want to monitor the file system with FileObserver, we need to know when this screen
    // is not visible any more in order to remove the observer
    DisposableEffect(true) {
        viewModel.onScreenVisible()
        onDispose {
            viewModel.onScreenHidden()
        }
    }

    if (fileNamesList.isEmpty()) {
        EmptyLogFilesListMessage()
    } else {
        LogFilesList(
            fileNamesList = fileNamesList,
            onItemClick = viewModel::onLogFilesListItemClick,
            shouldDisplayDeleteConfirmationDialog = shouldDisplayDeleteConfirmationDialog,
            nSelected = nSelected,
            onDeleteConfirmationDialogDismissed = viewModel::onDeleteConfirmationDialogDismissed,
            onDeleteConfirmed = viewModel::onDeleteConfirmed
        )
    }

    // When there are selections, a click on the system's back button should not
    // navigate back, but instead only clear all selections.
    BackHandler(
        enabled = nSelected > 0,
        onBack = {
            Timber.d("LogFilesListScreen#BackHandler.onBack called")
            viewModel.clearAllSelections()
        }
    )

    // We do this here and not in our ViewModel because sharing needs to be done
    // from within an Activity context.
    val context = LocalContext.current
    LaunchedEffect(context) {
        val activity = context.getActivity()
        viewModel.shouldShareFile.collect { fileShareInfo ->
            if (fileShareInfo.file != null) {
                if (activity != null) {
                    val emailAddresses = mainViewModel.settingsRepository.settingsStateFlow.value.emailAddressForSharing.split(",")
                        .toTypedArray()
                    // See: https://medium.com/androiddevelopers/sharing-content-between-android-apps-2e6db9d1368b  and  https://stackoverflow.com/a/52843942/1071117
                    // https://code.luasoftware.com/tutorials/android/android-share-intent-with-chooser/
                    val intent = ShareCompat.IntentBuilder(activity)
                        .setStream(fileShareInfo.uri)
                        .setType(fileShareInfo.mimeType)
                        .setSubject(context.getString(fileShareInfo.subjectLine))
                        .setEmailTo(emailAddresses)
                        .setChooserTitle(fileShareInfo.chooserTitle)
                        .intent
                        .setDataAndType(fileShareInfo.uri, fileShareInfo.mimeType)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                    val chooserIntent = Intent.createChooser(intent, context.getString(fileShareInfo.chooserTitle))
                    try {
                        startActivity(activity, chooserIntent, null)
                    } catch (e: Exception) {
                        Timber.e("Can't share URI:'${fileShareInfo.uri}'. Exception: ${e.message}")
                    }
                }

                viewModel.fileShareHandled()
            }
        }
    }
}

@Composable
private fun LogFilesList(
    fileNamesList: List<LogFilesListViewModel.LogFileListItemModel>,
    onItemClick: (itemId:Int) -> Unit,
    shouldDisplayDeleteConfirmationDialog: Boolean,
    nSelected: Int,
    onDeleteConfirmationDialogDismissed: () -> Unit,
    onDeleteConfirmed: () -> Unit,
) {
    // Remember the scroll position across compositions, and also used for scrolling the lazyColumn
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
    ) {
        items(count = fileNamesList.size) { itemIndex ->
            LogFileListItem(
                logFileItemModel = fileNamesList[itemIndex],
                itemIndex = itemIndex,
                onItemClick = {onItemClick(itemIndex)},
//                onItemSelected = {},
            )
        }
    }

    // A dialog asking to confirm "Delete" when the user clicks on the "delete" button
    if (shouldDisplayDeleteConfirmationDialog) {
        DeleteFilesConfirmationDialog(
            nSelected = nSelected,
            onDialogDismiss = onDeleteConfirmationDialogDismissed,
            onConfirm = onDeleteConfirmed
        )
    }
}

@Composable
private fun LogFileListItem(
    logFileItemModel: LogFilesListViewModel.LogFileListItemModel,
    itemIndex: Int,
    onItemClick: (itemIndex: Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick(itemIndex) }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = logFileItemModel.fileName,
                fontSize = 16.sp
            )
            Text(
                text = stringResource(R.string.size, logFileItemModel.fileSize),
                fontSize = 14.sp
            )
        }
        Checkbox(
            checked = logFileItemModel.isSelected,
            onCheckedChange = null, // The whole row is clickable
        )
    }
}

@Composable
fun DeleteFilesConfirmationDialog(
    nSelected: Int,
    onDialogDismiss: () -> Unit,
    onConfirm: () -> Unit)
{
    AlertDialog(
        onDismissRequest = {
            Timber.d("onDismissRequest")
            onDialogDismiss()
        },
        title = { Text(text= stringResource(R.string.delete_files)) },
        text = {
            Column {
                Text(text=stringResource(R.string.delete_files_warning, nSelected))
                Text(
                    text=stringResource(R.string.delete_files_are_you_sure),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top=10.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm()
                onDialogDismiss()
            }) {
                Text(stringResource(R.string.yes_all_caps))
            }
        },
        dismissButton = {
            TextButton(onClick = { onDialogDismiss() }) {
                Text(stringResource(R.string.cancel_all_caps))
            }
        }
    )
}


@Preview
@Composable
private fun LogFileListItemPreview() {
    Column {
        LogFileListItem(
            logFileItemModel = LogFilesListViewModel.LogFileListItemModel("UsbTerminal_20220224_0832.log", 777,false),
            itemIndex = 0,
            onItemClick = {},
//            onItemSelected = {},
        )
        LogFileListItem(
            logFileItemModel = LogFilesListViewModel.LogFileListItemModel("UsbTerminal_20220224_0832.log", 12345678, true),
            itemIndex = 0,
            onItemClick = {},
//            onItemSelected = {},
        )
    }
}


@Composable
private fun EmptyLogFilesListMessage() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.no_log_file),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.h6
        )
    }
}


