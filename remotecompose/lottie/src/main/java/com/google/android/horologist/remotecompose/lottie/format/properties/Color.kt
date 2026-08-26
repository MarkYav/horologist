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
import androidx.compose.ui.graphics.Color
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

/** Base class for all Lottie color properties. */
@Serializable(with = BaseColorPropertySerializer::class)
internal sealed class BaseColorProperty : LottieProperty<RemoteColor>

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
  @SerialName("k") val keyframes: List<ColorPropertyKeyframe> = emptyList(),
) : BaseColorProperty() {
  override val animated: Boolean
    get() = animatedInt == 1
}

/** A single keyframe for an animated color property. */
internal typealias ColorPropertyKeyframe = LottieKeyframe<RemoteColor>

/** Polymorphic serializer for [BaseColorProperty]. */
internal object BaseColorPropertySerializer : KSerializer<BaseColorProperty> {
  override val descriptor: SerialDescriptor =
    LottiePropertySerializer(ColorValueSerializer).descriptor

  override fun deserialize(decoder: Decoder): BaseColorProperty {
    val jsonDecoder = decoder as JsonDecoder
    val element = jsonDecoder.decodeJsonElement()
    val animated = element is JsonObject && element["a"]?.jsonPrimitive?.intOrNull == 1
    return if (animated) {
      jsonDecoder.json.decodeFromJsonElement(AnimatedColorPropertySerializer, element)
    } else {
      jsonDecoder.json.decodeFromJsonElement(StaticColorPropertySerializer, element)
    }
  }

  override fun serialize(encoder: Encoder, value: BaseColorProperty) {
    when (value) {
      is AnimatedColorProperty ->
        encoder.encodeSerializableValue(AnimatedColorPropertySerializer, value)
      is StaticColorProperty ->
        encoder.encodeSerializableValue(StaticColorPropertySerializer, value)
    }
  }
}

/** Helper to parse a color from a [JsonElement]. */
internal fun parseColorElement(element: JsonElement?): Color {
  return when (element) {
    null -> Color.Transparent
    is JsonPrimitive -> {
      if (element.isString) {
        parseHexColor(element.content) ?: Color.Transparent
      } else {
        element.intOrNull?.let { Color(it) } ?: Color.Transparent
      }
    }
    is JsonArray -> parseArrayColor(element)
    is JsonObject -> {
      element["k"]?.let { parseColorElement(it) }
        ?: element["s"]?.let { parseColorElement(it) }
        ?: Color.Transparent
    }
  }
}

/** Parses Hex color strings (#RGB, #ARGB, #RRGGBB, #AARRGGBB). */
internal fun parseHexColor(hexString: String): Color? {
  val hex = hexString.trim().removePrefix("#")
  return try {
    when (hex.length) {
      3 -> {
        val r = hex.substring(0, 1).repeat(2).toInt(16)
        val g = hex.substring(1, 2).repeat(2).toInt(16)
        val b = hex.substring(2, 3).repeat(2).toInt(16)
        Color(red = r / 255f, green = g / 255f, blue = b / 255f, alpha = 1f)
      }
      4 -> {
        val a = hex.substring(0, 1).repeat(2).toInt(16)
        val r = hex.substring(1, 2).repeat(2).toInt(16)
        val g = hex.substring(2, 3).repeat(2).toInt(16)
        val b = hex.substring(3, 4).repeat(2).toInt(16)
        Color(red = r / 255f, green = g / 255f, blue = b / 255f, alpha = a / 255f)
      }
      6 -> {
        val r = hex.substring(0, 2).toInt(16)
        val g = hex.substring(2, 4).toInt(16)
        val b = hex.substring(4, 6).toInt(16)
        Color(red = r / 255f, green = g / 255f, blue = b / 255f, alpha = 1f)
      }
      8 -> {
        val a = hex.substring(0, 2).toInt(16)
        val r = hex.substring(2, 4).toInt(16)
        val g = hex.substring(4, 6).toInt(16)
        val b = hex.substring(6, 8).toInt(16)
        Color(red = r / 255f, green = g / 255f, blue = b / 255f, alpha = a / 255f)
      }
      else -> null
    }
  } catch (e: Exception) {
    null
  }
}

/** Parses float or integer color arrays ([r, g, b] or [r, g, b, a]). */
internal fun parseArrayColor(array: JsonArray): Color {
  val r = array.getOrNull(0)?.jsonPrimitive?.floatOrNull ?: 0f
  val g = array.getOrNull(1)?.jsonPrimitive?.floatOrNull ?: 0f
  val b = array.getOrNull(2)?.jsonPrimitive?.floatOrNull ?: 0f
  val a = if (array.size > 3) array[3].jsonPrimitive.floatOrNull ?: 1f else 1f

  val red = if (r > 1f) (r / 255f).coerceIn(0f, 1f) else r.coerceIn(0f, 1f)
  val green = if (g > 1f) (g / 255f).coerceIn(0f, 1f) else g.coerceIn(0f, 1f)
  val blue = if (b > 1f) (b / 255f).coerceIn(0f, 1f) else b.coerceIn(0f, 1f)
  val alpha = if (a > 1f) (a / 255f).coerceIn(0f, 1f) else a.coerceIn(0f, 1f)

  return Color(red, green, blue, alpha)
}

/** Serializer for [StaticColorProperty]. */
internal object StaticColorPropertySerializer : KSerializer<StaticColorProperty> {
  private val delegate = StaticPropertySerializer(ColorValueSerializer)
  override val descriptor: SerialDescriptor = delegate.descriptor

  override fun deserialize(decoder: Decoder): StaticColorProperty {
    val prop = delegate.deserialize(decoder)
    return StaticColorProperty(slotId = prop.slotId, animated = prop.animated, value = prop.value)
  }

  override fun serialize(encoder: Encoder, value: StaticColorProperty) {
    delegate.serialize(encoder, StaticProperty(value.slotId, value.animated, value.value))
  }
}

/** Serializer for [AnimatedColorProperty]. */
internal object AnimatedColorPropertySerializer : KSerializer<AnimatedColorProperty> {
  private val delegate = AnimatedPropertySerializer(ColorValueSerializer)
  override val descriptor: SerialDescriptor = delegate.descriptor

  override fun deserialize(decoder: Decoder): AnimatedColorProperty {
    val prop = delegate.deserialize(decoder)
    return AnimatedColorProperty(
      slotId = prop.slotId,
      animatedInt = prop.animatedInt,
      keyframes = prop.keyframes,
    )
  }

  override fun serialize(encoder: Encoder, value: AnimatedColorProperty) {
    delegate.serialize(encoder, AnimatedProperty(value.slotId, value.animatedInt, value.keyframes))
  }
}

/** Serializer for [ColorPropertyKeyframe]. */
internal object ColorPropertyKeyframeSerializer :
  KSerializer<ColorPropertyKeyframe> by LottieKeyframeSerializer(ColorValueSerializer)
