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

package com.google.android.horologist.lottie

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.google.android.horologist.lottie.composables.LottieDemoModePlayer
import com.google.android.horologist.lottie.composables.LottieDetailPlayer
import com.google.android.horologist.lottie.composables.LottieGalleryList

/**
 * Main Lottie showcase screen for the sample application.
 *
 * Supports tri-mode presentation:
 * 1. Gallery list view of all 26+ animations grouped by category with a top-level Demo button.
 * 2. Interactive detail player with dual regimes (Time vs Crown), Next/Prev, and metadata.
 * 3. Kiosk auto-cycling demo player cycling animations hands-free.
 */
@Composable
fun LottieScreen(modifier: Modifier = Modifier) {
  var viewMode by remember { mutableStateOf<LottieViewMode>(LottieViewMode.Gallery) }

  BackHandler(enabled = viewMode !is LottieViewMode.Gallery) { viewMode = LottieViewMode.Gallery }

  when (val mode = viewMode) {
    is LottieViewMode.Gallery -> {
      LottieGalleryList(
        catalog = LottieDemoCatalog,
        onSelect = { index -> viewMode = LottieViewMode.Detail(index) },
        onStartDemo = { viewMode = LottieViewMode.Demo(0) },
        modifier = modifier,
      )
    }
    is LottieViewMode.Detail -> {
      val activeIndex = mode.index.coerceIn(0, LottieDemoCatalog.lastIndex)
      LottieDetailPlayer(
        item = LottieDemoCatalog[activeIndex],
        currentIndex = activeIndex,
        totalCount = LottieDemoCatalog.size,
        onPrevious = {
          val prevIndex = if (activeIndex > 0) activeIndex - 1 else LottieDemoCatalog.lastIndex
          viewMode = LottieViewMode.Detail(prevIndex)
        },
        onNext = {
          val nextIndex = if (activeIndex < LottieDemoCatalog.lastIndex) activeIndex + 1 else 0
          viewMode = LottieViewMode.Detail(nextIndex)
        },
        onClose = { viewMode = LottieViewMode.Gallery },
        modifier = modifier,
      )
    }
    is LottieViewMode.Demo -> {
      LottieDemoModePlayer(
        catalog = LottieDemoCatalog,
        initialIndex = mode.index,
        onClose = { viewMode = LottieViewMode.Gallery },
        modifier = modifier,
      )
    }
  }
}
