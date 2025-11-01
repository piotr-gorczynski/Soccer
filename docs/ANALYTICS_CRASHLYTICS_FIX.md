# AnalyticsManager Crashlytics Error Fix

## Issue
Crashlytics was reporting errors from `AnalyticsManager.trackSignupError` and `trackTournamentJoinError` methods. These appeared as error reports with stacktraces in the Crashlytics dashboard.

## Root Cause
The methods were using `crashlytics.recordException(new Exception("..."))` to track signup and tournament join errors. This API intentionally creates non-fatal exception reports in Crashlytics, which appear as "errors" in the dashboard even though they're not actual app crashes.

While this is a valid use of the Crashlytics API, it creates noise in error reports because:
1. These are not actual app crashes or bugs - they're expected business logic errors (e.g., network failures, invalid credentials)
2. The stacktrace points to the tracking code, not the actual source of the error
3. They make it harder to identify real bugs in the Crashlytics dashboard

## Solution
Removed the `crashlytics.recordException()` calls from both methods while keeping the `crashlytics.log()` calls. This change:

### In `trackSignupError` (line 115):
**Before:**
```java
crashlytics.recordException(new Exception("Signup error: " + safeErrorMessage));
crashlytics.log("Signup error - Method: " + safeMethod + ", Step: " + safeStep + ", Error: " + safeErrorMessage);
```

**After:**
```java
crashlytics.log("Signup error - Method: " + safeMethod + ", Step: " + safeStep + ", Error: " + safeErrorMessage);
```

### In `trackTournamentJoinError` (line 269):
**Before:**
```java
crashlytics.recordException(new Exception("Tournament join error: " + safeErrorMessage));
crashlytics.log("Tournament join error: " + safeTournamentId + " - " + safeErrorMessage);
```

**After:**
```java
crashlytics.log("Tournament join error: " + safeTournamentId + " - " + safeErrorMessage);
```

## Benefits
1. **Cleaner Crashlytics Dashboard**: No more synthetic exception reports for expected errors
2. **Better Signal-to-Noise Ratio**: Real bugs and crashes are easier to identify
3. **Maintained Tracking**: We still log these events to Crashlytics for debugging, they just don't appear as errors
4. **Maintained Analytics**: Firebase Analytics tracking is unchanged
5. **Maintained Null Safety**: All existing null safety checks remain in place

## Impact
- **No functional changes**: The methods still track errors and log them
- **No breaking changes**: All null safety protections remain
- **Better user experience for developers**: Crashlytics dashboard is cleaner and more useful

## Files Modified
- `mobile/app/src/main/java/piotr_gorczynski/soccer2/AnalyticsManager.java`
  - Lines 115-116: Removed `recordException` from `trackSignupError`
  - Line 117: Updated error message text
  - Lines 268-269: Removed `recordException` from `trackTournamentJoinError`

## Testing
The existing unit tests in `AnalyticsManagerTest.java` already verify:
- Methods handle null parameters without crashing
- Methods handle null Firebase instances gracefully
- No NullPointerException is thrown in any scenario

These tests remain valid as we only removed synthetic exception creation, not any safety checks.

## Related
- This fix specifically addresses Crashlytics error reports
- Does not affect the null safety fixes previously implemented
- Complements the existing error tracking infrastructure
