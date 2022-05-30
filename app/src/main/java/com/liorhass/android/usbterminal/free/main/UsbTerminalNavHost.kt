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
package com.liorhass.android.usbterminal.free.main

import androidx.activity.OnBackPressedDispatcher
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.liorhass.android.usbterminal.free.screens.about.AboutScreen
import com.liorhass.android.usbterminal.free.screens.about.AboutScreenAttributes
import com.liorhass.android.usbterminal.free.screens.devicelist.DeviceListScreen
import com.liorhass.android.usbterminal.free.screens.devicelist.DeviceListScreenAttributes
import com.liorhass.android.usbterminal.free.screens.help.HelpScreen
import com.liorhass.android.usbterminal.free.screens.help.HelpScreenAttributes
import com.liorhass.android.usbterminal.free.screens.logfiles.LogFilesListScreen
import com.liorhass.android.usbterminal.free.screens.logfiles.LogFilesListScreenAttributes
import com.liorhass.android.usbterminal.free.screens.terminal.TerminalScreen
import com.liorhass.android.usbterminal.free.screens.terminal.TerminalScreenAttributes
import com.liorhass.android.usbterminal.free.settings.ui.SettingsScreen
import com.liorhass.android.usbterminal.free.settings.ui.SettingsScreenAttributes
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@Composable
fun UsbTerminalNavHost(navController: NavHostController,
                   viewModel: MainViewModel,
                   modifier: Modifier = Modifier,
                   onBackPressedDispatcher: OnBackPressedDispatcher)
{

    // Launch a coroutine in the current scope that listens on a flow for navigation instructions.
    // This flow is fed by UsbTerminalNavigator which is a singleton object. Since it is a
    // singleton it can be use anywhere in the app.
    // Inspired by: https://proandroiddev.com/jetpack-compose-navigation-architecture-with-viewmodels-1de467f19e1c
    LaunchedEffect("navigation") {
        UsbTerminalNavigator.navTargetsSharedFlow.onEach { navTarget ->
            when (navTarget) {
                UsbTerminalNavigator.NavTargetBack -> {
                    //                    navController.popBackStack()
                    navController.popBackStackOrBackPressAction(onBackPressedDispatcher)
                }
                else -> {
                // navTarget.argument == null -> {
                    navController.navigate(navTarget.route) {
                        if (navTarget.isTopInBackStack) {
                            // Navigating to screens that are "topInBackStack" pops the stack
                            // completely so they're always at the top
                            popUpTo(navController.graph.findStartDestination().id) {
                                inclusive = true
                            }
                        }
                    }
                // else -> {
                //     val route = navTarget.route.plus("?$ARGUMENT1_KEY=${navTarget.argument}")
                //                 navController.navigate(route)
                // }
                }
            }
        }.launchIn(this)
    }

    // We manually provide the LocalViewModelStoreOwner to all composable screens.
    // This is needed because we use one scaffold that holds the NavDrawer, TopBar
    // and content. Our TopBar is specific to each screen, and it needs to be able
    // to talk to the screen's ViewModel. If we would have used the "normal" way
    // of obtaining a ViewModel by using the @Composable viewModel() function
    // without providing a common storeOwner, two different ViewModels would have
    // been created - one for the TopBar which is owned by the scaffold's
    // ViewModelStoreOwner, and the other for the screen content, which is owned
    // by the screen's ViewModelStoreOwner.
    // The cost of this is that once a ViewModel is created, it stays in memory for
    // the life of the app, waisting resources.
    // Hopefully a better solution can be found or provided by Google.
    // See: https://stackoverflow.com/a/69002254/1071117
    val viewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    }

    NavHost(
        navController = navController,
        startDestination = TerminalScreenAttributes.route,
        modifier = modifier
    ) {
//        composable(
//            route = HCCNavigator.NavTarget.QuestionnaireScreen.route
//                .plus("?$ARGUMENT1_KEY={$ARGUMENT1_KEY}"),
//            arguments = listOf(navArgument(ARGUMENT1_KEY) {
//                type = NavType.IntType
////                nullable = true
//                defaultValue = 0
//            })) { backStackEntry ->
//            val indexOfItemToScrollTo = backStackEntry.arguments?.getInt(ARGUMENT1_KEY)
//            QuestionnaireScreen(viewModel, indexOfItemToScrollTo = indexOfItemToScrollTo)
//        }
        composable(TerminalScreenAttributes.route) {
            CompositionLocalProvider(LocalViewModelStoreOwner provides viewModelStoreOwner) {
                TerminalScreen(viewModel)
            }
        }
        composable(DeviceListScreenAttributes.route) {
            CompositionLocalProvider(LocalViewModelStoreOwner provides viewModelStoreOwner) {
                DeviceListScreen(viewModel)
            }
        }
        composable(LogFilesListScreenAttributes.route) {
            CompositionLocalProvider(LocalViewModelStoreOwner provides viewModelStoreOwner) {
                LogFilesListScreen(viewModel)
            }
        }
        composable(SettingsScreenAttributes.route) {
            CompositionLocalProvider(LocalViewModelStoreOwner provides viewModelStoreOwner) {
                SettingsScreen(viewModel)
            }
        }
        composable(HelpScreenAttributes.route) {
            CompositionLocalProvider(LocalViewModelStoreOwner provides viewModelStoreOwner) {
                HelpScreen(viewModel)
            }
        }
        composable(AboutScreenAttributes.route) {
            CompositionLocalProvider(LocalViewModelStoreOwner provides viewModelStoreOwner) {
                AboutScreen(viewModel)
            }
        }
    }
}

/**
 * Enables back press to close the app if the pop-stack is empty. In Compose, the NavController
 * doesn't close the app when the back-stack is empty and we call popBackStack(). This is a
 * workaround for this problem.
 */
fun NavController.popBackStackOrBackPressAction(onBackPressedDispatcher: OnBackPressedDispatcher) {
    if (previousBackStackEntry == null) {
        onBackPressedDispatcher.onBackPressed()
    } else {
        popBackStack()
    }
}

//// From: https://stackoverflow.com/a/68423182/1071117
//fun Context.getActivity(): AppCompatActivity? = when (this) {
//    is AppCompatActivity -> this
//    is ContextWrapper -> baseContext.getActivity()
//    else -> null
//}
