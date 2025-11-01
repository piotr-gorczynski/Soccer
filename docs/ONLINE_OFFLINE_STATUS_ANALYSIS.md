# Online/Offline Status Management Analysis

## Overview

The Soccer app implements a sophisticated online/offline status management system that tracks user presence across different app states and device conditions. This analysis documents how the system works, how it's rendered to other users, and provides recommendations for improvements.

## Status States

The system uses a **3-state presence model**:

1. **"online"** - User is actively using the app (app in foreground)
2. **"active"** - User was recently seen (last heartbeat within 20 minutes) 
3. **"offline"** - User hasn't been seen for more than 20 minutes or explicitly logged out

## App Lifecycle Behavior

### 1. App in Foreground (`SoccerApp.onStart()`)
**Location:** `mobile/app/src/main/java/piotr_gorczynski/soccer2/SoccerApp.java:279-291`

When the app returns to foreground:
- Firebase Realtime Database goes online (`FirebaseDatabase.getInstance().goOnline()`)
- Heartbeat background worker is cancelled (`cancelHeartbeat()`)
- User status is immediately set to `"online"` with current timestamp (`setUserOnline()`)

### 2. App Goes to Background (`SoccerApp.onStop()`)
**Location:** `mobile/app/src/main/java/piotr_gorczynski/soccer2/SoccerApp.java:295-314`

When the app goes to background:
- User status is set to `"offline"` with current timestamp (`buildAway()`)
- Firebase Realtime Database goes offline (`FirebaseDatabase.getInstance().goOffline()`)
- Background heartbeat worker is scheduled (`scheduleHeartbeat()`)

### 3. Phone Sleep/Background with Heartbeat Worker
**Location:** `mobile/app/src/main/java/piotr_gorczynski/soccer2/SoccerApp.java:340-376`

When phone sleeps or app is in background:
- `HeartbeatWorker` runs every **15 minutes** (WorkManager minimum interval)
- Worker briefly goes online, updates `last_heartbeat` timestamp, then goes offline
- Does NOT change the "offline" state - only updates timestamp

## Heartbeat Update Frequency

### Active App (Foreground)
- **Immediate updates** when app state changes
- Real-time Firebase listeners maintain connection

### Inactive App (Background/Sleep)
- **Every 15 minutes** via `HeartbeatWorker`
- Uses WorkManager's `PeriodicWorkRequest` with minimum interval
- Worker temporarily connects, updates timestamp, then disconnects

### Server-Side Cleanup
**Location:** `firebase/functions/expire-presence/index.js`
- Firebase Cloud Function runs **every 5 minutes**
- Marks users as truly "offline" (with `last_heartbeat: 0`) if they've been "online" but inactive for more than 20 minutes

## How Status is Rendered on Other Users' Phones

### Real-Time Presence Detection
**Locations:** 
- `FriendAdapter.java:113-135`
- `MatchAdapter.java:223-250`

Other users see presence through Firebase Realtime Database listeners that:

1. **Listen to `/status/{uid}` node** for each friend/opponent
2. **Calculate presence state** based on:
   ```java
   if ("online".equals(stateStr)) {
       state = "online";
   } else if (System.currentTimeMillis() - lastHb < 20 * 60_000L) {
       state = "active";           // seen within 20 min window
   } else {
       state = "offline";
   }
   ```

### Visual Status Indicators

#### Friends List (`FriendAdapter.bindPresence()`)
- **"Online"** - Green text
- **"Last seen X minutes/hours ago"** - Green text (for "active" state)
- **"Offline"** - Grey text

#### Match List (`MatchAdapter.onBindViewHolder()`)
- Same visual scheme as Friends List
- Uses `englishRelative()` helper for human-readable timestamps

## Invite Button Visibility Logic

### Current Logic

#### Friends List (`FriendAdapter.java:158`)
```java
boolean isOffline = "offline".equalsIgnoreCase(state);
h.inviteBtn.setVisibility(isOffline ? View.GONE : View.VISIBLE);
```

#### Match List (`MatchAdapter.java:312-316`)
```java
boolean isActiveOrCompleted = "playing".equals(st) || "completed".equals(st);
boolean isOffline = pState == null || "offline".equalsIgnoreCase(pState);

if (isActiveOrCompleted || isOffline) {
    h.inviteBtn.setVisibility(View.GONE);
} else {
    h.inviteBtn.setVisibility(View.VISIBLE);
}
```

### Current Behavior Summary
- **Invite button is VISIBLE** only when:
  - Friend/opponent is "online" OR "active" (seen within 20 minutes)
  - Match is not already "playing" or "completed"
- **Invite button is HIDDEN** when:
  - User is "offline" (not seen for 20+ minutes)
  - Match is already active or completed

## Problem with Current System

### The Core Issue
When users forget to activate the game after their phone goes to sleep:
1. App goes to background → status becomes "offline"
2. HeartbeatWorker continues updating `last_heartbeat` every 15 minutes
3. Other users see them as "active" but **cannot send invites** because the button is hidden for "offline" state
4. This creates a contradiction: user appears active but is unreachable

### Root Cause Analysis
The system conflates **connection state** ("online"/"offline") with **reachability** for invites. A user can be:
- State: "offline" (app in background)
- But: `last_heartbeat` is recent (within 20 minutes)
- Result: Visible as "active" but unreachable for invites

## Recommendations

### 1. Enable Invites for "Active" Users
**Rationale:** If a user was seen within 20 minutes, they likely still have the app installed and receive push notifications.

**Implementation:** Change invite button logic from:
```java
// Current - hides button for any "offline" state
boolean isOffline = "offline".equalsIgnoreCase(state);
h.inviteBtn.setVisibility(isOffline ? View.GONE : View.VISIBLE);
```

To:
```java
// Proposed - only hide button for truly inactive users
boolean isTrulyOffline = "offline".equalsIgnoreCase(state) && 
    (System.currentTimeMillis() - lastHeartbeat > 20 * 60_000L);
h.inviteBtn.setVisibility(isTrulyOffline ? View.GONE : View.VISIBLE);
```

### 2. Enhanced Status Communication
Add visual distinction for different availability levels:
- **"Online"** - Green dot, full invite capability
- **"Active (Background)"** - Yellow dot, invite with notification
- **"Offline"** - Grey dot, no invites

### 3. Improve Heartbeat Strategy
Consider:
- Reducing heartbeat interval to 10 minutes for better responsiveness
- Adding immediate heartbeat on important app events (notifications received)
- Using Firebase Cloud Messaging to wake the heartbeat worker when needed

### 4. User Education
Add tooltips or help text explaining:
- When invites are sent as push notifications
- Why some users show as "active" but may respond slower
- How to keep the app "online" for immediate invites

## Technical Implementation Notes

### Database Structure
```
/status/{uid}
├── state: "online" | "offline"  
└── last_heartbeat: timestamp | 0
```

### State Transitions
```
User Login → "online" (immediate)
App Background → "offline" (but heartbeat continues)
App Foreground → "online" (immediate)  
User Logout → "offline" with last_heartbeat: 0
20min Inactivity → Server sets last_heartbeat: 0
```

### Code Files Involved
- `SoccerApp.java` - Core lifecycle and heartbeat management
- `FriendAdapter.java` - Friends list presence rendering and invite logic
- `MatchAdapter.java` - Match list presence rendering and invite logic  
- `expire-presence/index.js` - Server-side cleanup function

This analysis provides a comprehensive understanding of the current system and clear recommendations for improving user reachability while maintaining system performance.