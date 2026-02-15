# I18N QA Checklist

## Switch app language to Hebrew / Arabic / Persian

### Android 13+ (API 33 and newer)
1. Open Android Settings.
2. Go to **System > Languages & input > App languages**.
3. Select **Swipe Cleaner**.
4. Choose **Hebrew**, **Arabic**, or **Persian**.
5. Reopen the app and verify UI text and direction.

### Android 12 and older
1. Open **Swipe Cleaner**.
2. Go to **Settings**.
3. Open **App language**.
4. Select **Hebrew**, **Arabic**, or **Persian**.
5. Confirm the "Language updated" message and verify UI text and direction.

## Additional locale spot checks (Batch 2)
- Chinese (Simplified, `zh-CN`)
- Chinese (Traditional, `zh-TW`)
- Ukrainian (`uk`)
- Serbian (`sr`)
- Thai (`th`)

For each language above:
1. Switch app language.
2. Open each required screen.
3. Verify no clipped text, no placeholder artifacts, and no crashes.

## Screens to check
- Permission screen
- Main screen (bottom bar)
- Paywall
- Settings
- Smart Mode info dialog

## What good looks like
- Primary buttons are fully visible and not clipped.
- Counters and badges do not overlap nearby content.
- Text alignment follows RTL direction naturally.
- Swipe actions and labels remain readable.
- Dialog buttons stay on-screen in both portrait and landscape.
