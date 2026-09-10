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
import androidx.compose.remote.creation.compose.state.rf
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.PolyStar
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.PolyStarType
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.grouping.Transform
import com.google.android.horologist.remotecompose.lottie.format.properties.AnimatedScalarProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.ScalarPropertyKeyframe
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticPositionProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticScalarProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticVectorProperty
import com.google.android.horologist.remotecompose.lottie.format.values.Point
import com.google.android.horologist.remotecompose.lottie.renderer.RemoteCompiledGeometry
import com.google.android.horologist.remotecompose.lottie.renderer.RemoteDynamicGeometry
import com.google.android.horologist.remotecompose.lottie.renderer.RemoteShape
import com.google.android.horologist.remotecompose.lottie.renderer.modifiers.transformRepeaterShape
import com.google.android.horologist.remotecompose.lottie.renderer.properties.RemoteBezierValue
import com.google.android.horologist.remotecompose.lottie.renderer.shapes.polyStar
import com.google.common.truth.Truth.assertThat
import kotlin.math.cos
import kotlin.math.sin
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SuppressLint("RestrictedApi")
class PolyStarTest {

  private val settings = LottieSettings(0f.rf, SlotMap.Empty)

  private fun evaluatePolyStar(star: PolyStar, settings: LottieSettings): RemoteShape? =
    polyStar(star, settings)

  private fun transformRemoteShape(
    shape: RemoteShape,
    transform: Transform,
    settings: LottieSettings,
  ): RemoteShape = transformRepeaterShape(shape, transform, 1f, settings)

  private fun RemoteShape.firstSubpath(): RemoteBezierValue =
    when (this) {
      is RemoteCompiledGeometry -> bezierSubpaths?.first()!!
      is RemoteDynamicGeometry -> path.first()
      else -> error("Unexpected shape type: ${this::class.java}")
    }

  private val Point.xVal: Float
    get() = x.constantValueOrNull ?: 0f

  private val Point.yVal: Float
    get() = y.constantValueOrNull ?: 0f

  @Test
  fun evaluateStaticStarWithZeroRoundedness() {
    val star =
      PolyStar(
        starType = PolyStarType.Star,
        points = StaticScalarProperty(value = 5f.rf),
        position = StaticPositionProperty(value = Point(100f.rf, 100f.rf)),
        rotation = StaticScalarProperty(value = 0f.rf),
        innerRadius = StaticScalarProperty(value = 40f.rf),
        outerRadius = StaticScalarProperty(value = 100f.rf),
        innerRoundness = StaticScalarProperty(value = 0f.rf),
        outerRoundness = StaticScalarProperty(value = 0f.rf),
      )

    val result = evaluatePolyStar(star, settings)
    assertThat(result).isNotNull()

    val subpath = result!!.firstSubpath()
    assertThat(subpath.closed).isTrue()
    assertThat(subpath.vertices).hasSize(10)
    assertThat(subpath.inTangents).hasSize(10)
    assertThat(subpath.outTangents).hasSize(10)

    // First vertex (outer point at angle = rotation - 90 = -90 deg): (0, -100) + pos (100, 100) =
    // (100, 0)
    assertThat(subpath.vertices[0].xVal).isWithin(0.01f).of(100f)
    assertThat(subpath.vertices[0].yVal).isWithin(0.01f).of(0f)

    // Second vertex (inner point at angle = -90 + 36 = -54 deg): (40 * cos(-54°), 40 * sin(-54°)) +
    // (100, 100)
    val rad54 = Math.toRadians(-54.0)
    val expectedX1 = (40.0 * cos(rad54)).toFloat() + 100f
    val expectedY1 = (40.0 * sin(rad54)).toFloat() + 100f
    assertThat(subpath.vertices[1].xVal).isWithin(0.01f).of(expectedX1)
    assertThat(subpath.vertices[1].yVal).isWithin(0.01f).of(expectedY1)

    // Zero roundedness means zero tangents
    for (i in 0 until 10) {
      assertThat(subpath.inTangents[i].xVal).isWithin(0.001f).of(0f)
      assertThat(subpath.inTangents[i].yVal).isWithin(0.001f).of(0f)
      assertThat(subpath.outTangents[i].xVal).isWithin(0.001f).of(0f)
      assertThat(subpath.outTangents[i].yVal).isWithin(0.001f).of(0f)
    }
  }

