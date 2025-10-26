# MenuActivity.showMissedInviteDialog Crash Fix Summary

## Issue
App was potentially crashing with `WindowLeaked` exception in `MenuActivity.showMissedInviteDialog()` when the activity is finishing or destroyed while attempting to show an AlertDialog.

## Problem Analysis

The crash occurred in `MenuActivity.showMissedInviteDialog()` method due to missing defensive checks:

### The Problem Flow:
1. **checkForMissedInvitations()**: `showMissedInviteDialog()` is called asynchronously after Firestore query completes
2. Between this call and the actual dialog display, the activity could be finishing or destroyed
3. **showMissedInviteDialog() (BEFORE FIX)**: `AlertDialog.Builder.show()` attempts to show the dialog
4. **BUG**: If the activity is finishing/destroyed, showing a dialog causes `WindowLeaked` exception
5. This exception crashes the app or causes ANR (Application Not Responding)

### Why This Happens:
- User might navigate away quickly before the dialog is shown
- System might kill the activity due to memory pressure  
- Activity lifecycle could cause premature destruction during the async Firestore operation
- The dialog needs a valid window token which is not available when activity is finishing/destroyed

## Root Cause

The `showMissedInviteDialog()` method showed an AlertDialog without checking if the activity is still in a valid state to display dialogs. This is a common Android crash pattern, especially when dialogs are shown in response to asynchronous callbacks.

## The Fix

Added defensive check following the same pattern used in `GameActivity.showWinner()`, `MenuActivity.loadInterstitialAd()`, and `LanguageSelectionActivity.showLanguageSelectionDialog()`:

```java
private void showMissedInviteDialog() {
    // Check if activity is still valid before showing dialog
    if (isFinishing() || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && isDestroyed())) {
        Log.w(
                "TAG_Soccer",
                getClass().getSimpleName() + ".showMissedInviteDialog: Activity finishing or destroyed, skipping dialog"
        );
        return;
    }
    
    // ... rest of the method
}
```

## Example Behavior

### Before Fix:
1. User opens MenuActivity
2. Firestore query runs to check for missed invitations
3. User presses back button or system kills activity
4. Firestore callback executes and tries to show dialog → **CRASH: WindowLeaked**

### After Fix:
1. User opens MenuActivity
2. Firestore query runs to check for missed invitations
3. User presses back button or system kills activity
4. Firestore callback executes, detects activity is finishing/destroyed
5. Dialog is not shown, warning is logged
6. App continues gracefully without crash

## Impact

- **Crash Prevention**: Eliminates `WindowLeaked` exceptions from MenuActivity.showMissedInviteDialog
- **ANR Prevention**: Prevents potential ANRs related to dialog display timing
- **User Experience**: Smoother app behavior during navigation and lifecycle transitions
- **Consistency**: Matches defensive patterns used throughout the codebase

## Files Changed

1. **MenuActivity.java**
   - Added defensive check in `showMissedInviteDialog()` before showing dialog
   - Minimal change: 9 lines added

2. **MenuActivityCrashTest.java**
   - Added test method `testMissedInviteDialogStringResources()` 
   - Validates that all required string resources exist
   - 31 lines added

## Related Code Patterns

This fix follows the established defensive pattern used in:
- `GameActivity.showWinner()`
- `MenuActivity.loadInterstitialAd()`
- `LanguageSelectionActivity.showLanguageSelectionDialog()`
- `InAppMessagingHelper.showDialog()`

## Verification

✅ Code follows existing defensive patterns in the codebase
✅ Minimal change - only 9 lines added with warning log
✅ Consistent with GameActivity, LanguageSelectionActivity fixes
✅ Unit test added to validate string resources
✅ No new dependencies added
✅ Defensive check matches Android best practices
✅ Code compiles successfully

## Testing Recommendations

1. Test rapid navigation away from MenuActivity while loading
2. Test with Developer Options → Don't keep activities enabled
3. Test with low memory conditions
4. Test receiving invitations while offline then coming back online
5. Verify dialog shows correctly when activity is active
6. Verify no crashes when navigating away during Firestore callback

## References

- Android WindowLeaked exception: https://developer.android.com/guide/components/activities/activity-lifecycle
- Activity lifecycle best practices: https://developer.android.com/guide/components/activities/intro-activities
- Related fix: LanguageSelectionActivity crash fix in LANGUAGE_SELECTION_CRASH_FIX.md
- Related fix: GameActivity showWinner ANR fix in ANR_FIX_SUMMARY.md
