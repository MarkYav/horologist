# Lottie AST Layer Refactoring Plan

## Overview & Context

This document outlines the refactoring plan for classes inside the `format/layer/` directory of the `..` module, following the architectural paradigms, schema strictness, and RemoteCompose state type integration established during the property AST refactoring in `lottie-fix-AST-properties` (PR #2823, commit `10d8dd18b132bda7bf4c38d51a896bb09cf62572`).

---

## 1. Lessons & Patterns from `format/properties/` Refactoring

The refactoring across [Scalar.kt](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/properties/Scalar.kt), [Vector.kt](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/properties/Vector.kt), [Position.kt](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/properties/Position.kt), [Color.kt](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/properties/Color.kt), and [Bezier.kt](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/properties/Bezier.kt) established four primary tenets:

1. **Native RemoteCompose State Types**:
   - Replaced raw types (`Float`, `Boolean`, `FloatArray`) with RemoteCompose state types:
     - `RemoteFloat` / `SerializableRemoteFloat` for timestamps (`"t"`), scalars (`"k"`), and coordinate values.
     - `RemoteBoolean` / `SerializableRemoteBoolean` for integer-boolean flags (`"a"`, `"h"`).
     - `RemoteColor` / `SerializableRemoteColor` for RGBA vectors.
     - `Point` for 2D spatial coordinate vectors.
   - Eliminates runtime conversion adapters and per-frame wrapping allocations in the renderer.

2. **Strict Lottie 1.0.1 Specification Documentation & Invariants**:
   - Every file links directly to the canonical Lottie 1.0.1 specification (e.g. `https://lottie.github.io/lottie-spec/1.0.1/specs/...`).
   - KDoc includes formal sections:
     - **Essential Invariants**: Explaining sealed hierarchy, discriminators, and structural constraints.
     - **Schema Specification**: Enumerating Required Fields, Optional Fields with Schema Defaults, and Optional Fields without Defaults.
     - **Invariants**: Concrete guarantees for fields.
     - **Contract**: Preconditions, Postconditions, and Exceptions for polymorphic serializers.

3. **Schema Strictness & Exception Guarantees**:
   - Polymorphic serializers validate that incoming tokens are valid `JsonObject` instances (`element as? JsonObject ?: throw SerializationException(...)`).
   - Missing required schema fields or malformed discriminators throw `SerializationException` rather than silently defaulting to broken states.

4. **Boilerplate Reduction & Clean Architecture**:
   - Elimination of manual `equals()` and `hashCode()` overrides.
   - Elimination of intermediate wrapper fields (e.g., `animatedInt` alongside `override val animated: Boolean get() = animatedInt == 1`).

---

## 2. Current State & Gaps in `format/layer/`

Current files in `format/layer/`:
- [Layer.kt](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/layer/Layer.kt)
- [NullLayer.kt](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/layer/NullLayer.kt)
- [ShapeLayer.kt](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/layer/ShapeLayer.kt)
- [SolidColorLayer.kt](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/layer/SolidColorLayer.kt)

### Gaps:
1. **Primitive vs Remote Types**:
   - `startFrame` (`ip`) and `endFrame` (`op`) are raw `Int?`. In the Lottie 1.0.1 specification, frames are floating-point numbers supporting sub-frame animations. More critically, timeline evaluation in RemoteCompose compares frames against an active animation clock `RemoteFloat`. Representing frames as `RemoteFloat` enables zero-conversion evaluation.
   - `hidden` (`hd`) is raw `Boolean?`. Representing it as `RemoteBoolean` allows direct use in RemoteCompose conditional execution and matches property representations like `hold` and `animated`.
   - `solidWidth` (`sw`) and `solidHeight` (`sh`) in `SolidColorLayer` are raw `Float` rather than `RemoteFloat`.
2. **Missing Lottie 1.0.1 Specification Documentation**:
   - Classes lack official Lottie 1.0.1 specification URLs (`#/$defs/layers/layer`, `#/$defs/layers/visual-layer`, `#/$defs/layers/shape-layer`, `#/$defs/layers/solid-layer`, `#/$defs/layers/null-layer`).
   - Lacks formal KDoc sections (**Essential Invariants**, **Schema Specification**, **Invariants**, **Contract**).
3. **Serializer Safety**:
   - `LayerSerializer` directly accesses `element.jsonObject["ty"]`, which throws an unchecked `IllegalStateException` instead of `SerializationException` if given a non-object token.
4. **Resilience Requirement Preservation**:
   - [LottieDecoderResilienceTest.kt](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/test/java/com/google/android/horologist/remotecompose/lottie/LottieDecoderResilienceTest.kt#L38) enforces that unrecognized layer types (`ty: 999`) fall back to [NullLayer](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/layer/NullLayer.kt) so that child layers chained to unknown parents still receive their transform hierarchy. This resilience fallback must be maintained.

---

## 3. Detailed Target Architecture for `format/layer/`

### 3.1 Maximal Utilization of Remote Types in Abstract `Layer` Properties

The abstract [Layer](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/layer/Layer.kt) class maximizes the use of RemoteCompose state types across temporal, dimensional, visibility, and boolean attributes while strictly adhering to Lottie 1.0.1 JSON schema defaults and required constraints:

| Property      | Lottie Key | Lottie Type     | Required in Schema? | Schema Default | Target Remote / Kotlin Type  | Kotlin Default | Rationale & Semantics                                              |
| :------------ | :--------- | :-------------- | :------------------ | :------------- | :--------------------------- | :------------- | :----------------------------------------------------------------- |
| `name`        | `"nm"`     | string          | No                  | None           | `String?`                    | `= null`       | Non-rendering metadata and debug identifier.                       |
| `hidden`      | `"hd"`     | boolean         | No                  | `false`        | `SerializableBoolean`        | `= false.rb`   | Visual visibility flag; parsed via BooleanRemoteSerializer.       |
| `type`        | `"ty"`     | integer         | Yes                 | *(const)*      | `LayerType`                  | *(const)*      | Discrete integer discriminator enum.                               |
| `index`       | `"ind"`    | integer         | No                  | None           | `Int?`                       | `= null`       | Graph node ID for parenting and matte resolution.                  |
| `parent`      | `"parent"` | integer         | No                  | None           | `Int?`                       | `= null`       | Parent layer node ID in transform tree hierarchy.                  |
| `startFrame`  | `"ip"`     | number          | Yes                 | None           | `SerializableRemoteFloat`    | **None**       | In-point timeline frame; required in schema, evaluated on clock.   |
| `endFrame`    | `"op"`     | number          | Yes                 | None           | `SerializableRemoteFloat`    | **None**       | Out-point timeline frame; required in schema, evaluated on clock.  |
| `transform`   | `"ks"`     | transform       | Yes (visual)        | None           | `Transform?`                 | `= null`       | Spatial 2D transform (anchor, position, scale, rotation, opacity). |
| `autoOrient`  | `"ao"`     | integer-boolean | No                  | `0`            | `SerializableRemoteBoolean`  | `= false.rb`   | Automatic rotation along curved motion paths.                      |
| `matteMode`   | `"tt"`     | integer         | No                  | `0`            | `MatteMode`                  | `= Normal`     | Track matte mode for layer compositing (Normal, Alpha, etc.).      |
| `matteParent` | `"tp"`     | integer         | No                  | None           | `Int?`                       | `= null`       | Index of the layer used as matte (defaults to layer directly above). |
| `masks`       | `"masksProperties"` | array  | No                  | None           | `List<Mask>?`                | `= null`       | Optional array of clipping and compositing masks applied to layer. |

#### Concrete Layer Class Specific Properties

| Layer Subclass    | `ty` | Additional Property | Required in Schema? | Schema Default | Target Remote / Kotlin Type | Kotlin Default | Description                                       |
| :---------------- | :--- | :------------------ | :------------------ | :------------- | :-------------------------- | :------------- | :------------------------------------------------ |
| `NullLayer`       | `3`  | *(none)*            | —                   | —              | —                           | —              | Non-rendering transform node in hierarchy tree.   |
| `ShapeLayer`      | `4`  | `shapes`            | Yes                 | None           | `List<GraphicElement>`      | **None**       | Visual layer rendering vector geometry elements.  |
| `SolidColorLayer` | `1`  | `solidColor`        | Yes                 | None           | `SerializableHexColor`      | **None**       | Rectangle fill color (hex string).               |
|                   |      | `solidWidth`        | Yes                 | None           | `SerializableRemoteInt`     | **None**       | Canvas rectangle width in integer pixels.        |
|                   |      | `solidHeight`       | Yes                 | None           | `SerializableRemoteInt`     | **None**       | Canvas rectangle height in integer pixels.       |

### 3.2 Analysis: `SerializableRemoteBoolean` vs `hidden` (`hd`)

- **Integer Boolean Properties (`ao`)**:
  - In Lottie 1.0.1, `"ao"` (Auto Orient) is defined as `#/$defs/values/int-boolean`, taking integer tokens `0` or `1`.
  - For this field, `SerializableRemoteBoolean` (bound to `IntBooleanRemoteSerializer`) is used directly. The schema specifies `"default": 0`, so it defaults to `= false.rb`.
- **Hidden Visibility Flag (`hd`)**:
  - In Lottie 1.0.1, `"hd"` is defined as `"type": "boolean"` (standard JSON boolean primitives `true` / `false`), which is what AE and Lottie exporters output (e.g. `"hd": false`).
  - `SerializableRemoteBoolean` cannot be used for `"hd"` because its underlying `IntBooleanRemoteSerializer` calls `decoder.decodeInt()` and strictly expects integers `0` or `1`, throwing a `JsonDecodingException` on standard JSON boolean literals (`true`/`false`).
  - Therefore, `hidden` is typed as `override val hidden: Boolean = false`, exactly matching the Lottie specification schema (`"type": "boolean"`), avoiding unnecessary serializer overhead, and staying completely consistent with all other AST models across the codebase ([Transform](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/grouping/Transform.kt), [GraphicElement](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/GraphicElement.kt), [Group](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/grouping/Group.kt), etc.).

---

### 3.3 Refactoring `Layer.kt`

```kotlin
/**
 * Base class for all Lottie animation layers conforming to
 * [Lottie Layers](https://lottie.github.io/lottie-spec/1.0.1/specs/layers/#common-properties)
 * and [Visual Layer](https://lottie.github.io/lottie-spec/1.0.1/specs/layers/#visual-layer).
 *
 * Layers are independent visual, temporal, and spatial nodes arranged in a compositing tree.
 *
 * Essential Invariants:
 * - Discriminator: Partitioned by integer [type] ([Layer Type](https://lottie.github.io/lottie-spec/1.0.1/specs/constants/#layer-type)).
 * - Parenting Hierarchy: Child layer transforms are concatenated with their parent's current
 *   transformation matrix: CTM(child) = CTM(parent) * Transform(child).
 * - Timeline Visibility Window: A layer is active on frame t when ip <= t < op.
 * - Hidden Layers: [hidden] (hd) suppresses direct rendering while retaining participation
 *   in parenting and track matte hierarchies.
 */
@Serializable(with = LayerSerializer::class)
internal sealed class Layer {
  abstract val name: String?
  abstract val hidden: SerializableBoolean
  abstract val type: LayerType
  abstract val index: Int?
  abstract val parent: Int?
  abstract val startFrame: SerializableRemoteFloat
  abstract val endFrame: SerializableRemoteFloat
  abstract val transform: Transform?
  abstract val autoOrient: SerializableRemoteBoolean
  abstract val matteMode: MatteMode
  abstract val matteParent: Int?
  abstract val masks: List<Mask>?
}
```

#### Canonical `LayerType` Enum
Expanded with official Lottie 1.0.1 layer types:
- `Precomposition(0)`
- `Solid(1)`
- `Image(2)`
- `Null(3)`
- `Shape(4)`
- `Text(5)`
- `Audio(6)`

#### `LayerSerializer` Contract
```kotlin
/**
 * Polymorphic serializer for [Layer] discriminating on the integer "ty" field per
 * [Layer Type](https://lottie.github.io/lottie-spec/1.0.1/specs/constants/#layer-type).
 *
 * Contract:
 * - Preconditions: [element] must be a [JsonObject].
 * - Postconditions:
 *     - Selects [SolidColorLayer.serializer] when "ty" is 1.
 *     - Selects [NullLayer.serializer] when "ty" is 3.
 *     - Selects [ShapeLayer.serializer] when "ty" is 4.
 *     - Falls back to [NullLayer.serializer] for unrecognized or unsupported layer types,
 *       preserving transform parenting chains without crashing animation decoding.
 * - Exceptions:
 *     - Throws [SerializationException] if [element] is not a [JsonObject].
 *     - Throws [SerializationException] if "ty" is missing or not an integer.
 */
```

---

### 3.4 Refactoring `NullLayer.kt`

- **Spec URL**: [Null Layer](https://lottie.github.io/lottie-spec/1.0.1/specs/layers/#null-layer)
- **Role**: Non-rendering structural node used for parenting and hierarchical transform grouping.
- **Model**:
```kotlin
@Serializable
internal data class NullLayer(
  @SerialName("nm") override val name: String? = null,
  @SerialName("hd") override val hidden: SerializableBoolean = false.rb,
  @SerialName("ty") override val type: LayerType = LayerType.Null,
  @SerialName("ind") override val index: Int? = null,
  @SerialName("parent") override val parent: Int? = null,
  @SerialName("ip") override val startFrame: SerializableRemoteFloat,
  @SerialName("op") override val endFrame: SerializableRemoteFloat,
  @SerialName("ks") override val transform: Transform? = null,
  @SerialName("ao") override val autoOrient: SerializableRemoteBoolean = false.rb,
  @SerialName("tt") override val matteMode: MatteMode = MatteMode.Normal,
  @SerialName("tp") override val matteParent: Int? = null,
  @SerialName("masksProperties") override val masks: List<Mask>? = null,
) : Layer()
```

---

### 3.5 Refactoring `ShapeLayer.kt`

- **Spec URL**: [Shape Layer](https://lottie.github.io/lottie-spec/1.0.1/specs/layers/#shape-layer)
- **Role**: Visual layer rendering vector geometry items (`shapes`).
- **Model**:
```kotlin
@Serializable
internal data class ShapeLayer(
  @SerialName("nm") override val name: String? = null,
  @SerialName("hd") override val hidden: SerializableBoolean = false.rb,
  @SerialName("ty") override val type: LayerType = LayerType.Shape,
  @SerialName("ind") override val index: Int? = null,
  @SerialName("parent") override val parent: Int? = null,
  @SerialName("ip") override val startFrame: SerializableRemoteFloat,
  @SerialName("op") override val endFrame: SerializableRemoteFloat,
  @SerialName("ks") override val transform: Transform? = null,
  @SerialName("ao") override val autoOrient: SerializableRemoteBoolean = false.rb,
  @SerialName("tt") override val matteMode: MatteMode = MatteMode.Normal,
  @SerialName("tp") override val matteParent: Int? = null,
  @SerialName("masksProperties") override val masks: List<Mask>? = null,
  @SerialName("shapes") val shapes: List<GraphicElement>,
) : Layer()
```

---

### 3.6 Refactoring `SolidColorLayer.kt`

- **Spec URL**: [Solid Layer](https://lottie.github.io/lottie-spec/1.0.1/specs/layers/#solid-layer)
- **Role**: Visual layer rendering a solid color rectangle of dimensions `solidWidth` by `solidHeight`.
- **Remote Types**:
  - `solidWidth`: `SerializableRemoteInt` (required, no default)
  - `solidHeight`: `SerializableRemoteInt` (required, no default)
  - `solidColor`: `SerializableHexColor` (required, no default)
- **Model**:
```kotlin
@Serializable
internal data class SolidColorLayer(
  @SerialName("nm") override val name: String? = null,
  @SerialName("hd") override val hidden: SerializableBoolean = false.rb,
  @SerialName("ty") override val type: LayerType = LayerType.Solid,
  @SerialName("ind") override val index: Int? = null,
  @SerialName("parent") override val parent: Int? = null,
  @SerialName("ip") override val startFrame: SerializableRemoteFloat,
  @SerialName("op") override val endFrame: SerializableRemoteFloat,
  @SerialName("ks") override val transform: Transform? = null,
  @SerialName("ao") override val autoOrient: SerializableRemoteBoolean = false.rb,
  @SerialName("tt") override val matteMode: MatteMode = MatteMode.Normal,
  @SerialName("tp") override val matteParent: Int? = null,
  @SerialName("masksProperties") override val masks: List<Mask>? = null,
  @SerialName("sw") val solidWidth: SerializableRemoteInt,
  @SerialName("sh") val solidHeight: SerializableRemoteInt,
  @SerialName("sc") val solidColor: SerializableHexColor,
) : Layer()
```

---

## 4. Downstream Updates & Existing Tests

Per the instruction, **no separate new test files** will be created; only existing tests and call sites will be updated where types have evolved.

2. **Renderer Updates**:
   - [renderer/layers/Layer.kt](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/layers/Layer.kt): Add `else -> {}` to `when (layer.type)` to handle non-visual or future layer enum values.
   - [renderer/layers/ShapeLayer.kt](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/layers/ShapeLayer.kt): Check `if (layer.hidden.constantValue) return`.
2. **Existing Test Updates**:
   - [LottieScalingDiffScreenshotTest.kt](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/test/java/com/google/android/horologist/remotecompose/lottie/LottieScalingDiffScreenshotTest.kt#L249): Update manual constructor invocations from `startFrame = 0, endFrame = 60` to `startFrame = 0f.rf, endFrame = 60f.rf`.
   - Update [LottieDecoderResilienceTest.kt](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/test/java/com/google/android/horologist/remotecompose/lottie/LottieDecoderResilienceTest.kt) test cases to supply required `"ip"`/`"op"` fields where omitted in hand-crafted JSON snippets.
   - Verify that [ParsingTest.kt](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/test/java/com/google/android/horologist/remotecompose/lottie/ParsingTest.kt) and other test suites compile and pass.

---

## 5. Execution Steps

| Step | Phase                      | Target Files          | Description                                                               |
| :--- | :------------------------- | :-------------------- | :------------------------------------------------------------------------ |
| 1    | **Base Layer Refactoring** | `format/layer/`       | Refactor `Layer.kt` (KDocs, Remote types, `LayerType`, strict serializer).|
| 2    | **NullLayer Refactoring**  | `format/layer/`       | Refactor `NullLayer.kt` (KDocs, Remote types, schema defaults).           |
| 3    | **ShapeLayer Refactoring** | `format/layer/`       | Refactor `ShapeLayer.kt` (KDocs, Remote types, vector shapes).            |
| 4    | **SolidColor Refactoring** | `format/layer/`       | Refactor `SolidColorLayer.kt` (KDocs, Remote dimensions).                 |
| 5    | **Renderer Integration**   | `renderer/layers/`    | Update `when (layer.type)` and `layer.hidden` checks.                     |
| 6    | **Existing Tests Update**  | `../src/test`           | Update `LottieScalingDiffScreenshotTest.kt` and `LottieDecoderResilienceTest.kt`. |
| 7    | **Verification**           | Gradle test runner    | Run `./gradlew :remotecompose:lottie:testDebugUnitTest` to verify.        |
