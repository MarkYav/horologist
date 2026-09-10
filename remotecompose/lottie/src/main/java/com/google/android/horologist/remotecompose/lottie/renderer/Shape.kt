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

package com.google.android.horologist.remotecompose.lottie.renderer

import android.annotation.SuppressLint
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.runtime.Composable
import com.google.android.horologist.remotecompose.lottie.LocalAnimationSettings
import com.google.android.horologist.remotecompose.lottie.LottieSettings
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.GraphicElement
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.Ellipse
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.Path
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.PolyStar
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.Rectangle
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.grouping.Group
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.grouping.Transform
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.MergePaths
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.OffsetPath
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.PuckerBloat
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.Repeater
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.RoundedCorners
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.TrimPath
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.Twist
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.ZigZag
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.Fill
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.FillRule
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.GradientFill
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.GradientStroke
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.Stroke
import com.google.android.horologist.remotecompose.lottie.format.mask.Mask
import com.google.android.horologist.remotecompose.lottie.format.mask.MaskMode
import com.google.android.horologist.remotecompose.lottie.renderer.layers.MatteContext
import com.google.android.horologist.remotecompose.lottie.renderer.layers.applyLayerMasks
import com.google.android.horologist.remotecompose.lottie.renderer.layers.applyMatteClip
import com.google.android.horologist.remotecompose.lottie.renderer.modifiers.RepeatedShapeInstance
import com.google.android.horologist.remotecompose.lottie.renderer.modifiers.evaluateMergePaths
import com.google.android.horologist.remotecompose.lottie.renderer.modifiers.evaluateOffsetPath
import com.google.android.horologist.remotecompose.lottie.renderer.modifiers.evaluatePuckerBloat
import com.google.android.horologist.remotecompose.lottie.renderer.modifiers.evaluateRepeater
import com.google.android.horologist.remotecompose.lottie.renderer.modifiers.evaluateTwist
import com.google.android.horologist.remotecompose.lottie.renderer.modifiers.evaluateZigZag
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animateColor
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animateGradient
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animatePosition
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animateScalar
import com.google.android.horologist.remotecompose.lottie.renderer.shapes.ellipse
import com.google.android.horologist.remotecompose.lottie.renderer.shapes.path
import com.google.android.horologist.remotecompose.lottie.renderer.shapes.polyStar
import com.google.android.horologist.remotecompose.lottie.renderer.shapes.rectangle

internal data class StyledShapes(val shapes: List<RemoteShape>, val style: RemoteStyle)

/** Renders a list of Lottie Shapes to the RemoteCanvas. */
@SuppressLint("RestrictedApi")
@Composable
@RemoteComposable
internal fun RenderShapes(
  shapes: List<GraphicElement>,
  transformStack: List<Transform>,
  matteContext: MatteContext? = null,
  layerVisibility: RemoteFloat = 1f.rf,
  masks: List<Mask> = emptyList(),
) {
  val animationSettings = LocalAnimationSettings.current
  val shapeGroups = gatherShapes(shapes, animationSettings)

  // Aspect-ratio scaling and centering is applied once, at the top level, by the
  // drawWithContent modifier in LottieAnimation - shapes draw in raw Lottie coordinates here.
  RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) {
    val hasMasks = masks.any { it.mode != MaskMode.None && it.path != null }
    val needsSave = matteContext != null || hasMasks
    if (needsSave) {
      remoteCanvas.save()
    }

    if (matteContext != null) {
      applyMatteClip(matteContext, animationSettings, remoteCanvas)
    }

    if (hasMasks) {
      for (transform in transformStack) {
        transform(transform, null, animationSettings, remoteCanvas)
      }
      applyLayerMasks(masks, animationSettings, remoteCanvas)
      for (transform in transformStack.reversed()) {
        inverseTransform(transform, animationSettings, remoteCanvas)
      }
    }

    val layerOpacity =
      (transformStack.lastOrNull()?.opacity?.let { animateScalar(it, animationSettings) / 100f }
        ?: 1f.rf) * layerVisibility

    for (shapeGroup in shapeGroups) {
      val paint = shapeGroup.style.getPaint(layerOpacity)

      for (transform in transformStack) {
        remoteCanvas.save()
        transform(transform, null, animationSettings, remoteCanvas)
      }

      usePaint(paint) {
        for (shape in shapeGroup.shapes) {
          shape.draw(this, remoteCanvas, layerOpacity)
        }
      }

      for (transform in transformStack) {
        remoteCanvas.restore()
      }
    }

    if (needsSave) {
      remoteCanvas.restore()
    }
  }
}

