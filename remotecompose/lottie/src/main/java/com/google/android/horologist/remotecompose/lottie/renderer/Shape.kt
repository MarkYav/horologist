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
import androidx.compose.remote.creation.RemotePath
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ClipOp
import com.google.android.horologist.remotecompose.lottie.LocalAnimationSettings
import com.google.android.horologist.remotecompose.lottie.LottieSettings
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.GraphicElement
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.ShapeType
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
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.toStrokeCap
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.toStrokeJoin
import com.google.android.horologist.remotecompose.lottie.format.layer.MatteMode
import com.google.android.horologist.remotecompose.lottie.format.layer.ShapeLayer
import com.google.android.horologist.remotecompose.lottie.format.layer.SolidColorLayer
import com.google.android.horologist.remotecompose.lottie.format.mask.Mask
import com.google.android.horologist.remotecompose.lottie.format.mask.MaskMode
import com.google.android.horologist.remotecompose.lottie.renderer.layers.MatteContext
import com.google.android.horologist.remotecompose.lottie.renderer.properties.RemoteBezierValue
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animateBezier
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animateColor
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animateGradient
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animatePosition
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animateScalar
import com.google.android.horologist.remotecompose.lottie.renderer.shapes.RepeatedShapeInstance
import com.google.android.horologist.remotecompose.lottie.renderer.shapes.ellipse
import com.google.android.horologist.remotecompose.lottie.renderer.shapes.evaluateMergePaths
import com.google.android.horologist.remotecompose.lottie.renderer.shapes.evaluatePolyStar
import com.google.android.horologist.remotecompose.lottie.renderer.shapes.evaluateRectangle
import com.google.android.horologist.remotecompose.lottie.renderer.shapes.evaluateRepeater
import com.google.android.horologist.remotecompose.lottie.renderer.shapes.path

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
      val paint = shapeGroup.style.getPaint(layerOpacity, this)

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

