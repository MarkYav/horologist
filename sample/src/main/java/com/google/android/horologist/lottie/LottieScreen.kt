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
import androidx.annotation.RawRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CompactButton
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScalingLazyColumnDefaults
import com.google.android.horologist.compose.layout.ScalingLazyColumnDefaults.padding
import com.google.android.horologist.compose.layout.ScreenScaffold
import com.google.android.horologist.compose.layout.rememberResponsiveColumnState
import com.google.android.horologist.compose.material.Chip
import com.google.android.horologist.compose.material.SecondaryTitle
import com.google.android.horologist.remotecompose.lottie.LottieAnimatedPreview
import com.google.android.horologist.remotecompose.lottie.LottiePreview
import com.google.android.horologist.sample.R

/** Lightweight metadata describing a Lottie animation showcase entry. */
internal data class LottieDemoItem(
  val title: String,
  val subtitle: String,
  val category: String,
  @param:RawRes val rawRes: Int,
)

/** Canonical catalog of all test and feature showcase animations in the sample app. */
internal val LottieDemoCatalog: List<LottieDemoItem> =
  listOf(
    // 1. Media & System Controls
    LottieDemoItem(
      title = "Geometry",
      subtitle = "Multi-shape morphing",
      category = "Media & Controls",
      rawRes = R.raw.geometry,
    ),
    LottieDemoItem(
      title = "Play / Pause",
      subtitle = "Playback state transition",
      category = "Media & Controls",
      rawRes = R.raw.play_pause,
    ),
    LottieDemoItem(
      title = "Next Track",
      subtitle = "Directional chevron motion",
      category = "Media & Controls",
      rawRes = R.raw.next,
    ),
    LottieDemoItem(
      title = "M3 Play / Pause",
      subtitle = "Material 3 expressive easing",
      category = "Media & Controls",
      rawRes = R.raw.m3_play_pause,
    ),
    LottieDemoItem(
      title = "M3 Next Track",
      subtitle = "Material 3 expressive arrow",
      category = "Media & Controls",
      rawRes = R.raw.m3_next,
    ),
    LottieDemoItem(
      title = "Volume Up",
      subtitle = "Speaker waves expanding",
      category = "Media & Controls",
      rawRes = R.raw.volume_up,
    ),
    LottieDemoItem(
      title = "Volume Down",
      subtitle = "Speaker waves contracting",
      category = "Media & Controls",
      rawRes = R.raw.volume_down,
    ),
    LottieDemoItem(
      title = "Mute to Unmute",
      subtitle = "Slash dismiss transition",
      category = "Media & Controls",
      rawRes = R.raw.mute_to_unmute,
    ),
    LottieDemoItem(
      title = "Unmute to Mute",
      subtitle = "Slash draw transition",
      category = "Media & Controls",
      rawRes = R.raw.unmute_to_mute,
    ),

    // 2. Parametric Shapes & Hierarchies
    LottieDemoItem(
      title = "PolyStar",
      subtitle = "5-point star with roundness",
      category = "Shapes & Hierarchies",
      rawRes = R.raw.polystar,
    ),
    LottieDemoItem(
      title = "Rect & Ellipse",
      subtitle = "Rounded rect and oval primitives",
      category = "Shapes & Hierarchies",
      rawRes = R.raw.rect_ellipse,
    ),
    LottieDemoItem(
      title = "Position Animated",
      subtitle = "Spatial Bézier 2D curve",
      category = "Shapes & Hierarchies",
      rawRes = R.raw.position_animated,
    ),
    LottieDemoItem(
      title = "Position Static",
      subtitle = "Absolute coordinate alignment",
      category = "Shapes & Hierarchies",
      rawRes = R.raw.position_static,
    ),
    LottieDemoItem(
      title = "Parent Chain",
      subtitle = "20-deep ancestor transform chain",
      category = "Shapes & Hierarchies",
      rawRes = R.raw.parent_chain,
    ),
    LottieDemoItem(
      title = "Grandparent",
      subtitle = "3-level nested square hierarchy",
      category = "Shapes & Hierarchies",
      rawRes = R.raw.grandparent,
    ),
    LottieDemoItem(
      title = "Transform Skew",
      subtitle = "2D skew and skew-axis shear",
      category = "Shapes & Hierarchies",
      rawRes = R.raw.transform_skew,
    ),

    // 3. Gradients, Strokes & Fills
    LottieDemoItem(
      title = "Linear Gradient Fill",
      subtitle = "3-color stop linear shader",
      category = "Gradients & Strokes",
      rawRes = R.raw.gradient_linear_fill,
    ),
    LottieDemoItem(
      title = "Radial Gradient Fill",
      subtitle = "Center-origin radial shader",
      category = "Gradients & Strokes",
      rawRes = R.raw.gradient_radial_fill,
    ),
    LottieDemoItem(
      title = "Gradient Stroke",
      subtitle = "Linear gradient on stroke line",
      category = "Gradients & Strokes",
      rawRes = R.raw.gradient_stroke,
    ),
    LottieDemoItem(
      title = "Stroke Dash Pattern",
      subtitle = "Animated dash & gap sequence",
      category = "Gradients & Strokes",
      rawRes = R.raw.stroke_dash_pattern,
    ),
    LottieDemoItem(
      title = "EvenOdd Fill Rule",
      subtitle = "Self-intersecting star cutout",
      category = "Gradients & Strokes",
      rawRes = R.raw.fill_rule_even_odd,
    ),

    // 4. Modifiers & Path Operations
    LottieDemoItem(
      title = "Trim Path Primitives",
      subtitle = "Parametric start/end trimming",
      category = "Modifiers & Paths",
      rawRes = R.raw.trim_path_primitives,
    ),
    LottieDemoItem(
      title = "Repeater Linear",
      subtitle = "5 copies with opacity fade",
      category = "Modifiers & Paths",
      rawRes = R.raw.repeater_linear_copies,
    ),
    LottieDemoItem(
      title = "Repeater Radial",
      subtitle = "60° rotational flower petals",
      category = "Modifiers & Paths",
      rawRes = R.raw.repeater_radial_distribution,
    ),
    LottieDemoItem(
      title = "Rounded Corners",
      subtitle = "Filleted star polygon vertices",
      category = "Modifiers & Paths",
      rawRes = R.raw.rounded_corners_star,
    ),
    LottieDemoItem(
      title = "Merge Paths",
      subtitle = "Boolean union of circles",
      category = "Modifiers & Paths",
      rawRes = R.raw.merge_paths_overlapping_circles,
    ),
  )