/** Overloaded [RenderShapes] accepting a unified [RenderContext]. */
@SuppressLint("RestrictedApi")
@Composable
@RemoteComposable
internal fun RenderShapes(
  shapes: List<GraphicElement>,
  renderContext: RenderContext,
  masks: List<Mask> = emptyList(),
) {
  RenderShapes(
    shapes = shapes,
    transformStack = renderContext.transformStack,
    matteContext = renderContext.matteContext,
    layerVisibility = renderContext.effectiveOpacity,
    masks = masks,
  )
}

@SuppressLint("RestrictedApi")
internal fun gatherShapes(
  shapes: List<GraphicElement>,
  animationSettings: LottieSettings,
  parentTrimPath: TrimPath? = null,
  parentRoundedCorners: RoundedCorners? = null,
  inheritedStyle: RemoteStyle? = null,
): List<StyledShapes> {
  val shapeGroups = mutableListOf<StyledShapes>()
  var currentGeometries = mutableListOf<RepeatedShapeInstance>()
  var currentGroups = mutableListOf<Group>()
  val activeTrimPath: TrimPath? =
    shapes.filterIsInstance<TrimPath>().firstOrNull { it.hidden?.constantValue != true }
      ?: parentTrimPath
  val activeRoundedCorners: RoundedCorners? =
    shapes.filterIsInstance<RoundedCorners>().firstOrNull { it.hidden?.constantValue != true }
      ?: parentRoundedCorners
  var hasEmittedStyle = false

  for (shape in shapes) {
    when (shape) {
      is TrimPath -> {}
      is RoundedCorners -> {}
      is Repeater -> {
        if (shape.hidden?.constantValue != true && currentGeometries.isNotEmpty()) {
          val baseShapes = currentGeometries.map { it.shape }
          currentGeometries = evaluateRepeater(baseShapes, shape, animationSettings).toMutableList()
        }
      }
      is MergePaths -> {
        if (shape.hidden?.constantValue != true && currentGeometries.isNotEmpty()) {
          val baseShapes = currentGeometries.map { it.shape }
          val mergedShapes = evaluateMergePaths(baseShapes, shape, animationSettings)
          currentGeometries = mergedShapes.map { RepeatedShapeInstance(it) }.toMutableList()
        }
      }
      is ZigZag -> {
        if (shape.hidden?.constantValue != true && currentGeometries.isNotEmpty()) {
          val baseShapes = currentGeometries.map { it.shape }
          val modified = evaluateZigZag(baseShapes, shape, animationSettings)
          currentGeometries = modified.map { RepeatedShapeInstance(it) }.toMutableList()
        }
      }
      is PuckerBloat -> {
        if (shape.hidden?.constantValue != true && currentGeometries.isNotEmpty()) {
          val baseShapes = currentGeometries.map { it.shape }
          val modified = evaluatePuckerBloat(baseShapes, shape, animationSettings)
          currentGeometries = modified.map { RepeatedShapeInstance(it) }.toMutableList()
        }
      }
      is Twist -> {
        if (shape.hidden?.constantValue != true && currentGeometries.isNotEmpty()) {
          val baseShapes = currentGeometries.map { it.shape }
          val modified = evaluateTwist(baseShapes, shape, animationSettings)
          currentGeometries = modified.map { RepeatedShapeInstance(it) }.toMutableList()
        }
      }
      is OffsetPath -> {
        if (shape.hidden?.constantValue != true && currentGeometries.isNotEmpty()) {
          val baseShapes = currentGeometries.map { it.shape }
          val modified = evaluateOffsetPath(baseShapes, shape, animationSettings)
          currentGeometries = modified.map { RepeatedShapeInstance(it) }.toMutableList()
        }
      }
      is Path -> {
        if (hasEmittedStyle) {
          currentGeometries = mutableListOf()
          currentGroups = mutableListOf()
          hasEmittedStyle = false
        }
        val remoteShape = path(shape, animationSettings, activeTrimPath, activeRoundedCorners)
        if (remoteShape != null) {
          currentGeometries.add(RepeatedShapeInstance(remoteShape))
        }
      }
      is Rectangle -> {
        if (hasEmittedStyle) {
          currentGeometries = mutableListOf()
          currentGroups = mutableListOf()
          hasEmittedStyle = false
        }
        val remoteShape = rectangle(shape, animationSettings, activeTrimPath, activeRoundedCorners)
        if (remoteShape != null) {
          currentGeometries.add(RepeatedShapeInstance(remoteShape))
        }
      }
      is Ellipse -> {
        if (hasEmittedStyle) {
          currentGeometries = mutableListOf()
          currentGroups = mutableListOf()
          hasEmittedStyle = false
        }
        val remoteShape = ellipse(shape, animationSettings, activeTrimPath, activeRoundedCorners)
        if (remoteShape != null) {
          currentGeometries.add(RepeatedShapeInstance(remoteShape))
        }
      }
      is PolyStar -> {
        if (hasEmittedStyle) {
          currentGeometries = mutableListOf()
          currentGroups = mutableListOf()
          hasEmittedStyle = false
        }
        val remoteShape = polyStar(shape, animationSettings, activeTrimPath, activeRoundedCorners)
        if (remoteShape != null) {
          currentGeometries.add(RepeatedShapeInstance(remoteShape))
        }
      }
      is Group -> {
        if (hasEmittedStyle) {
          currentGeometries = mutableListOf()
          currentGroups = mutableListOf()
          hasEmittedStyle = false
        }
        val groupShape =
          group(shape, animationSettings, activeTrimPath, activeRoundedCorners, inheritedStyle)
        if (groupShape != null) {
          shapeGroups.add(StyledShapes(listOf(groupShape), inheritedStyle ?: NoopStyle()))
        }
        currentGroups.add(shape)
      }
      is Fill -> {
        if (shape.hidden?.constantValue != true) {
          val fill = fill(shape, animationSettings)
          emitStyledShapes(
            shapeGroups,
            currentGeometries,
            currentGroups,
            fill,
            animationSettings,
            activeTrimPath,
            activeRoundedCorners,
          )
          hasEmittedStyle = true
        }
      }
      is Stroke -> {
        if (shape.hidden?.constantValue != true) {
          val stroke = stroke(shape, animationSettings)
          emitStyledShapes(
            shapeGroups,
            currentGeometries,
            currentGroups,
            stroke,
            animationSettings,
            activeTrimPath,
            activeRoundedCorners,
          )
          hasEmittedStyle = true
        }
      }
      is GradientFill -> {
        if (shape.hidden?.constantValue != true) {
          val gradientFill = gradientFill(shape, animationSettings)
          emitStyledShapes(
            shapeGroups,
            currentGeometries,
            currentGroups,
            gradientFill,
            animationSettings,
            activeTrimPath,
            activeRoundedCorners,
          )
          hasEmittedStyle = true
        }
      }
      is GradientStroke -> {
        if (shape.hidden?.constantValue != true) {
          val gradientStroke = gradientStroke(shape, animationSettings)
          emitStyledShapes(
            shapeGroups,
            currentGeometries,
            currentGroups,
            gradientStroke,
            animationSettings,
            activeTrimPath,
            activeRoundedCorners,
          )
          hasEmittedStyle = true
        }
      }
      is Transform -> {}
      else -> {}
    }
  }

  if (inheritedStyle != null && currentGeometries.isNotEmpty()) {
    emitStyledShapes(
      shapeGroups,
      currentGeometries,
      emptyList(),
      inheritedStyle,
      animationSettings,
      activeTrimPath,
      activeRoundedCorners,
    )
  }

  return shapeGroups.reversed()
}

