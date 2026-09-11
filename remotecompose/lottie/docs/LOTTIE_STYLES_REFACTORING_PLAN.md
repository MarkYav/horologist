# Lottie AST Styles (`format/graphicelement/styles/`) Refactoring Plan

## 1. What: Scope & Objectives

This document specifies the refactoring plan for the shape style AST classes in `format/graphicelement/styles/` within the `:remotecompose:lottie` module.

The refactoring aligns style AST models strictly with the canonical [Lottie 1.0.1 JSON Schema](https://lottie.github.io/lottie-spec/1.0.1/lottie.schema.json) and [Lottie Shape Style Specification](https://lottie.github.io/lottie-spec/latest/specs/shapes/#shape-style), adhering to the architectural patterns established in `lottie-fix-AST-properties`.

### Key Objectives
1. Introduce the sealed category interface [ShapeStyle](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/styles/ShapeStyle.kt) implementing [GraphicElement](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/GraphicElement.kt), exposing common style opacity: `val opacity: BaseScalarProperty`.
2. Refactor [Fill.kt](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/styles/Fill.kt) to strictly match schema attributes (`nm`, `hd`, `ty`, `o`, `c`, `r`) without extraneous properties or unbacked default values.
3. Add canonical Lottie 1.0.1 style domain models strictly defined in the schema:
   - [Stroke.kt](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/styles/Stroke.kt): Solid stroke with width (`w`), color (`c`), line cap (`lc`), line join (`lj`), miter limits (`ml`, `ml2`), and dash patterns (`d`).
   - [GradientFill.kt](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/styles/GradientFill.kt): Linear and radial gradient fill using gradient stops (`g`), start point (`s`), end point (`e`), gradient type (`t`), fill rule (`r`), and highlight parameters (`h`, `a`).
   - [GradientStroke.kt](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/styles/GradientStroke.kt): Gradient stroke combining stroke geometry (`w`, `lc`, `lj`, `ml`, `ml2`, `d`) with gradient definitions (`g`, `s`, `e`, `t`, `h`, `a`).
   - Supporting schema types: [StrokeDash.kt](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/styles/Stroke.kt) and schema-defined constants (`FillRule`, `LineCap`, `LineJoin`, `StrokeDashType`, `GradientType`).
4. Enforce strict schema defaults: only declare Kotlin constructor default values where the JSON schema explicitly specifies `"default"`. Do not invent defaults for fields where the schema does not define them.
5. Exclude all types and attributes outside the schema:
   - Exclude `NoStyle` (`"no"`), which is not part of the Lottie JSON schema.
   - Exclude After Effects metadata fields (`ix`, `mn`, `cix`) from style classes because they are not defined in `#/$defs/shapes/shape-style` or child schemas.
   - Exclude `bm` (BlendMode), which is not defined in `#/$defs/shapes/shape-style`.
6. Maintain full compatibility with existing tests and renderer without generating separate new test files.

---

## 2. Glossary

- **Shape Style:** A graphic element (`#/$defs/shapes/shape-style`) that defines the visual presentation (fill color, stroke width, gradient) of preceding shape geometries within the current group scope.
- **Graphic Element:** The base node type in a Lottie shape tree (`#/$defs/shapes/graphic-element`), containing `"nm"`, `"hd"`, and discriminated by `"ty"`.
- **Fill Rule:** Rule determining interior points for intersecting paths (`#/$defs/constants/fill-rule`: `1` = NonZero, `2` = EvenOdd).
- **Line Cap:** End cap style for open stroke paths (`#/$defs/constants/line-cap`: `1` = Butt, `2` = Round, `3` = Square).
- **Line Join:** Corner join style for strokes (`#/$defs/constants/line-join`: `1` = Miter, `2` = Round, `3` = Bevel).
- **Stroke Dash:** Item defining a dash segment or gap in a stroked outline (`#/$defs/shapes/stroke-dash`).
- **Stroke Dash Type:** Dash pattern item role (`#/$defs/constants/stroke-dash-type`: `"d"` = Dash, `"g"` = Gap, `"o"` = Offset).
- **Gradient Type:** Transition geometry for gradients (`#/$defs/constants/gradient-type`: `1` = Linear, `2` = Radial).

---

## 3. How: Schema Property & Default Audit

### 3.1 Strict Schema Property Mapping

Audit against [Lottie 1.0.1 JSON Schema](https://lottie.github.io/lottie-spec/1.0.1/lottie.schema.json):

| Schema Definition | Schema Pointer | Schema Properties | Schema Required | Schema Defaults | Excluded (Non-Schema) |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `graphic-element` | `#/$defs/shapes/graphic-element` | `nm` (string), `hd` (boolean), `ty` (string) | `ty` | None | `ix`, `mn`, `cix` |
| `shape-style` | `#/$defs/shapes/shape-style` | `allOf[graphic-element]`, `o` (scalar-property) | `o`, `ty` | None | `bm` |
| `fill` | `#/$defs/shapes/fill` | `allOf[shape-style]`, `c` (color-property), `r` (fill-rule) | `ty`, `c`, `o` | None | `ix`, `mn`, `cix`, `bm` |
| `base-stroke` | `#/$defs/shapes/base-stroke` | `w` (scalar), `lc` (line-cap), `lj` (line-join), `ml` (number), `ml2` (scalar), `d` (array of stroke-dash) | `w` | `lc: 2`, `lj: 2`, `ml: 0` | None |
| `stroke` | `#/$defs/shapes/stroke` | `allOf[shape-style, base-stroke]`, `c` (color-property) | `ty`, `c`, `w`, `o` | `lc: 2`, `lj: 2`, `ml: 0` | `ix`, `mn`, `cix`, `bm` |
| `stroke-dash` | `#/$defs/shapes/stroke-dash` | `allOf[visual-object]`, `n` (stroke-dash-type), `v` (scalar) | None | `n: "d"` | None |
| `base-gradient` | `#/$defs/shapes/base-gradient` | `g` (gradient), `s` (position), `e` (position), `t` (gradient-type), `h` (scalar), `a` (scalar) | `s`, `e`, `g`, `t` | None | None |
| `gradient-fill` | `#/$defs/shapes/gradient-fill` | `allOf[shape-style, base-gradient]`, `r` (fill-rule) | `ty`, `s`, `e`, `g`, `t`, `o` | None | `ix`, `mn`, `cix`, `bm` |
| `gradient-stroke` | `#/$defs/shapes/gradient-stroke` | `allOf[shape-style, base-stroke, base-gradient]` | `ty`, `w`, `s`, `e`, `g`, `t`, `o` | `lc: 2`, `lj: 2`, `ml: 0` | `ix`, `mn`, `cix`, `bm` |

### 3.2 Constructor Default Value Strategy

In strict adherence to the schema:
1. **Fields with Schema Defaults:** Only fields that have an explicit `"default"` in `lottie.schema.json` receive a default value in their Kotlin constructor:
   - `lc`: `LineCap = LineCap.Round` (`default: 2`)
   - `lj`: `LineJoin = LineJoin.Round` (`default: 2`)
   - `ml`: `Float = 0f` (`default: 0`)
   - `n`: `StrokeDashType = StrokeDashType.Dash` (`default: "d"`)
   - `ty`: `ShapeType = ShapeType.<Element>` (discriminator const)
2. **Optional Fields without Schema Defaults:** Receive `= null` in Kotlin so omitting them from JSON decodes to `null`:
   - `nm`: `String? = null`
   - `hd`: `SerializableBoolean? = null`
   - `r`: `FillRule? = null`
   - `ml2`: `BaseScalarProperty? = null`
   - `d`: `List<StrokeDash>? = null`
   - `h`: `BaseScalarProperty? = null`
   - `a`: `BaseScalarProperty? = null`
   - `v`: `BaseScalarProperty? = null` (in `StrokeDash`)
3. **Required Fields without Schema Defaults:** **No default value** in Kotlin constructor. Callers and JSON decoders must provide them explicitly:
   - `o`: `val opacity: BaseScalarProperty`
   - `c`: `val color: BaseColorProperty`
   - `w`: `val strokeWidth: BaseScalarProperty`
   - `g`: `val colors: BaseGradientProperty`
   - `s`: `val startPoint: BasePositionProperty`
   - `e`: `val endPoint: BasePositionProperty`
   - `t`: `val gradientType: GradientType`

---

## 4. Details: Target Implementation

### 4.1 `ShapeStyle.kt`

Location: `../src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/styles/ShapeStyle.kt`

```kotlin
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

package com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles

import com.google.android.horologist.remotecompose.lottie.format.graphicelement.GraphicElement
import com.google.android.horologist.remotecompose.lottie.format.properties.BaseScalarProperty

/**
 * Sealed category interface representing all visual styling elements in a Lottie shape hierarchy,
 * conforming to [Lottie Shape Style](https://lottie.github.io/lottie-spec/latest/specs/shapes/#shape-style).
 *
 * Essential Invariants:
 * - Scoping: Styles define the visual appearance (such as fill color, stroke width, or gradients)
 *   of all preceding shape geometries within the current group scope.
 * - Multi-Style Stacking: When multiple styles apply to the same shape, the shape is rendered
 *   repeatedly for each style in reverse array order (bottom-to-top).
 * - opacity: Animatable scalar controlling overall style opacity, normalized on [0.0, 100.0].
 *   Defined as required in the Lottie JSON schema without a default value.
 */
internal sealed interface ShapeStyle : GraphicElement {
  val opacity: BaseScalarProperty
}
```

---

### 4.2 `Fill.kt`

Location: `../src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/styles/Fill.kt`

```kotlin
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

package com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles

import com.google.android.horologist.remotecompose.lottie.format.graphicelement.ShapeType
import com.google.android.horologist.remotecompose.lottie.format.properties.BaseColorProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.BaseScalarProperty
import com.google.android.horologist.remotecompose.lottie.format.values.SerializableBoolean
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Shape Element representing a solid color fill, conforming to
 * [Lottie Fill](https://lottie.github.io/lottie-spec/latest/specs/shapes/#fill).
 *
 * Essential Invariants:
 * - Scoping: Colors the interior of all preceding shape curves within the current group scope.
 * - Stacking Order: Evaluated in bottom-to-top order when combined with sibling fills or strokes.
 * - Discriminator: type is strictly [ShapeType.Fill] ("fl").
 *
 * Schema Specification:
 * - Required Fields: "ty" (const "fl"), "c" (Color), "o" (Opacity).
 * - Optional Fields without Defaults: "nm" (String), "hd" (Boolean), "r" (FillRule).
 *
 * @property name Human-readable element name.
 * @property hidden When true, suppresses rendering of this fill.
 * @property type Shape type discriminator, strictly [ShapeType.Fill].
 * @property opacity Animatable fill opacity on [0.0, 100.0]. Required in schema.
 * @property color Animatable solid RGBA color property. Required in schema.
 * @property fillRule Path winding rule used to resolve multi-path interiors and self-intersections.
 */
@Serializable
internal data class Fill(
  @SerialName("nm") override val name: String? = null,
  @SerialName("hd") override val hidden: SerializableBoolean? = null,
  @SerialName("ty") override val type: ShapeType = ShapeType.Fill,
  @SerialName("o") override val opacity: BaseScalarProperty,
  @SerialName("c") val color: BaseColorProperty,
  @SerialName("r") val fillRule: FillRule? = null,
) : ShapeStyle

/**
 * Rule used to handle multiple shapes or intersecting paths rendered with the same fill object,
 * conforming to [Lottie Fill Rule](https://lottie.github.io/lottie-spec/latest/specs/constants/#fill-rule).
 */
@Serializable(with = FillRuleSerializer::class)
internal enum class FillRule(val value: Int) {
  NonZero(1),
  EvenOdd(2);

  companion object {
    fun fromValueOrNull(value: Int): FillRule? = entries.firstOrNull { it.value == value }
  }
}

/**
 * Serializer for [FillRule] supporting integer and float representations with fallback to [FillRule.NonZero].
 *
 * Contract:
 * - Deserialization Preconditions: Incoming token is a numerical primitive or JSON element.
 * - Deserialization Postconditions: Returns matching [FillRule], defaulting to [FillRule.NonZero] (1) on unknown tokens.
 * - Exceptions: Does not throw; malformed tokens fall back to [FillRule.NonZero].
 * - Serialization: Encodes the primitive integer value via [Encoder.encodeInt].
 */
internal object FillRuleSerializer : KSerializer<FillRule> {
  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor("FillRule", PrimitiveKind.INT)

  override fun deserialize(decoder: Decoder): FillRule {
    return try {
      val jsonDecoder = decoder as? JsonDecoder
      if (jsonDecoder != null) {
        val element = jsonDecoder.decodeJsonElement()
        val intVal =
          element.jsonPrimitive.intOrNull ?: element.jsonPrimitive.floatOrNull?.toInt() ?: 1
        FillRule.fromValueOrNull(intVal) ?: FillRule.NonZero
      } else {
        val value = decoder.decodeInt()
        FillRule.fromValueOrNull(value) ?: FillRule.NonZero
      }
    } catch (e: Exception) {
      FillRule.NonZero
    }
  }

  override fun serialize(encoder: Encoder, value: FillRule) {
    encoder.encodeInt(value.value)
  }
}
```

---

### 4.3 `Stroke.kt`

Location: `../src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/styles/Stroke.kt`

```kotlin
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

package com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles

import com.google.android.horologist.remotecompose.lottie.format.graphicelement.ShapeType
import com.google.android.horologist.remotecompose.lottie.format.properties.BaseColorProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.BaseScalarProperty
import com.google.android.horologist.remotecompose.lottie.format.values.SerializableBoolean
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Shape Element representing a solid stroke outline, conforming to
 * [Lottie Stroke](https://lottie.github.io/lottie-spec/latest/specs/shapes/#stroke)
 * and [Base Stroke](https://lottie.github.io/lottie-spec/latest/specs/shapes/#base-stroke).
 *
 * Essential Invariants:
 * - Scoping: Outlines all preceding shape curves within the current group scope.
 * - Stacking Order: Evaluated in bottom-to-top order when combined with sibling fills or strokes.
 * - Discriminator: type is strictly [ShapeType.Stroke] ("st").
 *
 * Schema Specification:
 * - Required Fields: "ty" (const "st"), "c" (Color), "w" (Stroke width), "o" (Opacity).
 * - Optional Fields with Schema Defaults:
 *   - "lc": Line cap (schema default: 2 -> [LineCap.Round]).
 *   - "lj": Line join (schema default: 2 -> [LineJoin.Round]).
 *   - "ml": Numeric miter limit (schema default: 0 -> 0f).
 * - Optional Fields without Schema Defaults:
 *   - "nm": Human-readable name (default: null).
 *   - "hd": Hidden boolean flag (default: null).
 *   - "ml2": Animatable miter limit (default: null).
 *   - "d": Dash pattern array (default: null).
 *
 * @property name Human-readable element name.
 * @property hidden When true, suppresses rendering of this stroke.
 * @property type Shape type discriminator, strictly [ShapeType.Stroke].
 * @property opacity Animatable stroke opacity on [0.0, 100.0]. Required in schema.
 * @property color Animatable solid RGBA stroke color. Required in schema.
 * @property strokeWidth Animatable stroke width. Required in schema.
 * @property lineCap Style at the end of stroked lines. Defaults to [LineCap.Round] per schema.
 * @property lineJoin Style at sharp corners of stroked lines. Defaults to [LineJoin.Round] per schema.
 * @property miterLimit Maximum miter limit before beveling. Defaults to 0f per schema.
 * @property miterLimitAnimatable Animatable scalar alternative to miterLimit.
 * @property dashes Optional list of dash segments, gaps, and offsets.
 */
@Serializable
internal data class Stroke(
  @SerialName("nm") override val name: String? = null,
  @SerialName("hd") override val hidden: SerializableBoolean? = null,
  @SerialName("ty") override val type: ShapeType = ShapeType.Stroke,
  @SerialName("o") override val opacity: BaseScalarProperty,
  @SerialName("c") val color: BaseColorProperty,
  @SerialName("w") val strokeWidth: BaseScalarProperty,
  @SerialName("lc") val lineCap: LineCap = LineCap.Round,
  @SerialName("lj") val lineJoin: LineJoin = LineJoin.Round,
  @SerialName("ml") val miterLimit: Float = 0f,
  @SerialName("ml2") val miterLimitAnimatable: BaseScalarProperty? = null,
  @SerialName("d") val dashes: List<StrokeDash>? = null,
) : ShapeStyle

/**
 * Style at the end of a stroked line, conforming to
 * [Lottie Line Cap](https://lottie.github.io/lottie-spec/latest/specs/constants/#line-cap).
 */
@Serializable(with = LineCapSerializer::class)
internal enum class LineCap(val value: Int) {
  Butt(1),
  Round(2),
  Square(3);

  companion object {
    fun fromValueOrNull(value: Int): LineCap? = entries.firstOrNull { it.value == value }
  }
}

/** Serializer for [LineCap] supporting integer and float primitives with fallback to [LineCap.Round]. */
internal object LineCapSerializer : KSerializer<LineCap> {
  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor("LineCap", PrimitiveKind.INT)

  override fun deserialize(decoder: Decoder): LineCap {
    return try {
      val jsonDecoder = decoder as? JsonDecoder
      if (jsonDecoder != null) {
        val element = jsonDecoder.decodeJsonElement()
        val intVal =
          element.jsonPrimitive.intOrNull ?: element.jsonPrimitive.floatOrNull?.toInt() ?: 2
        LineCap.fromValueOrNull(intVal) ?: LineCap.Round
      } else {
        val value = decoder.decodeInt()
        LineCap.fromValueOrNull(value) ?: LineCap.Round
      }
    } catch (e: Exception) {
      LineCap.Round
    }
  }

  override fun serialize(encoder: Encoder, value: LineCap) {
    encoder.encodeInt(value.value)
  }
}

/**
 * Style at a sharp corner of a stroked line, conforming to
 * [Lottie Line Join](https://lottie.github.io/lottie-spec/latest/specs/constants/#line-join).
 */
@Serializable(with = LineJoinSerializer::class)
internal enum class LineJoin(val value: Int) {
  Miter(1),
  Round(2),
  Bevel(3);

  companion object {
    fun fromValueOrNull(value: Int): LineJoin? = entries.firstOrNull { it.value == value }
  }
}

/** Serializer for [LineJoin] supporting integer and float primitives with fallback to [LineJoin.Round]. */
internal object LineJoinSerializer : KSerializer<LineJoin> {
  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor("LineJoin", PrimitiveKind.INT)

  override fun deserialize(decoder: Decoder): LineJoin {
    return try {
      val jsonDecoder = decoder as? JsonDecoder
      if (jsonDecoder != null) {
        val element = jsonDecoder.decodeJsonElement()
        val intVal =
          element.jsonPrimitive.intOrNull ?: element.jsonPrimitive.floatOrNull?.toInt() ?: 2
        LineJoin.fromValueOrNull(intVal) ?: LineJoin.Round
      } else {
        val value = decoder.decodeInt()
        LineJoin.fromValueOrNull(value) ?: LineJoin.Round
      }
    } catch (e: Exception) {
      LineJoin.Round
    }
  }

  override fun serialize(encoder: Encoder, value: LineJoin) {
    encoder.encodeInt(value.value)
  }
}

/**
 * An item describing the dash pattern in a stroked path, conforming to
 * [Lottie Stroke Dash](https://lottie.github.io/lottie-spec/latest/specs/shapes/#stroke-dash).
 *
 * @property name Human-readable name inherited from Visual Object.
 * @property type Type of dash item. Defaults to [StrokeDashType.Dash] per schema.
 * @property length Length of the dash or gap segment.
 */
@Serializable
internal data class StrokeDash(
  @SerialName("nm") val name: String? = null,
  @SerialName("n") val type: StrokeDashType = StrokeDashType.Dash,
  @SerialName("v") val length: BaseScalarProperty? = null,
)

/**
 * Type of a dash item in a stroked line, conforming to
 * [Lottie Stroke Dash Type](https://lottie.github.io/lottie-spec/latest/specs/constants/#stroke-dash-type).
 */
@Serializable
internal enum class StrokeDashType(val value: String) {
  @SerialName("d") Dash("d"),
  @SerialName("g") Gap("g"),
  @SerialName("o") Offset("o");

  companion object {
    fun fromValueOrNull(value: String): StrokeDashType? = entries.firstOrNull { it.value == value }
  }
}
```

---

### 4.4 `GradientFill.kt` & `GradientStroke.kt`

Location: `../src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/styles/GradientFill.kt`

```kotlin
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

package com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles

import com.google.android.horologist.remotecompose.lottie.format.graphicelement.ShapeType
import com.google.android.horologist.remotecompose.lottie.format.properties.BaseGradientProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.BasePositionProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.BaseScalarProperty
import com.google.android.horologist.remotecompose.lottie.format.values.SerializableBoolean
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Type of a gradient, conforming to
 * [Lottie Gradient Type](https://lottie.github.io/lottie-spec/latest/specs/constants/#gradient-type).
 */
@Serializable(with = GradientTypeSerializer::class)
internal enum class GradientType(val value: Int) {
  Linear(1),
  Radial(2);

  companion object {
    fun fromValueOrNull(value: Int): GradientType? = entries.firstOrNull { it.value == value }
  }
}

/** Serializer for [GradientType] supporting integer and float primitives with fallback to [GradientType.Linear]. */
internal object GradientTypeSerializer : KSerializer<GradientType> {
  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor("GradientType", PrimitiveKind.INT)

  override fun deserialize(decoder: Decoder): GradientType {
    return try {
      val jsonDecoder = decoder as? JsonDecoder
      if (jsonDecoder != null) {
        val element = jsonDecoder.decodeJsonElement()
        val intVal =
          element.jsonPrimitive.intOrNull ?: element.jsonPrimitive.floatOrNull?.toInt() ?: 1
        GradientType.fromValueOrNull(intVal) ?: GradientType.Linear
      } else {
        val value = decoder.decodeInt()
        GradientType.fromValueOrNull(value) ?: GradientType.Linear
      }
    } catch (e: Exception) {
      GradientType.Linear
    }
  }

  override fun serialize(encoder: Encoder, value: GradientType) {
    encoder.encodeInt(value.value)
  }
}

/**
 * Shape Element representing a gradient fill color, conforming to
 * [Lottie Gradient Fill](https://lottie.github.io/lottie-spec/latest/specs/shapes/#gradient-fill)
 * and [Base Gradient](https://lottie.github.io/lottie-spec/latest/specs/shapes/#base-gradient).
 *
 * Schema Specification:
 * - Required Fields: "ty" (const "gf"), "o" (Opacity), "g" (Colors), "s" (Start point),
 *   "e" (End point), "t" (Gradient type).
 * - Optional Fields without Schema Defaults: "nm" (String), "hd" (Boolean), "r" (FillRule),
 *   "h" (Highlight length), "a" (Highlight angle).
 *
 * @property name Human-readable element name.
 * @property hidden When true, suppresses rendering of this fill.
 * @property type Shape type discriminator, strictly [ShapeType.GradientFill].
 * @property opacity Animatable fill opacity on [0.0, 100.0]. Required in schema.
 * @property colors Gradient stops and color definitions. Required in schema.
 * @property startPoint Starting point coordinate for the gradient. Required in schema.
 * @property endPoint Ending point coordinate for the gradient. Required in schema.
 * @property gradientType Type of gradient (linear or radial). Required in schema.
 * @property fillRule Path winding rule for multi-path intersections.
 * @property highlightLength Radial highlight length as a percentage between start and end points.
 * @property highlightAngle Radial highlight angle in clockwise degrees.
 */
@Serializable
internal data class GradientFill(
  @SerialName("nm") override val name: String? = null,
  @SerialName("hd") override val hidden: SerializableBoolean? = null,
  @SerialName("ty") override val type: ShapeType = ShapeType.GradientFill,
  @SerialName("o") override val opacity: BaseScalarProperty,
  @SerialName("g") val colors: BaseGradientProperty,
  @SerialName("s") val startPoint: BasePositionProperty,
  @SerialName("e") val endPoint: BasePositionProperty,
  @SerialName("t") val gradientType: GradientType,
  @SerialName("r") val fillRule: FillRule? = null,
  @SerialName("h") val highlightLength: BaseScalarProperty? = null,
  @SerialName("a") val highlightAngle: BaseScalarProperty? = null,
) : ShapeStyle

/**
 * Shape Element representing a gradient outline stroke, conforming to
 * [Lottie Gradient Stroke](https://lottie.github.io/lottie-spec/latest/specs/shapes/#gradient-stroke),
 * [Base Stroke](https://lottie.github.io/lottie-spec/latest/specs/shapes/#base-stroke), and
 * [Base Gradient](https://lottie.github.io/lottie-spec/latest/specs/shapes/#base-gradient).
 *
 * Schema Specification:
 * - Required Fields: "ty" (const "gs"), "o" (Opacity), "w" (Stroke width), "g" (Colors),
 *   "s" (Start point), "e" (End point), "t" (Gradient type).
 * - Optional Fields with Schema Defaults:
 *   - "lc": Line cap (schema default: 2 -> [LineCap.Round]).
 *   - "lj": Line join (schema default: 2 -> [LineJoin.Round]).
 *   - "ml": Numeric miter limit (schema default: 0 -> 0f).
 * - Optional Fields without Schema Defaults:
 *   - "nm": Human-readable name (default: null).
 *   - "hd": Hidden boolean flag (default: null).
 *   - "ml2": Animatable miter limit (default: null).
 *   - "d": Dash pattern array (default: null).
 *   - "h": Highlight length (default: null).
 *   - "a": Highlight angle (default: null).
 */
@Serializable
internal data class GradientStroke(
  @SerialName("nm") override val name: String? = null,
  @SerialName("hd") override val hidden: SerializableBoolean? = null,
  @SerialName("ty") override val type: ShapeType = ShapeType.GradientStroke,
  @SerialName("o") override val opacity: BaseScalarProperty,
  @SerialName("w") val strokeWidth: BaseScalarProperty,
  @SerialName("g") val colors: BaseGradientProperty,
  @SerialName("s") val startPoint: BasePositionProperty,
  @SerialName("e") val endPoint: BasePositionProperty,
  @SerialName("t") val gradientType: GradientType,
  @SerialName("lc") val lineCap: LineCap = LineCap.Round,
  @SerialName("lj") val lineJoin: LineJoin = LineJoin.Round,
  @SerialName("ml") val miterLimit: Float = 0f,
  @SerialName("ml2") val miterLimitAnimatable: BaseScalarProperty? = null,
  @SerialName("d") val dashes: List<StrokeDash>? = null,
  @SerialName("h") val highlightLength: BaseScalarProperty? = null,
  @SerialName("a") val highlightAngle: BaseScalarProperty? = null,
) : ShapeStyle
```

---

## 5. Downstream Impact & Compatibility Verification

### 5.1 `GraphicElement.kt` Updates
- Register schema-defined style shape types in [ShapeType](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/GraphicElement.kt#L56):
  ```kotlin
  Stroke("st"),
  GradientFill("gf"),
  GradientStroke("gs"),
  ```
- Update [GraphicElementSerializer](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/GraphicElement.kt#L72) dispatching:
  ```kotlin
  ShapeType.Stroke.value -> Stroke.serializer()
  ShapeType.GradientFill.value -> GradientFill.serializer()
  ShapeType.GradientStroke.value -> GradientStroke.serializer()
  ```
- Note: `NoStyle` (`"no"`) is omitted as it is outside the schema.

### 5.2 Renderer Compatibility ([renderer/Shape.kt](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/Shape.kt))
- Existing renderer reads `fill.color` via `is Fill -> fill(shape, animationSettings)`.
- Because `color` and `opacity` are preserved on `Fill`, existing renderer integration continues without modification.

### 5.3 Existing Test Verification (No New Tests Generated)
Per user instructions, **no separate new unit tests are generated**; existing tests are verified and updated only if strictly necessary:
1. [ParsingTest.kt](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/test/java/com/google/android/horologist/remotecompose/lottie/ParsingTest.kt#L106):
   - `val fill = group.shapes[1] as Fill; assertThat(fill.color.slotId).isEqualTo("color.primary")` -> **Pass** (`geometry.json` provides `"c"` and `"o"` explicitly).
2. [LottieScalingDiffScreenshotTest.kt](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/test/java/com/google/android/horologist/remotecompose/lottie/LottieScalingDiffScreenshotTest.kt#L176-L226):
   - Named argument construction:
     ```kotlin
     Fill(
       name = "Circle Fill",
       color = StaticColorProperty(value = Color(0.95f, 0.25f, 0.2f, 1.0f).rc),
       opacity = StaticScalarProperty(animated = false.rb, value = 100f.rf),
     )
     ```
     Both `color` and `opacity` are explicitly supplied. Removing default values does not break these call sites -> **Pass**.
3. [LottieDecoderResilienceTest.kt](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/test/java/com/google/android/horologist/remotecompose/lottie/LottieDecoderResilienceTest.kt#L82):
   - Deserialization of valid Lottie JSON containing `"ty": "fl"` -> **Pass**.

---

## 6. Implementation Roadmap

1. **Step 1**: Create [ShapeStyle.kt](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/styles/ShapeStyle.kt).
2. **Step 2**: Refactor [Fill.kt](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/styles/Fill.kt) with strictly schema-compliant attributes, `FillRule`, `FillRuleSerializer`, and `ShapeStyle` inheritance.
3. **Step 3**: Add [Stroke.kt](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/styles/Stroke.kt) (with `LineCap`, `LineJoin`, `StrokeDash`, `StrokeDashType`), and [GradientFill.kt](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/styles/GradientFill.kt) & [GradientStroke.kt](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/styles/GradientStroke.kt) (with `GradientType`).
4. **Step 4**: Update [GraphicElement.kt](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/GraphicElement.kt) (`ShapeType` and `GraphicElementSerializer`).
5. **Step 5**: Execute verification following `../.agents/rules/testing_policy.md`:
   - `./gradlew :remotecompose:lottie:ktfmtFormat`
   - `./gradlew :remotecompose:lottie:compileDebugKotlin`
   - `./gradlew :remotecompose:lottie:testDebugUnitTest`

---

## 7. Alternatives Considered

### Alternative 1: Adding After Effects Metadata Fields (`ix`, `mn`, `cix`) to Shape Styles
- **Proposal:** Include `ix` (index), `mn` (matchName), and `cix` (propertyIndex) on all shape styles.
- **Pros:** Matches legacy Bodymovin export artifacts found in older After Effects files.
- **Cons:** Violates user directive to strictly stick to attributes defined in [Lottie 1.0.1 JSON Schema](https://lottie.github.io/lottie-spec/1.0.1/lottie.schema.json). `#/$defs/shapes/shape-style`, `#/$defs/shapes/fill`, and `#/$defs/shapes/stroke` do not define `ix`, `mn`, or `cix`.
- **Decision Rejected:** Exclude `ix`, `mn`, and `cix` from shape styles to ensure 100% schema conformance.

### Alternative 2: Including `NoStyle` (`"no"`)
- **Proposal:** Create a `NoStyle` class implementing `ShapeStyle` for unstyled shapes.
- **Pros:** Provides an explicit placeholder in polymorphic deserialization.
- **Cons:** Violates user directive: `NoStyle` does not exist anywhere in the Lottie specification or JSON schema.
- **Decision Rejected:** Exclude `NoStyle`. Only include types explicitly defined in the schema (`Fill`, `Stroke`, `GradientFill`, `GradientStroke`).

### Alternative 3: Inventing Default Values for Required Schema Fields (`color`, `opacity`, `strokeWidth`, `startPoint`, `endPoint`)
- **Proposal:** Provide fallback defaults such as `opacity = 100f.rf`, `color = Color.Black.rc`, `strokeWidth = 1f.rf`, `startPoint = (0,0)`, `endPoint = (0,0)`, and `fillRule = NonZero`.
- **Pros:** Allows zero-argument or sparse construction.
- **Cons:** Directly violates user directive: "if there are no default values -- do not come up with ones!". In the JSON schema, these fields have no `"default"` attribute; `opacity`, `color`, `strokeWidth`, and gradient coordinates are marked `"required"`. Inventing synthetic defaults masks corrupted or incomplete Lottie files and diverges from the canonical specification.
- **Decision Rejected:** Strictly adhere to the JSON schema. Only supply default parameters for fields with an explicit `"default"` in `lottie.schema.json` (`lc = 2`, `lj = 2`, `ml = 0`, `n = "d"`).
