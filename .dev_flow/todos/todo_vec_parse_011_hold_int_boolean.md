# TODO: Implement int-boolean support for hold property 'h' in VectorPropertyKeyframe

- **Traceable ID:** `VEC-PARSE-011`
- **Target File:** `remotecompose/lottie/src/main/java/com/google/android/horologist/remotecompose/lottie/format/properties/Vector.kt`
- **Specification:** [Lottie Integer Boolean](https://lottie.github.io/lottie-spec/1.0.1/specs/values/#int-boolean), [Lottie Vector Keyframe](https://lottie.github.io/lottie-spec/1.0.1/specs/properties/#vector-keyframe)
- **Problem:** `VectorPropertyKeyframe` uses Kotlinx serialization's default `Boolean` serializer for `hold`, which fails when JSON supplies `h: 1` or `h: 0` with `JsonDecodingException: Expected boolean, but had 1`.
- **Expected Solution:** Implement a custom serializer for `VectorPropertyKeyframe` or a flexible `IntBooleanSerializer` (similar to `ScalarPropertyKeyframeSerializer` in `Scalar.kt`) to decode both `Boolean` and `Int` (0/1).
- **Test:** Enable `@Ignore` test `deserializesVectorKeyframe_whenHoldFlagIsIntegerBoolean` in `VectorPropertyParsingTest.kt`.
