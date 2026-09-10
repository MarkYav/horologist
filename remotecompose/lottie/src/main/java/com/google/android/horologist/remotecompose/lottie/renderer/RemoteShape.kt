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
import androidx.compose.remote.creation.RemotePath
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteDrawScope
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.remotePath
import androidx.compose.remote.creation.compose.state.rf
import com.google.android.horologist.remotecompose.lottie.LottieSettings
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.grouping.Transform
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.FillRule
import com.google.android.horologist.remotecompose.lottie.renderer.properties.RemoteBezierValue
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animateScalar

/** Represents an evaluated vector shape ready for canvas rendering. */
@SuppressLint("RestrictedApi")
internal interface EvaluatedGeometry {
  fun draw(drawScope: RemoteDrawScope, canvas: RemoteCanvas)

  fun withFillRule(fillRule: FillRule): EvaluatedGeometry
}

@SuppressLint("RestrictedApi")
internal interface RemoteShape : EvaluatedGeometry {
  override fun draw(drawScope: RemoteDrawScope, canvas: RemoteCanvas) {
    draw(drawScope, canvas, 1f.rf)
  }

  fun draw(drawScope: RemoteDrawScope, canvas: RemoteCanvas, inheritedOpacity: RemoteFloat = 1f.rf)

  override fun withFillRule(fillRule: FillRule): RemoteShape = this

  fun toRemotePath(): RemotePath
}

@SuppressLint("RestrictedApi")
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
internal fun RemoteCanvas.drawPathWithFillRule(
  path: RemotePath,
  fillRule: FillRule,
  paint: androidx.compose.remote.creation.compose.state.RemotePaint? = null,
) {
  val winding = if (fillRule == FillRule.EvenOdd) 1 else 0
  val op =
    internalCanvas.recordRenderingOp(paint) {
      val pathId = document.addPathData(path, winding)
      document.drawPath(pathId)
    }
  internalCanvas.buffer.addRoots(op, path)
}

/** Pre-compiled static path when no vertices or dimensions are animated. */
@SuppressLint("RestrictedApi")
internal class RemoteCompiledGeometry(
  val path: RemotePath,
  val fillRule: FillRule = FillRule.NonZero,
  val bezierSubpaths: List<RemoteBezierValue>? = null,
) : RemoteShape {
  override fun draw(
    drawScope: RemoteDrawScope,
    canvas: RemoteCanvas,
    inheritedOpacity: RemoteFloat,
  ) {
    canvas.drawPathWithFillRule(path, fillRule)
  }

  override fun withFillRule(fillRule: FillRule): RemoteCompiledGeometry =
    if (this.fillRule == fillRule) this else RemoteCompiledGeometry(path, fillRule, bezierSubpaths)

  override fun toRemotePath(): RemotePath = path
}

/** Dynamic Bézier spline when vertices or control points are animated via expressions. */
@SuppressLint("RestrictedApi")
internal class RemoteDynamicGeometry(
  val path: List<RemoteBezierValue>,
  val fillRule: FillRule = FillRule.NonZero,
) : RemoteShape {
  constructor(
    singlePath: RemoteBezierValue,
    fillRule: FillRule = FillRule.NonZero,
  ) : this(listOf(singlePath), fillRule)

  override fun toRemotePath(): RemotePath = buildRemotePathFromBezier(path)

  override fun draw(
    drawScope: RemoteDrawScope,
    canvas: RemoteCanvas,
    inheritedOpacity: RemoteFloat,
  ) {
    if (path.isEmpty()) return

    val rcPath = drawScope.remotePath {
      for (subpath in path) {
        val vertices = subpath.vertices
        val inTangents = subpath.inTangents
        val outTangents = subpath.outTangents

        if (vertices.isEmpty()) continue

        val startX = vertices[0].x
        val startY = vertices[0].y
        moveTo(startX, startY)

        val maxIndex = if (subpath.closed) vertices.size else vertices.size - 1
        for (i in 0 until maxIndex) {
          val p0 = vertices[i]
          val lastIndex = if (i == vertices.size - 1 && subpath.closed) 0 else i + 1
          val p4 = vertices[lastIndex]
          val inTangent = inTangents.getOrNull(lastIndex)
          val outTangent = outTangents.getOrNull(i)

          val inTangentX = inTangent?.x ?: 0f.rf
          val inTangentY = inTangent?.y ?: 0f.rf
          val outTangentX = outTangent?.x ?: 0f.rf
          val outTangentY = outTangent?.y ?: 0f.rf

          if (
            inTangentX.constantValueOrNull == 0f &&
              inTangentY.constantValueOrNull == 0f &&
              outTangentX.constantValueOrNull == 0f &&
              outTangentY.constantValueOrNull == 0f
          ) {
            lineTo(p4.x, p4.y)
          } else {
            val p1x = p0.x + outTangentX
            val p1y = p0.y + outTangentY
            val p2x = p4.x + inTangentX
            val p2y = p4.y + inTangentY

            curveTo(p1x, p1y, p2x, p2y, p4.x, p4.y)
          }
        }

        if (subpath.closed) {
          close()
        }
      }
    }

    canvas.drawPathWithFillRule(rcPath, fillRule)
  }

  override fun withFillRule(fillRule: FillRule): RemoteDynamicGeometry =
    if (this.fillRule == fillRule) this else RemoteDynamicGeometry(path, fillRule)
}

