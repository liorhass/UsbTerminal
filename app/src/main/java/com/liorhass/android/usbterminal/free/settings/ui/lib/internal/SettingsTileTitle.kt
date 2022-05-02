// MIT License
// Copyright (c) 2021 Bernat Borrás Paronella
// https://github.com/alorma/Compose-Settings
package com.liorhass.android.usbterminal.free.settings.ui.lib.internal

import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.runtime.Composable

@Composable
internal fun SettingsTileTitle(
    title: @Composable () -> Unit,
    enabled: Boolean = true,
) {
    val textStyle = if (enabled) {
        MaterialTheme.typography.subtitle1
    } else {
        MaterialTheme.typography.subtitle1.copy(
            color = MaterialTheme.colors.onBackground.copy(alpha = 0.5f)
//            color = MaterialTheme.typography.subtitle1.color.copy(alpha = 0.5f)
        )
    }
    ProvideTextStyle(value = textStyle) {
        title()
    }
}