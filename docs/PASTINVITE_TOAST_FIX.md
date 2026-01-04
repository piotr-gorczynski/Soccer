# Fix for Toast Messages Not Showing in PastInviteAdapter

## Issue Summary
When users tried to interact with disabled buttons in the Past Invites screen (PastInviteAdapter), no toast messages appeared to explain why the buttons were disabled. The buttons for "Send Invite" and "Add Friend" would appear dimmed but would not respond to clicks at all.

## Root Cause
The buttons were being disabled using `setEnabled(false)` when certain conditions were met (tournament ended, match completed, user offline, etc.). In Android, disabled buttons do not respond to click events, which prevented the `onClickListener` from executing and showing the explanatory toast messages.

The logic flow was:
1. `updateButtonState()` method would call `setEnabled(false)` on buttons
2. User would tap the disabled button
3. Android would ignore the tap because the button was disabled
4. Toast message in the `onClickListener` would never execute

## Solution
Changed the approach to keep buttons **always enabled** (`setEnabled(true)`) but still visually indicate their disabled state using alpha transparency. The validation logic in the click listeners now handles all cases and shows appropriate toast messages.

### Key Changes in PastInviteAdapter.java

1. **onCreateViewHolder() - Click Listeners** (lines 85-130)
   - Refactored click listener to check conditions and show toasts
   - Toast for tournament not running: `R.string.tournament_not_running`
   - Toast for match already completed: `R.string.tournament_match_already_completed`
   - Toast for already friends: `R.string.already_friends`

2. **onBindViewHolder() - Initial State** (lines 184-194)
   - Changed from `setEnabled(false)` to `setEnabled(true)` when uid is null
   - Added `setAlpha(0.3f)` to sendInviteBtn for visual feedback

3. **onBindViewHolder() - Presence Loading** (lines 347-352)
   - Changed from `setEnabled(!alreadyFriend)` to `setEnabled(true)`
   - Maintained alpha transparency for visual feedback

4. **updateButtonState()** (lines 453-503)
   - Removed all `setEnabled(false)` calls
   - Changed to always use `setEnabled(true)`
   - Maintained existing alpha logic (0.3f when disabled-looking, 1f when enabled)
   - Added clarifying comments explaining the approach

## Behavior After Fix

### Send Invite Button
When the button appears disabled (alpha 0.3f) and user taps it:
- **Tournament ended**: Shows toast "Tournament [name] is no longer running"
- **Match completed**: Shows toast "This match was already completed and [winner] won"
- **User offline/deleted**: Button is dimmed but click listener can still fire if needed

### Add Friend Button  
When the button appears disabled (alpha 0.3f) and user taps it:
- **Already friends**: Shows toast "Already friends"
- **User offline/deleted**: Button is dimmed but can show message if needed

## Testing Recommendations

### Manual Testing
1. **Test Tournament Ended Scenario**:
   - Have a past invite for a tournament that has ended
   - Tap the dimmed "Send Invite" button
   - Verify toast appears: "Tournament [name] is no longer running"

2. **Test Match Completed Scenario**:
   - Have a past invite for a completed match
   - Tap the dimmed "Send Invite" button
   - Verify toast appears: "This match was already completed and [winner] won"

3. **Test Already Friends Scenario**:
   - Have a past invite from someone who is already your friend
   - Tap the dimmed "Add Friend" button
   - Verify toast appears: "Already friends"

### Automated Testing
The existing test infrastructure (`MenuActivityButtonStateTest.java`) validates button state management. Similar tests could be added for `PastInviteAdapter` to verify:
- Buttons remain enabled regardless of state
- Alpha values change correctly based on conditions
- Click listeners execute and show appropriate toasts

## Related Code References
- String resources: `/mobile/app/src/main/res/values/strings.xml`
  - `tournament_not_running` (line 393)
  - `tournament_match_already_completed` (line 394)
  - `already_friends` (line 83)
- Related adapters with similar patterns:
  - `PendingInviteAdapter.java`
  - `MatchAdapter.java`
  - `TournamentAdapter.java`

## Notes
- This fix maintains all existing visual behavior (buttons still appear dimmed when they shouldn't be used)
- The only change is that now users get feedback when they try to use a dimmed button
- No changes to business logic or validation rules were made
- The fix is minimal and surgical, affecting only button enable/disable state
