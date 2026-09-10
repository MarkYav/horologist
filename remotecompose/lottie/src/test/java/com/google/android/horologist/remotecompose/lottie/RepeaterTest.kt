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
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.ui.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.horologist.remotecompose.lottie.format.LottieDecoder
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.GraphicElement
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.Path
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.Rectangle
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.grouping.Transform
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.CompositeMode
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.Repeater
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.Fill
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticBezierProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticColorProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticPositionProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticScalarProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticVectorProperty
import com.google.android.horologist.remotecompose.lottie.format.values.BezierValue
import com.google.android.horologist.remotecompose.lottie.format.values.Point
import com.google.android.horologist.remotecompose.lottie.renderer.RemoteDynamicGeometry
import com.google.android.horologist.remotecompose.lottie.renderer.gatherShapesForTest
import com.google.android.horologist.remotecompose.lottie.renderer.modifiers.evaluateRepeater
import com.google.android.horologist.remotecompose.lottie.renderer.properties.RemoteBezierValue
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SuppressLint("RestrictedApi")
class RepeaterTest {

  private val settings = LottieSettings(0f.rf, SlotMap.Empty)

  private fun pt(x: Float, y: Float): Point = Point(x.rf, y.rf)

  private fun testBezier(
    closed: Boolean,
    vertices: List<Point>,
    inTangents: List<Point>,
    outTangents: List<Point>,
  ): BezierValue =
    BezierValue(
      closed = closed.rb,
      vertices = vertices,
      inTangents = inTangents,
      outTangents = outTangents,
    )

  private val Point.xVal: Float
    get() = x.constantValueOrNull ?: 0f

  private val Point.yVal: Float
    get() = y.constantValueOrNull ?: 0f

  @Test
  fun repeater_jsonDeserialization() {
    val json =
      """
      {
        "ty": "rp",
        "nm": "Repeater 1",
        "c": {"a": 0, "k": 5},
        "o": {"a": 0, "k": 1},
        "m": 2,
        "tr": {
          "p": {"a": 0, "k": [10, 20]},
          "a": {"a": 0, "k": [0, 0]},
          "s": {"a": 0, "k": [100, 100]},
          "r": {"a": 0, "k": 45},
          "so": {"a": 0, "k": 100},
          "eo": {"a": 0, "k": 0}
        }
      }
      """
        .trimIndent()

    val element = LottieDecoder.json.decodeFromString<GraphicElement>(json)
    assertThat(element).isInstanceOf(Repeater::class.java)
    val repeater = element as Repeater
    assertThat(repeater.name).isEqualTo("Repeater 1")
    assertThat((repeater.copies as StaticScalarProperty).value.constantValueOrNull).isEqualTo(5f)
    assertThat((repeater.offset as StaticScalarProperty).value.constantValueOrNull).isEqualTo(1f)
    assertThat(repeater.composite).isEqualTo(CompositeMode.Below)
    assertThat(repeater.transform).isNotNull()
    assertThat((repeater.transform?.startOpacity as StaticScalarProperty).value.constantValueOrNull)
      .isEqualTo(100f)
    assertThat((repeater.transform?.endOpacity as StaticScalarProperty).value.constantValueOrNull)
      .isEqualTo(0f)
  }

  @Test
  fun repeaterGeometryDuplication_linearTranslation() {
    val subpath =
      RemoteBezierValue(
        closed = true,
        vertices = listOf(pt(10f, 10f)),
        inTangents = listOf(pt(0f, 0f)),
        outTangents = listOf(pt(0f, 0f)),
      )
    val lottiePath = RemoteDynamicGeometry(listOf(subpath))

    val repeater =
      Repeater(
        copies = StaticScalarProperty(value = 3f.rf),
        offset = StaticScalarProperty(value = 0f.rf),
        composite = CompositeMode.Above,
        transform =
          Transform(
            anchorPoint = StaticPositionProperty(value = pt(0f, 0f)),
            positionTranslation = StaticPositionProperty(value = pt(50f, 0f)),
            scale = StaticVectorProperty(animated = false.rb, value = listOf(100f.rf, 100f.rf)),
            rotation = StaticScalarProperty(value = 0f.rf),
            opacity = StaticScalarProperty(value = 100f.rf),
          ),
      )

    val results = evaluateRepeater(listOf(lottiePath), repeater, settings)
    assertThat(results).hasSize(3)

    // Copy 0: offset 0 -> position (10, 10)
    val path0 = results[0].shape as RemoteDynamicGeometry
    assertThat(path0.path[0].vertices[0].xVal).isWithin(0.01f).of(10f)
    assertThat(path0.path[0].vertices[0].yVal).isWithin(0.01f).of(10f)

    // Copy 1: offset 1 -> position (60, 10)
    val path1 = results[1].shape as RemoteDynamicGeometry
    assertThat(path1.path[0].vertices[0].xVal).isWithin(0.01f).of(60f)
    assertThat(path1.path[0].vertices[0].yVal).isWithin(0.01f).of(10f)

    // Copy 2: offset 2 -> position (110, 10)
    val path2 = results[2].shape as RemoteDynamicGeometry
    assertThat(path2.path[0].vertices[0].xVal).isWithin(0.01f).of(110f)
    assertThat(path2.path[0].vertices[0].yVal).isWithin(0.01f).of(10f)
  }

