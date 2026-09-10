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

import android.annotation.SuppressLint
import androidx.compose.remote.creation.RemotePath
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.ui.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.Rectangle
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.Fill
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.FillRule
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.GradientFill
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.GradientType
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticColorProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticGradientProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticPositionProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticScalarProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticVectorProperty
import com.google.android.horologist.remotecompose.lottie.format.values.ColorStop
import com.google.android.horologist.remotecompose.lottie.format.values.GradientValue
import com.google.android.horologist.remotecompose.lottie.format.values.Point
import com.google.android.horologist.remotecompose.lottie.renderer.NoopStyle
import com.google.android.horologist.remotecompose.lottie.renderer.RemoteCompiledPath
import com.google.android.horologist.remotecompose.lottie.renderer.RemoteFill
import com.google.android.horologist.remotecompose.lottie.renderer.RemoteGradientFill
import com.google.android.horologist.remotecompose.lottie.renderer.RemoteGroup
import com.google.android.horologist.remotecompose.lottie.renderer.RemoteLottiePath
import com.google.android.horologist.remotecompose.lottie.renderer.StyledShapes
import com.google.android.horologist.remotecompose.lottie.renderer.gatherShapesForTest
import com.google.android.horologist.remotecompose.lottie.renderer.properties.RemoteGradientValue
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SuppressLint("RestrictedApi")
class PrimitiveTrimPathAndFillRuleTest {

  private val settings = LottieSettings(currentFrame = 0f.rf, slotMap = SlotMap.Empty)

  @Test
  fun fillRule_defaultsToNonZero() {
    val fill =
      Fill(
        color = StaticColorProperty(value = Color.Black.rc),
        opacity = StaticScalarProperty(value = 100f.rf),
      )
    assertThat(fill.fillRule).isNull()

    val gradientFill =
      GradientFill(
        opacity = StaticScalarProperty(value = 100f.rf),
        colors =
          StaticGradientProperty(
            value =
              GradientValue(
                colorStops = listOf(ColorStop(0f, Color.Red.rc)),
                transparencyStops = emptyList(),
              )
          ),
        startPoint = StaticPositionProperty(value = Point(0f.rf, 0f.rf)),
        endPoint = StaticPositionProperty(value = Point(100f.rf, 0f.rf)),
        gradientType = GradientType.Linear,
      )
    assertThat(gradientFill.fillRule).isNull()
  }

  @Test
  fun fillRule_supportsEvenOdd() {
    val fill =
      Fill(
        color = StaticColorProperty(value = Color.Red.rc),
        opacity = StaticScalarProperty(value = 100f.rf),
        fillRule = FillRule.EvenOdd,
      )
    assertThat(fill.fillRule).isEqualTo(FillRule.EvenOdd)

    val gradientFill =
      GradientFill(
        opacity = StaticScalarProperty(value = 100f.rf),
        colors =
          StaticGradientProperty(
            value =
              GradientValue(
                colorStops = listOf(ColorStop(0f, Color.Red.rc)),
                transparencyStops = emptyList(),
              )
          ),
        startPoint = StaticPositionProperty(value = Point(0f.rf, 0f.rf)),
        endPoint = StaticPositionProperty(value = Point(100f.rf, 0f.rf)),
        gradientType = GradientType.Linear,
        fillRule = FillRule.EvenOdd,
      )
    assertThat(gradientFill.fillRule).isEqualTo(FillRule.EvenOdd)
  }

  @Test
  fun remoteFill_and_remoteGradientFill_holdFillRule() {
    val remoteFill = RemoteFill(fillColor = Color.Red.rc, fillRule = FillRule.EvenOdd)
    assertThat(remoteFill.fillRule).isEqualTo(FillRule.EvenOdd)

    val gradientValue =
      RemoteGradientValue(
        numberOfColors = 2,
        values = listOf(0f.rf, 1f.rf, 0f.rf, 0f.rf, 1f.rf, 0f.rf, 0f.rf, 1f.rf),
      )
    val remoteGradientFill =
      RemoteGradientFill(
        gradient = gradientValue,
        startPoint = Point(0f.rf, 0f.rf),
        endPoint = Point(100f.rf, 0f.rf),
        gradientType = GradientType.Linear,
        opacity = 100f.rf,
        fillRule = FillRule.EvenOdd,
      )
    assertThat(remoteGradientFill.fillRule).isEqualTo(FillRule.EvenOdd)
  }

  @Test
  fun remoteLottiePath_and_remoteCompiledPath_holdFillRule() {
    val lottiePath = RemoteLottiePath(path = emptyList(), fillRule = FillRule.EvenOdd)
    assertThat(lottiePath.fillRule).isEqualTo(FillRule.EvenOdd)

    val compiledPath = RemoteCompiledPath(path = RemotePath(), fillRule = FillRule.EvenOdd)
    assertThat(compiledPath.fillRule).isEqualTo(FillRule.EvenOdd)
  }

