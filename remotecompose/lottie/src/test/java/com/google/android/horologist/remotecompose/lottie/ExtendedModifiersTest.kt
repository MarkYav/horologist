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
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.ui.graphics.Color
import com.google.android.horologist.remotecompose.lottie.format.Animation
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.Path
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.OffsetPath
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.PuckerBloat
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.Twist
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.ZigZag
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.ZigZagType
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.Fill
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.LineJoin
import com.google.android.horologist.remotecompose.lottie.format.layer.ShapeLayer
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticBezierProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticColorProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticPositionProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticScalarProperty
import com.google.android.horologist.remotecompose.lottie.format.values.BezierValue
import com.google.android.horologist.remotecompose.lottie.renderer.RemoteLottiePath
import com.google.android.horologist.remotecompose.lottie.renderer.gatherShapesForTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test

@SuppressLint("RestrictedApi")
class ExtendedModifiersTest {

  private val settings = LottieSettings(0f.rf, SlotMap(emptyMap()))

  @Test
  fun puckerBloat_jsonDeserialization() {
    val json =
      """
      {
        "v": "5.5.7",
        "fr": 60,
        "ip": 0,
        "op": 60,
        "w": 100,
        "h": 100,
        "layers": [
          {
            "ty": 4,
            "nm": "Layer",
            "shapes": [
              {
                "ty": "pb",
                "nm": "PuckerBloat 1",
                "a": { "a": 0, "k": 45.0 }
              }
            ]
          }
        ]
      }
      """
        .trimIndent()

    val anim = Animation.decodeFromString(json)
    val layer = anim.layers[0] as ShapeLayer
    val pb = layer.shapes[0] as PuckerBloat
    assertThat(pb.name).isEqualTo("PuckerBloat 1")
    assertThat(pb.amount).isInstanceOf(StaticScalarProperty::class.java)
    assertThat((pb.amount as StaticScalarProperty).value).isEqualTo(45f)
  }

  @Test
  fun twist_jsonDeserialization() {
    val json =
      """
      {
        "v": "5.5.7",
        "fr": 60,
        "ip": 0,
        "op": 60,
        "w": 100,
        "h": 100,
        "layers": [
          {
            "ty": 4,
            "nm": "Layer",
            "shapes": [
              {
                "ty": "tw",
                "nm": "Twist 1",
                "a": { "a": 0, "k": 90.0 },
                "c": { "a": 0, "k": [10.0, 20.0] }
              }
            ]
          }
        ]
      }
      """
        .trimIndent()

    val anim = Animation.decodeFromString(json)
    val layer = anim.layers[0] as ShapeLayer
    val tw = layer.shapes[0] as Twist
    assertThat(tw.name).isEqualTo("Twist 1")
    assertThat((tw.angle as StaticScalarProperty).value).isEqualTo(90f)
    assertThat((tw.center as StaticPositionProperty).value).containsExactly(10f, 20f).inOrder()
  }

  @Test
  fun zigZag_jsonDeserialization() {
    val json =
      """
      {
        "v": "5.5.7",
        "fr": 60,
        "ip": 0,
        "op": 60,
        "w": 100,
        "h": 100,
        "layers": [
          {
            "ty": 4,
            "nm": "Layer",
            "shapes": [
              {
                "ty": "zz",
                "nm": "ZigZag 1",
                "s": { "a": 0, "k": 10.0 },
                "r": { "a": 0, "k": 4.0 },
                "pt": 2
              }
            ]
          }
        ]
      }
      """
        .trimIndent()

    val anim = Animation.decodeFromString(json)
    val layer = anim.layers[0] as ShapeLayer
    val zz = layer.shapes[0] as ZigZag
    assertThat(zz.name).isEqualTo("ZigZag 1")
    assertThat((zz.size as StaticScalarProperty).value).isEqualTo(10f)
    assertThat((zz.ridgesPerSegment as StaticScalarProperty).value).isEqualTo(4f)
    assertThat(zz.pointType).isEqualTo(ZigZagType.Smooth)
  }

  @Test
  fun offsetPath_jsonDeserialization() {
    val json =
      """
      {
        "v": "5.5.7",
        "fr": 60,
        "ip": 0,
        "op": 60,
        "w": 100,
        "h": 100,
        "layers": [
          {
            "ty": 4,
            "nm": "Layer",
            "shapes": [
              {
                "ty": "op",
                "nm": "OffsetPath 1",
                "a": { "a": 0, "k": 5.0 },
                "lj": 1,
                "ml": { "a": 0, "k": 4.0 }
              }
            ]
          }
        ]
      }
      """
        .trimIndent()

    val anim = Animation.decodeFromString(json)
    val layer = anim.layers[0] as ShapeLayer
    val op = layer.shapes[0] as OffsetPath
    assertThat(op.name).isEqualTo("OffsetPath 1")
    assertThat((op.amount as StaticScalarProperty).value).isEqualTo(5f)
    assertThat(op.lineJoin).isEqualTo(LineJoin.Miter)
    assertThat((op.miterLimit as StaticScalarProperty).value).isEqualTo(4f)
  }

  @Test
  fun gatherShapes_withExtendedModifiers_gracefullyPreservesGeometries() {
    val line =
      BezierValue(
        closed = false,
        vertices = listOf(listOf(0f, 0f), listOf(100f, 0f)),
        inTangents = listOf(listOf(0f, 0f), listOf(0f, 0f)),
        outTangents = listOf(listOf(0f, 0f), listOf(0f, 0f)),
      )
    val elements =
      listOf(
        Path(shape = StaticBezierProperty(value = line)),
        ZigZag(
          size = StaticScalarProperty(value = 10f),
          ridgesPerSegment = StaticScalarProperty(value = 2f),
          pointType = ZigZagType.Corner,
        ),
        PuckerBloat(amount = StaticScalarProperty(value = 20f)),
        Twist(
          angle = StaticScalarProperty(value = 45f),
          center = StaticPositionProperty(value = listOf(50f, 0f)),
        ),
        OffsetPath(amount = StaticScalarProperty(value = 5f)),
        Fill(color = StaticColorProperty(value = Color.Blue.rc)),
      )

    val shapeGroups = gatherShapesForTest(elements, settings)
    assertThat(shapeGroups).hasSize(1)
    val group = shapeGroups[0]
    val lottiePath = group.shapes[0] as RemoteLottiePath
    assertThat(lottiePath.path).isNotEmpty()
  }
}
