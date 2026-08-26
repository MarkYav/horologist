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

import com.google.android.horologist.remotecompose.lottie.format.values.BezierValue
import com.google.android.horologist.remotecompose.lottie.format.values.BezierValueSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** Base class for all Lottie Bezier (Shape) properties. */
@Serializable(with = BaseBezierPropertySerializer::class)
internal sealed class BaseBezierProperty : LottieProperty<BezierValue>

/** A static bezier property holding a [BezierValue]. */
@Serializable(with = StaticBezierPropertySerializer::class)
internal data class StaticBezierProperty(
  @SerialName("sid") override val slotId: String? = null,
  override val animated: Boolean = false,
  @SerialName("k") val value: BezierValue,
) : BaseBezierProperty()

/** An animated bezier property with keyframes. */
@Serializable(with = AnimatedBezierPropertySerializer::class)
internal data class AnimatedBezierProperty(
  @SerialName("sid") override val slotId: String? = null,
  @SerialName("a") val animatedInt: Int = 1,
  @SerialName("k") val keyframes: List<BezierPropertyKeyframe> = emptyList(),
) : BaseBezierProperty() {
  override val animated: Boolean
    get() = animatedInt == 1
}

/** A single keyframe for an animated bezier property. */
internal typealias BezierPropertyKeyframe = LottieKeyframe<List<BezierValue>>

/** Polymorphic serializer for [BaseBezierProperty] based on "a" field. */
internal object BaseBezierPropertySerializer : KSerializer<BaseBezierProperty> {
  override val descriptor: SerialDescriptor =
    buildClassSerialDescriptor("BaseBezierProperty") {
      element<String?>("sid", isOptional = true)
      element<Int>("a", isOptional = true)
      element<JsonElement>("k")
    }

  override fun deserialize(decoder: Decoder): BaseBezierProperty {
    val jsonDecoder = decoder as JsonDecoder
    val element = jsonDecoder.decodeJsonElement()
    val animated = element is JsonObject && element["a"]?.jsonPrimitive?.intOrNull == 1
    return if (animated) {
      jsonDecoder.json.decodeFromJsonElement(AnimatedBezierPropertySerializer, element)
    } else {
      jsonDecoder.json.decodeFromJsonElement(StaticBezierPropertySerializer, element)
    }
  }

  override fun serialize(encoder: Encoder, value: BaseBezierProperty) {
    when (value) {
      is AnimatedBezierProperty ->
        encoder.encodeSerializableValue(AnimatedBezierPropertySerializer, value)
      is StaticBezierProperty ->
        encoder.encodeSerializableValue(StaticBezierPropertySerializer, value)
    }
  }
}

/** Serializer for [StaticBezierProperty]. */
internal object StaticBezierPropertySerializer : KSerializer<StaticBezierProperty> {
  override val descriptor: SerialDescriptor =
    buildClassSerialDescriptor("StaticBezierProperty") {
      element<String?>("sid", isOptional = true)
      element<Boolean>("animated", isOptional = true)
      element<BezierValue>("k")
    }

  override fun deserialize(decoder: Decoder): StaticBezierProperty {
    val jsonDecoder = decoder as JsonDecoder
    val element = jsonDecoder.decodeJsonElement()
    return when (element) {
      is JsonObject -> {
        val slotId = element["sid"]?.jsonPrimitive?.contentOrNull
        val kElem = element["k"]
        val bezierValue =
          if (kElem != null) {
            jsonDecoder.json.decodeFromJsonElement(BezierValueSerializer, kElem)
          } else {
            jsonDecoder.json.decodeFromJsonElement(BezierValueSerializer, element)
          }
        StaticBezierProperty(slotId = slotId, animated = false, value = bezierValue)
      }
      else -> {
        val bezierValue = jsonDecoder.json.decodeFromJsonElement(BezierValueSerializer, element)
        StaticBezierProperty(slotId = null, animated = false, value = bezierValue)
      }
    }
  }

  override fun serialize(encoder: Encoder, value: StaticBezierProperty) {
    val jsonEncoder = encoder as JsonEncoder
    jsonEncoder.encodeJsonElement(
      buildJsonObject {
        value.slotId?.let { put("sid", it) }
        put("a", 0)
        put("k", jsonEncoder.json.encodeToJsonElement(BezierValueSerializer, value.value))
      }
    )
  }
}

/** Serializer for [AnimatedBezierProperty]. */
internal object AnimatedBezierPropertySerializer : KSerializer<AnimatedBezierProperty> {
  override val descriptor: SerialDescriptor =
    buildClassSerialDescriptor("AnimatedBezierProperty") {
      element<String?>("sid", isOptional = true)
      element<Int>("a")
      element<List<BezierPropertyKeyframe>>("k")
    }

  override fun deserialize(decoder: Decoder): AnimatedBezierProperty {
    val jsonDecoder = decoder as JsonDecoder
    val obj = jsonDecoder.decodeJsonElement().jsonObject
    val slotId = obj["sid"]?.jsonPrimitive?.contentOrNull
    val animatedInt = obj["a"]?.jsonPrimitive?.intOrNull ?: 1
    val keyframesArray = obj["k"]?.jsonArray
    val keyframes =
      keyframesArray?.map { element ->
        jsonDecoder.json.decodeFromJsonElement(BezierPropertyKeyframeSerializer, element)
      } ?: emptyList()
    return AnimatedBezierProperty(slotId = slotId, animatedInt = animatedInt, keyframes = keyframes)
  }

  override fun serialize(encoder: Encoder, value: AnimatedBezierProperty) {
    val jsonEncoder = encoder as JsonEncoder
    jsonEncoder.encodeJsonElement(
      buildJsonObject {
        value.slotId?.let { put("sid", it) }
        put("a", value.animatedInt)
        put(
          "k",
          jsonEncoder.json.encodeToJsonElement(
            ListSerializer(BezierPropertyKeyframeSerializer),
            value.keyframes,
          ),
        )
      }
    )
  }
}

/** Serializer for [BezierPropertyKeyframe]. */
internal object BezierPropertyKeyframeSerializer :
  KSerializer<BezierPropertyKeyframe> by LottieKeyframeSerializer(BezierListValueSerializer)
