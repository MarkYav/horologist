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

package com.google.android.horologist.remotecompose.lottie.renderer.modifiers

import android.annotation.SuppressLint
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rf
import com.google.android.horologist.remotecompose.lottie.LottieSettings
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.TrimPath
import com.google.android.horologist.remotecompose.lottie.format.properties.AnimatedBezierProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.AnimatedScalarProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.BaseBezierProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.BaseScalarProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.BezierKeyframe
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticBezierProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticScalarProperty
import com.google.android.horologist.remotecompose.lottie.format.values.BezierValue
import com.google.android.horologist.remotecompose.lottie.format.values.KeyframeEasing
import com.google.android.horologist.remotecompose.lottie.format.values.Point
import com.google.android.horologist.remotecompose.lottie.renderer.properties.RemoteBezierValue
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animateBezierSubpaths
import com.google.android.horologist.remotecompose.lottie.renderer.properties.toRemote
import kotlin.math.absoluteValue
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot

/** Represents a 2D float point used for Bézier arc-length calculations and trimming. */
internal data class Vec2(val x: Float, val y: Float) {
  operator fun plus(other: Vec2): Vec2 = Vec2(x + other.x, y + other.y)

  operator fun minus(other: Vec2): Vec2 = Vec2(x - other.x, y - other.y)

  operator fun times(scalar: Float): Vec2 = Vec2(x * scalar, y * scalar)

  fun distanceTo(other: Vec2): Float = hypot(x - other.x, y - other.y)

  fun toPoint(): Point = Point(x.rf, y.rf)

  companion object {
    fun lerp(p0: Vec2, p1: Vec2, t: Float): Vec2 =
      Vec2(p0.x + (p1.x - p0.x) * t, p0.y + (p1.y - p0.y) * t)

    fun fromPoint(p: Point): Vec2 =
      Vec2(p.x.constantValueOrNull ?: 0f, p.y.constantValueOrNull ?: 0f)
  }
}

/**
 * A cubic Bézier curve segment defined by start point [p0], start control point [p1], end control
 * point [p2], and end point [p3].
 */
internal data class CubicSegment(val p0: Vec2, val p1: Vec2, val p2: Vec2, val p3: Vec2) {
  /** Evaluates the point on the cubic Bézier curve at parameter [t] in [0, 1]. */
  fun pointAt(t: Float): Vec2 {
    val clampedT = t.coerceIn(0f, 1f)
    val u = 1f - clampedT
    val tt = clampedT * clampedT
    val uu = u * u
    val uuu = uu * u
    val ttt = tt * clampedT

    val x = uuu * p0.x + 3f * uu * clampedT * p1.x + 3f * u * tt * p2.x + ttt * p3.x
    val y = uuu * p0.y + 3f * uu * clampedT * p1.y + 3f * u * tt * p2.y + ttt * p3.y
    return Vec2(x, y)
  }

  /** Approximates the arc length of the cubic segment by sampling [samples] sub-intervals. */
  fun approximateLength(samples: Int = 100): Float {
    var length = 0f
    var prev = p0
    for (i in 1..samples) {
      val t = i.toFloat() / samples
      val curr = pointAt(t)
      length += prev.distanceTo(curr)
      prev = curr
    }
    return length
  }

  /** Computes a cumulative length table of size [samples] + 1 for arc-length parameterization. */
  fun computeLengthTable(samples: Int = 100): FloatArray {
    val table = FloatArray(samples + 1)
    table[0] = 0f
    var prev = p0
    var accumulated = 0f
    for (i in 1..samples) {
      val t = i.toFloat() / samples
      val curr = pointAt(t)
      accumulated += prev.distanceTo(curr)
      table[i] = accumulated
      prev = curr
    }
    return table
  }

  /**
   * Finds the parametric value [t] corresponding to [targetDistance] using linear interpolation
   * between samples in [lengthTable].
   */
  fun tAtDistance(targetDistance: Float, lengthTable: FloatArray): Float {
    val totalLength = lengthTable.last()
    if (targetDistance <= 0f || totalLength <= 0f) return 0f
    if (targetDistance >= totalLength) return 1f

    val samples = lengthTable.size - 1
    for (i in 0 until samples) {
      val d0 = lengthTable[i]
      val d1 = lengthTable[i + 1]
      if (targetDistance in d0..d1) {
        val segmentDist = d1 - d0
        val alpha = if (segmentDist > 0.00001f) (targetDistance - d0) / segmentDist else 0f
        return (i + alpha) / samples
      }
    }
    return 1f
  }

