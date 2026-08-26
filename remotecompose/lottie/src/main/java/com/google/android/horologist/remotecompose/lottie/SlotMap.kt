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

import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.ui.graphics.Color
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticColorProperty

/**
 * A mapping of slot IDs to values.
 *
 * Slots can be used to share values between properties, or to override values at runtime. For
 * example, a fill color can reference a slot ID, which can be resolved to a color provided by the
 * application to enable dynamic theming.
 */
class SlotMap(colors: Map<String, RemoteColor> = emptyMap()) {
  private val colorSlots: Map<String, StaticColorProperty> = colors.mapValues { (slotId, color) ->
    StaticColorProperty(slotId = slotId, value = color)
  }

  fun getColor(slotId: String): RemoteColor? {
    val prop = colorSlots[slotId] ?: return null
    return prop.value
  }

  operator fun get(slotId: String): RemoteColor? = getColor(slotId)

  operator fun plus(other: SlotMap): SlotMap {
    val merged = mutableMapOf<String, RemoteColor>()
    for ((k, v) in colorSlots) {
      merged[k] = v.value
    }
    for ((k, v) in other.colorSlots) {
      merged[k] = v.value
    }
    return SlotMap(merged)
  }

  operator fun plus(pair: Pair<String, RemoteColor>): SlotMap {
    val merged = mutableMapOf<String, RemoteColor>()
    for ((k, v) in colorSlots) {
      merged[k] = v.value
    }
    merged[pair.first] = pair.second
    return SlotMap(merged)
  }

  @JvmName("plusColor")
  operator fun plus(pair: Pair<String, Color>): SlotMap {
    return plus(pair.first to pair.second.rc)
  }

  fun isEmpty(): Boolean = colorSlots.isEmpty()

  fun isNotEmpty(): Boolean = colorSlots.isNotEmpty()

  val size: Int
    get() = colorSlots.size

  fun toMap(): Map<String, RemoteColor> = colorSlots.mapValues { it.value.value }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is SlotMap) return false
    if (colorSlots.keys != other.colorSlots.keys) return false
    for (k in colorSlots.keys) {
      val c1 = colorSlots[k]?.value
      val c2 = other.colorSlots[k]?.value
      if (c1 == c2) continue
      val v1 = c1?.constantValueOrNull
      val v2 = c2?.constantValueOrNull
      if (v1 != null && v2 != null && v1 == v2) continue
      return false
    }
    return true
  }

  override fun hashCode(): Int {
    var result = 0
    for ((k, v) in colorSlots) {
      result = 31 * result + k.hashCode()
      val cv = v.value.constantValueOrNull
      result = 31 * result + (cv?.hashCode() ?: v.value.hashCode())
    }
    return result
  }

  override fun toString(): String = "SlotMap(colors=$colorSlots)"

  companion object {
    val Empty: SlotMap = SlotMap(emptyMap())
  }
}

/** Builder for creating [SlotMap] instances via DSL. */
class SlotMapBuilder {
  private val colors = mutableMapOf<String, RemoteColor>()

  operator fun set(slotId: String, color: RemoteColor) {
    colors[slotId] = color
  }

  operator fun set(slotId: String, color: Color) {
    colors[slotId] = color.rc
  }

  operator fun get(slotId: String): RemoteColor? = colors[slotId]

  fun build(): SlotMap = SlotMap(colors)
}

/** Builds a [SlotMap] using the given DSL [builder] lambda. */
fun slotMap(builder: SlotMapBuilder.() -> Unit): SlotMap {
  return SlotMapBuilder().apply(builder).build()
}

/** Creates an empty [SlotMap]. */
fun slotMapOf(): SlotMap = SlotMap.Empty

/** Creates a [SlotMap] populated with the given slot ID to [RemoteColor] pairs. */
fun slotMapOf(vararg pairs: Pair<String, RemoteColor>): SlotMap {
  return SlotMap(pairs.toMap())
}

/** Creates a [SlotMap] populated with the given slot ID to Compose [Color] pairs. */
@JvmName("slotMapOfColors")
fun slotMapOf(vararg pairs: Pair<String, Color>): SlotMap {
  return SlotMap(pairs.associate { (k, v) -> k to v.rc })
}

/** Converts a [Map] of slot IDs to [RemoteColor] into a [SlotMap]. */
fun Map<String, RemoteColor>.toSlotMap(): SlotMap = SlotMap(this)

/** Converts a [Map] of slot IDs to Compose [Color] into a [SlotMap]. */
@JvmName("colorsToSlotMap")
fun Map<String, Color>.toSlotMap(): SlotMap = SlotMap(mapValues { it.value.rc })
