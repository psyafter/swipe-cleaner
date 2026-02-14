# Swipe Cleaner

Offline-first Android app for fast gallery cleanup with swipe gestures.

## Version

Current release version:

- `versionCode = 100`
- `versionName = "1.0.0"`

Defined in `app/build.gradle.kts`.

## Build release APK / AAB

### 1) Create local signing key (do not commit)

```bash
keytool -genkeypair \
  -v \
  -storetype PKCS12 \
  -keystore ./keystore/swipe-cleaner-release.keystore \
  -alias swipe-cleaner \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

### 2) Create local `keystore.properties` in repository root

```properties
storeFile=keystore/swipe-cleaner-release.keystore
storePassword=CHANGE_ME
keyAlias=swipe-cleaner
keyPassword=CHANGE_ME
```

If `keystore.properties` is absent, local release build uses debug signing only for local verification.

### 3) Build release artifacts

Build AAB (Play Console upload artifact):

```bash
./gradlew :app:bundleRelease
```

Build release APK:

```bash
./gradlew :app:assembleRelease
```

## Billing and limits constants

- `PRO_PRODUCT_ID` location: `app/src/main/java/com/swipecleaner/AppConstants.kt`
- `FREE_DELETE_LIMIT` location: `app/src/main/java/com/swipecleaner/AppConstants.kt`

## Google Play publishing and keys

- Upload to Play Console: **AAB only** (`app-release.aab`).
- Enable **Play App Signing** (recommended default flow).
- Keep upload keystore and credentials local only.
- Never commit keystore files, passwords, or `keystore.properties`.

## Quick commands

```bash
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
./gradlew :app:bundleRelease
```
