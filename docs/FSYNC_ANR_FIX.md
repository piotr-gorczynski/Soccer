# fsync ANR Fix - SharedPreferences commit() to apply()

## Problem

The application was experiencing ANR (Application Not Responding) errors due to synchronous `fsync` system calls during SharedPreferences write operations. The stack trace from Crashlytics showed the main thread being blocked while waiting for disk I/O operations to complete.

### Error Details
- **Issue**: `fsync` system call blocking the main thread
- **Component**: SharedPreferences write operations
- **Method**: `SharedPreferences.Editor.commit()`
- **Impact**: ANR errors reported in Crashlytics, frozen UI during preference saves
- **Crashlytics Report**: ANR session 6911AFC9004300011EBE9B9D60541038

## Root Cause

The `SharedPreferences.Editor.commit()` method performs synchronous disk I/O operations including:
1. Writing preference data to disk
2. Calling `fsync` to ensure data is physically written to storage
3. Blocking the calling thread until all I/O operations complete

When `commit()` is called on the main thread (especially during activity lifecycle methods like `onResume()`), the following can happen:
1. Disk I/O operations take significant time (especially on slow storage or during heavy I/O)
2. The `fsync` system call blocks waiting for hardware to complete write operations
3. The main thread is blocked, causing the UI to freeze
4. If the total time exceeds Android's ANR threshold (~5 seconds), an ANR is triggered

## Solution

Replace synchronous `commit()` calls with asynchronous `apply()` for SharedPreferences operations that don't require immediate persistence guarantees.

### Key Differences: commit() vs apply()

| Feature | commit() | apply() |
|---------|----------|---------|
| **Synchronous** | Yes - blocks until complete | No - returns immediately |
| **Return Value** | boolean (success/failure) | void |
| **fsync** | Yes - forces disk write | Yes - but on background thread |
| **Thread** | Caller thread (often main thread) | Background thread |
| **Use Case** | When immediate success confirmation needed | Normal preference saves |

## Changes Made

### 1. LanguageManager.java (line 80)

**Before:**
```java
public static void setLanguage(Context context, String languageCode) {
    // Save to SharedPreferences
    context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            .edit()
            .putString(PREF_LANGUAGE_CODE, languageCode)
            .commit();
```

**After:**
```java
public static void setLanguage(Context context, String languageCode) {
    // Save to SharedPreferences asynchronously to prevent fsync ANR
    context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            .edit()
            .putString(PREF_LANGUAGE_CODE, languageCode)
            .apply();
```

**Context:** This method is called when the user changes their language preference. The language is also saved to Firestore asynchronously, so there's no need for synchronous SharedPreferences commit.

### 2. MenuActivity.java (line 617)

**Before:**
```java
if (auth.getCurrentUser() == null) {
    // Remove only user-specific data, preserve language preferences and other device settings
    SharedPreferences.Editor ed = prefs.edit();
    ed.remove("uid")
      .remove("email")
      .remove("nickname")
      .remove("method")
      .remove("facebookId")
      .remove("facebookName")
      .remove("facebookPhotoUrl")
      .remove("fcmToken");
    ed.commit();
```

**After:**
```java
if (auth.getCurrentUser() == null) {
    // Remove only user-specific data, preserve language preferences and other device settings
    SharedPreferences.Editor ed = prefs.edit();
    ed.remove("uid")
      .remove("email")
      .remove("nickname")
      .remove("method")
      .remove("facebookId")
      .remove("facebookName")
      .remove("facebookPhotoUrl")
      .remove("fcmToken");
    // Use apply() instead of commit() to prevent fsync ANR
    ed.apply();
```

**Context:** This code runs in `continueOnResumeAfterBackendCheck()` during activity `onResume()`. Clearing user data doesn't need to be synchronous, and using `apply()` prevents blocking the main thread during this critical lifecycle method.

## Benefits

1. **Eliminates ANR**: No more blocking fsync operations on the main thread
2. **Improved Responsiveness**: UI remains responsive during preference saves
3. **Better UX**: Users don't experience freezes when preferences are being saved
4. **Maintains Data Integrity**: `apply()` still ensures data is written to disk, just asynchronously
5. **Minimal Risk**: Both operations involved non-critical data that doesn't require immediate confirmation

## Trade-offs

- **No Immediate Confirmation**: `apply()` doesn't return a boolean indicating success/failure
- **Slight Delay**: Data write happens asynchronously (typically within milliseconds)
- **Process Death**: If the app process is killed immediately after `apply()`, changes might not be persisted (very rare)

**Risk Assessment**: Both changed locations are low-risk for asynchronous writes:
- Language preference: Also saved to Firestore, can be reloaded on next launch
- User data removal: Happens on sign-out, and data can be cleared again if needed

## Testing

The fix was verified by:
- ✅ Code review to ensure correct method substitution
- ✅ Verification that changed locations don't require immediate persistence
- ✅ Confirmation that no return value is used from `commit()`
- ✅ Review of Android best practices for SharedPreferences

## Files Modified

- `mobile/app/src/main/java/piotr_gorczynski/soccer2/LanguageManager.java` (line 80)
- `mobile/app/src/main/java/piotr_gorczynski/soccer2/MenuActivity.java` (line 617)

## Related Documentation

- [PROFILEINSTALLER_ANR_FIX.md](./PROFILEINSTALLER_ANR_FIX.md) - Previous ANR fix for ProfileInstaller
- [MENUACTIVITY_ANR_FIX.md](./MENUACTIVITY_ANR_FIX.md) - ANR fix for bitmap processing
- [ANR_FIX_SUMMARY.md](./ANR_FIX_SUMMARY.md) - ANR fix for async Firestore operations
- [Android SharedPreferences Best Practices](https://developer.android.com/reference/android/content/SharedPreferences.Editor)
- [Android ANR Best Practices](https://developer.android.com/topic/performance/vitals/anr)

## Additional Notes

- This fix specifically addresses ANR issues related to `fsync` system calls during SharedPreferences operations
- The pattern can be applied to other SharedPreferences write operations throughout the app
- **When to use commit()**: Only when you need immediate confirmation of write success/failure (rare cases)
- **When to use apply()**: For all normal preference saves (recommended default)
- This change makes the app more resilient to disk I/O delays and improves overall responsiveness
- The Android framework guarantees that `apply()` writes will complete before component state changes (like activity destruction)

## References

From Android documentation:
> Unlike commit(), which writes its preferences out to persistent storage synchronously, apply() commits its changes to the in-memory SharedPreferences immediately but starts an asynchronous commit to disk and you won't be notified of any failures. If another editor on this SharedPreferences does a regular commit() while a apply() is still outstanding, the commit() will block until all async commits are completed as well as the commit itself.
