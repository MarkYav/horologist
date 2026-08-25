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
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.horologist.remotecompose.lottie.format.Animation
import com.google.android.horologist.remotecompose.lottie.format.layer.ShapeLayer
import com.google.android.horologist.remotecompose.lottie.format.layer.SolidColorLayer
import com.google.android.horologist.remotecompose.lottie.format.mask.MaskMode
import com.google.android.horologist.remotecompose.lottie.format.properties.AnimatedBezierProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticBezierProperty
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animateBezier
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SuppressLint("RestrictedApi")
@RunWith(AndroidJUnit4::class)
class MaskTest {

  @Test
  fun mask_deserialization_fromLottieJson_withAllProperties() {
    val json =
      """
      {
        "v": "5.9.6",
        "fr": 30.0,
        "ip": 0.0,
        "op": 60.0,
        "w": 300,
        "h": 300,
        "layers": [
          {
            "ty": 4,
            "nm": "MaskedShapeLayer",
            "ind": 1,
            "ip": 0.0,
            "op": 60.0,
            "masksProperties": [
              {
                "nm": "Mask 1 - Add",
                "mode": "a",
                "inv": false,
                "o": { "a": 0, "k": 100.0 },
                "x": { "a": 0, "k": 0.0 },
                "pt": {
                  "a": 0,
                  "k": {
                    "c": true,
                    "v": [[10.0, 10.0], [90.0, 10.0], [90.0, 90.0], [10.0, 90.0]],
                    "i": [[0.0, 0.0], [0.0, 0.0], [0.0, 0.0], [0.0, 0.0]],
                    "o": [[0.0, 0.0], [0.0, 0.0], [0.0, 0.0], [0.0, 0.0]]
                  }
                }
              },
              {
                "nm": "Mask 2 - Subtract Inverted",
                "mode": "s",
                "inv": true,
                "pt": {
                  "a": 0,
                  "k": {
                    "c": true,
                    "v": [[20.0, 20.0], [80.0, 20.0], [80.0, 80.0], [20.0, 80.0]],
                    "i": [[0.0, 0.0], [0.0, 0.0], [0.0, 0.0], [0.0, 0.0]],
                    "o": [[0.0, 0.0], [0.0, 0.0], [0.0, 0.0], [0.0, 0.0]]
                  }
                }
              },
              {
                "nm": "Mask 3 - Intersect",
                "mode": "i",
                "inv": false
              },
              {
                "nm": "Mask 4 - None",
                "mode": "n"
              }
            ],
            "shapes": []
          }
        ]
      }
      """
        .trimIndent()

    val animation = Animation.decodeFromString(json)
    assertThat(animation.layers).hasSize(1)

    val layer = animation.layers[0] as ShapeLayer
    assertThat(layer.masksProperties).hasSize(4)

    val mask1 = layer.masksProperties[0]
    assertThat(mask1.name).isEqualTo("Mask 1 - Add")
    assertThat(mask1.mode).isEqualTo(MaskMode.Add)
    assertThat(mask1.inverted).isFalse()
    assertThat(mask1.path).isInstanceOf(StaticBezierProperty::class.java)

    val mask2 = layer.masksProperties[1]
    assertThat(mask2.name).isEqualTo("Mask 2 - Subtract Inverted")
    assertThat(mask2.mode).isEqualTo(MaskMode.Subtract)
    assertThat(mask2.inverted).isTrue()

    val mask3 = layer.masksProperties[2]
    assertThat(mask3.mode).isEqualTo(MaskMode.Intersect)
    assertThat(mask3.inverted).isFalse()
    assertThat(mask3.path).isNull()

    val mask4 = layer.masksProperties[3]
    assertThat(mask4.mode).isEqualTo(MaskMode.None)
  }

  @Test
  fun mask_deserialization_defaults_handlesMissingOptionalFields() {
    val json =
      """
      {
        "v": "5.9.6",
        "fr": 30.0,
        "ip": 0.0,
        "op": 60.0,
        "w": 300,
        "h": 300,
        "layers": [
          {
            "ty": 1,
            "nm": "SolidLayer",
            "ind": 1,
            "sc": "#FF0000",
            "sw": 100,
            "sh": 100,
            "masksProperties": [
              {}
            ]
          }
        ]
      }
      """
        .trimIndent()

    val animation = Animation.decodeFromString(json)
    val layer = animation.layers[0] as SolidColorLayer
    assertThat(layer.masksProperties).hasSize(1)

    val defaultMask = layer.masksProperties[0]
    assertThat(defaultMask.mode).isEqualTo(MaskMode.Add)
    assertThat(defaultMask.inverted).isFalse()
    assertThat(defaultMask.path).isNull()
    assertThat(defaultMask.opacity).isNull()
    assertThat(defaultMask.expand).isNull()
  }

  @Test
  fun maskMode_deserialization_handlesKnownAndUnknownModes() {
    assertThat(MaskMode.fromValueOrNull("a")).isEqualTo(MaskMode.Add)
    assertThat(MaskMode.fromValueOrNull("s")).isEqualTo(MaskMode.Subtract)
    assertThat(MaskMode.fromValueOrNull("i")).isEqualTo(MaskMode.Intersect)
    assertThat(MaskMode.fromValueOrNull("l")).isEqualTo(MaskMode.Lighten)
    assertThat(MaskMode.fromValueOrNull("d")).isEqualTo(MaskMode.Darken)
    assertThat(MaskMode.fromValueOrNull("f")).isEqualTo(MaskMode.Difference)
    assertThat(MaskMode.fromValueOrNull("n")).isEqualTo(MaskMode.None)
    assertThat(MaskMode.fromValueOrNull("unknown_mode")).isNull()
  }

  @Test
  fun mask_animatedBezierPath_evaluatesAtTimelineFrame() {
    val json =
      """
      {
        "v": "5.9.6",
        "fr": 30.0,
        "ip": 0.0,
        "op": 60.0,
        "w": 300,
        "h": 300,
        "layers": [
          {
            "ty": 4,
            "nm": "AnimatedMaskLayer",
            "masksProperties": [
              {
                "nm": "Animated Mask",
                "mode": "a",
                "pt": {
                  "a": 1,
                  "k": [
                    {
                      "t": 0.0,
                      "s": [
                        {
                          "c": true,
                          "v": [[0.0, 0.0], [50.0, 0.0]],
                          "i": [[0.0, 0.0], [0.0, 0.0]],
                          "o": [[0.0, 0.0], [0.0, 0.0]]
                        }
                      ]
                    },
                    {
                      "t": 30.0,
                      "s": [
                        {
                          "c": true,
                          "v": [[0.0, 0.0], [100.0, 0.0]],
                          "i": [[0.0, 0.0], [0.0, 0.0]],
                          "o": [[0.0, 0.0], [0.0, 0.0]]
                        }
                      ]
                    }
                  ]
                }
              }
            ],
            "shapes": []
          }
        ]
      }
      """
        .trimIndent()

    val animation = Animation.decodeFromString(json)
    val layer = animation.layers[0] as ShapeLayer
    val mask = layer.masksProperties[0]
    assertThat(mask.path).isInstanceOf(AnimatedBezierProperty::class.java)

    val settingsFrame0 = LottieSettings(currentFrame = 0f.rf)
    val resultFrame0 = animateBezier(mask.path!!, settingsFrame0)
    assertThat(resultFrame0).hasSize(1)
    val verticesFrame0 = resultFrame0[0].vertices
    assertThat(verticesFrame0[1][0].constantValueOrNull).isWithin(0.01f).of(50f)
  }
}
