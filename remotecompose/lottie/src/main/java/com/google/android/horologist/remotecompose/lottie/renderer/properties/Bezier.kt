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
import com.google.android.horologist.remotecompose.lottie.format.properties.AnimatedBezierProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.BaseBezierProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticBezierProperty
import com.google.android.horologist.remotecompose.lottie.format.values.BezierValue

@SuppressLint("RestrictedApi")
internal data class RemoteBezierValue(
  val closed: Boolean,
  val inTangents: List<List<RemoteFloat>>,
  val outTangents: List<List<RemoteFloat>>,
  val vertices: List<List<RemoteFloat>>,
)

@SuppressLint("RestrictedApi")
internal fun BezierValue.toRemote(): RemoteBezierValue {
  return RemoteBezierValue(
    closed = closed,
    inTangents = inTangents.map { point -> point.map { it.rf } },
    outTangents = outTangents.map { point -> point.map { it.rf } },
    vertices = vertices.map { point -> point.map { it.rf } },
  )
}

@SuppressLint("RestrictedApi")
internal object BezierSubpathInterpolator : KeyframeInterpolator<BezierValue, RemoteBezierValue> {
  override fun toResult(value: BezierValue): RemoteBezierValue = value.toRemote()

  override fun interpolate(
    start: BezierValue,
    end: BezierValue,
    progress: RemoteFloat,
  ): RemoteBezierValue {
    return RemoteBezierValue(
      closed = start.closed,
      inTangents =
        start.inTangents.mapIndexed { v, point ->
          point.mapIndexed { c, startCoord ->
            val endCoord = end.inTangents.getOrNull(v)?.getOrNull(c) ?: startCoord
            lerp(startCoord.rf, endCoord.rf, progress)
          }
        },
      outTangents =
        start.outTangents.mapIndexed { v, point ->
          point.mapIndexed { c, startCoord ->
            val endCoord = end.outTangents.getOrNull(v)?.getOrNull(c) ?: startCoord
            lerp(startCoord.rf, endCoord.rf, progress)
          }
        },
      vertices =
        start.vertices.mapIndexed { v, point ->
          point.mapIndexed { c, startCoord ->
            val endCoord = end.vertices.getOrNull(v)?.getOrNull(c) ?: startCoord
            lerp(startCoord.rf, endCoord.rf, progress)
          }
        },
    )
  }

  override fun hold(
    start: BezierValue,
    end: BezierValue,
    frameInAnimation: RemoteFloat,
    duration: Float,
  ): RemoteBezierValue {
    return RemoteBezierValue(
      closed = start.closed,
      inTangents =
        start.inTangents.mapIndexed { v, point ->
          point.mapIndexed { c, startCoord ->
            val endCoord = end.inTangents.getOrNull(v)?.getOrNull(c) ?: startCoord
            selectIfLt(frameInAnimation, duration.rf, startCoord.rf, endCoord.rf)
          }
        },
      outTangents =
        start.outTangents.mapIndexed { v, point ->
          point.mapIndexed { c, startCoord ->
            val endCoord = end.outTangents.getOrNull(v)?.getOrNull(c) ?: startCoord
            selectIfLt(frameInAnimation, duration.rf, startCoord.rf, endCoord.rf)
          }
        },
      vertices =
        start.vertices.mapIndexed { v, point ->
          point.mapIndexed { c, startCoord ->
            val endCoord = end.vertices.getOrNull(v)?.getOrNull(c) ?: startCoord
            selectIfLt(frameInAnimation, duration.rf, startCoord.rf, endCoord.rf)
          }
        },
    )
  }

  override fun select(
    frame: RemoteFloat,
    threshold: RemoteFloat,
    ifTrue: RemoteBezierValue,
    ifFalse: RemoteBezierValue,
  ): RemoteBezierValue {
    return RemoteBezierValue(
      closed = ifTrue.closed,
      inTangents =
        ifTrue.inTangents.mapIndexed { v, point ->
          point.mapIndexed { c, coordVal ->
            val remainingVal = ifFalse.inTangents.getOrNull(v)?.getOrNull(c) ?: coordVal
            selectIfLt(frame, threshold, coordVal, remainingVal)
          }
        },
      outTangents =
        ifTrue.outTangents.mapIndexed { v, point ->
          point.mapIndexed { c, coordVal ->
            val remainingVal = ifFalse.outTangents.getOrNull(v)?.getOrNull(c) ?: coordVal
            selectIfLt(frame, threshold, coordVal, remainingVal)
          }
        },
      vertices =
        ifTrue.vertices.mapIndexed { v, point ->
          point.mapIndexed { c, coordVal ->
            val remainingVal = ifFalse.vertices.getOrNull(v)?.getOrNull(c) ?: coordVal
            selectIfLt(frame, threshold, coordVal, remainingVal)
          }
        },
    )
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
internal fun animateBezier(
  path: BaseBezierProperty,
  animationSettings: LottieSettings,
): List<RemoteBezierValue> {
  return when (path) {
    is StaticBezierProperty -> listOf(path.value.toRemote())
    is AnimatedBezierProperty -> {
      if (path.keyframes.isEmpty()) return emptyList()
      val firstKeyframe = path.keyframes[0]
      val subpathCount = firstKeyframe.value.size
      if (subpathCount == 0) return emptyList()

      (0 until subpathCount).map { subpathIndex ->
        val firstSubpath = firstKeyframe.value[subpathIndex]
        evaluateKeyframes(
          keyframes = path.keyframes,
          animationSettings = animationSettings,
          getFrame = { it.frame },
          getValue = { it.value.getOrElse(subpathIndex) { firstSubpath } },
          getHold = { it.hold },
          getInTangent = { it.inTangent },
          getOutTangent = { it.outTangent },
          defaultValue = { firstSubpath.toRemote() },
          interpolator = BezierSubpathInterpolator,
        )
      }
    }
  }
}
