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

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.horologist.remotecompose.lottie.format.Animation
import com.google.android.horologist.remotecompose.lottie.format.layer.LayerType
import com.google.android.horologist.remotecompose.lottie.format.layer.ShapeLayer
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LottieTestFixturesTest {

  @Test
  fun buildLottieJson_withDefaults_producesValidLottieDocument() {
    val json = buildLottieJson { name = "TestDoc" }

    val animation = Animation.decodeFromString(json)
    assertThat(animation.name).isEqualTo("TestDoc")
    assertThat(animation.version).isEqualTo("5.7.0")
    assertThat(animation.frameRate).isEqualTo(30f)
    assertThat(animation.startFrame).isEqualTo(0f)
    assertThat(animation.endFrame).isEqualTo(60f)
    assertThat(animation.width).isEqualTo(100)
    assertThat(animation.height).isEqualTo(100)
    assertThat(animation.layers).isEmpty()
  }

  @Test
  fun singleShapeLayerJson_createsValidShapeLayerAnimation() {
    val rectShape =
      """
      {
        "ty": "rc",
        "nm": "MyRect",
        "p": { "k": [10.0, 20.0] },
        "s": { "k": [50.0, 50.0] },
        "r": { "k": 0.0 }
      }
      """
        .trimIndent()

    val json =
      singleShapeLayerJson(
        shapesJson = rectShape,
        layerName = "CustomShapeLayer",
        width = 200,
        height = 200,
      )

    val animation = Animation.decodeFromString(json)
    assertThat(animation.width).isEqualTo(200)
    assertThat(animation.height).isEqualTo(200)
    assertThat(animation.layers).hasSize(1)

    val layer = animation.layers[0] as ShapeLayer
    assertThat(layer.type).isEqualTo(LayerType.Shape)
    assertThat(layer.name).isEqualTo("CustomShapeLayer")
    assertThat(layer.shapes).hasSize(1)
    assertThat(layer.shapes[0].name).isEqualTo("MyRect")
  }

  @Test
  fun evalAt_evaluatesWithGivenFrame() {
    val result = evalAt(frame = 15f) { settings -> settings.currentFrame.constantValueOrNull }
    assertThat(result).isEqualTo(15f)
  }

  @Test
  fun assertPropertyTimeline_evaluatesAllCheckpoints() {
    val evaluatedFrames = mutableListOf<Float>()
    assertPropertyTimeline(0f to 0f, 10f to 20f, 20f to 40f) { settings ->
      val f = settings.currentFrame.constantValueOrNull ?: 0f
      evaluatedFrames.add(f)
      f * 2f
    }

    assertThat(evaluatedFrames).containsExactly(0f, 10f, 20f).inOrder()
  }

  @Test
  fun assertFloatTimeline_assertsWithinTolerance() {
    assertFloatTimeline(0f to 10.001f, 5f to 20.005f, 10f to 30.002f, tolerance = 0.01f) { settings
      ->
      val f = settings.currentFrame.constantValueOrNull ?: 0f
      10f + f * 2f
    }
  }
}
