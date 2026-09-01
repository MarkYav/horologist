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
import androidx.compose.remote.creation.compose.state.selectIfLt
import androidx.compose.remote.creation.compose.state.sqrt
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.unit.LayoutDirection
import com.google.android.horologist.remotecompose.lottie.LottieSettings
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.FillRule
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.GradientType
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.LineCap
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.LineJoin
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.StrokeDash
import com.google.android.horologist.remotecompose.lottie.format.properties.AnimatedScalarProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.BaseScalarProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticScalarProperty
import com.google.android.horologist.remotecompose.lottie.renderer.properties.Point
import com.google.android.horologist.remotecompose.lottie.renderer.properties.RemoteGradientValue
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animateScalar

@SuppressLint("RestrictedApi")
internal object DefaultRemoteStateScope : RemoteStateScope {
  @get:SuppressLint("RestrictedApi")
  override val parentScope: RemoteStateScope
    get() = this

  override val layoutDirection: LayoutDirection = LayoutDirection.Ltr
  override val remoteDensity: RemoteDensity = RemoteDensity(1f.rf, 1f.rf)
}

internal interface RemoteStyle {
  fun getPaint(
    inheritedOpacity: RemoteFloat = 1f.rf,
    scope: RemoteStateScope = DefaultRemoteStateScope,
  ): RemotePaint
}

@SuppressLint("RestrictedApi")
internal class RemoteStyleWithOpacity(
  val baseStyle: RemoteStyle,
  val opacityMultiplier: RemoteFloat,
) : RemoteStyle {
  override fun getPaint(inheritedOpacity: RemoteFloat, scope: RemoteStateScope): RemotePaint {
    return baseStyle.getPaint(inheritedOpacity * opacityMultiplier, scope)
  }
}

@SuppressLint("RestrictedApi")
internal class RemoteFill(
  val fillColor: RemoteColor,
  val opacity: RemoteFloat = 100f.rf,
  val fillRule: FillRule = FillRule.NonZero,
) : RemoteStyle {
  override fun getPaint(inheritedOpacity: RemoteFloat, scope: RemoteStateScope): RemotePaint {
    return RemotePaint {
      val effectiveAlpha = fillColor.alpha * (opacity / 100f) * inheritedOpacity
      this.color = fillColor.copy(alpha = effectiveAlpha)
      style = PaintingStyle.Fill
    }
  }
}

@SuppressLint("RestrictedApi")
internal class RemoteStroke(
  val strokeColor: RemoteColor,
  val strokeWidth: RemoteFloat,
  val opacity: RemoteFloat,
  val lineCap: LineCap = LineCap.Round,
  val lineJoin: LineJoin = LineJoin.Round,
  val miterLimit: RemoteFloat? = null,
  val dashPattern: PathEffect? = null,
) : RemoteStyle {
  override fun getPaint(inheritedOpacity: RemoteFloat, scope: RemoteStateScope): RemotePaint {
    return RemotePaint {
      val baseAlpha = strokeColor.alpha * (opacity / 100f) * inheritedOpacity
      val effectiveAlpha = selectIfLt(this@RemoteStroke.strokeWidth, 0.001f.rf, 0f.rf, baseAlpha)
      this.color = strokeColor.copy(alpha = effectiveAlpha)
      this.style = PaintingStyle.Stroke
      this.strokeWidth = this@RemoteStroke.strokeWidth
      this.strokeCap =
        when (lineCap) {
          LineCap.Butt -> StrokeCap.Butt
          LineCap.Round -> StrokeCap.Round
          LineCap.Square -> StrokeCap.Square
        }
      this.strokeJoin =
        when (lineJoin) {
          LineJoin.Miter -> StrokeJoin.Miter
          LineJoin.Round -> StrokeJoin.Round
          LineJoin.Bevel -> StrokeJoin.Bevel
        }
      if (this@RemoteStroke.dashPattern != null) {
        this.pathEffect = this@RemoteStroke.dashPattern
      }
    }
  }
}

@SuppressLint("RestrictedApi")
internal class NoopStyle : RemoteStyle {
  override fun getPaint(inheritedOpacity: RemoteFloat, scope: RemoteStateScope): RemotePaint {
    return RemotePaint()
  }
}

