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
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.ui.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.horologist.remotecompose.lottie.format.Animation
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.PolyStar
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.PolyStarType
import com.google.android.horologist.remotecompose.lottie.format.properties.AnimatedPositionProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.KeyframeEasing
import com.google.android.horologist.remotecompose.lottie.format.properties.PositionPropertyKeyframe
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticScalarProperty
import com.google.android.horologist.remotecompose.lottie.renderer.layers.parseHexColor
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animatePosition
import com.google.android.horologist.remotecompose.lottie.renderer.shapes.evaluatePolyStar
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Pairwise Combinatorial Boundary & Singularity Test Suite.
 *
 * Enforces rigorous functional validation across interacting boundary pairs derived from the Lottie
 * 1.0.1 specification and internal specification requirements.
 */
@SuppressLint("RestrictedApi")
@RunWith(AndroidJUnit4::class)
class PairwiseBoundaryTest {

  private val emptySlotMap = SlotMap.Empty

  @Test
  fun evaluateKeyframeEasing_withAnisotropicDimensionsAndSpatialTangents_computesCurvedTrajectory() {
    val keyframe =
      PositionPropertyKeyframe(
        frame = 0f,
        value = listOf(0f, 0f),
        spatialInTangent = listOf(0f, 50f),
        spatialOutTangent = listOf(0f, 50f),
        outTangent = KeyframeEasing(x = listOf(0.0f, 0.4f), y = listOf(0.0f, 0.0f)),
        inTangent = KeyframeEasing(x = listOf(1.0f, 0.6f), y = listOf(1.0f, 1.0f)),
      )
    val keyframeEnd = PositionPropertyKeyframe(frame = 10f, value = listOf(100f, 0f))

    val animPosition = AnimatedPositionProperty(keyframes = listOf(keyframe, keyframeEnd))

    val settingsStart = LottieSettings(currentFrame = 0f.rf, slotMap = emptySlotMap)
    val posStart = animatePosition(animPosition, settingsStart)
    assertThat(posStart.x.constantValueOrNull).isWithin(0.01f).of(0f)
    assertThat(posStart.y.constantValueOrNull).isWithin(0.01f).of(0f)

    val settingsMid = LottieSettings(currentFrame = 5f.rf, slotMap = emptySlotMap)
    val posMid = animatePosition(animPosition, settingsMid)
    // Linear X reaches 50%, Spatial curved Y evaluates along bezier trajectory
    assertThat(posMid.x.constantValueOrNull).isWithin(0.01f).of(50f)
    assertThat(posMid.y.constantValueOrNull).isWithin(0.01f).of(37.5f)

    val settingsEnd = LottieSettings(currentFrame = 10f.rf, slotMap = emptySlotMap)
    val posEnd = animatePosition(animPosition, settingsEnd)
    assertThat(posEnd.x.constantValueOrNull).isWithin(0.01f).of(100f)
    assertThat(posEnd.y.constantValueOrNull).isWithin(0.01f).of(0f)
  }

  @Test
  fun evaluatePolyStar_withNanPoints_fallsBackGracefullyWithoutCrash() {
    val star =
      PolyStar(
        starType = PolyStarType.Star,
        points = StaticScalarProperty(value = Float.NaN),
        outerRadius = StaticScalarProperty(value = 50f),
        innerRadius = StaticScalarProperty(value = 20f),
      )

    val settings = LottieSettings(currentFrame = 0f.rf, slotMap = emptySlotMap)
    val remotePath = evaluatePolyStar(star, settings)
    if (remotePath != null) {
      assertThat(remotePath.path.first().vertices).isEmpty()
    }
  }

  @Test
  fun evaluateHexColorParsing_allSupportedFormats() {
    // 6-digit RGB with and without hash
    assertThat(parseHexColor("#FF5722")).isEqualTo(Color(0xFFFF5722))
    assertThat(parseHexColor("FF5722")).isEqualTo(Color(0xFFFF5722))

    // 8-digit ARGB with and without hash
    assertThat(parseHexColor("#80FF5722")).isEqualTo(Color(0x80FF5722))
    assertThat(parseHexColor("80FF5722")).isEqualTo(Color(0x80FF5722))

    // 3-digit RGB with and without hash
    assertThat(parseHexColor("#F00")).isEqualTo(Color(0xFFFF0000))
    assertThat(parseHexColor("F00")).isEqualTo(Color(0xFFFF0000))

    // 4-digit ARGB with and without hash
    assertThat(parseHexColor("#8F00")).isEqualTo(Color(0x88FF0000))
    assertThat(parseHexColor("8F00")).isEqualTo(Color(0x88FF0000))

    // Invalid formats return transparent fallback
    assertThat(parseHexColor("")).isEqualTo(Color.Transparent)
    assertThat(parseHexColor("invalid")).isEqualTo(Color.Transparent)
    assertThat(parseHexColor("#GGGGGG")).isEqualTo(Color.Transparent)
    assertThat(parseHexColor("#12345")).isEqualTo(Color.Transparent)
  }

  @Test
  fun malformedJsonFragments_withUnknownProperties_decodeSafely() {
    val json =
      """
      {
        "v": "5.9.6",
        "fr": 60,
        "ip": 0,
        "op": 120,
        "w": 300,
        "h": 300,
        "unknown_root_prop": {"foo": "bar"},
        "layers": [
          {
            "ty": 4,
            "nm": "ShapeLayer",
            "unknown_layer_prop": [1, 2, 3],
            "shapes": [
              {
                "ty": "sh",
                "nm": "Path1",
                "unknown_shape_meta": "test"
              }
            ]
          }
        ]
      }
      """
        .trimIndent()

    val animation = Animation.decodeFromString(json)
    assertThat(animation.layers).hasSize(1)
  }
}
