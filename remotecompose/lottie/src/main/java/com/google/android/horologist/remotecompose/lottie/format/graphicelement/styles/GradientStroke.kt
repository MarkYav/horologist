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

package com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles

import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.ShapeType
import com.google.android.horologist.remotecompose.lottie.format.properties.BaseGradientProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.BasePositionProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.BaseScalarProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticGradientProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticPositionProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticScalarProperty
import com.google.android.horologist.remotecompose.lottie.format.values.GradientValue
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

@Serializable(with = LineCapSerializer::class)
internal enum class LineCap(val value: Int) {
  Butt(1),
  Round(2),
  Square(3);

  fun toStrokeCap(): StrokeCap =
    when (this) {
      Butt -> StrokeCap.Butt
      Round -> StrokeCap.Round
      Square -> StrokeCap.Square
    }

  companion object {
    fun fromValueOrNull(value: Int): LineCap? = values().firstOrNull { it.value == value }
  }
}

internal object LineCapSerializer : KSerializer<LineCap> {
  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor("LineCap", PrimitiveKind.INT)

  override fun deserialize(decoder: Decoder): LineCap {
    return try {
      val jsonDecoder = decoder as? JsonDecoder
      if (jsonDecoder != null) {
        val element = jsonDecoder.decodeJsonElement()
        val intVal =
          element.jsonPrimitive.intOrNull ?: element.jsonPrimitive.floatOrNull?.toInt() ?: 2
        LineCap.fromValueOrNull(intVal) ?: LineCap.Round
      } else {
        val value = decoder.decodeInt()
        LineCap.fromValueOrNull(value) ?: LineCap.Round
      }
    } catch (e: Exception) {
      LineCap.Round
    }
  }

  override fun serialize(encoder: Encoder, value: LineCap) {
    encoder.encodeInt(value.value)
  }
}

@Serializable(with = LineJoinSerializer::class)
internal enum class LineJoin(val value: Int) {
  Miter(1),
  Round(2),
  Bevel(3);

  fun toStrokeJoin(): StrokeJoin =
    when (this) {
      Miter -> StrokeJoin.Miter
      Round -> StrokeJoin.Round
      Bevel -> StrokeJoin.Bevel
    }

  companion object {
    fun fromValueOrNull(value: Int): LineJoin? = values().firstOrNull { it.value == value }
  }
}

internal object LineJoinSerializer : KSerializer<LineJoin> {
  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor("LineJoin", PrimitiveKind.INT)

  override fun deserialize(decoder: Decoder): LineJoin {
    return try {
      val jsonDecoder = decoder as? JsonDecoder
      if (jsonDecoder != null) {
        val element = jsonDecoder.decodeJsonElement()
        val intVal =
          element.jsonPrimitive.intOrNull ?: element.jsonPrimitive.floatOrNull?.toInt() ?: 2
        LineJoin.fromValueOrNull(intVal) ?: LineJoin.Round
      } else {
        val value = decoder.decodeInt()
        LineJoin.fromValueOrNull(value) ?: LineJoin.Round
      }
    } catch (e: Exception) {
      LineJoin.Round
    }
  }

  override fun serialize(encoder: Encoder, value: LineJoin) {
    encoder.encodeInt(value.value)
  }
}

/** Gradient stroke style */
@Serializable
internal data class GradientStroke(
  @SerialName("nm") override val name: String? = "",
  @SerialName("hd") override val hidden: Boolean? = false,
  @SerialName("ty") override val type: ShapeType = ShapeType.GradientStroke,
  @SerialName("t") val gradientType: GradientType = GradientType.Linear,
  @SerialName("s")
  val startPoint: BasePositionProperty = StaticPositionProperty(value = listOf(0f, 0f)),
  @SerialName("e")
  val endPoint: BasePositionProperty = StaticPositionProperty(value = listOf(0f, 0f)),
  @SerialName("g")
  val gradientColors: BaseGradientProperty =
    StaticGradientProperty(value = GradientValue(numberOfColors = 0)),
  @SerialName("o") override val opacity: BaseScalarProperty = StaticScalarProperty(value = 100f),
  @SerialName("w") val strokeWidth: BaseScalarProperty = StaticScalarProperty(value = 1f),
  @SerialName("lc") val lineCap: LineCap = LineCap.Round,
  @SerialName("lj") val lineJoin: LineJoin = LineJoin.Round,
  @SerialName("ml") val miterLimit: Float? = 4f,
  @SerialName("h") val highlightLength: BaseScalarProperty? = null,
  @SerialName("a") val highlightAngle: BaseScalarProperty? = null,
) : ShapeStyle
