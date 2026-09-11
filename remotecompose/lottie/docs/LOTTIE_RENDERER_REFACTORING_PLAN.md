# Lottie Renderer (`renderer/`) Refactoring Design Document & Migration Plan

## 1. What: Scope & Objectives

This document specifies the architectural design and incremental migration plan for refactoring the rendering engine in `remotecompose/lottie/renderer/`.

The objective is to re-implement the Lottie rendering pipeline to consume the canonical Lottie 1.0.1 AST models in `format/`, while eliminating the architectural flaws of the fork implementation in `prepear-for-merge` branch.

### Key Objectives
1. **Decoupled 3-Stage Pipeline:** Separate AST traversal, modifier transformation, and RemoteCompose canvas emission into distinct, testable phases.
2. **Dual-Path Property Evaluation:** Distinguish constant leaf values from keyframed animations: pre-bake static paths (`RemotePath`) and constant `RemoteFloat` / `RemoteColor` values during composition, reserving multi-node `selectIfLt` / `lerp` expression trees strictly for keyframed animations.
3. **Encapsulated Render State:** Replace 8-parameter recursive calls with an immutable [RenderContext](#31-rendercontext-state-encapsulation) carrying transforms, opacity, timeline settings, and active masks.
4. **Complete Style Support:** Implement full rendering for all sealed [ShapeStyle](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/styles/ShapeStyle.kt) variants ([Fill](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/styles/Fill.kt), [Stroke](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/styles/Stroke.kt), [GradientFill](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/styles/GradientFill.kt), [GradientStroke](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/styles/GradientStroke.kt)), cascading opacity down to `RemotePaint`.
5. **Incremental PR Roadmap:** Execute the migration in 7 atomic, self-verifying pull requests with zero regressions.

---

## 2. Context: Lottie AST Representation & Data Layout

Understanding how the Lottie format encodes animations in the AST is essential for designing the renderer. The Lottie format relies on flat arrays with implicit relational links rather than deeply nested object trees.

### 2.1 Flat Layer Hierarchy
At the composition root ([Animation](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/Animation.kt)), layers are stored as a **single flat list** (`layers: List<Layer>`), rather than a nested tree:
- **Parent-Child Transform Links:** Hierarchical parenting is not structural; it is encoded via numeric pointers. A child layer defines a `parent: Int?` field containing the `index: Int` of its parent layer. The renderer must build a topological ancestor chain (`buildAncestorTransforms`) to resolve the compounded canvas transforms for each layer.
- **Track Matte Relationships:** Layer masking (track mattes) is defined by sequential adjacency or explicit index pointers (`matteTarget`, `matteParent`, `matteMode`). A layer can serve as an alpha or luminance clipping mask for the preceding layer in the flat list.
- **Timeline Intervals:** Each layer specifies independent timeline bounds via `startFrame` (`ip`) and `endFrame` (`op`), along with optional `startTime` and `timeStretch` scalars that remap the layer's local timeline relative to the root composition clock.

### 2.2 Flat Graphic Element Sequencing Inside Groups
Within each [ShapeLayer](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/layer/ShapeLayer.kt) and [Group](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/grouping/Group.kt), elements are also represented as a **heterogeneous flat list** (`shapes: List<GraphicElement>`):
- **Decoupled Geometries and Styles:** Unlike vector formats such as SVG where shapes own their styling attributes (e.g. `<path fill="red" stroke="blue"/>`), Lottie separates geometry descriptors ([Rectangle](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/geometry/Rectangle.kt), [Ellipse](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/geometry/Ellipse.kt), [Path](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/geometry/Path.kt), [PolyStar](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/geometry/PolyStar.kt)) from visual presentation styles ([Fill](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/styles/Fill.kt), [Stroke](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/styles/Stroke.kt), [GradientFill](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/styles/GradientFill.kt), [GradientStroke](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/styles/GradientStroke.kt)).
- **Scope-Based Paint Triggers:** A style element consumes and paints all unstyled geometry nodes that precede that style element within the current group scope. Multiple consecutive styles (such as a `Fill` followed by a `Stroke`) paint the identical preceding geometry set multiple times with different paint properties.
- **Interspersed Procedural Modifiers:** Shape modifiers (`TrimPath`, `Repeater`, `RoundedCorners`, `MergePaths`) sit as sibling entries in the same flat list, mutating the geometry buffer before any subsequent style consumes it.

### 2.3 Stacking Order (Painter's Algorithm)

The overall animation document forms a hierarchical tree of layers and nested groups, terminating in flat element sequences:

```
Animation
  └── Layer
        └── Group
              ├── Group (nested child)
              └── [Rectangle, Ellipse, Fill, Stroke] (elements sequence)
```

#### Hierarchy & Stacking Principles
1. **Layer Hierarchy:** At the composition root, [Animation](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/Animation.kt) contains a flat `layers: List<Layer>`. Compounded layer transforms are resolved via numeric `parent` pointers.
2. **Group Hierarchy:** Within each layer or group, children can be container groups (which maintain their own local transforms and coordinate systems) or leaf sequences (`shapes: List<GraphicElement>`).
3. **The Painter's Algorithm:** In vector graphics rendering, objects are drawn from back to front (bottom of the visual stack to top of the visual stack). Later drawing operations paint over earlier ones.

#### Array Index vs. Visual Drawing Order Breakdown
Consider the leaf element sequence from the tree above:
```
Index 0: Rectangle (Geometry)
Index 1: Ellipse   (Geometry)
Index 2: Fill      (Color: Red)
Index 3: Stroke    (Color: Blue, Width: 4px)
```

Because Lottie decouples geometric paths from visual styling, this sequence produces a two-step execution pattern:

1. **Step 1: Forward AST Traversal (Geometry & Style Pairing):**
   The renderer traverses the sequence in forward order (`0 -> 1 -> 2 -> 3`):
   - At Index 0 and 1, the renderer evaluates `Rectangle` and `Ellipse` into vector paths and places them in an unpainted geometry buffer (`currentShapes = [RectPath, EllipsePath]`).
   - At Index 2, the renderer visits `Fill(Red)`. It pairs the red fill paint with all preceding unpainted shapes in `currentShapes`, producing the first draw batch: `StyledShapes(shapes = [RectPath, EllipsePath], style = RemoteFill(Red))`.
   - At Index 3, the renderer visits `Stroke(Blue, 4px)`. It pairs the 4px blue stroke paint with the preceding shapes, producing the second draw batch: `StyledShapes(shapes = [RectPath, EllipsePath], style = RemoteStroke(Blue, 4px))`.

2. **Step 2: Canvas Emission & Stacking Order:**
   The canvas executes the resulting draw batches:
   - **Batch 1 (Fill):** Fills the interior area of both the Rectangle and Ellipse with red.
   - **Batch 2 (Stroke):** Draws the 4px blue stroke outline over the boundary of both shapes.

Drawing the fill before the stroke ensures that the stroke outline is rendered cleanly on top of the fill, preventing the fill from covering the inner 2px of the stroke width.

Consequently:
- AST traversal proceeds in **forward order** to collect geometries before encountering the styles that consume them.
- Batch drawing on the canvas executes in **back-to-front order** (Painter's Algorithm) so that backgrounds and fills are laid down before foreground overlays and stroke outlines.

### 2.4 Animatable Properties Structure
Every animatable attribute in the AST is wrapped in a dedicated property model implementing a sealed property interface:
- **Static vs. Keyframed:** A property is either a `Static...Property` holding a fixed value (`RemoteFloat`, `RemoteColor`, `Point(RemoteFloat, RemoteFloat)`) or an `Animated...Property` containing an array of timeline keyframes.
- **Keyframe Descriptors:** Each keyframe defines a time frame (`t`), interpolation values (`s`, `e`), cubic Bézier easing control points (`i`, `o`), and a hold interpolation flag (`h`).
- The renderer resolves these property models into RemoteCompose state expressions on the fly as each node is visited during tree traversal.

---

## 3. How: System Architecture

The rendering engine operates via a **single-pass recursive tree traversal** (`Animation` -> `Layer` -> `Group`), evaluating properties into `RemoteFloat` / `RemoteColor` expressions on the fly as each AST node is visited.

Because Lottie defines shape geometries and paint styles as separate sibling nodes in a flat list, the traversal uses two local collections inside each group:

1. **`currentShapes: MutableList<RemoteShape>` (Unpainted Geometry Buffer):**
   - As the traversal visits geometry nodes ([Rectangle](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/geometry/Rectangle.kt), [Ellipse](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/geometry/Ellipse.kt), [Path](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/geometry/Path.kt)), it evaluates their dimensions on the go into vector paths and holds them in `currentShapes`.
   - These shapes do not yet have a color or paint; they are waiting for a style node to consume them.
2. **`shapeGroups: MutableList<StyledShapes>` (Draw Batches):**
   - When the traversal hits a style node ([Fill](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/styles/Fill.kt), [Stroke](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/styles/Stroke.kt)), it evaluates the style into a [RemoteStyle](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/RemoteStyle.kt) and pairs it with all preceding geometries: `StyledShapes(shapes = currentShapes.toList(), style = remoteStyle)`.
   - This batch is appended to `shapeGroups`.
   - Once all elements in the group are processed, `shapeGroups` represents the ordered batches ready for drawing.

---

### 3.1 End-to-End Execution Trace

Consider a Lottie group containing two shapes and two styles:

```
Group (shapes: [Rectangle, Ellipse, Fill(Red), Stroke(Blue, 4px)])
```

#### Step 1: Traversal & Expression Construction (on the go)
1. **Visit `Rectangle`:** Evaluates width, height, and corner radius into a `RemoteLottiePath`.
   - `currentShapes` = `[RectPath]`
   - `shapeGroups` = `[]`
2. **Visit `Ellipse`:** Evaluates radii and center into a `RemoteLottiePath`.
   - `currentShapes` = `[RectPath, EllipsePath]`
   - `shapeGroups` = `[]`
3. **Visit `Fill(Red)`:** Evaluates color and opacity into `RemoteFill(color = Red)`.
   - Emits batch: `StyledShapes(shapes = [RectPath, EllipsePath], style = RemoteFill(Red))`
   - `shapeGroups` = `[Batch 1 (Fill)]`
4. **Visit `Stroke(Blue, 4px)`:** Evaluates stroke color, width, and line caps into `RemoteStroke(color = Blue, width = 4)`.
   - Emits batch: `StyledShapes(shapes = [RectPath, EllipsePath], style = RemoteStroke(Blue))`
   - `shapeGroups` = `[Batch 1 (Fill), Batch 2 (Stroke)]`

#### Step 2: Canvas Drawing Execution
`RemoteCanvas` iterates over `shapeGroups` in order, setting the paint once per batch and drawing its paths:

```kotlin
// Batch 1: Fill
usePaint(redFillPaint) {
  canvas.drawPath(rectPath)
  canvas.drawPath(ellipsePath)
}

// Batch 2: Stroke
usePaint(blueStrokePaint) {
  canvas.drawPath(rectPath)
  canvas.drawPath(ellipsePath)
}
```

This ensures `rectPath` and `ellipsePath` are evaluated **once** during traversal, but painted **twice** with different paint configurations.

---

### 3.2 `RenderContext` State Encapsulation

To eliminate parameter sprawl across recursive functions, all rendering parameters are unified into an immutable context:

```kotlin
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
    return copy(
      transformStack = newStack,
      effectiveOpacity = effectiveOpacity * groupOpacity,
    )
  }

  fun withModifiers(trim: TrimPath?, corners: RoundedCorners?): RenderContext =
    copy(
      activeTrimPath = trim ?: activeTrimPath,
      activeRoundedCorners = corners ?: activeRoundedCorners,
    )
}
```

### 3.3 Intermediate Representation (IR)

Decouple AST models from canvas emission using two intermediate structures:

```kotlin
/** Represents an evaluated vector shape ready for canvas rendering. */
internal interface EvaluatedGeometry {
  fun draw(drawScope: RemoteDrawScope, canvas: RemoteCanvas)
  fun withFillRule(fillRule: FillRule): EvaluatedGeometry
}

/** Pre-compiled static path when no vertices or dimensions are animated. */
internal class RemoteCompiledGeometry(
  val path: RemotePath,
  val fillRule: FillRule = FillRule.NonZero,
) : EvaluatedGeometry { ... }

/** Dynamic Bézier spline when vertices or control points are animated via expressions. */
internal class RemoteDynamicGeometry(
  val path: List<RemoteBezierValue>,
  val fillRule: FillRule = FillRule.NonZero,
) : EvaluatedGeometry { ... }

/** Associates a style with all geometries it renders. */
internal data class StyledShapeGroup(
  val geometries: List<EvaluatedGeometry>,
  val style: RemoteStyle,
  val transformStack: List<Transform>,
)
```

### 3.4 Scope-Based Shape Gathering Algorithm

Inside `renderer/Shape.kt`, `gatherShapes` processes elements in forward sequence:

1. **Modifiers:** When a `TrimPath` or `RoundedCorners` element is found, update the active modifier in `context`. When a `Repeater` or `MergePaths` is found, immediately transform all geometries currently in the buffer.
2. **Geometries:** When a [GeometryShape](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/geometry/GeometryShape.kt) is found, evaluate it with the active `TrimPath` and `RoundedCorners` applied, and append to the active geometry buffer.
3. **Styles:** When a [ShapeStyle](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/styles/ShapeStyle.kt) is found:
   - Instantiate corresponding [RemoteStyle](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/RemoteStyle.kt) (`RemoteFill`, `RemoteStroke`, `RemoteGradientFill`, `RemoteGradientStroke`).
   - Emit `StyledShapeGroup(geometries = buffer.toList(), style = remoteStyle, transformStack = context.transformStack)`.
   - If subsequent geometries appear, clear buffer for the next style group.
4. **Groups:** When a [Group](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/graphicelement/grouping/Group.kt) is found, recursively call `gatherShapes` with a scoped `RenderContext`.
5. **Stack Inversion:** Return `resultList.reversed()`. Lottie specification dictates that elements at higher array indices are at the bottom of the stack and drawn first.

### 3.5 Dual-Path Property Evaluation Strategy

All resolved properties use canonical RemoteCompose types (`RemoteFloat`, `RemoteColor`, `RemotePath`). To prevent expression bloat, constant properties emit simple leaf values rather than runtime expression graphs:

| Property State | Evaluation Mechanism | Result Type | RemoteCompose Overhead |
| :--- | :--- | :--- | :--- |
| **Static / Constant Value** (`StaticScalarProperty`, etc.) | Directly returns the constant leaf value | Constant `RemoteFloat`, `RemoteColor` | Zero expression operations (single constant leaf) |
| **Static Geometry** (Non-animated size, position, vertices) | Pre-computed once during compose in Kotlin | Static `RemoteCompiledGeometry` (`RemotePath`) | Uploaded as static path; zero per-frame path recalculation |
| **Constant Timeline** (`currentFrame.constantValueOrNull != null`) | Pre-computed with easing at specific frame | Constant `RemoteFloat`, `RemoteColor` | Single constant leaf; avoids multi-segment `selectIfLt` chain |
| **Dynamic Keyframed** (`AnimatedScalarProperty`, etc.) | Chained `selectIfLt` + Bézier easing expressions | Dynamic `RemoteFloat`, `RemoteColor`, `RemoteDynamicGeometry` | Recorded into RemoteCompose expression graph for client-side playback |

---

## 4. Details: Module Structure & Class Contracts

### 4.1 Package Organization

```
remotecompose/lottie/renderer/
├── Animation.kt              # Easing lookups, bezier interpolation constants
├── RenderContext.kt          # Context encapsulation (new)
├── RemoteShape.kt            # EvaluatedGeometry, RemoteCompiledGeometry, RemoteDynamicGeometry
├── RemoteStyle.kt            # RemoteStyle hierarchy: RemoteFill, RemoteStroke, RemoteGradient*
├── Shape.kt                  # RenderShapes, gatherShapes, canvas emission loop
├── Transform.kt              # 2D transform, skew, scale singularity clamping, inverseTransform
├── layers/
│   ├── Layer.kt              # Top-level dispatcher, timeline bounds, track matte context
│   ├── ShapeLayer.kt         # Shape layer drawing
│   ├── SolidColorLayer.kt    # Solid color rectangle drawing (re-implemented)
│   ├── PrecompLayer.kt       # Nested composition asset resolution and time remapping
│   ├── ImageLayer.kt         # Bitmap asset drawing
│   └── TextLayer.kt          # Font glyph drawing
├── properties/
│   ├── Bezier.kt             # Multi-keyframe Bézier point and tangent animation
│   ├── Color.kt              # Solid color animation and slot override
│   ├── Gradient.kt           # Multi-stop color/opacity gradient animation (new)
│   ├── Position.kt           # 2D position, split position, spatial Bézier paths
│   ├── Scalar.kt             # Scalar float animation with hold keyframes
│   └── Vector.kt             # Multi-dimensional vector property animation
├── shapes/
│   ├── Ellipse.kt            # Ellipse parametric geometry evaluator
│   ├── GeometryTransform.kt  # In-place shape transformation
│   ├── Path.kt               # Path geometry evaluator
│   ├── PolyStar.kt           # Star and regular polygon geometry evaluator
│   └── Rectangle.kt          # Rectangle parametric geometry evaluator with corner radius
└── modifiers/
    ├── MergePaths.kt         # Path contour merging
    ├── OffsetPath.kt         # Path contour expansion/contraction
    ├── PuckerBloat.kt        # Geometric pucker and bloat deformation
    ├── Repeater.kt           # Instance repetition with transform offsets
    ├── RoundedCorners.kt     # Automatic fillet radius on bezier vertices
    ├── TrimPathEvaluator.kt  # Dynamic de Casteljau curve segmenting
    ├── Twist.kt              # Rotational vertex deformation
    └── ZigZag.kt             # Serrated edge ridge insertion
```

### 4.2 `RemoteStyle.kt` Class Contracts

```kotlin
internal interface RemoteStyle {
  fun getPaint(inheritedOpacity: RemoteFloat): RemotePaint
}

internal class RemoteFill(
  val fillColor: RemoteColor,
  val opacity: RemoteFloat,
  val fillRule: FillRule,
) : RemoteStyle {
  override fun getPaint(inheritedOpacity: RemoteFloat): RemotePaint = RemotePaint {
    val alpha = fillColor.alpha * (opacity / 100f) * inheritedOpacity
    this.color = fillColor.copy(alpha = alpha)
  }
}

internal class RemoteStroke(
  val strokeColor: RemoteColor,
  val strokeWidth: RemoteFloat,
  val opacity: RemoteFloat,
  val lineCap: LineCap,
  val lineJoin: LineJoin,
  val miterLimit: Float,
  val dashPattern: PathEffect? = null,
) : RemoteStyle {
  override fun getPaint(inheritedOpacity: RemoteFloat): RemotePaint = RemotePaint {
    val alpha = strokeColor.alpha * (opacity / 100f) * inheritedOpacity
    this.color = strokeColor.copy(alpha = alpha)
    this.style = PaintingStyle.Stroke
    this.strokeWidth = this@RemoteStroke.strokeWidth
    this.strokeCap = lineCap.toStrokeCap()
    this.strokeJoin = lineJoin.toStrokeJoin()
  }
}

internal class RemoteGradientFill(
  val gradient: RemoteGradientValue,
  val startPoint: Point,
  val endPoint: Point,
  val gradientType: GradientType,
  val opacity: RemoteFloat,
  val fillRule: FillRule,
  val highlightAngle: RemoteFloat? = null,
  val highlightLength: RemoteFloat? = null,
) : RemoteStyle { ... }

internal class RemoteGradientStroke(
  val gradient: RemoteGradientValue,
  val startPoint: Point,
  val endPoint: Point,
  val gradientType: GradientType,
  val strokeWidth: RemoteFloat,
  val opacity: RemoteFloat,
  val lineCap: LineCap,
  val lineJoin: LineJoin,
  val miterLimit: Float,
  val dashPattern: PathEffect? = null,
) : RemoteStyle { ... }
```

---

## 5. Migration & Pull Request Strategy

To ensure continuous stability, the refactoring is broken down into 7 sequential, atomic PRs:

### PR 1: Style Rendering Pipeline & Cascading Opacity
- **Scope:** [RemoteStyle.kt](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/RemoteStyle.kt), `renderer/properties/Gradient.kt`, [Shape.kt](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/Shape.kt).
- **Deliverables:**
  - Implement `RemoteStroke`, `RemoteGradientFill`, and `RemoteGradientStroke`.
  - Add gradient property animation in `renderer/properties/Gradient.kt`.
  - Update `RemoteStyle.getPaint(inheritedOpacity: RemoteFloat)` to cascade compound opacity.
  - Extend `gatherShapes` to collect and emit all `ShapeStyle` instances.
- **Verification:** Unit tests for paint emission; Roborazzi tests for stroked shapes and gradients.

### PR 2: SolidColorLayer & RenderContext Foundation
- **Scope:** [SolidColorLayer.kt](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/layers/SolidColorLayer.kt), [Layer.kt](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/layers/Layer.kt), `RenderContext.kt`.
- **Deliverables:**
  - Introduce `RenderContext` data class.
  - Implement [SolidColorLayer](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/layer/SolidColorLayer.kt) rendering with hex color parsing and transform stack.
  - Implement layer timeline bounds calculation (`[startFrame, endFrame)`) and `calculateLocalFrame`.
- **Verification:** Solid color layer rendering tests and timeline clipping unit tests.

### PR 3: Dual-Path Geometry Evaluation & Bézier Morphing
- **Scope:** [Bezier.kt](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/properties/Bezier.kt), [Path.kt](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/shapes/Path.kt), [RemoteShape.kt](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/RemoteShape.kt).
- **Deliverables:**
  - Implement `EvaluatedGeometry` intermediate representation.
  - Complete `animateBezier` with multi-keyframe point and tangent interpolation.
  - Add `RemoteCanvas.drawPathWithFillRule` supporting `EvenOdd` winding.
  - Separate static compilation (`RemoteCompiledGeometry`) from dynamic expression generation (`RemoteDynamicGeometry`).
- **Verification:** Path animation Roborazzi screenshot diff tests.

### PR 4: Spatial Position Trajectories & Transform Enhancements
- **Scope:** [Position.kt](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/properties/Position.kt), [Transform.kt](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/Transform.kt).
- **Deliverables:**
  - Implement `SplitPositionProperty` evaluation (independent X and Y channels).
  - Implement cubic Bézier spatial curves using `spatialInTangent` / `spatialOutTangent`.
  - Add 2D skew and skew-axis transformations in `Transform.kt`.
  - Implement `clampScale` to guard against matrix inversion singularities.
  - Implement `inverseTransform`.
- **Verification:** Curved trajectory animation tests and skew transform tests.

### PR 5: Core Shape Modifiers Pipeline (TrimPath, Repeater, RoundedCorners)
- **Scope:** `renderer/modifiers/TrimPathEvaluator.kt`, `Repeater.kt`, `RoundedCorners.kt`.
- **Deliverables:**
  - Implement `TrimPathEvaluator` using de Casteljau subdivision for open/closed Bézier paths.
  - Implement `Repeater` replicating geometries with cumulative offset, scale, and alpha decay.
  - Implement `RoundedCorners` filleting sharp vertices.
  - Integrate modifier hooks into `gatherShapes`.
- **Verification:** Combinatorial modifier test cases and visual snapshot checks.

### PR 6: Extended Modifiers (MergePaths, ZigZag, PuckerBloat, Twist, OffsetPath)
- **Scope:** `renderer/modifiers/MergePaths.kt`, `ZigZag.kt`, `PuckerBloat.kt`, `Twist.kt`, `OffsetPath.kt`.
- **Deliverables:**
  - Implement boolean path operations in `MergePaths` without host API 34 leaks.
  - Implement geometric deformation modifiers (`ZigZag`, `PuckerBloat`, `Twist`, `OffsetPath`).
- **Verification:** Comprehensive modifier screenshot tests.

### PR 7: Nested Precompositions, Assets & Track Mattes
- **Scope:** `PrecompLayer.kt`, `ImageLayer.kt`, `TextLayer.kt`, [Layer.kt](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/layers/Layer.kt).
- **Deliverables:**
  - Implement nested precompositions in `PrecompLayer` with `timeRemap` and asset resolution.
  - Implement bitmap drawing in `ImageLayer` and glyph layouts in `TextLayer`.
  - Connect `MatteContext` to support Alpha, Luma, and Inverted track mattes via canvas clipping.
- **Verification:** Complex Lottie showcase animations with precompositions and mattes.

---

## 6. Alternatives Considered

### Alternative A: Monolithic Cherry-Pick from `prepear-for-merge`
- **Description:** Directly port all rendering files from `prepear-for-merge` in one massive change.
- **Pros:** Fast initial feature restoration.
- **Cons:** Carries forward massive expression bloat, unencapsulated 8-parameter calls, and untyped downcasting. Unreviewable in a single PR.
- **Decision:** **Rejected.** Re-architecting into clean phases guarantees maintainability and performance.

### Alternative B: Direct Canvas Visitor Pattern (No Intermediate Representation)
- **Description:** Render each AST element directly to canvas during the initial AST traversal without intermediate structures.
- **Pros:** Fewer allocations; avoids creating intermediate `StyledShapeGroup` objects.
- **Cons:** Fails on Lottie's scope-based styling model, where styles consume all preceding geometries and elements must be drawn in reverse order.
- **Decision:** **Rejected.** An intermediate representation is necessary to decouple modifier transformations from drawing commands.

### Alternative C: Universal `RemoteFloat` Expression Graphs for All Values
- **Description:** Treat every coordinate, scale, and color as a dynamic `RemoteFloat` expression graph regardless of whether it is static or keyframed.
- **Pros:** Single code path for all properties.
- **Cons:** Generates thousands of unnecessary math nodes in RemoteCompose documents for static properties, bloating document byte sizes and causing client execution overhead.
- **Decision:** **Rejected.** Dual-path static vs. dynamic evaluation is essential for embedded performance.

### Alternative D: Unpacking Static Properties to Kotlin Primitives (`Float`, `Color`)
- **Description:** Strip `RemoteFloat` and `RemoteColor` down to raw Kotlin `Float` and Compose `Color` during evaluation, and convert them back to Remote types at canvas draw time.
- **Pros:** Can use standard Kotlin standard library math operators directly.
- **Cons:** Creates type churn and impedance mismatch. The AST format models already use canonical `RemoteFloat` and `RemoteColor` representations; canvas commands expect Remote types directly.
- **Decision:** **Rejected.** Keep `RemoteFloat` / `RemoteColor` as the ubiquitous types throughout the pipeline, distinguishing static vs. dynamic properties by constant leaf nodes versus dynamic expression trees.

---

## 7. Glossary

- **RenderContext:** Immutable state container passed through the render tree containing timeline settings, parent transforms, compound opacity, and clipping boundaries.
- **EvaluatedGeometry:** Intermediate vector geometry representation holding pre-compiled static paths (`RemoteCompiledPath`) or dynamic Bézier spline nodes (`RemoteLottiePath`).
- **StyledShapeGroup:** Paired grouping of an evaluated [RemoteStyle](file:///usr/local/google/home/myavorskyi/AndroidStudioProjects/my-horologist-lottie-grandchild-fix-v2/remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/renderer/RemoteStyle.kt) and a sequence of [EvaluatedGeometry](#32-intermediate-representation-ir) elements sharing that style.
- **Scope-Based Styling:** Lottie drawing rule where geometries and styles share a flat list, and a style consumes all preceding unstyled geometries in its enclosing group.
- **Shape Modifier:** A procedural geometric operator (`TrimPath`, `Repeater`, `RoundedCorners`) that mutates vector geometry before styles are applied.
- **Track Matte:** Layer masking technique where an adjacent or referenced layer defines the alpha or luminance mask of the target layer.
