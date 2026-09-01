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

import android.annotation.SuppressLint
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.lerp
import kotlin.math.hypot

/** A 2D point with Float coordinates. */
internal data class Point2D(val x: Float, val y: Float) {
  operator fun plus(other: Point2D): Point2D = Point2D(x + other.x, y + other.y)

  operator fun minus(other: Point2D): Point2D = Point2D(x - other.x, y - other.y)

  operator fun times(scalar: Float): Point2D = Point2D(x * scalar, y * scalar)

  operator fun unaryMinus(): Point2D = Point2D(-x, -y)

  fun distanceTo(other: Point2D): Float = hypot(x - other.x, y - other.y)

  fun dot(other: Point2D): Float = x * other.x + y * other.y

  fun length(): Float = hypot(x, y)

  companion object {
    val ZERO = Point2D(0f, 0f)

    fun lerp(p0: Point2D, p1: Point2D, t: Float): Point2D =
      Point2D(p0.x + (p1.x - p0.x) * t, p0.y + (p1.y - p0.y) * t)
  }
}

/** A 2D point with [RemoteFloat] coordinates for RemoteCompose state expressions. */
@SuppressLint("RestrictedApi")
internal data class RemotePoint2D(val x: RemoteFloat, val y: RemoteFloat) {
  operator fun plus(other: RemotePoint2D): RemotePoint2D = RemotePoint2D(x + other.x, y + other.y)

  operator fun minus(other: RemotePoint2D): RemotePoint2D = RemotePoint2D(x - other.x, y - other.y)

  operator fun times(scalar: RemoteFloat): RemotePoint2D = RemotePoint2D(x * scalar, y * scalar)

  operator fun unaryMinus(): RemotePoint2D = RemotePoint2D(-x, -y)

  companion object {
    fun lerp(p0: RemotePoint2D, p1: RemotePoint2D, progress: RemoteFloat): RemotePoint2D =
      RemotePoint2D(lerp(p0.x, p1.x, progress), lerp(p0.y, p1.y, progress))
  }
}
