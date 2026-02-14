# Milestones

## M0 skeleton
Done:
- Android Compose app skeleton, app theme, manifest permissions.
How to test:
- Build and launch app shell.
Known limits:
- No scan/UI logic.

## M1 scan
Done:
- MediaStore scan for images/videos with metadata queue.
How to test:
- Grant permission and verify scanned count message.
Known limits:
- No advanced filters.

## M2 swipe UI
Done:
- Single-card UI, gestures, action buttons, delete size counter.
How to test:
- Swipe left/right or tap actions; observe counters.
Known limits:
- Archive/Move are placeholders.

## M3 delete + undo
Done:
- Undo one action.
- Secure deletion confirmation flow for Android 11+.
- Fallback direct delete for Android 8-10.
How to test:
- Mark items delete, undo once, confirm delete.
Known limits:
- Android 10 recoverable flow per-item not implemented yet.
