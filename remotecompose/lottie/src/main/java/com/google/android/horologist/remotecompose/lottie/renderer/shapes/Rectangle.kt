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
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.Rectangle
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
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animateScalar
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animateVector

private const val RECTANGLE_CORNER_RADIUS_CONTROL_POINT_CONSTANT = 0.55228475f

/** Evaluates a Lottie [Rectangle] into a [RemoteShape]. */
@SuppressLint("RestrictedApi")
internal fun rectangle(
  rect: Rectangle,
  animationSettings: LottieSettings,
  trimPath: TrimPath? = null,
  roundedCorners: RoundedCorners? = null,
): RemoteShape? {
  if (rect.hidden?.constantValue == true) return null

  val pos = animatePosition(rect.position, animationSettings)
  val posX = pos.x.constantValueOrNull ?: 0f
  val posY = pos.y.constantValueOrNull ?: 0f

  val size = animateVector(rect.size, animationSettings)
  val width = size.getOrNull(0)?.constantValueOrNull ?: 0f
  val height = size.getOrNull(1)?.constantValueOrNull ?: 0f
  val halfWidth = width / 2f
  val halfHeight = height / 2f

  val cornerRadius =
    rect.cornerRadius?.let { animateScalar(it, animationSettings).constantValueOrNull } ?: 0f
  val maxRadius = minOf(halfWidth, halfHeight)
  val r = cornerRadius.coerceIn(0f, maxRadius)
  val kr = r * RECTANGLE_CORNER_RADIUS_CONTROL_POINT_CONSTANT

  val vertices =
    listOf(
      Point((posX + halfWidth).rf, (posY - halfHeight + r).rf),
      Point((posX + halfWidth).rf, (posY + halfHeight - r).rf),
      Point((posX + halfWidth - r).rf, (posY + halfHeight).rf),
      Point((posX - halfWidth + r).rf, (posY + halfHeight).rf),
      Point((posX - halfWidth).rf, (posY + halfHeight - r).rf),
      Point((posX - halfWidth).rf, (posY - halfHeight + r).rf),
      Point((posX - halfWidth + r).rf, (posY - halfHeight).rf),
      Point((posX + halfWidth - r).rf, (posY - halfHeight).rf),
    )
  val inTangents =
    listOf(
      Point(0f.rf, (-kr).rf),
      Point(0f.rf, 0f.rf),
      Point(kr.rf, 0f.rf),
      Point(0f.rf, 0f.rf),
      Point(0f.rf, kr.rf),
      Point(0f.rf, 0f.rf),
      Point((-kr).rf, 0f.rf),
      Point(0f.rf, 0f.rf),
    )
  val outTangents =
    listOf(
      Point(0f.rf, 0f.rf),
      Point(0f.rf, kr.rf),
      Point(0f.rf, 0f.rf),
      Point((-kr).rf, 0f.rf),
      Point(0f.rf, 0f.rf),
      Point(0f.rf, (-kr).rf),
      Point(0f.rf, 0f.rf),
      Point(kr.rf, 0f.rf),
    )

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

  if (r == 0f) {
    rcPath.moveTo(posX + halfWidth, posY - halfHeight)
    rcPath.lineTo(posX + halfWidth, posY + halfHeight)
    rcPath.lineTo(posX - halfWidth, posY + halfHeight)
    rcPath.lineTo(posX - halfWidth, posY - halfHeight)
    rcPath.close()
  } else {
    val k = r * 0.55228475f
    rcPath.moveTo(posX + halfWidth, posY - halfHeight + r)
    rcPath.lineTo(posX + halfWidth, posY + halfHeight - r)
    rcPath.cubicTo(
      posX + halfWidth,
      posY + halfHeight - r + k,
      posX + halfWidth - r + k,
      posY + halfHeight,
      posX + halfWidth - r,
      posY + halfHeight,
    )
    rcPath.lineTo(posX - halfWidth + r, posY + halfHeight)
    rcPath.cubicTo(
      posX - halfWidth + r - k,
      posY + halfHeight,
      posX - halfWidth,
      posY + halfHeight - r + k,
      posX - halfWidth,
      posY + halfHeight - r,
    )
    rcPath.lineTo(posX - halfWidth, posY - halfHeight + r)
    rcPath.cubicTo(
      posX - halfWidth,
      posY - halfHeight + r - k,
      posX - halfWidth + r - k,
      posY - halfHeight,
      posX - halfWidth + r,
      posY - halfHeight,
    )
    rcPath.lineTo(posX + halfWidth - r, posY - halfHeight)
    rcPath.cubicTo(
      posX + halfWidth - r + k,
      posY - halfHeight,
      posX + halfWidth,
      posY - halfHeight + r - k,
      posX + halfWidth,
      posY - halfHeight + r,
    )
    rcPath.close()
  }

  return RemoteCompiledGeometry(rcPath, bezierSubpaths = listOf(remoteBezier))
}