  @Test
  fun repeaterGeometryDuplication_withOffset() {
    val subpath =
      RemoteBezierValue(
        closed = true,
        vertices = listOf(pt(10f, 10f)),
        inTangents = listOf(pt(0f, 0f)),
        outTangents = listOf(pt(0f, 0f)),
      )
    val lottiePath = RemoteDynamicGeometry(listOf(subpath))

    val repeater =
      Repeater(
        copies = StaticScalarProperty(value = 3f.rf),
        offset = StaticScalarProperty(value = 1f.rf),
        composite = CompositeMode.Above,
        transform =
          Transform(
            anchorPoint = StaticPositionProperty(value = pt(0f, 0f)),
            positionTranslation = StaticPositionProperty(value = pt(50f, 0f)),
            scale = StaticVectorProperty(animated = false.rb, value = listOf(100f.rf, 100f.rf)),
            rotation = StaticScalarProperty(value = 0f.rf),
            opacity = StaticScalarProperty(value = 100f.rf),
          ),
      )

    val results = evaluateRepeater(listOf(lottiePath), repeater, settings)
    assertThat(results).hasSize(3)

    // Copy 0 with offset 1 -> k = 1 -> position (60, 10)
    val path0 = results[0].shape as RemoteDynamicGeometry
    assertThat(path0.path[0].vertices[0].xVal).isWithin(0.01f).of(60f)
    assertThat(path0.path[0].vertices[0].yVal).isWithin(0.01f).of(10f)

    // Copy 1 with offset 1 -> k = 2 -> position (110, 10)
    val path1 = results[1].shape as RemoteDynamicGeometry
    assertThat(path1.path[0].vertices[0].xVal).isWithin(0.01f).of(110f)

    // Copy 2 with offset 1 -> k = 3 -> position (160, 10)
    val path2 = results[2].shape as RemoteDynamicGeometry
    assertThat(path2.path[0].vertices[0].xVal).isWithin(0.01f).of(160f)
  }

  @Test
  fun repeaterGeometryDuplication_rotationAndScale() {
    val subpath =
      RemoteBezierValue(
        closed = true,
        vertices = listOf(pt(100f, 0f)),
        inTangents = listOf(pt(0f, 0f)),
        outTangents = listOf(pt(0f, 0f)),
      )
    val lottiePath = RemoteDynamicGeometry(listOf(subpath))

    val repeater =
      Repeater(
        copies = StaticScalarProperty(value = 4f.rf),
        offset = StaticScalarProperty(value = 0f.rf),
        composite = CompositeMode.Above,
        transform =
          Transform(
            anchorPoint = StaticPositionProperty(value = pt(0f, 0f)),
            positionTranslation = StaticPositionProperty(value = pt(0f, 0f)),
            scale = StaticVectorProperty(animated = false.rb, value = listOf(100f.rf, 100f.rf)),
            rotation = StaticScalarProperty(value = 90f.rf),
            opacity = StaticScalarProperty(value = 100f.rf),
          ),
      )

    val results = evaluateRepeater(listOf(lottiePath), repeater, settings)
    assertThat(results).hasSize(4)

    // Copy 0: rot 0 deg -> (100, 0)
    val path0 = results[0].shape as RemoteDynamicGeometry
    assertThat(path0.path[0].vertices[0].xVal).isWithin(0.01f).of(100f)
    assertThat(path0.path[0].vertices[0].yVal).isWithin(0.01f).of(0f)

    // Copy 1: rot 90 deg -> (0, 100)
    val path1 = results[1].shape as RemoteDynamicGeometry
    assertThat(path1.path[0].vertices[0].xVal).isWithin(0.01f).of(0f)
    assertThat(path1.path[0].vertices[0].yVal).isWithin(0.01f).of(100f)

    // Copy 2: rot 180 deg -> (-100, 0)
    val path2 = results[2].shape as RemoteDynamicGeometry
    assertThat(path2.path[0].vertices[0].xVal).isWithin(0.01f).of(-100f)
    assertThat(path2.path[0].vertices[0].yVal).isWithin(0.01f).of(0f)

    // Copy 3: rot 270 deg -> (0, -100)
    val path3 = results[3].shape as RemoteDynamicGeometry
    assertThat(path3.path[0].vertices[0].xVal).isWithin(0.01f).of(0f)
    assertThat(path3.path[0].vertices[0].yVal).isWithin(0.01f).of(-100f)
  }

