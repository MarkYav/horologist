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

import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.ui.graphics.PaintingStyle
import com.google.android.horologist.remotecompose.lottie.format.LottieDecoder
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.GraphicElement
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.ShapeType
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.GradientFill
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.GradientType
import com.google.android.horologist.remotecompose.lottie.renderer.RemoteGradientFill
import com.google.android.horologist.remotecompose.lottie.renderer.properties.Point
import com.google.android.horologist.remotecompose.lottie.renderer.properties.RemoteGradientValue
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StyleAndGradientSafetyTest {

  @Test
  fun emptyGradientStops_compilesWithoutCrash() {
    val emptyGradient = RemoteGradientValue(numberOfColors = 0, values = emptyList())
    val remoteFill =
      RemoteGradientFill(
        gradientType = GradientType.Linear,
        startPoint = Point(0f.rf, 0f.rf),
        endPoint = Point(100f.rf, 100f.rf),
        gradient = emptyGradient,
        opacity = 100f.rf,
      )

    val paint = remoteFill.getPaint()
    assertThat(paint.style).isEqualTo(PaintingStyle.Fill)
  }

  @Test
  fun singleGradientStop_compilesWithoutCrash() {
    val singleStop =
      RemoteGradientValue(numberOfColors = 1, values = listOf(0f.rf, 1f.rf, 0.5f.rf, 0.2f.rf))
    val remoteFill =
      RemoteGradientFill(
        gradientType = GradientType.Linear,
        startPoint = Point(0f.rf, 0f.rf),
        endPoint = Point(100f.rf, 100f.rf),
        gradient = singleStop,
        opacity = 100f.rf,
      )

    val paint = remoteFill.getPaint()
    assertThat(paint.style).isEqualTo(PaintingStyle.Fill)
  }

  @Test
  fun gradientWithAlphaStops_compilesWithoutCrash() {
    // 2 color stops (0.0 to red, 1.0 to blue) + 2 alpha stops (0.0 to 1.0, 1.0 to 0.5)
    val gradientWithAlpha =
      RemoteGradientValue(
        numberOfColors = 2,
        values =
          listOf(
            0f.rf,
            1f.rf,
            0f.rf,
            0f.rf, // stop 0: red
            1f.rf,
            0f.rf,
            0f.rf,
            1f.rf, // stop 1: blue
            0f.rf,
            1f.rf, // alpha stop 0: 1.0
            1f.rf,
            0.5f.rf, // alpha stop 1: 0.5
          ),
      )
    val remoteFill =
      RemoteGradientFill(
        gradientType = GradientType.Radial,
        startPoint = Point(50f.rf, 50f.rf),
        endPoint = Point(50f.rf, 100f.rf),
        gradient = gradientWithAlpha,
        opacity = 100f.rf,
      )

    val paint = remoteFill.getPaint()
    assertThat(paint.style).isEqualTo(PaintingStyle.Fill)
  }

  @Test
  fun zeroLengthGradient_compilesWithoutCrash() {
    val gradientVal =
      RemoteGradientValue(
        numberOfColors = 2,
        values = listOf(0f.rf, 1f.rf, 0f.rf, 0f.rf, 1f.rf, 0f.rf, 0f.rf, 1f.rf),
      )
    val zeroLengthFill =
      RemoteGradientFill(
        gradientType = GradientType.Linear,
        startPoint = Point(10f.rf, 10f.rf),
        endPoint = Point(10f.rf, 10f.rf),
        gradient = gradientVal,
        opacity = 100f.rf,
      )

    val paint = zeroLengthFill.getPaint()
    assertThat(paint.style).isEqualTo(PaintingStyle.Fill)
  }

  @Test
  fun zeroRadiusRadialGradient_compilesWithoutCrash() {
    val gradientVal =
      RemoteGradientValue(
        numberOfColors = 2,
        values = listOf(0f.rf, 1f.rf, 0f.rf, 0f.rf, 1f.rf, 0f.rf, 0f.rf, 1f.rf),
      )
    val zeroRadiusFill =
      RemoteGradientFill(
        gradientType = GradientType.Radial,
        startPoint = Point(50f.rf, 50f.rf),
        endPoint = Point(50f.rf, 50f.rf),
        gradient = gradientVal,
        opacity = 100f.rf,
      )

    val paint = zeroRadiusFill.getPaint()
    assertThat(paint.style).isEqualTo(PaintingStyle.Fill)
  }

  @Test
  fun unknownGradientType_deserializesGracefullyToLinear() {
    val json =
      """
      {
        "ty": "gf",
        "t": 999,
        "s": {"a": 0, "k": [0.0, 0.0]},
        "e": {"a": 0, "k": [10.0, 10.0]},
        "g": {"p": 1, "k": {"a": 0, "k": [0.0, 1.0, 1.0, 1.0]}},
        "o": {"a": 0, "k": 100.0}
      }
      """
        .trimIndent()

    val element = LottieDecoder.json.decodeFromString(GraphicElement.serializer(), json)
    assertThat(element).isInstanceOf(GradientFill::class.java)

    val fill = element as GradientFill
    assertThat(fill.type).isEqualTo(ShapeType.GradientFill)
    assertThat(fill.gradientType).isEqualTo(GradientType.Linear)
  }
}
