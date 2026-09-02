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

package com.google.android.horologist.remotecompose.lottie

import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.ui.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.horologist.remotecompose.lottie.format.properties.AnimatedColorProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.AnimatedVectorProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.ColorPropertyKeyframe
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticColorProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticVectorProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.VectorPropertyKeyframe
import com.google.android.horologist.remotecompose.lottie.renderer.layers.parseHexColor
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animateColor
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animateVector
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AnimationTest {
  private val emptySlotMap = SlotMap.Empty

  @Test
  fun animateVectorWithStaticInput_returnsInput() {
    val staticVector = StaticVectorProperty(value = listOf(1f, 2f, 3f))

    val result1 = animateVector(staticVector, LottieSettings(0.rf, emptySlotMap))
    assertThat(result1.map { it.constantValue }).isEqualTo(staticVector.value)

    val result2 = animateVector(staticVector, LottieSettings(5.rf, emptySlotMap))
    assertThat(result2.map { it.constantValue }).isEqualTo(staticVector.value)
  }

  @Test
  fun animateVectorWithEmptyKeyframes_returnsEmptyList() {
    val animatedVector = AnimatedVectorProperty(keyframes = emptyList())

    val result = animateVector(animatedVector, LottieSettings(0.rf, emptySlotMap))
    assertThat(result).isEmpty()
  }

  @Test
  fun animateVectorWithSingleKeyframe_returnsInput() {
    val animatedVector =
      AnimatedVectorProperty(
        keyframes = listOf(VectorPropertyKeyframe(frame = 0f, value = listOf(1f, 2f, 3f)))
      )

    val result1 = animateVector(animatedVector, LottieSettings(0.rf, emptySlotMap))
    assertThat(result1.map { it.constantValue }).isEqualTo(animatedVector.keyframes[0].value)

    val result2 = animateVector(animatedVector, LottieSettings(5.rf, emptySlotMap))
    assertThat(result2.map { it.constantValue }).isEqualTo(animatedVector.keyframes[0].value)
  }

  @Test
  fun animateVectorWithTwoKeyframes_returnsAnimatedValues() {
    val animatedVector =
      AnimatedVectorProperty(
        keyframes =
          listOf(
            VectorPropertyKeyframe(frame = 0f, value = listOf(1f, 2f, 3f)),
            VectorPropertyKeyframe(frame = 10f, value = listOf(4f, 5f, 6f)),
          )
      )

    val firstFrameResult = animateVector(animatedVector, LottieSettings(0.rf, emptySlotMap))
    val middleFrameResult = animateVector(animatedVector, LottieSettings(5.rf, emptySlotMap))
    val lastFrameResult = animateVector(animatedVector, LottieSettings(10.rf, emptySlotMap))
    val afterAnimationResult = animateVector(animatedVector, LottieSettings(15.rf, emptySlotMap))

    assertThat(firstFrameResult.map { it.constantValue })
      .isEqualTo(animatedVector.keyframes[0].value)
    assertThat(middleFrameResult.map { it.constantValue }).isEqualTo(listOf(2.5f, 3.5f, 4.5f))
    assertThat(lastFrameResult.map { it.constantValue })
      .isEqualTo(animatedVector.keyframes[1].value)
    assertThat(afterAnimationResult.map { it.constantValue })
      .isEqualTo(animatedVector.keyframes[1].value)
  }

  @Test
  fun animateColorWithStaticInput_returnsInput() {
    val staticColor = StaticColorProperty(value = Color.Red.rc)

    val result1 = animateColor(staticColor, LottieSettings(0.rf, emptySlotMap))
    assertThat(result1.constantValueOrNull).isEqualTo(Color.Red)

    val result2 = animateColor(staticColor, LottieSettings(5.rf, emptySlotMap))
    assertThat(result2.constantValueOrNull).isEqualTo(Color.Red)
  }

  @Test
  fun animateColorWithSlot_returnsSlotColor() {
    val slotMap = SlotMap(mapOf("color_slot" to Color.Blue.rc))
    val staticColor = StaticColorProperty(slotId = "color_slot", value = Color.Red.rc)

    val result = animateColor(staticColor, LottieSettings(0.rf, slotMap))
    assertThat(result.constantValueOrNull).isEqualTo(Color.Blue)

    val animatedColor =
      AnimatedColorProperty(
        slotId = "color_slot",
        keyframes = listOf(ColorPropertyKeyframe(frame = 0f, value = Color.Red.rc)),
      )
    val animResult = animateColor(animatedColor, LottieSettings(0.rf, slotMap))
    assertThat(animResult.constantValueOrNull).isEqualTo(Color.Blue)
  }

  @Test
  fun animateColorWithEmptyKeyframes_returnsTransparent() {
    val animatedColor = AnimatedColorProperty(keyframes = emptyList())

    val result = animateColor(animatedColor, LottieSettings(0.rf, emptySlotMap))
    assertThat(result.constantValueOrNull).isEqualTo(Color.Transparent)
  }

  @Test
  fun animateColorWithSingleKeyframe_returnsInput() {
    val animatedColor =
      AnimatedColorProperty(
        keyframes = listOf(ColorPropertyKeyframe(frame = 0f, value = Color.Green.rc))
      )

    val result1 = animateColor(animatedColor, LottieSettings(0.rf, emptySlotMap))
    assertThat(result1.constantValueOrNull).isEqualTo(Color.Green)

    val result2 = animateColor(animatedColor, LottieSettings(5.rf, emptySlotMap))
    assertThat(result2.constantValueOrNull).isEqualTo(Color.Green)
  }

  @Test
  fun animateColorWithTwoKeyframes_returnsAnimatedValues() {
    val animatedColor =
      AnimatedColorProperty(
        keyframes =
          listOf(
            ColorPropertyKeyframe(frame = 0f, value = Color(1f, 0f, 0f, 1f).rc),
            ColorPropertyKeyframe(frame = 10f, value = Color(0f, 1f, 0f, 1f).rc),
          )
      )

    val firstFrameResult = animateColor(animatedColor, LottieSettings(0.rf, emptySlotMap))
    val lastFrameResult = animateColor(animatedColor, LottieSettings(10.rf, emptySlotMap))
    val afterAnimationResult = animateColor(animatedColor, LottieSettings(15.rf, emptySlotMap))

    assertThat(firstFrameResult.constantValueOrNull).isEqualTo(Color(1f, 0f, 0f, 1f))
    assertThat(lastFrameResult.constantValueOrNull).isEqualTo(Color(0f, 1f, 0f, 1f))
    assertThat(afterAnimationResult.constantValueOrNull).isEqualTo(Color(0f, 1f, 0f, 1f))
  }

  @Test
  fun animateColorWithHoldKeyframe_holdsValue() {
    val animatedColor =
      AnimatedColorProperty(
        keyframes =
          listOf(
            ColorPropertyKeyframe(frame = 0f, hold = true, value = Color.Red.rc),
            ColorPropertyKeyframe(frame = 10f, value = Color.Blue.rc),
          )
      )

    val firstFrameResult = animateColor(animatedColor, LottieSettings(0.rf, emptySlotMap))
    val middleFrameResult = animateColor(animatedColor, LottieSettings(5.rf, emptySlotMap))
    val lastFrameResult = animateColor(animatedColor, LottieSettings(10.rf, emptySlotMap))

    assertThat(firstFrameResult.constantValueOrNull).isEqualTo(Color.Red)
    assertThat(middleFrameResult.constantValueOrNull).isEqualTo(Color.Red)
    assertThat(lastFrameResult.constantValueOrNull).isEqualTo(Color.Blue)
  }

  @Test
  fun animateColorWithDelayedStart_holdsInitialValue() {
    val animatedColor =
      AnimatedColorProperty(
        keyframes =
          listOf(
            ColorPropertyKeyframe(frame = 5f, value = Color.Yellow.rc),
            ColorPropertyKeyframe(frame = 10f, value = Color.Cyan.rc),
          )
      )

    val beforeStartResult = animateColor(animatedColor, LottieSettings(0.rf, emptySlotMap))
    assertThat(beforeStartResult.constantValueOrNull).isEqualTo(Color.Yellow)
  }

  @Test
  fun parseHexColor_parsesSixDigitHexWithAndWithoutHash() {
    assertThat(parseHexColor("#ff0000")).isEqualTo(Color(0xFFFF0000))
    assertThat(parseHexColor("00ff00")).isEqualTo(Color(0xFF00FF00))
    assertThat(parseHexColor("#0000ff")).isEqualTo(Color(0xFF0000FF))
  }

  @Test
  fun parseHexColor_parsesEightDigitHexWithAndWithoutHash() {
    assertThat(parseHexColor("#80ff0000")).isEqualTo(Color(0x80FF0000))
    assertThat(parseHexColor("4000ff00")).isEqualTo(Color(0x4000FF00))
  }

  @Test
  fun parseHexColor_parsesThreeDigitHexWithAndWithoutHash() {
    assertThat(parseHexColor("#f00")).isEqualTo(Color(0xFFFF0000))
    assertThat(parseHexColor("0f0")).isEqualTo(Color(0xFF00FF00))
  }

  @Test
  fun parseHexColor_invalidColorReturnsTransparent() {
    assertThat(parseHexColor("")).isEqualTo(Color.Transparent)
    assertThat(parseHexColor("invalid_hex")).isEqualTo(Color.Transparent)
    assertThat(parseHexColor("#zzzzzz")).isEqualTo(Color.Transparent)
  }
}
