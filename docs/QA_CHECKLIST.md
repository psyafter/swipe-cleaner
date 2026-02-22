# QA Checklist

## Permissions flow
1. Fresh install the app.
2. Tap **Grant access** and deny permissions.
3. Verify **Open Settings** is shown and opens App Info settings.
4. Grant media permissions in Settings and return to the app.
5. Verify the media queue appears.

## Session summary + swipe actions
1. In the main queue, check **Session summary** card is visible.
2. Swipe right on an item and verify **Kept** increments.
3. Swipe left on an item and verify **Marked** and **Estimated freeable** update.
4. Tap **Undo** and verify the last action and counters are reverted.

## Review selected flow
1. Mark at least 2 items for deletion.
2. Tap **Review (N)** in the bottom action bar.
3. Verify only marked items are shown with thumbnail, size, date, and source badge (when detected).
4. Tap **Keep** on one reviewed item.
5. Go back and verify marked counter and freeable size are updated.

## Delete confirmation flow
1. Mark at least one item for deletion.
2. Tap **Free space**.
3. Verify confirmation dialog appears when confirmation is enabled.
4. Tap **Cancel** and verify no deletion happens.
5. Repeat and tap **Continue**, then confirm Android system delete dialog.
6. If system delete is canceled/failed, verify actionable error message appears.

## Smart Mode + filters interaction
1. Turn **Smart Mode** ON from main screen header.
2. Verify filter chips are disabled and hint text says to turn off Smart Mode for manual filters.
3. Turn **Smart Mode** OFF and verify filter chips become interactive.
4. Select different filters and verify queue changes.

## Empty queue and rescan loading/refresh
1. Process all current queue items until empty state is visible.
2. Verify **Rescan** button is shown.
3. Tap **Rescan** and verify loading indicator appears.
4. Verify queue refreshes after scan completes.

## Scan failure recovery
1. Deny media permission and trigger scan path.
2. Verify scan failure state appears with friendly text and **Try again** action.
3. Re-allow permission in Settings.
4. Tap **Try again** and verify queue loads successfully.

## App language override
1. Open Settings -> App language.
2. Select **Use system language** and verify app returns to system locale.
3. Select **Hebrew (he)** and verify RTL layout renders correctly on main/settings/paywall/permission/smart info screens.
4. Select **Arabic (ar)** and verify RTL layout renders correctly on main/settings/paywall/permission/smart info screens.
5. Select **Persian (fa)** and verify RTL layout renders correctly on main/settings/paywall/permission/smart info screens.
6. Verify language change is applied consistently (immediately or after app restart, depending on OS behavior).