/** Builds a [RemotePath] from a list of [RemoteBezierValue] subpaths. */
@SuppressLint("RestrictedApi")
internal fun buildRemotePathFromBezier(path: List<RemoteBezierValue>): RemotePath {
  val rcPath = RemotePath()
  rcPath.reset()
  if (path.isEmpty()) return rcPath
  for (subpath in path) {
    val vertices = subpath.vertices
    val inTangents = subpath.inTangents
    val outTangents = subpath.outTangents

    if (vertices.isEmpty()) continue

    val startX = vertices[0].x.constantValueOrNull ?: 0f
    val startY = vertices[0].y.constantValueOrNull ?: 0f
    rcPath.moveTo(startX, startY)

    val maxIndex = if (subpath.closed) vertices.size else vertices.size - 1
    for (i in 0 until maxIndex) {
      val p0 = vertices[i]
      val lastIndex = if (i == vertices.size - 1 && subpath.closed) 0 else i + 1
      val p4 = vertices[lastIndex]
      val inTangent = inTangents.getOrNull(lastIndex)
      val outTangent = outTangents.getOrNull(i)

      val p0x = p0.x.constantValueOrNull ?: 0f
      val p0y = p0.y.constantValueOrNull ?: 0f
      val p4x = p4.x.constantValueOrNull ?: 0f
      val p4y = p4.y.constantValueOrNull ?: 0f

      val inTangentX = inTangent?.x?.constantValueOrNull ?: 0f
      val inTangentY = inTangent?.y?.constantValueOrNull ?: 0f
      val outTangentX = outTangent?.x?.constantValueOrNull ?: 0f
      val outTangentY = outTangent?.y?.constantValueOrNull ?: 0f

      if (inTangentX == 0f && inTangentY == 0f && outTangentX == 0f && outTangentY == 0f) {
        rcPath.lineTo(p4x, p4y)
      } else {
        val p1x = p0x + outTangentX
        val p1y = p0y + outTangentY
        val p2x = p4x + inTangentX
        val p2y = p4y + inTangentY

        rcPath.cubicTo(p1x, p1y, p2x, p2y, p4x, p4y)
      }
    }

    if (subpath.closed) {
      rcPath.close()
    }
  }
  return rcPath
}

internal typealias RemoteCompiledPath = RemoteCompiledGeometry

internal typealias RemoteLottiePath = RemoteDynamicGeometry

@SuppressLint("RestrictedApi")
internal class RemoteGroup(
  val childShapes: List<StyledShapes>,
  val animationSettings: LottieSettings,
  val transform: Transform?,
) : RemoteShape {
  override fun toRemotePath(): RemotePath = RemotePath()

  override fun draw(
    drawScope: RemoteDrawScope,
    canvas: RemoteCanvas,
    inheritedOpacity: RemoteFloat,
  ) {
    val groupOpacity =
      if (transform != null) {
        val o = animateScalar(transform.opacity, animationSettings)
        inheritedOpacity * (o / 100f)
      } else {
        inheritedOpacity
      }

    for (shapeGroup in childShapes) {
      canvas.save()

      if (transform != null) {
        transform(transform, null, animationSettings, canvas)
      }

      if (shapeGroup.style is NoopStyle) {
        for (shape in shapeGroup.shapes) {
          shape.draw(drawScope, canvas, groupOpacity)
        }
      } else {
        val paint = shapeGroup.style.getPaint(groupOpacity)
        drawScope.usePaint(paint) {
          for (shape in shapeGroup.shapes) {
            shape.draw(drawScope, canvas, groupOpacity)
          }
        }
      }

      canvas.restore()
    }
  }

  override fun withFillRule(fillRule: FillRule): RemoteGroup {
    val newChildShapes = childShapes.map { styledShapes ->
      StyledShapes(
        shapes = styledShapes.shapes.map { it.withFillRule(fillRule) },
        style = styledShapes.style,
      )
    }
    return RemoteGroup(newChildShapes, animationSettings, transform)
  }
}
