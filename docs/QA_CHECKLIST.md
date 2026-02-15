# QA Checklist

## Permissions flow
1. Fresh install the app.
2. Tap **Grant access** and deny permissions.
3. Verify **Open Settings** is shown and opens App Info settings.
4. Grant media permissions in Settings and return to the app.
5. Verify the media queue appears.

## Swipe actions
1. In the main queue, swipe right on an item.
2. Verify the item is kept and the queue advances.
3. Swipe left on an item.
4. Verify the item is marked for delete and selected count increases.
5. Tap **Undo** and verify the last action is reverted.

## Delete confirmation flow
1. Mark at least one item for deletion.
2. Tap **Free space**.
3. Verify confirmation dialog appears when confirmation is enabled.
4. Tap **Cancel** and verify no deletion happens.
5. Repeat and tap **Continue**, then confirm Android system delete dialog.

## Paywall flow
1. Reach free deletion limit on a non-Pro account.
2. Verify paywall appears.
3. Tap **Upgrade to Pro** and verify billing flow starts (or a clear message is shown if unavailable).
4. Tap **Restore purchases** and verify restore status message.
5. Tap **Not now** and verify paywall closes.

## Empty queue and rescan
1. Process all current queue items until empty state is visible.
2. Verify **Rescan** button is shown.
3. Tap **Rescan** and verify queue is rebuilt.

## Smart Mode
1. Open Settings and enable Smart Mode.
2. Verify ordering in **All** filter follows smart ranking (high-impact media first).
3. Open **What is this?** and verify explainer dialog appears.
4. Tap **Turn off** and verify Smart Mode is disabled and dialog closes.
5. Restart app and verify Smart Mode toggle state persists.

## App language override
1. Open Settings -> App language.
2. Select **Use system language** and verify app returns to system locale.
3. Select **Hebrew (he)** and verify RTL layout renders correctly on main/settings/paywall/permission/smart info screens.
4. Select **Arabic (ar)** and verify RTL layout renders correctly on main/settings/paywall/permission/smart info screens.
5. Select **Persian (fa)** and verify RTL layout renders correctly on main/settings/paywall/permission/smart info screens.
6. Verify language change is applied consistently (immediately or after app restart, depending on OS behavior).