internal fun gatherShapes(
  shapes: List<GraphicElement>,
  animationSettings: LottieSettings,
  parentTrimPath: TrimPath? = null,
  parentRoundedCorners: RoundedCorners? = null,
): List<StyledShapes> {
  val shapeGroups = mutableListOf<StyledShapes>()
  var currentGeometries = mutableListOf<RepeatedShapeInstance>()
  var activeTrimPath: TrimPath? =
    shapes.filterIsInstance<TrimPath>().firstOrNull { it.hidden != true } ?: parentTrimPath
  var activeRoundedCorners: RoundedCorners? =
    shapes.filterIsInstance<RoundedCorners>().firstOrNull { it.hidden != true }
      ?: parentRoundedCorners

  for (shape in shapes.reversed()) {
    when (shape) {
      is TrimPath -> {
        if (shape.hidden != true) {
          activeTrimPath = shape
        }
      }
      is RoundedCorners -> {
        if (shape.hidden != true) {
          activeRoundedCorners = shape
        }
      }
      is Repeater -> {
        if (shape.hidden != true && currentGeometries.isNotEmpty()) {
          val baseShapes = currentGeometries.map { it.shape }
          currentGeometries = evaluateRepeater(baseShapes, shape, animationSettings).toMutableList()
        }
      }
      is MergePaths -> {
        if (shape.hidden != true && currentGeometries.isNotEmpty()) {
          val baseShapes = currentGeometries.map { it.shape }
          val merged = evaluateMergePaths(baseShapes, shape, animationSettings)
          currentGeometries = merged.map { RepeatedShapeInstance(it) }.toMutableList()
        }
      }
      is Path -> {
        val s = path(shape, animationSettings, activeTrimPath, activeRoundedCorners)
        if (s != null) currentGeometries.add(RepeatedShapeInstance(s))
      }
      is Rectangle -> {
        val s = evaluateRectangle(shape, animationSettings, activeTrimPath, activeRoundedCorners)
        if (s != null) currentGeometries.add(RepeatedShapeInstance(s))
      }
      is Ellipse -> {
        val s = ellipse(shape, animationSettings)
        if (s != null) currentGeometries.add(RepeatedShapeInstance(s))
      }
      is PolyStar -> {
        val s = evaluatePolyStar(shape, animationSettings)
        if (s != null) currentGeometries.add(RepeatedShapeInstance(s))
      }
      is Group -> {
        val s = group(shape, animationSettings, activeTrimPath, activeRoundedCorners)
        if (s != null) currentGeometries.add(RepeatedShapeInstance(s))
      }
      is PuckerBloat,
      is Twist,
      is ZigZag,
      is OffsetPath -> {
        // Extended modifier stubs: preserved gracefully in AST pipeline
      }
      is Fill -> {
        val fill = fill(shape, animationSettings)
        val hasVaryingOpacity = currentGeometries.any {
          it.opacityMultiplier.constantValueOrNull != 1f
        }
        if (hasVaryingOpacity) {
          for (instance in currentGeometries) {
            val instanceStyle =
              if (instance.opacityMultiplier.constantValueOrNull == 1f) {
                fill
              } else {
                RemoteStyleWithOpacity(fill, instance.opacityMultiplier)
              }
            val styled =
              if (fill.fillRule != FillRule.NonZero) {
                listOf(instance.shape.withFillRule(fill.fillRule))
              } else {
                listOf(instance.shape)
              }
            shapeGroups.add(StyledShapes(styled, instanceStyle))
          }
        } else {
          val styled =
            if (fill.fillRule != FillRule.NonZero) {
              currentGeometries.map { it.shape.withFillRule(fill.fillRule) }
            } else {
              currentGeometries.map { it.shape }
            }
          shapeGroups.add(StyledShapes(styled, fill))
        }
        currentGeometries = mutableListOf()
      }
      is GradientFill -> {
        val gradFill = gradientFill(shape, animationSettings)
        val hasVaryingOpacity = currentGeometries.any {
          it.opacityMultiplier.constantValueOrNull != 1f
        }
        if (hasVaryingOpacity) {
          for (instance in currentGeometries) {
            val instanceStyle =
              if (instance.opacityMultiplier.constantValueOrNull == 1f) {
                gradFill
              } else {
                RemoteStyleWithOpacity(gradFill, instance.opacityMultiplier)
              }
            val styled =
              if (gradFill.fillRule != FillRule.NonZero) {
                listOf(instance.shape.withFillRule(gradFill.fillRule))
              } else {
                listOf(instance.shape)
              }
            shapeGroups.add(StyledShapes(styled, instanceStyle))
          }
        } else {
          val styled =
            if (gradFill.fillRule != FillRule.NonZero) {
              currentGeometries.map { it.shape.withFillRule(gradFill.fillRule) }
            } else {
              currentGeometries.map { it.shape }
            }
          shapeGroups.add(StyledShapes(styled, gradFill))
        }
        currentGeometries = mutableListOf()
      }
      is Stroke -> {
        val stroke = stroke(shape, animationSettings)
        val hasVaryingOpacity = currentGeometries.any {
          it.opacityMultiplier.constantValueOrNull != 1f
        }
        if (hasVaryingOpacity) {
          for (instance in currentGeometries) {
            val instanceStyle =
              if (instance.opacityMultiplier.constantValueOrNull == 1f) {
                stroke
              } else {
                RemoteStyleWithOpacity(stroke, instance.opacityMultiplier)
              }
            shapeGroups.add(StyledShapes(listOf(instance.shape), instanceStyle))
          }
        } else {
          shapeGroups.add(StyledShapes(currentGeometries.map { it.shape }, stroke))
        }
        currentGeometries = mutableListOf()
      }
      is GradientStroke -> {
        val gradStroke = gradientStroke(shape, animationSettings)
        val hasVaryingOpacity = currentGeometries.any {
          it.opacityMultiplier.constantValueOrNull != 1f
        }
        if (hasVaryingOpacity) {
          for (instance in currentGeometries) {
            val instanceStyle =
              if (instance.opacityMultiplier.constantValueOrNull == 1f) {
                gradStroke
              } else {
                RemoteStyleWithOpacity(gradStroke, instance.opacityMultiplier)
              }
            shapeGroups.add(StyledShapes(listOf(instance.shape), instanceStyle))
          }
        } else {
          shapeGroups.add(StyledShapes(currentGeometries.map { it.shape }, gradStroke))
        }
        currentGeometries = mutableListOf()
      }
      is Transform -> {} // No-op - handled groups
      else -> {}
    }
  }

  // Groups don't have to have styling information associated with them, because the child nodes
  // can have styles instead. If there's a Group node left over that doesn't have a style, add
  // it to the render tree anyway
  if (currentGeometries.isNotEmpty() && currentGeometries.all { it.shape is RemoteGroup }) {
    shapeGroups.add(StyledShapes(currentGeometries.map { it.shape }, NoopStyle()))
  }

  return shapeGroups
}

