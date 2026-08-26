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

package com.google.android.horologist.remotecompose.lottie.renderer.shapes

import android.annotation.SuppressLint
import androidx.compose.remote.creation.compose.state.rf
import com.google.android.horologist.remotecompose.lottie.LottieSettings
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.grouping.Transform
import com.google.android.horologist.remotecompose.lottie.renderer.RemoteLottiePath
import com.google.android.horologist.remotecompose.lottie.renderer.RemoteShape
import com.google.android.horologist.remotecompose.lottie.renderer.math.RemoteAffineMatrix2D
import com.google.android.horologist.remotecompose.lottie.renderer.properties.RemoteBezierValue
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animatePosition
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animateScalar
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animateVector

/**
 * Transforms a [RemoteShape] geometry by a Lottie [Transform] definition into the parent coordinate
 * space.
 */
@SuppressLint("RestrictedApi")
internal fun transformRemoteShape(
  shape: RemoteShape,
  transform: Transform,
  animationSettings: LottieSettings,
): RemoteShape {
  return when (shape) {
    is RemoteLottiePath -> transformLottiePath(shape, transform, animationSettings)
    else -> shape
  }
}

/**
 * Transforms all subpaths of a [RemoteLottiePath] by applying anchor point, scale, skew, rotation,
 * and translation transformations directly to path vertices and control points using affine matrix
 * multiplication.
 */
@SuppressLint("RestrictedApi")
internal fun transformLottiePath(
  lottiePath: RemoteLottiePath,
  transform: Transform,
  animationSettings: LottieSettings,
): RemoteLottiePath {
  val transformedSubpaths =
    lottiePath.path.map { subpath -> transformBezierValue(subpath, transform, animationSettings) }
  return RemoteLottiePath(transformedSubpaths, lottiePath.fillRule)
}

/** Transforms a single [RemoteBezierValue] by a Lottie [Transform] using [RemoteAffineMatrix2D]. */
@SuppressLint("RestrictedApi")
internal fun transformBezierValue(
  subpath: RemoteBezierValue,
  transform: Transform,
  animationSettings: LottieSettings,
): RemoteBezierValue {
  val anchorPoint = animatePosition(transform.anchorPoint, animationSettings)
  val translation = animatePosition(transform.positionTranslation, animationSettings)
  val scale = animateVector(transform.scale, animationSettings)
  val scaleX = (scale.getOrNull(0) ?: 100f.rf) / 100f
  val scaleY = (scale.getOrNull(1) ?: 100f.rf) / 100f
  val rotation = animateScalar(transform.rotation, animationSettings)
  val skew = transform.skew?.let { animateScalar(it, animationSettings) }
  val skewAxis = transform.skewAxis?.let { animateScalar(it, animationSettings) }

  val matrix =
    RemoteAffineMatrix2D.buildLottieTransform(
      anchorX = anchorPoint.x,
      anchorY = anchorPoint.y,
      scaleX = scaleX,
      scaleY = scaleY,
      rotation = rotation,
      skew = skew,
      skewAxis = skewAxis,
      transX = translation.x,
      transY = translation.y,
    )

  val newVertices =
    subpath.vertices.map { point ->
      val (x, y) = matrix.mapPoint(point.getOrElse(0) { 0f.rf }, point.getOrElse(1) { 0f.rf })
      listOf(x, y)
    }

  val newInTangents =
    subpath.inTangents.map { tangent ->
      val (dx, dy) =
        matrix.mapVector(tangent.getOrElse(0) { 0f.rf }, tangent.getOrElse(1) { 0f.rf })
      listOf(dx, dy)
    }

  val newOutTangents =
    subpath.outTangents.map { tangent ->
      val (dx, dy) =
        matrix.mapVector(tangent.getOrElse(0) { 0f.rf }, tangent.getOrElse(1) { 0f.rf })
      listOf(dx, dy)
    }

  return RemoteBezierValue(
    closed = subpath.closed,
    inTangents = newInTangents,
    outTangents = newOutTangents,
    vertices = newVertices,
  )
}
