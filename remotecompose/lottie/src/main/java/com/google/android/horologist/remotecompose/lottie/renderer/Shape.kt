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
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.Ellipse
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.GeometryShape
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.Path
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.PolyStar
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.Rectangle
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.grouping.Group
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.grouping.Transform
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.TrimPath
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.Fill
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.GradientFill
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.GradientStroke
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.Stroke
import com.google.android.horologist.remotecompose.lottie.format.layer.MatteMode
import com.google.android.horologist.remotecompose.lottie.format.layer.ShapeLayer
import com.google.android.horologist.remotecompose.lottie.format.mask.Mask
import com.google.android.horologist.remotecompose.lottie.format.mask.MaskMode
import com.google.android.horologist.remotecompose.lottie.renderer.layers.MatteContext
import com.google.android.horologist.remotecompose.lottie.renderer.properties.RemoteBezierValue
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animateBezier
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animateColor
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animateGradient
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animatePosition
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animateScalar
import com.google.android.horologist.remotecompose.lottie.renderer.shapes.evaluateEllipse
import com.google.android.horologist.remotecompose.lottie.renderer.shapes.evaluatePath
import com.google.android.horologist.remotecompose.lottie.renderer.shapes.evaluatePolyStar
import com.google.android.horologist.remotecompose.lottie.renderer.shapes.evaluateRectangle
import com.google.android.horologist.remotecompose.lottie.renderer.shapes.transformRemoteShape

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

@SuppressLint("RestrictedApi")
private fun gatherShapes(
  shapes: List<GraphicElement>,
  animationSettings: LottieSettings,
  parentTrimPath: TrimPath? = null,
): List<StyledShapes> {
  val shapeGroups = mutableListOf<StyledShapes>()
  var currentGeometries = mutableListOf<RemoteShape>()
  var currentGroups = mutableListOf<Group>()
  val activeTrimPath: TrimPath? =
    shapes.filterIsInstance<TrimPath>().firstOrNull { it.hidden != true } ?: parentTrimPath
  var hasEmittedStyle = false

  for (shape in shapes) {
    when (shape) {
      is TrimPath -> {
        // Handled via activeTrimPath
      }
      is GeometryShape -> {
        if (hasEmittedStyle) {
          currentGeometries = mutableListOf()
          currentGroups = mutableListOf()
          hasEmittedStyle = false
        }
        val remoteShape =
          when (shape) {
            is Path -> evaluatePath(shape, animationSettings, activeTrimPath)
            is Rectangle -> evaluateRectangle(shape, animationSettings, activeTrimPath)
            is Ellipse -> evaluateEllipse(shape, animationSettings, activeTrimPath)
            is PolyStar -> evaluatePolyStar(shape, animationSettings, activeTrimPath)
          }
        currentGeometries.addIfNotNull(remoteShape)
      }
      is Group -> {
        if (hasEmittedStyle) {
          currentGeometries = mutableListOf()
          currentGroups = mutableListOf()
          hasEmittedStyle = false
        }
        val groupShape = group(shape, animationSettings, activeTrimPath)
        if (groupShape != null) {
          shapeGroups.add(StyledShapes(listOf(groupShape), NoopStyle()))
        }
        currentGroups.add(shape)
      }
      is Fill -> {
        if (shape.hidden != true) {
          val fill = fill(shape, animationSettings)
          val shapesToStyle = mutableListOf<RemoteShape>()
          if (currentGeometries.isNotEmpty()) {
            shapesToStyle.addAll(currentGeometries)
          }
          for (group in currentGroups) {
            shapesToStyle.addAll(evaluateGroupGeometries(group, animationSettings, activeTrimPath))
          }
          if (shapesToStyle.isNotEmpty()) {
            shapeGroups.add(StyledShapes(shapesToStyle, fill))
          }
          hasEmittedStyle = true
        }
      }
      is Stroke -> {
        if (shape.hidden != true) {
          val stroke = stroke(shape, animationSettings)
          val shapesToStyle = mutableListOf<RemoteShape>()
          if (currentGeometries.isNotEmpty()) {
            shapesToStyle.addAll(currentGeometries)
          }
          for (group in currentGroups) {
            shapesToStyle.addAll(evaluateGroupGeometries(group, animationSettings, activeTrimPath))
          }
          if (shapesToStyle.isNotEmpty()) {
            shapeGroups.add(StyledShapes(shapesToStyle, stroke))
          }
          hasEmittedStyle = true
        }
      }
      is GradientFill -> {
        if (shape.hidden != true) {
          val gradientFill = gradientFill(shape, animationSettings)
          val shapesToStyle = mutableListOf<RemoteShape>()
          if (currentGeometries.isNotEmpty()) {
            shapesToStyle.addAll(currentGeometries)
          }
          for (group in currentGroups) {
            shapesToStyle.addAll(evaluateGroupGeometries(group, animationSettings, activeTrimPath))
          }
          if (shapesToStyle.isNotEmpty()) {
            shapeGroups.add(StyledShapes(shapesToStyle, gradientFill))
          }
          hasEmittedStyle = true
        }
      }
      is GradientStroke -> {
        if (shape.hidden != true) {
          val gradientStroke = gradientStroke(shape, animationSettings)
          val shapesToStyle = mutableListOf<RemoteShape>()
          if (currentGeometries.isNotEmpty()) {
            shapesToStyle.addAll(currentGeometries)
          }
          for (group in currentGroups) {
            shapesToStyle.addAll(evaluateGroupGeometries(group, animationSettings, activeTrimPath))
          }
          if (shapesToStyle.isNotEmpty()) {
            shapeGroups.add(StyledShapes(shapesToStyle, gradientStroke))
          }
          hasEmittedStyle = true
        }
      }
      else -> {} // Transform, other modifiers, unknown elements
    }
  }

  // In Lottie, elements at higher array indices are at the bottom of the stack and drawn first;
  // elements at lower array indices are at the top of the stack and drawn last.
  return shapeGroups.reversed()
}

