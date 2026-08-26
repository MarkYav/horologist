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

import com.google.android.horologist.remotecompose.lottie.format.graphicelement.GraphicElement
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Container for the fonts list in a Lottie composition. */
@Serializable internal data class FontList(@SerialName("list") val list: List<Font> = emptyList())

/** A font definition in a Lottie composition. */
@Serializable
internal data class Font(
  @SerialName("fName") val name: String = "",
  @SerialName("fFamily") val family: String = "",
  @SerialName("fStyle") val style: String = "",
  @SerialName("ascent") val ascent: Float? = null,
  @SerialName("fPath") val path: String? = null,
  @SerialName("fWeight") val weight: String? = null,
  @SerialName("fClass") val fontClass: String? = null,
  @SerialName("fOrigin") val origin: Int? = null,
)

/** A vector character glyph definition in a Lottie composition. */
@Serializable
internal data class FontChar(
  @SerialName("ch") val character: String = "",
  @SerialName("fFamily") val family: String = "",
  @SerialName("style") val style: String = "",
  @SerialName("size") val size: Float = 0f,
  @SerialName("w") val width: Float = 0f,
  @SerialName("data") val shapeData: FontShapeData? = null,
)

/** Shape data container for a character glyph. */
@Serializable
internal data class FontShapeData(
  @SerialName("shapes") val shapes: List<GraphicElement> = emptyList()
)
