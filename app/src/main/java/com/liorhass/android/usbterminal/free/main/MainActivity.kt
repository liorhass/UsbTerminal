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

// Update signing from APK to AAB with Play App Signing:
//    https://developer.android.com/studio/publish/app-signing#sign_release
//    https://stackoverflow.com/a/57195757/1071117
// Java (and keytool etc) is at "/Program Files/Android/Android Studio/jre/bin"


// todo: Support Android 11 (API 30): https://proandroiddev.com/the-quick-developers-guide-to-migrate-their-apps-to-android-11-e4ca2b011176
// todo: Support Android 12 (API 31): https://medium.com/tech-takeaways/migrating-my-app-to-android-12-7bafba3ed999
//
// todo: Support viewing of log files by implicit intent with FileProvider. See:
//       https://stackoverflow.com/questions/48725388/unable-to-open-trivial-text-file-with-fileprovider
//       https://developer.android.com/training/basics/intents/sending
//       https://developer.android.com/training/secure-file-sharing/setup-sharing#DefineMetaData
//
// todo: Color-picker in settings for screen background and text colors (per theme)

// Debug over WiFi (Android 10 and lower): https://developer.android.com/studio/command-line/adb#wireless
//     1. Connect the device to PC's USB
//     2. adb devices -l  (to verify that the device is recognized and get its transport-id)
//     3. adb [-t 2] tcpip 5555  ("-t 2" selects the device by it's transport-id if there are multiple devices )
//     4. Disconnect device from PC
//     5. adb connect 192.168.2.222  (find IP address by settings-->WiFi-->...-->Advanced)
// In case of troubles:
//     1. adb kill-server
//     2. adb start-server
//     3. adb connect 192.168.2.222  (find IP address by settings-->WiFi-->...-->Advanced)
//
// To install or upgrade the app on a device:
//     1. adb install -r app/debug/<apk-file-name>.apk
//        Does this should be used instead? adb shell pm install -r app/debug/<apk-file-name>.apk
//
// Debug over WiFi (Android 11+): https://developer.android.com/studio/command-line/adb#connect-to-a-device-over-wi-fi-android-11+
//

package com.liorhass.android.usbterminal.free.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.lifecycleScope
import com.liorhass.android.usbterminal.free.ui.theme.UsbTerminalTheme
import com.liorhass.android.usbterminal.free.ui.util.isDarkTheme
import com.liorhass.android.usbterminal.free.util.collectAsStateLifecycleAware
import timber.log.Timber


class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels { MainViewModel.Factory(application, intent) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Timber.d("onCreate()")
        if (!isTaskRoot) {
            // Android launched another instance of the root activity into an existing task
            //  so just quietly finish and go away, dropping the user back into the activity
            //  at the top of the stack (ie: the last state of this task)
            Timber.d("onCreate() +++++++ not root task ++++++")
        }

        // Our viewModel can trigger a shutdown
        lifecycleScope.launchWhenCreated {
            viewModel.shouldTerminateApp.collect {
                if (it)  finish()
            }
        }

        // Notify our ViewModel. The ViewModel needs this to know when to re-measure the screen dimensions (after orientation change)
        viewModel.onMainActivityCreate()

        // In order to be able to ask for USB permission
        UsbPermissionRequester.bindActivity(
            this,
            viewModel.shouldRequestUsbPermissionFlow,
            viewModel::usbPermissionWasRequested
        )

        setContent {
            val settingsData = viewModel.settingsRepository.settingsStateFlow.collectAsStateLifecycleAware()
            val isDarkTheme = isDarkTheme(settingsData.value)
            // This is ugly, but our VM needs to know about themes in order to select text colors.
            // Selecting colors is done in the VM because the VM generates annotatedStrings in
            // a pretty complex way. Finding the theme can't be done directly by the VM because
            // when the selected theme is "as system" we need to call isSystemInDarkTheme()
            // which is a Composable function
            viewModel.setIsDarkTheme(isDarkTheme)
            UsbTerminalTheme(isDarkTheme) {
                MainAppScreen(viewModel, onBackPressedDispatcher)
            }
            val density = LocalDensity.current
            WindowInsets.Companion.ime.getBottom(density)
        }
    }

    // We report these lifecycle events to the viewModel in order for it to know when
    // to make the service foreground or background
    override fun onStart() {
        super.onStart()
        // Timber.d("onStart()")
        viewModel.onActivityStart() // VM needs this in order to know when to make the service a background service
    }
    override fun onStop() {
        super.onStop()
        // Timber.d("onStop()")
        viewModel.onActivityStop() // VM needs this in order to know when to make the service a foreground service
    }

    override fun onDestroy() {
        super.onDestroy()
        // Timber.d("onDestroy()")
        UsbPermissionRequester.unbindActivity()
    }
}

