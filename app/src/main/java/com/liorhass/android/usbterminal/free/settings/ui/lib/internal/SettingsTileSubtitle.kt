// MIT License
// Copyright (c) 2021 Bernat Borrás Paronella
// https://github.com/alorma/Compose-Settings
package com.liorhass.android.usbterminal.free.settings.ui.lib.internal

import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
internal fun SettingsTileSubtitle(
    subtitle: @Composable () -> Unit,
    enabled: Boolean = true,
) {
    val textStyle = if (enabled) {
        MaterialTheme.typography.caption
    } else {
        MaterialTheme.typography.caption.copy(color = MaterialTheme.typography.subtitle1.color.copy(alpha = 0.5f))
    }
    ProvideTextStyle(value = textStyle) {
        CompositionLocalProvider(
            LocalContentAlpha provides ContentAlpha.medium,
            content = subtitle
        )
    }
}
