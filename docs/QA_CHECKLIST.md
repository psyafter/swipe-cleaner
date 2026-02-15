# QA Checklist

## Scope
Manual QA pass for visible actions and gestures in Swipe Cleaner.

## Devices / OS
- Android 13+ (recommended main flow)
- Android 10/11 (legacy permission + delete flow)

## Preconditions
- Install a debug/release build with media files available.
- Have at least images/videos across different sizes/ages.
- If billing test is needed, configure Play test account/product in internal testing.

## Test Cases

### 1) First launch onboarding
1. Launch app on fresh install.
2. Verify onboarding content is shown.
3. Tap **Start cleaning**.
4. Re-open app.

**Expected**
- Onboarding appears only on first launch.
- Start cleaning opens permission/main flow.
- On next launch onboarding is skipped.

### 2) Permissions flow
1. On first permission prompt, deny access.
2. Verify **Open Settings** button appears.
3. Tap **Open Settings**, grant media permission, return.
4. Trigger **Grant access** again if needed.

**Expected**
- Denied state has clear message and working Open Settings path.
- After permission granted, app scans and shows queue.

### 3) Swipe and action buttons
1. On media card swipe right.
2. On next card swipe left.
3. Use **Keep** and **Delete** buttons on next items.
4. Tap **Undo**.

**Expected**
- Right swipe keeps item and advances queue.
- Left swipe marks for delete and updates selected count/size.
- Buttons mirror swipe behavior.
- Undo restores last item/action.

### 4) Filter chips + queue updates
1. Tap each filter chip (All/Big/Old/Shots/WhatsApp).
2. Observe queue count and selected/free-space text.

**Expected**
- Queue is rescanned per filter.
- "You can free X" reflects current marked set (resets after rescan).

### 5) Delete session confirmation + system confirmation
1. Mark 1+ items for delete.
2. Tap **Free space (N)**.
3. Verify app confirmation dialog summary (count + estimated size).
4. Tap Continue and complete Android system confirmation.

**Expected**
- App shows pre-delete confirmation when setting enabled.
- Android system confirmation appears after app confirm.
- Success dialog shows correct count/freed size.

### 6) Settings toggle: require confirmation
1. Open **Settings**.
2. Disable "Require confirmation before deleting session".
3. Mark items and tap Free space.
4. Re-enable setting.

**Expected**
- When disabled: app confirmation dialog is skipped; system confirmation still appears.
- Toggle persists after reopening app.

### 7) Empty queue UX
1. Process all items until queue is empty.
2. Verify **All done** state.
3. Tap **Rescan**.
4. Try quick suggestions (**Big files**, **Old files**).

**Expected**
- Empty state has no dead end.
- Rescan and suggestion actions trigger scanning/filtering.

### 8) Paywall / free limit behavior
1. Reach free delete limit.
2. Attempt another delete session.
3. Tap **Not now**.
4. Open paywall again and tap **Restore purchases**.
5. Tap **Upgrade to Pro** (if billing configured).

**Expected**
- Paywall appears only when limit exceeded for deletion.
- Not now dismisses paywall and app remains usable.
- Restore triggers status message/state update.
- Buy triggers purchase flow if product available.

### 9) Post-delete success dialog actions
1. Perform successful deletion.
2. In success dialog tap **Continue cleaning**.
3. Perform another deletion and tap **Rate app**.

**Expected**
- Continue closes dialog and returns to flow.
- Rate opens Play Store (or web fallback) and dialog closes.

## Notes
- App remains offline/private-first (no analytics/network features added by this iteration).
- Billing uses Play Billing SDK flow only.