/**
 * Main Lottie showcase screen for the sample application.
 *
 * Supports dual-mode presentation:
 * 1. Gallery list view of all 26+ animations grouped by category.
 * 2. Interactive detail player with Play/Pause, stepping, and metadata.
 */
@Composable
fun LottieScreen(modifier: Modifier = Modifier) {
  var selectedIndex by remember { mutableStateOf<Int?>(null) }

  BackHandler(enabled = selectedIndex != null) { selectedIndex = null }

  val activeIndex = selectedIndex
  if (activeIndex != null && activeIndex in LottieDemoCatalog.indices) {
    LottieDetailPlayer(
      item = LottieDemoCatalog[activeIndex],
      currentIndex = activeIndex,
      totalCount = LottieDemoCatalog.size,
      onPrevious = {
        selectedIndex = if (activeIndex > 0) activeIndex - 1 else LottieDemoCatalog.lastIndex
      },
      onNext = {
        selectedIndex = if (activeIndex < LottieDemoCatalog.lastIndex) activeIndex + 1 else 0
      },
      onClose = { selectedIndex = null },
      modifier = modifier,
    )
  } else {
    LottieGalleryList(
      catalog = LottieDemoCatalog,
      onSelect = { index -> selectedIndex = index },
      modifier = modifier,
    )
  }
}

