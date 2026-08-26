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

package com.google.android.horologist.remotecompose.lottie.format.properties

import android.annotation.SuppressLint
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.horologist.remotecompose.lottie.format.LottieDecoder
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SuppressLint("RestrictedApi")
@RunWith(AndroidJUnit4::class)
class LottiePropertyTest {

  private val json = LottieDecoder.json

  // =========================================================================
  // Scalar Property Tests
  // =========================================================================

  @Test
  fun scalar_static_primitiveNumber() {
    val prop = json.decodeFromString<BaseScalarProperty>("42.5")
    assertThat(prop).isInstanceOf(StaticScalarProperty::class.java)
    val staticProp = prop as StaticScalarProperty
    assertThat(staticProp.value).isEqualTo(42.5f)
    assertThat(staticProp.animated).isFalse()
  }

  @Test
  fun scalar_static_array() {
    val prop = json.decodeFromString<BaseScalarProperty>("[100.0]")
    assertThat(prop).isInstanceOf(StaticScalarProperty::class.java)
    assertThat((prop as StaticScalarProperty).value).isEqualTo(100f)
  }

  @Test
  fun scalar_static_objectWithSlotId() {
    val jsonStr = """{"a": 0, "k": 75.0, "sid": "opacity_slot"}"""
    val prop = json.decodeFromString<BaseScalarProperty>(jsonStr)
    assertThat(prop).isInstanceOf(StaticScalarProperty::class.java)
    val staticProp = prop as StaticScalarProperty
    assertThat(staticProp.value).isEqualTo(75f)
    assertThat(staticProp.slotId).isEqualTo("opacity_slot")
  }

  @Test
  fun scalar_animated_withKeyframes() {
    val jsonStr =
      """
      {
        "a": 1,
        "k": [
          {"t": 0, "s": 0.0, "h": 1},
          {"t": 30, "s": 100.0, "i": {"x": 0.4, "y": 0.4}, "o": {"x": 0.2, "y": 0.8}}
        ]
      }
      """
        .trimIndent()
    val prop = json.decodeFromString<BaseScalarProperty>(jsonStr)
    assertThat(prop).isInstanceOf(AnimatedScalarProperty::class.java)
    val animProp = prop as AnimatedScalarProperty
    assertThat(animProp.animated).isTrue()
    assertThat(animProp.keyframes).hasSize(2)
    assertThat(animProp.keyframes[0].frame).isEqualTo(0f)
    assertThat(animProp.keyframes[0].value).isEqualTo(0f)
    assertThat(animProp.keyframes[0].hold).isTrue()
    assertThat(animProp.keyframes[1].frame).isEqualTo(30f)
    assertThat(animProp.keyframes[1].value).isEqualTo(100f)
    assertThat(animProp.keyframes[1].inTangent?.x).isEqualTo(0.4f)
    assertThat(animProp.keyframes[1].outTangent?.y).isEqualTo(0.8f)
  }

  // =========================================================================
  // Color Property Tests
  // =========================================================================

  @Test
  fun color_static_hexString() {
    val prop = json.decodeFromString<BaseColorProperty>("\"#ff0000\"")
    assertThat(prop).isInstanceOf(StaticColorProperty::class.java)
    val color = (prop as StaticColorProperty).value.constantValueOrNull
    assertThat(color).isNotNull()
    assertThat(color!!.red).isEqualTo(1f)
    assertThat(color.green).isEqualTo(0f)
    assertThat(color.blue).isEqualTo(0f)
  }

  @Test
  fun color_static_floatArray() {
    val prop = json.decodeFromString<BaseColorProperty>("[0.0, 1.0, 0.0, 1.0]")
    assertThat(prop).isInstanceOf(StaticColorProperty::class.java)
    val color = (prop as StaticColorProperty).value.constantValueOrNull
    assertThat(color).isNotNull()
    assertThat(color!!.green).isEqualTo(1f)
  }

  @Test
  fun color_animated_withKeyframes() {
    val jsonStr =
      """
      {
        "a": 1,
        "k": [
          {"t": 0, "s": [1.0, 0.0, 0.0, 1.0]},
          {"t": 20, "s": [0.0, 0.0, 1.0, 1.0]}
        ]
      }
      """
        .trimIndent()
    val prop = json.decodeFromString<BaseColorProperty>(jsonStr)
    assertThat(prop).isInstanceOf(AnimatedColorProperty::class.java)
    val animProp = prop as AnimatedColorProperty
    assertThat(animProp.keyframes).hasSize(2)
    val startColor = animProp.keyframes[0].value.constantValueOrNull
    val endColor = animProp.keyframes[1].value.constantValueOrNull
    assertThat(startColor?.red).isEqualTo(1f)
    assertThat(endColor?.blue).isEqualTo(1f)
  }

  // =========================================================================
  // Vector Property Tests
  // =========================================================================

  @Test
  fun vector_static_array() {
    val prop = json.decodeFromString<BaseVectorProperty>("[120.0, 80.0]")
    assertThat(prop).isInstanceOf(StaticVectorProperty::class.java)
    val staticProp = prop as StaticVectorProperty
    assertThat(staticProp.value).containsExactly(120f, 80f).inOrder()
  }

