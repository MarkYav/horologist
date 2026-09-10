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

import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.horologist.remotecompose.lottie.LottieSettings
import com.google.android.horologist.remotecompose.lottie.SlotMap
import com.google.android.horologist.remotecompose.lottie.format.LottieDecoder
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.GraphicElement
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.ShapeType
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.Rectangle
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.Fill
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.FillRule
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.GradientFill
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.GradientStroke
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.GradientType
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.LineCap
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.LineJoin
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.Stroke
import com.google.android.horologist.remotecompose.lottie.format.properties.AnimatedScalarProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.ScalarPropertyKeyframe
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticColorProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticGradientProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticPositionProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticScalarProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticVectorProperty
import com.google.android.horologist.remotecompose.lottie.format.values.ColorStop
import com.google.android.horologist.remotecompose.lottie.format.values.GradientValue
import com.google.android.horologist.remotecompose.lottie.format.values.Point
import com.google.android.horologist.remotecompose.lottie.renderer.properties.RemoteGradientValue
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StyleTest {

  private val animationSettings = LottieSettings(0f.rf, SlotMap.Empty)

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
    assertThat(gradientFill.fillRule).isEqualTo(FillRule.NonZero)
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
        "e": {"a": 0, "k": [100.0, 100.0]},
        "g": {
          "p": 2,
          "k": {"a": 0, "k": [0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0]}
        },
        "o": {"a": 0, "k": 100.0},
        "w": {"a": 0, "k": 4.0},
        "lc": 1,
        "lj": 1,
        "ml": 4.0
      }
      """
        .trimIndent()

    val element = LottieDecoder.json.decodeFromString(GraphicElement.serializer(), json)
    assertThat(element).isInstanceOf(GradientStroke::class.java)

    val gradientStroke = element as GradientStroke
    assertThat(gradientStroke.type).isEqualTo(ShapeType.GradientStroke)
    assertThat(gradientStroke.lineCap).isEqualTo(LineCap.Butt)
    assertThat(gradientStroke.lineJoin).isEqualTo(LineJoin.Miter)
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
        lineCap = LineCap.Round,
        lineJoin = LineJoin.Round,
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
        size = StaticVectorProperty(animated = false.rb, value = listOf(100f.rf, 100f.rf)),
        position = StaticPositionProperty(animated = false.rb, value = Point(50f.rf, 50f.rf)),
      )
    val gradValue =
      GradientValue(
        colorStops = listOf(ColorStop(0f, Color.Red.rc), ColorStop(1f, Color.Blue.rc)),
        transparencyStops = emptyList(),
      )
    val gradFill =
      GradientFill(
        gradientType = GradientType.Linear,
        startPoint = StaticPositionProperty(animated = false.rb, value = Point(0f.rf, 0f.rf)),
        endPoint = StaticPositionProperty(animated = false.rb, value = Point(100f.rf, 100f.rf)),
        colors = StaticGradientProperty(value = gradValue),
        opacity = StaticScalarProperty(value = 100f.rf),
      )
    val gradStroke =
      GradientStroke(
        gradientType = GradientType.Linear,
        startPoint = StaticPositionProperty(animated = false.rb, value = Point(0f.rf, 0f.rf)),
        endPoint = StaticPositionProperty(animated = false.rb, value = Point(100f.rf, 100f.rf)),
        colors = StaticGradientProperty(value = gradValue),
        opacity = StaticScalarProperty(value = 100f.rf),
        strokeWidth = StaticScalarProperty(value = 4f.rf),
      )

    val shapes = listOf(rect, gradFill, rect, gradStroke)
    val result = gatherShapes(shapes, animationSettings)

    assertThat(result).hasSize(2)
    assertThat(result[0].style).isInstanceOf(RemoteGradientStroke::class.java)
    assertThat(result[1].style).isInstanceOf(RemoteGradientFill::class.java)
  }

  @Test
  fun remoteFill_withDefaultOpacity_multipliesByInheritedOpacity() {
    val fill = RemoteFill(fillColor = Color.Red.rc)
    val paint = fill.getPaint(inheritedOpacity = 0.5f.rf)
    val alpha = paint.color.alpha.constantValueOrNull
    assertThat(alpha).isNotNull()
    assertThat(alpha!!).isWithin(0.01f).of(0.5f)
  }

  @Test
  fun remoteFill_withCustomOpacity_compoundsAlphaWithInheritedOpacity() {
    val fill = RemoteFill(fillColor = Color.Blue.rc, opacity = 50f.rf)
    val paint = fill.getPaint(inheritedOpacity = 0.8f.rf)
    val alpha = paint.color.alpha.constantValueOrNull
    assertThat(alpha).isNotNull()
    assertThat(alpha!!).isWithin(0.01f).of(1f * 0.5f * 0.8f) // 0.40f
  }

  @Test
  fun remoteFill_withAlphaColorAndCustomOpacity_compoundsAllAlphas() {
    val semiTransparentColor = Color(1f, 0f, 0f, 0.6f).rc
    val fill = RemoteFill(fillColor = semiTransparentColor, opacity = 50f.rf)
    val paint = fill.getPaint(inheritedOpacity = 0.5f.rf)
    val alpha = paint.color.alpha.constantValueOrNull
    assertThat(alpha).isNotNull()
    assertThat(alpha!!).isWithin(0.01f).of(0.6f * 0.5f * 0.5f) // 0.15f
  }

  @Test
  fun remoteStroke_withOpacity_compoundsAlphaWithInheritedOpacity() {
    val stroke = RemoteStroke(strokeColor = Color.White.rc, strokeWidth = 5f.rf, opacity = 80f.rf)
    val paint = stroke.getPaint(inheritedOpacity = 0.5f.rf)
    val alpha = paint.color.alpha.constantValueOrNull
    assertThat(alpha).isNotNull()
    assertThat(alpha!!).isWithin(0.01f).of(1f * 0.8f * 0.5f) // 0.40f
  }

  @Test
  fun gatherShapes_withStaticFillOpacity_createsRemoteFillWithEvaluatedOpacity() {
    val rect =
      Rectangle(
        position = StaticPositionProperty(animated = false.rb, value = Point(0f.rf, 0f.rf)),
        size = StaticVectorProperty(animated = false.rb, value = listOf(100f.rf, 100f.rf)),
      )
    val fill =
      Fill(
        color = StaticColorProperty(animated = false.rb, value = Color.Green.rc),
        opacity = StaticScalarProperty(value = 60f.rf),
        fillRule = FillRule.NonZero,
      )

    val settings = LottieSettings(currentFrame = 0f.rf, slotMap = SlotMap.Empty)
    val styledShapes = gatherShapesForTest(listOf(rect, fill), settings)

    assertThat(styledShapes).hasSize(1)
    val remoteFill = styledShapes[0].style as? RemoteFill
    assertThat(remoteFill).isNotNull()
    assertThat(remoteFill!!.opacity.constantValueOrNull).isEqualTo(60f)

    val paint = remoteFill.getPaint(inheritedOpacity = 1f.rf)
    assertThat(paint.color.alpha.constantValueOrNull).isWithin(1e-4f).of(0.6f)
  }

  @Test
  fun gatherShapes_withAnimatedFillOpacity_evaluatesOpacityAtCurrentFrame() {
    val rect =
      Rectangle(
        position = StaticPositionProperty(animated = false.rb, value = Point(0f.rf, 0f.rf)),
        size = StaticVectorProperty(animated = false.rb, value = listOf(100f.rf, 100f.rf)),
      )
    val keyframes =
      listOf(
        ScalarPropertyKeyframe(frame = 0f.rf, value = 0f.rf),
        ScalarPropertyKeyframe(frame = 30f.rf, value = 100f.rf),
      )
    val fill =
      Fill(
        color = StaticColorProperty(animated = false.rb, value = Color.Yellow.rc),
        opacity = AnimatedScalarProperty(keyframes = keyframes),
      )

    val frame0Settings = LottieSettings(currentFrame = 0f.rf, slotMap = SlotMap.Empty)
    val styledShapes0 = gatherShapesForTest(listOf(rect, fill), frame0Settings)
    val remoteFill0 = styledShapes0[0].style as RemoteFill
    assertThat(remoteFill0.opacity.constantValueOrNull).isEqualTo(0f)

    val frame30Settings = LottieSettings(currentFrame = 30f.rf, slotMap = SlotMap.Empty)
    val styledShapes30 = gatherShapesForTest(listOf(rect, fill), frame30Settings)
    val remoteFill30 = styledShapes30[0].style as RemoteFill
    assertThat(remoteFill30.opacity.constantValueOrNull).isEqualTo(100f)
  }

  @Test
  fun remoteStyleWithOpacity_compoundsMultiplierWithInheritedOpacity() {
    val baseFill = RemoteFill(fillColor = Color.Red.rc, opacity = 100f.rf)
    val wrapped = RemoteStyleWithOpacity(baseFill, opacityMultiplier = 0.5f.rf)
    val paint = wrapped.getPaint(inheritedOpacity = 0.5f.rf)
    val alpha = paint.color.alpha.constantValueOrNull
    assertThat(alpha).isNotNull()
    assertThat(alpha!!).isWithin(0.01f).of(0.25f)
  }

  @Test
  fun remoteStroke_withNearZeroWidth_suppressesAlphaToZero() {
    val stroke =
      RemoteStroke(strokeColor = Color.White.rc, strokeWidth = 0.0005f.rf, opacity = 100f.rf)
    val paint = stroke.getPaint(inheritedOpacity = 1f.rf)
    val alpha = paint.color.alpha.constantValueOrNull
    assertThat(alpha).isNotNull()
    assertThat(alpha!!).isEqualTo(0f)
  }
}
