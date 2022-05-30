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
package com.liorhass.android.usbterminal.free.screens.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.liorhass.android.usbterminal.free.R
import com.liorhass.android.usbterminal.free.ui.util.ComposeWebView

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun WelcomeMsgDialog(
    onAgree: () -> Unit,
    onDecline: () -> Unit,
) {
    Dialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onDecline,
    ) {
        Card(
            elevation = 8.dp,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .width(LocalConfiguration.current.screenWidthDp.dp)
                .height(LocalConfiguration.current.screenHeightDp.dp)
                .background(Color.Transparent)
                .padding(4.dp),
        ) {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colors.primary)
                    .padding(8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                val mainPageUrl = "https://appassets.androidplatform.net/assets/welcome.html"
                ComposeWebView(url = mainPageUrl, onPageLoaded = null)

                // Agree and Decline buttons
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(
                        onClick = onDecline,
                    ) {
                        Text(
                            text = stringResource(R.string.decline_all_caps),
                            color = MaterialTheme.colors.onPrimary,
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = onAgree,
                    ) {
                        Text(
                            text = stringResource(R.string.agree_all_caps),
                            color = MaterialTheme.colors.onPrimary,
                        )
                    }
                }
            }
        }
    }
}

