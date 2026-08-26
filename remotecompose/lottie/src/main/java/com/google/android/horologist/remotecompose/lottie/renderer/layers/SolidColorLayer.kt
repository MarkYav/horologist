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

package com.google.android.horologist.remotecompose.lottie.renderer.layers

import android.annotation.SuppressLint
import androidx.compose.remote.creation.RemotePath
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.grouping.Transform
import com.google.android.horologist.remotecompose.lottie.format.layer.SolidColorLayer

/** A Layer rendering a solid color rectangle. */
@SuppressLint("RestrictedApi")
@Composable
@RemoteComposable
internal fun SolidColorLayer(
  layer: SolidColorLayer,
  transformStack: List<Transform> = emptyList(),
  layerVisibility: RemoteFloat = 1f.rf,
) {
  if (layer.solidWidth <= 0f || layer.solidHeight <= 0f) {
    return
  }

  val color = parseHexColor(layer.solidColor)
  val path =
    RemotePath().apply {
      reset()
      moveTo(0f, 0f)
      lineTo(layer.solidWidth, 0f)
      lineTo(layer.solidWidth, layer.solidHeight)
      lineTo(0f, layer.solidHeight)
      close()
    }

  renderLayerShell(
    layer = layer,
    transformStack = transformStack,
    layerVisibility = layerVisibility,
  ) {
    val paint = RemotePaint { this.color = color.rc.copy(alpha = color.rc.alpha * layerOpacity) }
    usePaint(paint) { remoteCanvas.drawPath(path) }
  }
}

internal fun parseHexColor(colorStr: String): Color {
  return try {
    val clean = colorStr.trim().removePrefix("#")
    when (clean.length) {
      6 -> {
        val argb = (0xFF000000L or clean.toLong(16)).toInt()
        Color(argb)
      }
      8 -> {
        val argb = clean.toLong(16).toInt()
        Color(argb)
      }
      3 -> {
        val r = clean[0]
        val g = clean[1]
        val b = clean[2]
        val argb = (0xFF000000L or "$r$r$g$g$b$b".toLong(16)).toInt()
        Color(argb)
      }
      else -> {
        val formatted = if (colorStr.startsWith("#")) colorStr else "#$colorStr"
        Color(formatted.toColorInt())
      }
    }
  } catch (_: Exception) {
    Color.Transparent
  }
}
