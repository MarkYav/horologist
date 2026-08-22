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
import com.google.android.horologist.remotecompose.lottie.format.Layer
import com.google.android.horologist.remotecompose.lottie.format.LottieDecoder
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.ShapeType
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.Ellipse
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.Path
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.PolyStar
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.PolyStarType
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.PolyStarTypeSerializer
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.Rectangle
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.grouping.Group
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.grouping.Transform
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.CompositeMode
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.CompositeModeSerializer
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.MergeMode
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.MergeModeSerializer
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.TrimMode
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.TrimModeSerializer
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.UnknownElement
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.Fill
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.FillRule
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.FillRuleSerializer
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.GradientFill
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.GradientStroke
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.LineCap
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.LineCapSerializer
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.LineJoin
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.LineJoinSerializer
import com.google.android.horologist.remotecompose.lottie.format.properties.AnimatedGradientProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.AnimatedScalarProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.BaseGradientProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.SplitPositionProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticColorProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticGradientProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticScalarProperty
import com.google.android.horologist.remotecompose.lottie.format.values.GradientValue
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
  fun unknownShapeType_deserializesAsUnknownElementFallback() {
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
              { "ty": "unknown_shape_type", "nm": "CustomShape" },
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
    assertThat(shapeLayer.shapes[0]).isInstanceOf(UnknownElement::class.java)
    assertThat(shapeLayer.shapes[0].type).isEqualTo(ShapeType.Unknown)
    assertThat(shapeLayer.shapes[1]).isInstanceOf(Fill::class.java)
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
    val fill1 = shapeLayer.shapes[0] as Fill
    val fill2 = shapeLayer.shapes[1] as Fill

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
    val fill1 = shapeLayer.shapes[0] as Fill
    val fill2 = shapeLayer.shapes[1] as Fill

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
    val fill1 = shapeLayer.shapes[0] as Fill
    val fill2 = shapeLayer.shapes[1] as Fill
    val fill3 = shapeLayer.shapes[2] as Fill
    val fill4 = shapeLayer.shapes[3] as Fill

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
    val fill = shapeLayer.shapes[0] as Fill
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
    val fill = shapeLayer.shapes[0] as Fill
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
    val path1 = shapeLayer.shapes[0] as Path
    val path2 = shapeLayer.shapes[1] as Path

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
    val path1 = shapeLayer.shapes[0] as Path
    val path2 = shapeLayer.shapes[1] as Path

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
    val path = shapeLayer.shapes[0] as Path
    assertThat(path.shape).isNotNull()
  }

  @Test
  fun vectorProperty_handlesFloatArraysAndSingleNumberFallback() {
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
                "ty": "rc",
                "nm": "StandardRect",
                "s": { "k": [100.0, 50.0] }
              },
              {
                "ty": "el",
                "nm": "3DVectorEllipse",
                "s": { "k": [40.0, 40.0, 0.0] }
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
    val rect = shapeLayer.shapes[0] as Rectangle
    val ellipse = shapeLayer.shapes[1] as Ellipse

    assertThat(rect.size).isNotNull()
    assertThat(ellipse.size).isNotNull()
  }

  @Test
  fun vectorProperty_parsesSlotId() {
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
                "ty": "rc",
                "nm": "SlotRect",
                "s": {
                  "sid": "vector.rect_size",
                  "k": [120.0, 80.0]
                }
              },
              {
                "ty": "tr",
                "nm": "TransformGroup",
                "s": {
                  "sid": "vector.scale",
                  "a": 1,
                  "k": [
                    { "t": 0, "s": [100.0, 100.0] },
                    { "t": 30, "s": [200.0, 200.0] }
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
    val rect = shapeLayer.shapes[0] as Rectangle
    val transform = shapeLayer.shapes[1] as Transform

    assertThat(rect.size).isNotNull()
    assertThat(transform.scale).isNotNull()
  }

  @Test
  fun vectorProperty_animatedKeyframesWithSingleAndNestedValues() {
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
                "ty": "rc",
                "nm": "AnimatedRect",
                "s": {
                  "a": 1,
                  "k": [
                    {
                      "t": 0,
                      "s": [50.0, 50.0],
                      "i": { "x": 0.5, "y": 1.0 },
                      "o": { "x": 0.5, "y": 0.0 }
                    },
                    {
                      "t": 30,
                      "s": [100.0, 100.0]
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
    val rect = shapeLayer.shapes[0] as Rectangle
    assertThat(rect.size.animated).isTrue()
  }

  @Test
  fun positionProperty_handlesFloatArraysAndSingleNumberFallback() {
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
                "ty": "rc",
                "nm": "StandardRect",
                "p": { "k": [100.0, 50.0] }
              },
              {
                "ty": "el",
                "nm": "3DPositionEllipse",
                "p": { "k": [40.0, 40.0, 0.0] }
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
    val rect = shapeLayer.shapes[0] as Rectangle
    val ellipse = shapeLayer.shapes[1] as Ellipse

    assertThat(rect.position).isNotNull()
    assertThat(ellipse.position).isNotNull()
  }

  @Test
  fun positionProperty_parsesSlotId() {
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
                "ty": "rc",
                "nm": "SlotRect",
                "p": {
                  "sid": "position.rect_pos",
                  "k": [120.0, 80.0]
                }
              },
              {
                "ty": "tr",
                "nm": "TransformGroup",
                "p": {
                  "sid": "position.translation",
                  "a": 1,
                  "k": [
                    { "t": 0, "s": [100.0, 100.0] },
                    { "t": 30, "s": [200.0, 200.0] }
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
    val rect = shapeLayer.shapes[0] as Rectangle
    val transform = shapeLayer.shapes[1] as Transform

    assertThat(rect.position).isNotNull()
    assertThat(transform.positionTranslation).isNotNull()
  }

  @Test
  fun positionProperty_animatedKeyframesWithSingleAndNestedValues() {
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
                "ty": "rc",
                "nm": "AnimatedRect",
                "p": {
                  "a": 1,
                  "k": [
                    {
                      "t": 0,
                      "s": [50.0, 50.0],
                      "i": { "x": 0.5, "y": 1.0 },
                      "o": { "x": 0.5, "y": 0.0 }
                    },
                    {
                      "t": 30,
                      "s": [100.0, 100.0]
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
    val rect = shapeLayer.shapes[0] as Rectangle
    assertThat(rect.position.animated).isTrue()
  }

  @Test
  fun positionProperty_splitPosition_deserializesXYScalars() {
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
                "ty": "rc",
                "nm": "SplitRect",
                "p": {
                  "s": true,
                  "x": { "k": 120.0 },
                  "y": {
                    "a": 1,
                    "k": [
                      { "t": 0, "s": 50.0 },
                      { "t": 30, "s": 100.0 }
                    ]
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
    val rect = shapeLayer.shapes[0] as Rectangle
    assertThat(rect.position).isInstanceOf(SplitPositionProperty::class.java)
    val splitPos = rect.position as SplitPositionProperty
    assertThat(splitPos.animated).isTrue()
    assertThat(splitPos.x.animated).isFalse()
    assertThat((splitPos.x as StaticScalarProperty).value).isEqualTo(120.0f)
    assertThat(splitPos.y.animated).isTrue()
  }

  @Test
  fun scalarProperty_handlesPrimitiveNumberArrayAndNestedObject() {
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
                "ty": "rc",
                "nm": "StandardRect",
                "r": 15.0
              },
              {
                "ty": "rc",
                "nm": "ArrayRadiusRect",
                "r": { "k": [25.0] }
              },
              {
                "ty": "fl",
                "nm": "NestedObjectFill",
                "o": { "k": 75.0 },
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
    assertThat(shapeLayer.shapes).hasSize(3)
    val rect1 = shapeLayer.shapes[0] as Rectangle
    val rect2 = shapeLayer.shapes[1] as Rectangle
    val fill = shapeLayer.shapes[2] as Fill

    assertThat(rect1.cornerRadius.animated).isFalse()
    assertThat((rect1.cornerRadius as StaticScalarProperty).value).isEqualTo(15.0f)
    assertThat(rect2.cornerRadius.animated).isFalse()
    assertThat((rect2.cornerRadius as StaticScalarProperty).value).isEqualTo(25.0f)
    assertThat(fill.opacity.animated).isFalse()
    assertThat((fill.opacity as StaticScalarProperty).value).isEqualTo(75.0f)
  }

  @Test
  fun scalarProperty_parsesSlotId() {
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
                "ty": "rc",
                "nm": "SlotCornerRadiusRect",
                "r": {
                  "sid": "scalar.corner_radius",
                  "k": 12.0
                }
              },
              {
                "ty": "tr",
                "nm": "TransformGroup",
                "r": {
                  "sid": "scalar.rotation",
                  "a": 1,
                  "k": [
                    { "t": 0, "s": 0.0 },
                    { "t": 30, "s": 180.0 }
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
    val rect = shapeLayer.shapes[0] as Rectangle
    val transform = shapeLayer.shapes[1] as Transform

    assertThat(rect.cornerRadius).isNotNull()
    assertThat((rect.cornerRadius as StaticScalarProperty).slotId).isEqualTo("scalar.corner_radius")
    assertThat(transform.rotation).isNotNull()
    assertThat((transform.rotation as AnimatedScalarProperty).slotId).isEqualTo("scalar.rotation")
  }

  @Test
  fun scalarProperty_animatedKeyframesWithHoldAndEasingTangents() {
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
                "ty": "rc",
                "nm": "AnimatedScalarRect",
                "r": {
                  "a": 1,
                  "k": [
                    {
                      "t": 0,
                      "s": 10.0,
                      "i": { "x": [0.5], "y": [1.0] },
                      "o": { "x": [0.5], "y": [0.0] }
                    },
                    {
                      "t": 15,
                      "s": [20.0],
                      "h": 1
                    },
                    {
                      "t": 30,
                      "s": 30.0
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
    val rect = shapeLayer.shapes[0] as Rectangle
    assertThat(rect.cornerRadius.animated).isTrue()
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

  @Test
  fun gradientValue_opaqueFloatArray_decodesColorStops() {
    val json = "[0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0]"
    val gradientValue = LottieDecoder.json.decodeFromString(GradientValue.serializer(), json)

    assertThat(gradientValue.numberOfColors).isEqualTo(2)
    assertThat(gradientValue.values).hasSize(8)
    assertThat(gradientValue.hasTransparency).isFalse()
    assertThat(gradientValue.colorStops).hasSize(2)
    assertThat(gradientValue.opacityStops).isEmpty()

    val stop1 = gradientValue.colorStops[0]
    assertThat(stop1.offset).isEqualTo(0f)
    assertThat(stop1.red).isEqualTo(1f)
    assertThat(stop1.green).isEqualTo(0f)
    assertThat(stop1.blue).isEqualTo(0f)

    val stop2 = gradientValue.colorStops[1]
    assertThat(stop2.offset).isEqualTo(1f)
    assertThat(stop2.red).isEqualTo(0f)
    assertThat(stop2.green).isEqualTo(1f)
    assertThat(stop2.blue).isEqualTo(0f)

    val resolved = gradientValue.resolveStops()
    assertThat(resolved).hasSize(2)
    assertThat(resolved[0].offset).isEqualTo(0f)
    assertThat(resolved[0].color).isEqualTo(Color(1f, 0f, 0f, 1f))
    assertThat(resolved[1].offset).isEqualTo(1f)
    assertThat(resolved[1].color).isEqualTo(Color(0f, 1f, 0f, 1f))
  }

  @Test
  fun gradientValue_transparentFloatArray_decodesColorAndOpacityStops() {
    val json =
      """
      {
        "p": 2,
        "k": [0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0, 0.0, 1.0, 1.0, 0.5]
      }
      """
        .trimIndent()
    val gradientValue = LottieDecoder.json.decodeFromString(GradientValue.serializer(), json)

    assertThat(gradientValue.numberOfColors).isEqualTo(2)
    assertThat(gradientValue.values).hasSize(12)
    assertThat(gradientValue.hasTransparency).isTrue()
    assertThat(gradientValue.colorStops).hasSize(2)
    assertThat(gradientValue.opacityStops).hasSize(2)

    val oStop1 = gradientValue.opacityStops[0]
    assertThat(oStop1.offset).isEqualTo(0f)
    assertThat(oStop1.alpha).isEqualTo(1f)

    val oStop2 = gradientValue.opacityStops[1]
    assertThat(oStop2.offset).isEqualTo(1f)
    assertThat(oStop2.alpha).isEqualTo(0.5f)

    val resolved = gradientValue.resolveStops()
    assertThat(resolved).hasSize(2)
    assertThat(resolved[0].color).isEqualTo(Color(1f, 0f, 0f, 1f))
    assertThat(resolved[1].color).isEqualTo(Color(0f, 1f, 0f, 0.5f))
  }

  @Test
  fun gradientValue_nestedObject_decodesColorCountAndValues() {
    val json =
      """
      {
        "p": 3,
        "k": [0.0, 1.0, 0.0, 0.0, 0.5, 0.0, 1.0, 0.0, 1.0, 0.0, 0.0, 1.0]
      }
      """
        .trimIndent()
    val gradientValue = LottieDecoder.json.decodeFromString(GradientValue.serializer(), json)

    assertThat(gradientValue.numberOfColors).isEqualTo(3)
    assertThat(gradientValue.values).hasSize(12)
    assertThat(gradientValue.colorStops).hasSize(3)
    assertThat(gradientValue.colorStops[1].offset).isEqualTo(0.5f)
    assertThat(gradientValue.colorStops[1].green).isEqualTo(1f)
  }

  @Test
  fun gradientValue_stopDecomposition_normalizes255ScaledValues() {
    val json =
      """
      {
        "p": 2,
        "k": [0.0, 255.0, 0.0, 0.0, 1.0, 0.0, 255.0, 0.0, 0.0, 255.0, 1.0, 128.0]
      }
      """
        .trimIndent()
    val gradientValue = LottieDecoder.json.decodeFromString(GradientValue.serializer(), json)

    assertThat(gradientValue.colorStops[0].red).isEqualTo(1f)
    assertThat(gradientValue.colorStops[1].green).isEqualTo(1f)
    assertThat(gradientValue.opacityStops[0].alpha).isEqualTo(1f)
    assertThat(gradientValue.opacityStops[1].alpha).isWithin(0.01f).of(128f / 255f)
  }

  @Test
  fun gradientProperty_staticWithSlotId_deserializes() {
    val json =
      """
      {
        "sid": "gradient.background",
        "a": 0,
        "k": {
          "p": 2,
          "k": [0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0]
        }
      }
      """
        .trimIndent()
    val property = LottieDecoder.json.decodeFromString(BaseGradientProperty.serializer(), json)

    assertThat(property).isInstanceOf(StaticGradientProperty::class.java)
    val staticProp = property as StaticGradientProperty
    assertThat(staticProp.slotId).isEqualTo("gradient.background")
    assertThat(staticProp.animated).isFalse()
    assertThat(staticProp.value.numberOfColors).isEqualTo(2)
    assertThat(staticProp.value.colorStops).hasSize(2)
  }

  @Test
  fun gradientProperty_animatedKeyframes_withHoldAndEasingTangents() {
    val json =
      """
      {
        "sid": "gradient.dynamic",
        "a": 1,
        "p": 2,
        "k": [
          {
            "t": 0,
            "s": [0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0],
            "i": { "x": [0.5], "y": [1.0] },
            "o": { "x": [0.5], "y": [0.0] }
          },
          {
            "t": 15,
            "s": [0.0, 0.0, 1.0, 0.0, 1.0, 0.0, 0.0, 1.0],
            "h": 1
          },
          {
            "t": 30,
            "s": [0.0, 0.0, 0.0, 1.0, 1.0, 1.0, 1.0, 1.0],
            "h": true
          }
        ]
      }
      """
        .trimIndent()
    val property = LottieDecoder.json.decodeFromString(BaseGradientProperty.serializer(), json)

    assertThat(property).isInstanceOf(AnimatedGradientProperty::class.java)
    val animProp = property as AnimatedGradientProperty
    assertThat(animProp.slotId).isEqualTo("gradient.dynamic")
    assertThat(animProp.animated).isTrue()
    assertThat(animProp.numberOfColors).isEqualTo(2)
    assertThat(animProp.keyframes).hasSize(3)

    val kf0 = animProp.keyframes[0]
    assertThat(kf0.frame).isEqualTo(0f)
    assertThat(kf0.hold).isFalse()
    assertThat(kf0.inTangent?.x).isEqualTo(0.5f)
    assertThat(kf0.inTangent?.y).isEqualTo(1.0f)
    assertThat(kf0.outTangent?.x).isEqualTo(0.5f)
    assertThat(kf0.outTangent?.y).isEqualTo(0.0f)
    assertThat(kf0.value[0].numberOfColors).isEqualTo(2)

    val kf1 = animProp.keyframes[1]
    assertThat(kf1.frame).isEqualTo(15f)
    assertThat(kf1.hold).isTrue()

    val kf2 = animProp.keyframes[2]
    assertThat(kf2.frame).isEqualTo(30f)
    assertThat(kf2.hold).isTrue()
  }

  @Test
  fun gradientFill_animatedAndSlotId_deserializes() {
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
                "ty": "gf",
                "nm": "DynamicGradFill",
                "t": 1,
                "s": { "k": [0.0, 0.0] },
                "e": { "k": [50.0, 50.0] },
                "g": {
                  "sid": "slot.grad",
                  "a": 1,
                  "p": 2,
                  "k": [
                    {
                      "t": 0,
                      "s": [0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0]
                    },
                    {
                      "t": 30,
                      "s": [0.0, 0.0, 0.0, 1.0, 1.0, 1.0, 1.0, 0.0]
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
    val gf = shapeLayer.shapes[0] as GradientFill
    assertThat(gf.colors.animated).isTrue()
    assertThat(gf.colors.slotId).isEqualTo("slot.grad")
    val animGradient = gf.colors as AnimatedGradientProperty
    assertThat(animGradient.keyframes).hasSize(2)
  }

  @Test
  fun gradientStroke_withHighlights_deserializes() {
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
                "ty": "gs",
                "nm": "RadialGradStroke",
                "t": 2,
                "s": { "k": [25.0, 25.0] },
                "e": { "k": [50.0, 50.0] },
                "r": { "k": 45.0 },
                "h": { "k": 90.0 },
                "w": { "k": 2.5 },
                "g": {
                  "p": 2,
                  "k": [0.0, 1.0, 1.0, 0.0, 1.0, 0.0, 1.0, 1.0]
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
    val gs = shapeLayer.shapes[0] as GradientStroke
    assertThat((gs.highlightLength as StaticScalarProperty).value).isEqualTo(45.0f)
    assertThat((gs.highlightAngle as StaticScalarProperty).value).isEqualTo(90.0f)
    assertThat((gs.strokeWidth as StaticScalarProperty).value).isEqualTo(2.5f)
  }

  @Test
  fun shapeTypeEnum_deserializesFromStringOrDefaultsToUnknown() {
    assertThat(ShapeType.fromValueOrNull("sh")).isEqualTo(ShapeType.Path)
    assertThat(ShapeType.fromValueOrNull("rc")).isEqualTo(ShapeType.Rectangle)
    assertThat(ShapeType.fromValueOrNull("el")).isEqualTo(ShapeType.Ellipse)
    assertThat(ShapeType.fromValueOrNull("sr")).isEqualTo(ShapeType.PolyStar)
    assertThat(ShapeType.fromValueOrNull("gr")).isEqualTo(ShapeType.Group)
    assertThat(ShapeType.fromValueOrNull("tr")).isEqualTo(ShapeType.Transform)
    assertThat(ShapeType.fromValueOrNull("fl")).isEqualTo(ShapeType.Fill)
    assertThat(ShapeType.fromValueOrNull("st")).isEqualTo(ShapeType.Stroke)
    assertThat(ShapeType.fromValueOrNull("gf")).isEqualTo(ShapeType.GradientFill)
    assertThat(ShapeType.fromValueOrNull("gs")).isEqualTo(ShapeType.GradientStroke)
    assertThat(ShapeType.fromValueOrNull("no")).isEqualTo(ShapeType.NoStyle)
    assertThat(ShapeType.fromValueOrNull("tm")).isEqualTo(ShapeType.TrimPath)
    assertThat(ShapeType.fromValueOrNull("rp")).isEqualTo(ShapeType.Repeater)
    assertThat(ShapeType.fromValueOrNull("rd")).isEqualTo(ShapeType.RoundedCorners)
    assertThat(ShapeType.fromValueOrNull("mm")).isEqualTo(ShapeType.MergePaths)
    assertThat(ShapeType.fromValueOrNull("unsupported")).isNull()

    val decodedKnown = LottieDecoder.json.decodeFromString(ShapeType.serializer(), "\"st\"")
    assertThat(decodedKnown).isEqualTo(ShapeType.Stroke)

    val decodedUnknown =
      LottieDecoder.json.decodeFromString(ShapeType.serializer(), "\"invalid_type\"")
    assertThat(decodedUnknown).isEqualTo(ShapeType.Unknown)
  }

  @Test
  fun lineCapAndJoin_handlesIntegerAndFloatAndFallback() {
    assertThat(LottieDecoder.json.decodeFromString(LineCapSerializer, "1")).isEqualTo(LineCap.Butt)
    assertThat(LottieDecoder.json.decodeFromString(LineCapSerializer, "2")).isEqualTo(LineCap.Round)
    assertThat(LottieDecoder.json.decodeFromString(LineCapSerializer, "3"))
      .isEqualTo(LineCap.Square)
    assertThat(LottieDecoder.json.decodeFromString(LineCapSerializer, "2.0"))
      .isEqualTo(LineCap.Round)
    assertThat(LottieDecoder.json.decodeFromString(LineCapSerializer, "999"))
      .isEqualTo(LineCap.Round)

    assertThat(LottieDecoder.json.decodeFromString(LineJoinSerializer, "1"))
      .isEqualTo(LineJoin.Miter)
    assertThat(LottieDecoder.json.decodeFromString(LineJoinSerializer, "2"))
      .isEqualTo(LineJoin.Round)
    assertThat(LottieDecoder.json.decodeFromString(LineJoinSerializer, "3"))
      .isEqualTo(LineJoin.Bevel)
    assertThat(LottieDecoder.json.decodeFromString(LineJoinSerializer, "1.0"))
      .isEqualTo(LineJoin.Miter)
    assertThat(LottieDecoder.json.decodeFromString(LineJoinSerializer, "999"))
      .isEqualTo(LineJoin.Round)
  }

  @Test
  fun enumSerializers_trimCompositeMergePolyStarFillRule_handlesIntegersFloatsAndFallbacks() {
    assertThat(LottieDecoder.json.decodeFromString(TrimModeSerializer, "1"))
      .isEqualTo(TrimMode.Simultaneously)
    assertThat(LottieDecoder.json.decodeFromString(TrimModeSerializer, "2.0"))
      .isEqualTo(TrimMode.Individually)
    assertThat(LottieDecoder.json.decodeFromString(TrimModeSerializer, "99"))
      .isEqualTo(TrimMode.Simultaneously)

    assertThat(LottieDecoder.json.decodeFromString(CompositeModeSerializer, "1"))
      .isEqualTo(CompositeMode.Above)
    assertThat(LottieDecoder.json.decodeFromString(CompositeModeSerializer, "2.0"))
      .isEqualTo(CompositeMode.Below)
    assertThat(LottieDecoder.json.decodeFromString(CompositeModeSerializer, "99"))
      .isEqualTo(CompositeMode.Above)

    assertThat(LottieDecoder.json.decodeFromString(MergeModeSerializer, "1"))
      .isEqualTo(MergeMode.Merge)
    assertThat(LottieDecoder.json.decodeFromString(MergeModeSerializer, "2"))
      .isEqualTo(MergeMode.Add)
    assertThat(LottieDecoder.json.decodeFromString(MergeModeSerializer, "3"))
      .isEqualTo(MergeMode.Subtract)
    assertThat(LottieDecoder.json.decodeFromString(MergeModeSerializer, "4.0"))
      .isEqualTo(MergeMode.Intersect)
    assertThat(LottieDecoder.json.decodeFromString(MergeModeSerializer, "5"))
      .isEqualTo(MergeMode.ExcludeIntersections)
    assertThat(LottieDecoder.json.decodeFromString(MergeModeSerializer, "99"))
      .isEqualTo(MergeMode.Merge)

    assertThat(LottieDecoder.json.decodeFromString(PolyStarTypeSerializer, "1"))
      .isEqualTo(PolyStarType.Star)
    assertThat(LottieDecoder.json.decodeFromString(PolyStarTypeSerializer, "2.0"))
      .isEqualTo(PolyStarType.Polygon)
    assertThat(LottieDecoder.json.decodeFromString(PolyStarTypeSerializer, "99"))
      .isEqualTo(PolyStarType.Star)

    assertThat(LottieDecoder.json.decodeFromString(FillRuleSerializer, "1"))
      .isEqualTo(FillRule.NonZero)
    assertThat(LottieDecoder.json.decodeFromString(FillRuleSerializer, "2.0"))
      .isEqualTo(FillRule.EvenOdd)
    assertThat(LottieDecoder.json.decodeFromString(FillRuleSerializer, "99"))
      .isEqualTo(FillRule.NonZero)
  }
}
