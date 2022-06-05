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
package com.liorhass.android.usbterminal.free.screens.about

import android.icu.text.SimpleDateFormat
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liorhass.android.usbterminal.free.BuildConfig
import com.liorhass.android.usbterminal.free.R
import com.liorhass.android.usbterminal.free.main.MainViewModel
import com.liorhass.android.usbterminal.free.main.UsbTerminalScreenAttributes
import com.liorhass.android.usbterminal.free.ui.util.LinkifyText
import java.util.*

object AboutScreenAttributes : UsbTerminalScreenAttributes(
    isTopInBackStack = false,
    route = "About",
)

@Composable
fun AboutScreen(
    mainViewModel: MainViewModel = viewModel()
) {
    LaunchedEffect(true) { mainViewModel.setTopBarTitle(R.string.about_screen_title) }
    val scrollState = rememberScrollState()

    val buildDate = Date(BuildConfig.TIMESTAMP) // From: https://stackoverflow.com/a/26372474/1071117
    val timeFormatter = SimpleDateFormat("yyyyMMdd", Locale.US)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.primary) // todo: replace with surface https://developer.android.com/jetpack/compose/themes#content-color
            .padding(start = 10.dp, end = 10.dp)
            .verticalScroll(scrollState)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_round),
                contentDescription = "",
                modifier = Modifier
                    .height(120.dp)
                    .width(120.dp)
                    .align(Alignment.TopCenter)
            )
        }

        Row(modifier = Modifier
            .padding(top = 20.dp, bottom = 10.dp),
        ) {
            Text(
                text = stringResource(id = R.string.app_name_in_about_screen),
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .alignByBaseline()
                    .padding(start = 6.dp),
                fontSize = 22.sp
            )
            Text(
                // BuildConfig.VERSION_NAME is from build.gradle(:app) android/defaultConfig/versionName
                text = "V${BuildConfig.VERSION_NAME}.${timeFormatter.format(buildDate)}",
                color = MaterialTheme.colors.onPrimary,
                modifier = Modifier
                    .alignByBaseline()
                    .padding(start = 12.dp),
                fontSize = 14.sp
            )
        }

        val aboutParagraphs = stringArrayResource(R.array.about_text_in_about_screen)
        aboutParagraphs.forEach {
            LinkifyText(
                text = it,
                color = MaterialTheme.colors.onPrimary,
                linkColor = Color(0xFF6688EE),
                modifier = Modifier
                    .padding(top = 8.dp),
                fontSize = 18.sp
            )
        }

        Text(
            text = stringResource(id = R.string.privacy),
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier
                .padding(top = 20.dp),
            fontSize = 22.sp
        )
        val privacyParagraphs = stringArrayResource(R.array.privacy_text_in_about_screen)
        privacyParagraphs.forEach {
            LinkifyText(
                text = it,
                color = MaterialTheme.colors.onPrimary,
                linkColor = Color(0xFF6688EE),
                modifier = Modifier
                    .padding(top = 8.dp),
                fontSize = 18.sp
            )
        }

        Text(
            text = stringResource(id = R.string.third_party),
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier
                .padding(top = 20.dp),
            fontSize = 22.sp
        )
        val thirdPartyParagraphs = stringArrayResource(R.array.third_party_text_in_about_screen)
        thirdPartyParagraphs.forEach {
            LinkifyText(
                text = it,
                color = MaterialTheme.colors.onPrimary,
                linkColor = Color(0xFF6688EE),
                modifier = Modifier
                    .padding(top = 8.dp),
                fontSize = 18.sp
            )
        }
        Spacer(modifier = Modifier.height(30.dp))
    }
}