  /**
   * Splits this cubic Bézier curve at parameter [t] into two curves using de Casteljau's algorithm.
   */
  fun split(t: Float): Pair<CubicSegment, CubicSegment> {
    val clampedT = t.coerceIn(0f, 1f)
    val p01 = Vec2.lerp(p0, p1, clampedT)
    val p12 = Vec2.lerp(p1, p2, clampedT)
    val p23 = Vec2.lerp(p2, p3, clampedT)

    val p012 = Vec2.lerp(p01, p12, clampedT)
    val p123 = Vec2.lerp(p12, p23, clampedT)

    val p0123 = Vec2.lerp(p012, p123, clampedT)

    val left = CubicSegment(p0, p01, p012, p0123)
    val right = CubicSegment(p0123, p123, p23, p3)
    return Pair(left, right)
  }

  /**
   * Trims this cubic Bézier segment between parameters [tStart] and [tEnd], where 0 <= tStart <=
   * tEnd <= 1.
   */
  fun subsegment(tStart: Float, tEnd: Float): CubicSegment {
    val s = tStart.coerceIn(0f, 1f)
    val e = tEnd.coerceIn(0f, 1f)
    if (s <= 0f && e >= 1f) return this
    if (s >= e) {
      val pt = pointAt(s)
      return CubicSegment(pt, pt, pt, pt)
    }

    val (_, rightOfStart) = split(s)
    val scaledEnd = ((e - s) / (1f - s)).coerceIn(0f, 1f)
    val (sub, _) = rightOfStart.split(scaledEnd)
    return sub
  }
}

/** Converts a [BezierValue] subpath into a list of [CubicSegment]s. */
internal fun BezierValue.toCubicSegments(): List<CubicSegment> {
  val count = vertices.size
  if (count < 2) return emptyList()

  val isClosed = closed.constantValue
  val maxIndex = if (isClosed) count else count - 1
  val segments = mutableListOf<CubicSegment>()

  for (i in 0 until maxIndex) {
    val nextIndex = (i + 1) % count
    val p0 = Vec2.fromPoint(vertices[i])
    val outTangent = outTangents.getOrNull(i)?.let { Vec2.fromPoint(it) } ?: Vec2(0f, 0f)
    val p1 = p0 + outTangent

    val p3 = Vec2.fromPoint(vertices[nextIndex])
    val inTangent = inTangents.getOrNull(nextIndex)?.let { Vec2.fromPoint(it) } ?: Vec2(0f, 0f)
    val p2 = p3 + inTangent

    segments.add(CubicSegment(p0, p1, p2, p3))
  }

  return segments
}

