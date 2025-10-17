# How to Test the Friends List Sorting Fix

## What Was Fixed

This fix addresses two issues you reported:
1. ✅ Friends list not reliably sorted by "last seen" on activity start
2. ✅ Dropdown not working when selecting "Sort alphabetically"

## What Changed

### The Problem
The app was calling `loadFriends()` twice during startup, causing race conditions. When you tried to change the sort order, the new request would be blocked by the previous one still running.

### The Fix
- Added logic to prevent duplicate loads during startup
- Only the initial screen load triggers friend fetching
- User changes to the dropdown now properly trigger re-sorting
- Added extensive debug logging to help diagnose any remaining issues

## How to Test

### 1. Basic Functionality Test
1. Open the app and navigate to Friends List
2. The friends should be sorted by "last seen" (most recent first) by default
3. Change the dropdown to "Sort alphabetically"
4. Friends should re-sort by nickname (A-Z)
5. Change back to "Sort by last seen"
6. Friends should re-sort by last seen time

### 2. Debug Logging Test
To help us diagnose any remaining issues, please collect debug logs:

#### Option A: Using ADB (Command Line)
```bash
# Connect your device via USB, enable USB debugging
adb logcat -s FriendsListActivity:D *:S
```

#### Option B: Using Android Studio
1. Open Android Studio
2. Go to View → Tool Windows → Logcat
3. In the filter box, type: `FriendsListActivity`
4. Open the Friends List screen in the app
5. Copy the logs

### 3. What to Look For in Logs

When you open Friends List, you should see:
```
D/FriendsListActivity: onCreate: Initializing FriendsListActivity
D/FriendsListActivity: onItemSelected: position=0, spinnerInitialized=false
D/FriendsListActivity: onItemSelected: Initial spinner setup, marking as initialized
D/FriendsListActivity: onCreate: Set spinner selection to SORT_BY_LAST_SEEN
D/FriendsListActivity: onStart: Loading friends with currentSortMode=SORT_BY_LAST_SEEN
D/FriendsListActivity: loadFriends: Starting to load friends with sortMode=SORT_BY_LAST_SEEN
D/FriendsListActivity: loadFriends: Retrieved X friends from Firestore
D/FriendsListActivity: loadFriends: Sorting by last seen
D/FriendsListActivity: sortByLastSeen: Friend uid1 has heartbeat: 1234567890000
...
D/FriendsListActivity: sortByLastSeen: Sorted order: uid2(1234567890000), uid1(1234567880000), ...
```

When you change to alphabetical:
```
D/FriendsListActivity: onItemSelected: position=1, spinnerInitialized=true
D/FriendsListActivity: onItemSelected: User changed sort mode to SORT_ALPHABETICALLY
D/FriendsListActivity: loadFriends: Starting to load friends with sortMode=SORT_ALPHABETICALLY
D/FriendsListActivity: loadFriends: Sorting alphabetically
D/FriendsListActivity: sortByNickname: User uid1 has nickname: Alice
D/FriendsListActivity: sortByNickname: User uid2 has nickname: Bob
...
D/FriendsListActivity: sortByNickname: Sorted friends alphabetically, updating adapter
```

## What to Report

### If It Works
Great! Please confirm:
- ✅ Friends list shows sorted by last seen on start
- ✅ Dropdown changes to alphabetical sorting work
- ✅ Dropdown changes back to last seen work
- ✅ No errors in the logs

### If Issues Remain
Please provide:
1. **Steps to reproduce** - exactly what you did
2. **Expected behavior** - what should have happened
3. **Actual behavior** - what actually happened
4. **Debug logs** - paste the full logs from one of the methods above
   - Make sure to include logs from:
     - Opening the screen
     - Changing the dropdown
     - Any error messages

## Technical Details

For more information about the fix, see:
- `FRIENDS_LIST_SORTING_DEBUG_FIX.md` - Complete technical documentation
- The code changes are in `mobile/app/src/main/java/piotr_gorczynski/soccer2/FriendsListActivity.java`

## Questions?

If you have any questions or encounter issues, please:
1. Include the debug logs (they're very helpful!)
2. Describe what you were trying to do
3. Mention if this is a new issue or if it's related to the original report

Thank you for testing!
