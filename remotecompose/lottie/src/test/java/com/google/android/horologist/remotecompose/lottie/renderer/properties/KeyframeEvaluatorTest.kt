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
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.selectIfLt
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.horologist.remotecompose.lottie.LottieSettings
import com.google.android.horologist.remotecompose.lottie.SlotMap
import com.google.android.horologist.remotecompose.lottie.format.properties.ScalarPropertyKeyframe
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SuppressLint("RestrictedApi")
@RunWith(AndroidJUnit4::class)
class KeyframeEvaluatorTest {

  @Test
  fun chainSegmentsBinary_singleSegment_returnsConstant() {
    val segments = listOf(KeyframeSegment(0f, 42f.rf))
    val result =
      chainSegmentsBinary(segments, 10f.rf) { frame, threshold, ifTrue, ifFalse ->
        selectIfLt(frame, threshold, ifTrue, ifFalse)
      }
    assertThat(result.constantValueOrNull).isEqualTo(42f)
  }

  @Test
  fun chainSegmentsBinary_twoSegments_selectsCorrectly() {
    val segments = listOf(KeyframeSegment(0f, 10f.rf), KeyframeSegment(20f, 30f.rf))

    fun eval(frame: Float): Float? {
      return chainSegmentsBinary(segments, frame.rf) { f, threshold, ifTrue, ifFalse ->
          selectIfLt(f, threshold, ifTrue, ifFalse)
        }
        .constantValueOrNull
    }

    assertThat(eval(0f)).isEqualTo(10f)
    assertThat(eval(10f)).isEqualTo(10f)
    assertThat(eval(20f)).isEqualTo(30f)
    assertThat(eval(30f)).isEqualTo(30f)
  }

  @Test
  fun chainSegmentsBinary_multipleSegments_binaryTreeBranching() {
    val segments = (0..7).map { i -> KeyframeSegment((i * 10).toFloat(), (i * 100).toFloat().rf) }

    fun eval(frame: Float): Float? {
      return chainSegmentsBinary(segments, frame.rf) { f, threshold, ifTrue, ifFalse ->
          selectIfLt(f, threshold, ifTrue, ifFalse)
        }
        .constantValueOrNull
    }

    // Frame 0 -> Segment 0 (0f)
    assertThat(eval(0f)).isEqualTo(0f)
    assertThat(eval(5f)).isEqualTo(0f)
    // Frame 15 -> Segment 1 (100f)
    assertThat(eval(15f)).isEqualTo(100f)
    // Frame 25 -> Segment 2 (200f)
    assertThat(eval(25f)).isEqualTo(200f)
    // Frame 35 -> Segment 3 (300f)
    assertThat(eval(35f)).isEqualTo(300f)
    // Frame 45 -> Segment 4 (400f)
    assertThat(eval(45f)).isEqualTo(400f)
    // Frame 55 -> Segment 5 (500f)
    assertThat(eval(55f)).isEqualTo(500f)
    // Frame 65 -> Segment 6 (600f)
    assertThat(eval(65f)).isEqualTo(600f)
    // Frame 75 -> Segment 7 (700f)
    assertThat(eval(75f)).isEqualTo(700f)
  }

  @Test
  fun evaluateKeyframes_withScalarInterpolator_evaluatesTimeline() {
    val keyframes =
      listOf(
        ScalarPropertyKeyframe(frame = 0f, value = 0f),
        ScalarPropertyKeyframe(frame = 10f, value = 100f),
        ScalarPropertyKeyframe(frame = 20f, value = 200f),
      )

    fun eval(frame: Float): Float? {
      val settings = LottieSettings(frame.rf, SlotMap.Empty)
      return evaluateKeyframes(
          keyframes = keyframes,
          animationSettings = settings,
          getFrame = { it.frame },
          getValue = { it.value },
          getHold = { it.hold },
          getInTangent = { it.inTangent },
          getOutTangent = { it.outTangent },
          defaultValue = { 0f.rf },
          interpolator = ScalarInterpolator,
        )
        .constantValueOrNull
    }

    assertThat(eval(0f)).isWithin(0.01f).of(0f)
    assertThat(eval(5f)).isWithin(0.01f).of(50f)
    assertThat(eval(10f)).isWithin(0.01f).of(100f)
    assertThat(eval(15f)).isWithin(0.01f).of(150f)
    assertThat(eval(20f)).isWithin(0.01f).of(200f)
  }
}
