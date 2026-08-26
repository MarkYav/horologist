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
import androidx.compose.animation.core.CubicBezierEasing
import com.google.android.horologist.remotecompose.lottie.LottieSettings
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.TrimPath
import com.google.android.horologist.remotecompose.lottie.format.properties.AnimatedBezierProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.AnimatedScalarProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.BaseBezierProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.BaseScalarProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.BezierPropertyKeyframe
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticBezierProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticScalarProperty
import com.google.android.horologist.remotecompose.lottie.format.values.BezierValue
import com.google.android.horologist.remotecompose.lottie.renderer.math.CubicBezierSegment
import com.google.android.horologist.remotecompose.lottie.renderer.math.toBezierValue
import com.google.android.horologist.remotecompose.lottie.renderer.math.toCubicBezierSegments
import com.google.android.horologist.remotecompose.lottie.renderer.properties.RemoteBezierValue
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animateBezier
import com.google.android.horologist.remotecompose.lottie.renderer.properties.toRemote
import kotlin.math.absoluteValue

/**
 * Dynamically trims a [BezierValue] subpath based on [startFraction], [endFraction], and
 * [offsetFraction].
 *
 * All fractions are normalized to [0, 1] interval (where 100% = 1.0, 360 deg = 1.0). Supports
 * wrapping intervals, closed/open subpaths, and degenerate structure preservation.
 */
internal fun trimBezierValue(
  subpath: BezierValue,
  startFraction: Float,
  endFraction: Float,
  offsetFraction: Float = 0f,
  keepStructureIfDegenerate: Boolean = true,
): List<BezierValue> {
  if (subpath.vertices.isEmpty()) return emptyList()
  val segments = subpath.toCubicBezierSegments()
  if (segments.isEmpty()) return emptyList()

  val diff = (endFraction - startFraction).absoluteValue
  if (diff >= 1f && offsetFraction == 0f) {
    return listOf(subpath)
  }

  val segmentLengths = segments.map { it.approximateLength() }
  val lengthTables = segments.map { it.computeLengthTable() }
  val totalLength = segmentLengths.sum()
  if (totalLength <= 0.0001f) {
    return emptyList()
  }

  val s = startFraction + offsetFraction
  val e = endFraction + offsetFraction

  val minVal = minOf(s, e)
  val maxVal = maxOf(s, e)
  val span = maxVal - minVal

  if (span <= 0.00001f) {
    if (!keepStructureIfDegenerate) return emptyList()
    val startNorm = ((minVal % 1f) + 1f) % 1f
    val normDist = startNorm * totalLength

    var accumulated = 0f
    var degPoint = segments.first().p0
    for (idx in segments.indices) {
      val segLen = segmentLengths[idx]
      if (normDist <= accumulated + segLen || idx == segments.lastIndex) {
        val distInSeg = (normDist - accumulated).coerceIn(0f, segLen)
        val t = segments[idx].tAtDistance(distInSeg, lengthTables[idx])
        degPoint = segments[idx].pointAt(t)
        break
      }
      accumulated += segLen
    }

    val vertexCount = subpath.vertices.size
    val vertices = List(vertexCount) { listOf(degPoint.x, degPoint.y) }
    val inTangents = List(vertexCount) { listOf(0f, 0f) }
    val outTangents = List(vertexCount) { listOf(0f, 0f) }
    return listOf(
      BezierValue(
        closed = false,
        inTangents = inTangents,
        outTangents = outTangents,
        vertices = vertices,
      )
    )
  }

  if (span >= 1f) {
    return listOf(subpath)
  }

  val startNorm = ((minVal % 1f) + 1f) % 1f
  val endNorm = startNorm + span

  val intervals = mutableListOf<Pair<Float, Float>>()
  if (endNorm <= 1f) {
    intervals.add(startNorm * totalLength to endNorm * totalLength)
  } else {
    intervals.add(startNorm * totalLength to totalLength)
    intervals.add(0f to (endNorm - 1f) * totalLength)
  }

  val result = mutableListOf<BezierValue>()
  for ((dStart, dEnd) in intervals) {
    if (dStart >= dEnd) continue
    val trimmedSegments = mutableListOf<CubicBezierSegment>()
    var accumulated = 0f

    for (idx in segments.indices) {
      val seg = segments[idx]
      val segLen = segmentLengths[idx]
      val segStart = accumulated
      val segEnd = accumulated + segLen
      accumulated = segEnd

      val overlapStart = maxOf(dStart, segStart)
      val overlapEnd = minOf(dEnd, segEnd)

      if (overlapStart < overlapEnd && segLen > 0.0001f) {
        val distStartInSeg = (overlapStart - segStart).coerceIn(0f, segLen)
        val distEndInSeg = (overlapEnd - segStart).coerceIn(0f, segLen)

        val t0 = seg.tAtDistance(distStartInSeg, lengthTables[idx])
        val t1 = seg.tAtDistance(distEndInSeg, lengthTables[idx])

        trimmedSegments.add(seg.subsegment(t0, t1))
      }
    }

    if (trimmedSegments.isNotEmpty()) {
      result.add(trimmedSegments.toBezierValue())
    }
  }

  return result
}

