# Missed Invitation Notification Feature

## Overview

This feature notifies users when they receive game invitations while they were offline. When a user returns to the app after being away, the system checks if any new invitations were received during their absence and displays a notification dialog.

## Implementation Details

### Components Modified

1. **MenuActivity.java**
   - Added `PREF_LAST_ACTIVE_TIMESTAMP` constant to track when user was last active
   - Added `checkForMissedInvitations()` method to query for new invitations
   - Added `showMissedInviteDialog()` method to display the notification dialog
   - Added `updateLastActiveTimestamp()` method to update the timestamp
   - Modified `continueWithInviteRestore()` to call `checkForMissedInvitations()` after checking for outgoing invites

2. **String Resources**
   - `missed_invite_title`: Dialog title "You Missed an Invitation"
   - `missed_invite_message`: Dialog message "You received an invitation while you were offline."
   - `see_invites`: Positive button text "See Invites"
   - All strings translated to 19 supported languages

### How It Works

1. **Timestamp Tracking**: Each time the user activates the app (MenuActivity resumes), the system stores the current timestamp in SharedPreferences.

2. **Invitation Check**: When the app checks for active matches and pending outgoing invites, it also queries Firestore for:
   - Invitations sent TO the current user
   - Status = "pending"
   - Created after the last active timestamp
   - Not yet expired

3. **Notification Dialog**: If a new invitation is found:
   - A dialog pops up with the title and message
   - Two buttons are provided:
     - "See Invites": Opens the InvitationsActivity
     - "Close": Dismisses the dialog

4. **Backend Availability Check**: The feature respects the existing backend availability safeguards:
   - If backend is unavailable, the check is skipped
   - This prevents errors when the system is offline

### User Flow

```
User opens app
    ↓
MenuActivity.onResume()
    ↓
Check backend availability
    ↓
Check for active matches
    ↓
Check for outgoing pending invites (continueWithInviteRestore)
    ↓
Check for missed invitations (NEW)
    ↓
If new invitation found → Show dialog
    ↓
User clicks "See Invites" → Open InvitationsActivity
OR
User clicks "Close" → Dialog dismissed
```

### Edge Cases Handled

1. **First Run**: On first app launch, no notification is shown (timestamp = 0)
2. **Backend Offline**: Check is skipped if backend is unavailable
3. **Expired Invitations**: Only valid (not expired) invitations trigger the notification
4. **User Not Logged In**: Check is skipped if user is not authenticated
5. **Multiple Invitations**: Only one notification is shown even if multiple invitations arrived

### Testing

A test class `MenuActivityMissedInviteTest.java` verifies:
- Required string resources exist in all languages
- Required classes (MenuActivity, InvitationsActivity) exist
- String resources are not empty

### Future Enhancements

Potential improvements could include:
- Show the number of missed invitations in the message
- Option to go directly to a specific invitation
- Sound or vibration when notification is shown
- Notification history to track all missed invitations
