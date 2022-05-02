// MIT License
// Copyright (c) 2021 Bernat Borrás Paronella
// https://github.com/alorma/Compose-Settings
package com.liorhass.android.usbterminal.free.settings.ui.lib.internal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun RowScope.SettingsTileTexts(
    title: @Composable () -> Unit,
    subtitle: @Composable (() -> Unit)?,
    enabled: Boolean = true,
) {
    Column(
        modifier = Modifier.Companion.weight(1f),
        verticalArrangement = Arrangement.Center,
    ) {
        SettingsTileTitle(title, enabled)
        if (subtitle != null) {
            Spacer(modifier = Modifier.size(2.dp))
            SettingsTileSubtitle(subtitle, enabled)
        }
    }
}