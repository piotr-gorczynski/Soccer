# Test Documentation for cleanup-inactive-users Function

## Purpose
Scheduled function that runs every 24 hours to delete users who:
- Have been inactive (no sign-in) for more than 1 month
- Have not accepted terms (termsAccepted field missing or not true)

## Cleanup Actions
When a user meets deletion criteria, the function:
1. Deletes from Firebase Authentication
2. Deletes from Firestore `users` collection
3. Deletes from Realtime Database `status` collection
4. Removes user ID from all other users' `friends` subcollections

## Test Cases to Validate

| User Profile | Last Sign In | Terms Accepted | Expected Result | Notes |
|--------------|-------------|----------------|----------------|--------|
| Active user | 1 week ago | true | KEEP | Recent activity |
| Active user | 1 week ago | false | KEEP | Recent activity overrides terms |
| Inactive user | 2 months ago | true | KEEP | Terms accepted |
| Inactive user | 2 months ago | false | DELETE | Meets both criteria |
| Inactive user | 2 months ago | missing | DELETE | No terms field |
| New user | Never signed in, 2 months old | false | DELETE | Uses creation time |
| Orphaned Auth | 2 months ago | N/A (no Firestore doc) | DELETE | Auth-only account |

## Edge Cases Handled

### Partial Failures
- If Auth deletion fails: Stop processing (prevent orphaned data)
- If Firestore deletion fails: Continue (user already removed from Auth)
- If RTDB deletion fails: Continue with friends cleanup
- If friends cleanup fails: Log error but don't fail entire operation

### Performance Considerations
- Processes users in batches of 1000 (Firebase Auth API limit)
- Uses Firestore batch operations for friends cleanup
- Comprehensive logging for audit trail

## Logging Output
The function logs:
- Each user deletion with email, UID, last sign-in time, and terms status
- Summary count of deleted users
- Detailed list of all deleted users at the end
- Error details for any failed operations

## Safety Features
- Only deletes users meeting BOTH criteria (inactive AND no terms)
- Preserves users with accepted terms regardless of activity
- Preserves recently active users regardless of terms status
- Graceful error handling prevents partial deletions