internal fun gatherShapesForTest(
  shapes: List<GraphicElement>,
  animationSettings: LottieSettings,
  parentTrimPath: TrimPath? = null,
  parentRoundedCorners: RoundedCorners? = null,
): List<StyledShapes> =
  gatherShapes(shapes.reversed(), animationSettings, parentTrimPath, parentRoundedCorners)

private fun group(
  group: Group,
  animationSettings: LottieSettings,
  parentTrimPath: TrimPath? = null,
  parentRoundedCorners: RoundedCorners? = null,
): RemoteGroup? {
  if (group.hidden == true) {
    return null
  }

  val activeTrimPath =
    group.shapes.filterIsInstance<TrimPath>().firstOrNull { it.hidden != true } ?: parentTrimPath
  val activeRoundedCorners =
    group.shapes.filterIsInstance<RoundedCorners>().firstOrNull { it.hidden != true }
      ?: parentRoundedCorners

  val reversed = group.shapes.reversed()

  if (reversed.firstOrNull()?.type == ShapeType.Transform) {
    val transform = reversed[0] as Transform
    val styledShapes =
      gatherShapes(reversed.drop(1), animationSettings, activeTrimPath, activeRoundedCorners)
    return RemoteGroup(styledShapes, animationSettings, transform)
  } else {
    return RemoteGroup(
      gatherShapes(reversed, animationSettings, activeTrimPath, activeRoundedCorners),
      animationSettings,
      null,
    )
  }
}

private fun fill(fill: Fill, animationSettings: LottieSettings): RemoteFill {
  val fillColor = animateColor(fill.color, animationSettings)
  val opacity = animateScalar(fill.opacity, animationSettings)
  return RemoteFill(fillColor = fillColor, opacity = opacity, fillRule = fill.fillRule)
}

private fun gradientFill(
  fill: GradientFill,
  animationSettings: LottieSettings,
): RemoteGradientFill {
  return RemoteGradientFill(
    gradientType = fill.gradientType,
    startPoint = animatePosition(fill.startPoint, animationSettings),
    endPoint = animatePosition(fill.endPoint, animationSettings),
    gradient = animateGradient(fill.gradientColors, animationSettings),
    opacity = animateScalar(fill.opacity, animationSettings),
    fillRule = fill.fillRule,
  )
}

private fun stroke(stroke: Stroke, animationSettings: LottieSettings): RemoteStroke {
  val miterLimitRf =
    stroke.miterLimit?.let { animateScalar(it, animationSettings) } ?: stroke.miterLimitNumeric?.rf
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
  return RemoteGradientStroke(
    gradientType = stroke.gradientType,
    startPoint = animatePosition(stroke.startPoint, animationSettings),
    endPoint = animatePosition(stroke.endPoint, animationSettings),
    gradient = animateGradient(stroke.gradientColors, animationSettings),
    opacity = animateScalar(stroke.opacity, animationSettings),
    strokeWidth = animateScalar(stroke.strokeWidth, animationSettings),
    strokeCap = stroke.lineCap.toStrokeCap(),
    strokeJoin = stroke.lineJoin.toStrokeJoin(),
    miterLimit = stroke.miterLimit ?: 4f,
    dashPattern = createDashPathEffect(stroke.dashes, animationSettings),
  )
}