  @Test
  fun vector_animated_withKeyframes() {
    val jsonStr =
      """
      {
        "a": 1,
        "k": [
          {"t": 0, "s": [10.0, 10.0]},
          {"t": 15, "s": [50.0, 50.0]}
        ]
      }
      """
        .trimIndent()
    val prop = json.decodeFromString<BaseVectorProperty>(jsonStr)
    assertThat(prop).isInstanceOf(AnimatedVectorProperty::class.java)
    val animProp = prop as AnimatedVectorProperty
    assertThat(animProp.keyframes).hasSize(2)
    assertThat(animProp.keyframes[1].value).containsExactly(50f, 50f).inOrder()
  }

  // =========================================================================
  // Position Property Tests
  // =========================================================================

  @Test
  fun position_static_array() {
    val prop = json.decodeFromString<BasePositionProperty>("[150.0, 250.0]")
    assertThat(prop).isInstanceOf(StaticPositionProperty::class.java)
    val staticProp = prop as StaticPositionProperty
    assertThat(staticProp.value).containsExactly(150f, 250f).inOrder()
  }

  @Test
  fun position_animated_withSpatialTangents() {
    val jsonStr =
      """
      {
        "a": 1,
        "k": [
          {"t": 0, "s": [0.0, 0.0], "to": [10.0, 20.0], "ti": [-10.0, -20.0]},
          {"t": 30, "s": [100.0, 200.0]}
        ]
      }
      """
        .trimIndent()
    val prop = json.decodeFromString<BasePositionProperty>(jsonStr)
    assertThat(prop).isInstanceOf(AnimatedPositionProperty::class.java)
    val animProp = prop as AnimatedPositionProperty
    assertThat(animProp.keyframes).hasSize(2)
    assertThat(animProp.keyframes[0].spatialOutTangent).containsExactly(10f, 20f).inOrder()
    assertThat(animProp.keyframes[0].spatialInTangent).containsExactly(-10f, -20f).inOrder()
  }

  @Test
  fun position_split_withSeparateAxes() {
    val jsonStr =
      """
      {
        "s": true,
        "x": 45.0,
        "y": {
          "a": 1,
          "k": [
            {"t": 0, "s": 0.0},
            {"t": 20, "s": 80.0}
          ]
        }
      }
      """
        .trimIndent()
    val prop = json.decodeFromString<BasePositionProperty>(jsonStr)
    assertThat(prop).isInstanceOf(SplitPositionProperty::class.java)
    val splitProp = prop as SplitPositionProperty
    assertThat(splitProp.split).isTrue()
    assertThat((splitProp.x as StaticScalarProperty).value).isEqualTo(45f)
    assertThat(splitProp.y).isInstanceOf(AnimatedScalarProperty::class.java)
    assertThat(splitProp.animated).isTrue()
  }

  // =========================================================================
  // Gradient Property Tests
  // =========================================================================

  @Test
  fun gradient_static_withColorCount() {
    val jsonStr =
      """
      {
        "p": 2,
        "k": [0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0]
      }
      """
        .trimIndent()
    val prop = json.decodeFromString<BaseGradientProperty>(jsonStr)
    assertThat(prop).isInstanceOf(StaticGradientProperty::class.java)
    val staticProp = prop as StaticGradientProperty
    assertThat(staticProp.value.numberOfColors).isEqualTo(2)
    assertThat(staticProp.value.values).hasSize(8)
  }

  @Test
  fun gradient_animated_withKeyframes() {
    val jsonStr =
      """
      {
        "p": 2,
        "a": 1,
        "k": [
          {"t": 0, "s": [0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0]},
          {"t": 10, "s": [0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 1.0, 0.0]}
        ]
      }
      """
        .trimIndent()
    val prop = json.decodeFromString<BaseGradientProperty>(jsonStr)
    assertThat(prop).isInstanceOf(AnimatedGradientProperty::class.java)
    val animProp = prop as AnimatedGradientProperty
    assertThat(animProp.numberOfColors).isEqualTo(2)
    assertThat(animProp.keyframes).hasSize(2)
  }

  // =========================================================================
  // Bezier Property Tests
  // =========================================================================

  @Test
  fun bezier_static_object() {
    val jsonStr =
      """
      {
        "c": true,
        "v": [[0.0, 0.0], [10.0, 10.0]],
        "i": [[0.0, 0.0], [-2.0, -2.0]],
        "o": [[2.0, 2.0], [0.0, 0.0]]
      }
      """
        .trimIndent()
    val prop = json.decodeFromString<BaseBezierProperty>(jsonStr)
    assertThat(prop).isInstanceOf(StaticBezierProperty::class.java)
    val staticProp = prop as StaticBezierProperty
    assertThat(staticProp.value.closed).isTrue()
    assertThat(staticProp.value.vertices).hasSize(2)
  }

  @Test
  fun bezier_animated_withKeyframes() {
    val jsonStr =
      """
      {
        "a": 1,
        "k": [
          {
            "t": 0,
            "s": [
              {
                "c": true,
                "v": [[0.0, 0.0], [20.0, 20.0]],
                "i": [[0.0, 0.0], [0.0, 0.0]],
                "o": [[0.0, 0.0], [0.0, 0.0]]
              }
            ]
          }
        ]
      }
      """
        .trimIndent()
    val prop = json.decodeFromString<BaseBezierProperty>(jsonStr)
    assertThat(prop).isInstanceOf(AnimatedBezierProperty::class.java)
    val animProp = prop as AnimatedBezierProperty
    assertThat(animProp.keyframes).hasSize(1)
    assertThat(animProp.keyframes[0].value).hasSize(1)
    assertThat(animProp.keyframes[0].value[0].vertices).hasSize(2)
  }
}
