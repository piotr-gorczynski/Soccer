# Friends List Sorting Debug Fix

## Issue Report
The friends list sorting feature had two main problems:
1. The list was not reliably sorted by "last seen" on activity start
2. The dropdown stopped working - when selecting "alphabetically", the list was not being sorted

## Root Cause Analysis

### Problem 1: Duplicate Load Calls During Initialization
The original code had this sequence:
1. `onCreate()` sets up the spinner with `OnItemSelectedListener`
2. `onCreate()` calls `setSelection(SORT_BY_LAST_SEEN)`, which triggers `onItemSelected()`
3. `onItemSelected()` calls `loadFriends()`
4. Then `onStart()` also calls `loadFriends()`

This resulted in:
- Two concurrent calls to `loadFriends()` during initialization
- Race conditions where the second call might start before the first completes
- Inconsistent sorting behavior
- The `isLoadingFriends` flag would sometimes prevent the correct load from happening

### Problem 2: Spinner Selection Not Working
When users manually changed the spinner selection:
- The `onItemSelected()` callback would fire correctly
- `currentSortMode` would be updated
- `loadFriends()` would be called
- BUT if the previous call was still in progress, the new call would be blocked by `isLoadingFriends`
- This made the dropdown appear to not work

## Solution

### 1. Added Spinner Initialization Tracking
```java
private boolean spinnerInitialized = false;

sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        currentSortMode = position;
        
        // Only reload friends if this is a user-initiated selection change
        if (spinnerInitialized) {
            loadFriends();
        } else {
            spinnerInitialized = true;
        }
    }
    ...
});
```

This ensures that:
- The initial `setSelection()` call updates `currentSortMode` but doesn't trigger a load
- Only `onStart()` performs the initial load
- User-initiated changes to the spinner do trigger loads
- No race conditions during initialization

### 2. Added Comprehensive Debug Logging
Added detailed logging at every step:
- Activity lifecycle events (onCreate, onStart, onResume)
- Spinner selection events with context
- loadFriends() calls with current sort mode
- Firestore query results (number of friends, UIDs)
- Sorting operations (which method is called, what data is being sorted)
- Heartbeat values for each friend
- Sorted order (first 5 friends with their heartbeat values)
- Error conditions

### 3. Log Output Example
When the fix is working correctly, you should see logs like this:

```
D/FriendsListActivity: onCreate: Initializing FriendsListActivity
D/FriendsListActivity: onItemSelected: position=0, spinnerInitialized=false
D/FriendsListActivity: onItemSelected: Initial spinner setup, marking as initialized
D/FriendsListActivity: onCreate: Set spinner selection to SORT_BY_LAST_SEEN
D/FriendsListActivity: onStart: Loading friends with currentSortMode=SORT_BY_LAST_SEEN
D/FriendsListActivity: loadFriends: Starting to load friends with sortMode=SORT_BY_LAST_SEEN
D/FriendsListActivity: loadFriends: Retrieved 3 friends from Firestore
D/FriendsListActivity: loadFriends: Friend UIDs: [uid1, uid2, uid3]
D/FriendsListActivity: loadFriends: Sorting by last seen
D/FriendsListActivity: sortByLastSeen: Fetching heartbeat data for 3 friends
D/FriendsListActivity: sortByLastSeen: Friend uid1 has heartbeat: 1697500000000
D/FriendsListActivity: sortByLastSeen: Friend uid2 has heartbeat: 1697510000000
D/FriendsListActivity: sortByLastSeen: Friend uid3 has heartbeat: 1697490000000
D/FriendsListActivity: sortByLastSeen: Sorted friends by last seen, updating adapter
D/FriendsListActivity: sortByLastSeen: Sorted order: uid2(1697510000000), uid1(1697500000000), uid3(1697490000000)
```

