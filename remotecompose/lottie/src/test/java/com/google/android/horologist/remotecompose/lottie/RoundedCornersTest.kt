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
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.Path
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.RoundedCorners
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.TrimPath
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.Fill
import com.google.android.horologist.remotecompose.lottie.format.properties.AnimatedScalarProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.ScalarPropertyKeyframe
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticBezierProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticColorProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticScalarProperty
import com.google.android.horologist.remotecompose.lottie.format.values.BezierValue
import com.google.android.horologist.remotecompose.lottie.format.values.Point
import com.google.android.horologist.remotecompose.lottie.renderer.RemoteDynamicGeometry
import com.google.android.horologist.remotecompose.lottie.renderer.gatherShapesForTest
import com.google.android.horologist.remotecompose.lottie.renderer.modifiers.roundBezierValue
import com.google.android.horologist.remotecompose.lottie.renderer.shapes.path
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SuppressLint("RestrictedApi")
class RoundedCornersTest {

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
  fun roundBezierValue_sharpClosedTriangle_roundsAllThreeCorners() {
    // Closed right triangle: (0,0) -> (100,0) -> (0,100) -> closed
    val triangle =
      testBezier(
        closed = true,
        vertices = listOf(pt(0f, 0f), pt(100f, 0f), pt(0f, 100f)),
        inTangents = listOf(pt(0f, 0f), pt(0f, 0f), pt(0f, 0f)),
        outTangents = listOf(pt(0f, 0f), pt(0f, 0f), pt(0f, 0f)),
      )

    val rounded = roundBezierValue(triangle, radius = 10f)

    // Each of the 3 sharp corners is split into 2 vertices (pStart, pEnd) -> 6 vertices total
    assertThat(rounded.closed.constantValue).isTrue()
    assertThat(rounded.vertices).hasSize(6)
    assertThat(rounded.inTangents).hasSize(6)
    assertThat(rounded.outTangents).hasSize(6)

    // Corner 0 at (0,0):
    // vPrev is (0,100), edge length 100 -> pStart at (0, 10)
    // vNext is (100,0), edge length 100 -> pEnd at (10, 0)
    val pStart0 = rounded.vertices[0]
    val pEnd0 = rounded.vertices[1]
    assertThat(pStart0.xVal).isWithin(0.01f).of(0f)
    assertThat(pStart0.yVal).isWithin(0.01f).of(10f)
    assertThat(pEnd0.xVal).isWithin(0.01f).of(10f)
    assertThat(pEnd0.yVal).isWithin(0.01f).of(0f)

    // Out-tangent at pStart0 points towards (0,0): (0, -10 * 0.5519)
    assertThat(rounded.outTangents[0].xVal).isWithin(0.01f).of(0f)
    assertThat(rounded.outTangents[0].yVal).isLessThan(0f)

    // In-tangent at pEnd0 points from (0,0): (-10 * 0.5519, 0)
    assertThat(rounded.inTangents[1].xVal).isLessThan(0f)
    assertThat(rounded.inTangents[1].yVal).isWithin(0.01f).of(0f)
  }

  @Test
  fun roundBezierValue_zeroRadius_preservesSharpPolygonGeometry() {
    val triangle =
      testBezier(
        closed = true,
        vertices = listOf(pt(0f, 0f), pt(100f, 0f), pt(0f, 100f)),
        inTangents = listOf(pt(0f, 0f), pt(0f, 0f), pt(0f, 0f)),
        outTangents = listOf(pt(0f, 0f), pt(0f, 0f), pt(0f, 0f)),
      )

    val rounded = roundBezierValue(triangle, radius = 0f)
    assertThat(rounded.vertices).hasSize(6)
    assertThat(rounded.vertices[0].xVal).isEqualTo(0f)
    assertThat(rounded.vertices[0].yVal).isEqualTo(0f)
    assertThat(rounded.vertices[1].xVal).isEqualTo(0f)
    assertThat(rounded.vertices[1].yVal).isEqualTo(0f)
    assertThat(rounded.vertices[2].xVal).isEqualTo(100f)
    assertThat(rounded.vertices[2].yVal).isEqualTo(0f)
    assertThat(rounded.vertices[3].xVal).isEqualTo(100f)
    assertThat(rounded.vertices[3].yVal).isEqualTo(0f)
    assertThat(rounded.vertices[4].xVal).isEqualTo(0f)
    assertThat(rounded.vertices[4].yVal).isEqualTo(100f)
    assertThat(rounded.vertices[5].xVal).isEqualTo(0f)
    assertThat(rounded.vertices[5].yVal).isEqualTo(100f)
    for (i in 0 until 6) {
      assertThat(rounded.inTangents[i].xVal).isEqualTo(0f)
      assertThat(rounded.inTangents[i].yVal).isEqualTo(0f)
      assertThat(rounded.outTangents[i].xVal).isEqualTo(0f)
      assertThat(rounded.outTangents[i].yVal).isEqualTo(0f)
    }
  }

