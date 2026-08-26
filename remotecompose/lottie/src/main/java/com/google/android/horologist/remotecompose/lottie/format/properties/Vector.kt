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

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/** Base class for all Lottie vector properties (e.g. scale, size). */
@Serializable(with = BaseVectorPropertySerializer::class)
internal sealed class BaseVectorProperty : LottieProperty<List<Float>>

/** A static vector property holding a list of [Float]s. */
@Serializable(with = StaticVectorPropertySerializer::class)
internal data class StaticVectorProperty(
  @SerialName("sid") override val slotId: String? = null,
  override val animated: Boolean = false,
  @SerialName("k") val value: List<Float>,
) : BaseVectorProperty()

/** An animated vector property with keyframes. */
@Serializable(with = AnimatedVectorPropertySerializer::class)
internal data class AnimatedVectorProperty(
  @SerialName("sid") override val slotId: String? = null,
  @SerialName("a") val animatedInt: Int = 1,
  @SerialName("k") val keyframes: List<VectorPropertyKeyframe> = emptyList(),
) : BaseVectorProperty() {
  override val animated: Boolean
    get() = animatedInt == 1
}

/** A single keyframe for an animated vector property. */
internal typealias VectorPropertyKeyframe = LottieKeyframe<List<Float>>

/** Polymorphic serializer for [BaseVectorProperty]. */
internal object BaseVectorPropertySerializer : KSerializer<BaseVectorProperty> {
  override val descriptor: SerialDescriptor =
    LottiePropertySerializer(VectorValueSerializer).descriptor

  override fun deserialize(decoder: Decoder): BaseVectorProperty {
    val jsonDecoder = decoder as JsonDecoder
    val element = jsonDecoder.decodeJsonElement()
    val animated = element is JsonObject && element["a"]?.jsonPrimitive?.intOrNull == 1
    return if (animated) {
      jsonDecoder.json.decodeFromJsonElement(AnimatedVectorPropertySerializer, element)
    } else {
      jsonDecoder.json.decodeFromJsonElement(StaticVectorPropertySerializer, element)
    }
  }

  override fun serialize(encoder: Encoder, value: BaseVectorProperty) {
    when (value) {
      is AnimatedVectorProperty ->
        encoder.encodeSerializableValue(AnimatedVectorPropertySerializer, value)
      is StaticVectorProperty ->
        encoder.encodeSerializableValue(StaticVectorPropertySerializer, value)
    }
  }
}

/** Helper to parse a vector (list of floats) from a [JsonElement]. */
internal fun parseVectorElement(element: JsonElement?): List<Float> {
  return when (element) {
    null -> emptyList()
    is JsonPrimitive -> element.floatOrNull?.let { listOf(it) } ?: emptyList()
    is JsonArray -> {
      if (element.isEmpty()) {
        emptyList()
      } else if (element.first() is JsonArray) {
        parseVectorElement(element.first())
      } else {
        element.mapNotNull { it.jsonPrimitive.floatOrNull }
      }
    }
    is JsonObject -> {
      element["k"]?.let { parseVectorElement(it) }
        ?: element["s"]?.let { parseVectorElement(it) }
        ?: emptyList()
    }
  }
}

/** Serializer for [StaticVectorProperty]. */
internal object StaticVectorPropertySerializer : KSerializer<StaticVectorProperty> {
  private val delegate = StaticPropertySerializer(VectorValueSerializer)
  override val descriptor: SerialDescriptor = delegate.descriptor

  override fun deserialize(decoder: Decoder): StaticVectorProperty {
    val prop = delegate.deserialize(decoder)
    return StaticVectorProperty(slotId = prop.slotId, animated = prop.animated, value = prop.value)
  }

  override fun serialize(encoder: Encoder, value: StaticVectorProperty) {
    delegate.serialize(encoder, StaticProperty(value.slotId, value.animated, value.value))
  }
}

/** Serializer for [AnimatedVectorProperty]. */
internal object AnimatedVectorPropertySerializer : KSerializer<AnimatedVectorProperty> {
  private val delegate = AnimatedPropertySerializer(VectorValueSerializer)
  override val descriptor: SerialDescriptor = delegate.descriptor

  override fun deserialize(decoder: Decoder): AnimatedVectorProperty {
    val prop = delegate.deserialize(decoder)
    return AnimatedVectorProperty(
      slotId = prop.slotId,
      animatedInt = prop.animatedInt,
      keyframes = prop.keyframes,
    )
  }

  override fun serialize(encoder: Encoder, value: AnimatedVectorProperty) {
    delegate.serialize(encoder, AnimatedProperty(value.slotId, value.animatedInt, value.keyframes))
  }
}

/** Serializer for [VectorPropertyKeyframe]. */
internal object VectorPropertyKeyframeSerializer :
  KSerializer<VectorPropertyKeyframe> by LottieKeyframeSerializer(VectorValueSerializer)
