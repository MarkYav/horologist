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

package com.google.android.horologist.remotecompose.lottie.format.layer

import com.google.android.horologist.remotecompose.lottie.format.graphicelement.grouping.Transform
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * A layer in a Lottie animation.
 *
 * Layer parenting provides a way for layer transforms to be applied to child layers. This allows
 * for a single set of transforms to be applied to multiple layers.
 */
@Serializable(with = LayerSerializer::class)
internal sealed class Layer {
  abstract val name: String?
  abstract val hidden: Boolean?
  abstract val type: LayerType
  abstract val index: Int?
  abstract val parent: Int?
  abstract val startFrame: Float?
  abstract val endFrame: Float?
  abstract val startTime: Float?
  abstract val timeStretch: Float?
  abstract val transform: Transform?
  abstract val matteMode: MatteMode?
  abstract val matteParent: Int?
  abstract val matteTarget: Int?
}

@Serializable(with = LayerTypeSerializer::class)
internal enum class LayerType(val value: Int) {
  Precomposition(0),
  Solid(1),
  Image(2),
  Null(3),
  Shape(4),
  Text(5),
  Audio(6),
  Unknown(-1);

  companion object {
    fun fromValueOrNull(value: Int): LayerType? {
      return values().firstOrNull { it.value == value }
    }
  }
}

/** Polymorphic serializer for [Layer] based on integer "ty" field. */
internal object LayerSerializer : JsonContentPolymorphicSerializer<Layer>(Layer::class) {
  override fun selectDeserializer(element: JsonElement): DeserializationStrategy<Layer> {
    val tyPrimitive = element.jsonObject["ty"]?.jsonPrimitive
    val ty = tyPrimitive?.intOrNull ?: tyPrimitive?.floatOrNull?.toInt()
    return when (ty) {
      LayerType.Solid.value -> SolidColorLayer.serializer()
      LayerType.Null.value -> NullLayer.serializer()
      LayerType.Shape.value -> ShapeLayer.serializer()
      else -> UnknownLayer.serializer()
    }
  }
}

/** Serializer for [LayerType] enum. */
internal object LayerTypeSerializer : KSerializer<LayerType> {
  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor("LayerType", PrimitiveKind.INT)

  override fun deserialize(decoder: Decoder): LayerType {
    return try {
      val jsonDecoder = decoder as? JsonDecoder
      if (jsonDecoder != null) {
        val element = jsonDecoder.decodeJsonElement()
        val intVal =
          element.jsonPrimitive.intOrNull ?: element.jsonPrimitive.floatOrNull?.toInt() ?: -1
        LayerType.fromValueOrNull(intVal) ?: LayerType.Unknown
      } else {
        val value = decoder.decodeInt()
        LayerType.fromValueOrNull(value) ?: LayerType.Unknown
      }
    } catch (e: Exception) {
      LayerType.Unknown
    }
  }

  override fun serialize(encoder: Encoder, value: LayerType) {
    encoder.encodeInt(value.value)
  }
}