/** Converts a list of connected [CubicSegment]s into a [BezierValue] open subpath. */
internal fun List<CubicSegment>.toBezierValue(): BezierValue {
  if (isEmpty()) {
    return BezierValue(
      closed = false.rb,
      inTangents = emptyList(),
      outTangents = emptyList(),
      vertices = emptyList(),
    )
  }

  val vertices = mutableListOf<Point>()
  val inTangents = mutableListOf<Point>()
  val outTangents = mutableListOf<Point>()

  // First vertex
  vertices.add(this[0].p0.toPoint())
  inTangents.add(Point(0f.rf, 0f.rf))
  outTangents.add((this[0].p1 - this[0].p0).toPoint())

  for (i in 0 until size - 1) {
    val curr = this[i]
    val next = this[i + 1]
    vertices.add(curr.p3.toPoint())
    inTangents.add((curr.p2 - curr.p3).toPoint())
    outTangents.add((next.p1 - next.p0).toPoint())
  }

  // Last vertex
  val last = this.last()
  vertices.add(last.p3.toPoint())
  inTangents.add((last.p2 - last.p3).toPoint())
  outTangents.add(Point(0f.rf, 0f.rf))

  return BezierValue(
    closed = false.rb,
    inTangents = inTangents,
    outTangents = outTangents,
    vertices = vertices,
  )
}

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
  val segments = subpath.toCubicSegments()
  if (segments.isEmpty()) return emptyList()

  val diff = (endFraction - startFraction).absoluteValue
  if (diff >= 1f && offsetFraction == 0f) {
    return if (keepStructureIfDegenerate) listOf(segments.toBezierValue()) else listOf(subpath)
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

    val vertexCount = segments.size + 1
    val pt = degPoint.toPoint()
    val zeroPt = Point(0f.rf, 0f.rf)
    val vertices = List(vertexCount) { pt }
    val inTangents = List(vertexCount) { zeroPt }
    val outTangents = List(vertexCount) { zeroPt }
    return listOf(
      BezierValue(
        closed = false.rb,
        inTangents = inTangents,
        outTangents = outTangents,
        vertices = vertices,
      )
    )
  }

  if (span >= 1f) {
    return if (keepStructureIfDegenerate) listOf(segments.toBezierValue()) else listOf(subpath)
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
    val trimmedSegments = mutableListOf<CubicSegment>()

    var pTrimStart: Vec2? = null
    var pTrimEnd: Vec2? = null
    var acc = 0f
    for (idx in segments.indices) {
      val seg = segments[idx]
      val segLen = segmentLengths[idx]
      val segStart = acc
      val segEnd = acc + segLen
      acc = segEnd
      if (pTrimStart == null && dStart <= segEnd) {
        val distInSeg = (dStart - segStart).coerceIn(0f, segLen)
        val t = if (segLen > 0.0001f) seg.tAtDistance(distInSeg, lengthTables[idx]) else 0f
        pTrimStart = seg.pointAt(t)
      }
      if (pTrimEnd == null && dEnd <= segEnd) {
        val distInSeg = (dEnd - segStart).coerceIn(0f, segLen)
        val t = if (segLen > 0.0001f) seg.tAtDistance(distInSeg, lengthTables[idx]) else 1f
        pTrimEnd = seg.pointAt(t)
      }
    }
    val startPt = pTrimStart ?: segments.first().p0
    val endPt = pTrimEnd ?: segments.last().p3

    var accumulated = 0f
    for (idx in segments.indices) {
      val seg = segments[idx]
      val segLen = segmentLengths[idx]
      val segStart = accumulated
      val segEnd = accumulated + segLen
      accumulated = segEnd

      if (segEnd <= dStart) {
        if (keepStructureIfDegenerate) {
          trimmedSegments.add(CubicSegment(startPt, startPt, startPt, startPt))
        }
      } else if (segStart >= dEnd) {
        if (keepStructureIfDegenerate) {
          trimmedSegments.add(CubicSegment(endPt, endPt, endPt, endPt))
        }
      } else {
        val overlapStart = maxOf(dStart, segStart)
        val overlapEnd = minOf(dEnd, segEnd)
        if (overlapStart < overlapEnd && segLen > 0.0001f) {
          val distStartInSeg = (overlapStart - segStart).coerceIn(0f, segLen)
          val distEndInSeg = (overlapEnd - segStart).coerceIn(0f, segLen)

          val t0 = seg.tAtDistance(distStartInSeg, lengthTables[idx])
          val t1 = seg.tAtDistance(distEndInSeg, lengthTables[idx])

          trimmedSegments.add(seg.subsegment(t0, t1))
        } else if (keepStructureIfDegenerate) {
          trimmedSegments.add(CubicSegment(startPt, startPt, startPt, startPt))
        }
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
    is StaticScalarProperty -> scalar.value.constantValueOrNull ?: 0f
    is AnimatedScalarProperty -> {
      if (scalar.keyframes.isEmpty()) return 0f
      if (scalar.keyframes.size == 1) return scalar.keyframes[0].value.constantValueOrNull ?: 0f
      val first = scalar.keyframes[0]
      val firstFrame = first.frame.constantValueOrNull ?: 0f
      val firstValue = first.value.constantValueOrNull ?: 0f
      if (frame <= firstFrame) return firstValue
      val last = scalar.keyframes.last()
      val lastFrame = last.frame.constantValueOrNull ?: 0f
      val lastValue = last.value.constantValueOrNull ?: 0f
      if (frame >= lastFrame) return lastValue
      for (i in 0 until scalar.keyframes.size - 1) {
        val k0 = scalar.keyframes[i]
        val k1 = scalar.keyframes[i + 1]
        val k0Frame = k0.frame.constantValueOrNull ?: 0f
        val k1Frame = k1.frame.constantValueOrNull ?: 0f
        val k0Value = k0.value.constantValueOrNull ?: 0f
        val k1Value = k1.value.constantValueOrNull ?: 0f
        if (frame in k0Frame..k1Frame) {
          if (k0.hold.constantValue) return k0Value
          val duration = k1Frame - k0Frame
          if (duration <= 0.0001f) return k1Value
          val fraction = (frame - k0Frame) / duration
          val easing =
            CubicBezierEasing(
              k0.outTangent?.x?.constantValueOrNull ?: 0f,
              k0.outTangent?.y?.constantValueOrNull ?: 0f,
              k0.inTangent?.x?.constantValueOrNull ?: 1f,
              k0.inTangent?.y?.constantValueOrNull ?: 1f,
            )
          val progress = easing.transform(fraction)
          return k0Value + (k1Value - k0Value) * progress
        }
      }
      lastValue
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
      val firstFrame = first.frame.constantValueOrNull ?: 0f
      if (frame <= firstFrame) return first.value
      val last = bezier.keyframes.last()
      val lastFrame = last.frame.constantValueOrNull ?: 0f
      if (frame >= lastFrame) return last.value
      for (i in 0 until bezier.keyframes.size - 1) {
        val k0 = bezier.keyframes[i]
        val k1 = bezier.keyframes[i + 1]
        val k0Frame = k0.frame.constantValueOrNull ?: 0f
        val k1Frame = k1.frame.constantValueOrNull ?: 0f
        if (frame in k0Frame..k1Frame) {
          if (k0.hold.constantValue) return k0.value
          val duration = k1Frame - k0Frame
          if (duration <= 0.0001f) return k1.value
          val fraction = (frame - k0Frame) / duration
          val easing =
            CubicBezierEasing(
              k0.outTangent?.x?.constantValueOrNull ?: 0f,
              k0.outTangent?.y?.constantValueOrNull ?: 0f,
              k0.inTangent?.x?.constantValueOrNull ?: 1f,
              k0.inTangent?.y?.constantValueOrNull ?: 1f,
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
        val x0 = pt0.x.constantValueOrNull ?: 0f
        val y0 = pt0.y.constantValueOrNull ?: 0f
        val x1 = pt1.x.constantValueOrNull ?: 0f
        val y1 = pt1.y.constantValueOrNull ?: 0f
        Point((x0 + (x1 - x0) * t).rf, (y0 + (y1 - y0) * t).rf)
      },
    inTangents =
      b0.inTangents.mapIndexed { v, pt0 ->
        val pt1 = b1.inTangents.getOrNull(v) ?: pt0
        val x0 = pt0.x.constantValueOrNull ?: 0f
        val y0 = pt0.y.constantValueOrNull ?: 0f
        val x1 = pt1.x.constantValueOrNull ?: 0f
        val y1 = pt1.y.constantValueOrNull ?: 0f
        Point((x0 + (x1 - x0) * t).rf, (y0 + (y1 - y0) * t).rf)
      },
    outTangents =
      b0.outTangents.mapIndexed { v, pt0 ->
        val pt1 = b1.outTangents.getOrNull(v) ?: pt0
        val x0 = pt0.x.constantValueOrNull ?: 0f
        val y0 = pt0.y.constantValueOrNull ?: 0f
        val x1 = pt1.x.constantValueOrNull ?: 0f
        val y1 = pt1.y.constantValueOrNull ?: 0f
        Point((x0 + (x1 - x0) * t).rf, (y0 + (y1 - y0) * t).rf)
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
  if (trimPath == null || trimPath.hidden?.constantValue == true) {
    return animateBezierSubpaths(bezierProperty, animationSettings)
  }

  val isTrimAnimated =
    trimPath.start is AnimatedScalarProperty ||
      trimPath.end is AnimatedScalarProperty ||
      trimPath.offset is AnimatedScalarProperty

  if (!isTrimAnimated && bezierProperty is StaticBezierProperty) {
    val s = (trimPath.start as StaticScalarProperty).value.constantValue / 100f
    val e = (trimPath.end as StaticScalarProperty).value.constantValue / 100f
    val o = (trimPath.offset as StaticScalarProperty).value.constantValue / 360f
    val trimmed = trimBezierValue(bezierProperty.value, s, e, o, keepStructureIfDegenerate = false)
    return trimmed.map { it.toRemote() }
  }

  // Collect keyframe timestamps
  val keyframeTimes = mutableSetOf<Float>()
  (trimPath.start as? AnimatedScalarProperty)?.keyframes?.forEach {
    keyframeTimes.add(it.frame.constantValueOrNull ?: 0f)
  }
  (trimPath.end as? AnimatedScalarProperty)?.keyframes?.forEach {
    keyframeTimes.add(it.frame.constantValueOrNull ?: 0f)
  }
  (trimPath.offset as? AnimatedScalarProperty)?.keyframes?.forEach {
    keyframeTimes.add(it.frame.constantValueOrNull ?: 0f)
  }
  (bezierProperty as? AnimatedBezierProperty)?.keyframes?.forEach {
    val f = it.frame.constantValueOrNull ?: 0f
    keyframeTimes.add(f)
  }

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
  val keyframes = mutableListOf<BezierKeyframe>()

  val sampleFrames =
    if (isTrimAnimated) {
      val frames = mutableSetOf<Float>()
      if (sortedTimes.size <= 1) {
        frames.addAll(sortedTimes)
        frames.add(0f)
      } else {
        for (i in 0 until sortedTimes.size - 1) {
          val t0 = sortedTimes[i]
          val t1 = sortedTimes[i + 1]
          frames.add(t0)
          frames.add(t1)
          val startInt = ceil(t0).toInt()
          val endInt = floor(t1).toInt()
          for (frameInt in startInt..endInt) {
            frames.add(frameInt.toFloat())
          }
        }
      }
      frames.sorted()
    } else {
      sortedTimes
    }

  val linearEasingIn = KeyframeEasing(0f.rf, 0f.rf)
  val linearEasingOut = KeyframeEasing(1f.rf, 1f.rf)

  for (f in sampleFrames) {
    val s = sampleScalar(trimPath.start, f) / 100f
    val e = sampleScalar(trimPath.end, f) / 100f
    val o = sampleScalar(trimPath.offset, f) / 360f
    val baseSubpaths = sampleBezier(bezierProperty, f)
    val trimmedSubpaths = baseSubpaths.flatMap {
      trimBezierValue(it, s, e, o, keepStructureIfDegenerate = true)
    }

    if (isTrimAnimated) {
      keyframes.add(
        BezierKeyframe(
          frame = f.rf,
          value = trimmedSubpaths,
          inTangent = linearEasingIn,
          outTangent = linearEasingOut,
          hold = false.rb,
        )
      )
    } else {
      val primaryScalarKeyframe =
        (trimPath.start as? AnimatedScalarProperty)?.keyframes?.firstOrNull {
          (it.frame.constantValueOrNull ?: 0f) == f
        }
          ?: (trimPath.end as? AnimatedScalarProperty)?.keyframes?.firstOrNull {
            (it.frame.constantValueOrNull ?: 0f) == f
          }
          ?: (trimPath.offset as? AnimatedScalarProperty)?.keyframes?.firstOrNull {
            (it.frame.constantValueOrNull ?: 0f) == f
          }

      val bezierKf =
        (bezierProperty as? AnimatedBezierProperty)?.keyframes?.firstOrNull {
          (it.frame.constantValueOrNull ?: 0f) == f
        }

      val inTangent =
        primaryScalarKeyframe?.inTangent?.let { KeyframeEasing(it.x, it.y) } ?: bezierKf?.inTangent
      val outTangent =
        primaryScalarKeyframe?.outTangent?.let { KeyframeEasing(it.x, it.y) }
          ?: bezierKf?.outTangent
      val hold = primaryScalarKeyframe?.hold ?: bezierKf?.hold ?: false.rb

      keyframes.add(
        BezierKeyframe(
          frame = f.rf,
          value = trimmedSubpaths,
          inTangent = inTangent,
          outTangent = outTangent,
          hold = hold,
        )
      )
    }
  }

  val animatedTrimmedBezier = AnimatedBezierProperty(keyframes = keyframes)
  return animateBezierSubpaths(animatedTrimmedBezier, animationSettings)
}
