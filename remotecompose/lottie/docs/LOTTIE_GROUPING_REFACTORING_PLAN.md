# Lottie AST Grouping (`format/graphicelement/grouping/`) Refactoring Plan

## 1. What: Scope & Objectives

This document specifies the refactoring plan for the grouping AST classes in `format/graphicelement/grouping/` ([Group.kt](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/grouping/Group.kt) and [Transform.kt](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/grouping/Transform.kt)) within the `:remotecompose:lottie` module.

The refactoring strictly adheres to the canonical schema definitions and user directives:
- [Lottie Group Specification](https://lottie.github.io/lottie-spec/latest/specs/shapes/#group) (`#/$defs/shapes/group`)
- [Lottie Transform Specification](https://lottie.github.io/lottie-spec/latest/specs/shapes/#transform) (`#/$defs/shapes/transform`)

### Key Objectives & Core Rules
1. **Strict Schema Attribute Conformance:**
   - [Group.kt](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/grouping/Group.kt) models exclusively:
     - `"nm"`: Name (`String? = null`)
     - `"hd"`: Hidden (`SerializableBoolean? = null`)
     - `"ty"`: Shape Type (`ShapeType = ShapeType.Group`)
     - `"np"`: Number Of Properties (`SerializableRemoteFloat? = null`)
     - `"it"`: Shapes (`List<GraphicElement>`)
   - [Transform.kt](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/grouping/Transform.kt) models exclusively:
     - `"nm"`: Name (`String? = null`)
     - `"hd"`: Hidden (`SerializableBoolean? = null`)
     - `"ty"`: Shape Type (`ShapeType = ShapeType.Transform`)
     - `"a"`: Anchor Point (`BasePositionProperty`)
     - `"p"`: Position / Translation (`BasePositionProperty`)
     - `"r"`: Rotation (`BaseScalarProperty`)
     - `"s"`: Scale (`BaseVectorProperty`)
     - `"o"`: Opacity (`BaseScalarProperty`)
     - `"sk"`: Skew (`BaseScalarProperty? = null`)
     - `"sa"`: Skew Axis (`BaseScalarProperty? = null`)
   - Non-schema attributes (such as `ix`, `mn`, `cix`, `so`, `eo`) are **strictly excluded**.
2. **No Default Values Unless Stated in Specs:**
   - If a field is required in the specification or lacks a schema-defined default, it **must not** have a default value in Kotlin:
     - `shapes` in `Group` has **no** default value (`val shapes: List<GraphicElement>`).
     - `anchorPoint`, `positionTranslation`, `rotation`, `scale`, `opacity` in `Transform` have **no** default values.
   - Default values are permitted only when explicitly defined by the schema (e.g. constant discriminator `"ty"`: `ShapeType.Group` / `ShapeType.Transform`) or for optional schema fields without defaults (`null`).
3. **Remote-First State Types:**
   - Visibility flag [hidden] is typed as [SerializableBoolean] (wrapping [RemoteBoolean](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/values/SerializableRemoteBoolean.kt)).
   - Property count [numberOfProperties] is typed as [SerializableRemoteFloat](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/values/SerializableRemoteFloat.kt).
   - Coordinates ([anchorPoint], [positionTranslation]) use [BasePositionProperty](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/properties/Position.kt) holding [Point](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/values/Point.kt).
   - Scalars ([rotation], [opacity], [skew], [skewAxis]) use [BaseScalarProperty](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/properties/Scalar.kt).
   - Scale factor ([scale]) uses [BaseVectorProperty](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/properties/Vector.kt).
4. **Clean Data Classes (Empty Class Bodies):**
   - AST data classes must be clean data holders without methods, constructors, getters, setters, or conversion functions in the class body.
   - Any conversions (such as extracting constant values or booleans) must occur at call sites where client code consumes the AST.
5. **Test Scope Policy:**
   - In accordance with user instructions, no new test files will be generated; only existing test call sites will be updated if needed.

---

## 2. Glossary & Ubiquitous Language

- **Shape Group (`"ty": "gr"`):** A composite graphic element containing an ordered collection of child graphic elements (`"it"`), providing isolated visual scoping for styles and spatial scoping for transforms.
- **Transform Shape (`"ty": "tr"`):** An affine spatial mapping node specifying anchor point (`"a"`), position translation (`"p"`), scale (`"s"`), rotation angle (`"r"`), opacity (`"o"`), and skew parameters (`"sk"`, `"sa"`).
- **Anchor Point (`"a"`):** The spatial origin (relative to parent space) around which rotation, scaling, and skewing are applied.
- **Position Translation (`"p"`):** The 2D offset mapping the anchor point into the parent coordinate space.
- **Skew (`"sk"`):** Angular distortion applied to the coordinate system in degrees.
- **Skew Axis (`"sa"`):** Direction angle along which skew distortion is applied in degrees (`0` skews along the X axis, `90` along the Y axis).
- **SerializableBoolean:** Typealias binding [RemoteBoolean](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/values/SerializableRemoteBoolean.kt) to [BooleanRemoteSerializer](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/values/BooleanRemoteSerializer.kt).

---

## 3. How: Schema Analysis & Architecture

### 3.1 Strict Schema Definition Analysis

#### [Group Schema (`#/$defs/shapes/group`)](https://lottie.github.io/lottie-spec/latest/specs/shapes/#group)
- Inherits from: `#/$defs/shapes/graphic-element` (which inherits from `#/$defs/helpers/visual-object`)
- Schema Attributes:
  - `"nm"`: `string` (Human readable name)
  - `"hd"`: `boolean` (Whether the shape is hidden)
  - `"ty"`: `string = "gr"` (Shape Type discriminator)
  - `"np"`: `number` (Number Of Properties)
  - `"it"`: `array of Graphic Element` (Shapes)
- Total attributes in schema: 5.

#### [Transform Shape Schema (`#/$defs/shapes/transform`)](https://lottie.github.io/lottie-spec/latest/specs/shapes/#transform)
- Inherits from: `#/$defs/shapes/graphic-element` (`nm`, `hd`, `ty`)
- Inherits from: `#/$defs/helpers/transform` (`a`, `p`, `r`, `s`, `o`, `sk`, `sa`)
- Schema Attributes:
  - `"nm"`: `string` (Human readable name)
  - `"hd"`: `boolean` (Whether the shape is hidden)
  - `"ty"`: `string = "tr"` (Shape Type discriminator)
  - `"a"`: `Position` (Anchor Point)
  - `"p"`: `Splittable Position` (Position / Translation)
  - `"r"`: `Scalar` (Rotation in degrees, clockwise)
  - `"s"`: `Vector` (Scale factor, `[100, 100]` for no scaling)
  - `"o"`: `Scalar` (Opacity)
  - `"sk"`: `Scalar` (Skew amount as an angle in degrees)
  - `"sa"`: `Scalar` (Skew Axis direction along which skew is applied, in degrees)
- Total attributes in schema: 10.

### 3.2 Evaluation of Defaults Against Specification

| Class | Property | Spec Description | Default Defined in Spec? | Target Kotlin Signature | Default in Kotlin |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Group** | `"nm"` | Human readable name | No | `override val name: String?` | `null` |
| | `"hd"` | Whether shape is hidden | No | `override val hidden: SerializableBoolean?` | `null` |
| | `"ty"` | Shape Type constant (`"gr"`) | Yes (`"gr"`) | `override val type: ShapeType` | `ShapeType.Group` |
| | `"np"` | Number of properties | No | `val numberOfProperties: SerializableRemoteFloat?` | `null` |
| | `"it"` | Shapes array | No | `val shapes: List<GraphicElement>` | **None** |
| **Transform** | `"nm"` | Human readable name | No | `override val name: String?` | `null` |
| | `"hd"` | Whether shape is hidden | No | `override val hidden: SerializableBoolean?` | `null` |
| | `"ty"` | Shape Type constant (`"tr"`) | Yes (`"tr"`) | `override val type: ShapeType` | `ShapeType.Transform` |
| | `"a"` | Anchor Point | No | `val anchorPoint: BasePositionProperty` | **None** |
| | `"p"` | Position / Translation | No | `val positionTranslation: BasePositionProperty` | **None** |
| | `"r"` | Rotation | No | `val rotation: BaseScalarProperty` | **None** |
| | `"s"` | Scale | No | `val scale: BaseVectorProperty` | **None** |
| | `"o"` | Opacity | No | `val opacity: BaseScalarProperty` | **None** |
| | `"sk"` | Skew | No | `val skew: BaseScalarProperty?` | `null` |
| | `"sa"` | Skew Axis | No | `val skewAxis: BaseScalarProperty?` | `null` |

---

## 4. Current State Audit & Gaps in `format/graphicelement/grouping/`

### 4.1 Current Codebase Audit

1. **[Group.kt](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/grouping/Group.kt):**
   ```kotlin
   @Serializable
   internal data class Group(
     @SerialName("nm") override val name: String? = "",
     @SerialName("hd") override val hidden: Boolean? = false,
     @SerialName("ty") override val type: ShapeType = ShapeType.Group,
     @SerialName("np") val numberOfProperties: Int? = null,
     @SerialName("it") val shapes: List<GraphicElement>,
   ) : GraphicElement
   ```
   - **Gaps:**
     - `hidden` uses primitive `Boolean?` instead of `SerializableBoolean`.
     - `numberOfProperties` uses primitive `Int?` instead of RemoteCompose state type `SerializableRemoteFloat?`.
     - Missing KDoc specification references and contracts.

2. **[Transform.kt](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/grouping/Transform.kt):**
   ```kotlin
   @Serializable
   internal data class Transform(
     @SerialName("nm") override val name: String? = "",
     @SerialName("hd") override val hidden: Boolean? = false,
     @SerialName("ty") override val type: ShapeType = ShapeType.Transform,
     @SerialName("a")
     val anchorPoint: BasePositionProperty =
       StaticPositionProperty(animated = false.rb, value = Point(0f.rf, 0f.rf)),
     @SerialName("p")
     val positionTranslation: BasePositionProperty =
       StaticPositionProperty(animated = false.rb, value = Point(0f.rf, 0f.rf)),
     @SerialName("r")
     val rotation: BaseScalarProperty = StaticScalarProperty(animated = false.rb, value = 0f.rf),
     @SerialName("s")
     val scale: BaseVectorProperty =
       StaticVectorProperty(animated = false.rb, value = listOf(100f.rf, 100f.rf)),
     @SerialName("o")
     val opacity: BaseScalarProperty = StaticScalarProperty(animated = false.rb, value = 100f.rf),
   ) : GraphicElement
   ```
   - **Gaps:**
     - Contains artificial default values for `a`, `p`, `r`, `s`, `o` not defined in the specification.
     - `hidden` uses primitive `Boolean?` instead of `SerializableBoolean`.
     - Missing schema-defined skew parameters: `"sk"` (skew) and `"sa"` (skew axis).
     - Missing formal KDoc contracts.

---

## 5. Target Implementation Specification

### 5.1 Target `Group.kt`

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

package com.google.android.horologist.remotecompose.lottie.format.graphicelement.grouping

import com.google.android.horologist.remotecompose.lottie.format.graphicelement.GraphicElement
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.ShapeType
import com.google.android.horologist.remotecompose.lottie.format.values.SerializableBoolean
import com.google.android.horologist.remotecompose.lottie.format.values.SerializableRemoteFloat
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A group of graphic elements providing isolated styling and transformation scoping, conforming to
 * [Lottie Group](https://lottie.github.io/lottie-spec/latest/specs/shapes/#group)
 * (`#/$defs/shapes/group`).
 *
 * Essential Invariants:
 * - Scoping Boundary: A Group acts as an isolated visual and spatial container. Shapes defined
 *   within [shapes] are styled exclusively by styles preceding them within the same group, and
 *   transformed by the group's trailing [Transform] element.
 * - Array Ordering: The items array [shapes] is evaluated in reverse order during rendering:
 *   trailing transforms apply to all preceding elements, and styles apply to all preceding
 *   geometry shapes.
 * - Transform Locality: A Group must contain at most one trailing [Transform] element. If present,
 *   it must be the final entry in [shapes].
 * - Nested Hierarchy: Groups may be nested arbitrarily to compose hierarchical transformation
 *   matrices and compound vector shapes.
 *
 * Schema Specification:
 * - Required Fields:
 *   - `"ty"`: Shape type discriminator, constantly `"gr"`.
 *   - `"it"`: Ordered array of child graphic elements.
 * - Optional Fields without Schema Defaults:
 *   - `"nm"`: Display name of the group.
 *   - `"hd"`: Hidden boolean flag suppressing rendering.
 *   - `"np"`: Number of properties within the group.
 *
 * @property name Human readable name of the group.
 * @property hidden When true, suppresses rendering of this group and all its children.
 * @property type Shape discriminator, constantly [ShapeType.Group].
 * @property numberOfProperties Number of properties contained in this group.
 * @property shapes Ordered collection of child graphic elements (geometries, styles, and transform).
 */
@Serializable
internal data class Group(
  @SerialName("nm") override val name: String? = null,
  @SerialName("hd") override val hidden: SerializableBoolean? = null,
  @SerialName("ty") override val type: ShapeType = ShapeType.Group,
  @SerialName("np") val numberOfProperties: SerializableRemoteFloat? = null,
  @SerialName("it") val shapes: List<GraphicElement>,
) : GraphicElement
```

---

### 5.2 Target `Transform.kt`

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

package com.google.android.horologist.remotecompose.lottie.format.graphicelement.grouping

import com.google.android.horologist.remotecompose.lottie.format.graphicelement.GraphicElement
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.ShapeType
import com.google.android.horologist.remotecompose.lottie.format.properties.BasePositionProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.BaseScalarProperty
import com.google.android.horologist.remotecompose.lottie.format.properties.BaseVectorProperty
import com.google.android.horologist.remotecompose.lottie.format.values.SerializableBoolean
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Group transform element applying affine spatial transformations to surrounding graphic elements,
 * conforming to [Lottie Transform Shape](https://lottie.github.io/lottie-spec/latest/specs/shapes/#transform)
 * (`#/$defs/shapes/transform`).
 *
 * Essential Invariants:
 * - Mathematical Order of Application: Transformation components are concatenated to form the
 *   local matrix M using standard 2D affine composition:
 *   M = Translate(p) * Rotate(r) * Skew(sk, sa) * Scale(s) * Translate(-a)
 *   Points transformed by M are first shifted by the negative anchor point (-a), scaled, skewed,
 *   rotated, and finally translated by the position offset (p).
 * - Opacity Concatenation: Opacity scales the alpha channel of all preceding elements within the
 *   current group scope: Alpha_effective = Alpha_element * (opacity / 100.0).
 * - Shape Group Placement: Within a shape [Group], Transform must be the trailing element in the
 *   child shapes array.
 *
 * Schema Specification:
 * - Required Fields:
 *   - `"ty"`: Shape type discriminator, constantly `"tr"`.
 *   - `"a"`: Anchor point position property.
 *   - `"p"`: Position translation property.
 *   - `"r"`: Clockwise rotation in degrees.
 *   - `"s"`: Scale percentage vector.
 *   - `"o"`: Overall opacity percentage.
 * - Optional Fields without Schema Defaults:
 *   - `"nm"`: Display name of the transform.
 *   - `"hd"`: Hidden boolean flag suppressing application of this transform.
 *   - `"sk"`: Skew angle in degrees.
 *   - `"sa"`: Skew axis angle in degrees.
 *
 * @property name Human readable name of the transform.
 * @property hidden When true, suppresses application of this transform.
 * @property type Shape discriminator, constantly [ShapeType.Transform].
 * @property anchorPoint Anchor point around which transformations are applied.
 * @property positionTranslation Spatial offset mapping the anchor point into the parent space.
 * @property rotation Clockwise rotation angle in degrees.
 * @property scale Percentage scale vector along X and Y axes.
 * @property opacity Alpha multiplier on [0.0, 100.0] applied to rendered contents.
 * @property skew Skew angle in degrees distorting the coordinate space along [skewAxis].
 * @property skewAxis Direction angle in degrees along which [skew] distortion is applied.
 */
@Serializable
internal data class Transform(
  @SerialName("nm") override val name: String? = null,
  @SerialName("hd") override val hidden: SerializableBoolean? = null,
  @SerialName("ty") override val type: ShapeType = ShapeType.Transform,
  @SerialName("a") val anchorPoint: BasePositionProperty,
  @SerialName("p") val positionTranslation: BasePositionProperty,
  @SerialName("r") val rotation: BaseScalarProperty,
  @SerialName("s") val scale: BaseVectorProperty,
  @SerialName("o") val opacity: BaseScalarProperty,
  @SerialName("sk") val skew: BaseScalarProperty? = null,
  @SerialName("sa") val skewAxis: BaseScalarProperty? = null,
) : GraphicElement
```

---

### 5.3 Alignment in `GraphicElement.kt`

To support `RemoteBoolean` on `Group` and `Transform`, `GraphicElement.kt` will align its `hidden` declaration:
```kotlin
internal interface GraphicElement {
  val name: String?
  val hidden: SerializableBoolean?
  val type: ShapeType
}
```

---

## 6. Call Sites & Conversion Locations

In strict accordance with the rule that AST classes must not contain conversion functions or getters in their class body, all conversions are performed at the respective call sites:

### 6.1 `renderer/Shape.kt`
- **Visibility check:**
  ```kotlin
  // In group()
  if (group.hidden?.constantValue == true) {
    return null
  }
  ```
- **Shape extraction:**
  `group.shapes.reversed()` remains directly usable.

### 6.2 `renderer/Transform.kt`
- Direct access to `transform.rotation`, `transform.positionTranslation`, `transform.opacity`, `transform.anchorPoint`, `transform.scale` continues without change.

### 6.3 `renderer/layers/ShapeLayer.kt`
- `layer.hidden.constantValue` handles the `SerializableBoolean` conversion as currently implemented.

### 6.4 `LottieScalingDiffScreenshotTest.kt`
- At lines 180, 229, and 251, calls to `Transform(...)` are updated to provide explicit values for required properties (`anchorPoint`, `positionTranslation`, `rotation`, `scale`, `opacity`) instead of relying on removed default arguments:
  ```kotlin
  Transform(
    name = "Transform",
    anchorPoint = StaticPositionProperty(value = Point(0f.rf, 0f.rf)),
    positionTranslation = StaticPositionProperty(value = Point(cx.rf, cy.rf)),
    rotation = StaticScalarProperty(value = 0f.rf),
    scale = StaticVectorProperty(value = listOf(100f.rf, 100f.rf)),
    opacity = StaticScalarProperty(value = 100f.rf),
  )
  ```

---

## 7. Verification & Testing Strategy

In compliance with `../.agents/rules/testing_policy.md` and user instructions, verification will execute in module execution order without generating new test files:

1. **Static Analysis & Formatting:**
   ```bash
   ./gradlew :remotecompose:lottie:ktfmtFormat
   ```
2. **Kotlin Compilation Check:**
   ```bash
   ./gradlew :remotecompose:lottie:compileDebugKotlin
   ```
3. **Targeted Unit Test Verification:**
   ```bash
   ./gradlew :remotecompose:lottie:testDebugUnitTest --tests "com.google.android.horologist.remotecompose.lottie.ParsingTest"
   ./gradlew :remotecompose:lottie:testDebugUnitTest --tests "com.google.android.horologist.remotecompose.lottie.LottieDecoderResilienceTest"
   ```
4. **Roborazzi Screenshot Diff Verification:**
   ```bash
   ./gradlew :remotecompose:lottie:verifyRoborazziDebug
   ```
5. **Full Module Check:**
   ```bash
   ./gradlew :remotecompose:lottie:check
   ```

---

## 8. Alternatives Considered & Feedback Resolutions

### Alternative 1: Adding Default Values for Required Properties Without Spec Defaults
- **Proposal:** Provide `= emptyList()` for `shapes` in `Group`, and provide default `Static*Property` instances for `anchorPoint`, `positionTranslation`, `rotation`, `scale`, and `opacity` in `Transform`.
- **Pros:** Allows zero-argument constructor `Transform()` in manual test construction.
- **Cons:** Deviates from the canonical Lottie specification, hides missing required data in JSON payloads, and violates schema validation principles.
- **Decision: Rejected per user feedback.** Only include default values when explicitly defined in the official specification.

### Alternative 2: Adding Conversion Getters / Extension Functions in Class Body
- **Proposal:** Add properties like `val npInt: Int?`, `val isHidden: RemoteBoolean`, `val position`, `val staticAnchor` inside `Group` and `Transform` class bodies.
- **Pros:** Convenient shortcuts for callers.
- **Cons:** Pollutes AST data holders with business logic and incidental conversion code; couples data modeling with consumption logic.
- **Decision: Rejected per user feedback.** Keep class bodies empty. Conversions must be done at the call sites where the AST nodes are consumed.

### Alternative 3: Retaining Primitive `Boolean?` for `hidden`
- **Proposal:** Keep `hidden: Boolean? = false`.
- **Pros:** Avoids updating `GraphicElement.hidden` to `SerializableBoolean?`.
- **Cons:** Misses the opportunity to utilize native RemoteCompose reactive state types (`RemoteBoolean`) across the AST.
- **Decision: Rejected per user feedback.** Standardize on `SerializableBoolean?` across AST models.