/** Samples a [BaseScalarProperty] at a given animation [frame]. */
internal fun sampleScalar(scalar: BaseScalarProperty, frame: Float): Float {
  return when (scalar) {
    is StaticScalarProperty -> scalar.value
    is AnimatedScalarProperty -> {
      if (scalar.keyframes.isEmpty()) return 0f
      if (scalar.keyframes.size == 1) return scalar.keyframes[0].value
      val first = scalar.keyframes[0]
      if (frame <= first.frame) return first.value
      val last = scalar.keyframes.last()
      if (frame >= last.frame) return last.value
      for (i in 0 until scalar.keyframes.size - 1) {
        val k0 = scalar.keyframes[i]
        val k1 = scalar.keyframes[i + 1]
        if (frame in k0.frame..k1.frame) {
          if (k0.hold) return k0.value
          val duration = k1.frame - k0.frame
          if (duration <= 0.0001f) return k1.value
          val fraction = (frame - k0.frame) / duration
          val easing =
            CubicBezierEasing(
              k0.outTangent?.x ?: 0f,
              k0.outTangent?.y ?: 0f,
              k0.inTangent?.x ?: 1f,
              k0.inTangent?.y ?: 1f,
            )
          val progress = easing.transform(fraction)
          return k0.value + (k1.value - k0.value) * progress
        }
      }
      last.value
    }
  }
}

/** Samples a [BaseBezierProperty] at a given animation [frame]. */
internal fun sampleBezier(bezier: BaseBezierProperty, frame: Float): List<BezierValue> {
  return when (bezier) {
    is StaticBezierProperty -> listOf(bezier.value)
    is AnimatedBezierProperty -> {
      if (bezier.keyframes.isEmpty()) return emptyList()
      if (bezier.keyframes.size == 1) return bezier.keyframes[0].value
      val first = bezier.keyframes[0]
      if (frame <= first.frame) return first.value
      val last = bezier.keyframes.last()
      if (frame >= last.frame) return last.value
      for (i in 0 until bezier.keyframes.size - 1) {
        val k0 = bezier.keyframes[i]
        val k1 = bezier.keyframes[i + 1]
        if (frame in k0.frame..k1.frame) {
          if (k0.hold) return k0.value
          val duration = k1.frame - k0.frame
          if (duration <= 0.0001f) return k1.value
          val fraction = (frame - k0.frame) / duration
          val easing =
            CubicBezierEasing(
              k0.outTangent?.x ?: 0f,
              k0.outTangent?.y ?: 0f,
              k0.inTangent?.x ?: 1f,
              k0.inTangent?.y ?: 1f,
            )
          val progress = easing.transform(fraction)
          return k0.value.mapIndexed { idx, sub0 ->
            val sub1 = k1.value.getOrNull(idx) ?: sub0
            lerpBezierValue(sub0, sub1, progress)
          }
        }
      }
      last.value
    }
  }
}

/** Linearly interpolates between two [BezierValue]s with matching vertex topology. */
internal fun lerpBezierValue(b0: BezierValue, b1: BezierValue, t: Float): BezierValue {
  return BezierValue(
    closed = b0.closed,
    vertices =
      b0.vertices.mapIndexed { v, pt0 ->
        val pt1 = b1.vertices.getOrNull(v) ?: pt0
        listOf(
          pt0.getOrElse(0) { 0f } + (pt1.getOrElse(0) { 0f } - pt0.getOrElse(0) { 0f }) * t,
          pt0.getOrElse(1) { 0f } + (pt1.getOrElse(1) { 0f } - pt0.getOrElse(1) { 0f }) * t,
        )
      },
    inTangents =
      b0.inTangents.mapIndexed { v, pt0 ->
        val pt1 = b1.inTangents.getOrNull(v) ?: pt0
        listOf(
          pt0.getOrElse(0) { 0f } + (pt1.getOrElse(0) { 0f } - pt0.getOrElse(0) { 0f }) * t,
          pt0.getOrElse(1) { 0f } + (pt1.getOrElse(1) { 0f } - pt0.getOrElse(1) { 0f }) * t,
        )
      },
    outTangents =
      b0.outTangents.mapIndexed { v, pt0 ->
        val pt1 = b1.outTangents.getOrNull(v) ?: pt0
        listOf(
          pt0.getOrElse(0) { 0f } + (pt1.getOrElse(0) { 0f } - pt0.getOrElse(0) { 0f }) * t,
          pt0.getOrElse(1) { 0f } + (pt1.getOrElse(1) { 0f } - pt0.getOrElse(1) { 0f }) * t,
        )
      },
  )
}

