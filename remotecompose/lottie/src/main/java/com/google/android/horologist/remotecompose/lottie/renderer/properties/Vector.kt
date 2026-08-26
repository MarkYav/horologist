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
import androidx.compose.remote.creation.compose.state.lerp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.selectIfLt
import com.google.android.horologist.remotecompose.lottie.LottieSettings
import com.google.android.horologist.remotecompose.lottie.format.properties.AnimatedVectorProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.BaseVectorProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticVectorProperty

@SuppressLint("RestrictedApi")
internal object VectorInterpolator : KeyframeInterpolator<List<Float>, List<RemoteFloat>> {
  override fun toResult(value: List<Float>): List<RemoteFloat> = value.map { it.rf }

  override fun interpolate(
    start: List<Float>,
    end: List<Float>,
    progress: RemoteFloat,
  ): List<RemoteFloat> = start.mapIndexed { idx, sv ->
    lerp(sv.rf, (end.getOrElse(idx) { sv }).rf, progress)
  }

  override fun hold(
    start: List<Float>,
    end: List<Float>,
    frameInAnimation: RemoteFloat,
    duration: Float,
  ): List<RemoteFloat> = start.mapIndexed { idx, sv ->
    val ev = end.getOrElse(idx) { sv }
    selectIfLt(frameInAnimation, duration.rf, sv.rf, ev.rf)
  }

  override fun select(
    frame: RemoteFloat,
    threshold: RemoteFloat,
    ifTrue: List<RemoteFloat>,
    ifFalse: List<RemoteFloat>,
  ): List<RemoteFloat> = ifTrue.mapIndexed { idx, tv ->
    val fv = ifFalse.getOrElse(idx) { tv }
    selectIfLt(frame, threshold, tv, fv)
  }
}

/**
 * Animates a vector property.
 *
 * Takes a [BaseVectorProperty] (either static or animated) and resolves it to a list of
 * [RemoteFloat]s. Supports keyframed transitions with cubic Bézier easing curves, hold keyframes,
 * and delayed starts.
 */
@SuppressLint("RestrictedApi")
internal fun animateVector(
  vector: BaseVectorProperty,
  animationSettings: LottieSettings,
): List<RemoteFloat> {
  return when (vector) {
    is StaticVectorProperty -> vector.value.map { it.rf }
    is AnimatedVectorProperty ->
      evaluateKeyframes(
        keyframes = vector.keyframes,
        animationSettings = animationSettings,
        getFrame = { it.frame },
        getValue = { it.value },
        getHold = { it.hold },
        getInTangent = { it.inTangent },
        getOutTangent = { it.outTangent },
        defaultValue = { emptyList() },
        interpolator = VectorInterpolator,
      )
  }
}
