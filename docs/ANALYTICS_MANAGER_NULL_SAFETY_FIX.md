# AnalyticsManager Null Safety Fix

## Issue
This is a regressed issue where `AnalyticsManager` methods were crashing with `NullPointerException` when Firebase services (Crashlytics and/or FirebaseAnalytics) were null. The issue was previously fixed on 2025-09-25 for `trackSignupError` but has reappeared in other methods.

## Root Cause
Multiple methods in `AnalyticsManager.java` were calling Firebase Crashlytics and Analytics methods without first checking if the Firebase instances are null. This can happen when:
- Firebase services fail to initialize
- Context is null during initialization
- Firebase dependencies are unavailable
- Device is in an unusual state

## Methods Fixed
The following methods were updated to add null safety checks:

1. ✅ `trackSignupError` - Already had proper null checks (reference implementation)
2. ✅ `trackSignupDeclineReason` - **FIXED**
3. ✅ `trackTournamentListViewed` - **FIXED**
4. ✅ `trackTournamentJoinStart` - **FIXED**
5. ✅ `trackTournamentJoinSuccess` - **FIXED**
6. ✅ `trackTournamentJoinError` - **FIXED**
7. ✅ `trackAnonymousLinkPrompt` - **FIXED**
8. ✅ `trackAnonymousLinkDecision` - **FIXED**
9. ✅ `setUserProperties` - **FIXED**
10. ✅ `addAuthBreadcrumb` - **FIXED**
11. ✅ `addTournamentBreadcrumb` - **FIXED**

## Fix Pattern
All methods now follow this consistent pattern:

```java
public void someMethod(...) {
    // 1. Null-safe parameter handling (if needed)
    String safeParam = param != null ? param : "default_value";
    
    // 2. Local logging (always works, no Firebase dependency)
    Log.d(TAG, "Message: " + safeParam);
    
    // 3. Check Crashlytics is not null before using it
    if (crashlytics != null) {
        try {
            crashlytics.log("...");
            // or crashlytics.recordException(...);
        } catch (Exception e) {
            Log.e(TAG, "Failed to log to Crashlytics", e);
        }
    }
    
    // 4. Check FirebaseAnalytics is not null before using it
    if (firebaseAnalytics != null) {
        try {
            Bundle params = new Bundle();
            params.putString("key", safeParam);
            firebaseAnalytics.logEvent("event_name", params);
        } catch (Exception e) {
            Log.e(TAG, "Failed to log to Firebase Analytics", e);
        }
    }
}
```

## Changes Summary
- **Total lines changed**: 172 insertions, 54 deletions
- **File modified**: `mobile/app/src/main/java/piotr_gorczynski/soccer2/AnalyticsManager.java`
- **Type of change**: Bug fix (null safety)

## Testing
Existing unit tests in `AnalyticsManagerTest.java` verify:
- Methods handle null parameters without crashing
- Methods handle null Firebase instances gracefully
- No NullPointerException is thrown in any scenario

## Impact
This fix prevents crashes when:
- Firebase services are unavailable
- Firebase initialization fails
- App runs in degraded mode
- Analytics tracking encounters errors

All analytics calls are now defensive and will gracefully degrade to local logging only if Firebase services are unavailable.

## Related
- Original fix date: 2025-09-25
- Issue tracker: Mentioned in GOOGLE_PLAY_RELEASE_NOTES_v15.7.txt