/** Scrollable gallery displaying all animations grouped with live preview cards. */
@Composable
private fun LottieGalleryList(
  catalog: List<LottieDemoItem>,
  onSelect: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  val columnState =
    rememberResponsiveColumnState(
      contentPadding =
        padding(
          first = ScalingLazyColumnDefaults.ItemType.Text,
          last = ScalingLazyColumnDefaults.ItemType.Chip,
        )
    )

  val groupedByCategory = remember(catalog) { catalog.groupBy { it.category } }

  ScreenScaffold(scrollState = columnState) {
    ScalingLazyColumn(columnState = columnState, modifier = modifier.fillMaxSize()) {
      item {
        ListHeader {
          Text(
            text = "Lottie Gallery (${catalog.size})",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.title3,
          )
        }
      }

      groupedByCategory.forEach { (category, items) ->
        item { SecondaryTitle(category) }
        items.forEach { item ->
          val itemIndex = catalog.indexOf(item)
          item { LottieCard(item = item, onClick = { onSelect(itemIndex) }) }
        }
      }
    }
  }
}

/** Card item rendering a live thumbnail preview and descriptive labels. */
@Composable
private fun LottieCard(item: LottieDemoItem, onClick: () -> Unit) {
  Chip(
    label = item.title,
    secondaryLabel = item.subtitle,
    icon = {
      Box(
        modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFF1E1E1E)),
        contentAlignment = Alignment.Center,
      ) {
        LottieAnimatedPreview(animationResId = item.rawRes, modifier = Modifier.size(32.dp))
      }
    },
    onClick = onClick,
    modifier = Modifier.fillMaxWidth(),
  )
}

/** Interactive detail view rendering full-size animation with playback controls. */
@Composable
private fun LottieDetailPlayer(
  item: LottieDemoItem,
  currentIndex: Int,
  totalCount: Int,
  onPrevious: () -> Unit,
  onNext: () -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var isPlaying by remember(item.rawRes) { mutableStateOf(true) }

  val columnState =
    rememberResponsiveColumnState(
      contentPadding =
        padding(
          first = ScalingLazyColumnDefaults.ItemType.Text,
          last = ScalingLazyColumnDefaults.ItemType.Chip,
        )
    )

  ScreenScaffold(scrollState = columnState) {
    ScalingLazyColumn(columnState = columnState, modifier = modifier.fillMaxSize()) {
      // Header: Counter and Category
      item {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
          Text(
            text = "${currentIndex + 1} / $totalCount • ${item.category}",
            style = MaterialTheme.typography.caption2,
            color = Color(0xFFAAAAAA),
            textAlign = TextAlign.Center,
          )
        }
      }

      // Title
      item {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
          Text(
            text = item.title,
            style = MaterialTheme.typography.title3,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      // Subtitle
      item {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
          Text(
            text = item.subtitle,
            style = MaterialTheme.typography.caption1,
            color = Color(0xFF888888),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp),
          )
        }
      }

      // Large Live Animation Viewport
      item {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
          Box(
            modifier =
              Modifier.size(130.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF121212)),
            contentAlignment = Alignment.Center,
          ) {
            if (isPlaying) {
              LottieAnimatedPreview(animationResId = item.rawRes, modifier = Modifier.size(118.dp))
            } else {
              LottiePreview(animationResId = item.rawRes, modifier = Modifier.size(118.dp))
            }
          }
        }
      }

      // Playback Controls Row
      item {
        Row(
          modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          // Previous button
          CompactButton(onClick = onPrevious, colors = ButtonDefaults.secondaryButtonColors()) {
            Text(text = "◀", fontSize = 12.sp)
          }

          Spacer(modifier = Modifier.width(10.dp))

          // Play / Pause toggle button
          Button(
            onClick = { isPlaying = !isPlaying },
            colors =
              if (isPlaying) {
                ButtonDefaults.primaryButtonColors()
              } else {
                ButtonDefaults.secondaryButtonColors()
              },
            modifier = Modifier.size(44.dp),
          ) {
            Text(text = if (isPlaying) "⏸" else "▶", fontSize = 16.sp)
          }

          Spacer(modifier = Modifier.width(10.dp))

          // Next button
          CompactButton(onClick = onNext, colors = ButtonDefaults.secondaryButtonColors()) {
            Text(text = "▶", fontSize = 12.sp)
          }
        }
      }

      // Return to List Button
      item {
        Spacer(modifier = Modifier.height(6.dp))
        Chip(label = "Back to Gallery", onClick = onClose, modifier = Modifier.fillMaxWidth())
      }
    }
  }
}
