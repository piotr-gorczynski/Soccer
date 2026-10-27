# Stable MenuActivity loading

The menu starts with its existing localized loading overlay visible and opaque.
Every resume and connectivity recovery covers the menu before checking backend
availability. Profile and Terms reads may update controls underneath, but the
menu is revealed only after those operations settle. Terms/nickname navigation
keeps the menu covered until the destination takes over; resume checks afresh.

The loading card reuses the GameActivity style and menu_loading_message from
existing localized resources. A minimum 250 ms display avoids flashing the loader
itself. There is no fade-in exposing intermediate controls or delayed fade-out
that can uncover a newly started check. Covered content is hidden from accessibility.

A backend/profile/Terms failure reveals the existing offline UI. A 20-second
initialization limit prevents a permanent blocker; late callbacks from an expired
check cannot restore a stale online state. The existing bounded profile reader
still handles its own retries. Timers are removed on stop/destroy.

Menu readiness and ad/navigation loading own the overlay independently: neither
can hide the other. Ad loading retains its dimmed background; resolving menu state
uses an opaque background so button changes are not visible through the mask.

Automated coverage: initial frame, both completion orders, backend and profile
failures, timeout with a late callback, ad-overlay ownership and queued-hide
cancellation. Existing profile retry and Terms failure tests remain applicable.

Manual DEV checks for both flavours:
1. Cold start while signed out: loading, then the stable signed-out menu.
2. Sign in with an existing profile: loading, then the greeting and final buttons.
3. Return from Account, language selection or a game: no intermediate button states.
4. Launch offline with a cached nickname: offline controls and nickname remain usable.
5. Restore connectivity: loading, then the online menu without button flicker.
6. Slow/failing network: menu eventually becomes offline; it never stays blocked.
7. First-time onboarding: Terms and nickname screens still open in their existing order.
