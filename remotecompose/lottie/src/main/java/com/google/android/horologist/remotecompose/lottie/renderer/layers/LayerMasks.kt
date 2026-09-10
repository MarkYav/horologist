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
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.ui.graphics.ClipOp
import com.google.android.horologist.remotecompose.lottie.LottieSettings
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.GraphicElement
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.Ellipse
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.Path
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.PolyStar
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.Rectangle
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.grouping.Group
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.grouping.Transform
import com.google.android.horologist.remotecompose.lottie.format.layer.MatteMode
import com.google.android.horologist.remotecompose.lottie.format.layer.ShapeLayer
import com.google.android.horologist.remotecompose.lottie.format.layer.SolidColorLayer
import com.google.android.horologist.remotecompose.lottie.format.mask.Mask
import com.google.android.horologist.remotecompose.lottie.format.mask.MaskMode
import com.google.android.horologist.remotecompose.lottie.renderer.buildRemotePathFromBezier
import com.google.android.horologist.remotecompose.lottie.renderer.inverseTransform
import com.google.android.horologist.remotecompose.lottie.renderer.properties.RemoteBezierValue
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animateBezierSubpaths
import com.google.android.horologist.remotecompose.lottie.renderer.shapes.ellipse
import com.google.android.horologist.remotecompose.lottie.renderer.shapes.path
import com.google.android.horologist.remotecompose.lottie.renderer.shapes.polyStar
import com.google.android.horologist.remotecompose.lottie.renderer.shapes.rectangle
import com.google.android.horologist.remotecompose.lottie.renderer.transform

/** Applies layer clipping masks defined in [Mask] properties. */
@SuppressLint("RestrictedApi")
internal fun applyLayerMasks(
  masks: List<Mask>,
  animationSettings: LottieSettings,
  canvas: RemoteCanvas,
) {
  val nonInvertedAddSubpaths = mutableListOf<RemoteBezierValue>()

  for (mask in masks) {
    if (mask.mode == MaskMode.None) continue
    val maskPath = mask.path ?: continue
    val bezierList = animateBezierSubpaths(maskPath, animationSettings)
    if (bezierList.isEmpty()) continue

    val inverted = mask.inverted.constantValue
    if (mask.mode == MaskMode.Add && !inverted) {
      nonInvertedAddSubpaths.addAll(bezierList)
    } else {
      val rcPath = buildRemotePathFromBezier(bezierList)
      val clipOp =
        when (mask.mode) {
          MaskMode.Subtract -> if (inverted) ClipOp.Intersect else ClipOp.Difference
          MaskMode.Add -> ClipOp.Difference
          MaskMode.Intersect -> if (inverted) ClipOp.Difference else ClipOp.Intersect
          else -> continue
        }
      canvas.clipPath(rcPath, clipOp)
    }
  }

  if (nonInvertedAddSubpaths.isNotEmpty()) {
    val compositeAddPath = buildRemotePathFromBezier(nonInvertedAddSubpaths)
    canvas.clipPath(compositeAddPath, ClipOp.Intersect)
  }
}

/** Applies track matte clipping from an adjacent or targeted layer. */
@SuppressLint("RestrictedApi")
internal fun applyMatteClip(
  matteContext: MatteContext,
  animationSettings: LottieSettings,
  canvas: RemoteCanvas,
) {
  val matteLayer = matteContext.matteLayer
  val matteTransform = matteLayer.transform
  val layerTransforms =
    if (matteTransform != null) {
      matteContext.matteTransforms + matteTransform
    } else {
      matteContext.matteTransforms
    }

  for (t in layerTransforms) {
    transform(t, null, animationSettings, canvas)
  }

  val clipOp =
    if (
      matteContext.matteMode == MatteMode.InvertedAlpha ||
        matteContext.matteMode == MatteMode.InvertedLuma
    ) {
      ClipOp.Difference
    } else {
      ClipOp.Intersect
    }

  when (matteLayer) {
    is ShapeLayer -> clipShapes(matteLayer.shapes, animationSettings, canvas, clipOp)
    is SolidColorLayer -> {
      val rcPath = RemotePath()
      rcPath.reset()
      rcPath.moveTo(0f, 0f)
      val w = matteLayer.solidWidth.constantValue.toFloat()
      val h = matteLayer.solidHeight.constantValue.toFloat()
      rcPath.lineTo(w, 0f)
      rcPath.lineTo(w, h)
      rcPath.lineTo(0f, h)
      rcPath.close()
      canvas.clipPath(rcPath, clipOp)
    }
    else -> {}
  }

  for (t in layerTransforms.reversed()) {
    inverseTransform(t, animationSettings, canvas)
  }
}

@SuppressLint("RestrictedApi")
private fun clipShapes(
  shapes: List<GraphicElement>,
  animationSettings: LottieSettings,
  canvas: RemoteCanvas,
  clipOp: ClipOp = ClipOp.Intersect,
) {
  for (shape in shapes) {
    if (shape.hidden?.constantValueOrNull == true) continue
    when (shape) {
      is Rectangle -> {
        val s = rectangle(shape, animationSettings)
        if (s != null) {
          canvas.clipPath(s.toRemotePath(), clipOp)
        }
      }
      is Path -> {
        val s = path(shape, animationSettings)
        if (s != null) {
          canvas.clipPath(s.toRemotePath(), clipOp)
        }
      }
      is Ellipse -> {
        val s = ellipse(shape, animationSettings)
        if (s != null) {
          canvas.clipPath(s.toRemotePath(), clipOp)
        }
      }
      is PolyStar -> {
        val s = polyStar(shape, animationSettings)
        if (s != null) {
          canvas.clipPath(s.toRemotePath(), clipOp)
        }
      }
      is Group -> {
        val groupTransform = shape.shapes.filterIsInstance<Transform>().firstOrNull()
        if (groupTransform != null) {
          transform(groupTransform, null, animationSettings, canvas)
          clipShapes(shape.shapes.filter { it !is Transform }, animationSettings, canvas, clipOp)
          inverseTransform(groupTransform, animationSettings, canvas)
        } else {
          clipShapes(shape.shapes, animationSettings, canvas, clipOp)
        }
      }
      else -> {}
    }
  }
}
