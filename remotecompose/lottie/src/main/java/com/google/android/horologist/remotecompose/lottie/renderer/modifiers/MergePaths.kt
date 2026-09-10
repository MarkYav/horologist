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

package com.google.android.horologist.remotecompose.lottie.renderer.modifiers

import android.annotation.SuppressLint
import android.graphics.Path
import android.graphics.PathIterator
import android.graphics.PathMeasure
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.remote.creation.compose.state.rf
import com.google.android.horologist.remotecompose.lottie.LottieSettings
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.MergeMode
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.MergePaths
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.FillRule
import com.google.android.horologist.remotecompose.lottie.format.values.Point
import com.google.android.horologist.remotecompose.lottie.renderer.RemoteCompiledGeometry
import com.google.android.horologist.remotecompose.lottie.renderer.RemoteDynamicGeometry
import com.google.android.horologist.remotecompose.lottie.renderer.RemoteGroup
import com.google.android.horologist.remotecompose.lottie.renderer.RemoteShape
import com.google.android.horologist.remotecompose.lottie.renderer.properties.RemoteBezierValue
import kotlin.math.abs

/**
 * Evaluates a [MergePaths] modifier across a collection of [RemoteShape] geometries, performing
 * either contour concatenation (Merge) or boolean path operations (Add, Subtract, Intersect,
 * ExcludeIntersections).
 */
@SuppressLint("RestrictedApi")
internal fun evaluateMergePaths(
  shapes: List<RemoteShape>,
  mergePaths: MergePaths,
  animationSettings: LottieSettings,
): List<RemoteShape> {
  if (mergePaths.hidden?.constantValue == true || shapes.size <= 1) {
    return shapes
  }

  val fillRule =
    (shapes.firstOrNull() as? RemoteDynamicGeometry)?.fillRule
      ?: (shapes.firstOrNull() as? RemoteCompiledGeometry)?.fillRule
      ?: FillRule.NonZero

  return when (mergePaths.mode) {
    MergeMode.Merge -> {
      val allSubpaths = mutableListOf<RemoteBezierValue>()
      for (shape in shapes) {
        when (shape) {
          is RemoteDynamicGeometry -> allSubpaths.addAll(shape.path)
          is RemoteCompiledGeometry -> {
            if (shape.bezierSubpaths != null) {
              allSubpaths.addAll(shape.bezierSubpaths)
            } else {
              allSubpaths.addAll(shapeToBezierValues(shape))
            }
          }
          else -> allSubpaths.addAll(shapeToBezierValues(shape))
        }
      }
      listOf(RemoteDynamicGeometry(allSubpaths, fillRule = fillRule))
    }
    MergeMode.Add,
    MergeMode.Subtract,
    MergeMode.Intersect,
    MergeMode.ExcludeIntersections -> {
      val op =
        when (mergePaths.mode) {
          MergeMode.Add -> Path.Op.UNION
          MergeMode.Subtract -> Path.Op.DIFFERENCE
          MergeMode.Intersect -> Path.Op.INTERSECT
          MergeMode.ExcludeIntersections -> Path.Op.XOR
          else -> Path.Op.UNION
        }

      val resultPath = Path(shapeToAndroidPath(shapes[0]))
      for (i in 1 until shapes.size) {
        val nextPath = shapeToAndroidPath(shapes[i])
        resultPath.op(nextPath, op)
      }

      val subpaths = androidPathToBezierValues(resultPath)
      listOf(RemoteDynamicGeometry(subpaths, fillRule = fillRule))
    }
  }
}

@SuppressLint("RestrictedApi")
internal fun shapeToAndroidPath(shape: RemoteShape): Path {
  return when (shape) {
    is RemoteDynamicGeometry -> buildAndroidPathFromBezier(shape.path)
    is RemoteCompiledGeometry -> {
      if (shape.bezierSubpaths != null) {
        buildAndroidPathFromBezier(shape.bezierSubpaths)
      } else {
        Path()
      }
    }
    is RemoteGroup -> {
      val path = Path()
      for (group in shape.childShapes) {
        for (child in group.shapes) {
          path.addPath(shapeToAndroidPath(child))
        }
      }
      path
    }
    else -> Path()
  }
}

@SuppressLint("RestrictedApi")
internal fun shapeToBezierValues(shape: RemoteShape): List<RemoteBezierValue> {
  return when (shape) {
    is RemoteDynamicGeometry -> shape.path
    is RemoteCompiledGeometry -> {
      if (shape.bezierSubpaths != null) {
        shape.bezierSubpaths
      } else {
        val androidPath = shapeToAndroidPath(shape)
        androidPathToBezierValues(androidPath)
      }
    }
    is RemoteGroup -> {
      val subpaths = mutableListOf<RemoteBezierValue>()
      for (group in shape.childShapes) {
        for (child in group.shapes) {
          subpaths.addAll(shapeToBezierValues(child))
        }
      }
      subpaths
    }
    else -> {
      val androidPath = shapeToAndroidPath(shape)
      androidPathToBezierValues(androidPath)
    }
  }
}