  @Test
  fun repeaterCompositeMode_belowReversesOrder() {
    val subpath =
      RemoteBezierValue(
        closed = true,
        vertices = listOf(pt(0f, 0f)),
        inTangents = listOf(pt(0f, 0f)),
        outTangents = listOf(pt(0f, 0f)),
      )
    val lottiePath = RemoteDynamicGeometry(listOf(subpath))

    val repeater =
      Repeater(
        copies = StaticScalarProperty(value = 3f.rf),
        offset = StaticScalarProperty(value = 0f.rf),
        composite = CompositeMode.Below,
        transform =
          Transform(
            anchorPoint = StaticPositionProperty(value = pt(0f, 0f)),
            positionTranslation = StaticPositionProperty(value = pt(10f, 0f)),
            scale = StaticVectorProperty(animated = false.rb, value = listOf(100f.rf, 100f.rf)),
            rotation = StaticScalarProperty(value = 0f.rf),
            opacity = StaticScalarProperty(value = 100f.rf),
          ),
      )

    val results = evaluateRepeater(listOf(lottiePath), repeater, settings)
    assertThat(results).hasSize(3)

    // CompositeMode.Below renders index 2 first, then 1, then 0
    val path0 = results[0].shape as RemoteDynamicGeometry
    assertThat(path0.path[0].vertices[0].xVal).isWithin(0.01f).of(20f)

    val path1 = results[1].shape as RemoteDynamicGeometry
    assertThat(path1.path[0].vertices[0].xVal).isWithin(0.01f).of(10f)

    val path2 = results[2].shape as RemoteDynamicGeometry
    assertThat(path2.path[0].vertices[0].xVal).isWithin(0.01f).of(0f)
  }

  @Test
  fun repeaterOpacityInterpolation_startEndOpacity() {
    val subpath =
      RemoteBezierValue(
        closed = true,
        vertices = listOf(pt(0f, 0f)),
        inTangents = listOf(pt(0f, 0f)),
        outTangents = listOf(pt(0f, 0f)),
      )
    val lottiePath = RemoteDynamicGeometry(listOf(subpath))

    val repeater =
      Repeater(
        copies = StaticScalarProperty(value = 3f.rf),
        offset = StaticScalarProperty(value = 0f.rf),
        composite = CompositeMode.Above,
        transform =
          Transform(
            anchorPoint = StaticPositionProperty(value = pt(0f, 0f)),
            positionTranslation = StaticPositionProperty(value = pt(10f, 0f)),
            scale = StaticVectorProperty(animated = false.rb, value = listOf(100f.rf, 100f.rf)),
            rotation = StaticScalarProperty(value = 0f.rf),
            startOpacity = StaticScalarProperty(value = 100f.rf),
            endOpacity = StaticScalarProperty(value = 0f.rf),
          ),
      )

    val results = evaluateRepeater(listOf(lottiePath), repeater, settings)
    assertThat(results).hasSize(3)

    assertThat(results[0].opacityMultiplier.constantValueOrNull).isWithin(0.01f).of(1f)
    assertThat(results[1].opacityMultiplier.constantValueOrNull).isWithin(0.01f).of(0.5f)
    assertThat(results[2].opacityMultiplier.constantValueOrNull).isWithin(0.01f).of(0f)
  }