internal fun gatherShapesForTest(
  shapes: List<GraphicElement>,
  animationSettings: LottieSettings,
): List<StyledShapes> = gatherShapes(shapes, animationSettings)

@SuppressLint("RestrictedApi")
private fun emitStyledShapes(
  shapeGroups: MutableList<StyledShapes>,
  currentGeometries: List<RepeatedShapeInstance>,
  currentGroups: List<Group>,
  style: RemoteStyle,
  animationSettings: LottieSettings,
  activeTrimPath: TrimPath?,
  activeRoundedCorners: RoundedCorners? = null,
) {
  val fillRule =
    (style as? RemoteFill)?.fillRule
      ?: (style as? RemoteGradientFill)?.fillRule
      ?: (style as? RemoteStyleWithOpacity)?.let {
        (it.baseStyle as? RemoteFill)?.fillRule ?: (it.baseStyle as? RemoteGradientFill)?.fillRule
      }
      ?: FillRule.NonZero

  val styledGeometries =
    if (fillRule != FillRule.NonZero) {
      currentGeometries.map {
        RepeatedShapeInstance(it.shape.withFillRule(fillRule), it.opacityMultiplier)
      }
    } else {
      currentGeometries
    }

  val hasVaryingOpacity = styledGeometries.any { it.opacityMultiplier.constantValueOrNull != 1f }
  if (hasVaryingOpacity) {
    for (instance in styledGeometries.reversed()) {
      val instanceStyle =
        if (instance.opacityMultiplier.constantValueOrNull == 1f) {
          style
        } else {
          RemoteStyleWithOpacity(style, instance.opacityMultiplier)
        }
      shapeGroups.add(StyledShapes(listOf(instance.shape), instanceStyle))
    }
  } else if (styledGeometries.isNotEmpty()) {
    shapeGroups.add(StyledShapes(styledGeometries.map { it.shape }, style))
  }

  val groupShapes = mutableListOf<RemoteShape>()
  for (group in currentGroups) {
    groupShapes.addAll(
      evaluateGroupGeometries(group, animationSettings, activeTrimPath, activeRoundedCorners)
    )
  }
  if (groupShapes.isNotEmpty()) {
    val styledGroupShapes =
      if (fillRule != FillRule.NonZero) {
        groupShapes.map { it.withFillRule(fillRule) }
      } else {
        groupShapes
      }
    shapeGroups.add(StyledShapes(styledGroupShapes, style))
  }
}