@SuppressLint("RestrictedApi")
internal fun buildAndroidPathFromBezier(path: List<RemoteBezierValue>): Path {
  val androidPath = Path()
  if (path.isEmpty()) return androidPath
  for (subpath in path) {
    val vertices = subpath.vertices
    val inTangents = subpath.inTangents
    val outTangents = subpath.outTangents

    if (vertices.isEmpty()) continue

    val startX = vertices[0].x.constantValueOrNull ?: 0f
    val startY = vertices[0].y.constantValueOrNull ?: 0f
    androidPath.moveTo(startX, startY)

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

      val p1x = p0x + outTangentX
      val p1y = p0y + outTangentY
      val p2x = p4x + inTangentX
      val p2y = p4y + inTangentY

      androidPath.cubicTo(p1x, p1y, p2x, p2y, p4x, p4y)
    }

    if (subpath.closed) {
      androidPath.close()
    }
  }
  return androidPath
}

@SuppressLint("RestrictedApi")
internal fun androidPathToBezierValues(path: Path): List<RemoteBezierValue> {
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
    try {
      val subpaths = extractBezierViaPathIterator(path)
      if (subpaths.isNotEmpty()) {
        return subpaths
      }
    } catch (_: Throwable) {}
  }

  return extractBezierViaPathMeasure(path)
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@SuppressLint("RestrictedApi")
private fun extractBezierViaPathIterator(path: Path): List<RemoteBezierValue> {
  val subpaths = mutableListOf<RemoteBezierValue>()
  val iterator = path.pathIterator
  if (!iterator.hasNext()) return emptyList()

  val points = FloatArray(8)
  var currentVertices = mutableListOf<Point>()
  var currentInTangents = mutableListOf<Point>()
  var currentOutTangents = mutableListOf<Point>()
  var currentPoint = Vec2(0f, 0f)
  var startPoint = Vec2(0f, 0f)

  while (iterator.hasNext()) {
    val type = iterator.next(points, 0)
    when (type) {
      PathIterator.VERB_MOVE -> {
        if (currentVertices.isNotEmpty()) {
          subpaths.add(
            RemoteBezierValue(
              closed = false,
              vertices = currentVertices,
              inTangents = currentInTangents,
              outTangents = currentOutTangents,
            )
          )
          currentVertices = mutableListOf()
          currentInTangents = mutableListOf()
          currentOutTangents = mutableListOf()
        }
        val x = points[0]
        val y = points[1]
        currentPoint = Vec2(x, y)
        startPoint = Vec2(x, y)
        currentVertices.add(Point(x.rf, y.rf))
        currentInTangents.add(Point(0f.rf, 0f.rf))
        currentOutTangents.add(Point(0f.rf, 0f.rf))
      }
      PathIterator.VERB_LINE -> {
        val x = points[0]
        val y = points[1]
        currentPoint = Vec2(x, y)
        currentVertices.add(Point(x.rf, y.rf))
        currentInTangents.add(Point(0f.rf, 0f.rf))
        currentOutTangents.add(Point(0f.rf, 0f.rf))
      }
      PathIterator.VERB_QUAD -> {
        val x1 = points[0]
        val y1 = points[1]
        val x2 = points[2]
        val y2 = points[3]
        val outX = (2f / 3f) * (x1 - currentPoint.x)
        val outY = (2f / 3f) * (y1 - currentPoint.y)
        val inX = (2f / 3f) * (x1 - x2)
        val inY = (2f / 3f) * (y1 - y2)

        if (currentOutTangents.isNotEmpty()) {
          currentOutTangents[currentOutTangents.size - 1] = Point(outX.rf, outY.rf)
        }
        currentPoint = Vec2(x2, y2)
        currentVertices.add(Point(x2.rf, y2.rf))
        currentInTangents.add(Point(inX.rf, inY.rf))
        currentOutTangents.add(Point(0f.rf, 0f.rf))
      }
      PathIterator.VERB_CONIC -> {
        val x1 = points[0]
        val y1 = points[1]
        val x2 = points[2]
        val y2 = points[3]
        val outX = (2f / 3f) * (x1 - currentPoint.x)
        val outY = (2f / 3f) * (y1 - currentPoint.y)
        val inX = (2f / 3f) * (x1 - x2)
        val inY = (2f / 3f) * (y1 - y2)

        if (currentOutTangents.isNotEmpty()) {
          currentOutTangents[currentOutTangents.size - 1] = Point(outX.rf, outY.rf)
        }
        currentPoint = Vec2(x2, y2)
        currentVertices.add(Point(x2.rf, y2.rf))
        currentInTangents.add(Point(inX.rf, inY.rf))
        currentOutTangents.add(Point(0f.rf, 0f.rf))
      }
      PathIterator.VERB_CUBIC -> {
        val x1 = points[0]
        val y1 = points[1]
        val x2 = points[2]
        val y2 = points[3]
        val x3 = points[4]
        val y3 = points[5]

        val outX = x1 - currentPoint.x
        val outY = y1 - currentPoint.y
        val inX = x2 - x3
        val inY = y2 - y3

        if (currentOutTangents.isNotEmpty()) {
          currentOutTangents[currentOutTangents.size - 1] = Point(outX.rf, outY.rf)
        }
        currentPoint = Vec2(x3, y3)
        currentVertices.add(Point(x3.rf, y3.rf))
        currentInTangents.add(Point(inX.rf, inY.rf))
        currentOutTangents.add(Point(0f.rf, 0f.rf))
      }
      PathIterator.VERB_CLOSE -> {
        if (currentVertices.isNotEmpty()) {
          val lastV = currentVertices.last()
          val lastX = lastV.x.constantValueOrNull ?: 0f
          val lastY = lastV.y.constantValueOrNull ?: 0f
          val isSame = abs(lastX - startPoint.x) < 0.001f && abs(lastY - startPoint.y) < 0.001f
          if (isSame && currentVertices.size > 1) {
            val lastInTan = currentInTangents.last()
            currentVertices.removeAt(currentVertices.size - 1)
            currentInTangents.removeAt(currentInTangents.size - 1)
            currentOutTangents.removeAt(currentOutTangents.size - 1)
            currentInTangents[0] = lastInTan
          }
          subpaths.add(
            RemoteBezierValue(
              closed = true,
              vertices = currentVertices,
              inTangents = currentInTangents,
              outTangents = currentOutTangents,
            )
          )
          currentVertices = mutableListOf()
          currentInTangents = mutableListOf()
          currentOutTangents = mutableListOf()
        }
      }
      PathIterator.VERB_DONE -> break
    }
  }
  if (currentVertices.isNotEmpty()) {
    subpaths.add(
      RemoteBezierValue(
        closed = false,
        vertices = currentVertices,
        inTangents = currentInTangents,
        outTangents = currentOutTangents,
      )
    )
  }
  return subpaths
}

