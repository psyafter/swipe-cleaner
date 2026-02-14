# Swipe Cleaner

Offline-first Android cleaner for quickly reviewing media with swipe gestures.

## Build APKs

From a clean clone:

```bash
./gradlew assembleDebug
```

Or build module-specific variants:

```bash
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
```

Install debug build:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```


> Note: `gradle-wrapper.jar` is intentionally not committed in this branch (binary excluded for PR policy).
> If your clone is missing it, run `gradle wrapper` locally once to regenerate `gradle/wrapper/gradle-wrapper.jar`.

## Billing setup

One-time Pro product id is defined in `app/src/main/java/com/swipecleaner/Models.kt` as:

```kotlin
const val PRO_PRODUCT_ID = "pro_unlock"
```

Change this value to your Play Console INAPP product id if needed.

## Free tier limit

Free delete quota is defined in `app/src/main/java/com/swipecleaner/Models.kt`:

```kotlin
const val FREE_DELETE_LIMIT = 100
```

Adjust the value to change allowed free deletions.
