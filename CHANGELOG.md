# Changelog

Partner-facing changes to the VeryAILiveness SDK (`VeryAILiveness` on
CocoaPods / Swift Package Manager, `org.very:liveness` on Maven Central).
Releases before 1.1.0 were published from `very-mobile-sdk`.

## 1.3.7

Adds errors 6110 and 5013 and makes a timeout return Very's signed rejection.
Android drops three unused dependencies from the POM. The version jumps from
1.1.2 to 1.3.7 to match web SDK 1.3.7, whose fixes this release carries; there
is no native 1.2.x or 1.3.0–1.3.6. The two SDKs still differ where their notes
say so, for example in when the hand outline and the Touch dots show.

Swift Package Manager `from:` a 1.x version (such as `from: "1.1.2"`) and
CocoaPods `~> 1.1` resolve to 1.3.7. A lockfile keeps the version you have: run
`pod update VeryAILiveness`, or update the package in Xcode (File → Packages →
Update to Latest Package Versions). CocoaPods also needs the pod's `:source`
from 1.1.2 (Very's spec repository): trunk stops at 1.1.1.
A three-part CocoaPods pin such as `~> 1.1.2` (the form the 1.1.x install notes
gave) means below 1.2 and stays on 1.1.x: change it to `~> 1.3`.

### Added

- **Error 6110, camera unavailable** (`VeryLivenessErrorType.cameraUnavailable`
  / `CAMERA_UNAVAILABLE`), the web SDK's `camera_unavailable`. Camera access is
  granted, but the camera reports a fault (another app holds it, a call
  interrupts it on iOS, a device policy disables it on Android, or a device
  error) and then delivers no frame for 5 seconds. The check used to keep
  asking for a hand until the budget ran out and return 6107. A timeout while
  the camera is down also returns 6110. Once the scan has started the attempt
  is still submitted, so 6110 can arrive with `code:"rejected"` and the receipt.
  If your code switches over every `VeryLivenessErrorType`, add the new value.
- **Error 5013, too many requests** (`VeryLivenessErrorType.rateLimited` /
  `RATE_LIMITED`). Very answered a request with HTTP 429; try again later. This
  used to return 5002, invalid session. Add it to an exhaustive switch too.

### Changed

- **`VeryLivenessConfig.privacyMessage` keeps only the session notice's markup**
  (`<a href>` to an http(s) URL, `<b>`, `<strong>`, `<br>`, `<font color="#rgb">`);
  other markup is shown as text, as in a notice saved in the Developer Portal.
  On iOS the HTML importer no longer loads anything the host's notice refers to.
- **Android: three unused dependencies are gone from the POM**: Play Integrity
  (`com.google.android.play:integrity`), Guava (`com.google.guava:guava`) and
  `kotlinx-coroutines-guava`. The SDK never called them. If your app uses any of
  them without declaring it, declare it now.

### Fixed

- **A timed-out check returns Very's signed rejection.** The frame that arrived
  just after the capture budget ran out was recorded, so the evidence reported a
  capture longer than the session allowed. Very refused the body as invalid and
  the result was `code:"error"` 6107 with no receipt. A timeout now returns
  `code:"rejected"` with the receipt and error 6107, as the web SDK does.
- **A completed capture that Very passes is a success.** When the capture
  budget ran out (or the camera stopped) while the final checks of a completed
  capture were still running, Very could pass the attempt and the result was
  still `code:"error"` 6107 without the receipt. Very's signed pass now
  decides: the result is `code:"success"` with the receipt.
- **A setup timeout that waited on the network returns 6002.** When the session
  or Very's key has not arrived within the 30-second setup budget, the check
  returns 6002 (`timeout`, the network timeout) instead of 6107. A setup
  timeout with nothing in flight (for example the camera permission prompt
  left open) is still 6107.
- **The SDK shows the user's language by default.** With no
  `VeryLivenessConfig.language`, Android always showed English, whatever the
  system language, and kept a language set for an earlier check. Both platforms
  now try the user's languages in order (the system list, or the app's own
  language setting) and show the first one the SDK supports, as the web SDK
  does with the browser's; iOS used to follow the app's own localizations
  instead. A code is read as the web SDK reads it: an unlisted region keeps its
  language (`zh-CN`, `de-AT`, `es_MX`), Chinese script beats region
  (`zh-Hans-HK` is Simplified, `zh-Hant` Traditional), and `tl` is Filipino.
