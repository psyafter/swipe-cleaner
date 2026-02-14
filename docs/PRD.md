# Swipe Cleaner PRD (MVP → Beta)

## Product goal
Deliver a 15-second wow: user opens app, grants permission, instantly swipes through media to keep/delete and quickly free storage.

## Audience
- Android users with overloaded gallery (especially low-storage devices).
- Users who trust local-only tools and avoid cloud upload.

## Core scenarios
1. First launch → grant media permission → see first card in under ~15 sec.
2. Swipe right to keep, left to mark delete.
3. See running counter of reclaimable storage.
4. Undo previous action if mistaken.
5. Confirm deletion using system-safe dialog.

## Screens
1. Permission gate.
2. Swipe deck (single card focus) + quick actions.
3. Delete confirmation prompt (system UI).
4. (Beta) filters/presets and paywall.

## MVP scope (M0-M3)
- Offline-only scan via MediaStore.
- Single-card swipe flow with Keep/Delete/Archive/Move actions.
- Undo 1 step.
- Size counter for delete selection.
- Secure deletion request (`MediaStore.createDeleteRequest`) on Android 11+.
- Basic fallback deletion on Android 8-10.

## Out of scope until M4+
- Background worker analytics upload.
- Account/sign-in/cloud backup.
- Batch duplicate detection.

## Monetization
- Free tier: first 100 deletions.
- Pro one-time unlock: $2.99 via Google Play Billing.

## Privacy
- No server communication.
- No personal media uploads.
- Only local metadata required for sorting and deletion.

## Google Play compliance highlights
- Request only media permissions needed.
- In-app explanation before permission prompt.
- Use system-managed delete confirmation flow.
- Clear privacy policy: local-only processing, no data sharing.
