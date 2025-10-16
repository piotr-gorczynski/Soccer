# Friends List Sorting Fix

## Problem
When FriendsListActivity is opened, the friends list was not consistently sorted by last seen (most recent first) by default. The list would appear in an arbitrary order (Firestore's default order) instead of being sorted by heartbeat timestamp.

## Root Cause
The issue was caused by unreliable spinner initialization behavior in Android. The code had:

```java
// Old code
sortSpinner.setSelection(SORT_BY_LAST_SEEN);  // Set selection first
sortSpinner.setOnItemSelectedListener(...);    // Then attach listener
```

When `setSelection()` is called BEFORE attaching the `OnItemSelectedListener`, the listener callback is NOT triggered reliably. This is because the listener wasn't attached yet when the selection was set.

According to Android behavior:
- If you call `setSelection()` BEFORE `setOnItemSelectedListener()`, the callback is NOT triggered
- If you call `setSelection()` AFTER `setOnItemSelectedListener()`, the callback IS triggered (either immediately or during next layout)

## Solution

### 1. Fixed Spinner Initialization Order
Moved the `setSelection()` call to AFTER attaching the listener:

```java
// New code
sortSpinner.setOnItemSelectedListener(...);    // Attach listener first
sortSpinner.setSelection(SORT_BY_LAST_SEEN);  // Then set selection
```

This ensures that `onItemSelected()` is triggered with position 0 (SORT_BY_LAST_SEEN), which:
- Sets `currentSortMode = 0`
- Calls `loadFriends()` which then calls `sortByLastSeen()`

### 2. Added Concurrent Load Protection
Added a `isLoadingFriends` flag to prevent race conditions when `loadFriends()` is called multiple times:

```java
private boolean isLoadingFriends = false;

private void loadFriends() {
    if (isLoadingFriends) {
        return;  // Prevent concurrent loads
    }
    isLoadingFriends = true;
    // ... fetch and sort logic ...
}
```

This prevents issues when:
- The spinner's `onItemSelected()` callback fires during `onCreate()`
- Then `onStart()` also calls `loadFriends()`

## Expected Behavior After Fix

1. **On Activity Start:**
   - Spinner is initialized with listener attached
   - Selection is set to "Sort by last seen" (position 0)
   - `onItemSelected(0)` is triggered automatically
   - Friends are fetched from Firestore
   - Friends are sorted by heartbeat timestamp (most recent first)
   - Sorted list is displayed to user

2. **Sorting Logic:**
   - Friends with higher heartbeat timestamps appear first (most recently seen)
   - Friends with no heartbeat data (0) appear at the end
   - When heartbeats are equal, friends are sorted by UID for stable ordering

3. **User Interaction:**
   - User can still manually change sorting via the spinner
   - Changing to "Sort alphabetically" sorts by nickname
   - Changing back to "Sort by last seen" re-sorts by heartbeat

## Testing

Created `FriendsListSortingTest.java` with test cases covering:
- Sorting by last seen with different heartbeat values (descending order)
- Handling friends with zero/missing heartbeat data
- Edge cases (empty list, single friend)
- Stable sorting when heartbeats are equal (fallback to UID)

## Files Changed

1. **FriendsListActivity.java**
   - Moved `setSelection()` after `setOnItemSelectedListener()`
   - Added `isLoadingFriends` flag and guards
   - Added comments explaining the initialization order

2. **FriendsListSortingTest.java** (new)
   - Unit tests for sorting logic
   - Validates descending order by heartbeat
   - Tests edge cases and fallback behavior