- **Back or a swipe on the success sheet delivers the success.** Very had
  passed the check, but closing its "Thanks for confirming" sheet with Android's
  back key or an iOS swipe-down returned `cancelled` (6104) without the receipt.
  It now returns `code:"success"` with the receipt, as Close and `cancel()` do.
- **A failed submission reports what failed.** When the evidence could not reach
  Very (network, server error, expired session), a check that had also failed
  locally returned its local code (6102, 6107, 6110). It now returns the
  request's own code: 6004, 6006 or 5002.
- **A device-side failure is an error, not a rejection.** When the final local
  model checks did not run, the attempt was still sent and came back as
  `rejected` with a receipt. Like the web SDK, the SDK now ends it as
  `code:"error"` (6102) without sending it.
- **The scan never outlives the session.** The capture deadline is now capped at
  the session's expiry, as on the web: an attempt on an expired session ends at
  once with 5002 instead of after the gesture.
- **The camera prompt no longer eats the time budgets.** iOS started the capture
  budget while the system camera prompt was still up; the capture now starts once
  access is granted. Android restarts the 30-second setup budget at the grant, so
  a slow answer to the prompt is no longer reported as a network timeout (6002).
- **Android: `cancel()` right after `check()` is no longer lost.** A cancel that
  arrived before the page opened, or while Retry opened a new page, was dropped;
  the page now takes it as soon as it opens.
- **Android: privacy-notice colours and links.** `<font color="#rgb">` showed the
  wrong colour (`#fff` as dark blue); links the browser accepts (a space in the
  path, `_` or non-ASCII host names) were shown as raw markup.
- **Android: the session provider is released after each check.** The SDK kept
  every `sessionProvider` (and whatever it held) for the life of the process.
- **A capture completed in time is not a timeout.** When the person finished
  just before the budget ran out and Very then rejected the attempt, the result
  carried 6107 because the budget ended while the final local checks ran. As on
  the web, the budget stops at completion and the final checks get up to 15
  seconds of their own, so the rejection is 6102.
- **A slow network can no longer hold the check for minutes.** A request to Very
  was only bounded per wait for data, so a trickle of bytes kept "Checking" on
  screen well past a minute. Each request now gets at most 15 seconds in all
  (telemetry 5), as in the web SDK.
- **iOS: a swipe-down the user lets go of no longer freezes the scan.** With
  `presentationStyle = .modal`, starting to drag the sheet down and letting it
  settle back stopped the frame loop for good: the camera kept running but
  nothing was checked, the host's `sessionProvider` was asked for a second
  session, and the check ended with 6107. The scan now stops only when the sheet
  actually closes, and a session already received is never requested again.
- **iOS: privacy-notice links read the same on every iOS version.** iOS 16 and
  earlier refused a link with a space or a non-ASCII character that iOS 17,
  Android and the web accept; such a notice showed raw markup.
- **Telemetry follows the server's switch.** A device that had cached the
  switch as off kept its outcome reports to itself for up to 7 days after the
  switch was turned back on. Reports on a session now go out and the server
  applies its switch, as for the web SDK. Their fields are counted as the web
  SDK counts them (a hand lost during the action, hints, failed holds, time
  from the capture start), and a network failure keeps the blocker the frames
  showed.
- **The hand outline and the Touch dots follow the hand on screen.** A detected
  hand still outside the scan box showed both the outline and the box; the
  outline now shows only while no hand is detected. The Touch dots needed both
  fingertips inside the box, so a still hand with the thumb just outside kept
  no dots and a "keep your hand inside the frame" hint. They now need the
  fingertips only where the dots are drawn, between the dimmed edges. The palm
  must still be in the box before the check starts.
- **Faster setup on slow networks.** The SDK no longer waits for its feature
  flags before asking your `sessionProvider` for a session (a first check on the
  device used to), and fetches Very's receipt key while your server creates the
  session instead of after. On a network where Very is slow to reach, setup now
  pays one connection timeout instead of up to three in a row. Android also
  starts downloading the liveness models when `check()` is called, as iOS does.

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
