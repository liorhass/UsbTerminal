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
package com.liorhass.android.usbterminal.free.settings.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.google.accompanist.pager.*
import com.liorhass.android.usbterminal.free.settings.model.SettingsRepository
import com.liorhass.android.usbterminal.free.main.MainViewModel
import com.liorhass.android.usbterminal.free.main.UsbTerminalScreenAttributes
import com.liorhass.android.usbterminal.free.util.collectAsStateLifecycleAware
import com.liorhass.android.usbterminal.free.R
import kotlinx.coroutines.launch

object SettingsScreenAttributes : UsbTerminalScreenAttributes(
    isTopInBackStack = false,
    route = "Settings",
) {
    override fun getTopAppBarActions(
        mainViewModel: MainViewModel,
        isTopBarInContextualMode: Boolean): @Composable RowScope.() -> Unit =
        { SettingsTopAppBarActions(mainViewModel) }
}

sealed class TabItem(
    @StringRes val title: Int,
    val screen: @Composable () -> Unit,
    val mainViewModel: MainViewModel,
    val settingsData: SettingsRepository.SettingsData,
) {
    class General(mainViewModel: MainViewModel, settingsData: SettingsRepository.SettingsData) :
        TabItem(R.string.general, { GeneralSettingsPage(mainViewModel, settingsData) }, mainViewModel, settingsData)
    class Terminal(mainViewModel: MainViewModel, settingsData: SettingsRepository.SettingsData) :
        TabItem(R.string.terminal, { TerminalSettingsPage(mainViewModel, settingsData) }, mainViewModel, settingsData)
    class Serial(mainViewModel: MainViewModel, settingsData: SettingsRepository.SettingsData) :
        TabItem(R.string.serial, { SerialSettingsPage(mainViewModel, settingsData) }, mainViewModel, settingsData)
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun SettingsScreen(mainViewModel: MainViewModel) {
    LaunchedEffect(true) { mainViewModel.setTopBarTitle(R.string.settings_screen_title) }
    val settingsData by mainViewModel.settingsRepository.settingsStateFlow.collectAsStateLifecycleAware()

    val tabs = listOf(
        TabItem.General(mainViewModel, settingsData),
        TabItem.Terminal(mainViewModel, settingsData),
        TabItem.Serial(mainViewModel, settingsData),
    )

    val pagerState = rememberPagerState()

    Column {
        Tabs(tabs = tabs, pagerState = pagerState)
        HorizontalPager(state = pagerState, count = tabs.size) { pageIndex ->
            tabs[pageIndex].screen()
        }
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalPagerApi::class)
@Composable
fun Tabs(tabs: List<TabItem>, pagerState: PagerState) {
    val scope = rememberCoroutineScope()
    // OR ScrollableTabRow()
    TabRow(
        selectedTabIndex = pagerState.currentPage,
        backgroundColor = MaterialTheme.colors.primaryVariant,
//        backgroundColor = colorResource(id = R.color.black),
        contentColor = MaterialTheme.colors.onPrimary,
        indicator = { tabPositions ->
            TabRowDefaults.Indicator(
                Modifier.pagerTabIndicatorOffset(pagerState, tabPositions)
            )
        }) {
        tabs.forEachIndexed { index, tab ->
            // OR Tab()
            Tab(
                // icon = { Icon(painter = painterResource(id = tab.icon), contentDescription = "") },
                text = { Text(stringResource(tab.title)) },
                selected = pagerState.currentPage == index,
                onClick = {
                    scope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                },
            )
        }
    }
}


@Composable
fun SettingsTopAppBarActions(@Suppress("UNUSED_PARAMETER") viewModel: MainViewModel) {
    // Look at https://android--code.blogspot.com/2021/03/jetpack-compose-how-to-use-topappbar.html
}

