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
import com.google.android.horologist.remotecompose.lottie.format.properties.AnimatedScalarProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.BaseScalarProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticScalarProperty

@SuppressLint("RestrictedApi")
internal object ScalarInterpolator : KeyframeInterpolator<Float, RemoteFloat> {
  override fun toResult(value: Float): RemoteFloat = value.rf

  override fun interpolate(start: Float, end: Float, progress: RemoteFloat): RemoteFloat =
    lerp(start.rf, end.rf, progress)

  override fun hold(
    start: Float,
    end: Float,
    frameInAnimation: RemoteFloat,
    duration: Float,
  ): RemoteFloat = selectIfLt(frameInAnimation, duration.rf, start.rf, end.rf)

  override fun select(
    frame: RemoteFloat,
    threshold: RemoteFloat,
    ifTrue: RemoteFloat,
    ifFalse: RemoteFloat,
  ): RemoteFloat = selectIfLt(frame, threshold, ifTrue, ifFalse)
}

/**
 * Animates a scalar property.
 *
 * Takes a [BaseScalarProperty] (either static or animated) and resolves it to a [RemoteFloat].
 * Supports keyframed transitions with cubic Bézier easing curves, hold keyframes, and delayed
 * starts.
 */
@SuppressLint("RestrictedApi")
internal fun animateScalar(
  scalar: BaseScalarProperty,
  animationSettings: LottieSettings,
): RemoteFloat {
  return when (scalar) {
    is StaticScalarProperty -> scalar.value.rf
    is AnimatedScalarProperty ->
      evaluateKeyframes(
        keyframes = scalar.keyframes,
        animationSettings = animationSettings,
        getFrame = { it.frame },
        getValue = { it.value },
        getHold = { it.hold },
        getInTangent = { it.inTangent },
        getOutTangent = { it.outTangent },
        defaultValue = { 0f.rf },
        interpolator = ScalarInterpolator,
      )
  }
}
