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
package com.liorhass.android.usbterminal.free.ui.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.google.accompanist.systemuicontroller.rememberSystemUiController

private val DarkColorPalette = darkColors(
    primary = Teal900,
    primaryVariant = Teal900Dark,
    onPrimary = White,
    secondary = GummyDolphins,
    secondaryVariant = OceanBlue,
    onSecondary = Black,
    background = Black,
    onBackground = White,
)

private val LightColorPalette = lightColors(
    primary = Teal900,
    primaryVariant = Teal900Dark,
    onPrimary = White,
    secondary = GummyDolphins,
    secondaryVariant = OceanBlue,
    onSecondary = Black,
    background = White,
    onBackground = Black,

    /* Other default colors to override
    surface = Color.White,
    onSurface = Color.Black,
    */
)

// UsbTerminalTheme extends MaterialTheme to include proprietary colors. See: https://developer.android.com/jetpack/compose/themes/custom#extending-material
@Immutable
data class ExtendedColors(
    val contextualAppBarBackground: Color,
    val contextualAppBarOnBackground: Color,
    val ledColorWhenConnected: Color,
    val ledColorWhenDisconnected: Color,
    val textColorWhenConnected: Color,
    val textColorWhenDisconnected: Color,
    val statusLineDividerColor: Color,
    val textToXmitInputFieldBackgroundColor: Color,
    val textToXmitInputFieldBorderColor: Color,
)

private val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(
        contextualAppBarBackground = ContextualAppBarBackgroundColor,
        contextualAppBarOnBackground = ContextualAppBarOnBackgroundColor,
        ledColorWhenConnected = LedColorWhenConnected,
        ledColorWhenDisconnected = LedColorWhenDisconnected,
        textColorWhenConnected = TextColorWhenConnected,
        textColorWhenDisconnected = TextColorWhenDisconnected,
        statusLineDividerColor = Color.Transparent,
        textToXmitInputFieldBackgroundColor = Color.Transparent,
        textToXmitInputFieldBorderColor = Color.Transparent,
    )
}

object UsbTerminalTheme {
    val extendedColors: ExtendedColors
        @Composable get() = LocalExtendedColors.current
}

@Composable
fun UsbTerminalTheme(
    isDarkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val colors = if (isDarkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    val extendedColors = if (isDarkTheme) {
        ExtendedColors(
            contextualAppBarBackground = ContextualAppBarBackgroundColor,
            contextualAppBarOnBackground = ContextualAppBarOnBackgroundColor,
            ledColorWhenConnected = LedColorWhenConnected,
            ledColorWhenDisconnected = LedColorWhenDisconnected,
            textColorWhenConnected = TextColorWhenConnected,
            textColorWhenDisconnected = TextColorWhenDisconnected,
            statusLineDividerColor = Color.Gray,
            textToXmitInputFieldBackgroundColor = TextToXmitInputFieldBackgroundColorForDarkTheme,
            textToXmitInputFieldBorderColor = textToXmitInputFieldBorderColorForDarkTheme,
        )
    } else {
        ExtendedColors(
            contextualAppBarBackground = ContextualAppBarBackgroundColor,
            contextualAppBarOnBackground = ContextualAppBarOnBackgroundColor,
            ledColorWhenConnected = LedColorWhenConnected,
            ledColorWhenDisconnected = LedColorWhenDisconnected,
            textColorWhenConnected = TextColorWhenConnected,
            textColorWhenDisconnected = TextColorWhenDisconnected,
            statusLineDividerColor = Color.Transparent,
            textToXmitInputFieldBackgroundColor = TextToXmitInputFieldBackgroundColorForLightTheme,
            textToXmitInputFieldBorderColor = textToXmitInputFieldBorderColorForLightTheme,
        )
    }

    val systemUiController = rememberSystemUiController() // https://google.github.io/accompanist/systemuicontroller/
    if (isDarkTheme) {
        systemUiController.setStatusBarColor(  // setSystemBarsColor(
            color = colors.primary,
            darkIcons = true
        )
    } else {
        systemUiController.setStatusBarColor(  // setSystemBarsColor(
            color = colors.primary,
        )
    }

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colors = colors,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}