@SuppressLint("RestrictedApi")
private fun evaluateGroupGeometries(
  group: Group,
  animationSettings: LottieSettings,
  parentTrimPath: TrimPath? = null,
): List<RemoteShape> {
  if (group.hidden == true) return emptyList()
  val activeTrimPath =
    group.shapes.filterIsInstance<TrimPath>().firstOrNull { it.hidden != true } ?: parentTrimPath
  val groupTransform = group.shapes.filterIsInstance<Transform>().firstOrNull()
  val geometries = mutableListOf<RemoteShape>()
  for (shape in group.shapes) {
    when (shape) {
      is GeometryShape -> {
        val remoteShape =
          when (shape) {
            is Path -> evaluatePath(shape, animationSettings, activeTrimPath)
            is Rectangle -> evaluateRectangle(shape, animationSettings, activeTrimPath)
            is Ellipse -> evaluateEllipse(shape, animationSettings, activeTrimPath)
            is PolyStar -> evaluatePolyStar(shape, animationSettings, activeTrimPath)
          }
        if (remoteShape != null) {
          val transformed =
            if (groupTransform != null) {
              transformRemoteShape(remoteShape, groupTransform, animationSettings)
            } else {
              remoteShape
            }
          geometries.add(transformed)
        }
      }
      is Group -> {
        val nestedGeometries = evaluateGroupGeometries(shape, animationSettings, activeTrimPath)
        for (nested in nestedGeometries) {
          val transformed =
            if (groupTransform != null) {
              transformRemoteShape(nested, groupTransform, animationSettings)
            } else {
              nested
            }
          geometries.add(transformed)
        }
      }
      else -> {}
    }
  }
  return geometries
}

@SuppressLint("RestrictedApi")
private fun group(
  group: Group,
  animationSettings: LottieSettings,
  parentTrimPath: TrimPath? = null,
): RemoteGroup? {
  if (group.hidden == true) {
    return null
  }

  val transform = group.shapes.filterIsInstance<Transform>().firstOrNull()
  val contentShapes = group.shapes.filter { it !is Transform }
  val styledShapes = gatherShapes(contentShapes, animationSettings, parentTrimPath)
  if (styledShapes.isEmpty()) {
    return null
  }
  return RemoteGroup(styledShapes, animationSettings, transform)
}

@SuppressLint("RestrictedApi")
private fun fill(fill: Fill, animationSettings: LottieSettings): RemoteFill {
  return RemoteFill(animateColor(fill.color, animationSettings), fill.fillRule)
}

