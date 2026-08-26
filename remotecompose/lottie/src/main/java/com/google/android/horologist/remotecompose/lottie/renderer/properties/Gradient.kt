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
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.rf
import com.google.android.horologist.remotecompose.lottie.LottieSettings
import com.google.android.horologist.remotecompose.lottie.format.properties.AnimatedGradientProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.BaseGradientProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticGradientProperty
import com.google.android.horologist.remotecompose.lottie.format.values.GradientValue

@SuppressLint("RestrictedApi")
internal data class RemoteGradientValue(val numberOfColors: Int, val values: List<RemoteFloat>) {
  val hasTransparency: Boolean
    get() =
      if (numberOfColors > 0) {
        values.size > numberOfColors * 4
      } else {
        values.size > 0 && values.size % 4 != 0
      }
}

@SuppressLint("RestrictedApi")
internal fun GradientValue.toRemote(): RemoteGradientValue {
  return RemoteGradientValue(numberOfColors = numberOfColors, values = values.map { it.rf })
}

@SuppressLint("RestrictedApi")
internal fun animateGradient(
  gradient: BaseGradientProperty,
  animationSettings: LottieSettings,
): RemoteGradientValue {
  return when (gradient) {
    is StaticGradientProperty -> gradient.value.toRemote()
    is AnimatedGradientProperty -> {
      if (gradient.keyframes.isEmpty()) {
        return RemoteGradientValue(
          numberOfColors = gradient.numberOfColors ?: 0,
          values = emptyList(),
        )
      }
      val firstVal =
        gradient.keyframes[0].value.firstOrNull()
          ?: GradientValue(numberOfColors = gradient.numberOfColors ?: 0)
      val numberOfColors = gradient.numberOfColors ?: firstVal.numberOfColors

      val chainedValues =
        evaluateKeyframes(
          keyframes = gradient.keyframes,
          animationSettings = animationSettings,
          getFrame = { it.frame },
          getValue = { (it.value.firstOrNull() ?: firstVal).values },
          getHold = { it.hold },
          getInTangent = { it.inTangent },
          getOutTangent = { it.outTangent },
          defaultValue = { emptyList() },
          interpolator = VectorInterpolator,
        )

      RemoteGradientValue(numberOfColors = numberOfColors, values = chainedValues)
    }
  }
}
