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

package com.google.android.horologist.remotecompose.lottie.renderer.layers

import android.annotation.SuppressLint
import androidx.compose.remote.creation.compose.state.rf
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.horologist.remotecompose.lottie.LottieSettings
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.grouping.Transform
import com.google.android.horologist.remotecompose.lottie.format.mask.Mask
import com.google.android.horologist.remotecompose.lottie.format.mask.MaskMode
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticBezierProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticScalarProperty
import com.google.android.horologist.remotecompose.lottie.format.values.BezierValue
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SuppressLint("RestrictedApi")
@RunWith(AndroidJUnit4::class)
class LayerShellTest {

  @Test
  fun computeLayerOpacity_withNoTransform_returnsLayerVisibility() {
    val settings = LottieSettings(currentFrame = 0f.rf)
    val opacity =
      computeLayerOpacity(
        transformStack = emptyList(),
        layerVisibility = 0.8f.rf,
        animationSettings = settings,
      )
    assertThat(opacity.constantValueOrNull).isWithin(1e-4f).of(0.8f)
  }

  @Test
  fun computeLayerOpacity_withTransformOpacity_compoundsCorrectly() {
    val settings = LottieSettings(currentFrame = 0f.rf)
    val transform = Transform(opacity = StaticScalarProperty(value = 50f))
    val opacity =
      computeLayerOpacity(
        transformStack = listOf(transform),
        layerVisibility = 0.8f.rf,
        animationSettings = settings,
      )
    assertThat(opacity.constantValueOrNull).isWithin(1e-4f).of(0.4f)
  }

  @Test
  fun hasActiveMasks_withNoMasks_returnsFalse() {
    assertThat(hasActiveMasks(emptyList())).isFalse()
  }

  @Test
  fun hasActiveMasks_withNoneModeMask_returnsFalse() {
    val mask = Mask(mode = MaskMode.None, path = StaticBezierProperty(value = BezierValue()))
    assertThat(hasActiveMasks(listOf(mask))).isFalse()
  }

  @Test
  fun hasActiveMasks_withValidAddMask_returnsTrue() {
    val mask = Mask(mode = MaskMode.Add, path = StaticBezierProperty(value = BezierValue()))
    assertThat(hasActiveMasks(listOf(mask))).isTrue()
  }

  @Test
  fun parseHexColor_parsesVariousFormats() {
    val c6 = parseHexColor("#ff0000")
    assertThat(c6.red).isEqualTo(1f)
    assertThat(c6.green).isEqualTo(0f)
    assertThat(c6.blue).isEqualTo(0f)

    val c8 = parseHexColor("#8000ff00")
    assertThat(c8.alpha).isWithin(0.01f).of(0.5f)
    assertThat(c8.green).isEqualTo(1f)

    val c3 = parseHexColor("#0f0")
    assertThat(c3.green).isEqualTo(1f)
    assertThat(c3.red).isEqualTo(0f)
  }
}
