# Friends List Sorting Permission Fix

## Problem

The FriendsListActivity was failing to sort friends by "last seen" with a permission denied error when trying to read heartbeat data from Firebase Realtime Database.

### Error Log
```
sortByLastSeen: Failed to fetch heartbeat data from RTDB
java.lang.Exception: Permission denied
```

## Root Cause

The code was attempting to read the entire `/status` node from the Firebase Realtime Database:

```java
DatabaseReference statusRef = FirebaseDatabase.getInstance().getReference("status");
statusRef.get()
```

However, the database security rules in `gcp/cloud-build/database.rules.json` only permit reading individual user status nodes:

```json
"status": {
  "$uid": {
    ".read": "auth != null",
    ".write": "auth != null && auth.uid == $uid"
  }
}
```

This means authenticated users can read `/status/{specific_uid}` but not the entire `/status` node.

## Solution

Modified the `sortByLastSeen()` method in `FriendsListActivity.java` to:

1. **Read each friend's status individually** instead of reading the entire `/status` node
2. **Aggregate the results asynchronously** by tracking completed fetches
3. **Sort and update the adapter** only after all individual status reads complete (or fail)

### Key Changes

**Before:**
- Single database read of entire `/status` node
- Synchronous processing of all friends' data

**After:**
- Individual database reads for each friend at `/status/{friendUid}`
- Asynchronous aggregation with completion tracking
- Graceful handling of individual read failures

### Code Structure

The fix introduces two methods:

1. `sortByLastSeen()` - Initiates individual status reads for each friend and tracks completion
2. `performSortByLastSeen()` - Performs the actual sorting and adapter update once all data is collected

### Error Handling

- If an individual friend's status read fails, uses `0L` as the default heartbeat value
- Falls back to cached heartbeat values from the adapter when available
- Continues sorting even if some friend status reads fail

## Testing

- Code compiles successfully with `./gradlew :app:compile_devDebugJavaWithJavac`
- The fix respects the existing database security rules
- No changes to database rules were necessary

## Impact

- Friends list sorting by "last seen" will now work correctly
- Performance: Multiple individual reads instead of one bulk read (acceptable tradeoff for proper security)
- Maintains backward compatibility with existing data structures and caching logic