  @Test
  fun repeaterExponentialScale_withFractionalOffset_avoidsNaN() {
    val subpath =
      RemoteBezierValue(
        closed = true,
        vertices = listOf(pt(10f, 0f)),
        inTangents = listOf(pt(0f, 0f)),
        outTangents = listOf(pt(0f, 0f)),
      )
    val lottiePath = RemoteDynamicGeometry(listOf(subpath))

    val repeater =
      Repeater(
        copies = StaticScalarProperty(value = 2f.rf),
        offset = StaticScalarProperty(value = 0.5f.rf),
        composite = CompositeMode.Above,
        transform =
          Transform(
            anchorPoint = StaticPositionProperty(value = pt(0f, 0f)),
            positionTranslation = StaticPositionProperty(value = pt(0f, 0f)),
            scale = StaticVectorProperty(animated = false.rb, value = listOf((-100f).rf, 100f.rf)),
            rotation = StaticScalarProperty(value = 0f.rf),
          ),
      )

    val results = evaluateRepeater(listOf(lottiePath), repeater, settings)
    assertThat(results).hasSize(2)
    val path0 = results[0].shape as RemoteDynamicGeometry
    val x0 = path0.path[0].vertices[0].xVal
    assertThat(x0.isNaN()).isFalse()
  }

  @Test
  fun repeaterInShapeLayer_gatherShapesIntegration() {
    val rect =
      Rectangle(
        position = StaticPositionProperty(value = pt(0f, 0f)),
        size = StaticVectorProperty(animated = false.rb, value = listOf(20f.rf, 20f.rf)),
      )
    val repeater =
      Repeater(
        copies = StaticScalarProperty(value = 3f.rf),
        offset = StaticScalarProperty(value = 0f.rf),
        composite = CompositeMode.Above,
        transform =
          Transform(
            anchorPoint = StaticPositionProperty(value = pt(0f, 0f)),
            positionTranslation = StaticPositionProperty(value = pt(30f, 0f)),
            scale = StaticVectorProperty(animated = false.rb, value = listOf(100f.rf, 100f.rf)),
            rotation = StaticScalarProperty(value = 0f.rf),
            opacity = StaticScalarProperty(value = 100f.rf),
          ),
      )
    val fill =
      Fill(
        color = StaticColorProperty(value = Color.Red.rc),
        opacity = StaticScalarProperty(value = 100f.rf),
      )

    val styledGroups = gatherShapesForTest(listOf(rect, repeater, fill), settings)
    assertThat(styledGroups).isNotEmpty()
    val totalShapes = styledGroups.sumOf { it.shapes.size }
    assertThat(totalShapes).isEqualTo(3)
  }

  @Test
  fun repeaterInShapeLayer_withVaryingOpacity_drawOrder() {
    val path =
      Path(
        shape =
          StaticBezierProperty(
            value =
              testBezier(
                closed = true,
                vertices = listOf(pt(10f, 0f)),
                inTangents = listOf(pt(0f, 0f)),
                outTangents = listOf(pt(0f, 0f)),
              )
          )
      )
    val repeater =
      Repeater(
        copies = StaticScalarProperty(value = 3f.rf),
        offset = StaticScalarProperty(value = 0f.rf),
        composite = CompositeMode.Above,
        transform =
          Transform(
            anchorPoint = StaticPositionProperty(value = pt(0f, 0f)),
            positionTranslation = StaticPositionProperty(value = pt(30f, 0f)),
            scale = StaticVectorProperty(animated = false.rb, value = listOf(100f.rf, 100f.rf)),
            rotation = StaticScalarProperty(value = 0f.rf),
            startOpacity = StaticScalarProperty(value = 100f.rf),
            endOpacity = StaticScalarProperty(value = 20f.rf),
          ),
      )
    val fill =
      Fill(
        color = StaticColorProperty(value = Color.Red.rc),
        opacity = StaticScalarProperty(value = 100f.rf),
      )

    val styledGroups = gatherShapesForTest(listOf(path, repeater, fill), settings)
    // For varying opacity, individual StyledShapes are created
    assertThat(styledGroups).hasSize(3)
    // Draw order must be copy 0, then copy 1, then copy 2
    val p0 = styledGroups[0].shapes[0] as RemoteDynamicGeometry
    val p1 = styledGroups[1].shapes[0] as RemoteDynamicGeometry
    val p2 = styledGroups[2].shapes[0] as RemoteDynamicGeometry

    assertThat(p0.path[0].vertices[0].xVal).isWithin(0.01f).of(10f)
    assertThat(p1.path[0].vertices[0].xVal).isWithin(0.01f).of(40f)
    assertThat(p2.path[0].vertices[0].xVal).isWithin(0.01f).of(70f)
  }
}