  @Test
  fun remoteShape_withFillRule_updatesFillRuleCorrectly() {
    val lottiePath = RemoteLottiePath(path = emptyList(), fillRule = FillRule.NonZero)
    val updatedLottiePath = lottiePath.withFillRule(FillRule.EvenOdd)
    assertThat((updatedLottiePath as RemoteLottiePath).fillRule).isEqualTo(FillRule.EvenOdd)

    val compiledPath = RemoteCompiledPath(path = RemotePath(), fillRule = FillRule.NonZero)
    val updatedCompiledPath = compiledPath.withFillRule(FillRule.EvenOdd)
    assertThat((updatedCompiledPath as RemoteCompiledPath).fillRule).isEqualTo(FillRule.EvenOdd)

    val group =
      RemoteGroup(
        childShapes = listOf(StyledShapes(listOf(lottiePath), NoopStyle())),
        animationSettings = settings,
        transform = null,
      )
    val updatedGroup = group.withFillRule(FillRule.EvenOdd)
    val updatedInnerShape = updatedGroup.childShapes.first().shapes.first()
    assertThat((updatedInnerShape as RemoteLottiePath).fillRule).isEqualTo(FillRule.EvenOdd)
  }

  @Test
  fun gatherShapes_withEvenOddFill_propagatesFillRuleToGeometries() {
    val rect =
      Rectangle(
        position = StaticPositionProperty(value = Point(0f.rf, 0f.rf)),
        size = StaticVectorProperty(animated = false.rb, value = listOf(100f.rf, 100f.rf)),
      )
    val fill =
      Fill(
        color = StaticColorProperty(value = Color.Red.rc),
        opacity = StaticScalarProperty(value = 100f.rf),
        fillRule = FillRule.EvenOdd,
      )

    val styledShapes = gatherShapesForTest(listOf(rect, fill), settings)
    assertThat(styledShapes).isNotEmpty()

    val shapeGroup = styledShapes.first()
    assertThat(shapeGroup.shapes).isNotEmpty()
    val evaluatedShape = shapeGroup.shapes.first()
    assertThat((evaluatedShape as RemoteCompiledPath).fillRule).isEqualTo(FillRule.EvenOdd)
  }

  @Test
  fun gatherShapes_withEvenOddGradientFill_propagatesFillRuleToGeometries() {
    val rect =
      Rectangle(
        position = StaticPositionProperty(value = Point(0f.rf, 0f.rf)),
        size = StaticVectorProperty(animated = false.rb, value = listOf(100f.rf, 100f.rf)),
      )
    val gradientFill =
      GradientFill(
        opacity = StaticScalarProperty(value = 100f.rf),
        colors =
          StaticGradientProperty(
            value =
              GradientValue(
                colorStops = listOf(ColorStop(0f, Color.Red.rc)),
                transparencyStops = emptyList(),
              )
          ),
        startPoint = StaticPositionProperty(value = Point(0f.rf, 0f.rf)),
        endPoint = StaticPositionProperty(value = Point(100f.rf, 0f.rf)),
        gradientType = GradientType.Linear,
        fillRule = FillRule.EvenOdd,
      )

    val styledShapes = gatherShapesForTest(listOf(rect, gradientFill), settings)
    assertThat(styledShapes).isNotEmpty()

    val shapeGroup = styledShapes.first()
    assertThat(shapeGroup.shapes).isNotEmpty()
    val evaluatedShape = shapeGroup.shapes.first()
    assertThat((evaluatedShape as RemoteCompiledPath).fillRule).isEqualTo(FillRule.EvenOdd)
  }

  @Test
  fun remoteShape_drawing_withEvenOdd_maintainsFillRuleConfiguration() {
    val evenOddLottiePath = RemoteLottiePath(path = emptyList(), fillRule = FillRule.EvenOdd)
    val nonZeroLottiePath = RemoteLottiePath(path = emptyList(), fillRule = FillRule.NonZero)

    assertThat(evenOddLottiePath.fillRule).isEqualTo(FillRule.EvenOdd)
    assertThat(nonZeroLottiePath.fillRule).isEqualTo(FillRule.NonZero)

    val evenOddCompiledPath = RemoteCompiledPath(path = RemotePath(), fillRule = FillRule.EvenOdd)
    val nonZeroCompiledPath = RemoteCompiledPath(path = RemotePath(), fillRule = FillRule.NonZero)

    assertThat(evenOddCompiledPath.fillRule).isEqualTo(FillRule.EvenOdd)
    assertThat(nonZeroCompiledPath.fillRule).isEqualTo(FillRule.NonZero)
  }
}
