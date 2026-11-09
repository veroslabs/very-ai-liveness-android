# Third-party notices — VeryAILiveness native SDK

VeryAILiveness (iOS `VeryAILiveness.xcframework`, Android `org.very:liveness`)
is proprietary software of Veros Inc. (see `LICENSE`). It contains or depends on
the following third-party components, which remain under their own licenses.
The license texts are in `licenses/`. The Android AAR carries this file, `LICENSE`
and `licenses/` under `META-INF/very-liveness/`.

- **LiteRT (TensorFlow Lite)**, Apache License 2.0, Copyright Google LLC.
  - iOS: the LiteRT 2.3.0 C API is statically linked into `VeryAILiveness.framework`.
  - Android: resolved as `com.google.ai.edge.litert:litert` from Maven; it is not
    inside the AAR.
  - License: `licenses/LiteRT-LICENSE.txt`.
- **MediaPipe hand models**, Apache License 2.0, Copyright Google LLC. The palm
  detector and hand landmark models (`very_hand_detector.tflite`,
  `very_hand_landmarks.tflite`) are not inside the framework or the AAR; the SDK
  downloads them from Very's CDN on first use and keeps them in the app's data.
  License: `licenses/MediaPipe-models-LICENSE.txt`.
- **Inter typeface** (Regular, SemiBold), SIL Open Font License 1.1, Copyright
  2016 The Inter Project Authors. Inside the framework bundle and the AAR assets.
  License: `licenses/Inter-OFL.txt`.
- **Bouncy Castle** (`org.bouncycastle:bcprov-jdk18on`), Bouncy Castle Licence
  (MIT-style). Android only, resolved from Maven for Ed25519 verification; it is
  not inside the AAR.

The Android POM also declares AndroidX, CameraX, Kotlin coroutines and
AndroidSVG; each is resolved from Maven under its own license.

The `RecoveredModels` coefficients (downloaded on first use, like the hand
models) and Very's UI artwork, strings and screens are
Very's own and are covered by `LICENSE`, not by the licenses above.
