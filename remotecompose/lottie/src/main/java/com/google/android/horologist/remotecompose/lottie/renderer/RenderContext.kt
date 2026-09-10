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

import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.rf
import com.google.android.horologist.remotecompose.lottie.LottieSettings
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.grouping.Transform
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.RoundedCorners
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.TrimPath
import com.google.android.horologist.remotecompose.lottie.renderer.layers.MatteContext

/**
 * Immutable render state context passed down through the Lottie element hierarchy.
 *
 * Encapsulates timeline settings, compound layer/group opacity, transform stack, active shape
 * modifiers, and track matte clipping state.
 */
internal data class RenderContext(
  val settings: LottieSettings,
  val effectiveOpacity: RemoteFloat = 1f.rf,
  val transformStack: List<Transform> = emptyList(),
  val activeTrimPath: TrimPath? = null,
  val activeRoundedCorners: RoundedCorners? = null,
  val matteContext: MatteContext? = null,
) {
  fun withGroupTransform(groupTransform: Transform?, groupOpacity: RemoteFloat): RenderContext {
    val newStack = if (groupTransform != null) transformStack + groupTransform else transformStack
    return copy(transformStack = newStack, effectiveOpacity = effectiveOpacity * groupOpacity)
  }

  fun withModifiers(trim: TrimPath?, corners: RoundedCorners?): RenderContext =
    copy(
      activeTrimPath = trim ?: activeTrimPath,
      activeRoundedCorners = corners ?: activeRoundedCorners,
    )
}
