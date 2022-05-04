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

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.liorhass.android.usbterminal.free.R
import com.liorhass.android.usbterminal.free.screens.about.AboutScreenAttributes
import com.liorhass.android.usbterminal.free.screens.devicelist.DeviceListScreenAttributes
import com.liorhass.android.usbterminal.free.screens.help.HelpScreenAttributes
import com.liorhass.android.usbterminal.free.screens.logfiles.LogFilesListScreenAttributes
import com.liorhass.android.usbterminal.free.settings.ui.SettingsScreenAttributes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

data class UsbTerminalNavDrawerItemDefinition(
    @DrawableRes var icon: Int,
    @StringRes var text: Int,
    var route: String,
    val isTopInBackStack: Boolean = false
)
object NavDrawerItems {
    val items = listOf(
        UsbTerminalNavDrawerItemDefinition(
            icon = R.drawable.ic_baseline_usb_24,
            text = R.string.device_list_screen_title,
            route = DeviceListScreenAttributes.route,
            isTopInBackStack = DeviceListScreenAttributes.isTopInBackStack),
        UsbTerminalNavDrawerItemDefinition(
            icon = R.drawable.ic_baseline_list_24,
            text = R.string.log_files_screen_top_appbar_normal_title,
            route = LogFilesListScreenAttributes.route,
            isTopInBackStack = DeviceListScreenAttributes.isTopInBackStack),
        UsbTerminalNavDrawerItemDefinition(
            icon = R.drawable.ic_baseline_settings_24,
            text = R.string.settings_screen_title,
            route = SettingsScreenAttributes.route,
            isTopInBackStack = SettingsScreenAttributes.isTopInBackStack),
        UsbTerminalNavDrawerItemDefinition(
            icon = R.drawable.ic_baseline_help_24,
            text = R.string.help_screen_title,
            route = HelpScreenAttributes.route,
            isTopInBackStack = HelpScreenAttributes.isTopInBackStack),
        UsbTerminalNavDrawerItemDefinition(
            icon = R.drawable.ic_baseline_info_24,
            text = R.string.about_screen_title,
            route = AboutScreenAttributes.route,
            isTopInBackStack = AboutScreenAttributes.isTopInBackStack),
    )
}

@Composable
fun UsbTerminalNavDrawer(coroutineScope: CoroutineScope, scaffoldState: ScaffoldState, navHostController: NavHostController) {
    Column {
        NavDrawerHeader()

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .background(MaterialTheme.colors.primary)
        ) {
            Spacer(modifier = Modifier.height(5.dp))
            // List of navigation items
            val navBackStackEntry by navHostController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            NavDrawerItems.items.forEach { item ->
                UsbTerminalNavDrawerItem(
                    navDrawerItemDefinition = item,
                    selected = currentRoute == item.route,
                    onItemClick = {
                    navHostController.navigate(item.route) {
                        // Pop up to the start destination of the graph to avoid building up a large
                        // stack of destinations on the Back stack as users select items
                        navHostController.graph.startDestinationRoute?.let { route ->
                            popUpTo(route) {
                                saveState = true
                                // Navigating to screens that are "topInBackStack" pops the stack
                                // completely so they're always at the top
                                if (item.isTopInBackStack) inclusive = true
                            }
                        }
                        // Avoid multiple copies of the same destination when
                        // re-selecting the same item
                        launchSingleTop = true
                        // Restore state when re-selecting a previously selected item
                        restoreState = true
                    }
                    // Close drawer
                    coroutineScope.launch {
                        scaffoldState.drawerState.close()
                    }
                })
            }
//            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Preview(showBackground = false)
@Composable
fun UsbTerminalNavDrawerPreview() {
    val scope = rememberCoroutineScope()
    val scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Closed))
    val navHostController = rememberNavController()
    UsbTerminalNavDrawer(coroutineScope = scope, scaffoldState = scaffoldState, navHostController = navHostController)
}

@Composable
fun NavDrawerHeader() {
    // Header
    Row(
        modifier = Modifier
            .height(140.dp)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colors.primary,
                        MaterialTheme.colors.primary.copy(alpha = 0.2f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(1000f, 1000f)
                )
            )
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
//            painter = painterResource(id = R.drawable.ic_launcher_round),
            painter = painterResource(id = R.drawable.ic_launcher_round),
            contentDescription = "",
            modifier = Modifier
                .height(120.dp)
                .width(120.dp)
                .padding(start = 20.dp),
        )
        Column(modifier = Modifier
            .padding()
            .align(Alignment.CenterVertically)
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.h6,
                textAlign = TextAlign.Start,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(start = 14.dp, top=14.dp)
                    .align(Alignment.Start)
            )
        }
    }
}

@Composable
fun UsbTerminalNavDrawerItem(
    navDrawerItemDefinition: UsbTerminalNavDrawerItemDefinition,
    selected: Boolean,
    onItemClick: (UsbTerminalNavDrawerItemDefinition) -> Unit
) {
//    val backgroundColor = if (selected) MaterialTheme.colors.onPrimary else MaterialTheme.colors.primary.copy(alpha = 0f)
    val borderColor = if (selected) MaterialTheme.colors.secondary else MaterialTheme.colors.primary.copy(alpha = 0f)
    val fontWeight =  if (selected) FontWeight.Bold else FontWeight.Normal
    Surface(color = MaterialTheme.colors.primary.copy(alpha = 0f)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = { onItemClick(navDrawerItemDefinition) })
                .height(45.dp)
                .padding(horizontal = 5.dp)
                .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(10.dp))
        ) {
            Spacer(modifier = Modifier.width(5.dp))
            Icon(
                painterResource(id = navDrawerItemDefinition.icon),
                contentDescription = stringResource(id = navDrawerItemDefinition.text),
                tint = MaterialTheme.colors.onPrimary
            )
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                text = stringResource(id = navDrawerItemDefinition.text),
                fontSize = 18.sp,
                fontWeight = fontWeight,
                color = MaterialTheme.colors.onPrimary
            )
        }
    }
}

@Preview(showBackground = false)
@Composable
fun UsbTerminalNavDrawerItemPreview() {
    UsbTerminalNavDrawerItem(
        navDrawerItemDefinition = UsbTerminalNavDrawerItemDefinition(
            R.drawable.ic_baseline_settings_24,
            R.string.settings_screen_title,
            SettingsScreenAttributes.route),
        selected = false,
        onItemClick = {}
    )
}

