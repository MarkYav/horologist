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

package com.google.android.horologist.remotecompose.lottie.format

import com.google.android.horologist.remotecompose.lottie.format.properties.VectorPropertyKeyframe
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Base class for all animatable properties.
 *
 * This class is used to detect whether a property is animated or not for deserialization.
 */
@Serializable
internal sealed class AnimatableProperty {
  abstract val animated: Boolean
}

/** Base class for scalar (single Float) properties. */
@Serializable(with = BaseScalarPropertySerializer::class)
internal sealed class BaseScalarProperty : AnimatableProperty() {
  abstract override val animated: Boolean
}

/** A single float value that is not animated */
@Serializable(with = StaticScalarPropertySerializer::class)
internal data class StaticScalarProperty(
  @SerialName("sid") val slotId: String? = null,
  override val animated: Boolean = false,
  @SerialName("k") val value: Float = 0f,
) : BaseScalarProperty()

/** An animated scalar property with keyframes. */
@Serializable
internal data class AnimatedScalarProperty(
  @SerialName("sid") val slotId: String? = null,
  @SerialName("a") val animatedInt: Int = 1,
  @SerialName("k") val keyframes: List<ScalarPropertyKeyframe>,
) : BaseScalarProperty() {
  override val animated: Boolean
    get() = animatedInt == 1
}

/** A single keyframe for an animated scalar property. */
@Serializable(with = ScalarPropertyKeyframeSerializer::class)
internal data class ScalarPropertyKeyframe(
  @SerialName("t") val frame: Float = 0f,
  @SerialName("h") val hold: Boolean = false,
  @SerialName("i") val inTangent: ScalarKeyframeEasing? = null,
  @SerialName("o") val outTangent: ScalarKeyframeEasing? = null,
  @SerialName("s") val value: Float = 0f,
)

/** A position property is an array of floats (either 2D or 3D). */
@Serializable(with = BasePositionPropertySerializer::class)
internal sealed class BasePositionProperty : AnimatableProperty() {
  abstract override val animated: Boolean
}

/** A static position property is an array of floats with 2 or 3 values. */
@Serializable
internal data class StaticPositionProperty(
  @SerialName("sid") val slotId: String? = null,
  @SerialName("a") val animatedInt: Int = 0,
  @SerialName("k") val value: FloatArray,
) : BasePositionProperty() {
  override val animated: Boolean
    get() = animatedInt == 1

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as StaticPositionProperty
    if (slotId != other.slotId) return false
    if (!value.contentEquals(other.value)) return false
    return true
  }

  override fun hashCode(): Int {
    var result = slotId?.hashCode() ?: 0
    result = 31 * result + value.contentHashCode()
    return result
  }
}

/** An animated position property with keyframes. */
@Serializable
internal data class AnimatedPositionProperty(
  @SerialName("sid") val slotId: String? = null,
  @SerialName("a") val animatedInt: Int = 1,
  @SerialName("k") val keyframes: List<VectorPropertyKeyframe>,
) : BasePositionProperty() {
  override val animated: Boolean
    get() = animatedInt == 1
}

@Serializable(with = ScalarKeyframeEasingSerializer::class)
internal data class ScalarKeyframeEasing(val x: Float, val y: Float)
