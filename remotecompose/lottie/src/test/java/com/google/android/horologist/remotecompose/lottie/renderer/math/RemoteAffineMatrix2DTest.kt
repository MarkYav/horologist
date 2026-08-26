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
import androidx.compose.remote.creation.compose.state.rf
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SuppressLint("RestrictedApi")
@RunWith(AndroidJUnit4::class)
class RemoteAffineMatrix2DTest {

  @Test
  fun identity_mapsPointUnchanged() {
    val identity = RemoteAffineMatrix2D.IDENTITY
    val (x, y) = identity.mapPoint(10f.rf, 20f.rf)
    assertThat(x.constantValueOrNull).isWithin(0.001f).of(10f)
    assertThat(y.constantValueOrNull).isWithin(0.001f).of(20f)
  }

  @Test
  fun translation_mapsPointAndVector() {
    val matrix = RemoteAffineMatrix2D.translation(50f.rf, 100f.rf)

    val (px, py) = matrix.mapPoint(10f.rf, 20f.rf)
    assertThat(px.constantValueOrNull).isWithin(0.001f).of(60f)
    assertThat(py.constantValueOrNull).isWithin(0.001f).of(120f)

    val (vx, vy) = matrix.mapVector(10f.rf, 20f.rf)
    // Vectors/tangents must ignore translation
    assertThat(vx.constantValueOrNull).isWithin(0.001f).of(10f)
    assertThat(vy.constantValueOrNull).isWithin(0.001f).of(20f)
  }

  @Test
  fun scaling_mapsPointAndVector() {
    val matrix = RemoteAffineMatrix2D.scaling(2f.rf, 3f.rf)

    val (px, py) = matrix.mapPoint(10f.rf, 20f.rf)
    assertThat(px.constantValueOrNull).isWithin(0.001f).of(20f)
    assertThat(py.constantValueOrNull).isWithin(0.001f).of(60f)

    val (vx, vy) = matrix.mapVector(10f.rf, 20f.rf)
    assertThat(vx.constantValueOrNull).isWithin(0.001f).of(20f)
    assertThat(vy.constantValueOrNull).isWithin(0.001f).of(60f)
  }

  @Test
  fun rotation_mapsPointAndVector() {
    val matrix = RemoteAffineMatrix2D.rotation(90f.rf)

    // (100, 0) rotated 90 deg clockwise -> (0, 100)
    val (px, py) = matrix.mapPoint(100f.rf, 0f.rf)
    assertThat(px.constantValueOrNull).isWithin(0.01f).of(0f)
    assertThat(py.constantValueOrNull).isWithin(0.01f).of(100f)

    val (vx, vy) = matrix.mapVector(10f.rf, 0f.rf)
    assertThat(vx.constantValueOrNull).isWithin(0.01f).of(0f)
    assertThat(vy.constantValueOrNull).isWithin(0.01f).of(10f)
  }

  @Test
  fun skew_mapsPointAndVector() {
    val matrix = RemoteAffineMatrix2D.skew(45f.rf, null)

    // (0, 100) skewed by 45 deg -> (-100, 100)
    val (px, py) = matrix.mapPoint(0f.rf, 100f.rf)
    assertThat(px.constantValueOrNull).isWithin(0.01f).of(-100f)
    assertThat(py.constantValueOrNull).isWithin(0.01f).of(100f)
  }

  @Test
  fun multiply_composesTransformsCorrectly() {
    // Translate (10, 20), then Scale (2, 2)
    val t = RemoteAffineMatrix2D.translation(10f.rf, 20f.rf)
    val s = RemoteAffineMatrix2D.scaling(2f.rf, 2f.rf)

    // Composite M = T * S : scales first, then translates
    val composed = t * s
    val (px, py) = composed.mapPoint(5f.rf, 5f.rf)
    // 5 * 2 + 10 = 20, 5 * 2 + 20 = 30
    assertThat(px.constantValueOrNull).isWithin(0.001f).of(20f)
    assertThat(py.constantValueOrNull).isWithin(0.001f).of(30f)
  }

  @Test
  fun invert_invertsTransform() {
    val t = RemoteAffineMatrix2D.translation(50f.rf, 100f.rf)
    val r = RemoteAffineMatrix2D.rotation(45f.rf)
    val s = RemoteAffineMatrix2D.scaling(2f.rf, 3f.rf)

    val transform = t * r * s
    val inverse = transform.invert()

    val originalX = 15f.rf
    val originalY = 25f.rf
    val (tx, ty) = transform.mapPoint(originalX, originalY)
    val (backX, backY) = inverse.mapPoint(tx, ty)

    assertThat(backX.constantValueOrNull).isWithin(0.01f).of(15f)
    assertThat(backY.constantValueOrNull).isWithin(0.01f).of(25f)
  }

  @Test
  fun buildLottieTransformMatrix_matchesLottieOrder() {
    val matrix =
      RemoteAffineMatrix2D.buildLottieTransform(
        anchorX = 10f.rf,
        anchorY = 20f.rf,
        scaleX = 2f.rf,
        scaleY = 2f.rf,
        rotation = 90f.rf,
        skew = null,
        skewAxis = null,
        transX = 100f.rf,
        transY = 200f.rf,
      )

    // Point (10, 20) is the anchor point.
    // Shifting by anchor gives (0, 0).
    // Scale, rotation gives (0, 0).
    // Translation gives (100, 200).
    val (px, py) = matrix.mapPoint(10f.rf, 20f.rf)
    assertThat(px.constantValueOrNull).isWithin(0.01f).of(100f)
    assertThat(py.constantValueOrNull).isWithin(0.01f).of(200f)
  }
}
