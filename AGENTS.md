# AGENTS.md — very-ai-liveness-android

Prebuilt AAR distribution of the VeryAILiveness Android SDK (v1.1.1).
Maven Central (`org.very:liveness`) is canonical; the AAR in git is a
mirror. Every release regenerates this repository except `.github/` and
`.markdownlint.jsonc`, so an edit here does not survive the next release.

## Release invariants

- Each release is one commit on `main` plus the tag `1.1.1`; tags never move.
- `1.1.1` appears in the AAR filename, `README.md`, this file and
  `demo/app/build.gradle`; a release updates all of them together.
- `org.very:sdk-native-bundle` is no longer published or needed.

## Demo (`demo/`)

- Gradle project, app module `:app`, namespace `org.very.liveness.example`;
  compileSdk/targetSdk 34, minSdk 23, Java and Kotlin JVM target 11; JDK 17
  runs Gradle (AGP 8.5.2).
- Depends on `org.very:liveness:1.1.1` from Maven Central.
- Build types in `demo/app/build.gradle`: `debug`, `release` (signed with
  the `DEMO_*` environment keystore, else the debug key) and `minified`
  (debug-signed, R8 full mode; `./gradlew :app:installMinified`).
- `-PdemoVersionCode` / `-PdemoVersionName` override the version.
- The check needs a camera: run the demo on a device.

## Testing and CI

- No automated tests. CI only lints Markdown: `npx markdownlint-cli2 "**/*.md"`.
- Don't commit keystores, signing passwords or SDK keys.
