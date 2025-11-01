# Fix for Alphabetical Sorting Issue with >30 Friends

## Problem
When users attempted to sort their friends list alphabetically and had more than 30 friends, the app would crash with the following error:

```
com.google.firebase.firestore.FirebaseFirestoreException: INVALID_ARGUMENT: 'IN' supports up to 30 comparison values.
```

This occurred because Firestore's `whereIn()` query has a limitation of 30 items maximum in the comparison array.

## Root Cause
The `sortByNickname()` method in `FriendsListActivity.java` was using a single Firestore query:

```java
db.collection("users").whereIn(FieldPath.documentId(), friendUids)
```

When `friendUids` contained more than 30 items, the query would fail immediately.

## Solution
The fix implements batching to split the friend UIDs into chunks of 30 items:

1. **Batch Processing**: Friend UIDs are divided into batches of 30
2. **Parallel Queries**: Each batch is queried independently 
3. **Result Aggregation**: Results from all batches are collected into a single nickname map
4. **Completion Tracking**: A counter tracks when all batches complete
5. **Unified Sorting**: Once all batches complete, sorting is performed on the complete dataset

### Key Changes

#### Modified `sortByNickname()` Method
- Added `BATCH_SIZE` constant (30 items)
- Implemented loop to process friend UIDs in batches
- Added batch completion tracking
- Enhanced logging for debugging batch operations

#### New `performSortByNickname()` Method
- Extracted sorting logic into a separate method
- Called once all batches are complete
- Maintains same sorting behavior as before

### Code Structure
```java
private void sortByNickname(List<DocumentSnapshot> docs, List<String> friendUids) {
    final int BATCH_SIZE = 30;
    final Map<String, String> nicknameMap = new HashMap<>();
    final int totalBatches = (int) Math.ceil((double) friendUids.size() / BATCH_SIZE);
    final int[] completedBatches = {0};
    
    // Process in batches of 30
    for (int i = 0; i < friendUids.size(); i += BATCH_SIZE) {
        List<String> batch = friendUids.subList(i, Math.min(i + BATCH_SIZE, friendUids.size()));
        
        db.collection("users").whereIn(FieldPath.documentId(), batch)
            .get()
            .addOnSuccessListener(userSnap -> {
                // Add nicknames to map
                completedBatches[0]++;
                
                if (completedBatches[0] == totalBatches) {
                    performSortByNickname(mutableDocs, nicknameMap);
                }
            });
    }
}

private void performSortByNickname(List<DocumentSnapshot> mutableDocs, Map<String, String> nicknameMap) {
    // Sort and update UI
}
```

## Testing

### Unit Tests Added
The fix includes comprehensive unit tests for batching logic:

1. **testBatchingLogicFor31Friends**: Verifies 31 friends create 2 batches (30 + 1)
2. **testBatchingLogicFor60Friends**: Verifies 60 friends create 2 batches (30 + 30)
3. **testBatchingLogicFor61Friends**: Verifies 61 friends create 3 batches (30 + 30 + 1)
4. **testBatchingLogicFor30Friends**: Edge case - exactly 30 friends create 1 batch
5. **testAlphabeticalSortingWithMultipleBatches**: Verifies sorting correctness with 35 friends

### Edge Cases Handled
- Exactly 30 friends (1 batch)
- 31 friends (2 batches)
- 60 friends (2 full batches)
- 61+ friends (3+ batches)
- Batch failures (graceful degradation)

## Impact

### Fixed
- ✅ Alphabetical sorting now works with any number of friends
- ✅ No more crashes when sorting >30 friends
- ✅ Maintains existing sorting behavior

### Maintained
- ✅ Case-insensitive alphabetical sorting
- ✅ Null nickname handling (placed at end)
- ✅ Error handling for failed queries
- ✅ Debug logging

### Performance
- Multiple small queries instead of one large query
- Negligible performance impact for <30 friends (1 query, same as before)
- Slightly longer for >30 friends due to multiple queries, but still acceptable
- Queries run asynchronously, so UI remains responsive

## Compatibility
- No breaking changes
- Compatible with existing data structure
- No migration required
- Backward compatible with older app versions

## Deployment Notes
- No database schema changes required
- No special deployment steps needed
- Safe to deploy without data migration
