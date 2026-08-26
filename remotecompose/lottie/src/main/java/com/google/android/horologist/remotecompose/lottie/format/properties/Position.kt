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
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** Base class for all Lottie position properties. */
@Serializable(with = BasePositionPropertySerializer::class)
internal sealed class BasePositionProperty : LottieProperty<List<Float>>

/** A static position property holding a list of [Float]s. */
@Serializable(with = StaticPositionPropertySerializer::class)
internal data class StaticPositionProperty(
  @SerialName("sid") override val slotId: String? = null,
  override val animated: Boolean = false,
  @SerialName("k") val value: List<Float>,
) : BasePositionProperty()

/** An animated position property with multi-dimensional keyframes. */
@Serializable(with = AnimatedPositionPropertySerializer::class)
internal data class AnimatedPositionProperty(
  @SerialName("sid") override val slotId: String? = null,
  @SerialName("a") val animatedInt: Int = 1,
  @SerialName("k") val keyframes: List<PositionPropertyKeyframe> = emptyList(),
) : BasePositionProperty() {
  override val animated: Boolean
    get() = animatedInt == 1
}

/**
 * A split position property where individual X, Y, and optional Z coordinates are animated
 * separately using scalar properties.
 */
@Serializable(with = SplitPositionPropertySerializer::class)
internal data class SplitPositionProperty(
  @SerialName("sid") override val slotId: String? = null,
  @SerialName("s") val split: Boolean = true,
  @SerialName("x") val x: BaseScalarProperty = StaticScalarProperty(value = 0f),
  @SerialName("y") val y: BaseScalarProperty = StaticScalarProperty(value = 0f),
  @SerialName("z") val z: BaseScalarProperty? = null,
) : BasePositionProperty() {
  override val animated: Boolean
    get() = x.animated || y.animated || (z?.animated ?: false)
}

/** A single keyframe for an animated position property with optional spatial tangents. */
internal typealias PositionPropertyKeyframe = LottieKeyframe<List<Float>>

/** Polymorphic serializer for [BasePositionProperty] based on "s" and "a" fields. */
internal object BasePositionPropertySerializer : KSerializer<BasePositionProperty> {
  override val descriptor: SerialDescriptor =
    LottiePropertySerializer(PositionValueSerializer).descriptor

  override fun deserialize(decoder: Decoder): BasePositionProperty {
    val jsonDecoder = decoder as JsonDecoder
    val element = jsonDecoder.decodeJsonElement()
    if (element is JsonObject) {
      val isSplit =
        element["s"]?.let { sElem ->
          (sElem as? JsonPrimitive)?.booleanOrNull ?: ((sElem as? JsonPrimitive)?.intOrNull == 1)
        } ?: false
      if (isSplit) {
        return jsonDecoder.json.decodeFromJsonElement(SplitPositionPropertySerializer, element)
      }
      val animated = element["a"]?.jsonPrimitive?.intOrNull == 1
      if (animated) {
        return jsonDecoder.json.decodeFromJsonElement(AnimatedPositionPropertySerializer, element)
      }
    }
    return jsonDecoder.json.decodeFromJsonElement(StaticPositionPropertySerializer, element)
  }

  override fun serialize(encoder: Encoder, value: BasePositionProperty) {
    when (value) {
      is SplitPositionProperty ->
        encoder.encodeSerializableValue(SplitPositionPropertySerializer, value)
      is AnimatedPositionProperty ->
        encoder.encodeSerializableValue(AnimatedPositionPropertySerializer, value)
      is StaticPositionProperty ->
        encoder.encodeSerializableValue(StaticPositionPropertySerializer, value)
    }
  }
}

/** Helper to parse a position coordinate list from a [JsonElement]. */
internal fun parsePositionVectorElement(element: JsonElement?): List<Float> {
  return when (element) {
    null -> emptyList()
    is JsonPrimitive -> element.floatOrNull?.let { listOf(it) } ?: emptyList()
    is JsonArray -> {
      if (element.isEmpty()) {
        emptyList()
      } else if (element.first() is JsonArray) {
        parsePositionVectorElement(element.first())
      } else {
        element.mapNotNull { it.jsonPrimitive.floatOrNull }
      }
    }
    is JsonObject -> {
      element["k"]?.let { parsePositionVectorElement(it) }
        ?: element["s"]?.let { parsePositionVectorElement(it) }
        ?: emptyList()
    }
  }
}

