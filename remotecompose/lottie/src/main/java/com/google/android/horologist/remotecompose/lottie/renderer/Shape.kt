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
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.runtime.Composable
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
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.TrimPath
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.Fill
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.FillRule
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.GradientFill
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.GradientStroke
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.Stroke
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.toStrokeCap
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.toStrokeJoin
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animateColor
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animateGradient
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animatePosition
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animateScalar
import com.google.android.horologist.remotecompose.lottie.renderer.shapes.ellipse
import com.google.android.horologist.remotecompose.lottie.renderer.shapes.evaluatePolyStar
import com.google.android.horologist.remotecompose.lottie.renderer.shapes.path
import com.google.android.horologist.remotecompose.lottie.renderer.shapes.rectangle

internal data class StyledShapes(val shapes: List<RemoteShape>, val style: RemoteStyle)

/** Renders a list of Lottie Shapes to the RemoteCanvas. */
@SuppressLint("RestrictedApi")
@Composable
@RemoteComposable
internal fun RenderShapes(shapes: List<GraphicElement>, transformStack: List<Transform>) {
  val animationSettings = LocalAnimationSettings.current
  val shapeGroups = gatherShapes(shapes, animationSettings)

  val layerOpacity =
    (transformStack.lastOrNull()?.opacity?.let { animateScalar(it, animationSettings) / 100f }
      ?: 1f.rf)

  // Aspect-ratio scaling and centering is applied once, at the top level, by the
  // drawWithContent modifier in LottieAnimation - shapes draw in raw Lottie coordinates here.
  RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) {
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
  }
}

internal fun gatherShapes(
  shapes: List<GraphicElement>,
  animationSettings: LottieSettings,
  parentTrimPath: TrimPath? = null,
): List<StyledShapes> {
  val shapeGroups = mutableListOf<StyledShapes>()
  var currentShapes = mutableListOf<RemoteShape>()
  var activeTrimPath: TrimPath? =
    shapes.filterIsInstance<TrimPath>().firstOrNull { it.hidden != true } ?: parentTrimPath

  for (shape in shapes.reversed()) {
    when (shape) {
      is TrimPath -> {
        if (shape.hidden != true) {
          activeTrimPath = shape
        }
      }
      is Path -> currentShapes.addIfNotNull(path(shape, animationSettings, activeTrimPath))
      is Rectangle -> currentShapes.addIfNotNull(rectangle(shape, animationSettings))
      is Ellipse -> currentShapes.addIfNotNull(ellipse(shape, animationSettings))
      is PolyStar -> currentShapes.addIfNotNull(evaluatePolyStar(shape, animationSettings))
      is Group -> currentShapes.addIfNotNull(group(shape, animationSettings, activeTrimPath))
      is Fill -> {
        val fill = fill(shape, animationSettings)
        val styled =
          if (fill.fillRule != FillRule.NonZero) {
            currentShapes.map { it.withFillRule(fill.fillRule) }
          } else {
            currentShapes
          }
        shapeGroups.add(StyledShapes(styled, fill))
        currentShapes = mutableListOf()
      }
      is GradientFill -> {
        val gradFill = gradientFill(shape, animationSettings)
        val styled =
          if (gradFill.fillRule != FillRule.NonZero) {
            currentShapes.map { it.withFillRule(gradFill.fillRule) }
          } else {
            currentShapes
          }
        shapeGroups.add(StyledShapes(styled, gradFill))
        currentShapes = mutableListOf()
      }
      is Stroke -> {
        val stroke = stroke(shape, animationSettings)
        shapeGroups.add(StyledShapes(currentShapes, stroke))
        currentShapes = mutableListOf()
      }
      is GradientStroke -> {
        val gradStroke = gradientStroke(shape, animationSettings)
        shapeGroups.add(StyledShapes(currentShapes, gradStroke))
        currentShapes = mutableListOf()
      }
      is Transform -> {} // No-op - handled groups
      else -> {}
    }
  }

  // Groups don't have to have styling information associated with them, because the child nodes
  // can have styles instead. If there's a Group node left over that doesn't have a style, add
  // it to the render tree anyway
  if (currentShapes.isNotEmpty() && currentShapes.all { it is RemoteGroup }) {
    shapeGroups.add(StyledShapes(currentShapes, NoopStyle()))
  }

  return shapeGroups
}

internal fun gatherShapesForTest(
  shapes: List<GraphicElement>,
  animationSettings: LottieSettings,
  parentTrimPath: TrimPath? = null,
): List<StyledShapes> = gatherShapes(shapes.reversed(), animationSettings, parentTrimPath)

private fun group(
  group: Group,
  animationSettings: LottieSettings,
  parentTrimPath: TrimPath? = null,
): RemoteGroup? {
  if (group.hidden == true) {
    return null
  }

  val activeTrimPath =
    group.shapes.filterIsInstance<TrimPath>().firstOrNull { it.hidden != true } ?: parentTrimPath

  val reversed = group.shapes.reversed()

  if (reversed.firstOrNull()?.type == ShapeType.Transform) {
    val transform = reversed[0] as Transform
    val styledShapes = gatherShapes(reversed.drop(1), animationSettings, activeTrimPath)
    return RemoteGroup(styledShapes, animationSettings, transform)
  } else {
    return RemoteGroup(
      gatherShapes(reversed, animationSettings, activeTrimPath),
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
