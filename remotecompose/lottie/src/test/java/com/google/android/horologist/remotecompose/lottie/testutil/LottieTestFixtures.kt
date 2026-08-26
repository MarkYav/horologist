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

package com.google.android.horologist.remotecompose.lottie.testutil

import android.annotation.SuppressLint
import androidx.compose.remote.creation.compose.state.rf
import com.google.android.horologist.remotecompose.lottie.LottieSettings
import com.google.android.horologist.remotecompose.lottie.SlotMap
import com.google.common.truth.Truth.assertWithMessage

/** Builder DSL for constructing Lottie JSON string documents cleanly in tests. */
class LottieJsonBuilder {
  var version: String = "5.7.0"
  var frameRate: Float = 30f
  var inPoint: Float = 0f
  var outPoint: Float = 60f
  var width: Int = 100
  var height: Int = 100
  var name: String? = null
  private val layers = mutableListOf<String>()
  private val assets = mutableListOf<String>()

  fun addLayer(layerJson: String) {
    layers.add(layerJson.trim())
  }

  fun addAsset(assetJson: String) {
    assets.add(assetJson.trim())
  }

  fun shapeLayer(
    shapesJson: String,
    transformJson: String? = null,
    layerName: String = "ShapeLayer",
    inPoint: Float = this.inPoint,
    outPoint: Float = this.outPoint,
    index: Int = layers.size + 1,
  ) {
    addLayer(
      buildShapeLayerJson(
        shapesJson = shapesJson,
        transformJson = transformJson,
        layerName = layerName,
        inPoint = inPoint,
        outPoint = outPoint,
        index = index,
      )
    )
  }

  fun build(): String {
    val sb = StringBuilder()
    sb.append("{\n")
    sb.append("  \"v\": \"$version\",\n")
    sb.append("  \"fr\": $frameRate,\n")
    sb.append("  \"ip\": $inPoint,\n")
    sb.append("  \"op\": $outPoint,\n")
    sb.append("  \"w\": $width,\n")
    sb.append("  \"h\": $height")
    if (name != null) {
      sb.append(",\n  \"nm\": \"$name\"")
    }
    if (assets.isNotEmpty()) {
      sb.append(",\n  \"assets\": [\n    ${assets.joinToString(",\n    ")}\n  ]")
    }
    if (layers.isNotEmpty()) {
      sb.append(",\n  \"layers\": [\n    ${layers.joinToString(",\n    ")}\n  ]")
    } else {
      sb.append(",\n  \"layers\": []")
    }
    sb.append("\n}")
    return sb.toString()
  }
}

/** Builds a Lottie JSON document using a type-safe DSL block. */
fun buildLottieJson(builderAction: LottieJsonBuilder.() -> Unit): String {
  return LottieJsonBuilder().apply(builderAction).build()
}

/** Builds a single shape layer JSON snippet. */
fun buildShapeLayerJson(
  shapesJson: String,
  transformJson: String? = null,
  layerName: String = "ShapeLayer",
  inPoint: Float = 0f,
  outPoint: Float = 60f,
  index: Int = 1,
): String {
  val trimmedShapes = shapesJson.trim()
  val shapesSnippet =
    if (trimmedShapes.startsWith("[")) {
      trimmedShapes
    } else {
      "[\n      $trimmedShapes\n    ]"
    }
  val transformSnippet = transformJson?.let { ",\n    \"ks\": $it" } ?: ""
  return """
  {
    "ty": 4,
    "nm": "$layerName",
    "ind": $index,
    "ip": $inPoint,
    "op": $outPoint,
    "shapes": $shapesSnippet$transformSnippet
  }
  """
    .trimIndent()
}

/** Constructs a complete single-shape-layer Lottie JSON document string. */
fun singleShapeLayerJson(
  shapesJson: String,
  transformJson: String? = null,
  layerName: String = "ShapeLayer",
  inPoint: Float = 0f,
  outPoint: Float = 60f,
  width: Int = 100,
  height: Int = 100,
  frameRate: Float = 30f,
): String {
  return buildLottieJson {
    this.inPoint = inPoint
    this.outPoint = outPoint
    this.width = width
    this.height = height
    this.frameRate = frameRate
    shapeLayer(
      shapesJson = shapesJson,
      transformJson = transformJson,
      layerName = layerName,
      inPoint = inPoint,
      outPoint = outPoint,
    )
  }
}

/** Creates a [LottieSettings] initialized at the given [frame] and optional [slotMap]. */
@SuppressLint("RestrictedApi")
internal fun evalSettings(frame: Float, slotMap: SlotMap = SlotMap.Empty): LottieSettings =
  LottieSettings(frame.rf, slotMap)

/** Evaluates an animation property or expression at the given [frame]. */
@SuppressLint("RestrictedApi")
internal fun <T> evalAt(
  frame: Float,
  slotMap: SlotMap = SlotMap.Empty,
  evaluate: (LottieSettings) -> T,
): T = evaluate(evalSettings(frame, slotMap))

/** Asserts the timeline checkpoints evaluate to expected values across various frames. */
@SuppressLint("RestrictedApi")
internal fun <T> assertPropertyTimeline(
  vararg checkpoints: Pair<Float, T>,
  slotMap: SlotMap = SlotMap.Empty,
  evaluate: (LottieSettings) -> T,
) {
  for ((frame, expected) in checkpoints) {
    val actual = evaluate(evalSettings(frame, slotMap))
    assertWithMessage("Evaluation at frame $frame").that(actual).isEqualTo(expected)
  }
}

/** Asserts float timeline checkpoints evaluate within the specified tolerance. */
@SuppressLint("RestrictedApi")
internal fun assertFloatTimeline(
  vararg checkpoints: Pair<Float, Float>,
  tolerance: Float = 0.01f,
  slotMap: SlotMap = SlotMap.Empty,
  evaluate: (LottieSettings) -> Float?,
) {
  for ((frame, expected) in checkpoints) {
    val actual = evaluate(evalSettings(frame, slotMap))
    assertWithMessage("Evaluation at frame $frame").that(actual).isNotNull()
    assertWithMessage("Evaluation at frame $frame").that(actual!!).isWithin(tolerance).of(expected)
  }
}
