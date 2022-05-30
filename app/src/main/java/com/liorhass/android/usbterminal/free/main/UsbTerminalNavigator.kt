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

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow


/**
 * This is a singleton object. For this reason it can easily be used everywhere in the application.
 * It holds a hot flow that emits a NavTarget every time a component needs to navigate somewhere.
 * This flow is collected by the app's NavHost, which performs the navigation whenever a new
 * NavTarget is emitted.
 * Inspired by: https://proandroiddev.com/jetpack-compose-navigation-architecture-with-viewmodels-1de467f19e1c
 */
object UsbTerminalNavigator {

    @Suppress("ObjectPropertyName")
    private val _navTargetsSharedFlow = MutableSharedFlow<NavTarget>(extraBufferCapacity = 1)
    val navTargetsSharedFlow = _navTargetsSharedFlow.asSharedFlow()

    interface NavTarget {
        val route: String
        val isTopInBackStack: Boolean
    }
    object NavTargetBack : NavTarget {
        override val route = "Back"
        override val isTopInBackStack = false
    }

    @Suppress("unused")
    fun navigateTo(navTarget: NavTarget) {
        _navTargetsSharedFlow.tryEmit(navTarget)
    }

    fun navigateBack() {
        _navTargetsSharedFlow.tryEmit(NavTargetBack)
    }
}

