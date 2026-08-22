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

import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.ui.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.horologist.remotecompose.lottie.format.Animation
import com.google.android.horologist.remotecompose.lottie.format.GraphicElement
import com.google.android.horologist.remotecompose.lottie.format.Layer
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticColorProperty
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LottieDecoderResilienceTest {

  @Test
  fun unknownLayerType_deserializesAsNullLayerFallback() {
    val json =
      """
      {
        "v": "5.7.0",
        "fr": 60,
        "ip": 0,
        "op": 60,
        "w": 100,
        "h": 100,
        "layers": [
          { "ty": 999, "nm": "UnsupportedAudioLayer", "ind": 1 },
          { "ty": 4, "nm": "ValidShapeLayer", "ind": 2, "shapes": [] }
        ]
      }
      """
        .trimIndent()

    val animation = Animation.decodeFromString(json)

    assertThat(animation.layers).hasSize(2)
    assertThat(animation.layers[0]).isInstanceOf(Layer.NullLayer::class.java)
    assertThat(animation.layers[0].name).isEqualTo("UnsupportedAudioLayer")
    assertThat(animation.layers[1]).isInstanceOf(Layer.ShapeLayer::class.java)
  }

  @Test
  fun unknownShapeType_deserializesAsGroupFallback() {
    val json =
      """
      {
        "v": "5.7.0",
        "fr": 30,
        "ip": 0,
        "op": 30,
        "w": 50,
        "h": 50,
        "layers": [
          {
            "ty": 4,
            "nm": "ShapeLayer",
            "shapes": [
              { "ty": "unknown_shape_type", "nm": "CustomShape", "it": [] },
              {
                "ty": "fl",
                "nm": "RedFill",
                "c": { "k": [1.0, 0.0, 0.0, 1.0] }
              }
            ]
          }
        ]
      }
      """
        .trimIndent()

    val animation = Animation.decodeFromString(json)

    val shapeLayer = animation.layers[0] as Layer.ShapeLayer
    assertThat(shapeLayer.shapes).hasSize(2)
    assertThat(shapeLayer.shapes[0]).isInstanceOf(GraphicElement.Group::class.java)
    assertThat(shapeLayer.shapes[1]).isInstanceOf(GraphicElement.Fill::class.java)
  }

  @Test
  fun colorProperty_handlesRgbRgbaAndScaledIntegers() {
    val json =
      """
      {
        "v": "5.7.0",
        "fr": 30,
        "ip": 0,
        "op": 30,
        "w": 50,
        "h": 50,
        "layers": [
          {
            "ty": 4,
            "nm": "ShapeLayer",
            "shapes": [
              {
                "ty": "fl",
                "nm": "RgbFill",
                "c": { "k": [1.0, 0.5, 0.0] }
              },
              {
                "ty": "fl",
                "nm": "ScaledIntFill",
                "c": { "k": [255, 128, 0, 255] }
              }
            ]
          }
        ]
      }
      """
        .trimIndent()

    val animation = Animation.decodeFromString(json)

    val shapeLayer = animation.layers[0] as Layer.ShapeLayer
    val fill1 = shapeLayer.shapes[0] as GraphicElement.Fill
    val fill2 = shapeLayer.shapes[1] as GraphicElement.Fill

    assertThat((fill1.color as StaticColorProperty).value).isNotNull()
    assertThat((fill2.color as StaticColorProperty).value).isNotNull()
  }

  @Test
  fun colorProperty_parsesSlotId() {
    val json =
      """
      {
        "v": "5.7.0",
        "fr": 30,
        "ip": 0,
        "op": 30,
        "w": 50,
        "h": 50,
        "layers": [
          {
            "ty": 4,
            "nm": "ShapeLayer",
            "shapes": [
              {
                "ty": "fl",
                "nm": "SidFill",
                "c": { "sid": "color.primary", "k": [1.0, 0.0, 0.0, 1.0] }
              },
              {
                "ty": "fl",
                "nm": "DefaultFill",
                "c": { "k": [0.0, 1.0, 0.0, 1.0] }
              }
            ]
          }
        ]
      }
      """
        .trimIndent()

    val animation = Animation.decodeFromString(json)

    val shapeLayer = animation.layers[0] as Layer.ShapeLayer
    val fill1 = shapeLayer.shapes[0] as GraphicElement.Fill
    val fill2 = shapeLayer.shapes[1] as GraphicElement.Fill

    assertThat(fill1.color.slotId).isEqualTo("color.primary")
    assertThat(fill2.color.slotId).isNull()

    val slotMap = SlotMap(mapOf("color.primary" to RemoteColor(Color.Green)))
    assertThat(slotMap.getColor("color.primary")).isNotNull()
    assertThat(slotMap.getColor("unknown")).isNull()
  }

  @Test
  fun colorProperty_parsesHexColorStrings() {
    val json =
      """
      {
        "v": "5.7.0",
        "fr": 30,
        "ip": 0,
        "op": 30,
        "w": 50,
        "h": 50,
        "layers": [
          {
            "ty": 4,
            "nm": "ShapeLayer",
            "shapes": [
              {
                "ty": "fl",
                "nm": "Hex6Fill",
                "c": { "k": "#FF8000" }
              },
              {
                "ty": "fl",
                "nm": "Hex8Fill",
                "c": { "k": "#80FF8000" }
              },
              {
                "ty": "fl",
                "nm": "Hex3Fill",
                "c": { "k": "#F80" }
              },
              {
                "ty": "fl",
                "nm": "Hex4Fill",
                "c": { "k": "#8F80" }
              }
            ]
          }
        ]
      }
      """
        .trimIndent()

    val animation = Animation.decodeFromString(json)

    val shapeLayer = animation.layers[0] as Layer.ShapeLayer
    assertThat(shapeLayer.shapes).hasSize(4)
    val fill1 = shapeLayer.shapes[0] as GraphicElement.Fill
    val fill2 = shapeLayer.shapes[1] as GraphicElement.Fill
    val fill3 = shapeLayer.shapes[2] as GraphicElement.Fill
    val fill4 = shapeLayer.shapes[3] as GraphicElement.Fill

    assertThat((fill1.color as StaticColorProperty).value).isNotNull()
    assertThat((fill2.color as StaticColorProperty).value).isNotNull()
    assertThat((fill3.color as StaticColorProperty).value).isNotNull()
    assertThat((fill4.color as StaticColorProperty).value).isNotNull()
  }

  @Test
  fun colorProperty_4ComponentFloat_preservesAlpha() {
    val json =
      """
      {
        "v": "5.7.0",
        "fr": 30,
        "ip": 0,
        "op": 30,
        "w": 50,
        "h": 50,
        "layers": [
          {
            "ty": 4,
            "nm": "ShapeLayer",
            "shapes": [
              {
                "ty": "fl",
                "nm": "AlphaFill",
                "c": { "k": [1.0, 0.0, 0.0, 0.5] }
              }
            ]
          }
        ]
      }
      """
        .trimIndent()

    val animation = Animation.decodeFromString(json)

    val shapeLayer = animation.layers[0] as Layer.ShapeLayer
    val fill = shapeLayer.shapes[0] as GraphicElement.Fill
    assertThat((fill.color as StaticColorProperty).value).isNotNull()
  }

  @Test
  fun colorProperty_animatedColor_parsesKeyframes() {
    val json =
      """
      {
        "v": "5.7.0",
        "fr": 30,
        "ip": 0,
        "op": 30,
        "w": 50,
        "h": 50,
        "layers": [
          {
            "ty": 4,
            "nm": "ShapeLayer",
            "shapes": [
              {
                "ty": "fl",
                "nm": "AnimatedColorFill",
                "c": {
                  "a": 1,
                  "k": [
                    {
                      "t": 0,
                      "s": [1.0, 0.0, 0.0, 1.0],
                      "i": { "x": 0.5, "y": 1.0 },
                      "o": { "x": 0.5, "y": 0.0 }
                    },
                    {
                      "t": 30,
                      "s": [0.0, 0.0, 1.0, 1.0]
                    }
                  ]
                }
              }
            ]
          }
        ]
      }
      """
        .trimIndent()

    val animation = Animation.decodeFromString(json)

    val shapeLayer = animation.layers[0] as Layer.ShapeLayer
    val fill = shapeLayer.shapes[0] as GraphicElement.Fill
    assertThat(fill.color.animated).isTrue()
  }

  @Test
  fun bezierProperty_handlesBooleanAndIntClosedFlag() {
    val json =
      """
      {
        "v": "5.7.0",
        "fr": 30,
        "ip": 0,
        "op": 30,
        "w": 50,
        "h": 50,
        "layers": [
          {
            "ty": 4,
            "nm": "ShapeLayer",
            "shapes": [
              {
                "ty": "sh",
                "nm": "IntClosedPath",
                "ks": {
                  "a": 0,
                  "k": {
                    "c": 1,
                    "i": [[0.0, 0.0], [0.0, 0.0]],
                    "o": [[0.0, 0.0], [0.0, 0.0]],
                    "v": [[10.0, 10.0], [20.0, 20.0]]
                  }
                }
              },
              {
                "ty": "sh",
                "nm": "BoolClosedPath",
                "ks": {
                  "a": 0,
                  "k": {
                    "c": false,
                    "i": [[0.0, 0.0], [0.0, 0.0]],
                    "o": [[0.0, 0.0], [0.0, 0.0]],
                    "v": [[10.0, 10.0], [20.0, 20.0]]
                  }
                }
              }
            ]
          }
        ]
      }
      """
        .trimIndent()

    val animation = Animation.decodeFromString(json)

    val shapeLayer = animation.layers[0] as Layer.ShapeLayer
    val path1 = shapeLayer.shapes[0] as GraphicElement.Path
    val path2 = shapeLayer.shapes[1] as GraphicElement.Path

    assertThat(path1.shape).isNotNull()
    assertThat(path2.shape).isNotNull()
  }

  @Test
  fun bezierProperty_handlesSingleObjectAndArrayKeyframeValue() {
    val json =
      """
      {
        "v": "5.7.0",
        "fr": 30,
        "ip": 0,
        "op": 30,
        "w": 50,
        "h": 50,
        "layers": [
          {
            "ty": 4,
            "nm": "ShapeLayer",
            "shapes": [
              {
                "ty": "sh",
                "nm": "ArrayKeyframePath",
                "ks": {
                  "a": 1,
                  "k": [
                    {
                      "t": 0,
                      "s": [
                        {
                          "c": true,
                          "i": [[0.0, 0.0]],
                          "o": [[0.0, 0.0]],
                          "v": [[0.0, 0.0]]
                        }
                      ]
                    },
                    {
                      "t": 30,
                      "s": [
                        {
                          "c": true,
                          "i": [[0.0, 0.0]],
                          "o": [[0.0, 0.0]],
                          "v": [[10.0, 10.0]]
                        }
                      ]
                    }
                  ]
                }
              },
              {
                "ty": "sh",
                "nm": "SingleObjectKeyframePath",
                "ks": {
                  "a": 1,
                  "k": [
                    {
                      "t": 0,
                      "s": {
                        "c": 0,
                        "i": [[0.0, 0.0]],
                        "o": [[0.0, 0.0]],
                        "v": [[0.0, 0.0]]
                      }
                    },
                    {
                      "t": 30,
                      "s": {
                        "c": 0,
                        "i": [[0.0, 0.0]],
                        "o": [[0.0, 0.0]],
                        "v": [[10.0, 10.0]]
                      }
                    }
                  ]
                }
              }
            ]
          }
        ]
      }
      """
        .trimIndent()

    val animation = Animation.decodeFromString(json)

    val shapeLayer = animation.layers[0] as Layer.ShapeLayer
    val path1 = shapeLayer.shapes[0] as GraphicElement.Path
    val path2 = shapeLayer.shapes[1] as GraphicElement.Path

    assertThat(path1.shape.animated).isTrue()
    assertThat(path2.shape.animated).isTrue()
  }

  @Test
  fun bezierProperty_parsesSlotId() {
    val json =
      """
      {
        "v": "5.7.0",
        "fr": 30,
        "ip": 0,
        "op": 30,
        "w": 50,
        "h": 50,
        "layers": [
          {
            "ty": 4,
            "nm": "ShapeLayer",
            "shapes": [
              {
                "ty": "sh",
                "nm": "SlotPath",
                "ks": {
                  "sid": "path.custom_outline",
                  "a": 0,
                  "k": {
                    "c": true,
                    "i": [[0.0, 0.0]],
                    "o": [[0.0, 0.0]],
                    "v": [[0.0, 0.0]]
                  }
                }
              }
            ]
          }
        ]
      }
      """
        .trimIndent()

    val animation = Animation.decodeFromString(json)

    val shapeLayer = animation.layers[0] as Layer.ShapeLayer
    val path = shapeLayer.shapes[0] as GraphicElement.Path
    assertThat(path.shape).isNotNull()
  }

  @Test
  fun extraPluginMetadata_ignoredCleanly() {
    val json =
      """
      {
        "v": "5.7.0",
        "fr": 30,
        "ip": 0,
        "op": 30,
        "w": 50,
        "h": 50,
        "meta": { "g": "LottieFiles 2.0" },
        "_ae_version": "17.5.0",
        "layers": []
      }
      """
        .trimIndent()

    val animation = Animation.decodeFromString(json)

    assertThat(animation.frameRate).isEqualTo(30)
    assertThat(animation.layers).isEmpty()
  }
}
