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
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.selectIfLt
import com.google.android.horologist.remotecompose.lottie.LottieSettings
import com.google.android.horologist.remotecompose.lottie.format.properties.AnimatedBezierProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.BaseBezierProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticBezierProperty
import com.google.android.horologist.remotecompose.lottie.format.values.BezierValue
import com.google.android.horologist.remotecompose.lottie.format.values.Point
import com.google.android.horologist.remotecompose.lottie.renderer.lookupValueInBezier
import com.google.android.horologist.remotecompose.lottie.renderer.scalarLinearEasingIn
import com.google.android.horologist.remotecompose.lottie.renderer.scalarLinearEasingOut

internal data class RemoteBezierValue(
  val closed: Boolean,
  val inTangents: List<Point>,
  val outTangents: List<Point>,
  val vertices: List<Point>,
)

internal data class BezierAnimationSegment(val startFrame: Float, val value: RemoteBezierValue)

internal fun BezierValue.toRemote(): RemoteBezierValue {
  return RemoteBezierValue(
    closed = closed.constantValue,
    inTangents = inTangents,
    outTangents = outTangents,
    vertices = vertices,
  )
}

internal fun RemoteBezierValue.toBezierValue(): BezierValue =
  BezierValue(
    closed = closed.rb,
    inTangents = inTangents,
    outTangents = outTangents,
    vertices = vertices,
  )

/**
 * Animates a bezier property returning the evaluated [BezierValue].
 *
 * Emits constant leaf [RemoteFloat] values when timeline is constant, and dynamic expressions when
 * keyframed across frames.
 */
@SuppressLint("RestrictedApi")
internal fun animateBezier(
  path: BaseBezierProperty,
  animationSettings: LottieSettings,
): BezierValue {
  return when (path) {
    is StaticBezierProperty -> path.value
    is AnimatedBezierProperty -> {
      if (path.keyframes.isEmpty()) {
        return BezierValue(
          closed = false.rb,
          inTangents = emptyList(),
          outTangents = emptyList(),
          vertices = emptyList(),
        )
      }
      if (path.keyframes.size == 1) {
        return path.keyframes[0].value.firstOrNull()
          ?: BezierValue(
            closed = false.rb,
            inTangents = emptyList(),
            outTangents = emptyList(),
            vertices = emptyList(),
          )
      }

      val firstKeyframe = path.keyframes[0]
      val firstSubpath =
        firstKeyframe.value.firstOrNull()
          ?: BezierValue(
            closed = false.rb,
            inTangents = emptyList(),
            outTangents = emptyList(),
            vertices = emptyList(),
          )

      val constFrame = animationSettings.currentFrame.constantValueOrNull
      if (constFrame != null) {
        val firstFrameVal = firstKeyframe.frame.constantValue
        if (constFrame <= firstFrameVal) {
          return firstSubpath
        }

        val lastKeyframe = path.keyframes.last()
        val lastFrameVal = lastKeyframe.frame.constantValue
        if (constFrame >= lastFrameVal) {
          return lastKeyframe.value.firstOrNull() ?: firstSubpath
        }

        for (i in 0 until path.keyframes.size - 1) {
          val startKeyframe = path.keyframes[i]
          val endKeyframe = path.keyframes[i + 1]
          val startFrame = startKeyframe.frame.constantValue
          val endFrame = endKeyframe.frame.constantValue

          if (constFrame >= startFrame && (constFrame < endFrame || i == path.keyframes.size - 2)) {
            val startSubpath = startKeyframe.value.firstOrNull() ?: firstSubpath
            val endSubpath = endKeyframe.value.firstOrNull() ?: startSubpath

            if (startKeyframe.hold.constantValue) {
              return startSubpath
            }

            val duration = endFrame - startFrame
            val frameInAnimation = constFrame - startFrame
            val fraction = (frameInAnimation / duration).coerceIn(0f, 1f)

            val outTangent = startKeyframe.outTangent ?: scalarLinearEasingOut
            val inTangent = startKeyframe.inTangent ?: scalarLinearEasingIn

            val easingProgress =
              lookupValueInBezier(
                  outTangent.x,
                  outTangent.y,
                  inTangent.x,
                  inTangent.y,
                  duration,
                  frameInAnimation.rf,
                )
                .constantValueOrNull ?: fraction

            return BezierValue(
              closed = startSubpath.closed,
              inTangents =
                interpolatePointsConst(
                  startSubpath.inTangents,
                  endSubpath.inTangents,
                  easingProgress,
                ),
              outTangents =
                interpolatePointsConst(
                  startSubpath.outTangents,
                  endSubpath.outTangents,
                  easingProgress,
                ),
              vertices =
                interpolatePointsConst(startSubpath.vertices, endSubpath.vertices, easingProgress),
            )
          }
        }
      }

      val subpaths = animateBezierSubpaths(path, animationSettings)
      subpaths.firstOrNull()?.toBezierValue() ?: firstSubpath
    }
  }
}

