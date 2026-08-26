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
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** Base class for all Lottie scalar (single float) properties. */
@Serializable(with = BaseScalarPropertySerializer::class)
internal sealed class BaseScalarProperty : LottieProperty<Float>

/** A single float value that is not animated. */
@Serializable(with = StaticScalarPropertySerializer::class)
internal data class StaticScalarProperty(
  @SerialName("sid") override val slotId: String? = null,
  override val animated: Boolean = false,
  @SerialName("k") val value: Float = 0f,
) : BaseScalarProperty()

/** An animated scalar property with keyframes. */
@Serializable(with = AnimatedScalarPropertySerializer::class)
internal data class AnimatedScalarProperty(
  @SerialName("sid") override val slotId: String? = null,
  @SerialName("a") val animatedInt: Int = 1,
  @SerialName("k") val keyframes: List<ScalarPropertyKeyframe> = emptyList(),
) : BaseScalarProperty() {
  override val animated: Boolean
    get() = animatedInt == 1
}

/** A single keyframe for an animated scalar property. */
internal typealias ScalarPropertyKeyframe = LottieKeyframe<Float>

@Serializable(with = ScalarKeyframeEasingSerializer::class)
internal data class ScalarKeyframeEasing(val x: Float, val y: Float)

/** Polymorphic serializer for [BaseScalarProperty]. */
internal object BaseScalarPropertySerializer : KSerializer<BaseScalarProperty> {
  override val descriptor: SerialDescriptor =
    LottiePropertySerializer(ScalarValueSerializer).descriptor

  override fun deserialize(decoder: Decoder): BaseScalarProperty {
    val jsonDecoder = decoder as JsonDecoder
    val element = jsonDecoder.decodeJsonElement()
    val animated = element is JsonObject && element["a"]?.jsonPrimitive?.intOrNull == 1
    return if (animated) {
      jsonDecoder.json.decodeFromJsonElement(AnimatedScalarPropertySerializer, element)
    } else {
      jsonDecoder.json.decodeFromJsonElement(StaticScalarPropertySerializer, element)
    }
  }

  override fun serialize(encoder: Encoder, value: BaseScalarProperty) {
    when (value) {
      is AnimatedScalarProperty ->
        encoder.encodeSerializableValue(AnimatedScalarPropertySerializer, value)
      is StaticScalarProperty ->
        encoder.encodeSerializableValue(StaticScalarPropertySerializer, value)
    }
  }
}

/** Helper to parse a scalar float value from a [JsonElement]. */
internal fun parseScalarElement(element: JsonElement?): Float {
  return when (element) {
    null -> 0f
    is JsonPrimitive -> element.floatOrNull ?: 0f
    is JsonArray -> {
      if (element.isEmpty()) {
        0f
      } else {
        parseScalarElement(element.first())
      }
    }
    is JsonObject -> {
      element["k"]?.let { parseScalarElement(it) }
        ?: element["s"]?.let { parseScalarElement(it) }
        ?: 0f
    }
  }
}

/** Serializer for [StaticScalarProperty]. */
internal object StaticScalarPropertySerializer : KSerializer<StaticScalarProperty> {
  private val delegate = StaticPropertySerializer(ScalarValueSerializer)
  override val descriptor: SerialDescriptor = delegate.descriptor

  override fun deserialize(decoder: Decoder): StaticScalarProperty {
    val prop = delegate.deserialize(decoder)
    return StaticScalarProperty(slotId = prop.slotId, animated = prop.animated, value = prop.value)
  }

  override fun serialize(encoder: Encoder, value: StaticScalarProperty) {
    delegate.serialize(encoder, StaticProperty(value.slotId, value.animated, value.value))
  }
}

/** Serializer for [AnimatedScalarProperty]. */
internal object AnimatedScalarPropertySerializer : KSerializer<AnimatedScalarProperty> {
  private val delegate = AnimatedPropertySerializer(ScalarValueSerializer)
  override val descriptor: SerialDescriptor = delegate.descriptor

  override fun deserialize(decoder: Decoder): AnimatedScalarProperty {
    val prop = delegate.deserialize(decoder)
    return AnimatedScalarProperty(
      slotId = prop.slotId,
      animatedInt = prop.animatedInt,
      keyframes = prop.keyframes,
    )
  }

  override fun serialize(encoder: Encoder, value: AnimatedScalarProperty) {
    delegate.serialize(encoder, AnimatedProperty(value.slotId, value.animatedInt, value.keyframes))
  }
}

/** Serializer for [ScalarPropertyKeyframe]. */
internal object ScalarPropertyKeyframeSerializer :
  KSerializer<ScalarPropertyKeyframe> by LottieKeyframeSerializer(ScalarValueSerializer)

/** Serializer for [ScalarKeyframeEasing] handling numbers or 1-element arrays. */
internal object ScalarKeyframeEasingSerializer : KSerializer<ScalarKeyframeEasing> {
  override val descriptor: SerialDescriptor =
    buildClassSerialDescriptor("ScalarKeyframeEasing") {
      element<Float>("x")
      element<Float>("y")
    }

  override fun deserialize(decoder: Decoder): ScalarKeyframeEasing {
    val jsonDecoder = decoder as JsonDecoder
    val element = jsonDecoder.decodeJsonElement().jsonObject
    val x = parseTangentValue(element["x"])
    val y = parseTangentValue(element["y"])
    return ScalarKeyframeEasing(x, y)
  }

  private fun parseTangentValue(element: JsonElement?): Float {
    return when (element) {
      is JsonPrimitive -> element.floatOrNull ?: 0f
      is JsonArray -> element.firstOrNull()?.jsonPrimitive?.floatOrNull ?: 0f
      else -> 0f
    }
  }

  override fun serialize(encoder: Encoder, value: ScalarKeyframeEasing) {
    val jsonEncoder = encoder as JsonEncoder
    jsonEncoder.encodeJsonElement(
      buildJsonObject {
        put("x", value.x)
        put("y", value.y)
      }
    )
  }
}
