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
import androidx.compose.runtime.Composable
import com.google.android.horologist.remotecompose.lottie.LocalAnimationSettings
import com.google.android.horologist.remotecompose.lottie.LottieSettings
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.GraphicElement
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.ShapeType
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.Ellipse
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.GeometryShape
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.Path
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.PolyStar
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.Rectangle
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.grouping.Group
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.grouping.Transform
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.Fill
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.GradientFill
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.GradientStroke
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animateColor
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animateGradient
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animatePosition
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animateScalar
import com.google.android.horologist.remotecompose.lottie.renderer.shapes.evaluateEllipse
import com.google.android.horologist.remotecompose.lottie.renderer.shapes.evaluatePath
import com.google.android.horologist.remotecompose.lottie.renderer.shapes.evaluatePolyStar
import com.google.android.horologist.remotecompose.lottie.renderer.shapes.evaluateRectangle

internal data class StyledShapes(val shapes: List<RemoteShape>, val style: RemoteStyle)

/** Renders a list of Lottie Shapes to the RemoteCanvas. */
@SuppressLint("RestrictedApi")
@Composable
@RemoteComposable
internal fun RenderShapes(shapes: List<GraphicElement>, transformStack: List<Transform>) {
  val animationSettings = LocalAnimationSettings.current
  val shapeGroups = gatherShapes(shapes, animationSettings)

  // Aspect-ratio scaling and centering is applied once, at the top level, by the
  // drawWithContent modifier in LottieAnimation - shapes draw in raw Lottie coordinates here.
  RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) {
    for (shapeGroup in shapeGroups) {
      val paint = shapeGroup.style.getPaint()

      for (transform in transformStack) {
        remoteCanvas.save()
        transform(transform, paint, animationSettings, remoteCanvas)
      }

      usePaint(paint) {
        for (shape in shapeGroup.shapes) {
          shape.draw(this, remoteCanvas)
        }
      }

      for (transform in transformStack) {
        remoteCanvas.restore()
      }
    }
  }
}

private fun gatherShapes(
  shapes: List<GraphicElement>,
  animationSettings: LottieSettings,
): List<StyledShapes> {
  val shapeGroups = mutableListOf<StyledShapes>()
  var currentShapes = mutableListOf<RemoteShape>()

  for (shape in shapes.reversed()) {
    when (shape) {
      is GeometryShape -> {
        val remoteShape =
          when (shape) {
            is Path -> evaluatePath(shape, animationSettings)
            is Rectangle -> evaluateRectangle(shape, animationSettings)
            is Ellipse -> evaluateEllipse(shape, animationSettings)
            is PolyStar -> evaluatePolyStar(shape, animationSettings)
          }
        currentShapes.addIfNotNull(remoteShape)
      }
      is Group -> currentShapes.addIfNotNull(group(shape, animationSettings))
      is Fill -> {
        val fill = fill(shape, animationSettings)
        shapeGroups.add(StyledShapes(currentShapes, fill))
        currentShapes = mutableListOf()
      }
      is GradientFill -> {
        val gradientFill = gradientFill(shape, animationSettings)
        shapeGroups.add(StyledShapes(currentShapes, gradientFill))
        currentShapes = mutableListOf()
      }
      is GradientStroke -> {
        val gradientStroke = gradientStroke(shape, animationSettings)
        shapeGroups.add(StyledShapes(currentShapes, gradientStroke))
        currentShapes = mutableListOf()
      }
      else -> {} // Transform, modifiers, unknown elements
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

private fun group(group: Group, animationSettings: LottieSettings): RemoteGroup? {
  if (group.hidden == true) {
    return null
  }

  val reversed = group.shapes.reversed()

  if (reversed.firstOrNull()?.type == ShapeType.Transform) {
    val transform = reversed[0] as Transform
    val styledShapes = gatherShapes(reversed.drop(1), animationSettings)
    return RemoteGroup(styledShapes, animationSettings, transform)
  } else {
    return RemoteGroup(gatherShapes(reversed, animationSettings), animationSettings, null)
  }
}

private fun fill(fill: Fill, animationSettings: LottieSettings): RemoteFill {
  return RemoteFill(animateColor(fill.color, animationSettings))
}

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
  )
}

private fun gradientStroke(
  stroke: GradientStroke,
  animationSettings: LottieSettings,
): RemoteGradientStroke {
  val startPoint = animatePosition(stroke.startPoint, animationSettings)
  val endPoint = animatePosition(stroke.endPoint, animationSettings)
  val gradient = animateGradient(stroke.colors, animationSettings)
  val opacity = animateScalar(stroke.opacity, animationSettings)
  val strokeWidth = animateScalar(stroke.strokeWidth, animationSettings)
  return RemoteGradientStroke(
    gradient = gradient,
    startPoint = startPoint,
    endPoint = endPoint,
    gradientType = stroke.gradientType,
    opacity = opacity,
    strokeWidth = strokeWidth,
  )
}

private fun MutableList<RemoteShape>.addIfNotNull(shape: RemoteShape?) {
  if (shape != null) {
    this.add(shape)
  }
}
