# Changelog

Partner-facing changes to the VeryAILiveness SDK (`VeryAILiveness` on
CocoaPods / Swift Package Manager, `org.very:liveness` on Maven Central).
Releases before 1.1.0 were published from `very-mobile-sdk`.

## 1.1.2

A drop-in update to 1.1.1: no API, backend or behaviour change on devices.
Android is unchanged.

### Changed

- **CocoaPods: add a `:source` to the pod line.** CocoaPods trunk accepts no
  new versions from 2026-12-02, so Very publishes the pod to its own spec
  repository, which carries every version (1.1.0 is only there):
  `pod 'VeryAILiveness', '~> 1.1', :source => 'https://github.com/veroslabs/very-cocoapods-specs.git'`.
  The `:source` applies to this pod only. Without it CocoaPods resolves no
  version newer than 1.1.1, and never 1.1.0. Swift Package Manager is unchanged.
- **iOS: the simulator slice is back to arm64 and x86_64.** 1.1.0 and 1.1.1
  shipped an arm64-only simulator slice, so a Swift Package Manager app built
  for every simulator architecture (a generic simulator destination, a Release
  simulator build, an Intel Mac or Rosetta) failed with "unsupported Swift
  architecture", and the pod set `EXCLUDED_ARCHS[sdk=iphonesimulator*] = x86_64`
  on your app target. Neither is needed now: remove that setting if you added
  it for this SDK. As before, the simulator never runs a check:
  `isSupported()` returns `false` and `check` returns error 6102.
- iOS: `prefetch()` does nothing on the simulator, where it used to download
  the models (about 9.5 MB) that no check there could use.

## 1.1.1

A drop-in update to 1.1.0: no API, backend or integration changes. The scan
screen follows the latest design review on iOS and Android.

### Changed

- The scan page asks the user to "Show your hand" (it said palm), in every
  supported language. A session's own instruction text still replaces it.
- The hand outline shows on its own while the page waits for a hand; the guide
  box appears once a hand is detected and stays up while the hand is outside it.
- The scan frame sits a little lower, the hand outline is a little smaller, and
  the frame and its notice line up with the controls at the top.
- While the scan page loads, it shows only a spinner, without the
  "loading..." text. Screen readers still announce that it is loading.

## 1.1.0

Very evaluates the check on its servers from evidence the device collects.
Upgrading from 1.0.x needs a backend change and a few code changes.

### Breaking

