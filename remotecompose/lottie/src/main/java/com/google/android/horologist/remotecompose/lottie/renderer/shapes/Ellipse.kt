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

package com.google.android.horologist.remotecompose.lottie.renderer.shapes

import android.annotation.SuppressLint
import androidx.compose.remote.creation.RemotePath
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rf
import com.google.android.horologist.remotecompose.lottie.LottieSettings
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.Ellipse
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.RoundedCorners
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.TrimPath
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticBezierProperty
import com.google.android.horologist.remotecompose.lottie.format.values.BezierValue
import com.google.android.horologist.remotecompose.lottie.format.values.Point
import com.google.android.horologist.remotecompose.lottie.renderer.RemoteCompiledGeometry
import com.google.android.horologist.remotecompose.lottie.renderer.RemoteDynamicGeometry
import com.google.android.horologist.remotecompose.lottie.renderer.RemoteShape
import com.google.android.horologist.remotecompose.lottie.renderer.modifiers.evaluatePathGeometry
import com.google.android.horologist.remotecompose.lottie.renderer.properties.RemoteBezierValue
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animatePosition
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animateVector

private const val ELLIPSE_CONTROL_POINT_CONSTANT = 0.55228f

/** Evaluates a Lottie [Ellipse] into a [RemoteShape]. */
@SuppressLint("RestrictedApi")
internal fun ellipse(
  el: Ellipse,
  animationSettings: LottieSettings,
  trimPath: TrimPath? = null,
  roundedCorners: RoundedCorners? = null,
): RemoteShape? {
  if (el.hidden?.constantValue == true) return null

  val pos = animatePosition(el.position, animationSettings)
  val posX = pos.x.constantValueOrNull ?: 0f
  val posY = pos.y.constantValueOrNull ?: 0f

  val size = animateVector(el.size, animationSettings)
  val width = size.getOrNull(0)?.constantValueOrNull ?: 0f
  val height = size.getOrNull(1)?.constantValueOrNull ?: 0f
  val halfWidth = width / 2f
  val halfHeight = height / 2f

  val cpW = halfWidth * ELLIPSE_CONTROL_POINT_CONSTANT
  val cpH = halfHeight * ELLIPSE_CONTROL_POINT_CONSTANT

  val vertices: List<Point>
  val inTangents: List<Point>
  val outTangents: List<Point>

  if (el.direction == 3) {
    // Reversed (counter-clockwise)
    vertices =
      listOf(
        Point(posX.rf, (posY - halfHeight).rf),
        Point((posX - halfWidth).rf, posY.rf),
        Point(posX.rf, (posY + halfHeight).rf),
        Point((posX + halfWidth).rf, posY.rf),
      )
    inTangents =
      listOf(
        Point(cpW.rf, 0f.rf),
        Point(0f.rf, (-cpH).rf),
        Point((-cpW).rf, 0f.rf),
        Point(0f.rf, cpH.rf),
      )
    outTangents =
      listOf(
        Point((-cpW).rf, 0f.rf),
        Point(0f.rf, cpH.rf),
        Point(cpW.rf, 0f.rf),
        Point(0f.rf, (-cpH).rf),
      )
  } else {
    // Clockwise
    vertices =
      listOf(
        Point(posX.rf, (posY - halfHeight).rf),
        Point((posX + halfWidth).rf, posY.rf),
        Point(posX.rf, (posY + halfHeight).rf),
        Point((posX - halfWidth).rf, posY.rf),
      )
    inTangents =
      listOf(
        Point((-cpW).rf, 0f.rf),
        Point(0f.rf, (-cpH).rf),
        Point(cpW.rf, 0f.rf),
        Point(0f.rf, cpH.rf),
      )
    outTangents =
      listOf(
        Point(cpW.rf, 0f.rf),
        Point(0f.rf, cpH.rf),
        Point((-cpW).rf, 0f.rf),
        Point(0f.rf, (-cpH).rf),
      )
  }

  val remoteBezier =
    RemoteBezierValue(
      closed = true,
      inTangents = inTangents,
      outTangents = outTangents,
      vertices = vertices,
    )

  val hasTrim = trimPath != null && trimPath.hidden?.constantValue != true
  val hasRounding = roundedCorners != null && roundedCorners.hidden?.constantValue != true
  if (hasTrim || hasRounding) {
    val bezierValue =
      BezierValue(
        closed = true.rb,
        vertices = vertices,
        inTangents = inTangents,
        outTangents = outTangents,
      )
    val evaluated =
      evaluatePathGeometry(
        StaticBezierProperty(value = bezierValue, animated = false.rb),
        trimPath,
        roundedCorners,
        animationSettings,
      )
    return RemoteDynamicGeometry(evaluated)
  }

  val rcPath = RemotePath()
  rcPath.reset()

  rcPath.moveTo(posX, posY - halfHeight)
  rcPath.cubicTo(
    posX + cpW,
    posY - halfHeight,
    posX + halfWidth,
    posY - cpH,
    posX + halfWidth,
    posY,
  )
  rcPath.cubicTo(
    posX + halfWidth,
    posY + cpH,
    posX + cpW,
    posY + halfHeight,
    posX,
    posY + halfHeight,
  )
  rcPath.cubicTo(
    posX - cpW,
    posY + halfHeight,
    posX - halfWidth,
    posY + cpH,
    posX - halfWidth,
    posY,
  )
  rcPath.cubicTo(
    posX - halfWidth,
    posY - cpH,
    posX - cpW,
    posY - halfHeight,
    posX,
    posY - halfHeight,
  )
  rcPath.close()

  return RemoteCompiledGeometry(rcPath, bezierSubpaths = listOf(remoteBezier))
}
