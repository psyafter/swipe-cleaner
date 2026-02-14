# Google Play Data safety answers — Swipe Cleaner

Use this as a practical template when filling Play Console forms.

## Core answers

- **Does the app collect or share data?**
  - **Answer:** No, the app does not collect or share user data to developer servers.
  - **Note:** Billing transactions are handled by Google Play.

- **Is data processed securely?**
  - **Answer:** Data is processed locally on the device.

- **What permissions are used?**
  - **Answer:** Photo/Video permissions are used to access on-device media for browsing and deletion.

## Suggested Data safety form mapping

- Data collection: **No**
- Data sharing: **No**
- Data is encrypted in transit: **Not applicable** (no app-server transmission)
- Data deletion request: **Not applicable** (no server-side account/data)

## Photo & Video permissions declaration

### Core functionality justification

Swipe Cleaner’s core functionality is gallery cleanup:

- browse media files,
- review files quickly,
- delete user-selected items.

Photo and video access is required for this core user-facing flow.

### Explicit non-usage statements

The app does **not** use media access for:

- background scanning unrelated to active user action,
- uploading files to remote servers,
- advertising profiling.

### Recommended improvement (optional roadmap)

To reduce broad access requirements, consider implementing Android 14+ partial media access support where users can grant access to selected photos/videos only.