@SuppressLint("RestrictedApi")
internal fun gatherShapesForTest(
  shapes: List<GraphicElement>,
  animationSettings: LottieSettings,
  inheritedStyle: RemoteStyle? = null,
): List<StyledShapes> = gatherShapes(shapes, animationSettings, inheritedStyle = inheritedStyle)

@SuppressLint("RestrictedApi")
private fun evaluateGroupGeometries(
  group: Group,
  animationSettings: LottieSettings,
  parentTrimPath: TrimPath? = null,
  parentRoundedCorners: RoundedCorners? = null,
): List<RemoteShape> {
  if (group.hidden?.constantValue == true) return emptyList()
  val activeTrimPath =
    group.shapes.filterIsInstance<TrimPath>().firstOrNull { it.hidden?.constantValue != true }
      ?: parentTrimPath
  val activeRoundedCorners =
    group.shapes.filterIsInstance<RoundedCorners>().firstOrNull { it.hidden?.constantValue != true }
      ?: parentRoundedCorners
  var geometries = mutableListOf<RemoteShape>()
  for (shape in group.shapes) {
    when (shape) {
      is Path -> {
        val s = path(shape, animationSettings, activeTrimPath, activeRoundedCorners)
        if (s != null) geometries.add(s)
      }
      is Rectangle -> {
        val s = rectangle(shape, animationSettings, activeTrimPath, activeRoundedCorners)
        if (s != null) geometries.add(s)
      }
      is Ellipse -> {
        val s = ellipse(shape, animationSettings, activeTrimPath, activeRoundedCorners)
        if (s != null) geometries.add(s)
      }
      is PolyStar -> {
        val s = polyStar(shape, animationSettings, activeTrimPath, activeRoundedCorners)
        if (s != null) geometries.add(s)
      }
      is Group -> {
        val nested =
          evaluateGroupGeometries(shape, animationSettings, activeTrimPath, activeRoundedCorners)
        geometries.addAll(nested)
      }
      is Repeater -> {
        if (shape.hidden?.constantValue != true && geometries.isNotEmpty()) {
          geometries =
            evaluateRepeater(geometries, shape, animationSettings).map { it.shape }.toMutableList()
        }
      }
      is MergePaths -> {
        if (shape.hidden?.constantValue != true && geometries.isNotEmpty()) {
          geometries = evaluateMergePaths(geometries, shape, animationSettings).toMutableList()
        }
      }
      is ZigZag -> {
        if (shape.hidden?.constantValue != true && geometries.isNotEmpty()) {
          geometries = evaluateZigZag(geometries, shape, animationSettings).toMutableList()
        }
      }
      is PuckerBloat -> {
        if (shape.hidden?.constantValue != true && geometries.isNotEmpty()) {
          geometries = evaluatePuckerBloat(geometries, shape, animationSettings).toMutableList()
        }
      }
      is Twist -> {
        if (shape.hidden?.constantValue != true && geometries.isNotEmpty()) {
          geometries = evaluateTwist(geometries, shape, animationSettings).toMutableList()
        }
      }
      is OffsetPath -> {
        if (shape.hidden?.constantValue != true && geometries.isNotEmpty()) {
          geometries = evaluateOffsetPath(geometries, shape, animationSettings).toMutableList()
        }
      }
      else -> {}
    }
  }
  return geometries
}

