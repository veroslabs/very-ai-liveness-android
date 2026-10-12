# AGENTS.md — very-ai-liveness-android

Prebuilt AAR distribution of the VeryAILiveness Android SDK (v1.0.65). Maven
Central (`org.very:liveness`, `org.very:sdk-native-bundle`) is canonical; the
AARs in git are a mirror. SDK source: `veroslabs/very-mobile-sdk`.

## Release invariants

- Keep `1.0.65` in sync: AAR filenames, README (Gradle, manual AAR, demo sections), `demo/app/build.gradle`.
- Do not combine `org.very:sdk-native-bundle` with `org.very:sdk` (jniLibs merge conflict).

## Demo (`demo/`)

- Gradle project, app module `:app`, namespace `org.very.liveness.example`; compileSdk/targetSdk 34, minSdk 23, Java/Kotlin 11.
- Build types in `demo/app/build.gradle`: `debug`, `release` (demo keystore via `DEMO_*` env vars, else debug key), `minified` (R8, debug-signed; `./gradlew :app:installMinified`).
- `versionCode`/`versionName` can be overridden with `-PdemoVersionCode` / `-PdemoVersionName` (used by the fastlane `demo_release` lane, which lives in `very-mobile-sdk`).
- Run on a real ARM device; the emulator is unsupported for the native library.

## Testing and CI

- No automated tests. CI only lints Markdown: `npx markdownlint-cli2 "**/*.md"`.
- Don't commit keystores, signing passwords, or new SDK keys.
