# Android Level Analysis

## Issue Summary

This document analyzes how the Android level setting (Easy=0, Medium=3, Hard=10 seconds) impacts the MINMAX algorithm in the GameView.java `androidNextMove_v2` function.

## Current Implementation

### Android Level Values
The android level is configured in `mobile/app/src/main/res/xml/pref_android_level.xml` with three options:
- **Easy**: 0 seconds
- **Medium**: 3 seconds  
- **Hard**: 10 seconds

These values are defined in `mobile/app/src/main/res/values/strings.xml`:
```xml
<string-array name="pref_level_values">
    <item>0</item>
    <item>3</item>
    <item>10</item>
</string-array>
```

### How androidLevel is Used

The `androidLevel` parameter is used in the `androidNextMove_v2` function as a **time-based early termination condition**:

```java
// Line 608-612 in GameView.java
difference = (System.currentTimeMillis() - startThinkingTime)/1000;
if((nextMoveFound.found) && (difference>androidLevel)) {
    Log.d("TAG_Soccer", getClass().getSimpleName() + ".androidNextMove_v2: <timeLimitReached>" + difference + "</timeLimitReached>");
    break;
}
```

### Key Observations

1. **The androidLevel does NOT directly control tree depth or bouncing levels**: The hardcoded values in the algorithm are:
   - `gameTreeDepthLevel = 1` (line 629)
   - `gameBouncingLevel = 50` (line 602)

2. **Time-based termination**: The algorithm uses `androidLevel` as a maximum thinking time in seconds. Once a valid move is found (`nextMoveFound.found == true`) AND the elapsed time exceeds `androidLevel`, the search terminates early.

3. **Recursive search**: The `androidNextMove_v2` function is recursive and explores possible moves using the MINMAX algorithm.

## Answer to the Question: Can the Time Limit Condition Be Reached?

**YES, the condition CAN be reached**, but it depends on several factors:

### When the Condition WILL Be Reached:

1. **Complex game positions**: When there are many possible moves to evaluate
2. **Deep recursion**: When the algorithm needs to explore many levels deep
3. **Higher bouncing levels**: Positions with multiple bouncing moves increase computation time
4. **Lower androidLevel settings**: Easy (0 seconds) is much more likely to trigger the timeout than Hard (10 seconds)

### When the Condition WILL NOT Be Reached:

1. **Simple positions**: Few possible moves or early game states
2. **Fast hardware**: Modern devices may complete the search quickly
3. **Terminal positions**: When the algorithm finds a winning/losing move early
4. **Hard difficulty**: 10 seconds is a very generous time limit

### Evidence the Condition is Functional:

The algorithm includes logging that would show when the time limit is reached:
```java
Log.d("TAG_Soccer", getClass().getSimpleName() + ".androidNextMove_v2: <timeLimitReached>" + difference + "</timeLimitReached>");
```

The function also has a bouncing level limit:
```java
if((nextMoveFound.found) && (nextMoveFound.bouncingLevel> gameBouncingLevel)) {
    Log.d("TAG_Soccer", getClass().getSimpleName() + ".androidNextMove_v2: <gameBouncingLevelReached>" + gameBouncingLevel + "</gameBouncingLevelReached>");
    break;
}
```

## Critical Bug Found and Fixed: Preference Storage Mismatch

### The Bug

There WAS a **critical bug** preventing the androidLevel from being read correctly:

1. **SettingsFragment** saves preferences using `PreferenceManager.getDefaultSharedPreferences()`:
   ```java
   // Line 31 in SettingsFragment.java
   prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
   ```
   This stores preferences in the default Android preferences file (typically `<package_name>_preferences.xml`).

2. **GameActivity** WAS reading preferences from a different file:
   ```java
   // OLD CODE - Line 496-497 in GameActivity.java
   SharedPreferences sharedPreferences =
       getSharedPreferences(LanguageManager.PREFS_FILE, Context.MODE_PRIVATE);
   ```
   Where `LanguageManager.PREFS_FILE = "app_prefs"`.

### The Fix

Changed GameActivity to use the same PreferenceManager as SettingsFragment:
```java
// NEW CODE
SharedPreferences sharedPreferences =
    PreferenceManager.getDefaultSharedPreferences(this);
```

### Impact Before Fix

**The android level setting was NEVER read correctly by GameActivity**. The app always used the default value (0 seconds) or a stale value from the wrong preferences file. This meant:

- Changing the difficulty level in settings had NO EFFECT on the actual game AI behavior
- The algorithm was timing out after 0 seconds on Easy difficulty instead of respecting the user's choice
- Users selecting "Hard" (10 seconds) didn't actually get the intended behavior

### Impact After Fix

Now the android level setting will be correctly read from the user's preference selection, allowing:
- Easy (0 seconds): Very quick AI moves with minimal thinking time for easier first-time user experience
- Medium (3 seconds): Moderate AI analysis for balanced gameplay
- Hard (10 seconds): Extensive AI search for the best possible moves

## How the Algorithm Actually Works

Despite the androidLevel not being read correctly due to the bug, here's how the algorithm is INTENDED to work:

1. **Start thinking**: `startThinkingTime = System.currentTimeMillis()`
2. **Iterate through possible moves**: Evaluate each move using MINMAX
3. **Check conditions**: After finding at least one valid move:
   - If time elapsed > androidLevel: Stop searching and use best move found
   - If bouncing level > 50: Stop searching
   - If victory found: Stop immediately
4. **Recursive exploration**: For bouncing moves or opponent moves, recurse deeper
5. **Return best move**: Select the move with the minimum MINMAX evaluation

## Recommendations

1. ~~**Fix the preference storage bug**: Make SettingsFragment and GameActivity use the same SharedPreferences file~~ **FIXED** ✓
2. **Make tree depth configurable**: Consider making `gameTreeDepthLevel` and `gameBouncingLevel` configurable based on androidLevel for true difficulty control
3. **Add performance logging**: Log the actual time taken for AI moves to verify the timeout mechanism works
4. **Consider iterative deepening**: Instead of fixed depth, use available time budget to search progressively deeper
5. **Test the timeout**: Add test cases that verify the timeout condition can be triggered

## Conclusion

The `androidLevel` parameter serves as a **time budget** for AI thinking, not a direct control of search depth. The condition `difference > androidLevel` CAN be reached in complex positions, making the algorithm behave differently based on difficulty:

- **Easy (0s)**: Very quick, likely suboptimal moves for easier user experience
- **Medium (3s)**: Better moves with moderate lookahead
- **Hard (10s)**: Best possible moves with extensive search

The **preference storage bug has been fixed**, and now these settings will be correctly applied. The algorithm will respect the user's selected difficulty level and use the appropriate time budget for AI thinking.
