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
import com.google.android.horologist.remotecompose.lottie.format.properties.ScalarKeyframeEasing
import com.google.android.horologist.remotecompose.lottie.renderer.lookupValueInBezier
import com.google.android.horologist.remotecompose.lottie.renderer.scalarLinearEasingIn
import com.google.android.horologist.remotecompose.lottie.renderer.scalarLinearEasingOut

/** Represents a single animation segment starting at [startFrame] with evaluated result [value]. */
internal data class KeyframeSegment<T>(val startFrame: Float, val value: T)

/** Strategy interface for interpolating keyframed property values. */
@SuppressLint("RestrictedApi")
internal interface KeyframeInterpolator<V, R> {
  /** Converts a static or single keyframe value of type [V] to result type [R]. */
  fun toResult(value: V): R

  /** Interpolates between [start] and [end] values at normalized [progress] in [0..1]. */
  fun interpolate(start: V, end: V, progress: RemoteFloat): R

  /** Resolves hold keyframe value at [frameInAnimation] within [duration]. */
  fun hold(start: V, end: V, frameInAnimation: RemoteFloat, duration: Float): R

  /** Selects between [ifTrue] and [ifFalse] based on [frame] < [threshold]. */
  fun select(frame: RemoteFloat, threshold: RemoteFloat, ifTrue: R, ifFalse: R): R
}

/**
 * Chains keyframed animation segments into a balanced $O(\log K)$ binary decision tree.
 *
 * This reduces RemoteCompose expression depth and interpreter stack usage significantly compared to
 * linear chains.
 */
@SuppressLint("RestrictedApi")
internal fun <T> chainSegmentsBinary(
  segments: List<KeyframeSegment<T>>,
  frame: RemoteFloat,
  selector: (frame: RemoteFloat, threshold: RemoteFloat, ifTrue: T, ifFalse: T) -> T,
): T {
  require(segments.isNotEmpty()) { "Segments list must not be empty" }
  return buildSegmentTree(segments, 0, segments.size - 1, frame, selector)
}

@SuppressLint("RestrictedApi")
private fun <T> buildSegmentTree(
  segments: List<KeyframeSegment<T>>,
  low: Int,
  high: Int,
  frame: RemoteFloat,
  selector: (frame: RemoteFloat, threshold: RemoteFloat, ifTrue: T, ifFalse: T) -> T,
): T {
  if (low == high) {
    return segments[low].value
  }
  if (low + 1 == high) {
    return selector(frame, segments[high].startFrame.rf, segments[low].value, segments[high].value)
  }
  val mid = low + (high - low + 1) / 2
  val left = buildSegmentTree(segments, low, mid - 1, frame, selector)
  val right = buildSegmentTree(segments, mid, high, frame, selector)
  return selector(frame, segments[mid].startFrame.rf, left, right)
}

/** Universal evaluator for keyframed properties across timelines. */
@SuppressLint("RestrictedApi")
internal inline fun <K, V, R> evaluateKeyframes(
  keyframes: List<K>,
  animationSettings: LottieSettings,
  getFrame: (K) -> Float,
  getValue: (K) -> V,
  getHold: (K) -> Boolean,
  getInTangent: (K) -> ScalarKeyframeEasing?,
  getOutTangent: (K) -> ScalarKeyframeEasing?,
  defaultValue: () -> R,
  interpolator: KeyframeInterpolator<V, R>,
  noinline customSegmentValue:
    ((startKeyframe: K, endKeyframe: K, duration: Float, frameInAnimation: RemoteFloat) -> R)? =
    null,
): R {
  if (keyframes.isEmpty()) {
    return defaultValue()
  }
  if (keyframes.size == 1) {
    return interpolator.toResult(getValue(keyframes[0]))
  }

  val segments = mutableListOf<KeyframeSegment<R>>()
  val firstKeyframe = keyframes[0]
  if (getFrame(firstKeyframe) != 0f) {
    segments.add(KeyframeSegment(0f, interpolator.toResult(getValue(firstKeyframe))))
  }

  for (i in 0 until keyframes.size - 1) {
    val startKf = keyframes[i]
    val endKf = keyframes[i + 1]
    val startFrame = getFrame(startKf)
    val endFrame = getFrame(endKf)
    val duration = endFrame - startFrame
    val frameInAnimation = animationSettings.currentFrame - startFrame
    val startVal = getValue(startKf)
    val endVal = getValue(endKf)

    val segmentVal =
      if (customSegmentValue != null) {
        customSegmentValue(startKf, endKf, duration, frameInAnimation)
      } else if (getHold(startKf) || duration <= 0f) {
        interpolator.hold(startVal, endVal, frameInAnimation, duration)
      } else {
        val outTangent = getOutTangent(startKf) ?: scalarLinearEasingOut
        val inTangent = getInTangent(startKf) ?: scalarLinearEasingIn
        val progress =
          lookupValueInBezier(
            outTangent.x,
            outTangent.y,
            inTangent.x,
            inTangent.y,
            duration,
            frameInAnimation,
          )
        interpolator.interpolate(startVal, endVal, progress)
      }

    segments.add(KeyframeSegment(startFrame, segmentVal))
  }

  return chainSegmentsBinary(segments, animationSettings.currentFrame) {
    frame,
    threshold,
    ifTrue,
    ifFalse ->
    interpolator.select(frame, threshold, ifTrue, ifFalse)
  }
}
