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

package com.google.android.horologist.remotecompose.lottie.renderer

import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import com.google.android.horologist.remotecompose.lottie.LottieSettings
import com.google.android.horologist.remotecompose.lottie.SlotMap
import com.google.android.horologist.remotecompose.lottie.format.LottieDecoder
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.GraphicElement
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.ShapeType
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.Rectangle
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.GradientFill
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.GradientStroke
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.GradientType
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.LineCap
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.LineJoin
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticGradientProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticPositionProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticScalarProperty
import com.google.android.horologist.remotecompose.lottie.format.values.GradientValue
import com.google.android.horologist.remotecompose.lottie.renderer.properties.Point
import com.google.android.horologist.remotecompose.lottie.renderer.properties.RemoteGradientValue
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StyleTest {

  private val animationSettings = LottieSettings(0.rf, SlotMap.Empty)

  @Test
  fun deserializeGradientFill_linear() {
    val json =
      """
      {
        "ty": "gf",
        "nm": "Linear Gradient Fill",
        "t": 1,
        "s": {"a": 0, "k": [10.0, 20.0]},
        "e": {"a": 0, "k": [100.0, 200.0]},
        "g": {
          "p": 2,
          "k": {"a": 0, "k": [0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0]}
        },
        "o": {"a": 0, "k": 100.0},
        "r": 1
      }
      """
        .trimIndent()

    val element = LottieDecoder.json.decodeFromString(GraphicElement.serializer(), json)
    assertThat(element).isInstanceOf(GradientFill::class.java)

    val gradientFill = element as GradientFill
    assertThat(gradientFill.type).isEqualTo(ShapeType.GradientFill)
    assertThat(gradientFill.gradientType).isEqualTo(GradientType.Linear)
    assertThat((gradientFill.startPoint as StaticPositionProperty).value).containsExactly(10f, 20f)
    assertThat((gradientFill.endPoint as StaticPositionProperty).value).containsExactly(100f, 200f)
    assertThat((gradientFill.gradientColors as StaticGradientProperty).value.numberOfColors)
      .isEqualTo(2)
  }

  @Test
  fun deserializeGradientFill_radial() {
    val json =
      """
      {
        "ty": "gf",
        "nm": "Radial Gradient Fill",
        "t": 2,
        "s": {"a": 0, "k": [50.0, 50.0]},
        "e": {"a": 0, "k": [100.0, 50.0]},
        "g": {
          "p": 2,
          "k": {"a": 0, "k": [0.0, 1.0, 1.0, 0.0, 1.0, 0.0, 1.0, 1.0]}
        },
        "o": {"a": 0, "k": 80.0},
        "h": {"a": 0, "k": 0.0},
        "a": {"a": 0, "k": 0.0}
      }
      """
        .trimIndent()

    val element = LottieDecoder.json.decodeFromString(GraphicElement.serializer(), json)
    assertThat(element).isInstanceOf(GradientFill::class.java)

    val gradientFill = element as GradientFill
    assertThat(gradientFill.gradientType).isEqualTo(GradientType.Radial)
    assertThat((gradientFill.opacity as StaticScalarProperty).value).isEqualTo(80f)
    assertThat(gradientFill.highlightLength).isNotNull()
    assertThat(gradientFill.highlightAngle).isNotNull()
  }

  @Test
  fun deserializeGradientStroke() {
    val json =
      """
      {
        "ty": "gs",
        "nm": "Gradient Stroke",
        "t": 1,
        "s": {"a": 0, "k": [0.0, 0.0]},
        "e": {"a": 0, "k": [50.0, 50.0]},
        "g": {
          "p": 2,
          "k": {"a": 0, "k": [0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0]}
        },
        "o": {"a": 0, "k": 100.0},
        "w": {"a": 0, "k": 6.0},
        "lc": 2,
        "lj": 2,
        "ml": 4.0
      }
      """
        .trimIndent()

    val element = LottieDecoder.json.decodeFromString(GraphicElement.serializer(), json)
    assertThat(element).isInstanceOf(GradientStroke::class.java)

    val stroke = element as GradientStroke
    assertThat(stroke.type).isEqualTo(ShapeType.GradientStroke)
    assertThat(stroke.gradientType).isEqualTo(GradientType.Linear)
    assertThat((stroke.strokeWidth as StaticScalarProperty).value).isEqualTo(6f)
    assertThat(stroke.lineCap).isEqualTo(LineCap.Round)
    assertThat(stroke.lineJoin).isEqualTo(LineJoin.Round)
    assertThat(stroke.miterLimit).isEqualTo(4f)
  }

  @Test
  fun remoteGradientFill_linearCompilation() {
    val gradientVal =
      RemoteGradientValue(
        numberOfColors = 2,
        values = listOf(0f.rf, 1f.rf, 0f.rf, 0f.rf, 1f.rf, 0f.rf, 0f.rf, 1f.rf),
      )
    val remoteFill =
      RemoteGradientFill(
        gradientType = GradientType.Linear,
        startPoint = Point(0f.rf, 0f.rf),
        endPoint = Point(100f.rf, 100f.rf),
        gradient = gradientVal,
        opacity = 100f.rf,
      )

    val paint = remoteFill.getPaint()
    assertThat(paint.style).isEqualTo(PaintingStyle.Fill)
  }

  @Test
  fun remoteGradientFill_radialCompilation() {
    val gradientVal =
      RemoteGradientValue(
        numberOfColors = 2,
        values = listOf(0f.rf, 1f.rf, 0f.rf, 0f.rf, 1f.rf, 0f.rf, 1f.rf, 0f.rf),
      )
    val remoteFill =
      RemoteGradientFill(
        gradientType = GradientType.Radial,
        startPoint = Point(50f.rf, 50f.rf),
        endPoint = Point(100f.rf, 50f.rf),
        gradient = gradientVal,
        opacity = 100f.rf,
      )

    val paint = remoteFill.getPaint()
    assertThat(paint.style).isEqualTo(PaintingStyle.Fill)
  }

  @Test
  fun remoteGradientStroke_compilation() {
    val gradientVal =
      RemoteGradientValue(
        numberOfColors = 2,
        values = listOf(0f.rf, 1f.rf, 0f.rf, 0f.rf, 1f.rf, 0f.rf, 0f.rf, 1f.rf),
      )
    val remoteStroke =
      RemoteGradientStroke(
        gradientType = GradientType.Linear,
        startPoint = Point(0f.rf, 0f.rf),
        endPoint = Point(50f.rf, 50f.rf),
        gradient = gradientVal,
        opacity = 100f.rf,
        strokeWidth = 12f.rf,
        strokeCap = StrokeCap.Round,
        strokeJoin = StrokeJoin.Round,
        miterLimit = 4f,
      )

    val paint = remoteStroke.getPaint()
    assertThat(paint.style).isEqualTo(PaintingStyle.Stroke)
    assertThat(paint.strokeCap).isEqualTo(StrokeCap.Round)
    assertThat(paint.strokeJoin).isEqualTo(StrokeJoin.Round)
  }

  @Test
  fun gatherShapes_withGradientFillAndStroke() {
    val rect =
      Rectangle(
        size =
          com.google.android.horologist.remotecompose.lottie.format.properties.StaticVectorProperty(
            value = floatArrayOf(100f, 100f)
          ),
        position = StaticPositionProperty(value = listOf(50f, 50f)),
      )
    val gradFill =
      GradientFill(
        gradientType = GradientType.Linear,
        startPoint = StaticPositionProperty(value = listOf(0f, 0f)),
        endPoint = StaticPositionProperty(value = listOf(100f, 100f)),
        gradientColors =
          StaticGradientProperty(
            value =
              GradientValue(numberOfColors = 2, values = listOf(0f, 1f, 0f, 0f, 1f, 0f, 1f, 0f))
          ),
        opacity = StaticScalarProperty(value = 100f),
      )
    val gradStroke =
      GradientStroke(
        gradientType = GradientType.Linear,
        startPoint = StaticPositionProperty(value = listOf(0f, 0f)),
        endPoint = StaticPositionProperty(value = listOf(100f, 100f)),
        gradientColors =
          StaticGradientProperty(
            value =
              GradientValue(numberOfColors = 2, values = listOf(0f, 1f, 0f, 0f, 1f, 0f, 1f, 0f))
          ),
        opacity = StaticScalarProperty(value = 100f),
        strokeWidth = StaticScalarProperty(value = 4f),
      )

    val shapes = listOf(rect, gradFill, rect, gradStroke)
    val result = gatherShapes(shapes, animationSettings)

    assertThat(result).hasSize(2)
    assertThat(result[0].style).isInstanceOf(RemoteGradientStroke::class.java)
    assertThat(result[1].style).isInstanceOf(RemoteGradientFill::class.java)
  }
}
