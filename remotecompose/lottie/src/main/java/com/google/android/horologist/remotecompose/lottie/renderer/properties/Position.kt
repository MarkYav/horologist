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
import com.google.android.horologist.remotecompose.lottie.format.properties.AnimatedPositionProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.BasePositionProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.SplitPositionProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticPositionProperty
import com.google.android.horologist.remotecompose.lottie.renderer.lookupValueInBezier
import com.google.android.horologist.remotecompose.lottie.renderer.scalarLinearEasingIn
import com.google.android.horologist.remotecompose.lottie.renderer.scalarLinearEasingOut

/** A 2D point represented with RemoteFloats. */
@SuppressLint("RestrictedApi") internal data class Point(val x: RemoteFloat, val y: RemoteFloat)

@SuppressLint("RestrictedApi")
internal object PositionInterpolator : KeyframeInterpolator<List<Float>, Point> {
  override fun toResult(value: List<Float>): Point =
    Point(value.getOrElse(0) { 0f }.rf, value.getOrElse(1) { 0f }.rf)

  override fun interpolate(start: List<Float>, end: List<Float>, progress: RemoteFloat): Point {
    val startX = start.getOrElse(0) { 0f }
    val startY = start.getOrElse(1) { 0f }
    val endX = end.getOrElse(0) { startX }
    val endY = end.getOrElse(1) { startY }
    return Point(lerp(startX.rf, endX.rf, progress), lerp(startY.rf, endY.rf, progress))
  }

  override fun hold(
    start: List<Float>,
    end: List<Float>,
    frameInAnimation: RemoteFloat,
    duration: Float,
  ): Point {
    val startX = start.getOrElse(0) { 0f }
    val startY = start.getOrElse(1) { 0f }
    val endX = end.getOrElse(0) { startX }
    val endY = end.getOrElse(1) { startY }
    return Point(
      selectIfLt(frameInAnimation, duration.rf, startX.rf, endX.rf),
      selectIfLt(frameInAnimation, duration.rf, startY.rf, endY.rf),
    )
  }

  override fun select(
    frame: RemoteFloat,
    threshold: RemoteFloat,
    ifTrue: Point,
    ifFalse: Point,
  ): Point =
    Point(
      selectIfLt(frame, threshold, ifTrue.x, ifFalse.x),
      selectIfLt(frame, threshold, ifTrue.y, ifFalse.y),
    )
}

/**
 * Animates a position property.
 *
 * Takes a [BasePositionProperty] (either static, split, or animated) and resolves it to a [Point]
 * of [RemoteFloat]s (x, y). Supports keyframed transitions with cubic Bézier easing curves, hold
 * keyframes, delayed starts, and split dimensional scalar animations.
 */
@SuppressLint("RestrictedApi")
internal fun animatePosition(
  position: BasePositionProperty,
  animationSettings: LottieSettings,
): Point {
  return when (position) {
    is StaticPositionProperty ->
      Point(position.value.getOrElse(0) { 0f }.rf, position.value.getOrElse(1) { 0f }.rf)
    is SplitPositionProperty ->
      Point(
        x = animateScalar(position.x, animationSettings),
        y = animateScalar(position.y, animationSettings),
      )
    is AnimatedPositionProperty ->
      evaluateKeyframes(
        keyframes = position.keyframes,
        animationSettings = animationSettings,
        getFrame = { it.frame },
        getValue = { it.value },
        getHold = { it.hold },
        getInTangent = { it.inTangent },
        getOutTangent = { it.outTangent },
        defaultValue = { Point(0f.rf, 0f.rf) },
        interpolator = PositionInterpolator,
        customSegmentValue = { startKf, endKf, duration, frameInAnimation ->
          val startX = startKf.value.getOrElse(0) { 0f }
          val startY = startKf.value.getOrElse(1) { 0f }
          val endX = endKf.value.getOrElse(0) { startX }
          val endY = endKf.value.getOrElse(1) { startY }

          if (startKf.hold || duration <= 0f) {
            Point(
              selectIfLt(frameInAnimation, duration.rf, startX.rf, endX.rf),
              selectIfLt(frameInAnimation, duration.rf, startY.rf, endY.rf),
            )
          } else {
            val outTangent = startKf.outTangent ?: scalarLinearEasingOut
            val inTangent = startKf.inTangent ?: scalarLinearEasingIn
            val progress =
              lookupValueInBezier(
                outTangent.x,
                outTangent.y,
                inTangent.x,
                inTangent.y,
                duration,
                frameInAnimation,
              )
            val spatialOut = startKf.spatialOutTangent
            val spatialIn = startKf.spatialInTangent ?: endKf.spatialInTangent
            if (spatialOut != null || spatialIn != null) {
              val toX = spatialOut?.getOrElse(0) { 0f } ?: 0f
              val toY = spatialOut?.getOrElse(1) { 0f } ?: 0f
              val tiX = spatialIn?.getOrElse(0) { 0f } ?: 0f
              val tiY = spatialIn?.getOrElse(1) { 0f } ?: 0f

              val c1x = startX + toX
              val c1y = startY + toY
              val c2x = endX + tiX
              val c2y = endY + tiY

              val s = progress
              val oneMinusS = 1f.rf - s
              val oneMinusS2 = oneMinusS * oneMinusS
              val oneMinusS3 = oneMinusS2 * oneMinusS
              val s2 = s * s
              val s3 = s2 * s

              val c0 = oneMinusS3
              val c1 = 3f.rf * oneMinusS2 * s
              val c2 = 3f.rf * oneMinusS * s2
              val c3 = s3

              val bX = c0 * startX.rf + c1 * c1x.rf + c2 * c2x.rf + c3 * endX.rf
              val bY = c0 * startY.rf + c1 * c1y.rf + c2 * c2y.rf + c3 * endY.rf
              Point(bX, bY)
            } else {
              Point(lerp(startX.rf, endX.rf, progress), lerp(startY.rf, endY.rf, progress))
            }
          }
        },
      )
  }
}
