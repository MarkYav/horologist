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

package com.google.android.horologist.remotecompose.lottie.format.text

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/** Encapsulates text formatting, typography properties, and styling for a Lottie text document. */
@Serializable
internal data class TextDocument(
  @SerialName("t") val text: String = "",
  @SerialName("s") val fontSize: Float = 12f,
  @SerialName("f") val fontName: String = "",
  @SerialName("j") val justification: TextJustify = TextJustify.Left,
  @SerialName("tr") val tracking: Float = 0f,
  @SerialName("lh") val lineHeight: Float? = null,
  @SerialName("ls") val baselineShift: Float? = null,
  @SerialName("fc") val fillColor: List<Float>? = null,
  @SerialName("sc") val strokeColor: List<Float>? = null,
  @SerialName("sw") val strokeWidth: Float? = null,
  @SerialName("of") val strokeOverFill: Boolean? = true,
  @SerialName("ps") val boxSize: List<Float>? = null,
  @SerialName("p") val boxPosition: List<Float>? = null,
  @SerialName("ca") val capitalization: Int? = 0,
  @SerialName("sz") val renderedSize: List<Float>? = null,
)

/** Text justification / alignment enumeration according to the Lottie specification. */
@Serializable(with = TextJustifySerializer::class)
internal enum class TextJustify(val value: Int) {
  Left(0),
  Right(1),
  Center(2),
  JustifyWithLastLineLeft(3),
  JustifyWithLastLineRight(4),
  JustifyWithLastLineCenter(5),
  JustifyWithLastLineFull(6);

  companion object {
    fun fromValueOrNull(value: Int): TextJustify? = entries.firstOrNull { it.value == value }
  }
}

internal object TextJustifySerializer : KSerializer<TextJustify> {
  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor("TextJustify", PrimitiveKind.INT)

  override fun deserialize(decoder: Decoder): TextJustify {
    return try {
      val jsonDecoder = decoder as? JsonDecoder
      if (jsonDecoder != null) {
        val intVal = jsonDecoder.decodeJsonElement().jsonPrimitive.intOrNull ?: 0
        TextJustify.fromValueOrNull(intVal) ?: TextJustify.Left
      } else {
        val value = decoder.decodeInt()
        TextJustify.fromValueOrNull(value) ?: TextJustify.Left
      }
    } catch (_: Exception) {
      TextJustify.Left
    }
  }

  override fun serialize(encoder: Encoder, value: TextJustify) {
    encoder.encodeInt(value.value)
  }
}

/** Text capitalization options. */
internal enum class TextCaps(val value: Int) {
  Regular(0),
  AllCaps(1),
  SmallCaps(2),
  TitleCase(3);

  companion object {
    fun fromValueOrNull(value: Int): TextCaps? = entries.firstOrNull { it.value == value }
  }
}

/** Text grouping options for text animators. */
internal enum class TextGrouping(val value: Int) {
  Characters(1),
  Words(2),
  Line(3),
  All(4);

  companion object {
    fun fromValueOrNull(value: Int): TextGrouping? = entries.firstOrNull { it.value == value }
  }
}

/** Text property wrapping one or more [TextDocumentKeyframe] keyframes. */
@Serializable(with = TextDocumentPropertySerializer::class)
internal data class TextDocumentProperty(
  @SerialName("k") val keyframes: List<TextDocumentKeyframe> = emptyList()
)

/** A keyframe holding a [TextDocument] at a specific timestamp. */
@Serializable
internal data class TextDocumentKeyframe(
  @SerialName("s") val start: TextDocument? = null,
  @SerialName("t") val time: Float? = null,
)

/** Text container holding document property, layout options, and animators. */
@Serializable
internal data class TextData(
  @SerialName("d") val document: TextDocumentProperty? = null,
  @SerialName("m") val moreOptions: TextMoreOptions? = null,
  @SerialName("a") val animators: List<TextAnimator> = emptyList(),
)

/** Advanced text layout and alignment options. */
@Serializable
internal data class TextMoreOptions(
  @SerialName("a") val alignment: List<Float>? = null,
  @SerialName("g") val grouping: Int? = null,
)

/** Text animator definition. */
@Serializable internal data class TextAnimator(@SerialName("nm") val name: String? = null)

internal object TextDocumentPropertySerializer : KSerializer<TextDocumentProperty> {
  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor("TextDocumentProperty", PrimitiveKind.STRING)

  override fun deserialize(decoder: Decoder): TextDocumentProperty {
    val jsonDecoder = decoder as? JsonDecoder ?: return TextDocumentProperty()
    val json = jsonDecoder.json
    val element = jsonDecoder.decodeJsonElement()
    return try {
      if (element is JsonObject) {
        val kElement = element["k"]
        if (kElement is JsonArray) {
          val keyframes = kElement.mapNotNull { item ->
            try {
              if (item is JsonObject && item.containsKey("s")) {
                json.decodeFromJsonElement(TextDocumentKeyframe.serializer(), item)
              } else if (item is JsonObject && item.containsKey("t")) {
                val doc = json.decodeFromJsonElement(TextDocument.serializer(), item)
                TextDocumentKeyframe(start = doc, time = 0f)
              } else {
                null
              }
            } catch (_: Exception) {
              null
            }
          }
          TextDocumentProperty(keyframes)
        } else if (kElement is JsonObject) {
          if (kElement.containsKey("s")) {
            val kf = json.decodeFromJsonElement(TextDocumentKeyframe.serializer(), kElement)
            TextDocumentProperty(listOf(kf))
          } else {
            val doc = json.decodeFromJsonElement(TextDocument.serializer(), kElement)
            TextDocumentProperty(listOf(TextDocumentKeyframe(start = doc, time = 0f)))
          }
        } else {
          TextDocumentProperty()
        }
      } else {
        TextDocumentProperty()
      }
    } catch (_: Exception) {
      TextDocumentProperty()
    }
  }

  override fun serialize(encoder: Encoder, value: TextDocumentProperty) {
    // Serialization not required for deserializer
  }
}