  @Test
  fun evaluateStarWithRoundedness() {
    val star =
      PolyStar(
        starType = PolyStarType.Star,
        points = StaticScalarProperty(value = 5f.rf),
        position = StaticPositionProperty(value = Point(0f.rf, 0f.rf)),
        rotation = StaticScalarProperty(value = 0f.rf),
        innerRadius = StaticScalarProperty(value = 50f.rf),
        outerRadius = StaticScalarProperty(value = 100f.rf),
        innerRoundness = StaticScalarProperty(value = 20f.rf),
        outerRoundness = StaticScalarProperty(value = 30f.rf),
      )

    val result = evaluatePolyStar(star, settings)
    assertThat(result).isNotNull()
    val subpath = result!!.firstSubpath()

    // With non-zero roundedness, control point tangents are generated
    var hasNonZeroTangents = false
    for (i in 0 until 10) {
      val inX = subpath.inTangents[i].xVal
      val inY = subpath.inTangents[i].yVal
      val outX = subpath.outTangents[i].xVal
      val outY = subpath.outTangents[i].yVal
      if (inX != 0f || inY != 0f || outX != 0f || outY != 0f) {
        hasNonZeroTangents = true
      }
    }
    assertThat(hasNonZeroTangents).isTrue()
  }

  @Test
  fun evaluateStaticPolygonWithZeroRoundedness() {
    val polygon =
      PolyStar(
        starType = PolyStarType.Polygon,
        points = StaticScalarProperty(value = 6f.rf),
        position = StaticPositionProperty(value = Point(50f.rf, 50f.rf)),
        rotation = StaticScalarProperty(value = 0f.rf),
        outerRadius = StaticScalarProperty(value = 60f.rf),
        outerRoundness = StaticScalarProperty(value = 0f.rf),
      )

    val result = evaluatePolyStar(polygon, settings)
    assertThat(result).isNotNull()

    val subpath = result!!.firstSubpath()
    assertThat(subpath.closed).isTrue()
    assertThat(subpath.vertices).hasSize(6)
    assertThat(subpath.inTangents).hasSize(6)
    assertThat(subpath.outTangents).hasSize(6)

    // First vertex (at angle = -90 deg): (0, -60) + pos (50, 50) = (50, -10)
    assertThat(subpath.vertices[0].xVal).isWithin(0.01f).of(50f)
    assertThat(subpath.vertices[0].yVal).isWithin(0.01f).of(-10f)

    // All tangents should be zero for zero roundedness
    for (i in 0 until 6) {
      assertThat(subpath.inTangents[i].xVal).isWithin(0.001f).of(0f)
      assertThat(subpath.inTangents[i].yVal).isWithin(0.001f).of(0f)
      assertThat(subpath.outTangents[i].xVal).isWithin(0.001f).of(0f)
      assertThat(subpath.outTangents[i].yVal).isWithin(0.001f).of(0f)
    }
  }