- **A session provider is required.** Your backend creates each liveness
  session with Very (`POST /v1/sdk/liveness/sessions`, authenticated with the
  app's server secret, which never ships in the app) and the app hands the
  response back through `VeryLivenessConfig.sessionProvider`. A check without
  one fails at once with error `5002`. A 1.0.x integration compiles against
  1.1.0 but every check fails until the provider is set; see the README.
- **Liveness has its own result types, the same on iOS and Android.** The
  callback receives `VeryLivenessResult` instead of `VeryResult`: `code`,
  `signedToken`, `error`, `errorMessage`, `isSuccess` and `errorType`, which
  is a `VeryLivenessErrorType`. `userId`, `userStatus` and `supportCode`
  (always empty for a liveness check) and the enrollment and verification
  error values are gone; they belong to the full SDK. Rename the type in your
  callback; the fields you used keep their names. On Android both
  `VeryLivenessResult` and `VeryLivenessSessionRequest` are interfaces the
  SDK implements.
- **`VeryLivenessResult.signedToken`** carries Very's signed decision.
  `isSuccess` is true only when that decision passed; your backend must
  verify the token before acting on a result.
- **iOS: `presentationStyle` takes `VeryLivenessPresentationStyle`**
  (`.fullScreen`, the default, or `.modal`). The full SDK's `.push` and
  `.embed` are not offered for a liveness check.
- **`VeryLivenessConfig.sdkKey` is set only at initialization** (iOS; Android
  already was), and iOS's initializer no longer takes `livenessMode`:
  `VeryLivenessConfig(sdkKey:themeMode:language:debugLogging:)`, the same
  arguments as Android.
- **Android: `VeryLivenessSessionRequest.body` is a `Map<String, Any>`**, as
  iOS's `[String: Any]`; `toJson()` is unchanged.
- **`scanTimeoutSeconds` is removed.** The capture budget is the session's
  `durationMs`, which your backend sets when it creates the session.
- **`touchPolicyVersion` is replaced by `multiPointTouch`** (`Bool`). Set it
  to accept the server's multi-point Touch sequence; the app's server
  configuration decides how many points and how long each hold is.
- **Internal types are no longer public.** iOS: `CameraPreviewView`,
  `NV21Frame`, `CameraFlashMode`, `CameraAdjustmentState`, `VeryScanMode`,
  `VeryScanResult`, `VeryScanPalmModel` and `VeryConfig`. Android:
  `CameraPreviewHelper`, `VeryConfig` and `VeryPresentationStyle`. The
  liveness API never took them; code that referenced them must stop.
- **The models download on first use (about 9 MB, once).** The framework / AAR
  no longer contains them, so it is about 10 MB smaller; the SDK fetches them
  from Very's CDN at the first check, checks each file's SHA-256 and keeps
  them for later checks. Call `VeryAILiveness.prefetch()` (iOS) or
  `VeryAILiveness.prefetch(context)` (Android) early, for example at app start
  or on the screen before the check, so the first scan does not wait for the
  download. Without a connection the check fails with `6106`
  after offering a retry. iOS no longer needs `PalmAPISaas.xcframework`;
  Android no longer needs `org.very:sdk-native-bundle` — remove both.

### Added

- **`VeryLivenessResult.code == "rejected"`** when Very's signed decision did not
  pass. `signedToken` carries the receipt and `isSuccess` is false; `error`
  keeps the code this outcome had before (`6102`, or `6107` when the capture
  budget ran out first), so a host that only reads `error` behaves as before.
- **`VeryAILiveness.cancel()`** (iOS and Android): the host takes the check
  down. It completes with `code: "cancelled"` (error `6104`), as the user
  closing it would; once Very's decision has arrived, that decision is
  delivered instead.
- **One check at a time.** A second `check()` while one is on screen
  completes at once with error `6109` (`VeryLivenessErrorType.checkInProgress` /
  `CHECK_IN_PROGRESS`) and leaves the first alone.
- **The text saved in the Developer Portal is shown.** A session body
  carrying `appearance.instruction` / `appearance.privacyMessage` replaces
  the scan prompt and the privacy notice for that check, as on the web;
  `VeryLivenessConfig.customStrings` / `privacyMessage` remain the fallback.
- **A typed value for every code a check returns.** `VeryLivenessErrorType`
  covers 6101, 6102, 6104, 6105 (iOS), 6106, 6107, 6109, 5002, 5008,
  6001–6006 and 6999; `invalidSession`, `livenessTimeout`, `unknownError` and
  the others used to read as `serverError`.
- **Sessions up to 60 s.** A session's `durationMs` may now be 5000–60000
  (was 5000–30000), so a longer multi-point Touch sequence no longer fails
  with an unsupported session.

### Changed

- The `livenessMode` property is ignored: the SDK picks the mode itself. It
  stays for source compatibility and is deprecated.
- iOS ships a privacy manifest (`PrivacyInfo.xcprivacy`) declaring the
  required-reason APIs the SDK uses and the data it sends to Very.
- Hand landmarks (never images) are the evidence submitted to Very.
- The binaries carry only the public API. The iOS framework no longer
  exports the SDK's internal Swift symbols or the bundled LiteRT runtime's
  C/C++ symbols, and Android's public classes no longer expose obfuscated
  members (`VeryAILiveness.a(…)`, `VeryLivenessConfig.toVeryConfig$…`) to
  Java callers.
