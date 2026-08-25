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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CompactButton
import androidx.wear.compose.material.CompactChip
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
import kotlinx.coroutines.delay

/** Lightweight metadata describing a Lottie animation showcase entry. */
internal data class LottieDemoItem(
  val title: String,
  val subtitle: String,
  val category: String,
  @param:RawRes val rawRes: Int,
)

/** Playback regime for the interactive detail player. */
internal enum class PlaybackRegime {
  TIME,
  CROWN,
}

/** Active navigation / presentation mode within the Lottie showcase screen. */
internal sealed interface LottieViewMode {
  data object Gallery : LottieViewMode

  data class Detail(val index: Int) : LottieViewMode

  data class Demo(val index: Int) : LottieViewMode
}

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

/** Scrollable gallery displaying all animations grouped with live preview cards. */
@Composable
private fun LottieGalleryList(
  catalog: List<LottieDemoItem>,
  onSelect: (Int) -> Unit,
  onStartDemo: () -> Unit,
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

      // Top-level kiosk demo mode button
      item {
        Chip(
          label = "Start Demo Mode",
          secondaryLabel = "Auto-cycle all ${catalog.size} animations",
          icon = {
            Box(
              modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFF332200)),
              contentAlignment = Alignment.Center,
            ) {
              Text("▶", fontSize = 16.sp, color = Color(0xFFFFCC00))
            }
          },
          onClick = onStartDemo,
          modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
        )
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
  var regime by remember { mutableStateOf(PlaybackRegime.TIME) }
  var isPlaying by remember(item.rawRes) { mutableStateOf(true) }
  var crownProgress by remember(item.rawRes) { mutableFloatStateOf(0f) }

  val focusRequester = remember { FocusRequester() }

  LaunchedEffect(regime) {
    if (regime == PlaybackRegime.CROWN) {
      focusRequester.requestFocus()
    }
  }

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
            modifier = Modifier.padding(bottom = 4.dp),
          )
        }
      }

      // Regime selector row (Time vs Crown)
      item {
        Row(
          modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          CompactChip(
            label = { Text("⏰ Time", fontSize = 11.sp) },
            onClick = { regime = PlaybackRegime.TIME },
            colors =
              if (regime == PlaybackRegime.TIME) {
                ChipDefaults.primaryChipColors()
              } else {
                ChipDefaults.secondaryChipColors()
              },
          )

          Spacer(modifier = Modifier.width(6.dp))

          CompactChip(
            label = { Text("⚙️ Crown", fontSize = 11.sp) },
            onClick = { regime = PlaybackRegime.CROWN },
            colors =
              if (regime == PlaybackRegime.CROWN) {
                ChipDefaults.primaryChipColors()
              } else {
                ChipDefaults.secondaryChipColors()
              },
          )
        }
      }

      // Large Live Animation Viewport (Keyed by rawRes and regime for instant reload)
      item {
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .then(
                if (regime == PlaybackRegime.CROWN) {
                  Modifier.focusRequester(focusRequester).focusable().onRotaryScrollEvent { event ->
                    val delta = event.verticalScrollPixels * 0.002f
                    crownProgress = (crownProgress + delta).coerceIn(0f, 1f)
                    true
                  }
                } else {
                  Modifier
                }
              ),
          contentAlignment = Alignment.Center,
        ) {
          Box(
            modifier =
              Modifier.size(130.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF121212)),
            contentAlignment = Alignment.Center,
          ) {
            key(item.rawRes, regime) {
              when (regime) {
                PlaybackRegime.TIME -> {
                  if (isPlaying) {
                    LottieAnimatedPreview(
                      animationResId = item.rawRes,
                      modifier = Modifier.size(118.dp),
                    )
                  } else {
                    LottiePreview(animationResId = item.rawRes, modifier = Modifier.size(118.dp))
                  }
                }
                PlaybackRegime.CROWN -> {
                  Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                  ) {
                    LottiePreview(
                      animationResId = item.rawRes,
                      progress = crownProgress,
                      modifier = Modifier.size(100.dp),
                    )
                    Text(
                      text = "${(crownProgress * 100).toInt()}%",
                      fontSize = 10.sp,
                      color = Color(0xFFAAAAAA),
                    )
                  }
                }
              }
            }
          }
        }
      }

      // Playback Controls Row
      item {
        Row(
          modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          // Previous button
          CompactButton(onClick = onPrevious, colors = ButtonDefaults.secondaryButtonColors()) {
            Text(text = "◀", fontSize = 12.sp)
          }

          Spacer(modifier = Modifier.width(8.dp))

          when (regime) {
            PlaybackRegime.TIME -> {
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
            }
            PlaybackRegime.CROWN -> {
              // Step buttons for touch / non-rotary fallback
              CompactButton(
                onClick = { crownProgress = (crownProgress - 0.1f).coerceIn(0f, 1f) },
                colors = ButtonDefaults.secondaryButtonColors(),
              ) {
                Text(text = "-10%", fontSize = 9.sp)
              }
              Spacer(modifier = Modifier.width(4.dp))
              CompactButton(
                onClick = { crownProgress = (crownProgress + 0.1f).coerceIn(0f, 1f) },
                colors = ButtonDefaults.secondaryButtonColors(),
              ) {
                Text(text = "+10%", fontSize = 9.sp)
              }
            }
          }

          Spacer(modifier = Modifier.width(8.dp))

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

/** Auto-cycling kiosk demo mode player presenting all catalog animations sequentially. */
@Composable
private fun LottieDemoModePlayer(
  catalog: List<LottieDemoItem>,
  initialIndex: Int,
  onClose: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var currentIndex by remember { mutableStateOf(initialIndex.coerceIn(0, catalog.lastIndex)) }
  val currentItem = catalog[currentIndex]

  // Auto-advance timer: 3 seconds per animation
  LaunchedEffect(currentIndex) {
    delay(3000L)
    currentIndex = (currentIndex + 1) % catalog.size
  }

  Box(
    modifier = modifier.fillMaxSize().background(Color.Black).clickable { onClose() },
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier = Modifier.fillMaxSize().padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Text(
        text = "DEMO • ${currentIndex + 1} / ${catalog.size}",
        style = MaterialTheme.typography.caption2,
        color = Color(0xFFFFCC00),
        textAlign = TextAlign.Center,
      )

      Text(
        text = currentItem.title,
        style = MaterialTheme.typography.title3,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )

      Spacer(modifier = Modifier.height(4.dp))

      Box(
        modifier =
          Modifier.size(120.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF121212)),
        contentAlignment = Alignment.Center,
      ) {
        key(currentItem.rawRes) {
          LottieAnimatedPreview(
            animationResId = currentItem.rawRes,
            modifier = Modifier.size(110.dp),
          )
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = "Tap anywhere to exit",
        style = MaterialTheme.typography.caption2,
        color = Color(0xFF666666),
        textAlign = TextAlign.Center,
      )
    }
  }
}
