# VeryAILiveness — Android distribution

Prebuilt AAR distribution of the VeryAILiveness palm-liveness SDK for Android.

A check opens a palm scan, collects evidence on the device and submits it to
Very, which decides. Your backend creates each liveness session — the server
secret never ships in the app — and the SDK returns a `VeryLivenessResult` carrying
Very's signed decision.

## Install — Gradle (recommended)

`org.very:liveness` is on Maven Central:

```gradle
dependencies {
    implementation 'org.very:liveness:1.3.7'
}
```

The models (about 9 MB) are not in the AAR: the SDK downloads them on first
use, checks each file against a pinned SHA-256 and caches it on the device.
Call `VeryAILiveness.prefetch(context)` early (for example when the screen
that offers the check opens) to move the download off the scan page.
`org.very:sdk-native-bundle` is no longer needed — remove it.

## Install — manual AAR

`liveness-1.3.7.aar` is at the root of this tag. Prefer Maven Central:
a file dependency skips the POM, so add its runtime dependencies yourself
(listed in the POM on Maven Central: AndroidX, CameraX, LiteRT, Kotlin
coroutines, Bouncy Castle and AndroidSVG).

## Project requirements

| Setting | Value |
|---|---|
| `minSdk` | 23 |
| `compileSdk` | 34+ |
| Java / Kotlin target | 11 |
| Manifest permissions | `android.permission.CAMERA`, `android.permission.INTERNET` |

The SDK runs its models on LiteRT 1.0.1
(`com.google.ai.edge.litert:litert:1.0.1`), which keeps the
`org.tensorflow.lite` package. An app that also pulls in TensorFlow Lite
artifacts (`org.tensorflow:tensorflow-lite`, `tensorflow-lite-api`, or a library
that depends on them, such as `tensorflow-lite-support`) gets duplicate-class
build errors. Find them with `./gradlew :app:dependencies`, then move to the
LiteRT 1.0.1 artifacts: `litert` and `litert-api` for `tensorflow-lite`
and `tensorflow-lite-api`, `litert-support` and `litert-support-api` for
`tensorflow-lite-support`. In 1.x these keep the `org.tensorflow.lite` and
`org.tensorflow.lite.support` packages, so your imports do not change. Or exclude
the TensorFlow Lite ones where the API matches.

LiteRT Support 2.x is not a drop-in replacement: it moves the support classes to
`com.google.ai.edge.litert.support` (those imports change), requires minSdk 24,
and raises LiteRT itself to 2.x for the whole app, the SDK included.

## Quickstart

```kotlin
import org.very.liveness.VeryAILiveness
import org.very.liveness.VeryLivenessConfig
import org.very.liveness.VeryLivenessSessionProvider

val config = VeryLivenessConfig(sdkKey = "your-sdk-key")
config.sessionProvider = VeryLivenessSessionProvider { request, callback ->
    // Your backend creates the session with Very (server secret) and returns
    // the response body unchanged: callback.onSession(json) or callback.onError(e).
}

VeryAILiveness.check(context = this, config = config) { result ->
    // result.isSuccess only when Very's signed decision passed;
    // result.signedToken is the receipt for your backend.
}
```

## Upgrading from 1.0.x

1.1.0 evaluates the check on Very's servers: your backend must create each
session (`sessionProvider` is required, or every check fails with 5002) and
verify `result.signedToken`. `scanTimeoutSeconds` is gone and
`touchPolicyVersion` became `multiPointTouch`.
`VeryResult` became `VeryLivenessResult` (`errorType` is a
`VeryLivenessErrorType`). Remove `org.very:sdk-native-bundle`.
Details: `CHANGELOG.md`.

## Documentation

**https://very.org/docs/liveness-sdk/android**

## Demo

`demo/` is a standalone Android Studio project that pulls
`org.very:liveness:1.3.7` from Maven Central. It asks for the URL of a
session broker — your backend's session endpoint.

## Licenses

Proprietary — see `LICENSE`. Third-party components: `THIRD_PARTY_NOTICES.md`.
Changes by version: `CHANGELOG.md`.