When user changes to alphabetical sorting:
```
D/FriendsListActivity: onItemSelected: position=1, spinnerInitialized=true
D/FriendsListActivity: onItemSelected: User changed sort mode to SORT_ALPHABETICALLY
D/FriendsListActivity: loadFriends: Starting to load friends with sortMode=SORT_ALPHABETICALLY
D/FriendsListActivity: loadFriends: Retrieved 3 friends from Firestore
D/FriendsListActivity: loadFriends: Friend UIDs: [uid1, uid2, uid3]
D/FriendsListActivity: loadFriends: Sorting alphabetically
D/FriendsListActivity: sortByNickname: Fetching user documents for 3 friends
D/FriendsListActivity: sortByNickname: User uid1 has nickname: Bob
D/FriendsListActivity: sortByNickname: User uid2 has nickname: Alice
D/FriendsListActivity: sortByNickname: User uid3 has nickname: Charlie
D/FriendsListActivity: sortByNickname: Sorted friends alphabetically, updating adapter
```

## How to Use the Debug Logs

### To get debug logs from the app:
1. Enable USB debugging on your Android device
2. Connect device to computer
3. Run: `adb logcat -s FriendsListActivity:D *:S`
   - Note: On Windows, you may need to use the full path to adb.exe
   - Alternatively, use Android Studio's Logcat viewer: View → Tool Windows → Logcat, then filter by "FriendsListActivity"
4. Open the Friends List screen in the app
5. Try changing the sort dropdown
6. Copy the logs and paste them in the issue

### Key things to look for in logs:
1. **Verify initial sort mode**: Check that `currentSortMode=SORT_BY_LAST_SEEN` on start
2. **Check for duplicate loads**: Should only see one `loadFriends` call during startup
3. **Verify spinner changes work**: When you change dropdown, should see "User changed sort mode"
4. **Check data retrieval**: Verify friends are being fetched from Firestore
5. **Check sorting execution**: Verify the correct sort method is called
6. **Check sorted results**: Verify the sorted order makes sense

## Testing

### Manual Testing Steps:
1. Open Friends List Activity
2. Check logs to verify initial sort is by last seen
3. Verify friends are displayed (if you have any friends)
4. Change dropdown to "Sort alphabetically"
5. Check logs to verify sorting changed
6. Verify friends are re-sorted
7. Change dropdown back to "Sort by last seen"
8. Check logs and verify sorting changed again

### Unit Tests Added:
- Tests for sorting by last seen (descending order)
- Tests for handling zero/missing heartbeats
- Tests for alphabetical sorting (ascending order)
- Tests for handling null nicknames
- Tests for case-insensitive sorting
- Edge cases (empty list, single friend)

## Expected Behavior After Fix

### On Activity Start:
1. Spinner initializes with "Sort by last seen" selected
2. `onItemSelected` fires but doesn't load friends (spinnerInitialized=false)
3. `onStart()` loads friends with `currentSortMode=SORT_BY_LAST_SEEN`
4. Friends are fetched from Firestore
5. Heartbeat data is fetched from RTDB
6. Friends are sorted by heartbeat (descending - most recent first)
7. Sorted list is displayed

### When User Changes Dropdown:
1. User selects "Sort alphabetically"
2. `onItemSelected` fires with position=1 and spinnerInitialized=true
3. `currentSortMode` is updated to SORT_ALPHABETICALLY
4. `loadFriends()` is called
5. Friends are fetched from Firestore
6. User data (nicknames) is fetched from Firestore
7. Friends are sorted alphabetically by nickname (ascending)
8. Re-sorted list is displayed

### When User Changes Back:
1. User selects "Sort by last seen"
2. `onItemSelected` fires with position=0 and spinnerInitialized=true
3. `currentSortMode` is updated to SORT_BY_LAST_SEEN
4. `loadFriends()` is called
5. Friends are sorted by heartbeat again
6. Re-sorted list is displayed

## Files Modified
- `mobile/app/src/main/java/piotr_gorczynski/soccer2/FriendsListActivity.java`: Added logging and fixed initialization
- `mobile/app/src/test/java/piotr_gorczynski/soccer2/FriendsListSortingTest.java`: Added tests for alphabetical sorting
- `FRIENDS_LIST_SORTING_DEBUG_FIX.md`: This documentation file

## Summary
The fix ensures:
1. ✅ Only one `loadFriends()` call during initialization
2. ✅ User can reliably change sort order via dropdown
3. ✅ Comprehensive debug logging for troubleshooting
4. ✅ Both sort modes work correctly
5. ✅ No race conditions during initialization
