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

package com.google.android.horologist.remotecompose.lottie.renderer.modifiers

import android.annotation.SuppressLint
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.cos
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.sin
import androidx.compose.remote.creation.compose.state.tan
import androidx.compose.remote.creation.compose.state.toRad
import com.google.android.horologist.remotecompose.lottie.LottieSettings
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.grouping.Transform
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.CompositeMode
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.Repeater
import com.google.android.horologist.remotecompose.lottie.format.values.Point
import com.google.android.horologist.remotecompose.lottie.renderer.RemoteCompiledGeometry
import com.google.android.horologist.remotecompose.lottie.renderer.RemoteDynamicGeometry
import com.google.android.horologist.remotecompose.lottie.renderer.RemoteGroup
import com.google.android.horologist.remotecompose.lottie.renderer.RemoteShape
import com.google.android.horologist.remotecompose.lottie.renderer.StyledShapes
import com.google.android.horologist.remotecompose.lottie.renderer.properties.RemoteBezierValue
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animatePosition
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animateScalar
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animateVector
import kotlin.math.pow

/** Represents an evaluated instance of a repeated shape with an associated opacity multiplier. */
@SuppressLint("RestrictedApi")
internal data class RepeatedShapeInstance(
  val shape: RemoteShape,
  val opacityMultiplier: RemoteFloat = 1f.rf,
)

/**
 * Evaluates a [Repeater] modifier on a collection of [RemoteShape] instances, producing duplicate
 * geometry copies with incremental affine transforms and start/end opacity compounding.
 */
@SuppressLint("RestrictedApi")
internal fun evaluateRepeater(
  shapes: List<RemoteShape>,
  repeater: Repeater,
  animationSettings: LottieSettings,
): List<RepeatedShapeInstance> {
  if (repeater.hidden?.constantValue == true || shapes.isEmpty()) {
    return shapes.map { RepeatedShapeInstance(it) }
  }

  val copies = animateScalar(repeater.copies, animationSettings)
  val count = copies.constantValueOrNull?.toInt() ?: 1
  if (count <= 0) return emptyList()

  val offset = animateScalar(repeater.offset, animationSettings)
  val offsetVal = offset.constantValueOrNull ?: 0f

  val repeaterTransform = repeater.transform
  val startOpacity =
    repeaterTransform?.startOpacity?.let { animateScalar(it, animationSettings) }
      ?: repeaterTransform?.opacity?.let { animateScalar(it, animationSettings) }
      ?: 100f.rf
  val endOpacity =
    repeaterTransform?.endOpacity?.let { animateScalar(it, animationSettings) }
      ?: repeaterTransform?.opacity?.let { animateScalar(it, animationSettings) }
      ?: 100f.rf

  val copyIndices =
    if (repeater.composite == CompositeMode.Below) {
      count - 1 downTo 0
    } else {
      0 until count
    }

  val instances = mutableListOf<RepeatedShapeInstance>()
  for (i in copyIndices) {
    val k = i.toFloat() + offsetVal
    val alpha =
      if (count <= 1) {
        startOpacity / 100f
      } else {
        (startOpacity + (endOpacity - startOpacity) * (i.toFloat() / (count - 1).toFloat()).rf) /
          100f
      }

    for (shape in shapes) {
      val transformedShape =
        if (repeaterTransform != null) {
          transformRepeaterShape(shape, repeaterTransform, k, animationSettings)
        } else {
          shape
        }
      instances.add(RepeatedShapeInstance(shape = transformedShape, opacityMultiplier = alpha))
    }
  }

  return instances
}

/** Transforms a [RemoteShape] by a Lottie [Transform] at step [k]. */
@SuppressLint("RestrictedApi")
internal fun transformRepeaterShape(
  shape: RemoteShape,
  transform: Transform,
  k: Float,
  animationSettings: LottieSettings,
): RemoteShape {
  return when (shape) {
    is RemoteDynamicGeometry ->
      transformRepeaterDynamicGeometry(shape, transform, k, animationSettings)
    is RemoteCompiledGeometry -> {
      if (shape.bezierSubpaths != null) {
        val dynamic = RemoteDynamicGeometry(shape.bezierSubpaths, shape.fillRule)
        transformRepeaterDynamicGeometry(dynamic, transform, k, animationSettings)
      } else {
        shape
      }
    }
    is RemoteGroup -> {
      val newChildShapes =
        shape.childShapes.map { styledShapes ->
          StyledShapes(
            shapes =
              styledShapes.shapes.map { child ->
                transformRepeaterShape(child, transform, k, animationSettings)
              },
            style = styledShapes.style,
          )
        }
      RemoteGroup(
        childShapes = newChildShapes,
        animationSettings = shape.animationSettings,
        transform = shape.transform,
      )
    }
    else -> shape
  }
}

/** Transforms a [RemoteDynamicGeometry] by a repeater [Transform] at step [k]. */
@SuppressLint("RestrictedApi")
internal fun transformRepeaterDynamicGeometry(
  dyn: RemoteDynamicGeometry,
  transform: Transform,
  k: Float,
  animationSettings: LottieSettings,
): RemoteDynamicGeometry {
  val transformedSubpaths =
    dyn.path.map { subpath ->
      transformRepeaterBezierValue(subpath, transform, k, animationSettings)
    }
  return RemoteDynamicGeometry(transformedSubpaths, dyn.fillRule)
}