@SuppressLint("RestrictedApi")
private fun extractBezierViaPathMeasure(path: Path): List<RemoteBezierValue> {
  val subpaths = mutableListOf<RemoteBezierValue>()
  val pm = PathMeasure(path, false)
  var hasContour = true
  while (hasContour) {
    val length = pm.length
    if (length > 0f) {
      val segmentPath = Path()
      pm.getSegment(0f, length, segmentPath, true)
      val approx = segmentPath.approximate(0.5f)
      if (approx.size >= 6) {
        val vertices = mutableListOf<Point>()
        val inTangents = mutableListOf<Point>()
        val outTangents = mutableListOf<Point>()
        val pointCount = approx.size / 3
        val firstX = approx[1]
        val firstY = approx[2]
        val lastX = approx[approx.size - 2]
        val lastY = approx[approx.size - 1]
        val isClosed =
          pointCount > 1 && abs(firstX - lastX) < 0.001f && abs(firstY - lastY) < 0.001f
        val limit = if (isClosed) pointCount - 1 else pointCount

        for (i in 0 until limit) {
          val x = approx[i * 3 + 1]
          val y = approx[i * 3 + 2]
          vertices.add(Point(x.rf, y.rf))
          inTangents.add(Point(0f.rf, 0f.rf))
          outTangents.add(Point(0f.rf, 0f.rf))
        }
        subpaths.add(
          RemoteBezierValue(
            closed = pm.isClosed || isClosed,
            vertices = vertices,
            inTangents = inTangents,
            outTangents = outTangents,
          )
        )
      }
    }
    hasContour = pm.nextContour()
  }

  if (subpaths.isNotEmpty()) {
    return subpaths
  }

  return fallbackApproximatePath(path)
}

@SuppressLint("RestrictedApi")
private fun fallbackApproximatePath(path: Path): List<RemoteBezierValue> {
  val approx = path.approximate(0.5f)
  if (approx.size < 6) return emptyList()
  val vertices = mutableListOf<Point>()
  val inTangents = mutableListOf<Point>()
  val outTangents = mutableListOf<Point>()
  val pointCount = approx.size / 3
  val firstX = approx[1]
  val firstY = approx[2]
  val lastX = approx[approx.size - 2]
  val lastY = approx[approx.size - 1]
  val isClosed = pointCount > 1 && abs(firstX - lastX) < 0.001f && abs(firstY - lastY) < 0.001f
  val limit = if (isClosed) pointCount - 1 else pointCount

  for (i in 0 until limit) {
    val x = approx[i * 3 + 1]
    val y = approx[i * 3 + 2]
    vertices.add(Point(x.rf, y.rf))
    inTangents.add(Point(0f.rf, 0f.rf))
    outTangents.add(Point(0f.rf, 0f.rf))
  }
  return listOf(
    RemoteBezierValue(
      closed = isClosed,
      vertices = vertices,
      inTangents = inTangents,
      outTangents = outTangents,
    )
  )
}
