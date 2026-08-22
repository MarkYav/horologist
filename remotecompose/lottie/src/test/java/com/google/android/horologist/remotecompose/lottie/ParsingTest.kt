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

import android.content.Context
import androidx.compose.remote.creation.compose.state.rf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.horologist.remotecompose.lottie.format.Animation
import com.google.android.horologist.remotecompose.lottie.format.LottieDecoder
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.ShapeType
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.Ellipse
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.Path
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.PolyStar
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.PolyStarType
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.Rectangle
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.grouping.Group
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.grouping.Transform
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.CompositeMode
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.MergeMode
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.MergePaths
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.OffsetPath
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.PuckerBloat
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.Repeater
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.RoundedCorners
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.TrimMode
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.TrimPath
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.Twist
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.ZigZag
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.ZigZagType
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.Fill
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.FillRule
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.GradientFill
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.GradientStroke
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.GradientType
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.LineCap
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.LineJoin
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.NoStyle
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.Stroke
import com.google.android.horologist.remotecompose.lottie.format.layer.BlendMode
import com.google.android.horologist.remotecompose.lottie.format.layer.BlendModeSerializer
import com.google.android.horologist.remotecompose.lottie.format.layer.ImageLayer
import com.google.android.horologist.remotecompose.lottie.format.layer.LayerType
import com.google.android.horologist.remotecompose.lottie.format.layer.MatteMode
import com.google.android.horologist.remotecompose.lottie.format.layer.MatteModeSerializer
import com.google.android.horologist.remotecompose.lottie.format.layer.NullLayer
import com.google.android.horologist.remotecompose.lottie.format.layer.PrecompLayer
import com.google.android.horologist.remotecompose.lottie.format.layer.ShapeLayer
import com.google.android.horologist.remotecompose.lottie.format.layer.SolidColorLayer
import com.google.android.horologist.remotecompose.lottie.format.layer.TextLayer
import com.google.android.horologist.remotecompose.lottie.format.properties.AnimatedBezierProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.AnimatedGradientProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.AnimatedVectorProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.BaseGradientProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticGradientProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticPositionProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticScalarProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticVectorProperty
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animateGradient
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animateScalar
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ParsingTest {

  private fun loadGeometry(): Animation {
    val context = ApplicationProvider.getApplicationContext<Context>()
    return Animation.load(R.raw.geometry, context)
  }

  @Test
  fun geometryTest() {
    val animation = loadGeometry()

    assertThat(animation).isNotNull()
    assertThat(animation.name).isEqualTo("[lottie] geometry")
    assertThat(animation.version).isEqualTo("0.0.0")
    assertThat(animation.layers).hasSize(2)
  }

  @Test
  fun layerPolymorphism_deserializes() {
    val animation = loadGeometry()

    assertThat(animation.layers[0].name).isEqualTo("Scale (Import Fix)")
    assertThat(animation.layers[0].type).isEqualTo(LayerType.Null)
    assertThat(animation.layers[0].index).isEqualTo(1)
  }

  @Test
  fun layerTypeEnum_deserializes() {
    val animation = loadGeometry()

    assertThat(animation.layers[0].type).isEqualTo(LayerType.Null)
    assertThat(animation.layers[1].type).isEqualTo(LayerType.Shape)
  }

  @Test
  fun shapeTypePolymorphism_deserializes() {
    val animation = loadGeometry()

    val shapeLayer = animation.layers[1] as ShapeLayer
    val group = shapeLayer.shapes[0] as Group

    assertThat(group.shapes.size).isEqualTo(3)
  }

  @Test
  fun fillColorSlotId_deserializes() {
    val animation = loadGeometry()

    val shapeLayer = animation.layers[1] as ShapeLayer
    val group = shapeLayer.shapes[0] as Group
    val fill = group.shapes[1] as Fill

    assertThat(fill.color.slotId).isEqualTo("color.primary")
  }

  @Test
  fun shapeTypeEnum_deserializes() {
    val animation = loadGeometry()

    val shapeLayer = animation.layers[1] as ShapeLayer

    assertThat(shapeLayer.shapes[0].type).isEqualTo(ShapeType.Group)
  }

  @Test
  fun animatedBezierProperty_deserializes() {
    val animation = loadGeometry()

    val shapeLayer = animation.layers[1] as ShapeLayer
    val group = shapeLayer.shapes[0] as Group
    val path = group.shapes[0] as Path

    val animatedShape = path.shape as AnimatedBezierProperty

    assertThat(animatedShape.keyframes).hasSize(5)
    assertThat(animatedShape.keyframes[0].inTangent?.x).isEqualTo(0.999f)
    assertThat(animatedShape.keyframes[0].inTangent?.y).isEqualTo(1f)
  }

  @Test
  fun animatedVectorProperty_deserializes() {
    val animation = loadGeometry()

    val shapeLayer = animation.layers[1] as ShapeLayer
    val transform = shapeLayer.transform!!

    assertThat(transform.scale.animated).isTrue()
    val animatedScale = transform.scale as AnimatedVectorProperty

    assertThat(animatedScale.keyframes).hasSize(5)
    assertThat(animatedScale.keyframes[0].inTangent?.x).isEqualTo(0.999f)
  }

  /**
   * Tests deserialization of parametric rectangle and ellipse shapes.
   *
   * Source:
   * [Lottie Format Feature Support & Sample Test Suite](https://docs.google.com/document/d/1jXj3kbXL57kxjRc0soUqst2poa2-Lrc2qZAIzEmbB8w/edit)
   */
  @Test
  fun rectEllipse_deserializes() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val animation = Animation.load(R.raw.rect_ellipse, context)

    assertThat(animation).isNotNull()
    val shapeLayer = animation.layers[0] as ShapeLayer
    assertThat(shapeLayer.shapes).hasSize(4)

    val group1 = shapeLayer.shapes[0] as Group
    val rect = group1.shapes[0] as Rectangle
    assertThat(rect.type).isEqualTo(ShapeType.Rectangle)
    assertThat(rect.position.animated).isFalse()
    assertThat((rect.position as StaticPositionProperty).value).isEqualTo(listOf(36f, 36f))
    assertThat(rect.size.animated).isFalse()
    assertThat((rect.size as StaticVectorProperty).value).isEqualTo(listOf(48f, 40f))
    assertThat(rect.cornerRadius.animated).isFalse()
    assertThat((rect.cornerRadius as StaticScalarProperty).value).isEqualTo(10f)

    val settings = LottieSettings(0.rf, SlotMap.Empty)
    val cornerRadiusRf = animateScalar(rect.cornerRadius, settings)
    assertThat(cornerRadiusRf.constantValueOrNull).isEqualTo(10f)

    val group3 = shapeLayer.shapes[2] as Group
    val ellipse = group3.shapes[0] as Ellipse
    assertThat(ellipse.type).isEqualTo(ShapeType.Ellipse)
    assertThat(ellipse.position.animated).isFalse()
    assertThat((ellipse.position as StaticPositionProperty).value).isEqualTo(listOf(36f, 92f))
    assertThat(ellipse.size.animated).isFalse()
    assertThat((ellipse.size as StaticVectorProperty).value).isEqualTo(listOf(42f, 42f))
  }

  /**
   * Tests deserialization of parametric star and polygon shapes.
   *
   * Source:
   * [Lottie Format Feature Support & Sample Test Suite](https://docs.google.com/document/d/1jXj3kbXL57kxjRc0soUqst2poa2-Lrc2qZAIzEmbB8w/edit)
   */
  @Test
  fun polystar_deserializes() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val animation = Animation.load(R.raw.polystar, context)

    assertThat(animation).isNotNull()
    val shapeLayer = animation.layers[0] as ShapeLayer
    assertThat(shapeLayer.shapes).hasSize(4)

    val starGroup = shapeLayer.shapes[0] as Group
    val star = starGroup.shapes[0] as PolyStar
    assertThat(star.type).isEqualTo(ShapeType.PolyStar)
    assertThat(star.starType).isEqualTo(PolyStarType.Star)
    assertThat(star.points.animated).isFalse()
    assertThat((star.points as StaticScalarProperty).value).isEqualTo(5f)
    assertThat((star.outerRadius as StaticScalarProperty).value).isEqualTo(26f)
    assertThat((star.innerRadius as StaticScalarProperty).value).isEqualTo(13f)

    val polygonGroup = shapeLayer.shapes[1] as Group
    val polygon = polygonGroup.shapes[0] as PolyStar
    assertThat(polygon.type).isEqualTo(ShapeType.PolyStar)
    assertThat(polygon.starType).isEqualTo(PolyStarType.Polygon)
    assertThat(polygon.points.animated).isFalse()
    assertThat((polygon.points as StaticScalarProperty).value).isEqualTo(6f)
    assertThat((polygon.outerRadius as StaticScalarProperty).value).isEqualTo(24f)

    val settings = LottieSettings(0.rf, SlotMap.Empty)
    val pointsRf = animateScalar(polygon.points, settings)
    assertThat(pointsRf.constantValueOrNull).isEqualTo(6f)
    val outerRadiusRf = animateScalar(polygon.outerRadius, settings)
    assertThat(outerRadiusRf.constantValueOrNull).isEqualTo(24f)
  }

  @Test
  fun scalarProperties_shapesAndTransforms_deserializes() {
    val animation = loadGeometry()

    val shapeLayer = animation.layers[1] as ShapeLayer
    val transform = shapeLayer.transform!!
    assertThat(transform.rotation.animated).isFalse()
    assertThat((transform.rotation as StaticScalarProperty).value).isEqualTo(0f)
    assertThat(transform.opacity.animated).isFalse()
    assertThat((transform.opacity as StaticScalarProperty).value).isEqualTo(100f)

    val group = shapeLayer.shapes[0] as Group
    val fill = group.shapes[1] as Fill
    assertThat(fill.opacity.animated).isFalse()
    assertThat((fill.opacity as StaticScalarProperty).value).isEqualTo(100f)
  }

  @Test
  fun gradientProperties_deserializes() {
    val staticJson =
      """
      {
        "sid": "grad.theme",
        "a": 0,
        "k": {
          "p": 2,
          "k": [0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0]
        }
      }
      """
        .trimIndent()

    val staticProp =
      LottieDecoder.json.decodeFromString(BaseGradientProperty.serializer(), staticJson)
    assertThat(staticProp).isInstanceOf(StaticGradientProperty::class.java)
    assertThat(staticProp.slotId).isEqualTo("grad.theme")
    assertThat(staticProp.animated).isFalse()
    assertThat((staticProp as StaticGradientProperty).value.numberOfColors).isEqualTo(2)

    val animatedJson =
      """
      {
        "sid": "grad.anim",
        "a": 1,
        "p": 2,
        "k": [
          {
            "t": 0,
            "s": [0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0],
            "i": { "x": 0.5, "y": 1.0 },
            "o": { "x": 0.5, "y": 0.0 }
          },
          {
            "t": 60,
            "s": [0.0, 0.0, 1.0, 0.0, 1.0, 0.0, 0.0, 1.0]
          }
        ]
      }
      """
        .trimIndent()

    val animatedProp =
      LottieDecoder.json.decodeFromString(BaseGradientProperty.serializer(), animatedJson)
    assertThat(animatedProp).isInstanceOf(AnimatedGradientProperty::class.java)
    assertThat(animatedProp.slotId).isEqualTo("grad.anim")
    assertThat(animatedProp.animated).isTrue()
    val animGradient = animatedProp as AnimatedGradientProperty
    assertThat(animGradient.numberOfColors).isEqualTo(2)
    assertThat(animGradient.keyframes).hasSize(2)
    assertThat(animGradient.keyframes[0].frame).isEqualTo(0f)
    assertThat(animGradient.keyframes[1].frame).isEqualTo(60f)

    val settings = LottieSettings(0.rf, SlotMap.Empty)
    val remoteGrad = animateGradient(animGradient, settings)
    assertThat(remoteGrad.numberOfColors).isEqualTo(2)
    assertThat(remoteGrad.values).hasSize(8)
  }

  @Test
  fun gradientFill_deserializes() {
    val json =
      """
      {
        "v": "5.7.0",
        "fr": 30,
        "ip": 0,
        "op": 30,
        "w": 100,
        "h": 100,
        "layers": [
          {
            "ty": 4,
            "nm": "GradientFillLayer",
            "shapes": [
              {
                "ty": "gf",
                "nm": "LinearGradientFill",
                "t": 1,
                "s": { "k": [0.0, 0.0] },
                "e": { "k": [100.0, 100.0] },
                "o": { "k": 100.0 },
                "g": {
                  "p": 2,
                  "k": [0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0]
                }
              }
            ]
          }
        ]
      }
      """
        .trimIndent()

    val animation = Animation.decodeFromString(json)
    val shapeLayer = animation.layers[0] as ShapeLayer
    assertThat(shapeLayer.shapes).hasSize(1)
    val gf = shapeLayer.shapes[0] as GradientFill
    assertThat(gf.type).isEqualTo(ShapeType.GradientFill)
    assertThat(gf.gradientType).isEqualTo(GradientType.Linear)
    assertThat(gf.colors.animated).isFalse()
    assertThat((gf.colors as StaticGradientProperty).value.numberOfColors).isEqualTo(2)
    assertThat((gf.opacity as StaticScalarProperty).value).isEqualTo(100f)
  }

  @Test
  fun gradientStroke_deserializes() {
    val json =
      """
      {
        "v": "5.7.0",
        "fr": 30,
        "ip": 0,
        "op": 30,
        "w": 100,
        "h": 100,
        "layers": [
          {
            "ty": 4,
            "nm": "GradientStrokeLayer",
            "shapes": [
              {
                "ty": "gs",
                "nm": "RadialGradientStroke",
                "t": 2,
                "s": { "k": [50.0, 50.0] },
                "e": { "k": [100.0, 100.0] },
                "w": { "k": 4.0 },
                "o": { "k": 80.0 },
                "g": {
                  "p": 2,
                  "k": [0.0, 0.0, 0.0, 1.0, 1.0, 1.0, 1.0, 0.0]
                }
              }
            ]
          }
        ]
      }
      """
        .trimIndent()

    val animation = Animation.decodeFromString(json)
    val shapeLayer = animation.layers[0] as ShapeLayer
    assertThat(shapeLayer.shapes).hasSize(1)
    val gs = shapeLayer.shapes[0] as GradientStroke
    assertThat(gs.type).isEqualTo(ShapeType.GradientStroke)
    assertThat(gs.gradientType).isEqualTo(GradientType.Radial)
    assertThat((gs.strokeWidth as StaticScalarProperty).value).isEqualTo(4f)
    assertThat((gs.opacity as StaticScalarProperty).value).isEqualTo(80f)
    assertThat((gs.colors as StaticGradientProperty).value.numberOfColors).isEqualTo(2)
  }

  @Test
  fun stroke_deserializes() {
    val json =
      """
      {
        "v": "5.7.0",
        "fr": 30,
        "ip": 0,
        "op": 30,
        "w": 100,
        "h": 100,
        "layers": [
          {
            "ty": 4,
            "nm": "StrokeLayer",
            "shapes": [
              {
                "ty": "st",
                "nm": "SolidStroke",
                "c": { "k": [1.0, 0.0, 0.0, 1.0] },
                "o": { "k": 90.0 },
                "w": { "k": 3.5 },
                "lc": 2,
                "lj": 2,
                "ml": { "k": 4.0 },
                "d": [
                  { "nm": "dash 1", "n": "d", "v": { "k": 10.0 } },
                  { "nm": "gap 1", "n": "g", "v": { "k": 5.0 } }
                ]
              }
            ]
          }
        ]
      }
      """
        .trimIndent()

    val animation = Animation.decodeFromString(json)
    val shapeLayer = animation.layers[0] as ShapeLayer
    assertThat(shapeLayer.shapes).hasSize(1)
    val stroke = shapeLayer.shapes[0] as Stroke
    assertThat(stroke.type).isEqualTo(ShapeType.Stroke)
    assertThat(stroke.name).isEqualTo("SolidStroke")
    assertThat((stroke.opacity as StaticScalarProperty).value).isEqualTo(90f)
    assertThat((stroke.strokeWidth as StaticScalarProperty).value).isEqualTo(3.5f)
    assertThat(stroke.lineCap).isEqualTo(LineCap.Round)
    assertThat(stroke.lineJoin).isEqualTo(LineJoin.Round)
    assertThat((stroke.miterLimit as StaticScalarProperty).value).isEqualTo(4f)
    assertThat(stroke.dashes).hasSize(2)
    assertThat(stroke.dashes?.get(0)?.dashType).isEqualTo("d")
    assertThat((stroke.dashes?.get(0)?.value as StaticScalarProperty).value).isEqualTo(10f)
  }

  @Test
  fun fillAndGradientFill_withFillRule_deserializes() {
    val json =
      """
      {
        "v": "5.7.0",
        "fr": 30,
        "ip": 0,
        "op": 30,
        "w": 100,
        "h": 100,
        "layers": [
          {
            "ty": 4,
            "nm": "FillRuleLayer",
            "shapes": [
              {
                "ty": "fl",
                "nm": "EvenOddFill",
                "c": { "k": [0.0, 1.0, 0.0, 1.0] },
                "r": 2
              },
              {
                "ty": "gf",
                "nm": "EvenOddGradientFill",
                "t": 1,
                "s": { "k": [0.0, 0.0] },
                "e": { "k": [100.0, 100.0] },
                "g": {
                  "p": 2,
                  "k": [0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0]
                },
                "r": 2
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
    val fill = shapeLayer.shapes[0] as Fill
    assertThat(fill.fillRule).isEqualTo(FillRule.EvenOdd)
    val gf = shapeLayer.shapes[1] as GradientFill
    assertThat(gf.fillRule).isEqualTo(FillRule.EvenOdd)
  }

  @Test
  fun transform_withSkewAndSkewAxis_deserializes() {
    val json =
      """
      {
        "v": "5.7.0",
        "fr": 30,
        "ip": 0,
        "op": 30,
        "w": 100,
        "h": 100,
        "layers": [
          {
            "ty": 4,
            "nm": "TransformLayer",
            "shapes": [
              {
                "ty": "tr",
                "nm": "SkewedTransform",
                "p": { "k": [50.0, 50.0] },
                "sk": { "k": 15.0 },
                "sa": { "k": 45.0 }
              }
            ]
          }
        ]
      }
      """
        .trimIndent()

    val animation = Animation.decodeFromString(json)
    val shapeLayer = animation.layers[0] as ShapeLayer
    val tr = shapeLayer.shapes[0] as Transform
    assertThat(tr.type).isEqualTo(ShapeType.Transform)
    assertThat(tr.skew).isNotNull()
    assertThat((tr.skew as StaticScalarProperty).value).isEqualTo(15f)
    assertThat(tr.skewAxis).isNotNull()
    assertThat((tr.skewAxis as StaticScalarProperty).value).isEqualTo(45f)
  }

  @Test
  fun shapeModifiers_deserializes() {
    val json =
      """
      {
        "v": "5.7.0",
        "fr": 30,
        "ip": 0,
        "op": 30,
        "w": 100,
        "h": 100,
        "layers": [
          {
            "ty": 4,
            "nm": "ModifiersLayer",
            "shapes": [
              {
                "ty": "tm",
                "nm": "TrimPathMod",
                "s": { "k": 10.0 },
                "e": { "k": 90.0 },
                "o": { "k": 5.0 },
                "m": 2
              },
              {
                "ty": "rp",
                "nm": "RepeaterMod",
                "c": { "k": 3.0 },
                "o": { "k": 1.0 },
                "m": 1,
                "tr": {
                  "p": { "k": [20.0, 0.0] }
                }
              },
              {
                "ty": "rd",
                "nm": "RoundedCornersMod",
                "r": { "k": 8.0 }
              },
              {
                "ty": "mm",
                "nm": "MergePathsMod",
                "mm": 4
              },
              {
                "ty": "op",
                "nm": "OffsetPathMod",
                "a": { "k": 12.0 },
                "lj": 1,
                "ml": { "k": 5.0 }
              },
              {
                "ty": "pb",
                "nm": "PuckerBloatMod",
                "a": { "k": -25.0 }
              },
              {
                "ty": "tw",
                "nm": "TwistMod",
                "a": { "k": 90.0 },
                "c": { "k": [50.0, 50.0] }
              },
              {
                "ty": "zz",
                "nm": "ZigZagMod",
                "s": { "k": 15.0 },
                "r": { "k": 4.0 },
                "pt": 2
              },
              {
                "ty": "no",
                "nm": "NoStylePlaceholder"
              }
            ]
          }
        ]
      }
      """
        .trimIndent()

    val animation = Animation.decodeFromString(json)
    val shapeLayer = animation.layers[0] as ShapeLayer
    assertThat(shapeLayer.shapes).hasSize(9)

    val tm = shapeLayer.shapes[0] as TrimPath
    assertThat(tm.type).isEqualTo(ShapeType.TrimPath)
    assertThat((tm.start as StaticScalarProperty).value).isEqualTo(10f)
    assertThat((tm.end as StaticScalarProperty).value).isEqualTo(90f)
    assertThat((tm.offset as StaticScalarProperty).value).isEqualTo(5f)
    assertThat(tm.mode).isEqualTo(TrimMode.Individually)

    val rp = shapeLayer.shapes[1] as Repeater
    assertThat(rp.type).isEqualTo(ShapeType.Repeater)
    assertThat((rp.copies as StaticScalarProperty).value).isEqualTo(3f)
    assertThat((rp.offset as StaticScalarProperty).value).isEqualTo(1f)
    assertThat(rp.composite).isEqualTo(CompositeMode.Above)
    assertThat(rp.transform).isNotNull()

    val rd = shapeLayer.shapes[2] as RoundedCorners
    assertThat(rd.type).isEqualTo(ShapeType.RoundedCorners)
    assertThat((rd.radius as StaticScalarProperty).value).isEqualTo(8f)

    val mm = shapeLayer.shapes[3] as MergePaths
    assertThat(mm.type).isEqualTo(ShapeType.MergePaths)
    assertThat(mm.mode).isEqualTo(MergeMode.Intersect)

    val op = shapeLayer.shapes[4] as OffsetPath
    assertThat(op.type).isEqualTo(ShapeType.OffsetPath)
    assertThat((op.amount as StaticScalarProperty).value).isEqualTo(12f)
    assertThat(op.lineJoin).isEqualTo(LineJoin.Miter)
    assertThat((op.miterLimit as StaticScalarProperty).value).isEqualTo(5f)

    val pb = shapeLayer.shapes[5] as PuckerBloat
    assertThat(pb.type).isEqualTo(ShapeType.PuckerBloat)
    assertThat((pb.amount as StaticScalarProperty).value).isEqualTo(-25f)

    val tw = shapeLayer.shapes[6] as Twist
    assertThat(tw.type).isEqualTo(ShapeType.Twist)
    assertThat((tw.angle as StaticScalarProperty).value).isEqualTo(90f)
    assertThat((tw.center as StaticPositionProperty).value).isEqualTo(listOf(50f, 50f))

    val zz = shapeLayer.shapes[7] as ZigZag
    assertThat(zz.type).isEqualTo(ShapeType.ZigZag)
    assertThat((zz.size as StaticScalarProperty).value).isEqualTo(15f)
    assertThat((zz.ridgesPerSegment as StaticScalarProperty).value).isEqualTo(4f)
    assertThat(zz.pointType).isEqualTo(ZigZagType.Smooth)

    val no = shapeLayer.shapes[8] as NoStyle
    assertThat(no.type).isEqualTo(ShapeType.NoStyle)
  }

  @Test
  fun graphicElement_metadataProperties_deserializes() {
    val json =
      """
      {
        "v": "5.7.0",
        "fr": 30,
        "ip": 0,
        "op": 30,
        "w": 100,
        "h": 100,
        "layers": [
          {
            "ty": 4,
            "nm": "MetadataLayer",
            "shapes": [
              {
                "ty": "rc",
                "nm": "RectWithMeta",
                "mn": "ADBE Vector Shape - Rect",
                "ix": 3,
                "cix": 2,
                "d": 1,
                "s": { "k": [10.0, 10.0] }
              }
            ]
          }
        ]
      }
      """
        .trimIndent()

    val animation = Animation.decodeFromString(json)
    val shapeLayer = animation.layers[0] as ShapeLayer
    val rect = shapeLayer.shapes[0] as Rectangle
    assertThat(rect.name).isEqualTo("RectWithMeta")
    assertThat(rect.matchName).isEqualTo("ADBE Vector Shape - Rect")
    assertThat(rect.index).isEqualTo(3)
    assertThat(rect.propertyIndex).isEqualTo(2)
    assertThat(rect.direction).isEqualTo(1)
  }

  @Test
  fun shapeLayer_withFloatTimings_deserializes() {
    val json =
      """
      {
        "v": "5.7.0",
        "fr": 30,
        "ip": 0,
        "op": 60,
        "w": 100,
        "h": 100,
        "layers": [
          {
            "ty": 4,
            "nm": "FloatTimingShapeLayer",
            "ip": 12.5,
            "op": 45.0,
            "st": 5.5,
            "sr": 1.5,
            "ao": 1,
            "bm": 1,
            "tt": 2,
            "tp": 3,
            "td": 1,
            "ddd": 1,
            "shapes": []
          }
        ]
      }
      """
        .trimIndent()

    val animation = Animation.decodeFromString(json)
    assertThat(animation.layers).hasSize(1)
    val layer = animation.layers[0] as ShapeLayer
    assertThat(layer.name).isEqualTo("FloatTimingShapeLayer")
    assertThat(layer.type).isEqualTo(LayerType.Shape)
    assertThat(layer.startFrame).isEqualTo(12.5f)
    assertThat(layer.endFrame).isEqualTo(45.0f)
    assertThat(layer.startTime).isEqualTo(5.5f)
    assertThat(layer.timeStretch).isEqualTo(1.5f)
    assertThat(layer.autoOrient).isEqualTo(1)
    assertThat(layer.blendMode).isEqualTo(BlendMode.Multiply)
    assertThat(layer.matteMode).isEqualTo(MatteMode.InvertedAlpha)
    assertThat(layer.matteParent).isEqualTo(3)
    assertThat(layer.matteTarget).isEqualTo(1)
    assertThat(layer.is3d).isEqualTo(1)
  }

  @Test
  fun solidColorLayer_deserializes() {
    val json =
      """
      {
        "v": "5.7.0",
        "fr": 30,
        "ip": 0,
        "op": 60,
        "w": 200,
        "h": 200,
        "layers": [
          {
            "ty": 1,
            "nm": "BackgroundSolid",
            "sc": "#FF5722",
            "sw": 200.0,
            "sh": 200.0,
            "ip": 0.0,
            "op": 60.0
          }
        ]
      }
      """
        .trimIndent()

    val animation = Animation.decodeFromString(json)
    assertThat(animation.layers).hasSize(1)
    val layer = animation.layers[0] as SolidColorLayer
    assertThat(layer.name).isEqualTo("BackgroundSolid")
    assertThat(layer.type).isEqualTo(LayerType.Solid)
    assertThat(layer.solidColor).isEqualTo("#FF5722")
    assertThat(layer.solidWidth).isEqualTo(200f)
    assertThat(layer.solidHeight).isEqualTo(200f)
    assertThat(layer.startFrame).isEqualTo(0f)
    assertThat(layer.endFrame).isEqualTo(60f)
  }

  @Test
  fun nullLayer_withParentAndTransform_deserializes() {
    val json =
      """
      {
        "v": "5.7.0",
        "fr": 30,
        "ip": 0,
        "op": 60,
        "w": 100,
        "h": 100,
        "layers": [
          {
            "ty": 3,
            "nm": "ControllerNull",
            "ind": 1,
            "parent": 2,
            "ks": {
              "p": { "k": [50.0, 50.0] },
              "r": { "k": 45.0 }
            }
          }
        ]
      }
      """
        .trimIndent()

    val animation = Animation.decodeFromString(json)
    assertThat(animation.layers).hasSize(1)
    val layer = animation.layers[0] as NullLayer
    assertThat(layer.name).isEqualTo("ControllerNull")
    assertThat(layer.type).isEqualTo(LayerType.Null)
    assertThat(layer.index).isEqualTo(1)
    assertThat(layer.parent).isEqualTo(2)
    assertThat(layer.transform).isNotNull()
    assertThat((layer.transform?.rotation as StaticScalarProperty).value).isEqualTo(45f)
  }

  @Test
  fun precompLayer_deserializes() {
    val json =
      """
      {
        "v": "5.7.0",
        "fr": 30,
        "ip": 0,
        "op": 60,
        "w": 100,
        "h": 100,
        "layers": [
          {
            "ty": 0,
            "nm": "NestedComposition",
            "refId": "comp_1",
            "w": 1920.0,
            "h": 1080.0,
            "tm": {
              "a": 0,
              "k": 12.0
            }
          }
        ]
      }
      """
        .trimIndent()

    val animation = Animation.decodeFromString(json)
    assertThat(animation.layers).hasSize(1)
    val layer = animation.layers[0] as PrecompLayer
    assertThat(layer.name).isEqualTo("NestedComposition")
    assertThat(layer.type).isEqualTo(LayerType.Precomposition)
    assertThat(layer.refId).isEqualTo("comp_1")
    assertThat(layer.width).isEqualTo(1920f)
    assertThat(layer.height).isEqualTo(1080f)
    assertThat(layer.timeRemap).isNotNull()
    assertThat((layer.timeRemap as StaticScalarProperty).value).isEqualTo(12f)
  }

  @Test
  fun imageLayer_deserializes() {
    val json =
      """
      {
        "v": "5.7.0",
        "fr": 30,
        "ip": 0,
        "op": 60,
        "w": 100,
        "h": 100,
        "layers": [
          {
            "ty": 2,
            "nm": "BitmapImageLayer",
            "refId": "image_0"
          }
        ]
      }
      """
        .trimIndent()

    val animation = Animation.decodeFromString(json)
    assertThat(animation.layers).hasSize(1)
    val layer = animation.layers[0] as ImageLayer
    assertThat(layer.name).isEqualTo("BitmapImageLayer")
    assertThat(layer.type).isEqualTo(LayerType.Image)
    assertThat(layer.refId).isEqualTo("image_0")
  }

  @Test
  fun textLayer_deserializes() {
    val json =
      """
      {
        "v": "5.7.0",
        "fr": 30,
        "ip": 0,
        "op": 60,
        "w": 100,
        "h": 100,
        "layers": [
          {
            "ty": 5,
            "nm": "TypographyLayer",
            "ip": 0.0,
            "op": 30.0
          }
        ]
      }
      """
        .trimIndent()

    val animation = Animation.decodeFromString(json)
    assertThat(animation.layers).hasSize(1)
    val layer = animation.layers[0] as TextLayer
    assertThat(layer.name).isEqualTo("TypographyLayer")
    assertThat(layer.type).isEqualTo(LayerType.Text)
    assertThat(layer.startFrame).isEqualTo(0f)
    assertThat(layer.endFrame).isEqualTo(30f)
  }

  @Test
  fun blendModeAndMatteModeEnums_deserializes() {
    assertThat(BlendMode.fromValueOrNull(0)).isEqualTo(BlendMode.Normal)
    assertThat(BlendMode.fromValueOrNull(1)).isEqualTo(BlendMode.Multiply)
    assertThat(BlendMode.fromValueOrNull(2)).isEqualTo(BlendMode.Screen)
    assertThat(BlendMode.fromValueOrNull(3)).isEqualTo(BlendMode.Overlay)
    assertThat(BlendMode.fromValueOrNull(4)).isEqualTo(BlendMode.Darken)
    assertThat(BlendMode.fromValueOrNull(5)).isEqualTo(BlendMode.Lighten)
    assertThat(BlendMode.fromValueOrNull(6)).isEqualTo(BlendMode.ColorDodge)
    assertThat(BlendMode.fromValueOrNull(7)).isEqualTo(BlendMode.ColorBurn)
    assertThat(BlendMode.fromValueOrNull(8)).isEqualTo(BlendMode.HardLight)
    assertThat(BlendMode.fromValueOrNull(9)).isEqualTo(BlendMode.SoftLight)
    assertThat(BlendMode.fromValueOrNull(10)).isEqualTo(BlendMode.Difference)
    assertThat(BlendMode.fromValueOrNull(11)).isEqualTo(BlendMode.Exclusion)
    assertThat(BlendMode.fromValueOrNull(12)).isEqualTo(BlendMode.Hue)
    assertThat(BlendMode.fromValueOrNull(13)).isEqualTo(BlendMode.Saturation)
    assertThat(BlendMode.fromValueOrNull(14)).isEqualTo(BlendMode.Color)
    assertThat(BlendMode.fromValueOrNull(15)).isEqualTo(BlendMode.Luminosity)
    assertThat(BlendMode.fromValueOrNull(16)).isEqualTo(BlendMode.Add)
    assertThat(BlendMode.fromValueOrNull(17)).isEqualTo(BlendMode.HardMix)
    assertThat(BlendMode.fromValueOrNull(99)).isNull()

    assertThat(LottieDecoder.json.decodeFromString(BlendModeSerializer, "0"))
      .isEqualTo(BlendMode.Normal)
    assertThat(LottieDecoder.json.decodeFromString(BlendModeSerializer, "1.0"))
      .isEqualTo(BlendMode.Multiply)
    assertThat(LottieDecoder.json.decodeFromString(BlendModeSerializer, "99"))
      .isEqualTo(BlendMode.Normal)

    assertThat(MatteMode.fromValueOrNull(0)).isEqualTo(MatteMode.Normal)
    assertThat(MatteMode.fromValueOrNull(1)).isEqualTo(MatteMode.Alpha)
    assertThat(MatteMode.fromValueOrNull(2)).isEqualTo(MatteMode.InvertedAlpha)
    assertThat(MatteMode.fromValueOrNull(3)).isEqualTo(MatteMode.Luma)
    assertThat(MatteMode.fromValueOrNull(4)).isEqualTo(MatteMode.InvertedLuma)
    assertThat(MatteMode.fromValueOrNull(99)).isNull()

    assertThat(LottieDecoder.json.decodeFromString(MatteModeSerializer, "0"))
      .isEqualTo(MatteMode.Normal)
    assertThat(LottieDecoder.json.decodeFromString(MatteModeSerializer, "2.0"))
      .isEqualTo(MatteMode.InvertedAlpha)
    assertThat(LottieDecoder.json.decodeFromString(MatteModeSerializer, "99"))
      .isEqualTo(MatteMode.Normal)
  }
}
