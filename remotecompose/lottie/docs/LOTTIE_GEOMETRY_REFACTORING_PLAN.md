# Lottie AST Geometry (`format/graphicelement/geometry/`) Refactoring Plan

## Executive Summary

This document specifies the refactoring plan for geometry shapes in `format/graphicelement/geometry/` within the `:remotecompose:lottie` module.

The refactoring aligns the codebase with:
1. **Canonical Schema Definition**: Strict adherence to [Lottie Shapes Specification](https://lottie.github.io/lottie-spec/latest/specs/shapes/#shape) and the authoritative [Lottie 1.0.1 JSON Schema](https://lottie.github.io/lottie-spec/1.0.1/lottie.schema.json).
2. **Zero Invented Defaults**: Constructor parameters strictly mirror defaults defined in `lottie.schema.json`. If an attribute does not have a default defined in the JSON schema, no synthetic default is introduced.
3. **Strict Schema Typings**: Only types and attributes present in `lottie.schema.json` are included. Extraneous attributes (`ix`, `mn`, `cix`) and non-standard shape types are excluded.
4. **Common Ancestor Interface**: A sealed category interface `GeometryShape : GraphicElement` unifying the `"d"` attribute ([Shape Direction](https://lottie.github.io/lottie-spec/1.0.1/specs/constants/#shape-direction)), inspired by the category partitioning in [prepear-for-merge](https://github.com/MarkYav/horologist/tree/prepear-for-merge/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement).

Files in scope:
- [GeometryShape.kt](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/geometry/GeometryShape.kt) (New common ancestor interface)
- [Ellipse.kt](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/geometry/Ellipse.kt)
- [Path.kt](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/geometry/Path.kt)
- [Rectangle.kt](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/geometry/Rectangle.kt)
- [PolyStar.kt](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/geometry/PolyStar.kt)

No separate new test suites will be generated. Existing unit tests will be updated where required.

---

## 1. Schema Analysis & Default Value Audit

The following table details every attribute defined in [lottie.schema.json](https://lottie.github.io/lottie-spec/1.0.1/lottie.schema.json) for the base hierarchy and concrete geometry models:

### 1.1 Base Shape Hierarchy

| Model / Schema Ref | JSON Key | Schema Title / Ref | Schema Required | Schema Default | Kotlin Type | Kotlin Default |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **`visual-object`** (`#/$defs/helpers/visual-object`) | `"nm"` | Name (`string`) | No | None | `String?` | `= null` |
| **`graphic-element`** (`#/$defs/shapes/graphic-element`) | `"hd"` | Hidden (`boolean`) | No | None | `SerializableBoolean?` | `= null` |
| | `"ty"` | Shape Type (`string`) | Yes | None | `ShapeType` | Fixed per shape |
| **`shape`** (`#/$defs/shapes/shape`) | `"d"` | Direction (`#/$defs/constants/shape-direction`) | No | None | `Int?` | `= null` |

### 1.2 Concrete Shapes

| Shape / Schema Ref | JSON Key | Schema Title / Ref | Schema Required | Schema Default | Kotlin Type | Kotlin Default |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **`ellipse`** (`#/$defs/shapes/ellipse`) | `"ty"` | `"el"` | Yes | `"el"` | `ShapeType` | `= ShapeType.Ellipse` |
| | `"p"` | Position (`position-property`) | Yes | **None** | `BasePositionProperty` | **None** |
| | `"s"` | Size (`vector-property`) | Yes | **None** | `BaseVectorProperty` | **None** |
| **`path`** (`#/$defs/shapes/path`) | `"ty"` | `"sh"` | Yes | `"sh"` | `ShapeType` | `= ShapeType.Path` |
| | `"ks"` | Shape (`bezier-property`) | Yes | **None** | `BaseBezierProperty` | **None** |
| **`rectangle`** (`#/$defs/shapes/rectangle`) | `"ty"` | `"rc"` | Yes | `"rc"` | `ShapeType` | `= ShapeType.Rectangle` |
| | `"p"` | Position (`position-property`) | Yes | **None** | `BasePositionProperty` | **None** |
| | `"s"` | Size (`vector-property`) | Yes | **None** | `BaseVectorProperty` | **None** |
| | `"r"` | Roundness (`scalar-property`) | No | **None** | `BaseScalarProperty?` | `= null` |
| **`polystar`** (`#/$defs/shapes/polystar`) | `"ty"` | `"sr"` | Yes | `"sr"` | `ShapeType` | `= ShapeType.PolyStar` |
| | `"p"` | Position (`position-property`) | Yes | **None** | `BasePositionProperty` | **None** |
| | `"or"` | Outer Radius (`scalar-property`) | Yes | **None** | `BaseScalarProperty` | **None** |
| | `"os"` | Outer Roundness (`scalar-property`) | Yes | **None** | `BaseScalarProperty` | **None** |
| | `"r"` | Rotation (`scalar-property`) | Yes | **None** | `BaseScalarProperty` | **None** |
| | `"pt"` | Points (`scalar-property`) | Yes | **None** | `BaseScalarProperty` | **None** |
| | `"sy"` | Star Type (`star-type`) | No | **`1`** | `PolyStarType` | **`= PolyStarType.Star`** |
| | `"ir"` | Inner Radius (`scalar-property`) | No (cond. required) | **None** | `BaseScalarProperty?` | `= null` |
| | `"is"` | Inner Roundness (`scalar-property`) | No (cond. required) | **None** | `BaseScalarProperty?` | `= null` |

### 1.3 Key Schema Invariants
1. **Zero Invented Defaults**:
   - Attributes without schema defaults (`p`, `s`, `ks`, `or`, `os`, `r`, `pt`) do not have default values in Kotlin constructors.
   - Optional schema attributes without schema defaults (`nm`, `hd`, `d`, `r` in rectangle, `ir`, `is`) are nullable and default strictly to `null`.
   - The only non-null schema default in all geometry shapes is `polystar.sy` (`"default": 1`), which maps to `= PolyStarType.Star`.
2. **Zero Out-of-Schema Attributes**:
   - `ix` (index), `mn` (matchName), and `cix` (propertyIndex) are not defined in `lottie.schema.json` under `shapes/` and are excluded.
3. **Strict Schema Types**:
   - Only standard shapes defined in `#/$defs/shapes/all-graphic-elements` (`ellipse`, `path`, `rectangle`, `polystar`) and constants `#/$defs/constants/shape-direction` and `#/$defs/constants/star-type` are supported.

---

## 2. Target File Specifications

### 2.1 `GeometryShape.kt` (New File)

- **Location**: `../src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/geometry/GeometryShape.kt`
- **Specification**: [Lottie Shapes](https://lottie.github.io/lottie-spec/latest/specs/shapes/#shape) (`#/$defs/shapes/shape`) and [Shape Direction](https://lottie.github.io/lottie-spec/1.0.1/specs/constants/#shape-direction) (`#/$defs/constants/shape-direction`).

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

package com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry

import com.google.android.horologist.remotecompose.lottie.format.graphicelement.GraphicElement

/**
 * Sealed category interface for drawable geometry shapes conforming to
 * [Lottie Shapes](https://lottie.github.io/lottie-spec/latest/specs/shapes/#shape).
 *
 * Defines the contour curve geometry without visual styling information.
 *
 * Essential Invariants:
 * - Inherits visual element metadata ([name], [hidden], [type]) from [GraphicElement].
 * - [direction]: Drawing direction of the shape curve (`"d"`), conforming to
 *   [Shape Direction](https://lottie.github.io/lottie-spec/1.0.1/specs/constants/#shape-direction):
 *   `1` for Normal (clockwise), `3` for Reversed (counter-clockwise). Nullable when omitted from JSON.
 */
internal sealed interface GeometryShape : GraphicElement {
  val direction: Int?
}

/**
 * Drawing direction of a shape curve conforming to
 * [Shape Direction](https://lottie.github.io/lottie-spec/1.0.1/specs/constants/#shape-direction).
 *
 * Values:
 * - [Normal] (`1`): Usually clockwise drawing direction.
 * - [Reversed] (`3`): Usually counter-clockwise drawing direction.
 */
internal enum class ShapeDirection(val value: Int) {
  Normal(1),
  Reversed(3);

  companion object {
    fun fromValueOrNull(value: Int): ShapeDirection? =
      entries.firstOrNull { it.value == value }
  }
}

/**
 * Resolves the effective [ShapeDirection] for this geometry shape, defaulting to [ShapeDirection.Normal]
 * when [GeometryShape.direction] is null or unrecognized.
 */
internal val GeometryShape.shapeDirection: ShapeDirection
  get() = direction?.let { ShapeDirection.fromValueOrNull(it) } ?: ShapeDirection.Normal
```

---

### 2.2 `Ellipse.kt` Refactoring

- **Location**: `../src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/geometry/Ellipse.kt`
- **Specification**: [Ellipse Shape](https://lottie.github.io/lottie-spec/latest/specs/shapes/#ellipse) (`#/$defs/shapes/ellipse`, `"ty": "el"`).
- **Attributes in Schema**: `"ty"` (required const "el"), `"p"` (required), `"s"` (required), `"nm"` (optional), `"hd"` (optional), `"d"` (optional).
- **Schema Defaults**: None for `"p"` or `"s"`. No synthetic defaults in Kotlin constructor.

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

package com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry

import com.google.android.horologist.remotecompose.lottie.format.graphicelement.ShapeType
import com.google.android.horologist.remotecompose.lottie.format.properties.BasePositionProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.BaseVectorProperty
import com.google.android.horologist.remotecompose.lottie.format.values.SerializableBoolean
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Parametric ellipse shape conforming to
 * [Ellipse Shape](https://lottie.github.io/lottie-spec/latest/specs/shapes/#ellipse).
 *
 * Defined parametrically by its center position [position] and total size [size] (horizontal and
 * vertical diameter).
 *
 * Schema Specification:
 * - Required Fields: `"ty"` (`"el"`), `"p"` (position), `"s"` (size).
 * - Optional Fields without Schema Default:
 *     - `"nm"` (name, default: `null`)
 *     - `"hd"` (hidden flag, default: `null`)
 *     - `"d"` (shape direction, default: `null`)
 *
 * Invariants:
 * - [position]: Center coordinates of the ellipse. Required; no schema default.
 * - [size]: Vector `[width, height]` defining diameter. Required; no schema default.
 * - [direction]: Drawing direction (`"d"`). Nullable when omitted.
 */
@Serializable
internal data class Ellipse(
  @SerialName("nm") override val name: String? = null,
  @SerialName("hd") override val hidden: SerializableBoolean? = null,
  @SerialName("ty") override val type: ShapeType = ShapeType.Ellipse,
  @SerialName("d") override val direction: Int? = null,
  @SerialName("p") val position: BasePositionProperty,
  @SerialName("s") val size: BaseVectorProperty,
) : GeometryShape
```

---

### 2.3 `Path.kt` Refactoring

- **Location**: `../src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/geometry/Path.kt`
- **Specification**: [Path Shape](https://lottie.github.io/lottie-spec/latest/specs/shapes/#path) (`#/$defs/shapes/path`, `"ty": "sh"`).
- **Attributes in Schema**: `"ty"` (required const "sh"), `"ks"` (required), `"nm"` (optional), `"hd"` (optional), `"d"` (optional).
- **Schema Defaults**: None for `"ks"`. No synthetic defaults in Kotlin constructor.

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

package com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry

import com.google.android.horologist.remotecompose.lottie.format.graphicelement.ShapeType
import com.google.android.horologist.remotecompose.lottie.format.properties.BaseBezierProperty
import com.google.android.horologist.remotecompose.lottie.format.values.SerializableBoolean
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Freeform Bézier path shape conforming to
 * [Path Shape](https://lottie.github.io/lottie-spec/latest/specs/shapes/#path).
 *
 * Represents an explicit cubic Bézier curve containing ordered vertices, in-tangent control handles,
 * out-tangent control handles, and a closed flag.
 *
 * Schema Specification:
 * - Required Fields: `"ty"` (`"sh"`), `"ks"` (Bézier property).
 * - Optional Fields without Schema Default:
 *     - `"nm"` (name, default: `null`)
 *     - `"hd"` (hidden flag, default: `null`)
 *     - `"d"` (shape direction, default: `null`)
 *
 * Invariants:
 * - [shape]: Animatable Bézier curve geometry ([BaseBezierProperty]). Required; no schema default.
 * - [direction]: Drawing direction (`"d"`). Nullable when omitted.
 */
@Serializable
internal data class Path(
  @SerialName("nm") override val name: String? = null,
  @SerialName("hd") override val hidden: SerializableBoolean? = null,
  @SerialName("ty") override val type: ShapeType = ShapeType.Path,
  @SerialName("d") override val direction: Int? = null,
  @SerialName("ks") val shape: BaseBezierProperty,
) : GeometryShape
```

---

### 2.4 `Rectangle.kt` Refactoring

- **Location**: `../src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/geometry/Rectangle.kt`
- **Specification**: [Rectangle Shape](https://lottie.github.io/lottie-spec/latest/specs/shapes/#rectangle) (`#/$defs/shapes/rectangle`, `"ty": "rc"`).
- **Attributes in Schema**: `"ty"` (required const "rc"), `"p"` (required), `"s"` (required), `"r"` (optional), `"nm"` (optional), `"hd"` (optional), `"d"` (optional).
- **Schema Defaults**: None for `"p"`, `"s"`, or `"r"`. In Kotlin, `"r"` is nullable with default `null`.

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

package com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry

import com.google.android.horologist.remotecompose.lottie.format.graphicelement.ShapeType
import com.google.android.horologist.remotecompose.lottie.format.properties.BasePositionProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.BaseScalarProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.BaseVectorProperty
import com.google.android.horologist.remotecompose.lottie.format.values.SerializableBoolean
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Parametric rectangle shape conforming to
 * [Rectangle Shape](https://lottie.github.io/lottie-spec/latest/specs/shapes/#rectangle).
 *
 * Represents an axis-aligned rectangle with optional rounded corners, centered at [position].
 *
 * Schema Specification:
 * - Required Fields: `"ty"` (`"rc"`), `"p"` (position), `"s"` (size).
 * - Optional Fields without Schema Default:
 *     - `"r"` (corner roundness, default: `null`)
 *     - `"nm"` (name, default: `null`)
 *     - `"hd"` (hidden flag, default: `null`)
 *     - `"d"` (shape direction, default: `null`)
 *
 * Invariants:
 * - [position]: Center coordinates `[x, y]`. Required; no schema default.
 * - [size]: Total dimensions `[width, height]`. Required; no schema default.
 * - [cornerRadius]: Corner rounding radius (`"r"`). Optional; no schema default. Nullable when omitted.
 * - [direction]: Drawing direction (`"d"`). Nullable when omitted.
 */
@Serializable
internal data class Rectangle(
  @SerialName("nm") override val name: String? = null,
  @SerialName("hd") override val hidden: SerializableBoolean? = null,
  @SerialName("ty") override val type: ShapeType = ShapeType.Rectangle,
  @SerialName("d") override val direction: Int? = null,
  @SerialName("p") val position: BasePositionProperty,
  @SerialName("s") val size: BaseVectorProperty,
  @SerialName("r") val cornerRadius: BaseScalarProperty? = null,
) : GeometryShape
```

---

### 2.5 `PolyStar.kt` Refactoring

- **Location**: `../src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/geometry/PolyStar.kt`
- **Specification**: [PolyStar Shape](https://lottie.github.io/lottie-spec/latest/specs/shapes/#polystar) (`#/$defs/shapes/polystar`, `"ty": "sr"`), [Star Type](https://lottie.github.io/lottie-spec/1.0.1/specs/constants/#star-type).
- **Attributes in Schema**: `"ty"`, `"p"`, `"or"`, `"os"`, `"r"`, `"pt"`, `"sy"` (default: 1), `"ir"`, `"is"`, `"nm"`, `"hd"`, `"d"`.
- **Schema Defaults**: `"sy"` has schema default `1` (`PolyStarType.Star`). All other properties have no schema default.

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

package com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry

import com.google.android.horologist.remotecompose.lottie.format.graphicelement.ShapeType
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
 * Parametric star or regular polygon shape conforming to
 * [PolyStar Shape](https://lottie.github.io/lottie-spec/latest/specs/shapes/#polystar).
 *
 * Schema Specification:
 * - Required Fields: `"ty"` (`"sr"`), `"p"`, `"or"`, `"os"`, `"r"`, `"pt"`.
 * - Optional Fields with Schema Default:
 *     - `"sy"` (star type, default: `1` -> [PolyStarType.Star])
 * - Optional Fields without Schema Default:
 *     - `"ir"` (inner radius, default: `null`, conditionally required when [starType] is [PolyStarType.Star])
 *     - `"is"` (inner roundness, default: `null`, conditionally required when [starType] is [PolyStarType.Star])
 *     - `"nm"` (name, default: `null`)
 *     - `"hd"` (hidden flag, default: `null`)
 *     - `"d"` (shape direction, default: `null`)
 *
 * Invariants:
 * - [starType]: Selects star vs polygon topology (`"sy"`). Defaults to [PolyStarType.Star] per schema default `1`.
 * - [points], [position], [rotation], [outerRadius], [outerRoundness]: Required; no schema default.
 * - [innerRadius], [innerRoundness]: Optional in schema; evaluated when [starType] is [PolyStarType.Star].
 */
@Serializable
internal data class PolyStar(
  @SerialName("nm") override val name: String? = null,
  @SerialName("hd") override val hidden: SerializableBoolean? = null,
  @SerialName("ty") override val type: ShapeType = ShapeType.PolyStar,
  @SerialName("d") override val direction: Int? = null,
  @SerialName("sy") val starType: PolyStarType = PolyStarType.Star,
  @SerialName("pt") val points: BaseScalarProperty,
  @SerialName("p") val position: BasePositionProperty,
  @SerialName("r") val rotation: BaseScalarProperty,
  @SerialName("or") val outerRadius: BaseScalarProperty,
  @SerialName("os") val outerRoundness: BaseScalarProperty,
  @SerialName("ir") val innerRadius: BaseScalarProperty? = null,
  @SerialName("is") val innerRoundness: BaseScalarProperty? = null,
) : GeometryShape

/**
 * Geometric topology for [PolyStar] conforming to
 * [Star Type](https://lottie.github.io/lottie-spec/1.0.1/specs/constants/#star-type).
 *
 * Values:
 * - [Star] (`1`): Multi-pointed star topology.
 * - [Polygon] (`2`): Regular convex polygon topology.
 */
@Serializable(with = PolyStarTypeSerializer::class)
internal enum class PolyStarType(val value: Int) {
  Star(1),
  Polygon(2);

  companion object {
    fun fromValueOrNull(value: Int): PolyStarType? = entries.firstOrNull { it.value == value }
  }
}

/**
 * Serializer for [PolyStarType] decoding integer enum tokens.
 *
 * Contract:
 * - Deserialization: Decodes integer or numeric string; returns [PolyStarType.Star] for `1`,
 *   [PolyStarType.Polygon] for `2`. Defaults to [PolyStarType.Star] for unrecognized tokens.
 * - Serialization: Encodes the integer primitive [PolyStarType.value].
 */
internal object PolyStarTypeSerializer : KSerializer<PolyStarType> {
  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor("PolyStarType", PrimitiveKind.INT)

  override fun deserialize(decoder: Decoder): PolyStarType {
    return try {
      val jsonDecoder = decoder as? JsonDecoder
      if (jsonDecoder != null) {
        val element = jsonDecoder.decodeJsonElement()
        val intVal =
          element.jsonPrimitive.intOrNull ?: element.jsonPrimitive.floatOrNull?.toInt() ?: 1
        PolyStarType.fromValueOrNull(intVal) ?: PolyStarType.Star
      } else {
        val value = decoder.decodeInt()
        PolyStarType.fromValueOrNull(value) ?: PolyStarType.Star
      }
    } catch (e: Exception) {
      PolyStarType.Star
    }
  }

  override fun serialize(encoder: Encoder, value: PolyStarType) {
    encoder.encodeInt(value.value)
  }
}
```

---

## 3. Downstream Call Sites & Existing Tests Verification

### 3.1 Renderer Updates

In [renderer/shapes/Ellipse.kt](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/shapes/Ellipse.kt#L48):
- Current code:
  ```kotlin
  val sweepAngle = if (el.direction == 3) -360f else 360f
  ```
- Target code:
  ```kotlin
  val sweepAngle = if (el.shapeDirection == ShapeDirection.Reversed) -360f else 360f
  ```

### 3.2 Existing Test Adjustments

Per user instructions, no new test files will be generated. The only existing test that directly calls zero-argument constructors is [EvaluatorModularizationTest.kt](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/test/java/com/google/android/horologist/remotecompose/lottie/EvaluatorModularizationTest.kt#L114-L115):

```kotlin
// In EvaluatorModularizationTest.kt:
val rectShape =
    Rectangle(
        position = StaticPositionProperty(animated = false.rb, value = Point(0f.rf, 0f.rf)),
        size = StaticVectorProperty(animated = false.rb, value = listOf(100f.rf, 100f.rf)),
    )
val ellipseShape =
    Ellipse(
        position = StaticPositionProperty(animated = false.rb, value = Point(0f.rf, 0f.rf)),
        size = StaticVectorProperty(animated = false.rb, value = listOf(100f.rf, 100f.rf)),
    )
```

Other tests:
- [LottieScalingDiffScreenshotTest.kt](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/test/java/com/google/android/horologist/remotecompose/lottie/LottieScalingDiffScreenshotTest.kt#L144): Already passes `shape = ...` explicitly to `Path`.
- [ParsingTest.kt](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/test/java/com/google/android/horologist/remotecompose/lottie/ParsingTest.kt#L202): Deserializes JSON payloads with required fields present; passes without modification.

---

## 4. Execution Sequence

| Step | Action | Files | Verification Command |
| :--- | :--- | :--- | :--- |
| **1** | Create `GeometryShape` sealed interface and `ShapeDirection` enum. | `geometry/GeometryShape.kt` | `./gradlew :remotecompose:lottie:compileDebugKotlin` |
| **2** | Refactor `Ellipse` (implement `GeometryShape`, strictly schema attributes, no synthetic defaults). | `geometry/Ellipse.kt` | `./gradlew :remotecompose:lottie:compileDebugKotlin` |
| **3** | Refactor `Path` (implement `GeometryShape`, add `direction: Int? = null`, no synthetic defaults). | `geometry/Path.kt` | `./gradlew :remotecompose:lottie:compileDebugKotlin` |
| **4** | Refactor `Rectangle` (implement `GeometryShape`, nullable `cornerRadius`, no synthetic defaults). | `geometry/Rectangle.kt` | `./gradlew :remotecompose:lottie:compileDebugKotlin` |
| **5** | Refactor `PolyStar` (implement `GeometryShape`, schema default on `sy`, no synthetic defaults on required properties). | `geometry/PolyStar.kt` | `./gradlew :remotecompose:lottie:compileDebugKotlin` |
| **6** | Update `renderer/shapes/Ellipse.kt` to use `ShapeDirection.Reversed`. | `renderer/shapes/Ellipse.kt` | `./gradlew :remotecompose:lottie:compileDebugKotlin` |
| **7** | Update explicit constructor calls in `EvaluatorModularizationTest.kt`. | `src/test/.../EvaluatorModularizationTest.kt` | `./gradlew :remotecompose:lottie:testDebugUnitTest` |
| **8** | Run full verification suite. | Module `:remotecompose:lottie` | `./gradlew :remotecompose:lottie:check` |

---

## 5. Alternatives Considered

### 5.1 Providing Synthetic Convenience Defaults in Kotlin Constructors
- **Proposal**: Default required schema properties to empty static properties (e.g., `position: BasePositionProperty = StaticPositionProperty(Point(0f.rf, 0f.rf))`).
- **Why Rejected**:
  - Directly contradicts user requirement: *"pay attention to the default values defined in the json schema -- if there are no default values -- do not come up with ones!"*
  - Masking missing required fields during JSON deserialization hides malformed Lottie assets.
  - Required schema fields must be explicitly provided in both JSON payloads and test instantiations.

### 5.2 Including Extraneous Attributes (`ix`, `mn`, `cix`) from `prepear-for-merge`
- **Proposal**: Retain `val index: Int?`, `val matchName: String?`, and `val propertyIndex: Int?` as seen in `prepear-for-merge`.
- **Why Rejected**:
  - Neither `#/$defs/shapes/shape`, `#/$defs/shapes/graphic-element`, nor `#/$defs/helpers/visual-object` define `ix`, `mn`, or `cix` in `lottie.schema.json`.
  - Violates the rule: *"Stick to the attributes defined in the schema... Do not include types that are outside the schema."*