private fun MutableList<RemoteShape>.addIfNotNull(shape: RemoteShape?) {
  if (shape != null) {
    this.add(shape)
  }
}

@SuppressLint("RestrictedApi")
internal fun applyLayerMasks(
  masks: List<Mask>,
  animationSettings: LottieSettings,
  canvas: RemoteCanvas,
) {
  val nonInvertedAddSubpaths = mutableListOf<RemoteBezierValue>()

  for (mask in masks) {
    if (mask.mode == MaskMode.None) continue
    val maskPath = mask.path ?: continue
    val bezierList = animateBezier(maskPath, animationSettings)
    if (bezierList.isEmpty()) continue

    if (mask.mode == MaskMode.Add && !mask.inverted) {
      nonInvertedAddSubpaths.addAll(bezierList)
    } else {
      val rcPath = buildRemotePathFromBezier(bezierList)
      val clipOp =
        when (mask.mode) {
          MaskMode.Subtract -> if (mask.inverted) ClipOp.Intersect else ClipOp.Difference
          MaskMode.Add -> ClipOp.Difference
          MaskMode.Intersect -> if (mask.inverted) ClipOp.Difference else ClipOp.Intersect
          MaskMode.Difference -> ClipOp.Difference
          MaskMode.Lighten,
          MaskMode.Darken,
          MaskMode.None,
          MaskMode.Unknown -> continue
        }
      canvas.clipPath(rcPath, clipOp)
    }
  }

  if (nonInvertedAddSubpaths.isNotEmpty()) {
    val compositeAddPath = buildRemotePathFromBezier(nonInvertedAddSubpaths)
    canvas.clipPath(compositeAddPath, ClipOp.Intersect)
  }
}

@SuppressLint("RestrictedApi")
internal fun applyMatteClip(
  matteContext: MatteContext,
  animationSettings: LottieSettings,
  canvas: RemoteCanvas,
) {
  val matteLayer = matteContext.matteLayer
  val matteTransform = matteLayer.transform
  val layerTransforms =
    if (matteTransform != null) {
      matteContext.matteTransforms + matteTransform
    } else {
      matteContext.matteTransforms
    }

  for (transform in layerTransforms) {
    transform(transform, null, animationSettings, canvas)
  }

  val clipOp =
    if (
      matteContext.matteMode == MatteMode.InvertedAlpha ||
        matteContext.matteMode == MatteMode.InvertedLuma
    ) {
      ClipOp.Difference
    } else {
      ClipOp.Intersect
    }

  when (matteLayer) {
    is ShapeLayer -> clipShapes(matteLayer.shapes, animationSettings, canvas, clipOp)
    is SolidColorLayer -> {
      val rcPath = RemotePath()
      rcPath.reset()
      rcPath.moveTo(0f, 0f)
      rcPath.lineTo(matteLayer.solidWidth, 0f)
      rcPath.lineTo(matteLayer.solidWidth, matteLayer.solidHeight)
      rcPath.lineTo(0f, matteLayer.solidHeight)
      rcPath.close()
      canvas.clipPath(rcPath, clipOp)
    }
    else -> {}
  }

  for (transform in layerTransforms.reversed()) {
    inverseTransform(transform, animationSettings, canvas)
  }
}

