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
import com.google.android.horologist.remotecompose.lottie.format.LottieDecoder
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.ShapeType
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.Path
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.UnknownElement
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.Fill
import com.google.android.horologist.remotecompose.lottie.format.layer.LayerType
import com.google.android.horologist.remotecompose.lottie.format.layer.ShapeLayer
import com.google.android.horologist.remotecompose.lottie.format.layer.SolidColorLayer
import com.google.android.horologist.remotecompose.lottie.format.layer.UnknownLayer
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticBezierProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticColorProperty
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LottieDecoderResilienceTest {

  @Test
  fun unknownLayerType_deserializesAsUnknownLayerFallback() {
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
          {
            "ty": 999,
            "nm": "UnsupportedAudioLayer",
            "ind": 1,
            "ip": 5.0,
            "op": 55.0,
            "ks": { "p": { "k": [10.0, 20.0] } }
          },
          { "ty": 4, "nm": "ValidShapeLayer", "ind": 2, "shapes": [] }
        ]
      }
      """
        .trimIndent()

    val animation = Animation.decodeFromString(json)

    assertThat(animation.layers).hasSize(2)
    assertThat(animation.layers[0]).isInstanceOf(UnknownLayer::class.java)
    val unknownLayer = animation.layers[0] as UnknownLayer
    assertThat(unknownLayer.name).isEqualTo("UnsupportedAudioLayer")
    assertThat(unknownLayer.type).isEqualTo(LayerType.Unknown)
    assertThat(unknownLayer.index).isEqualTo(1)
    assertThat(unknownLayer.startFrame).isEqualTo(5.0f)
    assertThat(unknownLayer.endFrame).isEqualTo(55.0f)
    assertThat(unknownLayer.transform).isNotNull()
    assertThat(animation.layers[1]).isInstanceOf(ShapeLayer::class.java)
  }

  @Test
  fun unknownLayerType_preservesParentingTransformHierarchy() {
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
          {
            "ty": 888,
            "nm": "UnknownParent",
            "ind": 1,
            "ks": {
              "p": { "k": [100.0, 50.0] }
            }
          },
          {
            "ty": 4,
            "nm": "ChildShapeLayer",
            "ind": 2,
            "parent": 1,
            "shapes": []
          }
        ]
      }
      """
        .trimIndent()

    val animation = Animation.decodeFromString(json)

    assertThat(animation.layers).hasSize(2)
    val parentLayer = animation.layers[0] as UnknownLayer
    val childLayer = animation.layers[1] as ShapeLayer

    assertThat(parentLayer.transform).isNotNull()
    assertThat(childLayer.parent).isEqualTo(1)
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

    val shapeLayer = animation.layers[0] as ShapeLayer
    assertThat(shapeLayer.shapes).hasSize(2)
    assertThat(shapeLayer.shapes[0]).isInstanceOf(UnknownElement::class.java)
    val unknownElement = shapeLayer.shapes[0] as UnknownElement
    assertThat(unknownElement.name).isEqualTo("CustomShape")
    assertThat(unknownElement.type).isEqualTo(ShapeType.Unknown)
    assertThat(shapeLayer.shapes[1]).isInstanceOf(Fill::class.java)
  }

  @Test
  fun unknownShapeType_withoutItArray_deserializesCleanly() {
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
              { "ty": "custom_filter", "nm": "Blur" }
            ]
          }
        ]
      }
      """
        .trimIndent()

    val animation = Animation.decodeFromString(json)

    val shapeLayer = animation.layers[0] as ShapeLayer
    assertThat(shapeLayer.shapes).hasSize(1)
    assertThat(shapeLayer.shapes[0]).isInstanceOf(UnknownElement::class.java)
    assertThat(shapeLayer.shapes[0].name).isEqualTo("Blur")
  }

  @Test
  fun shapeType_fromValueOrNull_and_unknownFallback() {
    assertThat(ShapeType.fromValueOrNull("sh")).isEqualTo(ShapeType.Path)
    assertThat(ShapeType.fromValueOrNull("rc")).isEqualTo(ShapeType.Rectangle)
    assertThat(ShapeType.fromValueOrNull("el")).isEqualTo(ShapeType.Ellipse)
    assertThat(ShapeType.fromValueOrNull("sr")).isEqualTo(ShapeType.PolyStar)
    assertThat(ShapeType.fromValueOrNull("gr")).isEqualTo(ShapeType.Group)
    assertThat(ShapeType.fromValueOrNull("tr")).isEqualTo(ShapeType.Transform)
    assertThat(ShapeType.fromValueOrNull("fl")).isEqualTo(ShapeType.Fill)
    assertThat(ShapeType.fromValueOrNull("unsupported")).isNull()

    val decodedKnown = LottieDecoder.json.decodeFromString(ShapeType.serializer(), "\"fl\"")
    assertThat(decodedKnown).isEqualTo(ShapeType.Fill)

    val decodedUnknown =
      LottieDecoder.json.decodeFromString(ShapeType.serializer(), "\"invalid_type\"")
    assertThat(decodedUnknown).isEqualTo(ShapeType.Unknown)
  }

  @Test
  fun layerType_fromValueOrNull_and_unknownFallback() {
    assertThat(LayerType.fromValueOrNull(1)).isEqualTo(LayerType.Solid)
    assertThat(LayerType.fromValueOrNull(3)).isEqualTo(LayerType.Null)
    assertThat(LayerType.fromValueOrNull(4)).isEqualTo(LayerType.Shape)
    assertThat(LayerType.fromValueOrNull(999)).isNull()

    val decodedKnown = LottieDecoder.json.decodeFromString(LayerType.serializer(), "4")
    assertThat(decodedKnown).isEqualTo(LayerType.Shape)

    val decodedUnknown = LottieDecoder.json.decodeFromString(LayerType.serializer(), "999")
    assertThat(decodedUnknown).isEqualTo(LayerType.Unknown)
  }

  @Test
  fun animation_withMissingFields_deserializesWithSensibleDefaults() {
    val json = "{}"
    val animation = Animation.decodeFromString(json)

    assertThat(animation.frameRate).isEqualTo(30f)
    assertThat(animation.startFrame).isEqualTo(0f)
    assertThat(animation.endFrame).isEqualTo(0f)
    assertThat(animation.width).isEqualTo(0)
    assertThat(animation.height).isEqualTo(0)
    assertThat(animation.layers).isEmpty()
    assertThat(animation.version).isEqualTo("5.9.6")
  }

  @Test
  fun animation_withFractionalFrameRate_deserializesCorrectly() {
    val json2997 = """{"fr": 29.97, "ip": 0, "op": 100}"""
    val anim2997 = Animation.decodeFromString(json2997)
    assertThat(anim2997.frameRate).isEqualTo(29.97f)

    val json23976 = """{"fr": 23.976, "ip": 0, "op": 100}"""
    val anim23976 = Animation.decodeFromString(json23976)
    assertThat(anim23976.frameRate).isEqualTo(23.976f)

    val json5994 = """{"fr": 59.94, "ip": 0, "op": 100}"""
    val anim5994 = Animation.decodeFromString(json5994)
    assertThat(anim5994.frameRate).isEqualTo(59.94f)
  }

  @Test
  fun solidColorLayer_withMissingFields_deserializesWithSensibleDefaults() {
    val json =
      """
      {
        "layers": [
          { "ty": 1 }
        ]
      }
      """
        .trimIndent()
    val animation = Animation.decodeFromString(json)

    assertThat(animation.layers).hasSize(1)
    val solidLayer = animation.layers[0] as SolidColorLayer
    assertThat(solidLayer.solidColor).isEqualTo("#000000")
    assertThat(solidLayer.solidWidth).isEqualTo(0f)
    assertThat(solidLayer.solidHeight).isEqualTo(0f)
  }

  @Test
  fun shapes_withMissingColorAndBezierProperties_deserializeWithSensibleDefaults() {
    val json =
      """
      {
        "layers": [
          {
            "ty": 4,
            "shapes": [
              { "ty": "fl" },
              { "ty": "sh" }
            ]
          }
        ]
      }
      """
        .trimIndent()
    val animation = Animation.decodeFromString(json)

    assertThat(animation.layers).hasSize(1)
    val shapeLayer = animation.layers[0] as ShapeLayer
    assertThat(shapeLayer.shapes).hasSize(2)

    val fill = shapeLayer.shapes[0] as Fill
    assertThat(fill.color).isInstanceOf(StaticColorProperty::class.java)

    val path = shapeLayer.shapes[1] as Path
    assertThat(path.shape).isInstanceOf(StaticBezierProperty::class.java)
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

    val shapeLayer = animation.layers[0] as ShapeLayer
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

    val shapeLayer = animation.layers[0] as ShapeLayer
    val fill1 = shapeLayer.shapes[0] as Fill
    val fill2 = shapeLayer.shapes[1] as Fill

    assertThat(fill1.color.slotId).isEqualTo("color.primary")
    assertThat(fill2.color.slotId).isNull()

    val slotMap = SlotMap(mapOf("color.primary" to RemoteColor(Color.Green)))
    assertThat(slotMap.getColor("color.primary")).isNotNull()
    assertThat(slotMap.getColor("unknown")).isNull()
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

    assertThat(animation.frameRate).isEqualTo(30f)
    assertThat(animation.layers).isEmpty()
  }
}
