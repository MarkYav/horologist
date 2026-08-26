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
import androidx.compose.remote.creation.compose.state.cos
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.sin
import androidx.compose.remote.creation.compose.state.tan
import androidx.compose.remote.creation.compose.state.toRad

/**
 * Represents a 2D affine transformation matrix:
 * ```
 * [ a   c   tx ]
 * [ b   d   ty ]
 * [ 0   0   1  ]
 * ```
 *
 * Where points (x, y) transform as:
 * ```
 * x' = a * x + c * y + tx
 * y' = b * x + d * y + ty
 * ```
 *
 * And vectors/tangents (dx, dy) transform ignoring translation as:
 * ```
 * dx' = a * dx + c * dy
 * dy' = b * dx + d * dy
 * ```
 */
@SuppressLint("RestrictedApi")
internal data class RemoteAffineMatrix2D(
  val a: RemoteFloat,
  val b: RemoteFloat,
  val c: RemoteFloat,
  val d: RemoteFloat,
  val tx: RemoteFloat,
  val ty: RemoteFloat,
) {
  /**
   * Multiplies this matrix with [other] (this * other), representing applying [other] then [this].
   */
  operator fun times(other: RemoteAffineMatrix2D): RemoteAffineMatrix2D {
    return RemoteAffineMatrix2D(
      a = a * other.a + c * other.b,
      b = b * other.a + d * other.b,
      c = a * other.c + c * other.d,
      d = b * other.c + d * other.d,
      tx = a * other.tx + c * other.ty + tx,
      ty = b * other.tx + d * other.ty + ty,
    )
  }

  /** Maps a point (x, y) to its transformed coordinates. */
  fun mapPoint(x: RemoteFloat, y: RemoteFloat): Pair<RemoteFloat, RemoteFloat> {
    return Pair(a * x + c * y + tx, b * x + d * y + ty)
  }

  /** Maps a point [RemotePoint2D]. */
  fun mapPoint(point: RemotePoint2D): RemotePoint2D {
    val (nx, ny) = mapPoint(point.x, point.y)
    return RemotePoint2D(nx, ny)
  }

  /** Maps a vector / tangent (dx, dy) to its transformed direction without translation. */
  fun mapVector(dx: RemoteFloat, dy: RemoteFloat): Pair<RemoteFloat, RemoteFloat> {
    return Pair(a * dx + c * dy, b * dx + d * dy)
  }

  /** Inverts this affine matrix. */
  fun invert(): RemoteAffineMatrix2D {
    val det = a * d - b * c
    val invDet = 1f.rf / det
    val invA = d * invDet
    val invB = -b * invDet
    val invC = -c * invDet
    val invD = a * invDet
    val invTx = (c * ty - d * tx) * invDet
    val invTy = (b * tx - a * ty) * invDet
    return RemoteAffineMatrix2D(invA, invB, invC, invD, invTx, invTy)
  }

  companion object {
    val IDENTITY =
      RemoteAffineMatrix2D(a = 1f.rf, b = 0f.rf, c = 0f.rf, d = 1f.rf, tx = 0f.rf, ty = 0f.rf)

    fun translation(dx: RemoteFloat, dy: RemoteFloat): RemoteAffineMatrix2D =
      RemoteAffineMatrix2D(a = 1f.rf, b = 0f.rf, c = 0f.rf, d = 1f.rf, tx = dx, ty = dy)

    fun scaling(sx: RemoteFloat, sy: RemoteFloat): RemoteAffineMatrix2D =
      RemoteAffineMatrix2D(a = sx, b = 0f.rf, c = 0f.rf, d = sy, tx = 0f.rf, ty = 0f.rf)

    fun rotation(degrees: RemoteFloat): RemoteAffineMatrix2D {
      val rad = toRad(degrees)
      val cosR = cos(rad)
      val sinR = sin(rad)
      return RemoteAffineMatrix2D(a = cosR, b = sinR, c = -sinR, d = cosR, tx = 0f.rf, ty = 0f.rf)
    }

    fun skew(skewAngle: RemoteFloat, skewAxis: RemoteFloat?): RemoteAffineMatrix2D {
      val axis = skewAxis ?: 0f.rf
      val radAxis = toRad(90f.rf - axis)
      val cosA = cos(radAxis)
      val sinA = sin(radAxis)
      val tanSk = tan(toRad(skewAngle))

      val a = 1f.rf - sinA * cosA * tanSk
      val b = cosA * cosA * tanSk
      val c = -(sinA * sinA * tanSk)
      val d = 1f.rf + sinA * cosA * tanSk

      return RemoteAffineMatrix2D(a = a, b = b, c = c, d = d, tx = 0f.rf, ty = 0f.rf)
    }

    /**
     * Builds the complete composed Lottie transform matrix: Translate(trans) * Rotate(rotation) *
     * Skew(skew, skewAxis) * Scale(scale) * Translate(-anchor)
     */
    fun buildLottieTransform(
      anchorX: RemoteFloat,
      anchorY: RemoteFloat,
      scaleX: RemoteFloat,
      scaleY: RemoteFloat,
      rotation: RemoteFloat,
      skew: RemoteFloat?,
      skewAxis: RemoteFloat?,
      transX: RemoteFloat,
      transY: RemoteFloat,
    ): RemoteAffineMatrix2D {
      var m = translation(transX, transY) * rotation(rotation)
      if (skew != null) {
        m = m * skew(skew, skewAxis)
      }
      m = m * scaling(scaleX, scaleY)
      m = m * translation(-anchorX, -anchorY)
      return m
    }
  }
}
