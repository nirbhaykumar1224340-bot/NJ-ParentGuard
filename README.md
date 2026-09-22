# NJ-ParentGuard

Android-first parental-control MVP built with Kotlin + Jetpack Compose.

## Current milestone
- Android project scaffold
- ParentGuard Compose launcher screen
- Explicit runtime permission flow
- Room model/DAO for offline location queue
- Notification Listener Service foundation
- Location, camera, notification, telephony permission declarations

## Planned milestones
1. Parent/child authentication and secure device pairing
2. Firebase backend + FCM
3. Background location capture and offline sync
4. Parent notification-history dashboard
5. Supported call/SMS event handling
6. Permission-based camera sharing
7. Device status and audit controls

## Privacy
The child device must explicitly grant Android permissions. The project does not implement permission bypass or hidden camera activation.
