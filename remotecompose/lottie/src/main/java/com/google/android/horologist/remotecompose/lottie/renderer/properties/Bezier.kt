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

package com.google.android.horologist.remotecompose.lottie.renderer.properties

import android.annotation.SuppressLint
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.rf
import com.google.android.horologist.remotecompose.lottie.LottieSettings
import com.google.android.horologist.remotecompose.lottie.format.properties.AnimatedBezierProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.BaseBezierProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticBezierProperty
import com.google.android.horologist.remotecompose.lottie.format.values.BezierValue

internal data class RemoteBezierValue(
  val closed: Boolean,
  val inTangents: List<List<RemoteFloat>>,
  val outTangents: List<List<RemoteFloat>>,
  val vertices: List<List<RemoteFloat>>,
)

internal fun BezierValue.toRemote(): RemoteBezierValue {
  return RemoteBezierValue(
    closed = closed,
    inTangents = inTangents.map { point -> point.map { it.rf } },
    outTangents = outTangents.map { point -> point.map { it.rf } },
    vertices = vertices.map { point -> point.map { it.rf } },
  )
}

/**
 * Animates a bezier (shape) property.
 *
 * Takes a [BaseBezierProperty] (either static or animated) and resolves it to a list of
 * [RemoteBezierValue] subpaths.
 */
@SuppressLint("RestrictedApi")
internal fun animateBezier(
  path: BaseBezierProperty,
  animationSettings: LottieSettings,
): List<RemoteBezierValue> {
  return when (path) {
    is StaticBezierProperty -> listOf(path.value.toRemote())
    is AnimatedBezierProperty -> {
      if (path.keyframes.isEmpty()) {
        emptyList()
      } else {
        path.keyframes[0].value.map { it.toRemote() }
      }
    }
  }
}
