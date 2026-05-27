# VeryAILiveness — Android distribution

Prebuilt AAR distribution of the VeryAILiveness palm-liveness SDK for Android.
Source code lives in [veroslabs/very-mobile-sdk](https://github.com/veroslabs/very-mobile-sdk).

A liveness check opens a single-shot palm scan that confirms a live human
palm. The flow makes two backend calls (issue session on entry, record
outcome on exit) and produces a pass/fail `VeryResult` — no enrollment,
no verification, no signed token.

## Install — Gradle (recommended)

`org.very:liveness` is published to Maven Central. Add the dep in your
app's `build.gradle`:

```gradle
dependencies {
    implementation 'org.very:liveness:1.0.48'
}
```

Maven Central is the canonical channel. The AARs committed in this
repo are a parallel mirror for partners who prefer pulling binaries
out of git (vendoring, air-gapped CI, etc.) — see *Install — Manual AAR*
below.

## Install — Manual AAR

Two AARs ship at the root of every tagged release:

| File | Purpose |
|---|---|
| `liveness-1.0.48.aar` | Main SDK — `org.very:liveness` |
| `sdk-native-bundle-1.0.48.aar` | Optional bundled `.so` companion (see *Asset loading* below) |

Drop them in your app's `libs/` directory and add a fileTree
dependency:

```gradle
dependencies {
    implementation fileTree(dir: 'libs', include: ['liveness-1.0.48.aar'])
    // Optional — opt into bundled mode (see Asset loading)
    implementation fileTree(dir: 'libs', include: ['sdk-native-bundle-1.0.48.aar'])
    // VeryAILiveness depends on AndroidX + CameraX at runtime; the
    // released POM declares them, but fileTree skips POM resolution
    // so they have to be added manually:
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.11.0'
    def camerax_version = "1.4.1"
    implementation "androidx.camera:camera-core:${camerax_version}"
    implementation "androidx.camera:camera-camera2:${camerax_version}"
    implementation "androidx.camera:camera-lifecycle:${camerax_version}"
    implementation "androidx.camera:camera-view:${camerax_version}"
}
```

Prefer the Maven Central path unless you have a specific reason —
fileTree skips POM resolution so transitive deps have to be added by
hand and won't auto-bump between releases.

## Project requirements

| Setting | Value |
|---|---|
| `minSdk` | 23 |
| `compileSdk` | 34+ |
| Java / Kotlin target | 11 |
| Manifest permissions | `android.permission.CAMERA`, `android.permission.INTERNET` |

Liveness uses the device camera and the on-device PalmID native
library. Both work on real ARM devices only (no emulator support for
the .so).

## Usage

```kotlin
import org.very.liveness.VeryAILiveness
import org.very.liveness.VeryLivenessConfig

val config = VeryLivenessConfig(
    sdkKey = "your-sdk-key",        // required — issued by Very
    themeMode = "dark",             // "light" or "dark"
    language = "en",                // optional — ISO 639-1, defaults to system locale
)

VeryAILiveness.check(context = this, config = config) { result ->
    runOnUiThread {
        when {
            result.isSuccess           -> { /* liveness passed */ }
            result.code == "cancelled" -> { /* user dismissed the page */ }
            else                       -> {
                // result.error / result.errorMessage carry the failure detail
            }
        }
    }
}
```

`VeryLivenessConfig` is a slim subset of the full SDK's `VeryConfig` —
no `userId` because liveness binds no user identity, but `sdkKey` is
required to authenticate the backend session calls.

`VeryResult.code` is one of `"success"`, `"cancelled"`, or `"error"`.
On non-success, `result.error` carries an SDK error code and
`result.errorMessage` carries a localized human-readable message.

## Asset loading

The native palm-recognition library (`libPalmAPISaas.so`, ~18 MB per
ABI) ships in **slim mode** by default — `org.very:liveness` does not
include the .so. The SDK downloads it from CDN on first scan and
caches it under app-private storage; subsequent launches use the
cache. Plan for a loading state on the first scan (5–15 s on typical
networks).

To bundle the .so inside your APK (instant first scan, +18 MB per
ABI), add the companion `sdk-native-bundle` artifact alongside:

```gradle
dependencies {
    implementation 'org.very:liveness:1.0.48'
    implementation 'org.very:sdk-native-bundle:1.0.48'
}
```

> **Don't combine `sdk-native-bundle` with `org.very:sdk`** — the full
> SDK already bundles the same .so, and adding both produces a jniLibs
> merge conflict at AGP packaging time.

## Network endpoints

If your network restricts egress, allowlist the following:

| Purpose | URL |
|---|---|
| Liveness session create | `https://api.very.org/v1/sdk/liveness-sessions` |
| Liveness result POST | `https://api.very.org/v1/sdk/liveness-sessions/{id}/result` |
| Model download (primary) | `https://assets.very.org/sdk/v2/<abi>/libPalmAPISaas.so` |
| Model download (backup) | `https://r2.assets.very.org/sdk/v2/<abi>/libPalmAPISaas.so` |

The result POST is fire-and-forget — it doesn't block the host
callback. The model download path is unused in bundled mode after the
first install.

## Demo

A runnable demo lives in `demo/` — clone this repo, open
`demo/` in Android Studio, replace the placeholder `sdkKey` in
`demo/app/src/main/kotlin/org/very/liveness/example/MainActivity.kt`,
plug in a real ARM device, and run.

The demo's `app/build.gradle` pulls `org.very:liveness:1.0.48` from
Maven Central, so the same demo source builds against any released
version by editing one line.
