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
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.tween
import androidx.compose.ui.graphics.Color
import com.google.android.horologist.remotecompose.lottie.LottieSettings
import com.google.android.horologist.remotecompose.lottie.format.properties.AnimatedColorProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.BaseColorProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticColorProperty

@SuppressLint("RestrictedApi")
internal object ColorInterpolator : KeyframeInterpolator<RemoteColor, RemoteColor> {
  override fun toResult(value: RemoteColor): RemoteColor = value

  override fun interpolate(
    start: RemoteColor,
    end: RemoteColor,
    progress: RemoteFloat,
  ): RemoteColor = tween(start, end, progress)

  override fun hold(
    start: RemoteColor,
    end: RemoteColor,
    frameInAnimation: RemoteFloat,
    duration: Float,
  ): RemoteColor = frameInAnimation.isLessThan(duration.rf).select(ifTrue = start, ifFalse = end)

  override fun select(
    frame: RemoteFloat,
    threshold: RemoteFloat,
    ifTrue: RemoteColor,
    ifFalse: RemoteColor,
  ): RemoteColor = frame.isLessThan(threshold).select(ifTrue = ifTrue, ifFalse = ifFalse)
}

/**
 * Animates a color property.
 *
 * Takes a [BaseColorProperty] (either static or animated) and resolves it to a [RemoteColor].
 * Supports dynamic slot color overrides from [LottieSettings.slotMap], keyframed transitions with
 * cubic Bézier easing curves, and hold keyframes.
 */
@SuppressLint("RestrictedApi")
internal fun animateColor(
  property: BaseColorProperty,
  animationSettings: LottieSettings,
): RemoteColor {
  val slotColor = property.slotId?.let { animationSettings.slotMap.getColor(it) }
  if (slotColor != null) {
    return slotColor
  }

  return when (property) {
    is StaticColorProperty -> property.value
    is AnimatedColorProperty ->
      evaluateKeyframes(
        keyframes = property.keyframes,
        animationSettings = animationSettings,
        getFrame = { it.frame },
        getValue = { it.value },
        getHold = { it.hold },
        getInTangent = { it.inTangent },
        getOutTangent = { it.outTangent },
        defaultValue = { Color.Transparent.rc },
        interpolator = ColorInterpolator,
      )
  }
}