private fun group(
  group: Group,
  animationSettings: LottieSettings,
  parentTrimPath: TrimPath? = null,
  parentRoundedCorners: RoundedCorners? = null,
  inheritedStyle: RemoteStyle? = null,
): RemoteGroup? {
  if (group.hidden?.constantValue == true) {
    return null
  }

  val transform = group.shapes.filterIsInstance<Transform>().firstOrNull()
  val otherShapes = group.shapes.filter { it !is Transform }
  val styledShapes =
    gatherShapes(
      otherShapes,
      animationSettings,
      parentTrimPath = parentTrimPath,
      parentRoundedCorners = parentRoundedCorners,
      inheritedStyle = inheritedStyle,
    )
  return RemoteGroup(styledShapes, animationSettings, transform)
}

private fun fill(fill: Fill, animationSettings: LottieSettings): RemoteFill {
  val fillColor = animateColor(fill.color, animationSettings)
  val opacity = animateScalar(fill.opacity, animationSettings)
  return RemoteFill(
    fillColor = fillColor,
    opacity = opacity,
    fillRule = fill.fillRule ?: FillRule.NonZero,
  )
}

private fun gradientFill(
  fill: GradientFill,
  animationSettings: LottieSettings,
): RemoteGradientFill {
  return RemoteGradientFill(
    gradientType = fill.gradientType,
    startPoint = animatePosition(fill.startPoint, animationSettings),
    endPoint = animatePosition(fill.endPoint, animationSettings),
    gradient = animateGradient(fill.colors, animationSettings),
    opacity = animateScalar(fill.opacity, animationSettings),
    fillRule = fill.fillRule ?: FillRule.NonZero,
    highlightAngle = fill.highlightAngle?.let { animateScalar(it, animationSettings) },
    highlightLength = fill.highlightLength?.let { animateScalar(it, animationSettings) },
  )
}

private fun stroke(stroke: Stroke, animationSettings: LottieSettings): RemoteStroke {
  val miterLimitRf =
    stroke.miterLimitAnimatable?.let { animateScalar(it, animationSettings) }
      ?: stroke.miterLimit.rf
  return RemoteStroke(
    strokeColor = animateColor(stroke.color, animationSettings),
    strokeWidth = animateScalar(stroke.strokeWidth, animationSettings),
    opacity = animateScalar(stroke.opacity, animationSettings),
    lineCap = stroke.lineCap,
    lineJoin = stroke.lineJoin,
    miterLimit = miterLimitRf,
    dashPattern = createDashPathEffect(stroke.dashes, animationSettings),
  )
}

private fun gradientStroke(
  stroke: GradientStroke,
  animationSettings: LottieSettings,
): RemoteGradientStroke {
  val miterLimitRf =
    stroke.miterLimitAnimatable?.let { animateScalar(it, animationSettings) }
      ?: stroke.miterLimit.rf
  return RemoteGradientStroke(
    gradientType = stroke.gradientType,
    startPoint = animatePosition(stroke.startPoint, animationSettings),
    endPoint = animatePosition(stroke.endPoint, animationSettings),
    gradient = animateGradient(stroke.colors, animationSettings),
    opacity = animateScalar(stroke.opacity, animationSettings),
    strokeWidth = animateScalar(stroke.strokeWidth, animationSettings),
    lineCap = stroke.lineCap,
    lineJoin = stroke.lineJoin,
    miterLimit = miterLimitRf,
    dashPattern = createDashPathEffect(stroke.dashes, animationSettings),
  )
}
