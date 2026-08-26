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

import com.google.android.horologist.remotecompose.lottie.format.values.GradientValue
import com.google.android.horologist.remotecompose.lottie.format.values.GradientValueSerializer
import com.google.android.horologist.remotecompose.lottie.format.values.parseGradientValueElement
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

/** Base class for all Lottie gradient properties. */
@Serializable(with = BaseGradientPropertySerializer::class)
internal sealed class BaseGradientProperty : LottieProperty<GradientValue>

/** A static gradient property holding a [GradientValue]. */
@Serializable(with = StaticGradientPropertySerializer::class)
internal data class StaticGradientProperty(
  @SerialName("sid") override val slotId: String? = null,
  override val animated: Boolean = false,
  @SerialName("k") val value: GradientValue = GradientValue(),
) : BaseGradientProperty()

/** An animated gradient property with keyframes. */
@Serializable(with = AnimatedGradientPropertySerializer::class)
internal data class AnimatedGradientProperty(
  @SerialName("sid") override val slotId: String? = null,
  @SerialName("p") val numberOfColors: Int? = null,
  @SerialName("a") val animatedInt: Int = 1,
  @SerialName("k") val keyframes: List<GradientPropertyKeyframe> = emptyList(),
) : BaseGradientProperty() {
  override val animated: Boolean
    get() = animatedInt == 1
}

/** A single keyframe for an animated gradient property. */
internal typealias GradientPropertyKeyframe = LottieKeyframe<List<GradientValue>>

/** Polymorphic serializer for [BaseGradientProperty] based on "a" field. */
internal object BaseGradientPropertySerializer : KSerializer<BaseGradientProperty> {
  override val descriptor: SerialDescriptor =
    buildClassSerialDescriptor("BaseGradientProperty") {
      element<String?>("sid", isOptional = true)
      element<Int>("a", isOptional = true)
      element<JsonElement>("k")
    }

  override fun deserialize(decoder: Decoder): BaseGradientProperty {
    val jsonDecoder = decoder as JsonDecoder
    val element = jsonDecoder.decodeJsonElement()
    val animated = element is JsonObject && element["a"]?.jsonPrimitive?.intOrNull == 1
    return if (animated) {
      jsonDecoder.json.decodeFromJsonElement(AnimatedGradientPropertySerializer, element)
    } else {
      jsonDecoder.json.decodeFromJsonElement(StaticGradientPropertySerializer, element)
    }
  }

  override fun serialize(encoder: Encoder, value: BaseGradientProperty) {
    when (value) {
      is AnimatedGradientProperty ->
        encoder.encodeSerializableValue(AnimatedGradientPropertySerializer, value)
      is StaticGradientProperty ->
        encoder.encodeSerializableValue(StaticGradientPropertySerializer, value)
    }
  }
}

/** Serializer for [StaticGradientProperty]. */
internal object StaticGradientPropertySerializer : KSerializer<StaticGradientProperty> {
  override val descriptor: SerialDescriptor =
    buildClassSerialDescriptor("StaticGradientProperty") {
      element<String?>("sid", isOptional = true)
      element<Boolean>("animated", isOptional = true)
      element<GradientValue>("k")
    }

  override fun deserialize(decoder: Decoder): StaticGradientProperty {
    val jsonDecoder = decoder as JsonDecoder
    val element = jsonDecoder.decodeJsonElement()
    return when (element) {
      is JsonObject -> {
        val slotId = element["sid"]?.jsonPrimitive?.contentOrNull
        val kElem = element["k"]
        val p = element["p"]?.jsonPrimitive?.intOrNull
        val gradientValue =
          if (kElem != null) {
            val parsed = parseGradientValueElement(kElem)
            if (p != null && p > 0 && parsed.numberOfColors == 0) {
              parsed.copy(numberOfColors = p)
            } else {
              parsed
            }
          } else {
            parseGradientValueElement(element)
          }
        StaticGradientProperty(slotId = slotId, animated = false, value = gradientValue)
      }
      else -> {
        val gradientValue = parseGradientValueElement(element)
        StaticGradientProperty(slotId = null, animated = false, value = gradientValue)
      }
    }
  }

  override fun serialize(encoder: Encoder, value: StaticGradientProperty) {
    val jsonEncoder = encoder as JsonEncoder
    jsonEncoder.encodeJsonElement(
      buildJsonObject {
        value.slotId?.let { put("sid", it) }
        put("a", 0)
        put("k", jsonEncoder.json.encodeToJsonElement(GradientValueSerializer, value.value))
      }
    )
  }
}

/** Serializer for [AnimatedGradientProperty]. */
internal object AnimatedGradientPropertySerializer : KSerializer<AnimatedGradientProperty> {
  override val descriptor: SerialDescriptor =
    buildClassSerialDescriptor("AnimatedGradientProperty") {
      element<String?>("sid", isOptional = true)
      element<Int?>("p", isOptional = true)
      element<Int>("a")
      element<List<GradientPropertyKeyframe>>("k")
    }

  override fun deserialize(decoder: Decoder): AnimatedGradientProperty {
    val jsonDecoder = decoder as JsonDecoder
    val obj = jsonDecoder.decodeJsonElement().jsonObject
    val slotId = obj["sid"]?.jsonPrimitive?.contentOrNull
    val numberOfColors = obj["p"]?.jsonPrimitive?.intOrNull
    val animatedInt = obj["a"]?.jsonPrimitive?.intOrNull ?: 1
    val keyframesArray = obj["k"]?.jsonArray
    val keyframeSerializer = GradientPropertyKeyframeSerializer
    val keyframes =
      keyframesArray?.map { element ->
        val kf = jsonDecoder.json.decodeFromJsonElement(keyframeSerializer, element)
        if (numberOfColors != null && numberOfColors > 0) {
          kf.copy(
            value =
              kf.value.map { gv ->
                if (gv.numberOfColors == 0) gv.copy(numberOfColors = numberOfColors) else gv
              }
          )
        } else {
          kf
        }
      } ?: emptyList()
    return AnimatedGradientProperty(
      slotId = slotId,
      numberOfColors = numberOfColors,
      animatedInt = animatedInt,
      keyframes = keyframes,
    )
  }

  override fun serialize(encoder: Encoder, value: AnimatedGradientProperty) {
    val jsonEncoder = encoder as JsonEncoder
    jsonEncoder.encodeJsonElement(
      buildJsonObject {
        value.slotId?.let { put("sid", it) }
        value.numberOfColors?.let { put("p", it) }
        put("a", value.animatedInt)
        put(
          "k",
          jsonEncoder.json.encodeToJsonElement(
            ListSerializer(GradientPropertyKeyframeSerializer),
            value.keyframes,
          ),
        )
      }
    )
  }
}

/** Serializer for [GradientPropertyKeyframe]. */
internal object GradientPropertyKeyframeSerializer :
  KSerializer<GradientPropertyKeyframe> by LottieKeyframeSerializer(GradientListValueSerializer)
