# Past Invite Item Layout Redesign - Implementation Summary

## Overview
Redesigned the layout for `item_past_invite.xml` to display invite information more compactly and informatively.

## Changes Made

### Layout Changes (item_past_invite.xml)
**Before:**
```
Invite from: %USERNAME
Received %RELATIVE DATE
Status: %STATUS
%RELATIVE PRESENCE DATE
[Buttons]
```

**After:**
```
Invite from: %USERNAME
Received: %RELATIVE DATE | Status: %STATUS
%USERNAME status: %RELATIVE PRESENCE DATE
[Buttons]
```

### Key Modifications

1. **Merged TextViews** (item_past_invite.xml)
   - Removed separate `inviteReceivedTime` and `inviteStatus` TextViews
   - Added single `inviteReceivedAndStatus` TextView to show both on one line

2. **New String Resources** (strings.xml)
   - `invite_received_and_status`: "Received: %1$s | %2$s" (combines time and status)
   - `presence_user_online`: "%1$s status: Online" (includes username)
   - `presence_user_last_seen`: "%1$s status: Last seen %2$s" (includes username)
   - `presence_user_offline`: "%1$s status: Offline" (includes username)

3. **Adapter Updates** (PastInviteAdapter.java)
   - Modified `VH` class to use `inviteReceivedAndStatus` instead of separate fields
   - Updated `onBindViewHolder()` to combine received time and status into single text
   - Updated `bindPresence()` to include username in presence status messages
   - Added fallback "User" text if username not yet loaded

### Language Support
- Added new strings to 9 language files that already have invite translations:
  - Arabic (ar)
  - Amharic (am)
  - Bengali (bn)
  - German (de)
  - Khmer (km)
  - Malagasy (mg)
  - Polish (pl)
  - Somali (so)
  - Urdu (ur)
- English placeholders used for now (translators can update later)

### Files Modified
```
mobile/app/src/main/res/layout/item_past_invite.xml
mobile/app/src/main/java/piotr_gorczynski/soccer2/PastInviteAdapter.java
mobile/app/src/main/res/values/strings.xml
mobile/app/src/main/res/values-am/strings.xml
mobile/app/src/main/res/values-ar/strings.xml
mobile/app/src/main/res/values-bn/strings.xml
mobile/app/src/main/res/values-de/strings.xml
mobile/app/src/main/res/values-km/strings.xml
mobile/app/src/main/res/values-mg/strings.xml
mobile/app/src/main/res/values-pl/strings.xml
mobile/app/src/main/res/values-so/strings.xml
mobile/app/src/main/res/values-ur/strings.xml
```

## Implementation Details

### String Format Logic
The adapter now handles three scenarios for the combined received/status line:
1. Both time and status available: "Received: 2 hours ago | Status: Accepted"
2. Only time available: "Received: 2 hours ago"
3. Only status available: "Status: Accepted"
4. Neither available: Empty string

### Presence Display
The presence line now includes the username for better context:
- Online: "John status: Online"
- Last seen: "John status: Last seen 5 minutes ago"
- Offline: "John status: Offline"

If username is not yet loaded, falls back to "User status: ..."

## Testing Recommendations
1. Verify layout displays correctly with various invite states
2. Test with different username lengths to ensure text wrapping works
3. Verify all language strings display correctly
4. Test edge cases (missing data, very old invites, etc.)

## Benefits
- More compact layout saves vertical space
- Username in presence status provides better context
- Clearer visual hierarchy with combined received/status line
- Consistent with modern UI patterns
