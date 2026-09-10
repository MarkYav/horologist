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
import androidx.compose.remote.creation.compose.state.lerp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.selectIfLt
import com.google.android.horologist.remotecompose.lottie.LottieSettings
import com.google.android.horologist.remotecompose.lottie.format.properties.AnimatedPositionProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.BasePositionProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.SplitPositionProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticPositionProperty
import com.google.android.horologist.remotecompose.lottie.format.values.Point
import com.google.android.horologist.remotecompose.lottie.renderer.lookupValueInBezier
import com.google.android.horologist.remotecompose.lottie.renderer.scalarLinearEasingIn
import com.google.android.horologist.remotecompose.lottie.renderer.scalarLinearEasingOut

/**
 * Animates a position property.
 *
 * Take a BasePositionProperty (either animated, split, or static) and convert it to a [Point] of
 * RemoteFloats (x, y). If the position is animated, the RemoteFloats will change based on the
 * animation specified in the Lottie Position Property.
 */
@SuppressLint("RestrictedApi")
internal fun animatePosition(
  position: BasePositionProperty,
  animationSettings: LottieSettings,
): Point {
  return when (position) {
    // Static constant position: directly return the Point.
    is StaticPositionProperty -> position.value
    // Split position: evaluate x, y scalar properties independently.
    is SplitPositionProperty -> {
      Point(
        x = animateScalar(position.x, animationSettings),
        y = animateScalar(position.y, animationSettings),
      )
    }
    // Keyframed animated position: interpolate [x, y] across keyframes using Bézier easing curves.
    is AnimatedPositionProperty -> {
      if (position.keyframes.isEmpty()) {
        return Point(0f.rf, 0f.rf)
      }
      // Single keyframe: hold static position at that single value.
      if (position.keyframes.size == 1) {
        return position.keyframes[0].value
      }

      val segmentsX = mutableListOf<AnimationSegment>()
      val segmentsY = mutableListOf<AnimationSegment>()

      // If the first keyframe starts after frame 0, prepend an initial static segment
      // holding the first keyframe's value from frame 0 until the first keyframe.
      val firstKeyframe = position.keyframes[0]
      if (firstKeyframe.frame.constantValue != 0f) {
        segmentsX.add(AnimationSegment(0f, firstKeyframe.value.x))
        segmentsY.add(AnimationSegment(0f, firstKeyframe.value.y))
      }

      // Build interpolation segments between adjacent keyframe pairs.
      for (i in 0 until position.keyframes.size - 1) {
        val startKeyframe = position.keyframes[i]
        val endKeyframe = position.keyframes[i + 1]
        val duration = endKeyframe.frame.constantValue - startKeyframe.frame.constantValue
        val frameInAnimation = animationSettings.currentFrame - startKeyframe.frame

        val startX = startKeyframe.value.x
        val startY = startKeyframe.value.y
        val endX = endKeyframe.value.x
        val endY = endKeyframe.value.y

        val isHold = startKeyframe.hold.constantValue || duration <= 0f

        val (segX, segY) =
          if (isHold) {
            val hX = selectIfLt(frameInAnimation, duration.rf, startX, endX)
            val hY = selectIfLt(frameInAnimation, duration.rf, startY, endY)
            hX to hY
          } else {
            // Control point tangents for the cubic Bézier curve, defaulting to linear easing if
            // omitted.
            val outTangent = startKeyframe.outTangent ?: scalarLinearEasingOut
            val inTangent = startKeyframe.inTangent ?: scalarLinearEasingIn

            // Evaluate the cubic Bézier curve to obtain the normalized interpolation factor [0.0,
            // 1.0].
            val currentBezierValue =
              lookupValueInBezier(
                outTangent.x,
                outTangent.y,
                inTangent.x,
                inTangent.y,
                duration,
                frameInAnimation,
              )

            val spatialOut = startKeyframe.outSpatialTangent
            val spatialIn = startKeyframe.inSpatialTangent ?: endKeyframe.inSpatialTangent

            if (spatialOut != null || spatialIn != null) {
              val toX = spatialOut?.x ?: 0f.rf
              val toY = spatialOut?.y ?: 0f.rf
              val tiX = spatialIn?.x ?: 0f.rf
              val tiY = spatialIn?.y ?: 0f.rf

              val c1x = startX + toX
              val c1y = startY + toY
              val c2x = endX + tiX
              val c2y = endY + tiY

              val s = currentBezierValue
              val oneMinusS = 1f.rf - s
              val oneMinusS2 = oneMinusS * oneMinusS
              val oneMinusS3 = oneMinusS2 * oneMinusS
              val s2 = s * s
              val s3 = s2 * s

              val c0 = oneMinusS3
              val c1 = 3f.rf * oneMinusS2 * s
              val c2 = 3f.rf * oneMinusS * s2
              val c3 = s3

              val bX = c0 * startX + c1 * c1x + c2 * c2x + c3 * endX
              val bY = c0 * startY + c1 * c1y + c2 * c2y + c3 * endY
              bX to bY
            } else {
              // Linearly interpolate each coordinate (x, y) between the start and end keyframe
              // values.
              val lX = lerp(startX, endX, currentBezierValue)
              val lY = lerp(startY, endY, currentBezierValue)
              lX to lY
            }
          }

        segmentsX.add(AnimationSegment(startKeyframe.frame.constantValue, segX))
        segmentsY.add(AnimationSegment(startKeyframe.frame.constantValue, segY))
      }

      // Chain individual segments together into conditional expressions that resolve
      // the appropriate interpolated value for X and Y based on currentFrame.
      val chainedX = chainAnimation(segmentsX, animationSettings.currentFrame)
      val chainedY = chainAnimation(segmentsY, animationSettings.currentFrame)

      Point(x = chainedX, y = chainedY)
    }
  }
}
