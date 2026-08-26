/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.horologist.remotecompose.lottie

import androidx.compose.remote.creation.compose.capture.rememberRemoteDocument
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.ui.graphics.Color
import com.google.android.horologist.screenshots.rng.WearScreenshotTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LottieAnimationApiTest : WearScreenshotTest() {

  @Test
  fun testLottieAnimationFloatProgressJson() {
    val json =
      """
      {
        "v": "5.7.0",
        "fr": 30,
        "ip": 0,
        "op": 30,
        "w": 100,
        "h": 100,
        "layers": []
      }
      """
        .trimIndent()

    val slots = slotMap { set("accent", Color.Red) }

    composeRule.setContent {
      val doc = rememberRemoteDocument {
        // Overload with Float progress
        LottieAnimation(json = json, progress = 0.5f, modifier = RemoteModifier, slotMap = slots)
      }
      assertThat(doc).isNotNull()
    }
    composeRule.waitForIdle()
  }

  @Test
  fun testLottieAnimationFloatProgressRawRes() {
    composeRule.setContent {
      val doc = rememberRemoteDocument {
        // Overload with Float progress on rawRes
        LottieAnimation(
          rawRes = R.raw.geometry,
          progress = 0.75f,
          modifier = RemoteModifier,
          slotMap = slotMapOf("color1" to Color.Blue),
        )
      }
      assertThat(doc).isNotNull()
    }
    composeRule.waitForIdle()
  }
}