/**
 * Evaluates an animated or static [BaseBezierProperty] together with an optional [TrimPath]
 * modifier.
 */
@SuppressLint("RestrictedApi")
internal fun evaluateTrimmedBezier(
  bezierProperty: BaseBezierProperty,
  trimPath: TrimPath?,
  animationSettings: LottieSettings,
): List<RemoteBezierValue> {
  if (trimPath == null || trimPath.hidden == true) {
    return animateBezier(bezierProperty, animationSettings)
  }

  val isTrimAnimated =
    trimPath.start is AnimatedScalarProperty ||
      trimPath.end is AnimatedScalarProperty ||
      trimPath.offset is AnimatedScalarProperty

  if (!isTrimAnimated && bezierProperty is StaticBezierProperty) {
    val s = (trimPath.start as StaticScalarProperty).value / 100f
    val e = (trimPath.end as StaticScalarProperty).value / 100f
    val o = (trimPath.offset as StaticScalarProperty).value / 360f
    val trimmed = trimBezierValue(bezierProperty.value, s, e, o, keepStructureIfDegenerate = false)
    return trimmed.map { it.toRemote() }
  }

  // Collect keyframe timestamps
  val keyframeTimes = mutableSetOf<Float>()
  (trimPath.start as? AnimatedScalarProperty)?.keyframes?.forEach { keyframeTimes.add(it.frame) }
  (trimPath.end as? AnimatedScalarProperty)?.keyframes?.forEach { keyframeTimes.add(it.frame) }
  (trimPath.offset as? AnimatedScalarProperty)?.keyframes?.forEach { keyframeTimes.add(it.frame) }
  (bezierProperty as? AnimatedBezierProperty)?.keyframes?.forEach { keyframeTimes.add(it.frame) }

  if (keyframeTimes.isEmpty()) {
    // Both static or single keyframe
    val s = sampleScalar(trimPath.start, 0f) / 100f
    val e = sampleScalar(trimPath.end, 0f) / 100f
    val o = sampleScalar(trimPath.offset, 0f) / 360f
    val baseSubpaths = sampleBezier(bezierProperty, 0f)
    val trimmed = baseSubpaths.flatMap {
      trimBezierValue(it, s, e, o, keepStructureIfDegenerate = false)
    }
    return trimmed.map { it.toRemote() }
  }

  val sortedTimes = keyframeTimes.sorted()
  val keyframes = mutableListOf<BezierPropertyKeyframe>()

  for (i in sortedTimes.indices) {
    val f = sortedTimes[i]
    val s = sampleScalar(trimPath.start, f) / 100f
    val e = sampleScalar(trimPath.end, f) / 100f
    val o = sampleScalar(trimPath.offset, f) / 360f
    val baseSubpaths = sampleBezier(bezierProperty, f)
    val trimmedSubpaths = baseSubpaths.flatMap {
      trimBezierValue(it, s, e, o, keepStructureIfDegenerate = true)
    }

    // Find keyframe easing from the primary animated property
    val primaryScalarKeyframe =
      (trimPath.start as? AnimatedScalarProperty)?.keyframes?.firstOrNull { it.frame == f }
        ?: (trimPath.end as? AnimatedScalarProperty)?.keyframes?.firstOrNull { it.frame == f }
        ?: (trimPath.offset as? AnimatedScalarProperty)?.keyframes?.firstOrNull { it.frame == f }

    val bezierKf =
      (bezierProperty as? AnimatedBezierProperty)?.keyframes?.firstOrNull { it.frame == f }

    val inTangent = primaryScalarKeyframe?.inTangent ?: bezierKf?.inTangent
    val outTangent = primaryScalarKeyframe?.outTangent ?: bezierKf?.outTangent
    val hold = primaryScalarKeyframe?.hold ?: bezierKf?.hold ?: false

    keyframes.add(
      BezierPropertyKeyframe(
        frame = f,
        value = trimmedSubpaths,
        inTangent = inTangent,
        outTangent = outTangent,
        hold = hold,
      )
    )
  }

  val animatedTrimmedBezier = AnimatedBezierProperty(keyframes = keyframes)
  return animateBezier(animatedTrimmedBezier, animationSettings)
}
