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

package com.google.android.horologist.remotecompose.lottie.renderer

import android.annotation.SuppressLint
import androidx.compose.remote.creation.compose.capture.RemoteDensity
import androidx.compose.remote.creation.compose.layout.RemoteDrawScope
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.shaders.RemoteBrush
import androidx.compose.remote.creation.compose.shaders.RemoteLinearGradient
import androidx.compose.remote.creation.compose.shaders.RemoteRadialGradient
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.RemoteStateScope
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.sqrt
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.unit.LayoutDirection
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.GradientType
import com.google.android.horologist.remotecompose.lottie.renderer.properties.Point
import com.google.android.horologist.remotecompose.lottie.renderer.properties.RemoteGradientValue

@SuppressLint("RestrictedApi")
internal object DefaultRemoteStateScope : RemoteStateScope {
  @get:SuppressLint("RestrictedApi")
  override val parentScope: RemoteStateScope
    get() = this

  override val layoutDirection: LayoutDirection = LayoutDirection.Ltr
  override val remoteDensity: RemoteDensity = RemoteDensity(1f.rf, 1f.rf)
}

internal interface RemoteStyle {
  fun getPaint(scope: RemoteStateScope = DefaultRemoteStateScope): RemotePaint
}

internal class RemoteFill(val fillColor: RemoteColor) : RemoteStyle {
  override fun getPaint(scope: RemoteStateScope): RemotePaint {
    return RemotePaint {
      style = PaintingStyle.Fill
      this.color = fillColor
    }
  }
}

internal class NoopStyle : RemoteStyle {
  override fun getPaint(scope: RemoteStateScope): RemotePaint {
    return RemotePaint()
  }
}

@SuppressLint("RestrictedApi")
internal class RemoteGradientFill(
  val gradientType: GradientType,
  val startPoint: Point,
  val endPoint: Point,
  val gradient: RemoteGradientValue,
  val opacity: RemoteFloat,
) : RemoteStyle {
  override fun getPaint(scope: RemoteStateScope): RemotePaint {
    val brush = createGradientShaderBrush(gradientType, startPoint, endPoint, gradient)
    val size = (scope as? RemoteDrawScope)?.size ?: RemoteSize(0f.rf, 0f.rf)
    return RemotePaint {
      style = PaintingStyle.Fill
      with(brush) { scope.applyTo(this@RemotePaint, size) }
    }
  }
}

@SuppressLint("RestrictedApi")
internal class RemoteGradientStroke(
  val gradientType: GradientType,
  val startPoint: Point,
  val endPoint: Point,
  val gradient: RemoteGradientValue,
  val opacity: RemoteFloat,
  val strokeWidth: RemoteFloat,
  val strokeCap: StrokeCap = StrokeCap.Round,
  val strokeJoin: StrokeJoin = StrokeJoin.Round,
  val miterLimit: Float = 4f,
) : RemoteStyle {
  override fun getPaint(scope: RemoteStateScope): RemotePaint {
    val brush = createGradientShaderBrush(gradientType, startPoint, endPoint, gradient)
    val size = (scope as? RemoteDrawScope)?.size ?: RemoteSize(0f.rf, 0f.rf)
    return RemotePaint {
      style = PaintingStyle.Stroke
      this.strokeWidth = this@RemoteGradientStroke.strokeWidth
      this.strokeCap = this@RemoteGradientStroke.strokeCap
      this.strokeJoin = this@RemoteGradientStroke.strokeJoin
      with(brush) { scope.applyTo(this@RemotePaint, size) }
    }
  }
}

@SuppressLint("RestrictedApi")
internal fun createGradientShaderBrush(
  gradientType: GradientType,
  startPoint: Point,
  endPoint: Point,
  gradient: RemoteGradientValue,
): RemoteBrush {
  val n = gradient.numberOfColors
  val values = gradient.values

  val colors = mutableListOf<RemoteColor>()
  val stops = mutableListOf<RemoteFloat>()

  if (n <= 0 || values.isEmpty()) {
    colors.add(Color.Transparent.rc)
    colors.add(Color.Transparent.rc)
    stops.add(0f.rf)
    stops.add(1f.rf)
  } else if (n == 1) {
    val r = values.getOrElse(1) { 0f.rf }
    val g = values.getOrElse(2) { 0f.rf }
    val b = values.getOrElse(3) { 0f.rf }
    val alpha = if (values.size >= 6) values[5] else 1f.rf
    val c = RemoteColor.rgb(r, g, b, alpha)
    colors.add(c)
    colors.add(c)
    stops.add(0f.rf)
    stops.add(1f.rf)
  } else {
    for (i in 0 until n) {
      val stop = values.getOrElse(i * 4) { 0f.rf }
      val r = values.getOrElse(i * 4 + 1) { 0f.rf }
      val g = values.getOrElse(i * 4 + 2) { 0f.rf }
      val b = values.getOrElse(i * 4 + 3) { 0f.rf }
      val alphaIndex = n * 4 + i * 2 + 1
      val alpha = if (alphaIndex < values.size) values[alphaIndex] else 1f.rf
      colors.add(RemoteColor.rgb(r, g, b, alpha))
      stops.add(stop)
    }
  }

  return when (gradientType) {
    GradientType.Linear -> {
      RemoteLinearGradient(
        colors = colors,
        stops = stops,
        start = RemoteOffset(startPoint.x, startPoint.y),
        end = RemoteOffset(endPoint.x, endPoint.y),
        tileMode = TileMode.Clamp,
      )
    }
    GradientType.Radial -> {
      val dx = endPoint.x - startPoint.x
      val dy = endPoint.y - startPoint.y
      val radius = sqrt(dx * dx + dy * dy)
      RemoteRadialGradient(
        colors = colors,
        stops = stops,
        center = RemoteOffset(startPoint.x, startPoint.y),
        radius = radius,
        tileMode = TileMode.Clamp,
      )
    }
  }
}
