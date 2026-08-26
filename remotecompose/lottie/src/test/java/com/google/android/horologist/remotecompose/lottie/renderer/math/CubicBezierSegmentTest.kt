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

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CubicBezierSegmentTest {

  @Test
  fun pointAt_straightLine_evaluatesInterpolation() {
    val segment =
      CubicBezierSegment(
        p0 = Point2D(0f, 0f),
        p1 = Point2D(10f, 0f),
        p2 = Point2D(20f, 0f),
        p3 = Point2D(30f, 0f),
      )

    val start = segment.pointAt(0f)
    assertThat(start.x).isEqualTo(0f)
    assertThat(start.y).isEqualTo(0f)

    val mid = segment.pointAt(0.5f)
    assertThat(mid.x).isWithin(0.001f).of(15f)
    assertThat(mid.y).isEqualTo(0f)

    val end = segment.pointAt(1f)
    assertThat(end.x).isEqualTo(30f)
    assertThat(end.y).isEqualTo(0f)
  }

  @Test
  fun approximateLength_straightLine_computesExactDistance() {
    val segment =
      CubicBezierSegment(
        p0 = Point2D(0f, 0f),
        p1 = Point2D(25f, 0f),
        p2 = Point2D(75f, 0f),
        p3 = Point2D(100f, 0f),
      )

    val length = segment.approximateLength()
    assertThat(length).isWithin(0.01f).of(100f)
  }

  @Test
  fun split_deCasteljauSubdivision_matchesPointAt() {
    val segment =
      CubicBezierSegment(
        p0 = Point2D(0f, 0f),
        p1 = Point2D(0f, 100f),
        p2 = Point2D(100f, 100f),
        p3 = Point2D(100f, 0f),
      )

    val (left, right) = segment.split(0.5f)
    val midPoint = segment.pointAt(0.5f)

    assertThat(left.p0.x).isEqualTo(segment.p0.x)
    assertThat(left.p0.y).isEqualTo(segment.p0.y)
    assertThat(left.p3.x).isWithin(0.001f).of(midPoint.x)
    assertThat(left.p3.y).isWithin(0.001f).of(midPoint.y)

    assertThat(right.p0.x).isWithin(0.001f).of(midPoint.x)
    assertThat(right.p0.y).isWithin(0.001f).of(midPoint.y)
    assertThat(right.p3.x).isEqualTo(segment.p3.x)
    assertThat(right.p3.y).isEqualTo(segment.p3.y)
  }

  @Test
  fun subsegment_trimsMiddlePortion() {
    val segment =
      CubicBezierSegment(
        p0 = Point2D(0f, 0f),
        p1 = Point2D(0f, 100f),
        p2 = Point2D(100f, 100f),
        p3 = Point2D(100f, 0f),
      )

    val sub = segment.subsegment(0.25f, 0.75f)
    val expectedStart = segment.pointAt(0.25f)
    val expectedEnd = segment.pointAt(0.75f)

    assertThat(sub.p0.x).isWithin(0.01f).of(expectedStart.x)
    assertThat(sub.p0.y).isWithin(0.01f).of(expectedStart.y)
    assertThat(sub.p3.x).isWithin(0.01f).of(expectedEnd.x)
    assertThat(sub.p3.y).isWithin(0.01f).of(expectedEnd.y)
  }

  @Test
  fun tAtDistance_computesParametricTFromArcLength() {
    val segment =
      CubicBezierSegment(
        p0 = Point2D(0f, 0f),
        p1 = Point2D(33.33f, 0f),
        p2 = Point2D(66.66f, 0f),
        p3 = Point2D(100f, 0f),
      )

    val table = segment.computeLengthTable()
    val tHalf = segment.tAtDistance(50f, table)
    assertThat(tHalf).isWithin(0.01f).of(0.5f)
  }
}