/** Transforms a single [RemoteBezierValue] by a repeater [Transform] at step [k]. */
@SuppressLint("RestrictedApi")
internal fun transformRepeaterBezierValue(
  subpath: RemoteBezierValue,
  transform: Transform,
  k: Float,
  animationSettings: LottieSettings,
): RemoteBezierValue {
  val anchorPoint = animatePosition(transform.anchorPoint, animationSettings)
  val translation = animatePosition(transform.positionTranslation, animationSettings)
  val scale = animateVector(transform.scale, animationSettings)
  val scaleX = (scale.getOrNull(0) ?: 100f.rf) / 100f
  val scaleY = (scale.getOrNull(1) ?: 100f.rf) / 100f
  val rotation = animateScalar(transform.rotation, animationSettings)
  val skew = transform.skew?.let { animateScalar(it, animationSettings) }
  val skewAxis = transform.skewAxis?.let { animateScalar(it, animationSettings) }

  val scaleXK =
    scaleX.constantValueOrNull?.let { sx -> sx.toDouble().pow(k.toDouble()).toFloat().rf }
      ?: if (k == 0f) 1f.rf else if (k == 1f) scaleX else scaleX
  val scaleYK =
    scaleY.constantValueOrNull?.let { sy -> sy.toDouble().pow(k.toDouble()).toFloat().rf }
      ?: if (k == 0f) 1f.rf else if (k == 1f) scaleY else scaleY

  val rotK = rotation * k.rf
  val skewK = skew?.let { it * k.rf }
  val transXK = anchorPoint.x + translation.x * k.rf
  val transYK = anchorPoint.y + translation.y * k.rf

  val newVertices =
    subpath.vertices.map { point ->
      transformRepeaterPoint(
        x = point.x,
        y = point.y,
        anchorX = anchorPoint.x,
        anchorY = anchorPoint.y,
        scaleXK = scaleXK,
        scaleYK = scaleYK,
        rotK = rotK,
        skewK = skewK,
        skewAxis = skewAxis,
        transXK = transXK,
        transYK = transYK,
      )
    }

  val newInTangents =
    subpath.inTangents.map { tangent ->
      transformRepeaterTangent(
        dx = tangent.x,
        dy = tangent.y,
        scaleXK = scaleXK,
        scaleYK = scaleYK,
        rotK = rotK,
        skewK = skewK,
        skewAxis = skewAxis,
      )
    }

  val newOutTangents =
    subpath.outTangents.map { tangent ->
      transformRepeaterTangent(
        dx = tangent.x,
        dy = tangent.y,
        scaleXK = scaleXK,
        scaleYK = scaleYK,
        rotK = rotK,
        skewK = skewK,
        skewAxis = skewAxis,
      )
    }

  return RemoteBezierValue(
    closed = subpath.closed,
    inTangents = newInTangents,
    outTangents = newOutTangents,
    vertices = newVertices,
  )
}

@SuppressLint("RestrictedApi")
private fun transformRepeaterPoint(
  x: RemoteFloat,
  y: RemoteFloat,
  anchorX: RemoteFloat,
  anchorY: RemoteFloat,
  scaleXK: RemoteFloat,
  scaleYK: RemoteFloat,
  rotK: RemoteFloat,
  skewK: RemoteFloat?,
  skewAxis: RemoteFloat?,
  transXK: RemoteFloat,
  transYK: RemoteFloat,
): Point {
  var px = (x - anchorX) * scaleXK
  var py = (y - anchorY) * scaleYK

  if (skewK != null) {
    val axis = skewAxis ?: 0f.rf
    val radAxis = toRad(90f.rf - axis)
    val cosA = cos(radAxis)
    val sinA = sin(radAxis)
    val rx = px * cosA + py * sinA
    val ry = -px * sinA + py * cosA
    val skX = rx
    val skY = rx * tan(toRad(skewK)) + ry
    px = skX * cosA - skY * sinA
    py = skX * sinA + skY * cosA
  }

  val rad = toRad(rotK)
  val cosR = cos(rad)
  val sinR = sin(rad)
  val rx = px * cosR - py * sinR
  val ry = px * sinR + py * cosR

  val finalX = rx + transXK
  val finalY = ry + transYK

  return Point(finalX, finalY)
}

@SuppressLint("RestrictedApi")
private fun transformRepeaterTangent(
  dx: RemoteFloat,
  dy: RemoteFloat,
  scaleXK: RemoteFloat,
  scaleYK: RemoteFloat,
  rotK: RemoteFloat,
  skewK: RemoteFloat?,
  skewAxis: RemoteFloat?,
): Point {
  var px = dx * scaleXK
  var py = dy * scaleYK

  if (skewK != null) {
    val axis = skewAxis ?: 0f.rf
    val radAxis = toRad(90f.rf - axis)
    val cosA = cos(radAxis)
    val sinA = sin(radAxis)
    val rx = px * cosA + py * sinA
    val ry = -px * sinA + py * cosA
    val skX = rx
    val skY = rx * tan(toRad(skewK)) + ry
    px = skX * cosA - skY * sinA
    py = skX * sinA + skY * cosA
  }

  val rad = toRad(rotK)
  val cosR = cos(rad)
  val sinR = sin(rad)
  val rx = px * cosR - py * sinR
  val ry = px * sinR + py * cosR

  return Point(rx, ry)
}