/** Serializer for [StaticPositionProperty]. */
internal object StaticPositionPropertySerializer : KSerializer<StaticPositionProperty> {
  private val delegate = StaticPropertySerializer(PositionValueSerializer)
  override val descriptor: SerialDescriptor = delegate.descriptor

  override fun deserialize(decoder: Decoder): StaticPositionProperty {
    val prop = delegate.deserialize(decoder)
    return StaticPositionProperty(
      slotId = prop.slotId,
      animated = prop.animated,
      value = prop.value,
    )
  }

  override fun serialize(encoder: Encoder, value: StaticPositionProperty) {
    delegate.serialize(encoder, StaticProperty(value.slotId, value.animated, value.value))
  }
}

/** Serializer for [AnimatedPositionProperty]. */
internal object AnimatedPositionPropertySerializer : KSerializer<AnimatedPositionProperty> {
  private val delegate = AnimatedPropertySerializer(PositionValueSerializer)
  override val descriptor: SerialDescriptor = delegate.descriptor

  override fun deserialize(decoder: Decoder): AnimatedPositionProperty {
    val prop = delegate.deserialize(decoder)
    return AnimatedPositionProperty(
      slotId = prop.slotId,
      animatedInt = prop.animatedInt,
      keyframes = prop.keyframes,
    )
  }

  override fun serialize(encoder: Encoder, value: AnimatedPositionProperty) {
    delegate.serialize(encoder, AnimatedProperty(value.slotId, value.animatedInt, value.keyframes))
  }
}

/** Serializer for [SplitPositionProperty]. */
internal object SplitPositionPropertySerializer : KSerializer<SplitPositionProperty> {
  override val descriptor: SerialDescriptor =
    buildClassSerialDescriptor("SplitPositionProperty") {
      element<String?>("sid", isOptional = true)
      element<Boolean>("s", isOptional = true)
      element<BaseScalarProperty>("x", isOptional = true)
      element<BaseScalarProperty>("y", isOptional = true)
      element<BaseScalarProperty?>("z", isOptional = true)
    }

  override fun deserialize(decoder: Decoder): SplitPositionProperty {
    val jsonDecoder = decoder as JsonDecoder
    val obj = jsonDecoder.decodeJsonElement().jsonObject
    val slotId = obj["sid"]?.jsonPrimitive?.contentOrNull
    val split =
      obj["s"]?.let { sElem ->
        (sElem as? JsonPrimitive)?.booleanOrNull ?: ((sElem as? JsonPrimitive)?.intOrNull == 1)
      } ?: true
    val x =
      obj["x"]?.let { jsonDecoder.json.decodeFromJsonElement(BaseScalarPropertySerializer, it) }
        ?: StaticScalarProperty(value = 0f)
    val y =
      obj["y"]?.let { jsonDecoder.json.decodeFromJsonElement(BaseScalarPropertySerializer, it) }
        ?: StaticScalarProperty(value = 0f)
    val z =
      obj["z"]?.let { jsonDecoder.json.decodeFromJsonElement(BaseScalarPropertySerializer, it) }

    return SplitPositionProperty(slotId = slotId, split = split, x = x, y = y, z = z)
  }

  override fun serialize(encoder: Encoder, value: SplitPositionProperty) {
    val jsonEncoder = encoder as JsonEncoder
    jsonEncoder.encodeJsonElement(
      buildJsonObject {
        value.slotId?.let { put("sid", it) }
        put("s", value.split)
        put("x", jsonEncoder.json.encodeToJsonElement(BaseScalarPropertySerializer, value.x))
        put("y", jsonEncoder.json.encodeToJsonElement(BaseScalarPropertySerializer, value.y))
        value.z?.let {
          put("z", jsonEncoder.json.encodeToJsonElement(BaseScalarPropertySerializer, it))
        }
      }
    )
  }
}

/** Serializer for [PositionPropertyKeyframe]. */
internal object PositionPropertyKeyframeSerializer :
  KSerializer<PositionPropertyKeyframe> by LottieKeyframeSerializer(PositionValueSerializer)
