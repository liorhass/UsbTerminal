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

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import com.liorhass.android.usbterminal.free.screens.about.AboutScreenAttributes
import com.liorhass.android.usbterminal.free.screens.devicelist.DeviceListScreenAttributes
import com.liorhass.android.usbterminal.free.screens.help.HelpScreenAttributes
import com.liorhass.android.usbterminal.free.screens.logfiles.LogFilesListScreenAttributes
import com.liorhass.android.usbterminal.free.screens.terminal.TerminalScreenAttributes
import com.liorhass.android.usbterminal.free.settings.ui.SettingsScreenAttributes


abstract class UsbTerminalScreenAttributes(
    override val isTopInBackStack: Boolean,
    override val route: String
) : UsbTerminalNavigator.NavTarget {
    /**
     * Implemented by individual screens to define their "actions" field of the TopAppBar.
     * This allows each screen to define the set of TopAppBar's icons and their actions
     */
    open fun getTopAppBarActions(
        mainViewModel: MainViewModel,
        isTopBarInContextualMode: Boolean
    ): @Composable RowScope.() -> Unit = {}

    /**
     * Implemented by individual screens to define their FAB field. This allows each screen
     * to define whether it has a FAB and its action
     */
    open fun getFab(viewModel: MainViewModel): @Composable () -> Unit = {}

    companion object {
        fun fromRoute(route: String?): UsbTerminalScreenAttributes =
            when (route?.substringBefore("/")) {
                TerminalScreenAttributes.route -> TerminalScreenAttributes
                DeviceListScreenAttributes.route -> DeviceListScreenAttributes
                LogFilesListScreenAttributes.route -> LogFilesListScreenAttributes
                SettingsScreenAttributes.route -> SettingsScreenAttributes
                HelpScreenAttributes.route -> HelpScreenAttributes
                AboutScreenAttributes.route -> AboutScreenAttributes
                null -> TerminalScreenAttributes
                else -> throw IllegalArgumentException("Route $route is not recognized.")
            }
    }
}
