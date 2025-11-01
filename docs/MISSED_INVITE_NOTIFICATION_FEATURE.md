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
checkBackendAvailabilityAndContinue()
    ↓
continueOnResumeAfterBackendCheck()
    ↓
checkForActiveMatch()
    ├─ Active match found → Resume game
    └─ No active match found → continueWithInviteRestore()
           ↓
       Query for outgoing pending invites (from current user)
           ├─ Valid outgoing invite found → Resume WaitingActivity
           └─ No outgoing invites OR expired → checkForMissedInvitations() [NEW]
                  ↓
              Check if backend is available
                  ├─ Backend unavailable → Skip check, update timestamp
                  └─ Backend available → Query for incoming invites
                         ↓
                     Query Firestore for invitations:
                         - to: current user
                         - status: "pending"
                         - createdAt > lastActiveTimestamp
                         - orderBy: "createdAt"
                         - limit: 1
                         ↓
                     Found valid (not expired) invitation?
                         ├─ Yes → showMissedInviteDialog()
                         │           ↓
                         │       Show AlertDialog:
                         │           - Title: "You Missed an Invitation"
                         │           - Message: "You received an invitation..."
                         │           - Button 1: "See Invites" → Open InvitationsActivity
                         │           - Button 2: "Close" → Dismiss dialog
                         │
                         └─ No → Continue normally
                     
                     Update lastActiveTimestamp for next check
```

### Edge Cases Handled

1. **First Run**: On first app launch, no notification is shown (timestamp = 0)
2. **Backend Offline**: Check is skipped if backend is unavailable
3. **Expired Invitations**: Only valid (not expired) invitations trigger the notification
4. **User Not Logged In**: Check is skipped if user is not authenticated
5. **Multiple Invitations**: Only one notification is shown even if multiple invitations arrived

### Code Example

The key method `checkForMissedInvitations()` works as follows:

```java
private void checkForMissedInvitations() {
    // Skip check if backend is unavailable
    if (!isBackendAvailable) {
        updateLastActiveTimestamp();
        return;
    }

    String uid = FirebaseAuth.getInstance().getUid();
    if (uid == null) return;  // Not logged in

    SharedPreferences prefs = getSharedPreferences(LanguageManager.PREFS_FILE, MODE_PRIVATE);
    long lastActiveTimestamp = prefs.getLong(PREF_LAST_ACTIVE_TIMESTAMP, 0L);
    
    // Update timestamp for next check
    updateLastActiveTimestamp();

    // Skip on first run (no previous timestamp)
    if (lastActiveTimestamp == 0L) return;

    // Query for pending invitations created since last active time
    db.collection("invitations")
            .whereEqualTo("to", uid)
            .whereEqualTo("status", "pending")
            .whereGreaterThan("createdAt", new Timestamp(lastActiveTimestamp / 1000, 0))
            .orderBy("createdAt")
            .limit(1)
            .get()
            .addOnSuccessListener(snap -> {
                if (!snap.isEmpty()) {
                    DocumentSnapshot doc = snap.getDocuments().get(0);
                    // Check if invite is still valid (not expired)
                    if (doc.getTimestamp("expireAt") != null &&
                            doc.getTimestamp("expireAt").toDate().getTime() > nowMs) {
                        showMissedInviteDialog();
                    }
                }
            });
}
```

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