  @Test
  fun roundBezierValue_clampsRadiusToHalfSegmentLength() {
    // Short edge of length 20: (0,0) to (20,0), then to (20,100) and (0,100)
    val rect =
      testBezier(
        closed = true,
        vertices = listOf(pt(0f, 0f), pt(20f, 0f), pt(20f, 100f), pt(0f, 100f)),
        inTangents = listOf(pt(0f, 0f), pt(0f, 0f), pt(0f, 0f), pt(0f, 0f)),
        outTangents = listOf(pt(0f, 0f), pt(0f, 0f), pt(0f, 0f), pt(0f, 0f)),
      )

    // Requested radius 50f is larger than half of length 20 (which is 10f)
    val rounded = roundBezierValue(rect, radius = 50f)
    assertThat(rounded.vertices).hasSize(8)

    // Corner 1 at (20,0): edge from (0,0) has length 20 -> max radius clamped to 10f
    // pStart at (10, 0), pEnd at (20, 10)
    val pStart1 = rounded.vertices[2]
    val pEnd1 = rounded.vertices[3]
    assertThat(pStart1.xVal).isWithin(0.01f).of(10f)
    assertThat(pStart1.yVal).isWithin(0.01f).of(0f)
    assertThat(pEnd1.xVal).isWithin(0.01f).of(20f)
    assertThat(pEnd1.yVal).isWithin(0.01f).of(10f)
  }

  @Test
  fun roundBezierValue_openPath_leavesEndpointsUnrounded() {
    // Open path: (0,0) -> (50,50) -> (100,0)
    val openPath =
      testBezier(
        closed = false,
        vertices = listOf(pt(0f, 0f), pt(50f, 50f), pt(100f, 0f)),
        inTangents = listOf(pt(0f, 0f), pt(0f, 0f), pt(0f, 0f)),
        outTangents = listOf(pt(0f, 0f), pt(0f, 0f), pt(0f, 0f)),
      )

    val rounded = roundBezierValue(openPath, radius = 10f)
    // 3 vertices -> vertex 0 unchanged (1), vertex 1 rounded (2), vertex 2 unchanged (1) = 4 total
    assertThat(rounded.closed.constantValue).isFalse()
    assertThat(rounded.vertices).hasSize(4)

    // First vertex is unrounded (0,0)
    assertThat(rounded.vertices[0].xVal).isWithin(0.01f).of(0f)
    assertThat(rounded.vertices[0].yVal).isWithin(0.01f).of(0f)

    // Last vertex is unrounded (100,0)
    assertThat(rounded.vertices[3].xVal).isWithin(0.01f).of(100f)
    assertThat(rounded.vertices[3].yVal).isWithin(0.01f).of(0f)
  }

  @Test
  fun roundBezierValue_smoothCurvesPreserved() {
    // Vertex with existing non-zero in/out tangents (smooth curve)
    val curved =
      testBezier(
        closed = true,
        vertices = listOf(pt(0f, 0f), pt(100f, 0f)),
        inTangents = listOf(pt(10f, 10f), pt(-10f, -10f)),
        outTangents = listOf(pt(-10f, -10f), pt(10f, 10f)),
      )

    val rounded = roundBezierValue(curved, radius = 10f)
    // Both vertices already curved, preserved without splitting
    assertThat(rounded.vertices).hasSize(2)
    assertThat(rounded.inTangents[0].xVal).isEqualTo(10f)
    assertThat(rounded.inTangents[0].yVal).isEqualTo(10f)
    assertThat(rounded.outTangents[0].xVal).isEqualTo(-10f)
    assertThat(rounded.outTangents[0].yVal).isEqualTo(-10f)
  }

