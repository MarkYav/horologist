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
class Point2DTest {

  @Test
  fun point2D_vectorOperations() {
    val p1 = Point2D(10f, 20f)
    val p2 = Point2D(3f, 4f)

    val sum = p1 + p2
    assertThat(sum.x).isEqualTo(13f)
    assertThat(sum.y).isEqualTo(24f)

    val diff = p1 - p2
    assertThat(diff.x).isEqualTo(7f)
    assertThat(diff.y).isEqualTo(16f)

    val scaled = p1 * 2f
    assertThat(scaled.x).isEqualTo(20f)
    assertThat(scaled.y).isEqualTo(40f)

    val dist = p1.distanceTo(Point2D(13f, 24f))
    assertThat(dist).isWithin(0.001f).of(5f)

    val lerpMid = Point2D.lerp(p1, p2, 0.5f)
    assertThat(lerpMid.x).isEqualTo(6.5f)
    assertThat(lerpMid.y).isEqualTo(12f)
  }

  @Test
  fun remotePoint2D_vectorOperations() {
    val rp1 = RemotePoint2D(10f.rf, 20f.rf)
    val rp2 = RemotePoint2D(3f.rf, 4f.rf)

    val sum = rp1 + rp2
    assertThat(sum.x.constantValueOrNull).isEqualTo(13f)
    assertThat(sum.y.constantValueOrNull).isEqualTo(24f)

    val diff = rp1 - rp2
    assertThat(diff.x.constantValueOrNull).isEqualTo(7f)
    assertThat(diff.y.constantValueOrNull).isEqualTo(16f)

    val scaled = rp1 * 2f.rf
    assertThat(scaled.x.constantValueOrNull).isEqualTo(20f)
    assertThat(scaled.y.constantValueOrNull).isEqualTo(40f)

    val lerpMid = RemotePoint2D.lerp(rp1, rp2, 0.5f.rf)
    assertThat(lerpMid.x.constantValueOrNull).isWithin(0.01f).of(6.5f)
    assertThat(lerpMid.y.constantValueOrNull).isWithin(0.01f).of(12f)
  }
}
