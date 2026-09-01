/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.horologist.remotecompose.lottie

import androidx.compose.remote.creation.compose.state.rf
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.horologist.remotecompose.lottie.format.LottieDecoder
import com.google.android.horologist.remotecompose.lottie.format.properties.AnimatedGradientProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.BaseGradientProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.GradientPropertyKeyframe
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticGradientProperty
import com.google.android.horologist.remotecompose.lottie.format.values.GradientValue
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animateGradient
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GradientTest {

  private val emptySlotMap = SlotMap.Empty

  @Test
  fun animateGradientWithStaticTransparentInput_returnsInput() {
    val staticGradient =
      StaticGradientProperty(
        value =
          GradientValue(
            numberOfColors = 2,
            values = listOf(0f, 1f, 0f, 0f, 1f, 0f, 1f, 0f, 0f, 1f, 1f, 0.5f),
          )
      )

    val result = animateGradient(staticGradient, LottieSettings(0.rf, emptySlotMap))
    assertThat(result.numberOfColors).isEqualTo(2)
    assertThat(result.hasTransparency).isTrue()
    assertThat(result.values.map { it.constantValueOrNull }).isEqualTo(staticGradient.value.values)
  }

  @Test
  fun animateGradientWithEmptyKeyframes_returnsEmpty() {
    val animatedGradient = AnimatedGradientProperty(numberOfColors = 2, keyframes = emptyList())

    val result = animateGradient(animatedGradient, LottieSettings(0.rf, emptySlotMap))
    assertThat(result.numberOfColors).isEqualTo(2)
    assertThat(result.values).isEmpty()
  }

  @Test
  fun animateGradientWithSingleKeyframe_returnsInput() {
    val gradientValue =
      GradientValue(numberOfColors = 2, values = listOf(0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f))
    val animatedGradient =
      AnimatedGradientProperty(
        numberOfColors = 2,
        keyframes = listOf(GradientPropertyKeyframe(frame = 0f, value = listOf(gradientValue))),
      )

    val result1 = animateGradient(animatedGradient, LottieSettings(0.rf, emptySlotMap))
    assertThat(result1.numberOfColors).isEqualTo(2)
    assertThat(result1.values.map { it.constantValueOrNull }).isEqualTo(gradientValue.values)

    val result2 = animateGradient(animatedGradient, LottieSettings(5.rf, emptySlotMap))
    assertThat(result2.values.map { it.constantValueOrNull }).isEqualTo(gradientValue.values)
  }

  @Test
  fun animateGradientWithTwoKeyframes_returnsAnimatedValues() {
    val keyframe1 =
      GradientPropertyKeyframe(
        frame = 0f,
        value =
          listOf(GradientValue(numberOfColors = 2, values = listOf(0f, 1f, 0f, 0f, 1f, 0f, 1f, 0f))),
      )
    val keyframe2 =
      GradientPropertyKeyframe(
        frame = 10f,
        value =
          listOf(GradientValue(numberOfColors = 2, values = listOf(0f, 0f, 1f, 0f, 1f, 0f, 0f, 1f))),
      )
    val animatedGradient =
      AnimatedGradientProperty(numberOfColors = 2, keyframes = listOf(keyframe1, keyframe2))

    val firstFrameResult = animateGradient(animatedGradient, LottieSettings(0.rf, emptySlotMap))
    val middleFrameResult = animateGradient(animatedGradient, LottieSettings(5.rf, emptySlotMap))
    val lastFrameResult = animateGradient(animatedGradient, LottieSettings(10.rf, emptySlotMap))
    val afterAnimationResult =
      animateGradient(animatedGradient, LottieSettings(15.rf, emptySlotMap))

    assertThat(firstFrameResult.values.map { it.constantValueOrNull })
      .isEqualTo(listOf(0f, 1f, 0f, 0f, 1f, 0f, 1f, 0f))
    assertThat(middleFrameResult.values.map { it.constantValueOrNull })
      .isEqualTo(listOf(0f, 0.5f, 0.5f, 0f, 1f, 0f, 0.5f, 0.5f))
    assertThat(lastFrameResult.values.map { it.constantValueOrNull })
      .isEqualTo(listOf(0f, 0f, 1f, 0f, 1f, 0f, 0f, 1f))
    assertThat(afterAnimationResult.values.map { it.constantValueOrNull })
      .isEqualTo(listOf(0f, 0f, 1f, 0f, 1f, 0f, 0f, 1f))
  }

  @Test
  fun animateGradientWithHoldKeyframe_holdsValue() {
    val keyframe1 =
      GradientPropertyKeyframe(
        frame = 0f,
        hold = true,
        value =
          listOf(GradientValue(numberOfColors = 2, values = listOf(0f, 1f, 0f, 0f, 1f, 0f, 1f, 0f))),
      )
    val keyframe2 =
      GradientPropertyKeyframe(
        frame = 10f,
        value =
          listOf(GradientValue(numberOfColors = 2, values = listOf(0f, 0f, 0f, 1f, 1f, 1f, 1f, 1f))),
      )
    val animatedGradient =
      AnimatedGradientProperty(numberOfColors = 2, keyframes = listOf(keyframe1, keyframe2))

    val firstFrameResult = animateGradient(animatedGradient, LottieSettings(0.rf, emptySlotMap))
    val middleFrameResult = animateGradient(animatedGradient, LottieSettings(5.rf, emptySlotMap))
    val lastFrameResult = animateGradient(animatedGradient, LottieSettings(10.rf, emptySlotMap))
    val afterAnimationResult =
      animateGradient(animatedGradient, LottieSettings(15.rf, emptySlotMap))

    assertThat(firstFrameResult.values.map { it.constantValueOrNull })
      .isEqualTo(listOf(0f, 1f, 0f, 0f, 1f, 0f, 1f, 0f))
    assertThat(middleFrameResult.values.map { it.constantValueOrNull })
      .isEqualTo(listOf(0f, 1f, 0f, 0f, 1f, 0f, 1f, 0f))
    assertThat(lastFrameResult.values.map { it.constantValueOrNull })
      .isEqualTo(listOf(0f, 0f, 0f, 1f, 1f, 1f, 1f, 1f))
    assertThat(afterAnimationResult.values.map { it.constantValueOrNull })
      .isEqualTo(listOf(0f, 0f, 0f, 1f, 1f, 1f, 1f, 1f))
  }

  @Test
  fun animateGradientWithDelayedStart_holdsInitialValue() {
    val keyframe1 =
      GradientPropertyKeyframe(
        frame = 5f,
        value =
          listOf(GradientValue(numberOfColors = 2, values = listOf(0f, 1f, 0f, 0f, 1f, 0f, 1f, 0f))),
      )
    val keyframe2 =
      GradientPropertyKeyframe(
        frame = 10f,
        value =
          listOf(GradientValue(numberOfColors = 2, values = listOf(0f, 0f, 1f, 0f, 1f, 0f, 0f, 1f))),
      )
    val animatedGradient =
      AnimatedGradientProperty(numberOfColors = 2, keyframes = listOf(keyframe1, keyframe2))

    val beforeStartResult = animateGradient(animatedGradient, LottieSettings(0.rf, emptySlotMap))
    val startFrameResult = animateGradient(animatedGradient, LottieSettings(5.rf, emptySlotMap))
    val endFrameResult = animateGradient(animatedGradient, LottieSettings(10.rf, emptySlotMap))

    assertThat(beforeStartResult.values.map { it.constantValueOrNull })
      .isEqualTo(listOf(0f, 1f, 0f, 0f, 1f, 0f, 1f, 0f))
    assertThat(startFrameResult.values.map { it.constantValueOrNull })
      .isEqualTo(listOf(0f, 1f, 0f, 0f, 1f, 0f, 1f, 0f))
    assertThat(endFrameResult.values.map { it.constantValueOrNull })
      .isEqualTo(listOf(0f, 0f, 1f, 0f, 1f, 0f, 0f, 1f))
  }

  @Test
  fun gradientProperties_deserializes() {
    val staticJson =
      """
      {
        "sid": "grad.theme",
        "a": 0,
        "k": {
          "p": 2,
          "k": [0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0]
        }
      }
      """
        .trimIndent()

    val staticProp =
      LottieDecoder.json.decodeFromString(BaseGradientProperty.serializer(), staticJson)
    assertThat(staticProp).isInstanceOf(StaticGradientProperty::class.java)
    assertThat(staticProp.slotId).isEqualTo("grad.theme")
    assertThat(staticProp.animated).isFalse()
    assertThat((staticProp as StaticGradientProperty).value.numberOfColors).isEqualTo(2)

    val animatedJson =
      """
      {
        "sid": "grad.anim",
        "a": 1,
        "p": 2,
        "k": [
          {
            "t": 0,
            "s": [0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0],
            "i": { "x": 0.5, "y": 1.0 },
            "o": { "x": 0.5, "y": 0.0 }
          },
          {
            "t": 60,
            "s": [0.0, 0.0, 1.0, 0.0, 1.0, 0.0, 0.0, 1.0]
          }
        ]
      }
      """
        .trimIndent()

    val animatedProp =
      LottieDecoder.json.decodeFromString(BaseGradientProperty.serializer(), animatedJson)
    assertThat(animatedProp).isInstanceOf(AnimatedGradientProperty::class.java)
    assertThat(animatedProp.slotId).isEqualTo("grad.anim")
    assertThat(animatedProp.animated).isTrue()
    val animGradient = animatedProp as AnimatedGradientProperty
    assertThat(animGradient.numberOfColors).isEqualTo(2)
    assertThat(animGradient.keyframes).hasSize(2)
    assertThat(animGradient.keyframes[0].frame).isEqualTo(0f)
    assertThat(animGradient.keyframes[1].frame).isEqualTo(60f)

    val settings = LottieSettings(0.rf, emptySlotMap)
    val remoteGrad = animateGradient(animGradient, settings)
    assertThat(remoteGrad.numberOfColors).isEqualTo(2)
    assertThat(remoteGrad.values).hasSize(8)
  }
}
