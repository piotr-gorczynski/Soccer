# ProfileInstaller ANR Fix

## Problem

The application was experiencing ANR (Application Not Responding) errors during app startup due to `androidx.profileinstaller.ProfileInstallerInitializer.writeInBackground` blocking the main thread.

### Error Details
- **Component**: `androidx.profileinstaller.ProfileInstallerInitializer`
- **Method**: `writeInBackground`
- **Issue**: Blocking I/O operations on main thread during app initialization
- **Impact**: ANR errors reported in Crashlytics, poor user experience during app launch

## Root Cause

The ProfileInstaller library is included transitively when using AndroidX libraries. By default, it automatically initializes during app startup through the `androidx.startup.InitializationProvider`. During initialization, ProfileInstaller attempts to:

1. Read baseline profile information
2. Write optimized profiles to disk
3. These I/O operations can take significant time and block the main thread
4. If these operations take too long, Android detects the app as unresponsive and triggers an ANR

## Solution

Disable the automatic initialization of ProfileInstaller by adding a manifest entry that removes it from the startup initialization process.

### Implementation

Added the following to `AndroidManifest.xml`:

```xml
<!-- Disable ProfileInstaller to prevent ANR during app startup -->
<provider
    android:name="androidx.startup.InitializationProvider"
    android:authorities="${applicationId}.androidx-startup"
    android:exported="false"
    tools:node="merge">
    <meta-data
        android:name="androidx.profileinstaller.ProfileInstallerInitializer"
        android:value="androidx.startup"
        tools:node="remove" />
</provider>
```

### How It Works

1. **`androidx.startup.InitializationProvider`**: This is the component that automatically initializes various androidx libraries during app startup
2. **`tools:node="merge"`**: Merges with existing startup provider configuration
3. **`tools:node="remove"`**: Removes the ProfileInstallerInitializer from the list of components to initialize
4. **Result**: ProfileInstaller is no longer automatically initialized, preventing blocking I/O on the main thread

## Benefits

1. **Eliminates ANR**: No more blocking I/O operations on the main thread during startup
2. **Faster Startup**: App launches more quickly without waiting for profile writing
3. **Better UX**: Users experience a more responsive app launch
4. **Graceful Degradation**: The app still functions normally without automatic profile installation
5. **Minimal Impact**: No changes to app functionality, only affects startup initialization

## Trade-offs

- **Profile Optimization**: The app will not automatically install baseline profiles
- **Performance Impact**: Minimal - baseline profiles are primarily useful for improving performance on first launch after installation, and the benefit is often negligible for most apps
- **Manual Installation**: If needed, ProfileInstaller can still be invoked manually in code at a more appropriate time

## Testing

The fix was verified by:
- ✅ Successful build with the manifest changes
- ✅ Manifest merge verification to confirm ProfileInstallerInitializer is properly excluded
- ✅ Code review with no issues found
- ✅ Security scan passed
- ✅ No impact on existing app functionality

## Files Modified

- `mobile/app/src/main/AndroidManifest.xml`

## References

- [AndroidX Startup Documentation](https://developer.android.com/topic/libraries/app-startup)
- [ProfileInstaller Documentation](https://developer.android.com/topic/performance/baselineprofiles/overview)
- [Android ANR Best Practices](https://developer.android.com/topic/performance/vitals/anr)

## Additional Notes

- This fix only affects the automatic initialization of ProfileInstaller during app startup
- The ProfileInstaller library remains available in the app but is not automatically invoked
- If baseline profile optimization is needed in the future, it can be implemented manually at a more appropriate time (e.g., on a background thread after the app has fully launched)
- This change makes the app more resilient to I/O delays during startup
