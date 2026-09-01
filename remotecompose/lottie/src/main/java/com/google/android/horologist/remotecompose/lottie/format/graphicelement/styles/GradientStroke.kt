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

import com.google.android.horologist.remotecompose.lottie.format.graphicelement.ShapeType
import com.google.android.horologist.remotecompose.lottie.format.properties.BaseGradientProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.BasePositionProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.BaseScalarProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticGradientProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticPositionProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticScalarProperty
import com.google.android.horologist.remotecompose.lottie.format.values.GradientValue
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
  @SerialName("d") val dashes: List<StrokeDash>? = null,
  @SerialName("h") val highlightLength: BaseScalarProperty? = null,
  @SerialName("a") val highlightAngle: BaseScalarProperty? = null,
) : ShapeStyle