  @Test
  fun evaluatePolygonWithRoundedness() {
    val polygon =
      PolyStar(
        starType = PolyStarType.Polygon,
        points = StaticScalarProperty(value = 4f.rf),
        position = StaticPositionProperty(value = Point(0f.rf, 0f.rf)),
        rotation = StaticScalarProperty(value = 45f.rf),
        outerRadius = StaticScalarProperty(value = 80f.rf),
        outerRoundness = StaticScalarProperty(value = 15f.rf),
      )

    val result = evaluatePolyStar(polygon, settings)
    assertThat(result).isNotNull()
    val subpath = result!!.firstSubpath()
    assertThat(subpath.vertices).hasSize(4)

    var hasNonZeroTangents = false
    for (i in 0 until 4) {
      val inX = subpath.inTangents[i].xVal
      val inY = subpath.inTangents[i].yVal
      if (inX != 0f || inY != 0f) {
        hasNonZeroTangents = true
      }
    }
    assertThat(hasNonZeroTangents).isTrue()
  }

  @Test
  fun evaluateAnimatedPolyStar() {
    val star =
      PolyStar(
        starType = PolyStarType.Star,
        points = StaticScalarProperty(value = 5f.rf),
        position = StaticPositionProperty(value = Point(0f.rf, 0f.rf)),
        rotation =
          AnimatedScalarProperty(
            keyframes =
              listOf(
                ScalarPropertyKeyframe(frame = 0f.rf, value = 0f.rf),
                ScalarPropertyKeyframe(frame = 30f.rf, value = 180f.rf),
              )
          ),
        innerRadius = StaticScalarProperty(value = 30f.rf),
        outerRadius = StaticScalarProperty(value = 70f.rf),
        innerRoundness = StaticScalarProperty(value = 0f.rf),
        outerRoundness = StaticScalarProperty(value = 0f.rf),
      )

    val result = evaluatePolyStar(star, settings)
    assertThat(result).isNotNull()
    val subpath = result!!.firstSubpath()
    assertThat(subpath.vertices).hasSize(10)
  }

  @Test
  fun evaluateHiddenPolyStarReturnsNull() {
    val star =
      PolyStar(
        hidden = true.rb,
        starType = PolyStarType.Star,
        points = StaticScalarProperty(value = 5f.rf),
        position = StaticPositionProperty(value = Point(0f.rf, 0f.rf)),
        rotation = StaticScalarProperty(value = 0f.rf),
        innerRadius = StaticScalarProperty(value = 20f.rf),
        outerRadius = StaticScalarProperty(value = 50f.rf),
        outerRoundness = StaticScalarProperty(value = 0f.rf),
      )

    val result = evaluatePolyStar(star, settings)
    assertThat(result).isNull()
  }

  @Test
  fun polyStarTransformsWithGroupGeometryTransform() {
    val star =
      PolyStar(
        starType = PolyStarType.Star,
        points = StaticScalarProperty(value = 5f.rf),
        position = StaticPositionProperty(value = Point(0f.rf, 0f.rf)),
        rotation = StaticScalarProperty(value = 0f.rf),
        innerRadius = StaticScalarProperty(value = 50f.rf),
        outerRadius = StaticScalarProperty(value = 100f.rf),
        innerRoundness = StaticScalarProperty(value = 0f.rf),
        outerRoundness = StaticScalarProperty(value = 0f.rf),
      )

    val shape = evaluatePolyStar(star, settings)
    assertThat(shape).isNotNull()

    val transform =
      Transform(
        anchorPoint = StaticPositionProperty(value = Point(0f.rf, 0f.rf)),
        positionTranslation = StaticPositionProperty(value = Point(50f.rf, 50f.rf)),
        scale = StaticVectorProperty(animated = false.rb, value = listOf(200f.rf, 200f.rf)),
        rotation = StaticScalarProperty(value = 0f.rf),
        opacity = StaticScalarProperty(value = 100f.rf),
      )

    val transformedShape = transformRemoteShape(shape!!, transform, settings)
    val subpath = transformedShape.firstSubpath()

    // Original vertex 0 was (0, -100).
    // Scaled by 2.0 -> (0, -200) + translated by (50, 50) -> (50, -150)
    assertThat(subpath.vertices[0].xVal).isWithin(0.01f).of(50f)
    assertThat(subpath.vertices[0].yVal).isWithin(0.01f).of(-150f)
  }
}
