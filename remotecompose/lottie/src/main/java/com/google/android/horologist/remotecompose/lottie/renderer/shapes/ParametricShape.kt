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
import com.google.android.horologist.remotecompose.lottie.LottieSettings
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.RoundedCorners
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.TrimPath
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticBezierProperty
import com.google.android.horologist.remotecompose.lottie.format.values.BezierValue
import com.google.android.horologist.remotecompose.lottie.renderer.RemoteLottiePath
import com.google.android.horologist.remotecompose.lottie.renderer.properties.RemoteBezierValue

/** Applies modifier fallback (TrimPath and RoundedCorners) to a parametric Bézier subpath. */
@SuppressLint("RestrictedApi")
internal fun evaluateParametricShape(
  remoteBezier: RemoteBezierValue,
  trimPath: TrimPath? = null,
  roundedCorners: RoundedCorners? = null,
  animationSettings: LottieSettings,
): RemoteLottiePath {
  val hasTrim = trimPath != null && trimPath.hidden != true
  val hasRounding = roundedCorners != null && roundedCorners.hidden != true
  if (hasTrim || hasRounding) {
    val bezierValue =
      BezierValue(
        closed = remoteBezier.closed,
        vertices = remoteBezier.vertices.map { pt -> pt.map { it.constantValueOrNull ?: 0f } },
        inTangents = remoteBezier.inTangents.map { pt -> pt.map { it.constantValueOrNull ?: 0f } },
        outTangents = remoteBezier.outTangents.map { pt -> pt.map { it.constantValueOrNull ?: 0f } },
      )
    val evaluated =
      evaluatePathGeometry(
        StaticBezierProperty(value = bezierValue),
        trimPath,
        roundedCorners,
        animationSettings,
      )
    return RemoteLottiePath(evaluated)
  }

  return RemoteLottiePath(listOf(remoteBezier))
}
