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

package com.google.android.horologist.remotecompose.lottie.renderer.math

import com.google.android.horologist.remotecompose.lottie.format.values.BezierValue

/**
 * A cubic Bézier curve segment defined by start point [p0], start control point [p1], end control
 * point [p2], and end point [p3].
 */
internal data class CubicBezierSegment(
  val p0: Point2D,
  val p1: Point2D,
  val p2: Point2D,
  val p3: Point2D,
) {
  /** Evaluates the point on the cubic Bézier curve at parameter [t] in [0, 1]. */
  fun pointAt(t: Float): Point2D {
    val clampedT = t.coerceIn(0f, 1f)
    val u = 1f - clampedT
    val tt = clampedT * clampedT
    val uu = u * u
    val uuu = uu * u
    val ttt = tt * clampedT

    val x = uuu * p0.x + 3f * uu * clampedT * p1.x + 3f * u * tt * p2.x + ttt * p3.x
    val y = uuu * p0.y + 3f * uu * clampedT * p1.y + 3f * u * tt * p2.y + ttt * p3.y
    return Point2D(x, y)
  }

  /** Evaluates the first derivative (tangent vector) at parameter [t] in [0, 1]. */
  fun tangentAt(t: Float): Point2D {
    val clampedT = t.coerceIn(0f, 1f)
    val u = 1f - clampedT
    val dx =
      3f * u * u * (p1.x - p0.x) +
        6f * u * clampedT * (p2.x - p1.x) +
        3f * clampedT * clampedT * (p3.x - p2.x)
    val dy =
      3f * u * u * (p1.y - p0.y) +
        6f * u * clampedT * (p2.y - p1.y) +
        3f * clampedT * clampedT * (p3.y - p2.y)
    return Point2D(dx, dy)
  }

  /** Approximates the arc length of the cubic segment by sampling [samples] sub-intervals. */
  fun approximateLength(samples: Int = 16): Float {
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
  fun computeLengthTable(samples: Int = 16): FloatArray {
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
  fun split(t: Float): Pair<CubicBezierSegment, CubicBezierSegment> {
    val clampedT = t.coerceIn(0f, 1f)
    val p01 = Point2D.lerp(p0, p1, clampedT)
    val p12 = Point2D.lerp(p1, p2, clampedT)
    val p23 = Point2D.lerp(p2, p3, clampedT)

    val p012 = Point2D.lerp(p01, p12, clampedT)
    val p123 = Point2D.lerp(p12, p23, clampedT)

    val p0123 = Point2D.lerp(p012, p123, clampedT)

    val left = CubicBezierSegment(p0, p01, p012, p0123)
    val right = CubicBezierSegment(p0123, p123, p23, p3)
    return Pair(left, right)
  }

  /**
   * Trims this cubic Bézier segment between parameters [tStart] and [tEnd], where 0 <= tStart <=
   * tEnd <= 1.
   */
  fun subsegment(tStart: Float, tEnd: Float): CubicBezierSegment {
    val s = tStart.coerceIn(0f, 1f)
    val e = tEnd.coerceIn(0f, 1f)
    if (s <= 0f && e >= 1f) return this
    if (s >= e) {
      val pt = pointAt(s)
      return CubicBezierSegment(pt, pt, pt, pt)
    }

    val (_, rightOfStart) = split(s)
    val scaledEnd = ((e - s) / (1f - s)).coerceIn(0f, 1f)
    val (sub, _) = rightOfStart.split(scaledEnd)
    return sub
  }
}

/** Converts a [BezierValue] subpath into a list of [CubicBezierSegment]s. */
internal fun BezierValue.toCubicBezierSegments(): List<CubicBezierSegment> {
  val count = vertices.size
  if (count < 2) return emptyList()

  val maxIndex = if (closed) count else count - 1
  val segments = mutableListOf<CubicBezierSegment>()

  for (i in 0 until maxIndex) {
    val nextIndex = (i + 1) % count
    val p0 = Point2D(vertices[i].getOrElse(0) { 0f }, vertices[i].getOrElse(1) { 0f })
    val outTangent = outTangents.getOrNull(i)
    val p1 =
      Point2D(
        p0.x + (outTangent?.getOrElse(0) { 0f } ?: 0f),
        p0.y + (outTangent?.getOrElse(1) { 0f } ?: 0f),
      )

    val p3 =
      Point2D(vertices[nextIndex].getOrElse(0) { 0f }, vertices[nextIndex].getOrElse(1) { 0f })
    val inTangent = inTangents.getOrNull(nextIndex)
    val p2 =
      Point2D(
        p3.x + (inTangent?.getOrElse(0) { 0f } ?: 0f),
        p3.y + (inTangent?.getOrElse(1) { 0f } ?: 0f),
      )

    segments.add(CubicBezierSegment(p0, p1, p2, p3))
  }

  return segments
}

/** Converts a list of connected [CubicBezierSegment]s into a [BezierValue] open subpath. */
internal fun List<CubicBezierSegment>.toBezierValue(): BezierValue {
  if (isEmpty()) {
    return BezierValue(
      closed = false,
      inTangents = emptyList(),
      outTangents = emptyList(),
      vertices = emptyList(),
    )
  }

  val vertices = mutableListOf<List<Float>>()
  val inTangents = mutableListOf<List<Float>>()
  val outTangents = mutableListOf<List<Float>>()

  // First vertex
  vertices.add(listOf(this[0].p0.x, this[0].p0.y))
  inTangents.add(listOf(0f, 0f))
  outTangents.add(listOf(this[0].p1.x - this[0].p0.x, this[0].p1.y - this[0].p0.y))

  for (i in 0 until size - 1) {
    val curr = this[i]
    val next = this[i + 1]
    vertices.add(listOf(curr.p3.x, curr.p3.y))
    inTangents.add(listOf(curr.p2.x - curr.p3.x, curr.p2.y - curr.p3.y))
    outTangents.add(listOf(next.p1.x - next.p0.x, next.p1.y - next.p0.y))
  }

  // Last vertex
  val last = this.last()
  vertices.add(listOf(last.p3.x, last.p3.y))
  inTangents.add(listOf(last.p2.x - last.p3.x, last.p2.y - last.p3.y))
  outTangents.add(listOf(0f, 0f))

  return BezierValue(
    closed = false,
    inTangents = inTangents,
    outTangents = outTangents,
    vertices = vertices,
  )
}
