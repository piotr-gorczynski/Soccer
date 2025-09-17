# Test Documentation for cleanup-inactive-users Function

## Purpose
Scheduled function that runs every 24 hours to delete users who:
- Have been inactive (no sign-in) for more than 1 month
- Have not accepted terms (termsAccepted field missing or not true)
- Have no active involvement in the system (see safeguards below)

## Safeguards
Before deletion, the function checks that the user has NO active involvement in:
1. **Invitations**: No pending invitations sent by the user
2. **Matches**: Not involved as player0 or player1 in any matches
3. **Tournament Matches**: Not involved as player0 or player1 in tournament matches
4. **Tournament Participation**: Not a participant in any tournaments

If ANY of these conditions are found, the user is preserved and the reason is logged.

## Cleanup Actions
When a user meets deletion criteria AND passes all safeguards, the function:
1. Deletes from Firebase Authentication
2. Deletes from Firestore `users` collection
3. Deletes from Realtime Database `status` collection
4. Removes user ID from all other users' `friends` subcollections

## Test Cases to Validate

| User Profile | Last Sign In | Terms Accepted | Active Involvement | Expected Result | Notes |
|--------------|-------------|----------------|-------------------|----------------|--------|
| Active user | 1 week ago | true | None | KEEP | Recent activity |
| Active user | 1 week ago | false | None | KEEP | Recent activity overrides terms |
| Inactive user | 2 months ago | true | None | KEEP | Terms accepted |
| Inactive user | 2 months ago | false | None | DELETE | Meets all criteria |
| Inactive user | 2 months ago | false | Has pending invites | KEEP | Safeguard: active invitations |
| Inactive user | 2 months ago | false | In matches | KEEP | Safeguard: active matches |
| Inactive user | 2 months ago | false | In tournament matches | KEEP | Safeguard: tournament matches |
| Inactive user | 2 months ago | false | Tournament participant | KEEP | Safeguard: tournament participation |
| Inactive user | 2 months ago | missing | None | DELETE | No terms field |
| New user | Never signed in, 2 months old | false | None | DELETE | Uses creation time |
| Orphaned Auth | 2 months ago | N/A (no Firestore doc) | None | DELETE | Auth-only account |

## Edge Cases Handled

### Safeguard Checks
- Uses proper Firestore queries to check all relevant collections
- Uses collectionGroup queries for tournament matches and participants
- Handles query errors gracefully (preserves user on error)
- Logs specific reasons when users cannot be deleted

### Partial Failures
- If Auth deletion fails: Stop processing (prevent orphaned data)
- If Firestore deletion fails: Continue (user already removed from Auth)
- If RTDB deletion fails: Continue with friends cleanup
- If friends cleanup fails: Log error but don't fail entire operation

### Performance Considerations
- Processes users in batches of 1000 (Firebase Auth API limit)
- Uses parallel queries where possible (Promise.all)
- Uses Firestore batch operations for friends cleanup
- Comprehensive logging for audit trail

## Logging Output
The function logs:
- Safeguard check results for each user
- Specific reasons when users cannot be deleted (invitations, matches, tournaments)
- Each user deletion with email, UID, last sign-in time, and terms status
- Summary count of deleted users
- Detailed list of all deleted users at the end
- Error details for any failed operations

## Safety Features
- Only deletes users meeting ALL criteria (inactive AND no terms AND no active involvement)
- Preserves users with accepted terms regardless of activity
- Preserves recently active users regardless of terms status
- Preserves users with any active system involvement
- Graceful error handling prevents partial deletions
- Comprehensive safeguard logging for audit compliance