@SuppressLint("RestrictedApi")
internal class RemoteGradientFill(
  val gradient: RemoteGradientValue,
  val startPoint: Point,
  val endPoint: Point,
  val gradientType: GradientType = GradientType.Linear,
  val opacity: RemoteFloat,
  val fillRule: FillRule = FillRule.NonZero,
) : RemoteStyle {
  override fun getPaint(inheritedOpacity: RemoteFloat, scope: RemoteStateScope): RemotePaint {
    val effectiveOpacity = (opacity / 100f) * inheritedOpacity
    val brush =
      createGradientShaderBrush(gradientType, startPoint, endPoint, gradient, effectiveOpacity)
    val size = (scope as? RemoteDrawScope)?.size ?: RemoteSize(0f.rf, 0f.rf)
    return RemotePaint {
      style = PaintingStyle.Fill
      with(brush) { scope.applyTo(this@RemotePaint, size) }
    }
  }
}

@SuppressLint("RestrictedApi")
internal class RemoteGradientStroke(
  val gradient: RemoteGradientValue,
  val startPoint: Point,
  val endPoint: Point,
  val gradientType: GradientType = GradientType.Linear,
  val opacity: RemoteFloat,
  val strokeWidth: RemoteFloat,
  val strokeCap: StrokeCap = StrokeCap.Round,
  val strokeJoin: StrokeJoin = StrokeJoin.Round,
  val miterLimit: Float = 4f,
  val dashPattern: PathEffect? = null,
) : RemoteStyle {
  override fun getPaint(inheritedOpacity: RemoteFloat, scope: RemoteStateScope): RemotePaint {
    val effectiveOpacity =
      selectIfLt(this@RemoteGradientStroke.strokeWidth, 0.001f.rf, 0f.rf, 1f.rf) * opacity
    val brush =
      createGradientShaderBrush(
        gradientType,
        startPoint,
        endPoint,
        gradient,
        effectiveOpacity = (effectiveOpacity / 100f) * inheritedOpacity,
      )
    val size = (scope as? RemoteDrawScope)?.size ?: RemoteSize(0f.rf, 0f.rf)
    return RemotePaint {
      style = PaintingStyle.Stroke
      this.strokeWidth = this@RemoteGradientStroke.strokeWidth
      this.strokeCap = this@RemoteGradientStroke.strokeCap
      this.strokeJoin = this@RemoteGradientStroke.strokeJoin
      if (this@RemoteGradientStroke.dashPattern != null) {
        this.pathEffect = this@RemoteGradientStroke.dashPattern
      }
      with(brush) { scope.applyTo(this@RemotePaint, size) }
    }
  }
}

@SuppressLint("RestrictedApi")
internal fun createDashPathEffect(
  dashes: List<StrokeDash>?,
  animationSettings: LottieSettings,
): PathEffect? {
  if (dashes.isNullOrEmpty()) return null

  val intervalsList = mutableListOf<Float>()
  var phase = 0f

  for (dash in dashes) {
    val property = dash.value ?: continue
    val value = resolveScalarFloat(property, animationSettings)
    val type = dash.dashType?.lowercase() ?: dash.name?.lowercase()
    if (type == "o" || type == "offset" || type?.startsWith("o") == true) {
      phase = value
    } else {
      intervalsList.add(value)
    }
  }

  if (intervalsList.isEmpty()) return null

  val finalIntervals =
    if (intervalsList.size % 2 != 0) {
      (intervalsList + intervalsList).toFloatArray()
    } else {
      intervalsList.toFloatArray()
    }

  if (finalIntervals.all { it == 0f }) return null

  return PathEffect.dashPathEffect(finalIntervals, phase)
}

@SuppressLint("RestrictedApi")
private fun resolveScalarFloat(
  property: BaseScalarProperty,
  animationSettings: LottieSettings,
): Float {
  val rf = animateScalar(property, animationSettings)
  val constVal = rf.constantValueOrNull
  if (constVal != null) return constVal
  return when (property) {
    is StaticScalarProperty -> property.value
    is AnimatedScalarProperty -> property.keyframes.firstOrNull()?.value ?: 0f
  }
}

@SuppressLint("RestrictedApi")
internal fun createGradientShaderBrush(
  gradientType: GradientType,
  startPoint: Point,
  endPoint: Point,
  gradient: RemoteGradientValue,
  effectiveOpacity: RemoteFloat = 1f.rf,
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
    val finalAlpha = alpha * effectiveOpacity
    val c = RemoteColor.rgb(r, g, b, finalAlpha)
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
      val finalAlpha = alpha * effectiveOpacity
      colors.add(RemoteColor.rgb(r, g, b, finalAlpha))
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