@SuppressLint("RestrictedApi")
private fun clipShapes(
  shapes: List<GraphicElement>,
  animationSettings: LottieSettings,
  canvas: RemoteCanvas,
  clipOp: ClipOp = ClipOp.Intersect,
) {
  for (shape in shapes) {
    if (shape.hidden == true) continue
    when (shape) {
      is Rectangle -> {
        val lottiePath = evaluateRectangle(shape, animationSettings)
        if (lottiePath != null) {
          val rcPath = buildRemotePathFromBezier(lottiePath.path)
          canvas.clipPath(rcPath, clipOp)
        }
      }
      is Path -> {
        val lottiePath = path(shape, animationSettings, null)
        if (lottiePath != null) {
          val rcPath = buildRemotePathFromBezier(lottiePath.path)
          canvas.clipPath(rcPath, clipOp)
        }
      }
      is Ellipse -> {
        val compiledPath = ellipse(shape, animationSettings)
        if (compiledPath != null) {
          canvas.clipPath(compiledPath.path, clipOp)
        }
      }
      is PolyStar -> {
        val lottiePath = evaluatePolyStar(shape, animationSettings)
        if (lottiePath != null) {
          val rcPath = buildRemotePathFromBezier(lottiePath.path)
          canvas.clipPath(rcPath, clipOp)
        }
      }
      is Group -> {
        val groupTransform = shape.shapes.filterIsInstance<Transform>().firstOrNull()
        if (groupTransform != null) {
          transform(groupTransform, null, animationSettings, canvas)
          clipShapes(shape.shapes.filter { it !is Transform }, animationSettings, canvas, clipOp)
          inverseTransform(groupTransform, animationSettings, canvas)
        } else {
          clipShapes(shape.shapes, animationSettings, canvas, clipOp)
        }
      }
      else -> {}
    }
  }
}

@SuppressLint("RestrictedApi")
internal fun buildRemotePathFromBezier(path: List<RemoteBezierValue>): RemotePath {
  val rcPath = RemotePath()
  rcPath.reset()
  if (path.isEmpty()) return rcPath
  for (subpath in path) {
    val vertices = subpath.vertices
    val inTangents = subpath.inTangents
    val outTangents = subpath.outTangents

    if (vertices.isEmpty()) continue

    val startX = vertices[0].getOrElse(0) { 0f.rf }.constantValueOrNull ?: 0f
    val startY = vertices[0].getOrElse(1) { 0f.rf }.constantValueOrNull ?: 0f
    rcPath.moveTo(startX, startY)

    val maxIndex = if (subpath.closed) vertices.size else vertices.size - 1
    for (i in 0 until maxIndex) {
      val p0 = vertices[i]
      val lastIndex = if (i == vertices.size - 1 && subpath.closed) 0 else i + 1
      val p4 = vertices[lastIndex]
      val inTangent = inTangents.getOrNull(lastIndex)
      val outTangent = outTangents.getOrNull(i)

      val p0x = p0.getOrElse(0) { 0f.rf }.constantValueOrNull ?: 0f
      val p0y = p0.getOrElse(1) { 0f.rf }.constantValueOrNull ?: 0f
      val p4x = p4.getOrElse(0) { 0f.rf }.constantValueOrNull ?: 0f
      val p4y = p4.getOrElse(1) { 0f.rf }.constantValueOrNull ?: 0f

      val inTangentX = inTangent?.getOrElse(0) { 0f.rf }?.constantValueOrNull ?: 0f
      val inTangentY = inTangent?.getOrElse(1) { 0f.rf }?.constantValueOrNull ?: 0f
      val outTangentX = outTangent?.getOrElse(0) { 0f.rf }?.constantValueOrNull ?: 0f
      val outTangentY = outTangent?.getOrElse(1) { 0f.rf }?.constantValueOrNull ?: 0f

      val p1x = p0x + outTangentX
      val p1y = p0y + outTangentY
      val p2x = p4x + inTangentX
      val p2y = p4y + inTangentY

      rcPath.cubicTo(p1x, p1y, p2x, p2y, p4x, p4y)
    }

    if (subpath.closed) {
      rcPath.close()
    }
  }
  return rcPath
}
