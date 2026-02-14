# Swipe Cleaner Technical Design

## Architecture
- Presentation: Compose UI + ViewModel state machine.
- Domain: filtering/sorting/size calculator utils.
- Data: MediaRepository using MediaStore queries and ContentResolver deletion.

## Key classes
- `SwipeCleanerViewModel`: orchestrates permission, scan, swipe actions, undo, delete confirmation.
- `MediaRepository`: paged-like metadata fetch from MediaStore for images/videos.
- `SwipeCleanerScreen`: single-card swipe UI and action controls.
- `MediaFilters`: deterministic presets and size utilities.

## MediaStore strategy
- Query images and videos separately, merge and sort by recency.
- Pull metadata only: ID, URI, size, date, mime, relative path, bucket.
- Never load full bitmaps in memory; UI requests thumbnail/image lazily through Coil.

## Performance & memory
- Metadata-first scan in IO coroutine.
- Keep only queue metadata in RAM.
- Render one active card.
- Avoid pre-decoding large bitmaps.

## Deletion strategy
- Android 11+ (`R`): `MediaStore.createDeleteRequest` and system confirmation.
- Android 8-10: best-effort direct `ContentResolver.delete`; if OEM policy blocks, user sees cancellation/error info.

## Extensibility
- M4 adds filter selector bound to `MediaFilters` presets.
- M5 adds BillingClient + entitlement state.
- M6 polish adds animations, accessibility, and release hardening.
