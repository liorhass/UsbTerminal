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

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.liorhass.android.usbterminal.free.util.collectAsStateLifecycleAware
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun MainAppScreen(viewModel: MainViewModel, onBackPressedDispatcher: OnBackPressedDispatcher) {
    val navHostController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()
    val scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Closed))
    val backstackEntry by navHostController.currentBackStackEntryAsState()
    val currentScreenAttributes = UsbTerminalScreenAttributes.fromRoute(backstackEntry?.destination?.route)
    val isTopBarInContextualMode by viewModel.isTopBarInContextualMode.collectAsStateLifecycleAware()
    val topBarTitleParams by viewModel.topBarTitle.collectAsStateLifecycleAware()
    val topBarTitle = stringResource(topBarTitleParams.fmtResId, *topBarTitleParams.params) // The '*' is a "spread operator"

    // Needed in order to:
    // - Navigate back from screens that are not top-in-back-stack
    // - Close the nav-drawer on system Back button (if it's opened)
    // - "Deselect all" on logFiles-list when it has some selections (i.e. it's topBar is in contextual-mode)
    SystemBackButtonHandler(
        enabled = !currentScreenAttributes.isTopInBackStack
                || scaffoldState.drawerState.isOpen
                || isTopBarInContextualMode,
        coroutineScope = coroutineScope,
        scaffoldState = scaffoldState,
        navHostController = navHostController,
        mainViewModel = viewModel,
    )

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = { UsbTerminalTopAppBar(
            navigationIcon = getTopAppBarNavigationIcon(currentScreenAttributes, isTopBarInContextualMode),
            onNavigationIconClick = { onTopAppBarNavigationIconClick(
                currentScreenAttributes = currentScreenAttributes,
                navHostController = navHostController,
                scaffoldState = scaffoldState,
                scope = coroutineScope,
                isTopBarInContextualMode = isTopBarInContextualMode,
                onClearButtonClicked = viewModel::onTopBarClearButtonClicked
            )},
            isInContextualMode = isTopBarInContextualMode,
            title = topBarTitle,
            actions = currentScreenAttributes.getTopAppBarActions(viewModel, isTopBarInContextualMode)
        )},
        floatingActionButton = currentScreenAttributes.getFab(viewModel),
        drawerContent = {
            UsbTerminalNavDrawer(coroutineScope = coroutineScope, scaffoldState = scaffoldState, navHostController = navHostController)
        },
    ) { contentPadding ->
        UsbTerminalNavHost(
            navController = navHostController,
            viewModel = viewModel,
            modifier = Modifier.padding(contentPadding),
            onBackPressedDispatcher = onBackPressedDispatcher
        )
    }
}

@Composable
fun SystemBackButtonHandler(
    enabled: Boolean,
    coroutineScope: CoroutineScope,
    scaffoldState: ScaffoldState,
    navHostController: NavHostController,
    mainViewModel: MainViewModel,
) {
    navHostController.enableOnBackPressed(false)
    BackHandler(
        enabled = enabled,
        onBack = {
            if (scaffoldState.drawerState.isOpen) {
                coroutineScope.launch { scaffoldState.drawerState.close() } // Close drawer
            } else {
                mainViewModel.setIsTopBarInContextualMode(false) // For when the user clicks the system's back button while in contextual-mode
                UsbTerminalNavigator.navigateBack()
            }
        }
    )
}

/**
 * TopBar navigation icon can be either hamburger (which should open the nav-drawer),
 * a Back arrow (which should go to the previous screen), or a Clear (X) icon when
 * the top-bar is in action-mode
 */
fun onTopAppBarNavigationIconClick(
    currentScreenAttributes: UsbTerminalScreenAttributes,
    navHostController: NavHostController,
    scaffoldState: ScaffoldState,
    scope: CoroutineScope,
    isTopBarInContextualMode: Boolean,
    onClearButtonClicked: () -> Unit
) {
    when {
        // When in contextual-mode, the navigation icon is a clear sign (X) which should end the contextual-mode
        isTopBarInContextualMode -> onClearButtonClicked()
        currentScreenAttributes.isTopInBackStack -> {
            // Screens that are top-in-backstack have a hamburger icon which should open the nav-drawer
            scope.launch {
                scaffoldState.drawerState.open()
            }
        }
        else -> navHostController.popBackStack() // Screens not top-in-backstack have a back icon
    }
}

fun getTopAppBarNavigationIcon(
    currentScreenAttributes: UsbTerminalScreenAttributes,
    isTopBarInContextualMode: Boolean,
): UT2TopAppBarNavigationIcon {
    return when {
        isTopBarInContextualMode -> UT2TopAppBarNavigationIcon.Clear
        currentScreenAttributes.isTopInBackStack -> UT2TopAppBarNavigationIcon.Menu
        else -> UT2TopAppBarNavigationIcon.Back
    }
}