private fun interpolatePointsConst(
  from: List<Point>,
  to: List<Point>,
  progress: Float,
): List<Point> {
  return from.mapIndexed { index, point ->
    val target = to.getOrNull(index) ?: point
    val x0 = point.x.constantValueOrNull ?: 0f
    val y0 = point.y.constantValueOrNull ?: 0f
    val x1 = target.x.constantValueOrNull ?: x0
    val y1 = target.y.constantValueOrNull ?: y0
    Point(x = (x0 + (x1 - x0) * progress).rf, y = (y0 + (y1 - y0) * progress).rf)
  }
}

/**
 * Animates a bezier (shape) property.
 *
 * Takes a [BaseBezierProperty] (either static or animated) and resolves it to a list of
 * [RemoteBezierValue] subpaths. Supports keyframed transitions with cubic Bézier easing curves,
 * hold keyframes, and delayed starts.
 */
@SuppressLint("RestrictedApi")
internal fun animateBezierSubpaths(
  path: BaseBezierProperty,
  animationSettings: LottieSettings,
): List<RemoteBezierValue> {
  return when (path) {
    is StaticBezierProperty -> listOf(path.value.toRemote())
    is AnimatedBezierProperty -> {
      if (path.keyframes.isEmpty()) {
        return emptyList()
      }
      if (path.keyframes.size == 1) {
        return path.keyframes[0].value.map { it.toRemote() }
      }

      val firstKeyframe = path.keyframes[0]
      val subpathCount = firstKeyframe.value.size
      if (subpathCount == 0) {
        return emptyList()
      }

      (0 until subpathCount).map { subpathIndex ->
        val animationSegments = mutableListOf<BezierAnimationSegment>()
        val firstSubpath = firstKeyframe.value[subpathIndex]

        val firstFrame = firstKeyframe.frame.constantValue
        if (firstFrame != 0f) {
          animationSegments.add(BezierAnimationSegment(0f, firstSubpath.toRemote()))
        }

        for (i in 0 until path.keyframes.size - 1) {
          val startKeyframe = path.keyframes[i]
          val endKeyframe = path.keyframes[i + 1]
          val startFrameVal = startKeyframe.frame.constantValue
          val endFrameVal = endKeyframe.frame.constantValue
          val duration = endFrameVal - startFrameVal
          val frameInAnimation = animationSettings.currentFrame - startFrameVal.rf

          val startSubpath = startKeyframe.value.getOrElse(subpathIndex) { firstSubpath }
          val endSubpath = endKeyframe.value.getOrElse(subpathIndex) { startSubpath }

          val segmentValue =
            if (startKeyframe.hold.constantValue) {
              RemoteBezierValue(
                closed = startSubpath.closed.constantValue,
                inTangents =
                  startSubpath.inTangents.mapIndexed { v, point ->
                    val endPoint = endSubpath.inTangents.getOrNull(v) ?: point
                    Point(
                      x = selectIfLt(frameInAnimation, duration.rf, point.x, endPoint.x),
                      y = selectIfLt(frameInAnimation, duration.rf, point.y, endPoint.y),
                    )
                  },
                outTangents =
                  startSubpath.outTangents.mapIndexed { v, point ->
                    val endPoint = endSubpath.outTangents.getOrNull(v) ?: point
                    Point(
                      x = selectIfLt(frameInAnimation, duration.rf, point.x, endPoint.x),
                      y = selectIfLt(frameInAnimation, duration.rf, point.y, endPoint.y),
                    )
                  },
                vertices =
                  startSubpath.vertices.mapIndexed { v, point ->
                    val endPoint = endSubpath.vertices.getOrNull(v) ?: point
                    Point(
                      x = selectIfLt(frameInAnimation, duration.rf, point.x, endPoint.x),
                      y = selectIfLt(frameInAnimation, duration.rf, point.y, endPoint.y),
                    )
                  },
              )
            } else {
              val outTangent = startKeyframe.outTangent ?: scalarLinearEasingOut
              val inTangent = startKeyframe.inTangent ?: scalarLinearEasingIn

              val currentBezierValue =
                lookupValueInBezier(
                  outTangent.x,
                  outTangent.y,
                  inTangent.x,
                  inTangent.y,
                  duration,
                  frameInAnimation,
                )

              RemoteBezierValue(
                closed = startSubpath.closed.constantValue,
                inTangents =
                  startSubpath.inTangents.mapIndexed { v, point ->
                    val endPoint = endSubpath.inTangents.getOrNull(v) ?: point
                    Point(
                      x = lerp(point.x, endPoint.x, currentBezierValue),
                      y = lerp(point.y, endPoint.y, currentBezierValue),
                    )
                  },
                outTangents =
                  startSubpath.outTangents.mapIndexed { v, point ->
                    val endPoint = endSubpath.outTangents.getOrNull(v) ?: point
                    Point(
                      x = lerp(point.x, endPoint.x, currentBezierValue),
                      y = lerp(point.y, endPoint.y, currentBezierValue),
                    )
                  },
                vertices =
                  startSubpath.vertices.mapIndexed { v, point ->
                    val endPoint = endSubpath.vertices.getOrNull(v) ?: point
                    Point(
                      x = lerp(point.x, endPoint.x, currentBezierValue),
                      y = lerp(point.y, endPoint.y, currentBezierValue),
                    )
                  },
              )
            }

          animationSegments.add(BezierAnimationSegment(startFrameVal, segmentValue))
        }

        chainBezierAnimation(animationSegments, animationSettings.currentFrame)
      }
    }
  }
}

