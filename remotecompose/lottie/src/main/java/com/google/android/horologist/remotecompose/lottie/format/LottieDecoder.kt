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

import android.content.Context
import androidx.annotation.RawRes
import java.io.InputStream
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** `kotlinx.serialization` JSON decoder for Lottie animations. */
internal object LottieDecoder {

  val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
    encodeDefaults = true
    explicitNulls = false
  }

  fun decodeFromString(jsonString: String): Animation {
    return json.decodeFromString(Animation.serializer(), jsonString)
  }

  @OptIn(ExperimentalSerializationApi::class)
  fun decodeFromStream(stream: InputStream): Animation {
    return json.decodeFromStream(Animation.serializer(), stream)
  }

  fun load(@RawRes rawRes: Int, context: Context): Animation {
    return context.resources.openRawResource(rawRes).use { stream -> decodeFromStream(stream) }
  }
}

/** Polymorphic serializer for [Layer] based on integer "ty" field. */
internal object LayerSerializer : JsonContentPolymorphicSerializer<Layer>(Layer::class) {
  override fun selectDeserializer(element: JsonElement): DeserializationStrategy<Layer> {
    val ty = element.jsonObject["ty"]?.jsonPrimitive?.intOrNull
    return when (ty) {
      LayerType.Null.value -> Layer.NullLayer.serializer()
      LayerType.Shape.value -> Layer.ShapeLayer.serializer()
      else -> Layer.NullLayer.serializer()
    }
  }
}

/** Serializer for [LayerType] enum. */
internal object LayerTypeSerializer : KSerializer<LayerType> {
  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor("LayerType", PrimitiveKind.INT)

  override fun deserialize(decoder: Decoder): LayerType {
    val value = decoder.decodeInt()
    return LayerType.fromValueOrNull(value) ?: LayerType.Null
  }

  override fun serialize(encoder: Encoder, value: LayerType) {
    encoder.encodeInt(value.value)
  }
}

/** Polymorphic serializer for [GraphicElement] based on string "ty" field. */
internal object GraphicElementSerializer :
  JsonContentPolymorphicSerializer<GraphicElement>(GraphicElement::class) {
  override fun selectDeserializer(element: JsonElement): DeserializationStrategy<GraphicElement> {
    val ty = element.jsonObject["ty"]?.jsonPrimitive?.contentOrNull
    return when (ty) {
      ShapeType.Path.value -> GraphicElement.Path.serializer()
      ShapeType.Group.value -> GraphicElement.Group.serializer()
      ShapeType.Transform.value -> GraphicElement.Transform.serializer()
      ShapeType.Fill.value -> GraphicElement.Fill.serializer()
      ShapeType.GradientFill.value -> GraphicElement.GradientFill.serializer()
      ShapeType.GradientStroke.value -> GraphicElement.GradientStroke.serializer()
      ShapeType.Rectangle.value -> GraphicElement.Rectangle.serializer()
      ShapeType.Ellipse.value -> GraphicElement.Ellipse.serializer()
      ShapeType.PolyStar.value -> GraphicElement.PolyStar.serializer()
      else -> GraphicElement.Group.serializer()
    }
  }
}

/** Serializer for [ShapeType] enum. */
internal object ShapeTypeSerializer : KSerializer<ShapeType> {
  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor("ShapeType", PrimitiveKind.STRING)

  override fun deserialize(decoder: Decoder): ShapeType {
    val value = decoder.decodeString()
    return ShapeType.fromValueOrNull(value) ?: ShapeType.Group
  }

  override fun serialize(encoder: Encoder, value: ShapeType) {
    encoder.encodeString(value.value)
  }
}

/** Serializer for [PolyStarType] enum. */
internal object PolyStarTypeSerializer : KSerializer<PolyStarType> {
  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor("PolyStarType", PrimitiveKind.INT)

  override fun deserialize(decoder: Decoder): PolyStarType {
    val value = decoder.decodeInt()
    return PolyStarType.fromValueOrNull(value) ?: PolyStarType.Star
  }

  override fun serialize(encoder: Encoder, value: PolyStarType) {
    encoder.encodeInt(value.value)
  }
}

/** Serializer for [GradientType] enum. */
internal object GradientTypeSerializer : KSerializer<GradientType> {
  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor("GradientType", PrimitiveKind.INT)

  override fun deserialize(decoder: Decoder): GradientType {
    return try {
      val jsonDecoder = decoder as? JsonDecoder
      if (jsonDecoder != null) {
        val element = jsonDecoder.decodeJsonElement()
        val intVal =
          element.jsonPrimitive.intOrNull ?: element.jsonPrimitive.floatOrNull?.toInt() ?: 1
        GradientType.fromValueOrNull(intVal) ?: GradientType.Linear
      } else {
        val value = decoder.decodeInt()
        GradientType.fromValueOrNull(value) ?: GradientType.Linear
      }
    } catch (e: Exception) {
      GradientType.Linear
    }
  }

  override fun serialize(encoder: Encoder, value: GradientType) {
    encoder.encodeInt(value.value)
  }
}
