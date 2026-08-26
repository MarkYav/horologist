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
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteDrawScope
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.runtime.Composable
import com.google.android.horologist.remotecompose.lottie.LocalAnimationSettings
import com.google.android.horologist.remotecompose.lottie.LottieSettings
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.grouping.Transform
import com.google.android.horologist.remotecompose.lottie.format.layer.Layer
import com.google.android.horologist.remotecompose.lottie.format.mask.Mask
import com.google.android.horologist.remotecompose.lottie.format.mask.MaskMode
import com.google.android.horologist.remotecompose.lottie.renderer.applyLayerMasks
import com.google.android.horologist.remotecompose.lottie.renderer.applyMatteClip
import com.google.android.horologist.remotecompose.lottie.renderer.inverseTransform
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animateScalar
import com.google.android.horologist.remotecompose.lottie.renderer.transform

/** Scope provided to the drawing block inside [renderLayerShell]. */
@SuppressLint("RestrictedApi")
internal class LayerRenderScope(
  val drawScope: RemoteDrawScope,
  val animationSettings: LottieSettings,
  val layerOpacity: RemoteFloat,
  val transformStack: List<Transform>,
) {
  val remoteCanvas: RemoteCanvas
    get() = drawScope.remoteCanvas

  inline fun usePaint(paint: RemotePaint, crossinline block: RemoteDrawScope.() -> Unit) {
    drawScope.usePaint(paint) { drawScope.block() }
  }
}

/** Computes the compounded layer opacity from the innermost transform and visibility. */
@SuppressLint("RestrictedApi")
internal fun computeLayerOpacity(
  transformStack: List<Transform>,
  layerVisibility: RemoteFloat,
  animationSettings: LottieSettings,
): RemoteFloat {
  val lastTransformOpacity =
    transformStack.lastOrNull()?.opacity?.let { animateScalar(it, animationSettings) / 100f }
      ?: 1f.rf
  return lastTransformOpacity * layerVisibility
}

/** Determines whether the given mask list contains at least one active clipping mask. */
internal fun hasActiveMasks(masks: List<Mask>): Boolean = masks.any {
  it.mode != MaskMode.None && it.path != null
}

/**
 * Higher-order composable shell that unifies canvas state management, coordinate transformations,
 * layer opacity calculation, mask clipping, and track matte pairing across all layer renderers.
 */
@SuppressLint("RestrictedApi")
@Composable
@RemoteComposable
internal inline fun renderLayerShell(
  layer: Layer,
  transformStack: List<Transform> = emptyList(),
  matteContext: MatteContext? = null,
  layerVisibility: RemoteFloat = 1f.rf,
  masks: List<Mask> = layer.masksProperties,
  crossinline renderContent: LayerRenderScope.() -> Unit,
) {
  if (layer.hidden == true) {
    return
  }

  val updatedTransformStack =
    if (layer.transform != null) transformStack + layer.transform!! else transformStack

  renderLayerShell(
    transformStack = updatedTransformStack,
    matteContext = matteContext,
    layerVisibility = layerVisibility,
    masks = masks,
    renderContent = renderContent,
  )
}

/** Overload of [renderLayerShell] operating directly on an accumulated [transformStack]. */
@SuppressLint("RestrictedApi")
@Composable
@RemoteComposable
internal inline fun renderLayerShell(
  transformStack: List<Transform>,
  matteContext: MatteContext? = null,
  layerVisibility: RemoteFloat = 1f.rf,
  masks: List<Mask> = emptyList(),
  crossinline renderContent: LayerRenderScope.() -> Unit,
) {
  val animationSettings = LocalAnimationSettings.current
  val layerOpacity = computeLayerOpacity(transformStack, layerVisibility, animationSettings)
  val hasMasks = hasActiveMasks(masks)
  val needsSave = matteContext != null || hasMasks

  RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) {
    if (needsSave) {
      remoteCanvas.save()
    }

    if (matteContext != null) {
      applyMatteClip(matteContext, animationSettings, remoteCanvas)
    }

    if (hasMasks) {
      for (t in transformStack) {
        transform(t, null, animationSettings, remoteCanvas)
      }
      applyLayerMasks(masks, animationSettings, remoteCanvas)
      for (t in transformStack.reversed()) {
        inverseTransform(t, animationSettings, remoteCanvas)
      }
    }

    for (t in transformStack) {
      remoteCanvas.save()
      transform(t, null, animationSettings, remoteCanvas)
    }

    val scope =
      LayerRenderScope(
        drawScope = this,
        animationSettings = animationSettings,
        layerOpacity = layerOpacity,
        transformStack = transformStack,
      )
    scope.renderContent()

    for (t in transformStack) {
      remoteCanvas.restore()
    }

    if (needsSave) {
      remoteCanvas.restore()
    }
  }
}
