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
import androidx.compose.remote.creation.compose.state.rf
import com.google.android.horologist.remotecompose.lottie.LottieSettings
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.Twist
import com.google.android.horologist.remotecompose.lottie.format.values.Point
import com.google.android.horologist.remotecompose.lottie.renderer.RemoteCompiledGeometry
import com.google.android.horologist.remotecompose.lottie.renderer.RemoteDynamicGeometry
import com.google.android.horologist.remotecompose.lottie.renderer.RemoteGroup
import com.google.android.horologist.remotecompose.lottie.renderer.RemoteShape
import com.google.android.horologist.remotecompose.lottie.renderer.StyledShapes
import com.google.android.horologist.remotecompose.lottie.renderer.properties.RemoteBezierValue
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animatePosition
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animateScalar
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Evaluates a [Twist] modifier across [shapes], rotating vertices around a center proportional to
 * distance.
 */
@SuppressLint("RestrictedApi")
internal fun evaluateTwist(
  shapes: List<RemoteShape>,
  twist: Twist,
  animationSettings: LottieSettings,
): List<RemoteShape> {
  if (twist.hidden?.constantValue == true || shapes.isEmpty()) return shapes

  val angle = animateScalar(twist.angle, animationSettings).constantValueOrNull ?: 0f
  val center = animatePosition(twist.center, animationSettings)
  val cx = center.x.constantValueOrNull ?: 0f
  val cy = center.y.constantValueOrNull ?: 0f

  if (angle == 0f) return shapes

  return shapes.map { shape ->
    when (shape) {
      is RemoteDynamicGeometry -> {
        val newSubpaths = shape.path.map { subpath -> applyTwistToSubpath(subpath, angle, cx, cy) }
        RemoteDynamicGeometry(newSubpaths, shape.fillRule)
      }
      is RemoteCompiledGeometry -> {
        if (shape.bezierSubpaths != null) {
          val newSubpaths =
            shape.bezierSubpaths.map { subpath -> applyTwistToSubpath(subpath, angle, cx, cy) }
          RemoteDynamicGeometry(newSubpaths, shape.fillRule)
        } else {
          shape
        }
      }
      is RemoteGroup -> {
        val newChildShapes =
          shape.childShapes.map { styledShapes ->
            StyledShapes(
              shapes = evaluateTwist(styledShapes.shapes, twist, animationSettings),
              style = styledShapes.style,
            )
          }
        RemoteGroup(newChildShapes, shape.animationSettings, shape.transform)
      }
      else -> shape
    }
  }
}

@SuppressLint("RestrictedApi")
private fun applyTwistToSubpath(
  subpath: RemoteBezierValue,
  angleDeg: Float,
  cx: Float,
  cy: Float,
): RemoteBezierValue {
  val count = subpath.vertices.size
  if (count == 0) return subpath

  val newVertices = mutableListOf<Point>()
  val newInTangents = mutableListOf<Point>()
  val newOutTangents = mutableListOf<Point>()

  for (i in 0 until count) {
    val vx = subpath.vertices[i].x.constantValueOrNull ?: 0f
    val vy = subpath.vertices[i].y.constantValueOrNull ?: 0f

    val inTan = subpath.inTangents.getOrNull(i)
    val inX = inTan?.x?.constantValueOrNull ?: 0f
    val inY = inTan?.y?.constantValueOrNull ?: 0f

    val outTan = subpath.outTangents.getOrNull(i)
    val outX = outTan?.x?.constantValueOrNull ?: 0f
    val outY = outTan?.y?.constantValueOrNull ?: 0f

    val vTwisted = twistPoint(vx, vy, cx, cy, angleDeg)
    val inPointTwisted = twistPoint(vx + inX, vy + inY, cx, cy, angleDeg)
    val outPointTwisted = twistPoint(vx + outX, vy + outY, cx, cy, angleDeg)

    newVertices.add(Point(vTwisted.x.rf, vTwisted.y.rf))
    newInTangents.add(Point((inPointTwisted.x - vTwisted.x).rf, (inPointTwisted.y - vTwisted.y).rf))
    newOutTangents.add(
      Point((outPointTwisted.x - vTwisted.x).rf, (outPointTwisted.y - vTwisted.y).rf)
    )
  }

  return RemoteBezierValue(
    closed = subpath.closed,
    inTangents = newInTangents,
    outTangents = newOutTangents,
    vertices = newVertices,
  )
}

private fun twistPoint(px: Float, py: Float, cx: Float, cy: Float, angleDeg: Float): Vec2 {
  val dx = px - cx
  val dy = py - cy
  val dist = hypot(dx, dy)
  val theta = Math.toRadians((angleDeg * dist / 100f).toDouble()).toFloat()
  val cosT = cos(theta)
  val sinT = sin(theta)
  val newX = cx + dx * cosT - dy * sinT
  val newY = cy + dx * sinT + dy * cosT
  return Vec2(newX, newY)
}
