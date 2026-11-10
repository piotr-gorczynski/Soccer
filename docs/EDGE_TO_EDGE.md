# Edge-to-edge display (17.25)

Both markets use `BaseActivity`. On API 26 and newer it enables edge-to-edge with
the existing AndroidX Core dependency and applies system-bar, display-cutout and keyboard
insets to the app content container. Insets use the maximum on each edge rather
than adding overlapping areas. Repeated dispatches replace the previous insets;
child layout padding is preserved. Insets are consumed at this common boundary
to prevent a second application in descendants. App-owned screens have custom
toolbars (the application theme has no framework action bar). External SDK
activities are outside this boundary.

The app always uses a light theme. Dark status and navigation icons are selected
explicitly for the white background, including when focus returns from another
window. Enabling edge-to-edge alone does not set these icon appearance flags.
API 24/25 keep their original fitted window and dark navigation bar because
those releases cannot display dark navigation icons.

`BaseActivityInsetsTest` covers system bars, asymmetric cutouts, keyboard
show/hide, repeated dispatch, changed orientation insets and content replacement
on API 30 and 36, icon contrast and focus restoration, plus the native fitted
window on API 24. These are layout logic tests, not a visual device certification.

Before release, visually check Global and Bangladesh on Android 15/16 with
gesture navigation and three-button navigation: menu, Settings, login and its
keyboard, nickname, Terms, tournament rules and all confirmation buttons, and
the game board. Check portrait/landscape where supported, a camera cutout and
large font settings. Confirm system icons remain readable and game touch targets
still align with the board. Check return from full-screen ads separately.

The Play warning for `SHORT_EDGES` is distinct from app layout insets. Inspection
of the official Google Mobile Ads 25.2.0 AAR found a literal value of 1 assigned
to `layoutInDisplayCutoutMode` in `ads.internal.overlay.zzm`, and cutout handling
in `ads.internal.util.zzx`. The official 25.5.0 artifact still has these writes.
No SDK update or bytecode patch is included. Existing local R8 mapping maps
`zz0.q` to a synthesized LayoutParams API outline; it is not a handwritten app
method. This local mapping is not claimed to be the exact published BD mapping.
The SDK warning may remain until Google changes the SDK implementation.

References:
- https://developer.android.com/develop/ui/views/layout/edge-to-edge
- https://developer.android.com/about/versions/15/behavior-changes-15
- https://developers.google.com/admob/android/rel-notes
