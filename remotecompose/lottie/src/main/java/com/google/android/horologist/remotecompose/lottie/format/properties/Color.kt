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

package com.google.android.horologist.remotecompose.lottie.format.properties

import androidx.compose.remote.creation.compose.state.RemoteColor
import com.google.android.horologist.remotecompose.lottie.format.AnimatedColorPropertySerializer
import com.google.android.horologist.remotecompose.lottie.format.BaseColorPropertySerializer
import com.google.android.horologist.remotecompose.lottie.format.ColorPropertyKeyframeSerializer
import com.google.android.horologist.remotecompose.lottie.format.ScalarKeyframeEasing
import com.google.android.horologist.remotecompose.lottie.format.StaticColorPropertySerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Base class for all Lottie color properties.
 *
 * Unifies static constant colors ([StaticColorProperty]) and keyframed dynamic animations
 * ([AnimatedColorProperty]) under a shared contract for the AST and renderer pipeline.
 */
@Serializable(with = BaseColorPropertySerializer::class)
internal sealed class BaseColorProperty {
  abstract val animated: Boolean
  abstract val slotId: String?
}

/** A static color property holding a [RemoteColor] value. */
@Serializable(with = StaticColorPropertySerializer::class)
internal data class StaticColorProperty(
  @SerialName("sid") override val slotId: String? = null,
  override val animated: Boolean = false,
  @SerialName("k") val value: RemoteColor,
) : BaseColorProperty()

/** An animated color property with keyframes. */
@Serializable(with = AnimatedColorPropertySerializer::class)
internal data class AnimatedColorProperty(
  @SerialName("sid") override val slotId: String? = null,
  @SerialName("a") val animatedInt: Int = 1,
  @SerialName("k") val keyframes: List<ColorPropertyKeyframe>,
) : BaseColorProperty() {
  override val animated: Boolean
    get() = animatedInt == 1
}

/** A single keyframe for an animated color property. */
@Serializable(with = ColorPropertyKeyframeSerializer::class)
internal data class ColorPropertyKeyframe(
  @SerialName("t") val frame: Float = 0f,
  @SerialName("h") val hold: Boolean = false,
  @SerialName("i") val inTangent: ScalarKeyframeEasing? = null,
  @SerialName("o") val outTangent: ScalarKeyframeEasing? = null,
  @SerialName("s") val value: RemoteColor,
)
