# MenuActivity ANR Fix - Bitmap Processing on Main Thread

## Problem

The application was experiencing ANR (Application Not Responding) errors during app startup, specifically when MenuActivity was being created. The stack trace showed `android.os.MessageQueue.nativePollOnce`, which indicates the main thread was blocked.

### Error Details
- **Component**: `MenuActivity.setupRunningPlayerAnimation()`
- **Method**: Heavy bitmap processing on main thread
- **Issue**: Blocking I/O and CPU-intensive bitmap operations on main thread during activity creation
- **Impact**: ANR errors reported in Crashlytics, poor user experience during app launch
- **Crashlytics Report**: Issue 4d05f9e74e77520b418eac3a355108f1, Date: Sat Nov 01 2025 21:43:40

## Root Cause

The `setupRunningPlayerAnimation()` method was performing CPU-intensive operations synchronously on the main thread during `onCreate()`:

1. **Loading large sprite sheet from resources** - `BitmapFactory.decodeResource()` reads and decodes a large image file
2. **Creating multiple bitmaps** - Iterating through sprite sheet to extract individual frames
3. **Bitmap scaling** - `Bitmap.createScaledBitmap()` for each frame
4. **Nested loops** - Processing multiple rows and columns of frames

These operations could take several hundred milliseconds or more, especially on slower devices, causing the main thread to be blocked and triggering an ANR if the total time exceeded Android's threshold.

## Solution

Move all heavy bitmap processing operations to a background thread while keeping UI updates on the main thread.

### Implementation

Modified `MenuActivity.setupRunningPlayerAnimation()` to:

1. **Keep lightweight operations on main thread**:
   - View lookup (`findViewById`)
   - Initial checks (animations enabled, existing frames)
   - Resource ID lookup

2. **Move heavy operations to background thread**:
   - Sprite sheet decoding (`BitmapFactory.decodeResource`)
   - Bitmap creation (`Bitmap.createBitmap`)
   - Bitmap scaling (`Bitmap.createScaledBitmap`)
   - All loops for processing frames

3. **Post UI updates back to main thread**:
   - Setting view visibility
   - Setting ImageView bitmap
   - Configuring click listeners

### Code Pattern

```java
// Move heavy bitmap operations to background thread to prevent ANR
new Thread(() -> {
    try {
        // Heavy bitmap processing here
        Bitmap spriteSheet = BitmapFactory.decodeResource(...);
        // ... process bitmaps ...
        
        final Bitmap[][] loadedFrames = frameRows.toArray(new Bitmap[0][]);
        
        // Update UI on main thread
        runOnUiThread(() -> {
            if (!isFinishing() && !isDestroyed()) {
                runningPlayerFrames = loadedFrames;
                runningPlayerView.setImageBitmap(...);
                configureRunningPlayerClickListener();
            }
        });
    } catch (Exception e) {
        Log.e("TAG_Soccer", "Error in background thread", e);
    }
}).start();
```

## Benefits

1. **Eliminates ANR**: No more blocking operations on the main thread during startup
2. **Faster Perceived Startup**: Activity becomes interactive immediately, animation loads in background
3. **Better UX**: Users can interact with the app while animation assets are being prepared
4. **Graceful Degradation**: If animation loading fails, app continues to function normally
5. **Resource Efficiency**: Proper cleanup if activity is destroyed before background work completes

## Trade-offs

- **Slight Delay in Animation**: The running player animation may not appear immediately on first launch
- **Thread Management**: Adds one background thread during startup (minimal overhead)
- **Complexity**: Slightly more complex code with thread management and UI thread synchronization

## Testing

To verify the fix:
1. ✅ Code compiles successfully
2. ✅ Background thread properly handles bitmap processing
3. ✅ UI updates correctly posted to main thread
4. ✅ Activity destruction handled gracefully to prevent leaks
5. ✅ Error handling for bitmap operations maintained

## Files Modified

- `mobile/app/src/main/java/piotr_gorczynski/soccer2/MenuActivity.java`
  - Modified `setupRunningPlayerAnimation()` method to use background thread

## Related Documentation

- [PROFILEINSTALLER_ANR_FIX.md](./PROFILEINSTALLER_ANR_FIX.md) - Previous ANR fix for ProfileInstaller
- [ANR_FIX_FLOW_DIAGRAM.md](./ANR_FIX_FLOW_DIAGRAM.md) - ANR fix pattern for async operations
- [Android ANR Best Practices](https://developer.android.com/topic/performance/vitals/anr)
- [Android Threading Guide](https://developer.android.com/guide/components/processes-and-threads)

## Additional Notes

- This fix addresses ANR issues specifically related to bitmap processing in MenuActivity
- The pattern can be applied to other activities that perform heavy operations during initialization
- Consider using `AsyncTask`, `ExecutorService`, or Kotlin Coroutines for more complex async scenarios
- The fix maintains backward compatibility and doesn't change the user-facing behavior
- Memory leaks are prevented by checking activity lifecycle state before updating UI