  @Test
  fun evaluatePath_withRoundedCornersModifier() {
    val triangle =
      testBezier(
        closed = true,
        vertices = listOf(pt(0f, 0f), pt(100f, 0f), pt(0f, 100f)),
        inTangents = listOf(pt(0f, 0f), pt(0f, 0f), pt(0f, 0f)),
        outTangents = listOf(pt(0f, 0f), pt(0f, 0f), pt(0f, 0f)),
      )
    val pathGeometry = Path(shape = StaticBezierProperty(value = triangle))
    val roundedCorners = RoundedCorners(radius = StaticScalarProperty(value = 15f.rf))

    val result = path(pathGeometry, settings, roundedCorners = roundedCorners)
    assertThat(result).isNotNull()
    val dynamic = result as RemoteDynamicGeometry
    assertThat(dynamic.path).hasSize(1)
    val bezier = dynamic.path[0]
    assertThat(bezier.vertices).hasSize(6)
  }

  @Test
  fun evaluatePath_withAnimatedRoundedCorners() {
    val triangle =
      testBezier(
        closed = true,
        vertices = listOf(pt(0f, 0f), pt(100f, 0f), pt(0f, 100f)),
        inTangents = listOf(pt(0f, 0f), pt(0f, 0f), pt(0f, 0f)),
        outTangents = listOf(pt(0f, 0f), pt(0f, 0f), pt(0f, 0f)),
      )
    val pathGeometry = Path(shape = StaticBezierProperty(value = triangle))
    val animatedRadius =
      AnimatedScalarProperty(
        keyframes =
          listOf(
            ScalarPropertyKeyframe(frame = 0f.rf, value = 0f.rf),
            ScalarPropertyKeyframe(frame = 20f.rf, value = 20f.rf),
          )
      )
    val roundedCorners = RoundedCorners(radius = animatedRadius)

    // At frame 10 (halfway), radius is 10f
    val midSettings = LottieSettings(10f.rf, SlotMap.Empty)
    val result = path(pathGeometry, midSettings, roundedCorners = roundedCorners)
    assertThat(result).isNotNull()
    val dynamic = result as RemoteDynamicGeometry
    assertThat(dynamic.path).hasSize(1)
    val bezier = dynamic.path[0]
    assertThat(bezier.vertices).hasSize(6)
    // Corner 0 start point at (0, 10)
    assertThat(bezier.vertices[0].xVal).isWithin(0.01f).of(0f)
    assertThat(bezier.vertices[0].yVal).isWithin(0.01f).of(10f)
  }

  @Test
  fun gatherShapes_withRoundedCornersAndFill() {
    val triangle =
      testBezier(
        closed = true,
        vertices = listOf(pt(0f, 0f), pt(100f, 0f), pt(0f, 100f)),
        inTangents = listOf(pt(0f, 0f), pt(0f, 0f), pt(0f, 0f)),
        outTangents = listOf(pt(0f, 0f), pt(0f, 0f), pt(0f, 0f)),
      )
    val elements =
      listOf(
        Path(shape = StaticBezierProperty(value = triangle)),
        RoundedCorners(radius = StaticScalarProperty(value = 12f.rf)),
        Fill(
          color = StaticColorProperty(value = Color.Red.rc),
          opacity = StaticScalarProperty(value = 100f.rf),
        ),
      )

    val shapeGroups = gatherShapesForTest(elements, settings)
    assertThat(shapeGroups).hasSize(1)
    val group = shapeGroups[0]
    assertThat(group.shapes).hasSize(1)
    val lottiePath = group.shapes[0] as RemoteDynamicGeometry
    assertThat(lottiePath.path[0].vertices).hasSize(6)
  }

  @Test
  fun gatherShapes_withRoundedCornersAndTrimPath() {
    val triangle =
      testBezier(
        closed = true,
        vertices = listOf(pt(0f, 0f), pt(100f, 0f), pt(0f, 100f)),
        inTangents = listOf(pt(0f, 0f), pt(0f, 0f), pt(0f, 0f)),
        outTangents = listOf(pt(0f, 0f), pt(0f, 0f), pt(0f, 0f)),
      )
    val elements =
      listOf(
        Path(shape = StaticBezierProperty(value = triangle)),
        RoundedCorners(radius = StaticScalarProperty(value = 10f.rf)),
        TrimPath(
          start = StaticScalarProperty(value = 0f.rf),
          end = StaticScalarProperty(value = 50f.rf),
          offset = StaticScalarProperty(value = 0f.rf),
        ),
        Fill(
          color = StaticColorProperty(value = Color.Red.rc),
          opacity = StaticScalarProperty(value = 100f.rf),
        ),
      )

    val shapeGroups = gatherShapesForTest(elements, settings)
    assertThat(shapeGroups).hasSize(1)
    val group = shapeGroups[0]
    assertThat(group.shapes).hasSize(1)
    val lottiePath = group.shapes[0] as RemoteDynamicGeometry
    assertThat(lottiePath.path).isNotEmpty()
  }
}
