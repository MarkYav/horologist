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
import com.google.android.horologist.remotecompose.lottie.LottieSettings
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.Path
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.RoundedCorners
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.TrimPath
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticBezierProperty
import com.google.android.horologist.remotecompose.lottie.renderer.RemoteCompiledGeometry
import com.google.android.horologist.remotecompose.lottie.renderer.RemoteDynamicGeometry
import com.google.android.horologist.remotecompose.lottie.renderer.RemoteShape
import com.google.android.horologist.remotecompose.lottie.renderer.modifiers.evaluatePathGeometry
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animateBezierSubpaths
import com.google.android.horologist.remotecompose.lottie.renderer.properties.toRemote

/**
 * Evaluates a Lottie [Path] into an evaluated geometry ([RemoteCompiledGeometry] or
 * [RemoteDynamicGeometry]).
 */
@SuppressLint("RestrictedApi")
internal fun path(
  lottiePath: Path,
  animationSettings: LottieSettings,
  trimPath: TrimPath? = null,
  roundedCorners: RoundedCorners? = null,
): RemoteShape? {
  if (lottiePath.hidden?.constantValue == true) return null

  val hasTrim = trimPath != null && trimPath.hidden?.constantValue != true
  val hasRounding = roundedCorners != null && roundedCorners.hidden?.constantValue != true

  if (hasTrim || hasRounding) {
    val evaluated =
      evaluatePathGeometry(lottiePath.shape, trimPath, roundedCorners, animationSettings)
    return RemoteDynamicGeometry(evaluated)
  }

  if (lottiePath.shape is StaticBezierProperty) {
    val bezier = lottiePath.shape.value
    val vertices = bezier.vertices
    val inTangents = bezier.inTangents
    val outTangents = bezier.outTangents

    if (vertices.isEmpty()) return null

    val rcPath = RemotePath()
    rcPath.reset()

    val startX = vertices[0].x.constantValueOrNull ?: 0f
    val startY = vertices[0].y.constantValueOrNull ?: 0f
    rcPath.moveTo(startX, startY)

    val isClosed = bezier.closed.constantValue
    val maxIndex = if (isClosed) vertices.size else vertices.size - 1
    for (i in 0 until maxIndex) {
      val p0 = vertices[i]
      val lastIndex = if (i == vertices.size - 1 && isClosed) 0 else i + 1
      val p4 = vertices[lastIndex]
      val inTangent = inTangents.getOrNull(lastIndex)
      val outTangent = outTangents.getOrNull(i)

      val p0x = p0.x.constantValueOrNull ?: 0f
      val p0y = p0.y.constantValueOrNull ?: 0f
      val p4x = p4.x.constantValueOrNull ?: 0f
      val p4y = p4.y.constantValueOrNull ?: 0f

      val outX = outTangent?.x?.constantValueOrNull ?: 0f
      val outY = outTangent?.y?.constantValueOrNull ?: 0f
      val inX = inTangent?.x?.constantValueOrNull ?: 0f
      val inY = inTangent?.y?.constantValueOrNull ?: 0f

      if (outX == 0f && outY == 0f && inX == 0f && inY == 0f) {
        rcPath.lineTo(p4x, p4y)
      } else {
        val p1x = p0x + outX
        val p1y = p0y + outY
        val p2x = p4x + inX
        val p2y = p4y + inY
        rcPath.cubicTo(p1x, p1y, p2x, p2y, p4x, p4y)
      }
    }

    if (isClosed) {
      rcPath.close()
    }

    return RemoteCompiledGeometry(rcPath, bezierSubpaths = listOf(bezier.toRemote()))
  }

  val dynamicSubpaths = animateBezierSubpaths(lottiePath.shape, animationSettings)
  return RemoteDynamicGeometry(dynamicSubpaths)
}