/**
 * Support keyframed bezier animations (and delayed start animations) by chaining multiple animation
 * segments together across timeline thresholds.
 */
@SuppressLint("RestrictedApi")
private fun chainBezierAnimation(
  segments: List<BezierAnimationSegment>,
  frame: RemoteFloat,
): RemoteBezierValue {
  if (segments.size == 1) {
    return segments[0].value
  }

  val firstSegment = segments[0]
  val remainingChained = chainBezierAnimation(segments.subList(1, segments.size), frame)
  val nextStartFrame = segments[1].startFrame.rf

  return RemoteBezierValue(
    closed = firstSegment.value.closed,
    inTangents =
      firstSegment.value.inTangents.mapIndexed { v, point ->
        val remainingVal = remainingChained.inTangents.getOrNull(v) ?: point
        Point(
          x = selectIfLt(frame, nextStartFrame, point.x, remainingVal.x),
          y = selectIfLt(frame, nextStartFrame, point.y, remainingVal.y),
        )
      },
    outTangents =
      firstSegment.value.outTangents.mapIndexed { v, point ->
        val remainingVal = remainingChained.outTangents.getOrNull(v) ?: point
        Point(
          x = selectIfLt(frame, nextStartFrame, point.x, remainingVal.x),
          y = selectIfLt(frame, nextStartFrame, point.y, remainingVal.y),
        )
      },
    vertices =
      firstSegment.value.vertices.mapIndexed { v, point ->
        val remainingVal = remainingChained.vertices.getOrNull(v) ?: point
        Point(
          x = selectIfLt(frame, nextStartFrame, point.x, remainingVal.x),
          y = selectIfLt(frame, nextStartFrame, point.y, remainingVal.y),
        )
      },
  )
}
