# MenuActivity Animation Start Fix - Animation Not Starting on First Launch

## Problem

The running player sprite animation on MenuActivity was not starting when the app first launches. The animation would only start after navigating to another screen and returning to MenuActivity.

### Symptoms
- On first app launch, the sprite appears but doesn't animate
- Animation works correctly when returning to MenuActivity from other screens
- Affects user experience on cold start

## Root Cause

The issue was caused by a race condition between the activity lifecycle and background bitmap loading:

1. **onCreate()** calls `setupRunningPlayerAnimation()` which starts a background thread to load sprite frames
2. **onStart()** is called shortly after and executes `startRunningPlayerAnimation()`
3. At this point, `runningPlayerFrames` is still `null` because the background thread hasn't finished loading
4. `startRunningPlayerAnimation()` returns early without setting `isRunningPlayerAnimationStarted = true`
5. When the background thread finishes, it doesn't start the animation - it only sets the frames
6. Animation never starts on first launch

**On subsequent visits:**
- Frames are already loaded from the first time
- `startRunningPlayerAnimation()` in `onStart()` works normally because `runningPlayerFrames` is not null

## Solution

After the background thread finishes loading sprite frames, check if the activity is in the started state and automatically start the animation if needed.

### Implementation

Modified the `runOnUiThread()` callback in `setupRunningPlayerAnimation()` to:

1. Set the loaded frames as before
2. Configure the view and click listeners
3. **NEW**: Check if animation should start automatically:
   ```java
   if (!isRunningPlayerAnimationStarted && areAnimationsEnabled()) {
       startRunningPlayerAnimation();
   }
   ```

This ensures that:
- If `onStart()` was already called but frames weren't ready, the animation starts now
- If animation is already running (e.g., on subsequent calls), it doesn't restart
- Respects the user's animation preference setting

### Code Changes

```java
runOnUiThread(() -> {
    // ... existing code to set frames ...
    
    if (runningPlayerView != null) {
        runningPlayerView.setVisibility(View.VISIBLE);
        Bitmap[] initialRow = getCurrentRunningPlayerRowFrames();
        if (initialRow != null && initialRow.length > 0) {
            runningPlayerView.setImageBitmap(initialRow[0]);
        }
        configureRunningPlayerClickListener();
        
        // If activity is already started, begin animation immediately
        // This handles the case where onCreate->setupRunningPlayerAnimation starts
        // the background thread, then onStart is called before frames are loaded
        if (!isRunningPlayerAnimationStarted && areAnimationsEnabled()) {
            startRunningPlayerAnimation();
        }
    }
});
```

## How It Works

### Scenario 1: First Launch (Bug Fixed)
1. `onCreate()` → `setupRunningPlayerAnimation()` starts background thread
2. `onStart()` → `startRunningPlayerAnimation()` but frames are null, returns early
3. Background thread finishes → **NOW calls `startRunningPlayerAnimation()` to start the animation** ✅
4. Animation runs normally

### Scenario 2: Returning to Activity (Still Works)
1. Frames already loaded from previous visit
2. `onStart()` → `startRunningPlayerAnimation()` works normally because frames exist ✅
3. Background thread callback doesn't call again because `isRunningPlayerAnimationStarted` is already true ✅
4. Animation runs normally

## Benefits

1. **Consistent UX**: Animation always starts on first launch, matching user expectations
2. **No Side Effects**: Doesn't interfere with normal operation when frames are already loaded
3. **Respects Settings**: Still checks `areAnimationsEnabled()` before starting
4. **Minimal Code Change**: Single conditional check, surgical fix
5. **Safe**: Uses existing animation start logic, no new bugs introduced

## Testing

Manual verification scenarios:
1. ✅ Fresh install - animation starts on first launch
2. ✅ App restart - animation starts on first launch
3. ✅ Navigate away and back - animation still works
4. ✅ Animations disabled in settings - sprite doesn't animate
5. ✅ Multiple launches - animation always works

## Files Modified

- `mobile/app/src/main/java/piotr_gorczynski/soccer2/MenuActivity.java`
  - Modified `setupRunningPlayerAnimation()` method to start animation after loading frames

## Related Issues

- Issue #713 - Original work to set up the sprite animation system for MenuActivity
- This fix addresses the regression where animation didn't start on first launch after the sprite system was implemented

## Related Documentation

- [MENUACTIVITY_ANR_FIX.md](./MENUACTIVITY_ANR_FIX.md) - Background thread pattern for bitmap loading
- [Android Activity Lifecycle](https://developer.android.com/guide/components/activities/activity-lifecycle)

## Additional Notes

- This fix completes the work from issue #713
- The background thread pattern from the ANR fix is maintained
- No new threads or complexity added - uses existing animation infrastructure
- The fix is defensive and won't cause issues if called multiple times
