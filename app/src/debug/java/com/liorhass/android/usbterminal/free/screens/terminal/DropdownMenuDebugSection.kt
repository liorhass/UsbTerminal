package com.liorhass.android.usbterminal.free.screens.terminal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import com.liorhass.android.usbterminal.free.main.MainViewModel

@Suppress("unused")
@Composable
fun ColumnScope.DropdownMenuDebugSection(
    showOverflowMenu: MutableState<Boolean>,
    mainViewModel: MainViewModel,
) {
    DropdownMenuItem(onClick = {
        showOverflowMenu.value = false
    }
    ) {
        Text(
            text = "Debug 1",
            modifier = Modifier
                .clickable { mainViewModel.debug1(); showOverflowMenu.value = false }
        )
    }
    DropdownMenuItem(onClick = {
        showOverflowMenu.value = false
    }
    ) {
        Text(
            text = "Debug 2",
            modifier = Modifier
                .clickable { mainViewModel.debug2(); showOverflowMenu.value = false }
        )
    }
}