@SuppressLint("RestrictedApi")
private fun stroke(stroke: Stroke, animationSettings: LottieSettings): RemoteStroke {
  val strokeColor = animateColor(stroke.color, animationSettings)
  val strokeWidth = animateScalar(stroke.strokeWidth, animationSettings)
  val opacity = animateScalar(stroke.opacity, animationSettings)
  val miterLimit =
    stroke.miterLimit?.let { animateScalar(it, animationSettings) } ?: stroke.miterLimitNumeric?.rf
  val dashPattern = createDashPathEffect(stroke.dashes, animationSettings)
  return RemoteStroke(
    strokeColor = strokeColor,
    strokeWidth = strokeWidth,
    opacity = opacity,
    lineCap = stroke.lineCap,
    lineJoin = stroke.lineJoin,
    miterLimit = miterLimit,
    dashPattern = dashPattern,
  )
}

@SuppressLint("RestrictedApi")
private fun gradientFill(
  fill: GradientFill,
  animationSettings: LottieSettings,
): RemoteGradientFill {
  val startPoint = animatePosition(fill.startPoint, animationSettings)
  val endPoint = animatePosition(fill.endPoint, animationSettings)
  val gradient = animateGradient(fill.colors, animationSettings)
  val opacity = animateScalar(fill.opacity, animationSettings)
  return RemoteGradientFill(
    gradient = gradient,
    startPoint = startPoint,
    endPoint = endPoint,
    gradientType = fill.gradientType,
    opacity = opacity,
    fillRule = fill.fillRule,
  )
}

@SuppressLint("RestrictedApi")
private fun gradientStroke(
  stroke: GradientStroke,
  animationSettings: LottieSettings,
): RemoteGradientStroke {
  val startPoint = animatePosition(stroke.startPoint, animationSettings)
  val endPoint = animatePosition(stroke.endPoint, animationSettings)
  val gradient = animateGradient(stroke.colors, animationSettings)
  val opacity = animateScalar(stroke.opacity, animationSettings)
  val strokeWidth = animateScalar(stroke.strokeWidth, animationSettings)
  val miterLimit = stroke.miterLimit?.let { animateScalar(it, animationSettings) }
  val dashPattern = createDashPathEffect(stroke.dashes, animationSettings)
  return RemoteGradientStroke(
    gradient = gradient,
    startPoint = startPoint,
    endPoint = endPoint,
    gradientType = stroke.gradientType,
    opacity = opacity,
    strokeWidth = strokeWidth,
    lineCap = stroke.lineCap,
    lineJoin = stroke.lineJoin,
    miterLimit = miterLimit,
    dashPattern = dashPattern,
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
  for (mask in masks) {
    if (mask.mode == MaskMode.None) continue
    val maskPath = mask.path ?: continue
    val bezierList = animateBezier(maskPath, animationSettings)
    if (bezierList.isEmpty()) continue
    val rcPath = buildRemotePathFromBezier(bezierList)

    val clipOp =
      when (mask.mode) {
        MaskMode.Subtract -> if (mask.inverted) ClipOp.Intersect else ClipOp.Difference
        MaskMode.Add,
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

@SuppressLint("RestrictedApi")
private fun applyMatteClip(
  matteContext: MatteContext,
  animationSettings: LottieSettings,
  canvas: RemoteCanvas,
) {
  val matteLayer = matteContext.matteLayer
  if (matteLayer !is ShapeLayer || matteLayer.hidden == true) return

  val layerTransforms =
    if (matteLayer.transform != null) {
      matteContext.matteTransforms + matteLayer.transform
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

  clipShapes(matteLayer.shapes, animationSettings, canvas, clipOp)

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
        val lottiePath = evaluatePath(shape, animationSettings, null)
        if (lottiePath != null) {
          val rcPath = buildRemotePathFromBezier(lottiePath.path)
          canvas.clipPath(rcPath, clipOp)
        }
      }
      is Ellipse -> {
        val lottiePath = evaluateEllipse(shape, animationSettings)
        if (lottiePath != null) {
          val rcPath = buildRemotePathFromBezier(lottiePath.path)
          canvas.clipPath(rcPath, clipOp)
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
