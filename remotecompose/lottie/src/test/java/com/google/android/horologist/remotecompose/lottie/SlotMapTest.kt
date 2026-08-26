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

package com.google.android.horologist.remotecompose.lottie

import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SlotMapTest {

  @Test
  fun emptySlotMap() {
    val empty = SlotMap.Empty
    assertThat(empty.getColor("slot1")).isNull()
    assertThat(empty["slot1"]).isNull()
    assertThat(empty.isEmpty()).isTrue()
    assertThat(empty.isNotEmpty()).isFalse()
    assertThat(empty.size).isEqualTo(0)
  }

  @Test
  fun slotMapOfRemoteColors() {
    val map = slotMapOf("color_1" to Color.Red.rc, "color_2" to Color.Blue.rc)
    assertThat(map["color_1"]?.constantValueOrNull).isEqualTo(Color.Red)
    assertThat(map["color_2"]?.constantValueOrNull).isEqualTo(Color.Blue)
    assertThat(map.getColor("color_1")?.constantValueOrNull).isEqualTo(Color.Red)
    assertThat(map["unknown"]).isNull()
    assertThat(map.size).isEqualTo(2)
  }

  @Test
  fun slotMapOfComposeColors() {
    val map = slotMapOf("color_1" to Color.Green, "color_2" to Color.Yellow)
    assertThat(map["color_1"]?.constantValueOrNull).isEqualTo(Color.Green)
    assertThat(map["color_2"]?.constantValueOrNull).isEqualTo(Color.Yellow)
    assertThat(map["unknown"]).isNull()
  }

  @Test
  fun slotMapOfEmpty() {
    val map = slotMapOf()
    assertThat(map["key"]).isNull()
    assertThat(map.isEmpty()).isTrue()
  }

  @Test
  fun slotMapDslBuilder() {
    val map = slotMap {
      set("color_a", Color.Red)
      set("color_b", Color.Blue.rc)
      this["color_c"] = Color.Green
      this["color_d"] = Color.Yellow.rc
      assertThat(this["color_a"]?.constantValueOrNull).isEqualTo(Color.Red)
    }

    assertThat(map["color_a"]?.constantValueOrNull).isEqualTo(Color.Red)
    assertThat(map["color_b"]?.constantValueOrNull).isEqualTo(Color.Blue)
    assertThat(map["color_c"]?.constantValueOrNull).isEqualTo(Color.Green)
    assertThat(map["color_d"]?.constantValueOrNull).isEqualTo(Color.Yellow)
    assertThat(map["unknown"]).isNull()
  }

  @Test
  fun slotMapPlusOperator() {
    val map1 = slotMapOf("color_1" to Color.Red, "color_2" to Color.Blue)
    val map2 = slotMapOf("color_2" to Color.Green, "color_3" to Color.Yellow)

    val combined = map1 + map2
    assertThat(combined["color_1"]?.constantValueOrNull).isEqualTo(Color.Red)
    assertThat(combined["color_2"]?.constantValueOrNull)
      .isEqualTo(Color.Green) // Overridden by map2
    assertThat(combined["color_3"]?.constantValueOrNull).isEqualTo(Color.Yellow)
  }

  @Test
  fun slotMapPlusPair() {
    val map = slotMapOf("color_1" to Color.Red)
    val updatedCompose = map + ("color_2" to Color.Blue)
    val updatedRemote = updatedCompose + ("color_3" to Color.Green.rc)

    assertThat(updatedRemote["color_1"]?.constantValueOrNull).isEqualTo(Color.Red)
    assertThat(updatedRemote["color_2"]?.constantValueOrNull).isEqualTo(Color.Blue)
    assertThat(updatedRemote["color_3"]?.constantValueOrNull).isEqualTo(Color.Green)
  }

  @Test
  fun slotMapEquality() {
    val map1 = slotMapOf("k1" to Color.Red, "k2" to Color.Blue)
    val map2 = slotMap {
      this["k1"] = Color.Red.rc
      this["k2"] = Color.Blue
    }
    assertThat(map1).isEqualTo(map2)
    assertThat(map1.hashCode()).isEqualTo(map2.hashCode())
  }

  @Test
  fun slotMapExtensionConversions() {
    val remoteMap = mapOf("c1" to Color.Cyan.rc).toSlotMap()
    assertThat(remoteMap["c1"]?.constantValueOrNull).isEqualTo(Color.Cyan)

    val composeMap = mapOf("c2" to Color.Magenta).toSlotMap()
    assertThat(composeMap["c2"]?.constantValueOrNull).isEqualTo(Color.Magenta)
  }
}
